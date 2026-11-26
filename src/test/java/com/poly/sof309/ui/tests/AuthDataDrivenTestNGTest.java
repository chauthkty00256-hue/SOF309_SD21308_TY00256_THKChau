package com.poly.sof309.ui.tests;

import com.poly.sof309.ui.listeners.ScreenshotTestListener;
import com.poly.sof309.ui.pages.LoginPage;
import com.poly.sof309.ui.pages.RegisterPage;
import com.poly.sof309.ui.utils.DriverManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.UUID;

/**
 * [Y4 - P2] Kịch bản Data-Driven Testing bằng TestNG (@DataProvider + groups="smoke" + ITestListener)
 * cho các luồng Đăng nhập đa vai trò và Đăng ký + Xác thực OTP (Mock OTP 123456).
 */
@Listeners(ScreenshotTestListener.class)
public class AuthDataDrivenTestNGTest extends BaseUiTest {

    @BeforeMethod(alwaysRun = true)
    public void openBrowser() {
        DriverManager.initDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        DriverManager.quitDriver();
    }

    /**
     * Bộ dữ liệu tài khoản đa vai trò khớp với mock-data/vsport-mock-data.json
     * (mật khẩu chung: 123456) kèm các ca âm tính (sai mật khẩu / không tồn tại).
     */
    @DataProvider(name = "loginAccounts")
    public Object[][] provideLoginAccounts() {
        return new Object[][]{
                {"admin", "123456", true, "/admin/tong-quan", "Admin (Role 1)"},
                {"manager1@vsport.vn", "123456", true, "/manager/dashboard", "Manager1 Email (Role 2)"},
                {"0901000003", "123456", true, "/index.jsp", "Khach1 SĐT (Role 3)"},
                {"staff1", "123456", true, "/staff/dashboard", "Staff1 Lễ tân (Role 4)"},
                {"khach2", "WrongPass@123", false, "không chính xác", "Sai mật khẩu"},
                {"nouser_ui_99", "123456", false, "không chính xác", "Tài khoản không tồn tại"}
        };
    }

    @Test(
            groups = {"smoke", "auth"},
            dataProvider = "loginAccounts",
            description = "Data-Driven: Kiểm thử đăng nhập lặp nhiều tài khoản đa vai trò và ca sai mật khẩu"
    )
    public void testLoginDataDriven(
            String identifier,
            String password,
            boolean expectedSuccess,
            String expectedTargetOrError,
            String scenarioDesc
    ) {
        WebDriver driver = DriverManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);

        loginPage.open(baseUrl);
        loginPage.login(identifier, password);

        if (expectedSuccess) {
            boolean redirected = loginPage.waitForRedirectTo(expectedTargetOrError);
            Assert.assertTrue(
                    redirected,
                    "[" + scenarioDesc + "] Phải điều hướng tới " + expectedTargetOrError
                            + ", URL hiện tại: " + driver.getCurrentUrl()
            );
        } else {
            String toast = loginPage.waitForToastMessage();
            Assert.assertTrue(
                    toast.contains(expectedTargetOrError),
                    "[" + scenarioDesc + "] Thông báo lỗi phải chứa '" + expectedTargetOrError
                            + "', thực tế: " + toast
            );
        }
    }

    /**
     * Bộ dữ liệu kiểm thử xác thực OTP khi đăng ký tài khoản mới:
     * - OTP sai (000000) -> báo lỗi
     * - Mock OTP mặc định (123456) -> đăng ký thành công
     */
    @DataProvider(name = "otpRegistrationData")
    public Object[][] provideOtpRegistrationData() {
        return new Object[][]{
                {"000000", false, "không chính xác", "Nhập sai OTP (000000)"},
                {"123456", true, "thành công", "Nhập đúng Mock OTP (123456)"}
        };
    }

    @Test(
            groups = {"regression", "auth"},
            dataProvider = "otpRegistrationData",
            description = "Data-Driven: Đăng ký khách hàng mới và xác thực với OTP sai vs Mock OTP 123456"
    )
    public void testRegisterAndOtpVerification(
            String otpCode,
            boolean expectedSuccess,
            String expectedToastPart,
            String scenarioDesc
    ) {
        WebDriver driver = DriverManager.getDriver();
        RegisterPage registerPage = new RegisterPage(driver);

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String uniqueEmail = "ui_" + suffix + "@vsport.vn";
        String uniquePhone = "097" + String.format("%07d", Math.abs(suffix.hashCode()) % 10_000_000);

        registerPage.open(baseUrl);
        registerPage.fillRegistrationForm(
                "Khách Test UI " + suffix,
                uniquePhone,
                uniqueEmail,
                "Test@1234",
                "Test@1234",
                true
        );
        registerPage.submitRegistration();
        Assert.assertTrue(registerPage.waitForOtpModalVisible(), "[" + scenarioDesc + "] Modal OTP phải hiển thị");

        registerPage.enterOtpAndSubmit(otpCode);
        String toast = registerPage.waitForToastMessageContaining(expectedToastPart);

        if (expectedSuccess) {
            Assert.assertTrue(toast.contains(expectedToastPart), "[" + scenarioDesc + "] Kỳ vọng thành công, thực tế: " + toast);
        } else {
            Assert.assertTrue(toast.contains(expectedToastPart), "[" + scenarioDesc + "] Kỳ vọng báo lỗi OTP, thực tế: " + toast);
        }
    }
}
