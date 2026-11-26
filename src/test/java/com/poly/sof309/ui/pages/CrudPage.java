package com.poly.sof309.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * [Y4] Page Object Model (POM) cho màn hình CRUD Kho & Dịch vụ (/manager/kho-dich-vu).
 * Quản lý thao tác Thêm mới sản phẩm, Tìm kiếm/Lọc và Xóa mềm kèm xác nhận modal.
 */
public class CrudPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Header & Thanh lọc
    private final By pageHeading = By.xpath("//h1[contains(.,'Kho')]");
    private final By txtSearch = By.cssSelector("input[name='search']");
    private final By btnFilter = By.xpath("//form[@method='GET']//button[@type='submit']");
    private final By btnOpenAddModal = By.cssSelector("button[onclick='openAddModal()']");

    // Modal thêm mặt hàng (#addModal)
    private final By addModal = By.id("addModal");
    private final By txtAddSku = By.id("addSkuCode");
    private final By txtAddName = By.cssSelector("#addModal input[name='tenSanPham']");
    private final By selAddCategory = By.cssSelector("#addModal select[name='danhMucID']");
    private final By txtAddUnit = By.cssSelector("#addModal input[name='donViTinh']");
    private final By txtAddGiaNhap = By.cssSelector("#addModal input[name='giaNhap']");
    private final By txtAddDonGia = By.cssSelector("#addModal input[name='donGia']");
    private final By txtAddStock = By.cssSelector("#addModal input[name='soLuongTon']");
    private final By btnAddSubmit = By.cssSelector("#addModal button[type='submit']");

    // Alert kết quả & Modal xác nhận xóa mềm
    private final By alertSuccess = By.cssSelector("div.bg-emerald-50 p");
    private final By alertError = By.cssSelector("div.bg-rose-50 p");
    private final By customConfirmModal = By.id("customConfirmModal");
    private final By btnCustomConfirmSubmit = By.id("customConfirmSubmitBtn");
    private final By tableBody = By.cssSelector("table tbody");

    public CrudPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/manager/kho-dich-vu");
        wait.until(ExpectedConditions.visibilityOfElementLocated(pageHeading));
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    public boolean isLoaded() {
        return !driver.findElements(pageHeading).isEmpty() && driver.findElement(pageHeading).isDisplayed();
    }

    public void createProduct(String sku, String name, String unit, String giaNhap, String donGia, String stock) {
        WebElement openBtn = wait.until(ExpectedConditions.elementToBeClickable(btnOpenAddModal));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", openBtn);
        openBtn.click();

        // Nếu modal chưa mở do sự kiện click bị che/trễ script, gọi trực tiếp hàm mở modal của trang
        if (driver.findElement(addModal).getAttribute("class").contains("hidden")) {
            ((JavascriptExecutor) driver).executeScript(
                    "if (typeof openAddModal === 'function') { openAddModal(); } "
                            + "else { var m = document.getElementById('addModal'); m.classList.remove('hidden'); m.classList.add('flex'); }"
            );
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(txtAddSku));

        fillInput(driver.findElement(txtAddSku), sku);
        fillInput(driver.findElement(txtAddName), name);

        Select categorySelect = new Select(driver.findElement(selAddCategory));
        if (categorySelect.getOptions().size() > 1) {
            categorySelect.selectByIndex(1);
        } else if (!categorySelect.getOptions().isEmpty()) {
            categorySelect.selectByIndex(0);
        }

        fillInput(driver.findElement(txtAddUnit), unit);
        fillInput(driver.findElement(txtAddGiaNhap), giaNhap);
        fillInput(driver.findElement(txtAddDonGia), donGia);
        fillInput(driver.findElement(txtAddStock), stock);

        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(btnAddSubmit));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', behavior:'instant'});", submitBtn);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);

        wait.until(ExpectedConditions.visibilityOfElementLocated(alertSuccess));
    }

    private void fillInput(WebElement el, String value) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', behavior:'instant'});", el);
        el.clear();
        el.sendKeys(value);
        String current = el.getAttribute("value");
        if (current == null || !current.equals(value)) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];"
                            + "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));"
                            + "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                    el, value
            );
        }
    }

    public void searchProduct(String keyword) {
        WebElement searchEl = wait.until(ExpectedConditions.visibilityOfElementLocated(txtSearch));
        fillInput(searchEl, keyword);
        WebElement filterBtn = wait.until(ExpectedConditions.elementToBeClickable(btnFilter));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterBtn);
        wait.until(d -> d.getCurrentUrl().contains("search="));
        wait.until(ExpectedConditions.visibilityOfElementLocated(tableBody));
    }

    public boolean isProductVisibleInTable(String skuOrName) {
        try {
            return wait.until(d -> {
                List<WebElement> bodies = d.findElements(tableBody);
                if (bodies.isEmpty()) {
                    return false;
                }
                String text = bodies.get(0).getText();
                return text != null && text.contains(skuOrName);
            });
        } catch (Exception e) {
            return false;
        }
    }

    public void softDeleteProductBySku(String sku) {
        By deleteBtnLocator = By.xpath(
                "//table//tbody//tr[contains(., '" + sku + "')]//button[@title='Xóa mềm (khôi phục tại Thùng rác)']"
        );
        WebElement deleteBtn = wait.until(ExpectedConditions.presenceOfElementLocated(deleteBtnLocator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', behavior:'instant'});", deleteBtn);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteBtn);

        wait.until(ExpectedConditions.visibilityOfElementLocated(customConfirmModal));
        WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(btnCustomConfirmSubmit));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);

        // confirmDelete dùng setTimeout 1200ms trước khi submit form POST -> chờ alertSuccess hiển thị sau reload
        wait.until(d -> !d.findElements(alertSuccess).isEmpty()
                && d.findElement(alertSuccess).getText().contains("Thùng rác"));
    }

    public String getSuccessAlertText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(alertSuccess)).getText().trim();
    }

    public String getErrorAlertText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(alertError)).getText().trim();
    }
}
