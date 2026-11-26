package com.poly.sof309.ui.listeners;

import com.poly.sof309.ui.utils.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * [Y4 - P2] TestNG ITestListener tự động chụp ảnh màn hình mỗi khi test case fail (onTestFailure),
 * lưu vào thư mục screenshots/ tại gốc dự án với tên rõ ràng: <ClassName>_<MethodName>_<Timestamp>.png.
 */
public class ScreenshotTestListener implements ITestListener {

    public static final Path SCREENSHOT_DIR = Paths.get("screenshots");
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Override
    public void onStart(ITestContext context) {
        try {
            Files.createDirectories(SCREENSHOT_DIR);
        } catch (IOException e) {
            System.err.println("[ScreenshotTestListener] Không thể tạo thư mục screenshots/: " + e.getMessage());
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = DriverManager.getDriver();
        String className = result.getTestClass() != null
                ? result.getTestClass().getRealClass().getSimpleName()
                : "UnknownTest";
        String methodName = result.getMethod() != null
                ? result.getMethod().getMethodName()
                : "unknownMethod";

        Path savedPath = captureScreenshot(driver, className + "_" + methodName);
        if (savedPath != null) {
            System.out.println("[ScreenshotTestListener] Đã chụp ảnh lỗi tự động: " + savedPath.toAbsolutePath());
        }
    }

    /**
     * Chụp ảnh màn hình hiện tại của WebDriver vào thư mục screenshots/.
     * Trả về Path của file ảnh vừa lưu, hoặc null nếu driver không khả dụng.
     */
    public static Path captureScreenshot(WebDriver driver, String rawName) {
        if (!(driver instanceof TakesScreenshot takesScreenshot)) {
            System.err.println("[ScreenshotTestListener] WebDriver hiện tại là null hoặc không hỗ trợ TakesScreenshot.");
            return null;
        }
        try {
            Files.createDirectories(SCREENSHOT_DIR);
            String safeName = (rawName == null || rawName.isBlank() ? "failure" : rawName)
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = safeName + "_" + LocalDateTime.now().format(TS_FORMAT) + ".png";
            Path destination = SCREENSHOT_DIR.resolve(fileName);

            File tempFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
            Files.copy(tempFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (Exception e) {
            System.err.println("[ScreenshotTestListener] Lỗi khi chụp ảnh màn hình: " + e.getMessage());
            return null;
        }
    }
}
