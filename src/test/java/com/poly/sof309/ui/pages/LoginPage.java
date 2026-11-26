package com.poly.sof309.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * [Y4] Page Object Model (POM) cho màn hình Đăng nhập V-SPORT (/index.jsp?auth=login).
 * Toàn bộ thao tác DOM sử dụng WebDriverWait, không dùng Thread.sleep().
 */
public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Locator thực tế trên index.jsp
    private final By authView = By.id("authView");
    private final By loginForm = By.id("loginForm");
    private final By txtIdentifier = By.id("loginEmail");
    private final By txtPassword = By.id("loginPassword");
    private final By btnLogin = By.id("loginSubmitBtn");
    private final By lblEmailError = By.id("loginEmailError");
    private final By lblPasswordError = By.id("loginPasswordError");
    private final By toastContainer = By.id("successToast");
    private final By toastMessage = By.id("toastMessage");
    private final By loggedInUserButton = By.id("vsUserBtn");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/index.jsp?auth=login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(loginForm));
    }

    public void enterUsername(String identifier) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(txtIdentifier));
        el.clear();
        if (identifier != null) {
            el.sendKeys(identifier);
        }
    }

    public void enterPassword(String password) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(txtPassword));
        el.clear();
        if (password != null) {
            el.sendKeys(password);
        }
    }

    public void clickLogin() {
        wait.until(ExpectedConditions.elementToBeClickable(btnLogin)).click();
    }

    public void login(String identifier, String password) {
        enterUsername(identifier);
        enterPassword(password);
        clickLogin();
    }

    public String waitForToastMessage() {
        wait.until(d -> {
            WebElement toast = d.findElement(toastContainer);
            String cls = toast.getAttribute("class");
            String text = d.findElement(toastMessage).getText();
            return cls != null && cls.contains("active") && text != null && !text.isBlank();
        });
        return driver.findElement(toastMessage).getText().trim();
    }

    public boolean waitForRedirectTo(String expectedUrlPart) {
        return wait.until(d -> {
            String url = d.getCurrentUrl();
            if ("/index.jsp".equals(expectedUrlPart)) {
                return !url.contains("auth=login")
                        && !d.findElements(loggedInUserButton).isEmpty();
            }
            return url.contains(expectedUrlPart);
        });
    }

    public String getErrorMessage() {
        String toast = driver.findElements(toastMessage).isEmpty()
                ? ""
                : driver.findElement(toastMessage).getText().trim();
        if (!toast.isEmpty()) {
            return toast;
        }
        String emailErr = driver.findElement(lblEmailError).getText().trim();
        if (!emailErr.isEmpty()) {
            return emailErr;
        }
        return driver.findElement(lblPasswordError).getText().trim();
    }

    public boolean isAuthViewVisible() {
        return !driver.findElements(authView).isEmpty() && driver.findElement(loginForm).isDisplayed();
    }
}
