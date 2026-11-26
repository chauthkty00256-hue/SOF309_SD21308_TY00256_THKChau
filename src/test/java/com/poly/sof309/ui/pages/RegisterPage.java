package com.poly.sof309.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * [Y4] Page Object Model (POM) cho màn hình Đăng ký & Modal Xác thực OTP V-SPORT.
 * Quản lý chuyển tab sang #registerCol (do ?auth=register mặc định mở #loginCol) và nhập 6 ô OTP.
 */
public class RegisterPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By tabRegister = By.cssSelector(".auth-tab-btn[data-target='registerCol']");
    private final By registerForm = By.id("registerForm");
    private final By txtFullName = By.id("regName");
    private final By txtPhone = By.id("regPhone");
    private final By txtEmail = By.id("regEmail");
    private final By txtPassword = By.id("regPassword");
    private final By txtConfirmPassword = By.id("regConfirmPassword");
    private final By chkAgreeTerms = By.id("agreeTerms");
    private final By btnRegisterSubmit = By.id("registerSubmitBtn");

    // Modal OTP trên index.jsp
    private final By otpModal = By.id("otpModal");
    private final By otpDigits = By.cssSelector("#otpBoxRow input.otp-digit");
    private final By btnOtpSubmit = By.id("otpSubmitBtn");
    private final By toastContainer = By.id("successToast");
    private final By toastMessage = By.id("toastMessage");

    public RegisterPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/index.jsp?auth=register");
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        // Trên mobile cần click tab Đăng ký; trên desktop (>=768px) 2 cột hiển thị song song nên #regName đã hiện sẵn
        List<WebElement> regInputs = driver.findElements(txtFullName);
        if (regInputs.isEmpty() || !regInputs.get(0).isDisplayed()) {
            wait.until(ExpectedConditions.elementToBeClickable(tabRegister)).click();
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(txtFullName));
    }

    public void fillRegistrationForm(String fullName, String phone, String email, String password, String confirmPassword, boolean agree) {
        WebElement nameEl = wait.until(ExpectedConditions.visibilityOfElementLocated(txtFullName));
        nameEl.clear();
        nameEl.sendKeys(fullName);

        WebElement phoneEl = driver.findElement(txtPhone);
        phoneEl.clear();
        phoneEl.sendKeys(phone);

        WebElement emailEl = driver.findElement(txtEmail);
        emailEl.clear();
        emailEl.sendKeys(email);

        WebElement passEl = driver.findElement(txtPassword);
        passEl.clear();
        passEl.sendKeys(password);

        WebElement confirmEl = driver.findElement(txtConfirmPassword);
        confirmEl.clear();
        confirmEl.sendKeys(confirmPassword);

        WebElement agreeEl = driver.findElement(chkAgreeTerms);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', behavior:'instant'});", agreeEl);
        if (agree && !agreeEl.isSelected()) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", agreeEl);
        } else if (!agree && agreeEl.isSelected()) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", agreeEl);
        }
    }

    public void submitRegistration() {
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(btnRegisterSubmit));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', behavior:'instant'});", btn);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
    }

    public boolean waitForOtpModalVisible() {
        wait.until(d -> {
            WebElement modal = d.findElement(otpModal);
            String cls = modal.getAttribute("class");
            return modal.isDisplayed() || (cls != null && cls.contains("active"));
        });
        return true;
    }

    public void enterOtpAndSubmit(String sixDigitOtp) {
        waitForOtpModalVisible();
        List<WebElement> boxes = wait.until(ExpectedConditions.numberOfElementsToBe(otpDigits, 6));
        for (int i = 0; i < 6 && i < sixDigitOtp.length(); i++) {
            WebElement box = boxes.get(i);
            box.clear();
            box.sendKeys(String.valueOf(sixDigitOtp.charAt(i)));
        }
        wait.until(ExpectedConditions.elementToBeClickable(btnOtpSubmit)).click();
    }

    public String waitForToastMessageContaining(String expectedSubstring) {
        wait.until(d -> {
            WebElement toast = d.findElement(toastContainer);
            String cls = toast.getAttribute("class");
            String text = d.findElement(toastMessage).getText();
            return cls != null && cls.contains("active") && text != null
                    && (expectedSubstring == null || expectedSubstring.isEmpty() || text.contains(expectedSubstring));
        });
        return driver.findElement(toastMessage).getText().trim();
    }

    public boolean isRegisterFormVisible() {
        return !driver.findElements(registerForm).isEmpty() && driver.findElement(txtFullName).isDisplayed();
    }
}
