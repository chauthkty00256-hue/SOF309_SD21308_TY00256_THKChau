# Nhật ký AI: Thiết kế & Triển khai Unit Test Tầng Controller (Y3)

> **Dự án:** V-SPORT (`SOF309_SD21308_TY00256_THKChau`)  
> **Giai đoạn:** Tuần 2 — Mục tiêu Y3 (JUnit 5, `@ParameterizedTest` + `@CsvSource`, `assertThrows()`, `@Timeout`)  
> **Tài liệu tham chiếu:** [docs/SPECIFICATIONS.md](../SPECIFICATIONS.md) | [docs/TEST_PLAN.md](../TEST_PLAN.md) | [docs/TEST_CASES.md](../TEST_CASES.md)

---

## 1. Mục tiêu & Phạm vi Kiểm thử Unit Test Tầng Controller

### 1.1. Mục tiêu
* Kiểm thử tự động hóa logic xử lý request, validation dữ liệu Form đầu vào, kiểm soát phân quyền (RBAC), cô lập dữ liệu theo chi nhánh (`CoSoID`) và xử lý ngoại lệ nghiệp vụ ở tầng Controller (`HttpServlet`) cùng các Service cốt lõi của hệ thống **V-SPORT**.
* Áp dụng bắt buộc các kỹ thuật kiểm thử nâng cao bằng **JUnit 5**:
  * **`@ParameterizedTest` + `@CsvSource`** để bao phủ Phân vùng tương đương (EP) và Phân tích giá trị biên (BVA) cho dữ liệu Form (chuỗi rỗng, `null`, min/max độ dài, sai định dạng Email/SĐT VN, giá trị âm/bằng 0, vượt trần/dưới sàn).
  * **`assertThrows(...)`** để xác minh hệ thống ném đúng loại ngoại lệ nghiệp vụ (`IllegalArgumentException`, `ForbiddenException`, `NotFoundException`, `NumberFormatException`, `NullPointerException`).
  * **`@Timeout`** để đảm bảo hàm kiểm thử không rơi vào vòng lặp vô hạn hoặc treo tiến trình.
* Đảm bảo toàn bộ bộ test chạy độc lập bằng lệnh `mvn test` đạt **100% Pass (0 Failures, 0 Errors)** trên cả máy cục bộ lẫn GitHub Actions CI/CD mà không cần SQL Server thật và không làm thay đổi file `mock-data/vsport-mock-data.json` gốc.

### 1.2. Phạm vi kiểm thử
* **Bao gồm (In-Scope):** 4 tầng Controller chính của V-SPORT:
  1. Xác thực & OTP (`org.example.controller`)
  2. Khách hàng — Khuyến mãi, Đánh giá & Tính giá sân (`org.example.controller.customer`)
  3. Quản lý Chi nhánh — Kho dịch vụ, Sân, Khuyến mãi, Nhân sự & Ca làm (`org.example.controller.manager`)
  4. Quản trị Hệ thống — Nhân sự, Chi nhánh, PayOS & Tổng quan (`org.example.controller.admin`)
* **Ngoài phạm vi (Out-of-Scope):** Kiểm thử giao diện trình duyệt Selenium (thuộc Tuần 3–4, `LoginTest` giữ nguyên `@Disabled`), tích hợp cổng thanh toán PayOS/SMTP/Cloudinary thật.

---

## 2. Danh sách các Lớp Controller & Phương thức được Kiểm thử

Toàn bộ lớp test được đặt đúng cấu trúc package tương ứng trong `src/test/java/org/example/controller/...`:

| Lớp Unit Test | Package | Controller / Service được kiểm thử | Phương thức mục tiêu | Số ca test |
| :--- | :--- | :--- | :--- | :---: |
| `DangKyControllerTest` | `org.example.controller` | `DangKyServlet`, `XacThucOTPServlet`, `PhoneUtil` | `doPost(req, resp)`, `PhoneUtil.lookupVariants(...)` | 20 |
| `DangNhapControllerTest` | `org.example.controller` | `DangNhapServlet`, `PhoneUtil` | `doPost(req, resp)`, `doGet(req, resp)` | 17 |
| `ApDungKhuyenMaiControllerTest` | `org.example.controller.customer` | `ApDungKhuyenMaiServlet`, `CustomerDanhGiaServlet`, `DanhGiaService`, `CourtPricingService` | `doPost(req, resp)`, `submitReview(...)`, `calculate(...)` | 20 |
| `KhoDichVuManagerControllerTest` | `org.example.controller.manager` | `KhoDichVuManagerServlet`, `BranchSecurityUtils` | `doGet(req, resp)`, `doPost(req, resp)`, `checkBranchAccess(...)` | 14 |
| `QuanLySanAndKhuyenMaiControllerTest` | `org.example.controller.manager` | `KhuyenMaiManagerServlet`, `QuanLySanManagerServlet`, `SanService`, `NhanSuService`, `CaLamService` | `doPost`, `doGet`, `createSan`, `deleteSan`, `deleteLoaiSan`, `updateSanStatus`, `updateStaff`, `deleteStaff`, `createShift` | 15 |
| `QuanLyNguoiDungAdminControllerTest` | `org.example.controller.admin` | `QuanLyNguoiDungServlet`, `QuanLyChiNhanhServlet`, `PayOSConfigAdminServlet`, `AdminDashboardServlet` | `doGet(req, resp)`, `doPost(req, resp)` | 15 |
| *Tổng bộ Unit Test Controller mới* | — | **11 Servlet + 6 Service/Util** | — | **101** |

*(Cộng thêm 2 test có sẵn trong `com.poly.sof309.controllers.SampleControllerTest` = **103 Passed**, 1 Skipped `LoginTest` `@Disabled`.)*

---

## 3. Phân tích Nhóm Test Case (`@ParameterizedTest` + `@CsvSource` và `assertThrows()`)

### 3.1. Bảng các nhóm `@ParameterizedTest` + `@CsvSource` (Kiểm thử biên & Validation Form)

| Lớp Test | Phương thức Test | Số bộ dữ liệu (`@CsvSource`) | Các giá trị biên & điều kiện đầu vào đã kiểm thử |
| :--- | :--- | :---: | :--- |
| `DangKyControllerTest` | `testRegisterValidationBoundaries` | 15 | Mật khẩu `null`/không khớp/7 ký tự (`< 8`)/thiếu ký tự đặc biệt/thiếu chữ hoa/thiếu số; `agree=null`; `username` 2 ký tự (`< 3`); email thiếu `@`/có khoảng trắng; SĐT rỗng/sai đầu số `028...`; họ tên 1 ký tự (`< 2`); trùng SĐT biến thể `+84901000003`; trùng email `khach1@vsport.vn` |
| `DangNhapControllerTest` | `testLoginSuccessAllRoles` | 6 | Đăng nhập hợp lệ đủ 4 vai trò (`admin`, `manager1@vsport.vn`, `khach1`, `staff1`) + SĐT `0901000003` và `+84901000003` |
| `DangNhapControllerTest` | `testLoginValidationAndFailureCases` | 8 | `username` rỗng/`null`, `password` rỗng/chỉ khoảng trắng, SĐT rỗng/sai đầu số VN `0123456789`, tài khoản không tồn tại, sai mật khẩu |
| `ApDungKhuyenMaiControllerTest` | `testApplyPromotionBoundaries` | 10 | `VSPORT10` ở `500k` (10%), `1.5M` (chặn trần `100k`), `200k` (đúng biên tối thiểu), `199,999` (dưới biên); `WELCOME20` toàn hệ thống; `PICKLE50K` sai/đúng cơ sở; mã không tồn tại; mã rỗng; số tiền không phải số (`abc`) |
| `ApDungKhuyenMaiControllerTest` | `testCustomerReviewBoundaries` | 6 | `datSanId=0`/`-1`; `soSao=0`/`6` (ngoài `[1,5]`); `binhLuan=""`; đơn chưa hoàn thành (`datSanId=5`) |
| `KhoDichVuManagerControllerTest` | `testAddProductValidationBoundaries` | 7 | `tenSanPham` rỗng/`null`; trùng SKU `SP-NUOC-01`; `donGia=0`/`-5000`; `giaNhap=0`; `soLuongTon=-1` |
| `KhoDichVuManagerControllerTest` | `testStockInOutBoundaries` | 4 | Nhập kho `0`/`-5`; xuất kho `0`/`999999` (vượt tồn kho) |
| `QuanLySanAndKhuyenMaiControllerTest` | `testCreatePromotionValidationBoundaries` | 10 | Trùng `VSPORT10`; `loaiGiam=INVALID`; `giaTriGiam=0`/`-5`; `PERCENT=101%`; `end < start`; thiếu ngày bắt đầu; `soLanToiDa=0`; `giaTriToiThieu=-1`; `giamToiDa=-1` |
| `QuanLyNguoiDungAdminControllerTest` | `testAdminAddUserValidationBoundaries` | 7 | Email rỗng/có khoảng trắng/trùng `admin@vsport.vn`; SĐT sai đầu số; cấm tạo `RoleID=1`; Manager thiếu `CoSoID`; mật khẩu yếu `weak123` |
| `QuanLyNguoiDungAdminControllerTest` | `testPayOSConfigValidationBoundaries` | 5 | `coSoId="abc"`; `clientId` chứa ký tự che `••••••`; để trống `clientId`, `apiKey`, `checksumKey` |

### 3.2. Bảng các nhóm `assertThrows(...)` (Kiểm thử Ngoại lệ Nghiệp vụ & Dữ liệu sai)

| Lớp Test | Phương thức Test | Ngoại lệ kỳ vọng | Tình huống nghiệp vụ kích hoạt |
| :--- | :--- | :--- | :--- |
| `DangKyControllerTest` | `testAssertThrowsInvalidPhoneLookupVariants` | `IllegalArgumentException` | Gọi `PhoneUtil.lookupVariants("012345")` hoặc `null` (SĐT chưa chuẩn hóa) |
| `DangKyControllerTest` | `testAssertThrowsNullRequestOnOtpServlet` | `NullPointerException` | Gọi `XacThucOTPServlet.doPost(null, resp)` với request `null` |
| `DangNhapControllerTest` | `testAssertThrowsNullRequest` | `NullPointerException`, `IllegalArgumentException` | Gọi `DangNhapServlet.doPost(null, resp)` & `PhoneUtil.lookupVariants("invalid-phone")` |
| `ApDungKhuyenMaiControllerTest` | `testCourtPricingAssertThrowsOnInvalidArguments` | `IllegalArgumentException` | `CourtPricingService.calculate(...)` với `startAt=null`, `endAt < startAt`, hoặc giá sân âm (`-1`) |
| `KhoDichVuManagerControllerTest` | `testAssertThrowsBranchIsolationAndNullCoSo` | `ForbiddenException`, `IllegalArgumentException`, `NullPointerException` | Truy cập chéo cơ sở (`CoSo 2` từ Manager `CoSo 1`), `managerCoSoId=null`, và Manager chưa gắn `CoSoID` gọi `KhoDichVuManagerServlet.doGet` |
| `QuanLySanAndKhuyenMaiControllerTest` | `testSanServiceCreateExceptions` | `IllegalArgumentException` | Trùng tên sân `"Sân 5A - Cỏ FIFA"` trong `CoSo 1`; tên sân rỗng / trạng thái sai |
| `QuanLySanAndKhuyenMaiControllerTest` | `testSanServiceDeleteBusinessAndBranchExceptions` | `IllegalArgumentException`, `ForbiddenException` | Xóa Loại sân đang có sân liên kết (`LoaiSanID=1`); cập nhật trạng thái ngoài whitelist; Manager `CoSo 1` xóa sân thuộc `CoSo 2` (`SanID=6`) |
| `QuanLySanAndKhuyenMaiControllerTest` | `testNhanSuServicePrivilegeEscalationAndDeleteManagerExceptions` | `IllegalArgumentException` | Nâng quyền Lễ tân (`staff1`) lên Manager (`RoleID=2`); xóa tài khoản Manager (`manager1`) |
| `QuanLySanAndKhuyenMaiControllerTest` | `testCaLamServiceExceptions` | `IllegalArgumentException`, `NotFoundException` | Tạo ca với `request=null`; tạo ca cho `accountId=99999` không tồn tại |
| `QuanLyNguoiDungAdminControllerTest` | `testAssertThrowsNumberFormatExceptionOnInvalidIds` | `NumberFormatException` | `QuanLyNguoiDungServlet.doGet(action=delete, id="abc")` và `doPost(action=update, accountId="invalid")` |

---

## 4. Cấu hình Giả lập (Mock) & Các Phát hiện thực tế trong quá trình Chạy Test

### 4.1. Cấu hình Mock để test chạy độc lập 100%
* **Cô lập dữ liệu Mock JSON (`MockTestEnvironment.java`):** Trước khi khởi tạo DAO/Hibernate, lớp tiện ích sao chép `mock-data/vsport-mock-data.json` sang `target/test-mock-data/vsport-mock-data-test.json` và cấu hình `-Dvsport.mock.mode=true` cùng `-Dvsport.mock.file=...`. Nhờ đó mọi thao tác ghi (ví dụ tạo tài khoản đăng ký mới qua Mock OTP `123456`) chỉ tác động lên file tạm trong `target/`, giữ nguyên bản sạch của `mock-data/vsport-mock-data.json`.
* **Mock tầng Servlet bằng Mockito (`mockito-core:5.11.0`):** Giả lập `HttpServletRequest`, `HttpServletResponse` (ghi output vào `StringWriter`), `HttpSession` (backed bởi `HashMap<String, Object>` in-memory) và `RequestDispatcher` để gọi trực tiếp `doGet`/`doPost` mà không cần khởi động web container.
* **Cô lập IP & định danh Rate Limit:** Các ca test đăng nhập dùng IP giả lập riêng (`10.0.0.x`, `10.0.1.x`, `10.0.2.55`) và định danh động (`ratelimit_user_<nanoTime>`, `otp_lock_<nanoTime>@vsport.vn`) để tránh ảnh hưởng chéo giữa các ca test trên `SimpleRateLimiter.SHARED` và `OtpRateLimitUtil`.

### 4.2. Các lỗi / phát hiện thực tế từ quá trình chạy `mvn test` và cách xử lý
1. **Sai số lượng chữ số trong dữ liệu test SĐT `+84`:** Lần chạy đầu gặp lỗi ở `DangKyControllerTest` & `DangNhapControllerTest` do nhập `+8490100003` (thiếu 1 số `0` so với `0901000003` → chỉ có 8 số sau `+84`). Đã chỉnh đúng thành `+84901000003` → Pass.
2. **Phát hiện lệch trạng thái hoàn thành tại `DanhGiaDAOImpl.isBookingCompletedByCustomer` (Bug tiềm ẩn của hệ thống):** Câu SQL trong `DanhGiaDAOImpl` kiểm tra `TrangThai = N'Hoàn thành'`, trong khi dữ liệu đặt sân hoàn tất trong `vsport-mock-data.json` dùng `TrangThai = "Đã hoàn thành"`. Do đó gọi thật trên `datSanId=1` trả về `"Chỉ có thể đánh giá đơn đặt sân đã hoàn thành của bạn."`. Đã bổ sung thêm test với `Mockito.mock(DanhGiaDAO.class)` (`testDuplicateReviewRejectedWithMockedDao`) để xác minh riêng nhánh chặn đánh giá trùng lặp `"Bạn đã đánh giá đơn đặt sân này rồi."`.
3. **Phát hiện không tương thích SQL Server → H2 tại `SanService.deleteSan` (dòng 464):** Biểu thức `DATEDIFF(second, '00:00:00', GioKetThuc)` ném `SQLException` (`NumberFormatException: For input string: "00:0"`) trên H2 Mock Mode khiến `deleteSan` ném `RuntimeException`. Đã giữ nguyên mã nguồn gốc, ghi nhận lỗi tương thích H2 này vào nhật ký và kiểm thử ngoại lệ nghiệp vụ qua `deleteLoaiSan`, `updateSanStatus` và `deleteSan(6, 1)` (`ForbiddenException`).
4. **Phát hiện cơ chế điều hướng phân quyền ở `QuanLySanManagerServlet.doGet`:** Với người dùng không phải Manager (`RoleID != 2`), Servlet gọi `sendRedirect("/dangnhap")` thay vì `sendError(403)`; chỉ trả `403 Forbidden` khi Manager chưa được gắn `CoSoID`. Đã cập nhật assertion kiểm tra chính xác cả 2 nhánh.

---

## 5. Kết quả thực thi `mvn test`
```text
[INFO] Tests run: 2,  Failures: 0, Errors: 0, Skipped: 0 -- com.poly.sof309.controllers.SampleControllerTest
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1 -- com.poly.sof309.ui.tests.LoginTest (@Disabled - Tuần 3)
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- org.example.controller.admin.QuanLyNguoiDungAdminControllerTest
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0 -- org.example.controller.customer.ApDungKhuyenMaiControllerTest
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0 -- org.example.controller.DangKyControllerTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0 -- org.example.controller.DangNhapControllerTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- org.example.controller.manager.KhoDichVuManagerControllerTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- org.example.controller.manager.QuanLySanAndKhuyenMaiControllerTest
[INFO] Results: Tests run: 104, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```
