package com.poly.sof309.ui.tests;

import com.poly.sof309.ui.listeners.ScreenshotTestListener;
import com.poly.sof309.ui.pages.CrudPage;
import com.poly.sof309.ui.pages.LoginPage;
import com.poly.sof309.ui.utils.DriverManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * [Y4 - P2] Kịch bản kiểm thử luồng CRUD Kho & Dịch vụ bằng TestNG:
 * - Gắn nhãn groups = "smoke" cho luồng cốt lõi (Đăng nhập -> Mở trang -> Tạo sản phẩm)
 *   và groups = "regression" cho bước tìm kiếm & xóa mềm dọn dữ liệu.
 * - Ràng buộc thứ tự thực thi chặt chẽ bằng dependsOnMethods.
 * - Tự động chụp ảnh lỗi vào screenshots/ thông qua ScreenshotTestListener.
 */
@Listeners(ScreenshotTestListener.class)
public class ManagerCrudFlowTestNGTest extends BaseUiTest {

    private WebDriver driver;
    private LoginPage loginPage;
    private CrudPage crudPage;
    private String createdSku;
    private String createdName;

    @BeforeClass(alwaysRun = true)
    public void setUpClass() {
        driver = DriverManager.initDriver();
        loginPage = new LoginPage(driver);
        crudPage = new CrudPage(driver);
        String tag = String.valueOf(System.currentTimeMillis() % 100000);
        createdSku = "SKU-POM-" + tag;
        createdName = "Nước điện giải POM " + tag;
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        DriverManager.quitDriver();
    }

    @Test(
            groups = {"smoke", "crud"},
            description = "Bước 1: Đăng nhập tài khoản Manager (manager1 / 123456) trước khi thao tác CRUD"
    )
    public void testStep1_ManagerLogin() {
        loginPage.open(baseUrl);
        loginPage.login("manager1", "123456");

        boolean redirected = loginPage.waitForRedirectTo("/manager/dashboard");
        Assert.assertTrue(redirected, "Manager phải điều hướng vào /manager/dashboard sau đăng nhập.");
    }

    @Test(
            groups = {"smoke", "crud"},
            dependsOnMethods = {"testStep1_ManagerLogin"},
            description = "Bước 2: Mở màn hình Kho & Dịch vụ (/manager/kho-dich-vu)"
    )
    public void testStep2_OpenKhoDichVuPage() {
        crudPage.open(baseUrl);
        Assert.assertTrue(crudPage.isLoaded(), "Trang Kho & Dịch vụ phải hiển thị tiêu đề.");
    }

    @Test(
            groups = {"smoke", "crud"},
            dependsOnMethods = {"testStep2_OpenKhoDichVuPage"},
            description = "Bước 3: Thêm mới sản phẩm hợp lệ qua #addModal"
    )
    public void testStep3_CreateProductSuccess() {
        crudPage.createProduct(createdSku, createdName, "Chai", "12000", "22000", "25");

        String successText = crudPage.getSuccessAlertText();
        Assert.assertFalse(successText.isBlank(), "Phải hiển thị thông báo thêm sản phẩm thành công.");
        Assert.assertTrue(
                crudPage.isProductVisibleInTable(createdSku),
                "Sản phẩm mới tạo (" + createdSku + ") phải xuất hiện trong bảng dữ liệu."
        );
    }

    @Test(
            groups = {"regression", "crud"},
            dependsOnMethods = {"testStep3_CreateProductSuccess"},
            description = "Bước 4: Tìm kiếm đúng sản phẩm vừa tạo theo mã SKU"
    )
    public void testStep4_SearchCreatedProduct() {
        crudPage.searchProduct(createdSku);
        Assert.assertTrue(
                crudPage.isProductVisibleInTable(createdName),
                "Kết quả tìm kiếm phải hiển thị sản phẩm: " + createdName
        );
    }

    @Test(
            groups = {"regression", "crud"},
            dependsOnMethods = {"testStep4_SearchCreatedProduct"},
            description = "Bước 5: Xóa mềm sản phẩm vừa tạo qua modal xác nhận #customConfirmSubmitBtn"
    )
    public void testStep5_SoftDeleteCreatedProduct() {
        crudPage.softDeleteProductBySku(createdSku);

        String successText = crudPage.getSuccessAlertText();
        Assert.assertTrue(
                successText.contains("Thùng rác"),
                "Xóa mềm phải hiển thị thông báo chuyển vào Thùng rác, thực tế: " + successText
        );
    }
}
