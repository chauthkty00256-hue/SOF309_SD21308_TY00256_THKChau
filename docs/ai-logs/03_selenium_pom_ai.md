# Nhật ký AI: Thiết kế Khung Selenium POM & Nâng cấp TestNG (Y4 - P1 & P2)

> **Dự án:** V-SPORT (`SOF309_SD21308_TY00256_THKChau`)  
> **Giai đoạn:** Tuần 3–4 — Mục tiêu Y4 (`Page Object Model`, `WebDriverWait`, `TestNG groups`, `dependsOnMethods`, `@DataProvider`, `ITestListener` chụp ảnh lỗi, `testng.xml`)  
> **Tài liệu tham chiếu:** [docs/SPECIFICATIONS.md](../SPECIFICATIONS.md) | [docs/TEST_CASES.md](../TEST_CASES.md) | [testng.xml](../../testng.xml)

---

## 1. Kiến trúc Selenium Page Object Model (POM) + TestNG

Toàn bộ mã nguồn kiểm thử giao diện được tổ chức tách biệt rõ ràng thành 4 tầng trong `src/test/java/com/poly/sof309/ui/`:

```text
src/test/java/com/poly/sof309/ui/
├── utils/
│   └── DriverManager.java                # Quản lý WebDriver theo ThreadLocal, hỗ trợ GUI & Chrome Headless (CI=true / -Dheadless=true)
├── listeners/
│   ├── ScreenshotTestListener.java       # Implements ITestListener: tự động chụp ảnh lỗi vào screenshots/ khi onTestFailure
│   └── ScreenshotTestListenerTest.java   # Unit Test xác minh cơ chế lưu file ảnh của ScreenshotTestListener
├── pages/                                # Tầng Page Object Model (quản lý Locator + Action với WebDriverWait, không dùng Thread.sleep)
│   ├── LoginPage.java                    # Màn hình Đăng nhập (/index.jsp?auth=login)
│   ├── RegisterPage.java                 # Màn hình Đăng ký + Modal xác thực OTP (/index.jsp?auth=register)
│   └── CrudPage.java                     # Màn hình CRUD Kho & Dịch vụ (/manager/kho-dich-vu)
└── tests/                                # Tầng Test Script (chỉ chứa kịch bản và Assert)
    ├── BaseUiTest.java                   # Tự kiểm tra http://localhost:8080; nếu chưa chạy sẽ tự khởi động Embedded Tomcat Mock Mode cô lập
    ├── AuthDataDrivenTestNGTest.java     # TestNG @DataProvider + groups="smoke" (Đăng nhập 4 vai trò + OTP)
    ├── ManagerCrudFlowTestNGTest.java    # TestNG groups="smoke"/"regression" + chuỗi dependsOnMethods 5 bước CRUD
    └── LoginTest.java                    # File mẫu JUnit 5 ban đầu (giữ nguyên @Disabled)
```

* **Nguyên tắc đồng bộ DOM:** 100% các thao tác chờ phần tử trong `LoginPage`, `RegisterPage` và `CrudPage` dùng `WebDriverWait` (`ExpectedConditions.visibilityOfElementLocated`, `elementToBeClickable`, custom lambda wait cho Toast/Modal/URL), tuyệt đối không dùng `Thread.sleep()`.

---

## 2. Chi tiết các Kỹ thuật TestNG Nâng cao đã Triển khai

### 2.1. Phân nhóm `groups` (`smoke` / `regression`)
* **`groups = {"smoke", "auth"}`**: `AuthDataDrivenTestNGTest.testLoginDataDriven` — kiểm tra luồng đăng nhập cốt lõi đa vai trò và thông báo lỗi khi sai mật khẩu.
* **`groups = {"smoke", "crud"}`**: 3 bước cốt lõi của `ManagerCrudFlowTestNGTest` (`testStep1_ManagerLogin` → `testStep2_OpenKhoDichVuPage` → `testStep3_CreateProductSuccess`).
* **`groups = {"regression", "auth"}`**: `AuthDataDrivenTestNGTest.testRegisterAndOtpVerification` — luồng đăng ký tài khoản mới kèm xác thực OTP sai (`000000`) và Mock OTP (`123456`).
* **`groups = {"regression", "crud"}`**: `testStep4_SearchCreatedProduct` → `testStep5_SoftDeleteCreatedProduct` (tìm kiếm và xóa mềm dọn dữ liệu vừa tạo).

### 2.2. Chuỗi phụ thuộc `dependsOnMethods` (`ManagerCrudFlowTestNGTest`)
Thiết lập ràng buộc tuần tự trên cùng một phiên trình duyệt (`@BeforeClass` / `@AfterClass`); nếu bước trước thất bại, TestNG tự động **Skip** các bước phụ thuộc phía sau thay vì chạy lỗi dây chuyền:
1. `testStep1_ManagerLogin` *(đăng nhập `manager1` / `123456` → xác nhận vào `/manager/dashboard`)*
2. `testStep2_OpenKhoDichVuPage` (`dependsOnMethods = {"testStep1_ManagerLogin"}`) *(mở `/manager/kho-dich-vu`)*
3. `testStep3_CreateProductSuccess` (`dependsOnMethods = {"testStep2_OpenKhoDichVuPage"}`) *(thêm sản phẩm `SKU-POM-<tag>` qua `#addModal`)*
4. `testStep4_SearchCreatedProduct` (`dependsOnMethods = {"testStep3_CreateProductSuccess"}`) *(tìm theo mã SKU trong bảng)*
5. `testStep5_SoftDeleteCreatedProduct` (`dependsOnMethods = {"testStep4_SearchCreatedProduct"}`) *(xóa mềm qua `#customConfirmSubmitBtn`, xác nhận thông báo `"Thùng rác"`)*

### 2.3. Data-Driven Testing với `@DataProvider` (`AuthDataDrivenTestNGTest`)
* **`@DataProvider(name = "loginAccounts")` (6 bộ dữ liệu khớp `mock-data/vsport-mock-data.json`):**

| Identifier | Mật khẩu | Kỳ vọng thành công | Đích URL / Chuỗi lỗi kỳ vọng | Kịch bản |
| :--- | :--- | :---: | :--- | :--- |
| `admin` | `123456` | `true` | `/admin/tong-quan` | Admin (`RoleID=1`) |
| `manager1@vsport.vn` | `123456` | `true` | `/manager/dashboard` | Manager Email (`RoleID=2`) |
| `0901000003` | `123456` | `true` | `/index.jsp` | Customer SĐT (`RoleID=3`) |
| `staff1` | `123456` | `true` | `/staff/dashboard` | Staff Lễ tân (`RoleID=4`) |
| `khach2` | `WrongPass@123` | `false` | `không chính xác` | Sai mật khẩu |
| `nouser_ui_99` | `123456` | `false` | `không chính xác` | Tài khoản không tồn tại |

* **`@DataProvider(name = "otpRegistrationData")` (2 bộ dữ liệu OTP):**
  1. `otp = "000000"` → thất bại, toast chứa `"không chính xác"`.
  2. `otp = "123456"` (Mock OTP mặc định) → thành công, toast chứa `"thành công"`.

### 2.4. Cơ chế tự động chụp ảnh màn hình lỗi qua `ITestListener` (`ScreenshotTestListener`)
* Khai báo cả trong `testng.xml` (`<listeners>`) và annotation `@Listeners(ScreenshotTestListener.class)`.
* Khi `onStart(ITestContext)`: tự động gọi `Files.createDirectories(Paths.get("screenshots"))` để đảm bảo thư mục `screenshots/` luôn tồn tại ở gốc dự án.
* Khi `onTestFailure(ITestResult)`: lấy `WebDriver` hiện hành từ `DriverManager.getDriver()`, ép kiểu `TakesScreenshot`, lưu file ảnh dạng:
  `screenshots/<ClassName>_<MethodName>_<yyyyMMdd_HHmmss_SSS>.png`.
* Đã kiểm chứng thực tế trên cả Chrome thật (khi phát sinh lỗi timeout/click trong các lượt debug đầu tiên, hệ thống đã tự động ghi ảnh PNG thực tế vào `screenshots/`) và qua `ScreenshotTestListenerTest`.

---

## 3. Cấu hình `testng.xml` & Tích hợp Maven Surefire (`pom.xml`)

* **`testng.xml`** (gốc dự án): khai báo `<suite name="V-SPORT Automation Test Suite">`, `<listeners>` trỏ tới `com.poly.sof309.ui.listeners.ScreenshotTestListener`, `<groups><run><include name="smoke"/><include name="regression"/></run></groups>` và 2 lớp test `AuthDataDrivenTestNGTest`, `ManagerCrudFlowTestNGTest`.
* **`pom.xml`**:
  * Bổ sung `org.testng:testng:7.10.2` (`scope=test`).
  * Giữ mặc định `mvn test` chạy bộ JUnit 5 (105 tests) để tương thích pipeline CI nhanh.
  * Thêm profile **`testng`** (`surefire-testng:3.2.5` + `<suiteXmlFile>testng.xml</suiteXmlFile>`) giúp chạy trọn bộ TestNG + Selenium bằng 1 lệnh:
    * Chạy có giao diện Chrome local: `mvn test -Ptestng`
    * Chạy Chrome Headless (local/CI): `mvn test -Ptestng -Dheadless=true`
    * Chỉ chạy nhóm Smoke: `mvn test -Ptestng -Dgroups=smoke -Dheadless=true`

---

## 4. Các Vấn đề Thực tế đã Phát hiện & Cách Xử lý (nhờ ảnh trong `screenshots/`)

Trong quá trình chạy thử trên Chrome Headless `158.0`, `ScreenshotTestListener` đã tự chụp ảnh lỗi và giúp chẩn đoán chính xác 4 vấn đề giao diện sau:
1. **Tab Đăng ký chỉ hiện trên Mobile (`AuthDataDrivenTestNGTest`):** Ảnh chụp cho thấy trên độ phân giải desktop `1440x900`, 2 cột Đăng nhập và Đăng ký hiển thị song song nên `.auth-tab-btn[data-target='registerCol']` bị ẩn (`display: none`). Đã sửa `RegisterPage.open()` chỉ bấm tab khi `#regName` chưa hiển thị.
2. **`ElementClickInterceptedException` tại checkbox `#agreeTerms`:** Checkbox nằm sát mép dưới khung nhìn khi trang có smooth-scroll. Đã cuộn tức thời (`behavior: 'instant'`) vào giữa màn hình và click an toàn qua `JavascriptExecutor`.
3. **Modal `#addModal` trên `KhoDichVu.jsp` chưa kịp phản hồi nhập liệu trong hiệu ứng `pop`:** Bổ sung hàm `fillInput(...)` trong `CrudPage` kiểm tra `getAttribute("value")` sau `sendKeys` và đồng bộ sự kiện `input`/`change` nếu trình duyệt headless bỏ sót phím gõ.
4. **Hiệu ứng fade-in CSS trên bảng kết quả tìm kiếm (`testStep4_SearchCreatedProduct`):** Ảnh chụp cho thấy `Tổng mặt hàng: 1` đã lọc đúng nhưng `table tbody` đang ở `opacity: 0` do animation `fadeUp`. Đã thêm `WebDriverWait` chờ URL chứa `search=` và `table tbody` hiển thị đầy đủ văn bản sản phẩm.

---

## 5. Kết quả Thực thi Cuối cùng (`mvn test -Ptestng -Dheadless=true -B`)

```text
[INFO] JUnit 5 Suite : Tests run: 105, Failures: 0, Errors: 0, Skipped: 1 (LoginTest @Disabled)
[INFO] TestNG Suite  : Tests run: 13,  Failures: 0, Errors: 0, Skipped: 0 (Time elapsed: 36.24 s)
[INFO] BUILD SUCCESS (Total time: 55.167 s)
```
