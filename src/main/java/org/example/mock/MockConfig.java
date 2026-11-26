package org.example.mock;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cấu hình chế độ chạy cục bộ không cần SQL Server (Mock / In-Memory + JSON).
 *
 * Mặc định tự động bật Mock Mode nếu không có cấu hình DB_URL / db.url,
 * hoặc khi VSPORT_MOCK_MODE=true / -Dvsport.mock.mode=true.
 * Để dùng lại SQL Server thật, đặt VSPORT_MOCK_MODE=false và cấu hình DB_URL, DB_USERNAME, DB_PASSWORD.
 */
public final class MockConfig {

    public static final String DEFAULT_MOCK_OTP = "123456";

    private MockConfig() {
    }

    public static boolean isMockMode() {
        String envFlag = System.getenv("VSPORT_MOCK_MODE");
        if (envFlag == null || envFlag.isBlank()) {
            envFlag = System.getProperty("vsport.mock.mode");
        }
        if (envFlag == null || envFlag.isBlank()) {
            envFlag = System.getProperty("mockMode");
        }
        if (envFlag != null && !envFlag.isBlank()) {
            return !"false".equalsIgnoreCase(envFlag.trim()) && !"0".equals(envFlag.trim());
        }
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getProperty("db.url");
        }
        return dbUrl == null || dbUrl.isBlank();
    }

    public static boolean isMockOtpAccepted(String userOtp) {
        return isMockMode() && userOtp != null && DEFAULT_MOCK_OTP.equals(userOtp.trim());
    }

    public static Path getMockDataFilePath() {
        String custom = System.getenv("VSPORT_MOCK_FILE");
        if (custom == null || custom.isBlank()) {
            custom = System.getProperty("vsport.mock.file");
        }
        if (custom != null && !custom.isBlank()) {
            return Paths.get(custom.trim()).toAbsolutePath();
        }
        return Paths.get("mock-data", "vsport-mock-data.json").toAbsolutePath();
    }
}
