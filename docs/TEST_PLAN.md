# KẾ HOẠCH KIỂM THỬ (TEST PLAN)

* **Dự án:** V-SPORT — Hệ thống Quản lý & Đặt sân Thể thao Trực tuyến (Đa cơ sở, Đa vai trò)
* **Sinh viên:** THKChau — MSSV: `TY00256` — Lớp: `SD21308`
* **Môn học:** SOF309 — Kiểm thử Nâng cao cùng AI
* **Tài liệu đặc tả tham chiếu:** [docs/SPECIFICATIONS.md](./SPECIFICATIONS.md)
* **Bộ kịch bản kiểm thử:** [docs/TEST_CASES.md](./TEST_CASES.md)

---

## 1. MỤC TIÊU KIỂM THỬ
* Xác minh toàn bộ các chức năng cốt lõi của hệ thống **V-SPORT** hoạt động đúng theo quy tắc nghiệp vụ trong `docs/SPECIFICATIONS.md`, bao gồm: Xác thực/OTP, Quản lý tài khoản, Tìm kiếm/Bản đồ sân & Khuyến mãi, Đặt lịch sân/Giữ chỗ/Giỏ hàng, Hủy & Hoàn tiền, CRUD Quản lý Chi nhánh (Kho dịch vụ, Sân, Khuyến mãi), Vận hành Lễ tân và Quản trị Admin.
* Kiểm thử logic backend (Unit Test với **JUnit 5**: `@ParameterizedTest`, `@CsvSource`, `assertThrows`, `@Timeout`) cho các lớp Validation, Service và Controller.
* Kiểm thử giao diện tự động (Automation UI Test với **Selenium WebDriver 4** theo mô hình **Page Object Model — POM**, kết hợp TestNG `@DataProvider`, `groups`, `ITestListener` chụp ảnh lỗi).
* Xác nhận tính nhất quán của phân quyền đa vai trò (**Admin = 1**, **Manager = 2**, **Customer = 3**, **Staff = 4**) và giới hạn phạm vi dữ liệu theo từng chi nhánh (`CoSoID`).
* Tích hợp chạy kiểm thử tự động liên tục trên **GitHub Actions CI/CD** (`.github/workflows/test.yml`).

---

## 2. PHẠM VI KIỂM THỬ

### 2.1. Trong phạm vi (In-Scope)
* **Xác thực & Onboarding:** Đăng nhập (Username/Email/SĐT, Rate Limit 5 lần/5 phút, kiểm tra cơ sở hoạt động), Đăng xuất, Đăng ký Khách hàng kèm Mock OTP (`123456`), Quên mật khẩu, Đăng ký Chủ cơ sở 3 bước.
* **Tài khoản & Hồ sơ Khách hàng:** Cập nhật hồ sơ cá nhân, đổi Email kèm xác thực OTP, đổi Avatar, đổi mật khẩu, cập nhật thể chất/cá nhân hóa/ghi chú, lịch sử & phân loại điểm uy tín.
* **Khám phá, Đặt lịch & Giỏ hàng:** Tìm kiếm sân, API bản đồ Haversine, cửa hàng công khai & ẩn số tồn, kiểm tra 7 bước áp dụng mã khuyến mãi, tính giá động theo giờ bật đèn (`CourtPricingService`), giữ chỗ tạm 2 phút, tạo đơn đặt sân (tại quầy `sau` & luồng tạo đơn `payos`), giỏ hàng (`/customer/gio-hang`), thêm dịch vụ khi đơn `Đang sử dụng`.
* **Hủy đơn, Hoàn tiền & Tương tác:** Xem trước & xác nhận hủy đơn 4 bậc phí (`24h/12h/4h`), cập nhật thông tin ngân hàng hoàn tiền, đánh giá sân `1–5★`, ghép kèo cá nhân (ngưỡng điểm uy tín `≥ 60`).
* **Quản lý Chi nhánh (Manager — `RoleID=2`) — Trọng tâm CRUD:**
  * CRUD Kho dịch vụ/sản phẩm (`/manager/kho-dich-vu`), nhập/xuất kho, quản lý nhóm dịch vụ, xóa mềm & khôi phục từ Thùng rác.
  * CRUD Sân & Loại sân (`/manager/quan-ly-san`), ràng buộc trạng thái sân đang có lịch đặt.
  * CRUD Khuyến mãi (`/manager/khuyen-mai`), điều chỉnh điểm uy tín (`/manager/adjust-reputation`).
* **Quản trị Hệ thống (Admin — `RoleID=1`) & Lễ tân (Staff — `RoleID=4`):**
  * Thêm/xóa mềm chi nhánh, duyệt/từ chối Owner, thêm/sửa/khóa/xóa mềm nhân sự kèm OTP, cấu hình PayOS kèm OTP, kiểm tra phân quyền RBAC chéo vai trò.
  * Check-in đặt trước/vãng lai, chặn giảm trừ trả sân sớm đối với Lễ tân và khách đặt trước.

### 2.2. Ngoài phạm vi (Out-of-Scope)
* **Tích hợp dịch vụ bên thứ ba thật:** Thanh toán tiền thật qua cổng PayOS Webhook production, gửi email SMTP thật (Mock Mode tự dùng OTP `123456` thay vì gửi SMTP), upload/xóa ảnh thật trên Cloudinary production, render bản đồ MapTiler phụ thuộc API key ngoài.
* **SQL Server Production:** Không kiểm thử hiệu năng/migration trực tiếp trên Microsoft SQL Server thật; toàn bộ quá trình kiểm thử chạy trên **Môi trường Mock Data (H2 In-Memory + đồng bộ JSON `mock-data/vsport-mock-data.json`)** qua `SqlServerCompatDataSource` và `SqlServerSqlTranslator`.
* **Kiểm thử phi chức năng chuyên sâu:** Load/Stress/Performance Testing quy mô lớn, Penetration Testing chuyên sâu.

---

## 3. CHIẾN LƯỢC KIỂM THỬ

| Loại kiểm thử | Phương pháp & Kỹ thuật áp dụng | Công cụ / Phạm vi thực thi |
| :--- | :--- | :--- |
| **Functional Testing** (Kiểm thử chức năng) | Kiểm thử luồng chính (Happy Path) end-to-end cho từng vai trò: Đăng ký → OTP → Đăng nhập → Tìm sân → Đặt lịch → Giỏ hàng → Hủy/Hoàn tiền; Manager CRUD Kho/Sân/Khuyến mãi; Admin duyệt Owner & quản lý nhân sự. | Manual + Selenium POM + JUnit 5 |
| **Boundary Value Analysis (BVA) & Equivalence Partitioning (EP)** | Phân vùng tương đương & kiểm tra giá trị biên cho tất cả trường số/chuỗi/thời gian: mật khẩu (`7`/`8` ký tự), họ tên (`1`/`2`/`100`/`101`), chiều cao (`49`/`50`/`260`/`261`), thời lượng đặt sân (`29`/`30`/`240`/`241` phút), ngày đặt (`quá khứ`/`hôm nay`/`+30`/`+31` ngày), `donGia`/`giaNhap` (`0`/`1`), `soLuongTon` (`-1`/`0`), `% giảm` (`0`/`100`/`101`), điểm uy tín (`19`/`20`, `59`/`60`), bán kính bản đồ (`0`/`200`/`201` km), `soSao` (`0`/`1`/`5`/`6`). | JUnit 5 `@ParameterizedTest` + `@CsvSource` & Selenium Data-Driven |
| **Negative & Exception Testing** | Kiểm thử nhập liệu sai định dạng (email có khoảng trắng, SĐT không hợp lệ), trùng lặp dữ liệu (email, SĐT nhiều biến thể `0`/`+84`/`84`, mã SKU, tên sân, mã khuyến mãi), vượt giới hạn (`3 đơn/ngày`, `5 lần sai OTP`, `5 lần đăng nhập sai/5 phút`), thao tác sai trạng thái (đặt dịch vụ khi đơn chưa `Đang sử dụng`, xóa sân đang có lịch). | JUnit 5 `assertThrows()` + Servlet/UI Negative Test Cases |
| **UI & Cross-Flow Automation Testing** | Xây dựng kiến trúc Page Object Model (`LoginPage`, `RegisterPage`, `KhoDichVuPage`, `BookingPage`), sử dụng `WebDriverWait` (không dùng `Thread.sleep`), chạy được cả Chrome thường (local) và Chrome Headless (`--headless=new` trên GitHub Actions), tự động chụp ảnh màn hình vào `screenshots/` khi test fail. | Selenium WebDriver 4 + TestNG (`@DataProvider`, `groups`, `ITestListener`) |
| **Security & RBAC Testing** | Kiểm tra phân quyền truy cập chéo giữa Guest, Customer (`RoleID=3`), Staff (`RoleID=4`), Manager (`RoleID=2`) và Admin (`RoleID=1`); kiểm tra cô lập chi nhánh (`CoSoID=1` vs `CoSoID=2`); kiểm tra từ chối truy cập sau `/logout`. | Selenium + HTTP/Servlet Test |

---

## 4. TIÊU CHÍ VÀO / RA (ENTRY & EXIT CRITERIA)

### 4.1. Tiêu chí đầu vào (Entry Criteria)
1. Mã nguồn V-SPORT đã được tích hợp đầy đủ vào `src/main/` và `mock-data/vsport-mock-data.json`, file `pom.xml` đã hợp nhất đầy đủ thư viện.
2. Lệnh `mvn test-compile` biên dịch thành công (`BUILD SUCCESS`, 0 lỗi biên dịch).
3. Máy chủ cục bộ `LocalServerLauncher` khởi động thành công tại `http://localhost:8080/` ở chế độ Mock Mode (`-Dvsport.mock.mode=true`), không phát sinh exception trong `logs/vsport-app.log`.
4. Tài liệu `docs/SPECIFICATIONS.md` đã được rà soát và phê duyệt làm chuẩn đối chiếu.

### 4.2. Tiêu chí đầu ra (Exit Criteria)
1. **100%** các ca kiểm thử mức ưu tiên **High** (Smoke & luồng cốt lõi) đã được thực thi và đạt **Pass**.
2. Không còn lỗi nghiêm trọng mức **Critical / Blocker** mở chưa xử lý trong các luồng Đăng ký, Đăng nhập, Đặt sân và CRUD chính.
3. Toàn bộ bộ Unit Test (JUnit 5) và Automation UI Test (Selenium) chạy đạt **100% Pass** với lệnh `mvn clean test` (không có Failure hay Error).
4. Pipeline **GitHub Actions** (`.github/workflows/test.yml`) hiển thị trạng thái **Xanh (Success)** và tải được báo cáo `target/surefire-reports/` (và `screenshots/` nếu có lỗi) trên tab Artifacts.

### 4.3. Tiêu chí tạm dừng & tiếp tục (Suspension / Resumption Criteria)
* **Tạm dừng:** Máy chủ Embedded Tomcat không khởi động được trên cổng `8080`, file `mock-data/vsport-mock-data.json` bị hỏng cấu trúc JSON làm H2 không nạp được dữ liệu, hoặc lỗi chặn đăng nhập toàn hệ thống.
* **Tiếp tục:** Khôi phục bản gốc `mock-data/vsport-mock-data.json` qua Git (`git checkout -- mock-data/vsport-mock-data.json`), khởi động lại server và xác nhận `GET /` trả `200 OK`.

---

## 5. MÔI TRƯỜNG & DỮ LIỆU KIỂM THỬ

### 5.1. Môi trường kiểm thử (Test Environment)

| Thành phần | Môi trường Local | Môi trường CI/CD (GitHub Actions) |
| :--- | :--- | :--- |
| **Hệ điều hành** | Windows 11 | `ubuntu-latest` |
| **JDK & Build Tool** | JDK 17 + Apache Maven 3.10+ | Temurin JDK 17 + Maven |
| **Web Server** | Embedded Tomcat 10.1.31 (`LocalServerLauncher`, cổng `8080`) | Khởi chạy trong quá trình test / Unit test độc lập |
| **Cơ sở dữ liệu** | **H2 In-Memory (Mock Mode)** nạp từ `mock-data/vsport-mock-data.json` | H2 In-Memory (Mock Mode) |
| **Trình duyệt** | Google Chrome (GUI) | Google Chrome Headless (`--headless=new --no-sandbox --disable-dev-shm-usage`) |
| **Test Frameworks** | JUnit 5 (`5.10.2`), Selenium Java (`4.20.0`), Mockito (`5.11.0`), TestNG (Tuần 4) | Chạy tự động qua `mvn clean test` |

### 5.2. Dữ liệu kiểm thử chuẩn (khớp `mock-data/vsport-mock-data.json`)
* **Mật khẩu chung:** `123456` | **Mock OTP mặc định:** `123456`.
* **Tài khoản mẫu:**
  * Admin (Role 1): `admin` / `admin@vsport.vn` / `0901000001`
  * Manager CoSo 1 (Role 2): `manager1` / `manager1@vsport.vn` / `0901000002`
  * Manager CoSo 2 (Role 2): `manager2` / `manager2@vsport.vn` / `0901000005`
  * Staff CoSo 1 (Role 4): `staff1` / `staff1@vsport.vn` / `0901000004`
  * Customer 1 (Role 3, uy tín 96): `khach1` / `khach1@vsport.vn` / `0901000003`
  * Customer 2 (Role 3, uy tín 88): `khach2` / `khach2@vsport.vn` / `0901000006`
* **Cơ sở & Sân mẫu:** `CoSoID=1` (V-SPORT Arena Quận 1 — 5 sân, `Sân 5A` `300k/420k` đèn `17:30–23:00`), `CoSoID=2` (V-SPORT Center Thủ Đức — 3 sân), `CoSoID=3` (V-SPORT Club Vũng Tàu — 1 sân).
* **Mã khuyến mãi mẫu:** `VSPORT10` (CoSo 1, 10%, min `200k`, max `100k`), `PICKLE50K` (CoSo 2, `50k`, min `250k`), `WELCOME20` (Toàn hệ thống, 20%, min `150k`, max `80k`).

---

## 6. QUẢN LÝ RỦI RO & BIỆN PHÁP GIẢM THIỂU

| Mã rủi ro | Mô tả rủi ro (dựa trên mã nguồn thực tế) | Mức độ | Biện pháp giảm thiểu |
| :---: | :--- | :---: | :--- |
| **R-01** | **Biến đổi dữ liệu JSON sau mỗi lượt chạy test:** `MockDataStore` tự động ghi ngược mọi `INSERT/UPDATE/DELETE` từ H2 ra `mock-data/vsport-mock-data.json` sau 800ms, có thể làm sai lệch dữ liệu cho lượt test kế tiếp. | Cao | Sao lưu hoặc đặt lại `mock-data/vsport-mock-data.json` trước mỗi đợt chạy regression; với Unit Test dùng file tạm qua `-Dvsport.mock.file=...`. |
| **R-02** | **Trạng thái Rate Limiter lưu trong bộ nhớ JVM:** `SimpleRateLimiter.SHARED` (đăng nhập 5 lần/5p, quên MK 5 lần/15p) và `OtpRateLimitUtil` (khóa email 15p) giữ trạng thái trên RAM đến khi khởi động lại server. | Trung bình | Tách riêng tài khoản/định danh dùng cho các test case Negative Rate Limit (`rate_test@...`) để không làm khóa tài khoản mẫu `khach1`/`admin`. |
| **R-03** | **Khác biệt hành vi UI & Backend đã phát hiện trong mã nguồn:** (a) `?auth=register` chỉ mở `#authView` mà chưa tự chuyển tab `#registerCol`; (b) `DoiMatKhau.jsp` dùng `input[type="text"]` ẩn bằng CSS; (c) `DatSanServlet` chưa lưu `promoCode` từ `XacNhanDatSan.jsp`; (d) dịch vụ thuê trong mock-data có `giaNhap = 0` trái với rule `giaNhap > 0` khi sửa. | Trung bình | Ghi nhận rõ trong `SPECIFICATIONS.md` (Mục 5); cấu hình locator POM chính xác theo DOM thực tế và tách rõ test case Preview Khuyến mãi (`/api/promotion/apply`) với test case Tạo đơn (`/customer/dat-san`). |
| **R-04** | **Phụ thuộc dịch vụ ngoài (PayOS / Cloudinary / SMTP):** Khi chạy offline hoặc CI không có API key thật, các luồng gọi cổng thật sẽ trả lỗi cấu hình. | Trung bình | Tập trung kiểm thử luồng thanh toán tại quầy (`paymentMethod=sau`), Mock OTP (`123456`), validation phía server và mock các dependency ngoài bằng Mockito trong Unit Test. |
| **R-05** | **Phân quyền ở mức Servlet thay vì Filter tập trung:** Một số nhánh phụ (`QuanLyChiNhanhServlet`, `PayOSConfigAdminServlet`) có thể thiếu kiểm tra `roleId == 1`. | Cao | Bổ sung nhóm Test Case RBAC chuyên biệt (`TC_AUTH_11` – `TC_AUTH_15`) để phát hiện sớm và báo lỗi bảo mật nếu vượt quyền thành công. |

---

## 7. LỊCH TRÌNH & SẢN PHẨM BÀN GIAO (THEO LỘ TRÌNH SOF309)

| Giai đoạn | Nội dung thực hiện | Sản phẩm nghiệm thu |
| :--- | :--- | :--- |
| **Tuần 1 (Y1 & Y2)** | Gộp mã nguồn V-SPORT, chạy mượt `localhost:8080`, hoàn thiện đặc tả, Test Plan & bộ Test Cases bằng AI RAG. | `docs/SPECIFICATIONS.md`, `docs/TEST_PLAN.md`, `docs/TEST_CASES.md`, `docs/ai-logs/01_test_design_ai.md` |
| **Tuần 2 (Y3)** | Viết Unit Test JUnit 5 (`@ParameterizedTest`, `assertThrows`, `@Timeout`) cho Validation, Pricing, Promotion, Booking, KhoDichVu/San/KhuyenMai. | `src/test/java/com/poly/sof309/controllers/*.java`, `mvn test` 100% Pass |
| **Tuần 3–4 (Y4)** | Xây dựng Selenium POM (`LoginPage`, `RegisterPage`, `BookingPage`, `KhoDichVuPage`), nâng cấp TestNG (`groups`, `@DataProvider`, `ITestListener` chụp ảnh lỗi). | `src/test/java/com/poly/sof309/ui/**`, `testng.xml`, `screenshots/` |
| **Tuần 5–6 (Y5 & Y6)** | Chạy tự động trên GitHub Actions Chrome Headless, tải Artifacts báo cáo, hoàn thiện 6 file `docs/ai-logs/` & Slide bảo vệ. | GitHub Actions xanh, hồ sơ minh chứng AI & file nộp bài |
