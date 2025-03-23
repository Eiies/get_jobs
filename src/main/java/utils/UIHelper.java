package utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户界面辅助类，提供进度显示和状态更新功能
 * 
 * @author loks666 项目链接:
 *         <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
@Slf4j
public class UIHelper {
    private static final String STATUS_FILE = "./html/status.json";
    private static final String LOG_FILE = "./html/activity_log.json";
    private static AtomicInteger totalJobs = new AtomicInteger(0);
    private static AtomicInteger processedJobs = new AtomicInteger(0);
    private static String currentPlatform = "";
    private static String currentStatus = "准备中";
    private static Date startTime = new Date();

    /**
     * 初始化UI辅助类
     * 
     * @param expectedTotalJobs 预期总任务数
     */
    public static void initialize(int expectedTotalJobs) {
        totalJobs.set(expectedTotalJobs);
        processedJobs.set(0);
        startTime = new Date();
        currentStatus = "准备中";
        updateStatusFile();
        initLogFile();
    }

    /**
     * 更新当前平台信息
     * 
     * @param platform 平台名称
     */
    public static void updatePlatform(String platform) {
        currentPlatform = platform;
        log.info("切换到平台: {}", platform);
        updateStatusFile();
        addLogEntry("切换到平台: " + platform);
    }

    /**
     * 更新当前状态
     * 
     * @param status 状态描述
     */
    public static void updateStatus(String status) {
        currentStatus = status;
        log.info("状态更新: {}", status);
        updateStatusFile();
        addLogEntry("状态更新: " + status);
    }

    /**
     * 更新进度
     * 
     * @param processed 已处理任务数
     * @param message 进度消息
     */
    public static void updateProgress(int processed, String message) {
        processedJobs.set(processed);
        log.info("进度更新: {}/{} - {}", processed, totalJobs.get(), message);
        updateStatusFile();
        addLogEntry(message);
    }

    /**
     * 增加已处理任务数
     * 
     * @param message 进度消息
     */
    public static void incrementProgress(String message) {
        int current = processedJobs.incrementAndGet();
        log.info("进度更新: {}/{} - {}", current, totalJobs.get(), message);
        updateStatusFile();
        addLogEntry(message);
    }

    /**
     * 记录成功投递的职位
     * 
     * @param company 公司名称
     * @param position 职位名称
     * @param recruiter 招聘人员
     */
    public static void logJobApplication(String company, String position, String recruiter) {
        String message = String.format("已投递: %s - %s (招聘官: %s)", company, position, recruiter);
        log.info(message);
        addLogEntry(message);
        incrementProgress(message);
    }

    /**
     * 记录错误信息
     * 
     * @param errorMessage 错误消息
     */
    public static void logError(String errorMessage) {
        log.error(errorMessage);
        addLogEntry("错误: " + errorMessage);
        updateStatusFile();
    }

    /**
     * 完成所有任务
     * 
     * @param summary 任务总结
     */
    public static void complete(String summary) {
        currentStatus = "已完成";
        log.info("任务完成: {}", summary);
        updateStatusFile();
        addLogEntry("任务完成: " + summary);
    }

    /**
     * 更新状态文件
     */
    private static void updateStatusFile() {
        try {
            // 确保目录存在
            File dir = new File("./html");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 计算进度百分比
            int total = totalJobs.get();
            int processed = processedJobs.get();
            int percentage = total > 0 ? (processed * 100) / total : 0;

            // 计算运行时间
            String duration = JobUtils.formatDuration(startTime, new Date());

            // 创建状态JSON
            String statusJson = String.format(
                    "{\"platform\": \"%s\", " + "\"status\": \"%s\", " + "\"progress\": %d, "
                            + "\"processed\": %d, " + "\"total\": %d, " + "\"duration\": \"%s\", "
                            + "\"lastUpdate\": \"%s\"}",
                    currentPlatform, currentStatus, percentage, processed, total, duration,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            // 写入文件
            try (FileWriter writer = new FileWriter(STATUS_FILE)) {
                writer.write(statusJson);
            }
        } catch (IOException e) {
            log.error("更新状态文件失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化日志文件
     */
    private static void initLogFile() {
        try {
            // 确保目录存在
            File dir = new File("./html");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 创建空的日志数组
            String initialLog = "[]"; 
            
            // 写入文件
            try (FileWriter writer = new FileWriter(LOG_FILE)) {
                writer.write(initialLog);
            }
        } catch (IOException e) {
            log.error("初始化日志文件失败: {}", e.getMessage());
        }
    }

    /**
     * 添加日志条目
     * @param message 日志消息
     */
    private static void addLogEntry(String message) {
        try {
            // 读取现有日志
            String logContent = "[]";
            if (Files.exists(Paths.get(LOG_FILE))) {
                logContent = new String(Files.readAllBytes(Paths.get(LOG_FILE)));
            }
            
            // 如果是空文件或格式不正确，初始化为空数组
            if (logContent.trim().isEmpty()) {
                logContent = "[]";
            }
            
            // 移除结尾的 ]
            if (logContent.endsWith("]")) {
                logContent = logContent.substring(0, logContent.length() - 1);
            } else {
                logContent = "[";
            }
            
            // 添加新条目
            String timestamp = new SimpleDateFormat("yyyy-MM-