package com.poly.sof309.ui.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * [Y4 - P2] Quản lý WebDriver tập trung theo ThreadLocal giúp:
 * - Mỗi luồng test độc lập trình duyệt.
 * - ScreenshotTestListener (ITestListener) luôn truy xuất đúng WebDriver đang lỗi để chụp ảnh.
 * - Hỗ trợ tự động bật Chrome Headless khi chạy trên GitHub Actions (CI=true) hoặc -Dheadless=true.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver initDriver() {
        ChromeOptions options = new ChromeOptions();
        boolean isCi = Boolean.parseBoolean(System.getenv("CI"));
        boolean isHeadlessProp = Boolean.parseBoolean(System.getProperty("headless", "false"));
        if (isCi || isHeadlessProp) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1440,900");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        DRIVER_THREAD_LOCAL.set(driver);
        return driver;
    }

    public static WebDriver getDriver() {
        return DRIVER_THREAD_LOCAL.get();
    }

    public static void setDriver(WebDriver driver) {
        DRIVER_THREAD_LOCAL.set(driver);
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER_THREAD_LOCAL.remove();
            }
        }
    }
}
