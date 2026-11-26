package com.poly.sof309.ui.listeners;

import com.poly.sof309.ui.utils.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.IClass;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Xác minh ScreenshotTestListener (ITestListener.onTestFailure) tự động tạo thư mục screenshots/
 * và lưu tệp ảnh định dạng <ClassName>_<MethodName>_<Timestamp>.png khi có test fail.
 */
@DisplayName("ScreenshotTestListener - Verification Test")
public class ScreenshotTestListenerTest {

    @AfterEach
    void cleanupDriver() {
        DriverManager.setDriver(null);
    }

    @Test
    @DisplayName("onTestFailure tự động chụp ảnh màn hình và lưu vào thư mục screenshots/")
    void testOnTestFailureSavesScreenshot() throws Exception {
        // Tạo 1 file PNG 1x1 tối thiểu hợp lệ để mô phỏng output từ TakesScreenshot
        File tempPng = Files.createTempFile("mock-shot-", ".png").toFile();
        tempPng.deleteOnExit();
        byte[] minimalPng = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };
        Files.write(tempPng.toPath(), minimalPng);

        WebDriver mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
        when(((TakesScreenshot) mockDriver).getScreenshotAs(OutputType.FILE)).thenReturn(tempPng);
        DriverManager.setDriver(mockDriver);

        ITestResult result = mock(ITestResult.class);
        IClass testClass = mock(IClass.class);
        ITestNGMethod method = mock(ITestNGMethod.class);
        when(testClass.getRealClass()).thenReturn((Class) ScreenshotTestListenerTest.class);
        when(method.getMethodName()).thenReturn("sampleFailedCase");
        when(result.getTestClass()).thenReturn(testClass);
        when(result.getMethod()).thenReturn(method);

        ScreenshotTestListener listener = new ScreenshotTestListener();
        listener.onTestFailure(result);

        assertTrue(Files.isDirectory(ScreenshotTestListener.SCREENSHOT_DIR), "Thư mục screenshots/ phải tồn tại");
        List<Path> created;
        try (Stream<Path> s = Files.list(ScreenshotTestListener.SCREENSHOT_DIR)) {
            created = s.filter(p -> p.getFileName().toString().startsWith("ScreenshotTestListenerTest_sampleFailedCase_"))
                    .toList();
        }
        assertFalse(created.isEmpty(), "Phải có ảnh lỗi được lưu vào screenshots/");
        // Dọn ảnh test ảo để giữ screenshots/ sạch cho ảnh thực tế từ Chrome
        for (Path p : created) {
            Files.deleteIfExists(p);
        }
    }
}
