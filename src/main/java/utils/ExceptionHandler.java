package utils;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 统一异常处理类，提供各种异常处理方法
 * 
 * @author loks666 项目链接:
 *         <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
@Slf4j
public class ExceptionHandler {

    /**
     * 执行可能抛出异常的操作，带有重试机制
     * 
     * @param operation 要执行的操作
     * @param maxRetries 最大重试次数
     * @param retryDelayMs 重试延迟（毫秒）
     * @param operationName 操作名称（用于日志）
     * @param <T> 返回类型
     * @return 操作结果，如果所有重试都失败则返回null
     */
    public static <T> T executeWithRetry(Callable<T> operation, int maxRetries, long retryDelayMs,
            String operationName) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    log.info("正在进行第{}次重试 {}", retryCount, operationName);
                }
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                retryCount++;

                if (retryCount <= maxRetries) {
                    log.warn("{}失败，将在{}ms后重试 ({}/{}): {}", operationName, retryDelayMs, retryCount,
                            maxRetries, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMs * retryCount); // 指数退避策略
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断", ie);
                    }
                }
            }
        }

        log.error("{}在{}次尝试后失败: {}", operationName, maxRetries, lastException.getMessage());
        return null;
    }

    /**
     * 安全执行不返回值的操作，捕获并记录异常
     * 
     * @param operation 要执行的操作
     * @param operationName 操作名称（用于日志）
     */
    public static void executeSafely(Runnable operation, String operationName) {
        try {
            operation.run();
        } catch (Exception e) {
            log.error("执行{}时发生异常: {}", operationName, e.getMessage(), e);
        }
    }

    /**
     * 处理特定类型的异常
     * 
     * @param operation 要执行的操作
     * @param exceptionHandler 异常处理器
     * @param exceptionClass 要处理的异常类型
     * @param <T> 返回类型
     * @param <E> 异常类型
     * @return 操作结果，如果发生异常则返回异常处理器的结果
     */
    public static <T, E extends Exception> T handleException(Callable<T> operation,
            Function<E, T> exceptionHandler, Class<E> exceptionClass) {
        try {
            return operation.call();
        } catch (Exception e) {
            if (exceptionClass.isInstance(e)) {
                return exceptionHandler.apply(exceptionClass.cast(e));
            }
            throw new RuntimeException("未处理的异常", e);
        }
    }

    /**
     * 分类处理网络相关异常
     * 
     * @param operation 要执行的操作
     * @param networkExceptionHandler 网络异常处理器
     * @param timeoutExceptionHandler 超时异常处理器
     * @param otherExceptionHandler 其他异常处理器
     * @param <T> 返回类型
     * @return 操作结果或异常处理器的结果
     */
    public static <T> T handleNetworkExceptions(Callable<T> operation,
            Function<Exception, T> networkExceptionHandler,
            Function<Exception, T> timeoutExceptionHandler,
            Function<Exception, T> otherExceptionHandler) {
        try {
            return operation.call();
        } catch (IOException e) {
            log.error("网络连接异常: {}", e.getMessage());
            return networkExceptionHandler.apply(e);
        } catch (WebDriverException e) {
            log.error("操作超时: {}", e.getMessage());
            return timeoutExceptionHandler.apply(e);
        } catch (Exception e) {
            log.error("其他异常: {}", e.getMessage());
            return otherExceptionHandler.apply(e);
        }
    }

    /**
     * 记录异常并继续执行
     * 
     * @param operation 要执行的操作
     * @param errorHandler 错误处理器
     * @param operationName 操作名称（用于日志）
     */
    public static void logAndContinue(Runnable operation, Consumer<Exception> errorHandler,
            String operationName) {
        try {
            operation.run();
        } catch (Exception e) {
            log.warn("{}时发生异常，但将继续执行: {}", operationName, e.getMessage());
            errorHandler.accept(e);
        }
    }
}
