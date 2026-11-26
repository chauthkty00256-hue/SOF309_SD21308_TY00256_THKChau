# ĐẶC TẢ NGHIỆP VỤ WEB CÁ NHÂN (SPECIFICATIONS)
> *Dùng làm ngữ cảnh (Context) gửi cho AI để sinh Test Plan & Test Case chính xác theo dự án riêng của bạn.*

---

## 1. THÔNG TIN DỰ ÁN
* **Tên đề tài:** V-SPORT — Hệ thống Quản lý & Đặt sân Thể thao Trực tuyến (Đa cơ sở, Đa vai trò)
* **Công nghệ:** Java 17, Jakarta Servlet 6.0 / JSP + JSTL 3.0, Hibernate ORM 6.4 (JPA 3.1) + HikariCP, BCrypt, Gson, ZXing, PayOS SDK, Cloudinary, Embedded Tomcat 10.1 (`LocalServerLauncher`), H2 In-Memory (chế độ Mock tự động đồng bộ JSON `mock-data/vsport-mock-data.json`) / SQL Server (`mssql-jdbc`)
* **Kiến trúc đóng gói:** Maven WAR (`com.poly:sof309-assignment`, tích hợp sẵn JUnit 5 & Selenium WebDriver 4)
* **URL chạy local:** `http://localhost:8080`
* **Cơ chế phiên & bảo mật:**
  * Session timeout `30 phút`, cookie `HttpOnly=true`, tracking mode `COOKIE` (`WEB-INF/web.xml`).
  * Filter duy nhất đang kích hoạt: `EncodingAndCacheControlFilter` (`/*`) ép UTF-8 và gắn header `no-cache, no-store, must-revalidate` cho các request không phải tài nguyên tĩnh.
  * Phân quyền (RBAC) kiểm tra trực tiếp trong từng Servlet theo `RoleID` trong session (`user`, `roleId`, `accountId`, `coSoId`).
* **Hệ thống vai trò (Roles):**

| RoleID | Tên vai trò | Trang chủ sau đăng nhập | Phạm vi chính |
| :---: | :--- | :--- | :--- |
| `1` | **Admin** | `/admin/tong-quan` | Quản trị toàn hệ thống, duyệt chủ cơ sở (Owner), quản lý chi nhánh, nhân sự, PayOS, thùng rác, Audit Log |
| `2` | **Quản lý (Manager / Owner)** | `/manager/dashboard` | Quản lý theo chi nhánh (`CoSoID`): sân & loại sân, kho dịch vụ/sản phẩm, khuyến mãi, nhân sự, ca làm, hóa đơn, hoàn tiền, mã QR sân, điểm uy tín khách |
| `3` | **Khách hàng (Customer)** | `/index.jsp` | Tìm kiếm/bản đồ sân, đặt lịch trực quan, thanh toán PayOS/tại quầy, giỏ hàng & lịch sử đặt sân, hủy & hoàn tiền, ghép kèo, đội nhóm, chia tiền nhóm, đánh giá, hồ sơ cá nhân |
| `4` | **Lễ tân (Staff)** | `/staff/dashboard` | Check-in đặt trước & khách vãng lai, mở/dừng ca chơi, thanh toán/tách hóa đơn, gia hạn sân, xác nhận tiền mặt chia nhóm, hoàn tiền, ca làm & đổi ca, xử lý yêu cầu QR tại sân |

---

## 2. QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

### 2.1. Nhóm Xác thực, OTP & Đăng ký Đối tác (Authentication & Onboarding)

#### A. Chức năng Đăng ký tài khoản Khách hàng (`POST /dangky` → `POST /nhapma`)
* **Giao diện:** Form `#registerForm` trong `index.jsp` (mở `#authView` qua `?auth=register` rồi chuyển tab `Đăng ký`), gửi AJAX `POST /dangky`.
* **Trường dữ liệu đầu vào & ràng buộc validation (theo đúng thứ tự kiểm tra backend trong `DangKyServlet`):**

| Trường (`param` / `id`) | Bắt buộc | Ràng buộc dữ liệu (Backend) | Thông báo lỗi chính xác |
| :--- | :---: | :--- | :--- |
| `password` (`#regPassword`) | Có | Khớp `confirm_password`; regex mạnh: `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$` (≥ 8 ký tự, gồm chữ thường, chữ hoa, số, ký tự đặc biệt) | Không khớp: `"Mật khẩu không trùng khớp!"`<br>Yếu: `"Mật khẩu phải có tối thiểu 8 ký tự, bao gồm cả chữ hoa, chữ thường, số và ký tự đặc biệt."` |
| `confirm_password` (`#regConfirmPassword`) | Có | Bằng `password` | `"Mật khẩu không trùng khớp!"` |
| `agree` (`#agreeTerms`) | Có | Phải gửi giá trị `"Đồng ý"` | `"Bạn chưa đồng ý với điều khoản!"` |
| `username` | Tùy chọn | Nếu có: `^[A-Za-z0-9_]{3,50}$`, không trùng DB. Nếu bỏ trống: hệ thống tự sinh từ phần trước `@` của email (3–30 ký tự + hậu tố ngẫu nhiên) | Sai định dạng: `"Tên đăng nhập không hợp lệ (3-50 ký tự, không chứa khoảng trắng)!"`<br>Trùng: `"Tên đăng nhập đã tồn tại!"` |
| `email` (`#regEmail`) | Có | Regex `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`, không khoảng trắng, không trùng DB | Sai định dạng: `"Email không hợp lệ và không được chứa khoảng trắng!"`<br>Trùng: `"Email đã tồn tại!"` |
| `phone` (`#regPhone`) | Có | Chuẩn hóa VN qua `PhoneUtil.normalizeVN` (nhận `0`, `+84`, `84` đầu số `3/5/7/8/9`, 10 số dạng `0XXXXXXXXX`), không trùng bất kỳ biến thể nào (`0...`, `+84...`, `84...`) | Sai định dạng: `"Số điện thoại không hợp lệ. Vui lòng nhập số di động Việt Nam (0, +84 hoặc 84)."`<br>Trùng: `"Số điện thoại đã được đăng ký. Vui lòng dùng số khác hoặc đăng nhập."` |
| `fullname` (`#regName`) | Có | Sau `.trim()`: độ dài `2 – 100` ký tự | `"Vui lòng nhập họ và tên hợp lệ (2-100 ký tự)."` |

* **Luồng xác thực OTP đăng ký (`POST /nhapma` & `GET/POST /resend-otp`):**
  * Sau khi vượt qua validation, mật khẩu được băm bằng `BCrypt (gensalt 12)`, thông tin tạm lưu vào session (`tempAccount`, `otp`, `authType="REGISTER"`), và gửi mã OTP 6 chữ số đến email.
  * **Chế độ Mock Mode (`vsport.mock.mode=true`):** Mã OTP mặc định luôn là **`123456`** (`MockConfig.DEFAULT_MOCK_OTP`).
  * Nhập đúng OTP → tạo tài khoản `RoleID=3`, `DiemUyTin=100`, `DiemTrinhDo=1000`, `NhanThongBaoSos=true`, `IsLocked=false`; trả về `"Đăng ký thành công! Vui lòng đăng nhập với tài khoản của bạn."` và chuyển hướng `/index.jsp?auth=login`.
  * Nhập sai OTP (luồng `REGISTER`): trả về `"Mã xác thực không chính xác hoặc đã hết hạn."`. Mất session: `"Phiên làm việc đã hết hạn. Vui lòng đăng ký lại."`.
  * Gửi lại OTP (`/resend-otp`): tối đa **5 lần/phiên** (`"Bạn đã vượt quá số lần gửi lại mã OTP tối đa (5/5 lần)."`), phía client có cooldown **60 giây**.
  * *Lưu ý riêng cho luồng OTP nội bộ (`ADMIN_ADD`, `ADMIN_EDIT`, `MANAGER_ADD`, `MANAGER_EDIT`):* Nhập sai 1–4 lần báo `"Mã xác thực không chính xác. Bạn còn {5 - attempts} lần thử."`; sai lần thứ **5** khóa tạm email **15 phút** (`OtpRateLimitUtil`): `"Bạn đã nhập sai mã OTP 5 lần! Email ({email}) đã bị tạm khóa trong 15 phút."`.

#### B. Chức năng Đăng nhập & Đăng xuất (`POST /dangnhap`, `POST /he-thong/dang-nhap`, `GET/POST /logout`)
* **Giao diện:** Form `#loginForm` trong `index.jsp` (`#loginEmail`, `#loginPassword`, `#loginSubmitBtn`). JavaScript tự nhận diện nếu đầu vào khớp `/^(0|\+84|84)[0-9]{8,9}$/` thì gửi `loginMethod=phone`, ngược lại gửi `loginMethod=account` (username hoặc email).
* **Quy tắc nghiệp vụ & ràng buộc:**
  * Trống định danh hoặc mật khẩu:
    * Chế độ SĐT: `"Số điện thoại và mật khẩu không được để trống!"`
    * Chế độ tài khoản: `"Tài khoản và mật khẩu không được để trống!"`
  * Sai định dạng SĐT VN (`loginMethod=phone`): `"Số điện thoại không hợp lệ. Vui lòng nhập số di động Việt Nam (0, +84 hoặc 84)."`
  * **Giới hạn tần suất (Rate Limit):** Cửa sổ trượt **5 phút** — tối đa **5 lần/định danh** và **20 lần/IP** (`SimpleRateLimiter`). Vượt ngưỡng hoặc sai tài khoản/mật khẩu/tài khoản bị khóa (`IsLocked=true`) hoặc bị xóa mềm (`IsDeleted=true`) đều trả thông báo chung (chống dò tài khoản): `"Email, số điện thoại hoặc mật khẩu không chính xác."`.
  * **Kiểm tra trạng thái cơ sở:** Nếu tài khoản có `CoSoID` nhưng cơ sở không còn hoạt động (`!isFacilityActive`) → chặn đăng nhập với mã `FACILITY_INACTIVE`: `"Cơ sở của tài khoản này đã ngừng hoạt động. Vui lòng liên hệ quản trị viên."`.
  * **Đăng nhập thành công:** Hủy session cũ & tạo session mới (chống Session Fixation), chuyển hướng theo `RoleID` (riêng `ROLE_CUSTOMER = 3` tôn trọng tham số `redirect` nội bộ hợp lệ bắt đầu bằng `/` và không bắt đầu bằng `//`).
  * **Đăng xuất (`/logout`):** Hủy session, xóa cookie `JSESSIONID` (`Max-Age=0`), chuyển hướng `/he-thong/dang-nhap` (vai trò nội bộ) hoặc `/dangnhap` (khách hàng).

#### C. Chức năng Quên mật khẩu (`POST /quenmatkhau`, `POST /he-thong/quen-mat-khau`)
* **Luồng đơn bước (không qua OTP):** Hệ thống sinh mật khẩu ngẫu nhiên 10 ký tự đạt chuẩn mạnh, cập nhật thẳng hash BCrypt vào DB và gửi qua email.
* **Ràng buộc & lỗi:**
  * `method=email` sai định dạng: `"Email không hợp lệ. Vui lòng kiểm tra lại."`
  * `method=phone` (chỉ cổng customer) sai SĐT VN: `"Số điện thoại không hợp lệ. Vui lòng nhập số di động Việt Nam (0, +84 hoặc 84)."`
  * **Rate Limit (15 phút):** Tối đa **5 lần/định danh**, **12 lần/IP** → `"Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau ít phút."`.
  * Tài khoản không tồn tại / bị khóa / chưa có email hợp lệ: `"Không thể gửi mã đến email này. Vui lòng kiểm tra lại thông tin."` (hoặc `"...với số điện thoại này..."`).
  * Thành công: `"Mật khẩu mới đã được gửi đến email của bạn. Vui lòng đăng nhập bằng mật khẩu mới."`.

#### D. Chức năng Đăng ký làm Chủ cơ sở / Đối tác (`GET/POST /owner/register`, `/owner/send-otp`, `/owner/verify-otp`, `/owner/otp-status`)
* **Luồng 3 bước tại `ownerLanding.jsp`:**
  1. **Bước 1 — Thông tin liên hệ & gửi OTP (`/owner/send-otp`):** `ownerName`, `email`, `phone` (regex `^(0|\+84)[35789][0-9]{8}$`), `address`, tọa độ `viDo`/`kinhDo`. Email đã tồn tại bị chặn (`"Email đã tồn tại trong hệ thống."`), ngoại trừ trường hợp chủ cơ sở từng bị `Từ chối` được phép đăng ký lại.
  2. **Bước 2 — Xác thực OTP (`/owner/verify-otp`):** OTP có thời hạn **5 phút** (Mock Mode chấp nhận `123456`). Quá hạn: `"Mã OTP đã hết hạn. Vui lòng gửi lại."`; sai mã: `"Mã OTP không đúng."`.
  3. **Bước 3 — Cấu hình cơ sở & nộp hồ sơ (`/owner/register`):** Bắt buộc đã xác thực email (`"Email chưa được xác thực."`), kiểm tra cặp tọa độ (`viDo ∈ [-90, 90]`, `kinhDo ∈ [-180, 180]`), khung giờ mở/đóng cửa, ngày hoạt động, danh sách môn thể thao + số lượng sân (≥ 1), dịch vụ bổ sung (`capabilities`). Hệ thống tạo `CoSo` trạng thái **`Chờ duyệt`** và tài khoản `RoleID=2` ở trạng thái **`IsLocked=true`** chờ Admin phê duyệt.

---

### 2.2. Nhóm Quản lý Tài khoản & Hồ sơ Cá nhân

#### A. Cập nhật hồ sơ, đổi Email (kèm OTP), đổi Avatar & Đổi mật khẩu (`POST /account/update-profile` & `/admin|/manager|/staff/update-profile`)
* **`action=updateAvatar`:** File `avatar` (multipart tối đa **2MB**), chỉ nhận `image/jpeg`, `image/png`, `image/webp`, `image/gif` (`"Chỉ hỗ trợ ảnh JPG, PNG, WEBP hoặc GIF."`).
* **`action=updateInfo`:**
  * `fullName`: bắt buộc, `2–100` ký tự (`"Họ và tên không được để trống."` / `"Họ và tên phải từ 2 đến 100 ký tự."`).
  * `email`: hợp lệ, không khoảng trắng, không trùng tài khoản khác (`"Email đã được sử dụng bởi tài khoản khác."`). Nếu thay đổi email → phát sinh OTP 6 số (hạn **5 phút**, Mock Mode: `123456`) và yêu cầu xác nhận qua `action=verifyEmailOtp` trước khi lưu.
  * `phoneNumber`: bắt buộc, khớp `^(0|\+84)[35789][0-9]{8}$` (`"Số điện thoại không hợp lệ."`).
  * `birthday`: định dạng `yyyy-MM-dd`, không ở tương lai (`"Ngày sinh không được ở tương lai."`).
  * `gender`: thuộc tập `{Nam, Nữ, Khác}` hoặc rỗng (`"Giới tính không hợp lệ."`).
* **`action=changePassword` (giao diện `/customer/doi-mat-khau` → `DoiMatKhau.jsp`):**
  * Không được để trống (`"Mật khẩu không được để trống hoặc chỉ chứa khoảng trắng!"`).
  * `newPassword` phải đạt chuẩn mật khẩu mạnh (`"Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ hoa, chữ thường, số và ký tự đặc biệt."`).
  * `confirmPassword` phải khớp (`"Xác nhận mật khẩu mới không khớp."`).
  * Kiểm tra `currentPassword` đúng BCrypt (`"Mật khẩu hiện tại không chính xác!"`) và `newPassword` không trùng `currentPassword` (`"Mật khẩu mới không được trùng với mật khẩu hiện tại."`).
  * Thành công: `"Đổi mật khẩu tài khoản thành công."`.

#### B. Hồ sơ thể thao, Cài đặt thông báo, Điểm uy tín & Xóa tài khoản Khách hàng
* **Tổng quan tài khoản (`/customer/tai-khoan`):** Chỉ `RoleID=3`. Hiển thị thống kê đặt sân & tối đa 5 lịch sắp tới. Hỗ trợ `POST action=deleteAccount` (yêu cầu đúng mật khẩu hiện tại, thực hiện xóa mềm + khóa tài khoản + đăng xuất).
* **Hồ sơ mở rộng (`/customer/ho-so`):**
  * Thể chất (`/customer/ho-so/cap-nhat-the-chat`): `heightCm ∈ [50, 260]` (`"Chiều cao phải trong khoảng 50-260cm."`), `weightKg ∈ [20, 300]` (`"Cân nặng phải trong khoảng 20-300kg."`).
  * Ghi chú (`/customer/ho-so/cap-nhat-ghi-chu`): tối đa **500 ký tự** (`"Ghi chú tối đa 500 ký tự."`).
  * Cá nhân hóa (`/customer/ho-so/cap-nhat-ca-nhan-hoa`): `location` ≤ 255, `goal` ≤ 255, `level` ∈ `{Mới chơi, Cơ bản, Trung bình, Khá, Nâng cao}`, `frequency` ∈ `{1 lần/tuần, 2-3 lần/tuần, 4+ lần/tuần, Không cố định}`.
  * Ảnh bìa (`/customer/ho-so/doi-cover`): tối đa **5MB**, chỉ `JPG/PNG/WEBP` + kiểm tra magic bytes.
* **Cài đặt thông báo (`/customer/notification-settings`):** Bật/tắt `nhanThongBaoMarketing` (PRG redirect `?saved=1`).
* **Lịch sử điểm uy tín (`/customer/lich-su-diem-uy-tin`):** Lọc theo `type` (`ALL`, `CANCEL_6_TO_24`, `LATE_CANCEL`, `NO_SHOW`, `COMPLETED_BOOKING`, `MANUAL_ADJUST`). Phân loại nhãn: `≥ 80` (*Uy tín tốt*), `60–79` (*Cần lưu ý*), `30–59` (*Rủi ro cao*), `< 30` (*Rủi ro rất cao*). Ngưỡng ghép kèo: `≥ 60`; ngưỡng bị chặn đặt sân: `< 20`.

---

### 2.3. Nhóm Danh sách Sân, Sản phẩm/Dịch vụ, Bản đồ & Khuyến mãi (Public / Customer)

* **Tìm kiếm cơ sở & sân (`GET /customer/tim-kiem`):** Tham số `q` (cắt tối đa 120 ký tự), `sportId` (mặc định môn yêu thích của user nếu đã đăng nhập), `coSoId`, `openNow` (`true`/`1` lọc theo `GioMoCua..GioDongCua`). Đính kèm 1 khuyến mãi công khai mới nhất cho mỗi cơ sở.
* **Bản đồ cơ sở (`GET /customer/ban-do`, `GET /api/customer/facilities/map`):** Trả cơ sở `Đang hoạt động` có tọa độ hợp lệ. `lat ∈ [-90, 90]` & `lng ∈ [-180, 180]` phải đi theo cặp; `radiusKm ∈ (0, 200]` (tính khoảng cách Haversine, sắp xếp tăng dần).
* **Chi tiết cơ sở & Cửa hàng sản phẩm công khai:**
  * `GET /api/customer/facilities/detail?coSoId=...`: Trả danh sách sân, tối đa 30 dịch vụ còn tồn (`SoLuongTon > 0`), thư viện ảnh, `shopAvailable` và `activePromotions`.
  * `GET /api/customer/facilities/shop?coSoId=...` & `GET /api/customer/san-pham`: Chỉ mở nếu cơ sở đã được duyệt capability shop (`SAN_PHAM`/`THUE_DUNG_CU`/`DO_AN_NUOC_UONG`). Giới hạn 200 sản phẩm; **ẩn số lượng tồn chính xác**, chỉ trả nhãn: `≤ 0` → `HET_HANG`, `1–5` → `SAP_HET`, `> 5` → `CON_HANG`.
* **Trang Ưu đãi (`GET /customer/uu-dai`, `GET /api/customer/promotions`) & Áp dụng mã khuyến mãi (`POST /api/promotion/apply`):**
  * Đầu vào: `code` (uppercase), `originalAmount`, `coSoId` (tùy chọn), `bookingDate` (mặc định hôm nay).
  * Kiểm tra theo 7 bước:
    1. Tồn tại mã → `"Mã khuyến mãi '<code>' không tồn tại."`
    2. Trạng thái `Hoạt động` / `ACTIVE` → `"Mã khuyến mãi hiện không khả dụng."`
    3. Trong khoảng `[NgayBatDau, NgayKetThuc]` → `"Mã khuyến mãi chưa đến thời gian áp dụng."` / `"Mã khuyến mãi đã hết hạn sử dụng."`
    4. Đúng cơ sở (nếu mã gắn `CoSoID`) → `"Mã khuyến mãi không áp dụng cho cơ sở này."`
    5. Còn lượt toàn hệ thống (`SoLanDaDung < SoLanToiDa`) → `"Mã khuyến mãi đã hết tổng lượt sử dụng trên hệ thống."`
    6. Đạt giá trị tối thiểu (`originalAmount >= GiaTriToiThieu`) → `"Đơn hàng phải từ <X> VNĐ mới được áp dụng mã này."`
    7. Giới hạn mỗi khách (`maxUsagePerUser = 1` nếu đăng nhập) → `"Bạn đã sử dụng hết số lần cho phép của mã khuyến mãi này."`
  * Công thức giảm giá: `PERCENT`/`PHAN_TRAM` tính `%` của `originalAmount` (làm tròn `HALF_UP` đến đơn vị VND); `FIXED` trừ thẳng `GiaTriGiam`; chặn trần bởi `GiamToiDa` và `originalAmount`. Thành công: `"Áp dụng mã khuyến mãi thành công! Bạn được giảm <X> VNĐ."`.

---

### 2.4. Nhóm Đặt lịch Sân, Giữ chỗ tạm, Thanh toán PayOS & Giỏ hàng

#### A. Quy tắc tính giá & ràng buộc khung giờ (`CourtPricingService`, `VisualBookingServlet`, `DatSanServlet`)
* **Độ dài ca đặt:** Tối thiểu **30 phút**, tối đa **240 phút (4 giờ)** (`"Thời lượng tối thiểu là 30 phút."`, `"Thời lượng tối đa là 4 giờ (240 phút)."`). `gioKetThuc` phải lớn hơn `gioBatDau` (`"Giờ kết thúc phải sau giờ bắt đầu."`).
* **Cửa sổ ngày đặt:** Không được đặt ngày/giờ quá khứ; đặt trước tối đa **+30 ngày**.
* **Giờ hoạt động cơ sở:** Nằm trong `[GioMoCua, GioDongCua]` (mặc định `06:00 – 23:00` nếu chưa cấu hình).
* **Giới hạn đặt sân/ngày:** Tối đa **3 đơn đang hoạt động/ngày/tài khoản** (`Chờ xác nhận`, `Đã xác nhận`, `Đang sử dụng`) → vượt giới hạn trả mã `BOOKING_LIMIT_REACHED`: `"Bạn đã đạt giới hạn đặt sân tối đa trong ngày hôm nay (tối đa 3 lượt đặt/ngày)."`.
* **Chặn điểm uy tín thấp:** Tài khoản có `DiemUyTin < 20` bị chặn đặt sân (`REPUTATION_BLOCKED`).
* **Tính giá động theo giờ bật đèn (`CourtPricingService`):** Tự động tách khung giờ qua mốc `GioBatDauLenDen`..`GioKetThucLenDen` (hỗ trợ cả ca qua đêm), mỗi phân đoạn tính `hourlyRate * minutes / 60` làm tròn `HALF_UP` (0 chữ số thập phân).

#### B. Luồng Đặt lịch trực quan → Giữ chỗ → Xác nhận → Thanh toán
1. **Xem lưới giờ trống (`GET /customer/dat-lich-truc-quan`, `/customer/api/timetable-availability`, `/customer/api/timetable-price`):** Hiển thị các ô 30 phút với trạng thái `AVAILABLE`, `LOCKED`, `PAST`, `BOOKED`, `HOLD_SELF`, `HOLD`.
2. **Giữ chỗ tạm (`POST /customer/giu-cho-tam`):** Khóa bi quan `UPDLOCK, ROWLOCK` trên sân, giữ chỗ **2 phút (`120 giây`)**; báo `SLOT_ALREADY_RESERVED` nếu có người khác đang giữ hoặc đã đặt trùng.
3. **Xác nhận & Tạo đơn (`GET/POST /customer/dat-lich-truc-quan/xac-nhan` → `POST /customer/dat-san`):**
   * Đầu vào: `sanId`, `ngayDat`, `gioBatDau`, `gioKetThuc`, `paymentMethod` (`payos` | `sau`), `ghiChu` (≤ 255 ký tự), tùy chọn `serviceId[]` & `serviceQty[]` (> 0, ≤ tồn kho, thuộc cùng cơ sở).
   * **Thanh toán tại quầy (`paymentMethod=sau`):** Tạo `LichDatSan` trạng thái **`Chờ xác nhận`** (tự động hủy sau **2 giờ** nếu cơ sở chưa duyệt), chuyển hướng `/customer/gio-hang` kèm thông báo `"Đặt sân thành công! Sân đã được thêm vào giỏ hàng. Vui lòng chờ cơ sở xác nhận."`.
   * **Thanh toán trực tuyến (`paymentMethod=payos`):** Tạo `LichDatSan` trạng thái **`Chờ thanh toán`** kèm `HoldExpiresAt = NOW + 10 phút`, gọi PayOS tạo mã VietQR và chuyển sang `/customer/thanh-toan-qr?datSanId=...` (render ảnh QR tại `/customer/qr-image`, polling `/customer/payos-status`, cho phép tạo lại link `/customer/payos-retry`, chuyển sang trả tại quầy `/customer/payos-pay-counter`, hoặc hủy `/customer/payos-cancel`).

#### C. Giỏ hàng, Lịch sử đặt sân & Đặt thêm dịch vụ (`/customer/gio-hang`, `/customer/lich-su-dat-san`, `/customer/dat-dich-vu`)
* **Bản chất Giỏ hàng trong V-SPORT:** Không có giỏ hàng thương mại điện tử tách biệt; `/customer/gio-hang` hiển thị danh sách **đơn đặt sân chờ xử lý** (`Chờ xác nhận`, `Chờ thanh toán` — số lượng badge lấy từ `GET /customer/api/cart-count`), kết hợp tab **Lịch sử đặt sân** và tab **Hoàn tiền** (`?tab=refund`).
* **Thêm/cập nhật dịch vụ kèm đơn đặt sân (`GET/POST /customer/dat-dich-vu`):**
  * Chỉ cho phép khi đơn đặt sân đang ở trạng thái **`Đang sử dụng`** (`"Chỉ được phép thêm/cập nhật dịch vụ khi đơn đặt sân ở trạng thái 'Đang sử dụng'."`) và hóa đơn `MAIN` chưa thanh toán.
  * Tự động hoàn trả tồn kho cũ, khóa dòng `SanPham_DichVu`, kiểm tra `stock >= qty`, trừ tồn kho mới và cập nhật `ChiTietHoaDon`.

---

### 2.5. Nhóm Hủy đơn, Hoàn tiền, Đánh giá, Ghép kèo, Đội nhóm, Chia tiền & QR tại sân

* **Hủy đặt sân & Khởi tạo hoàn tiền (`GET /customer/refund-cancel/preview`, `POST /customer/refund-cancel/confirm`):**
  * Bậc phí hủy áp dụng trên giao diện `GioHang.jsp`:
    * Hủy trước `≥ 24 giờ`: **miễn phí (hoàn 100%)**.
    * Hủy trước `12 – < 24 giờ`: **phí 30% (hoàn 70%)**.
    * Hủy trước `4 – < 12 giờ`: **phí 50% (hoàn 50%)**.
    * Hủy `< 4 giờ` hoặc đã quá giờ bắt đầu: **phí 100% (hoàn 0%)**.
  * Nếu đơn đã thanh toán & còn tiền hoàn > 0 → tạo bản ghi `HoanTien` ở trạng thái `CHO_BO_SUNG_THONG_TIN` và chuyển khách tới `/customer/hoan-tien?id=...`.
  * *(Lưu ý: `POST /customer/huy-dat-san` đã bị khai tử và trả `410 Gone`; lớp `BookingCancellationService` tồn tại song song với chính sách 3 bậc 24h / 6h kèm trừ điểm uy tín `-5` / `-10` phục vụ kiểm thử đơn vị/API).*
* **Quản lý hoàn tiền phía Khách hàng (`GET/POST /customer/hoan-tien`):**
  * Vòng đời trạng thái: `CHO_BO_SUNG_THONG_TIN` ↔ `CHO_XU_LY` → `DA_DUYET` → `DANG_HOAN_TIEN` → `DA_HOAN_TIEN` (hoặc `TU_CHOI` / `DA_HUY`).
  * Khách bổ sung thông tin ngân hàng (`action=update-bank`: bắt buộc đủ `nganHang`, `soTaiKhoan`, `chuTaiKhoan`) và tùy chọn ảnh QR (`qrImage`, JPG/PNG/WEBP ≤ 5MB) hoặc tự hủy yêu cầu (`action=cancel`) khi đang ở `CHO_BO_SUNG_THONG_TIN` / `CHO_XU_LY`.
* **Đánh giá sân (`POST /customer/danh-gia-san`):** `RoleID=3`, chỉ đánh giá đơn `Đã hoàn thành` của chính mình và chỉ đánh giá 1 lần (`"Bạn đã đánh giá đơn đặt sân này rồi."`). `soSao ∈ [1, 5]` (`"Số sao phải từ 1 đến 5."`), `binhLuan` bắt buộc, tối đa **255 ký tự** (`"Bình luận không được để trống."`, `"Bình luận tối đa 255 ký tự."`).
* **Ghép kèo cá nhân (`/customer/ghep-keo`, `/customer/api/matches/*`):** Yêu cầu `DiemUyTin ≥ 60` cho cả chủ kèo và người tham gia; đơn đặt sân phải `Đã xác nhận` và ngày ≥ hôm nay; `soNguoiCanTim ∈ [1, 20]`, `hinhThucDuyet ∈ {auto, manual}`, `minReputation ∈ [0, 100]`, `note ≤ 240` ký tự.
* **Đội nhóm & Thách đấu đội (`/customer/doi-nhom/*`, `/customer/api/teams/*`, `/customer/api/team-matches/*`):** `teamName` `3–50` ký tự (duy nhất theo đội trưởng), `description ≤ 225`, `locationText ≤ 255`, `maxMembers ∈ [2, 30]` (khi sửa `≥ số thành viên hiện tại`), cấm ký tự `<` và `>`, ảnh ≤ 5MB (JPG/PNG/WEBP/GIF).
* **Chia tiền nhóm (`/customer/chia-tien-nhom`, `/chia-tien/thanh-toan?token=...`, `/chia-tien/qr?token=...`):** `splitType ∈ {EQUAL, CUSTOM}`, `participantCount ∈ [2, 50]`; chỉ tạo khi đơn đã kết thúc và hóa đơn `MAIN` chưa thanh toán. Chế độ `CUSTOM` bắt buộc mỗi phần > 0 và tổng khớp chính xác `HoaDon.TongThanhToan`. Mỗi người nhận link token 256-bit để thanh toán tiền mặt tại quầy (`/staff/chia-tien-xac-nhan`).
* **Quét mã QR tại sân (`GET /qr/{shortCode}`, `GET /api/qr/san-pham`, `POST /api/qr/yeu-cau`, `GET /api/qr/yeu-cau-status`):** Không cần đăng nhập (dùng `guestToken`). Hỗ trợ `CALL_STAFF`, `ORDER_ITEM`, `SERVICE_REQUEST`, `PAYMENT_REQUEST` (chỉ nhận `note` là `"Tiền mặt"` hoặc `"Chuyển khoản"`). Chống spam: chặn gửi trùng `guestToken + sanId + requestType` trong vòng **20 giây** hoặc khi đang `IN_PROGRESS` (`409 Conflict`).

---

### 2.6. Nhóm Quản trị Hệ thống (Admin — `RoleID = 1`)

| Chức năng | Endpoint | Nghiệp vụ CRUD & Ràng buộc chính | Thông báo lỗi tiêu biểu |
| :--- | :--- | :--- | :--- |
| **Tổng quan** | `GET /admin/tong-quan` | Thống kê tổng tài khoản theo vai trò, tổng chi nhánh & chi nhánh hoạt động, 5 tài khoản mới nhất | Redirect `/dangnhap` nếu không phải Role 1 |
| **Quản lý Chi nhánh** | `GET/POST /admin/chi-nhanh`<br>`POST /admin/chi-nhanh/them`<br>`GET/POST /admin/chi-nhanh/sua`<br>`GET /admin/chi-nhanh/xoa` | • **Thêm chi nhánh:** `tenCoSo`, `diaChi`, `soDienThoai`, `gioMoCua`/`gioDongCua`, tọa độ `viDo ∈ [-90,90]` & `kinhDo ∈ [-180,180]` (cả 2 cùng có hoặc cùng trống), tự sinh sân theo số lượng từng môn.<br>• **Sửa chi nhánh:** Bị chặn `403` với Admin.<br>• **Xóa mềm chi nhánh:** Chặn nếu còn sân `Đang sử dụng` hoặc giao dịch PayOS `PENDING`.<br>• **Duyệt / Từ chối Owner:** `?action=duyet` (kích hoạt cơ sở + capability + reset & gửi mật khẩu), `?action=khong-duyet`/`tu-choi` (ghi `AdminTrash`). | • Tọa độ sai: `"Vị trí cơ sở chưa hợp lệ. Vui lòng chọn lại vị trí trên bản đồ hoặc nhập đầy đủ tọa độ."`<br>• Sửa: `403 "Admin không có quyền chỉnh sửa cơ sở."`<br>• Xóa đang hoạt động: `"Không thể ngừng hoạt động cơ sở vì đang có <n> sân sử dụng..."` |
| **Quản lý Owner & Capability** | `GET /admin/quan-ly-owner` | Duyệt/từ chối cơ sở chờ duyệt, khóa/mở khóa chủ cơ sở (`khoa`/`mo-khoa`), duyệt/từ chối/tạm ngưng/kích hoạt từng capability (`reject`/`suspend` bắt buộc nhập `reason`). | `"Vui lòng nhập lý do từ chối."`, `"Vui lòng nhập lý do tạm ngưng."` |
| **Quản lý Nhân sự / Người dùng** | `GET/POST /admin/nhan-su` | • **Thêm (`action=add`):** Qua OTP (`ADMIN_ADD`, Mock OTP `123456`). Cấm tạo `RoleID=1`; `RoleID=2` bắt buộc có `coSoId`; email hợp lệ & không trùng; SĐT khớp `^(0\|\+84)[35789][0-9]{8}$`; mật khẩu (nếu nhập) phải mạnh ≥ 8 ký tự.<br>• **Sửa (`action=update`):** Đổi email kích hoạt OTP (`ADMIN_EDIT`); cấm đổi vai trò Admin, cấm cấp quyền Admin, cấm sửa mật khẩu trực tiếp cho vai trò `2/3/4/5`; hỗ trợ khóa/mở khóa (`isLocked`).<br>• **Xóa mềm (`softDelete`) & Khôi phục (`restore`):** Cấm xóa tài khoản Admin. | • `"Không được tạo tài khoản Quản trị viên!"`<br>• `"Quản lý cần phải có Cơ sở!"`<br>• `"Email đã tồn tại!"`<br>• `"Không thể xóa tài khoản Quản trị viên."` |
| **Cấu hình PayOS theo Chi nhánh** | `GET/POST /admin/chi-nhanh/payos` | Lưu `clientId`, `apiKey`, `checksumKey` (tối đa 500 ký tự, không chứa ký tự điều khiển hoặc chuỗi che `•`/`*`), bắt buộc xác thực OTP (`verify-otp`, sai 5 lần khóa `429`, gửi lại tối đa 5 lần/cooldown 60s; Mock OTP: `123456`). | `"Client ID không được để trống."`, `"API Key không được để trống."`, `"Checksum Key không được để trống."`, `"Mã OTP không đúng. Còn <n> lần thử."` |
| **Thùng rác Admin & Audit Log** | `/admin/thung-rac`<br>`/admin/audit-log` | Khôi phục `CoSo`, `Account`, `OwnerRequest`; tra cứu lịch sử tác động phân trang (`30 dòng/trang`). | `"Mục này đã được thu hồi trước đó."`, `"Không thể thu hồi vì dữ liệu gốc không còn tồn tại."` |

---

### 2.7. Nhóm Quản lý Chi nhánh (Manager — `RoleID = 2`, phạm vi `CoSoID`)

#### A. CRUD Kho Dịch vụ & Sản phẩm (`GET/POST /manager/kho-dich-vu`) — *Trọng tâm kiểm thử CRUD*
* **Thêm mới (`action=add`) & Cập nhật (`action=update`):**
  * `tenSanPham`: bắt buộc, tối đa **100 ký tự** (`"Tên sản phẩm không được trống."`, `"Tên sản phẩm không được vượt quá 100 ký tự."`).
  * `skuCode`: tự sinh `SKU-XXXXXX` nếu bỏ trống; **không được trùng trong cùng chi nhánh** (`"Mã SKU '<sku>' đã tồn tại trong kho của chi nhánh."` / `"...đã được sử dụng bởi sản phẩm khác."`).
  * `donGia` & `giaNhap`: **bắt buộc `> 0`**; `soLuongTon`: **bắt buộc `>= 0`** (`"Giá bán lẻ và giá nhập phải lớn hơn 0. Số lượng tồn không được là số âm."`).
  * `hinhAnhFile`: tùy chọn, tối đa **5MB**, chỉ nhận `JPG/PNG/WEBP/GIF` (`"Chỉ hỗ trợ ảnh sản phẩm định dạng JPG, PNG, WEBP hoặc GIF."`).
* **Xóa mềm (`action=delete`):** Chuyển vào Thùng rác (`"Đã chuyển sản phẩm/dịch vụ vào Thùng rác. Bạn có thể vào trang Thùng rác để khôi phục."`).
* **Nhập kho / Xuất kho (`action=nhap-kho`, `action=xuat-kho`):**
  * `amount > 0` (`"Số lượng nhập kho phải lớn hơn 0."` / `"Số lượng xuất kho phải lớn hơn 0."`).
  * Xuất kho không được vượt tồn kho (`"Lỗi: Số lượng xuất kho vượt quá số lượng tồn hiện có (<stock>)."`). Tự động chuyển trạng thái giữa `"Đang kinh doanh"` và `"Tạm hết hàng"` khi tồn về `0` hoặc tăng lại `> 0`.
* **CRUD Nhóm dịch vụ (`add-category`, `update-category`, `delete-category`):** Tên bắt buộc & duy nhất không phân biệt hoa thường; chặn xóa nếu còn sản phẩm thuộc nhóm (`"Không thể xóa nhóm dịch vụ này vì có <count> mặt hàng/dịch vụ đang thuộc nhóm."`).

#### B. CRUD Sân & Loại sân (`GET/POST /manager/quan-ly-san`) — *Trọng tâm kiểm thử CRUD*
* **Sân (`add`, `update`, `delete`, `updateStatus`):**
  * `tenSan`: bắt buộc, tối đa **50 ký tự**, duy nhất trong chi nhánh (`"Tên sân không được để trống"`, `"Tên sân không được vượt quá 50 ký tự"`, `"Tên sân đã tồn tại trong cơ sở này."`).
  * `loaiSanID > 0` (`"Phải chọn loại sân"`), `soLuong` tạo hàng loạt kẹp `[1, 10]`.
  * `trangThai` chỉ nhận 5 giá trị: `Sẵn sàng`, `Tạm đóng`, `Bảo trì`, `Đang dùng`, `Đang sử dụng`.
  * Chặn chuyển sang `Tạm đóng`/`Bảo trì` hoặc xóa mềm nếu còn lịch đặt hoạt động/tương lai (`Chờ xác nhận`, `Đã xác nhận`, `Đang sử dụng`). Chặn chuyển sang `Sẵn sàng` khi đang có ca `Đang sử dụng`.
* **Loại sân (`addType`, `updateType`, `deleteType`):** `tenLoai` bắt buộc, `monTheThaoID > 0`, `giaKhongDen >= 0`; nếu có bật đèn thì bắt buộc `giaCoDen > 0` cùng đủ `gioBatDauLenDen` và `gioKetThucLenDen` (`"Vui lòng nhập đầy đủ giá và thời gian áp dụng giá có đèn."`). Chặn xóa loại sân nếu còn sân liên kết.

#### C. CRUD Khuyến mãi (`GET/POST /manager/khuyen-mai`) — *Trọng tâm kiểm thử CRUD*
* `maCode`: viết hoa, **duy nhất toàn hệ thống** (`"Mã khuyến mãi '<maCode>' đã tồn tại."`).
* `loaiGiam`: `PERCENT` / `FIXED` (`PhanTram` / `SoTien`) (`"Vui lòng chọn hình thức giảm."`).
* `giaTriGiam`: bắt buộc `> 0` (`"Giá trị giảm phải lớn hơn 0."`); nếu `PERCENT` thì `<= 100` (`"Phần trăm giảm không được vượt quá 100."`).
* `ngayBatDau` & `ngayKetThuc`: bắt buộc đủ cả hai và `ngayKetThuc >= ngayBatDau` (`"Ngày kết thúc phải sau ngày bắt đầu."`).
* Tùy chọn: `soLanToiDa > 0`, `giaTriToiThieu >= 0`, `giamToiDa >= 0`. Hỗ trợ bật/tắt trạng thái (`action=toggle`).

#### D. Các nghiệp vụ Manager khác
* **Quản lý Nhân sự chi nhánh (`/manager/nhan-su`):** Thêm/sửa/xóa mềm/khôi phục nhân viên (thêm & đổi email qua OTP `MANAGER_ADD`/`MANAGER_EDIT`), **cấm nâng lên Role 1/2** (`"Không thể nâng cấp lên quyền Quản trị hoặc Quản lý!"`), **cấm xóa vĩnh viễn** (`405 PERMANENT_DELETE_DISABLED`), quản lý khung ca lặp theo thứ (`thu ∈ [2, 8]`, kiểm tra trùng giờ).
* **Lịch ca làm việc (`/manager/ca-lam`):** `CaLamValidationEngine` kiểm tra chặt: chỉ gán cho `RoleID=4` thuộc chi nhánh, không tạo ca quá khứ, nằm trong giờ mở cửa cơ sở, không hỗ trợ ca đêm/qua ngày, thời lượng thực `60–720 phút`, không trùng ca, nghỉ tối thiểu `8h` giữa 2 ngày liên tiếp, tối đa `12h/ngày`, `2 ca/ngày`, `48h/tuần`, `160h/tháng`. Hỗ trợ duyệt đổi ca, publish tuần, check-in/out thủ công.
* **Hóa đơn (`/manager/hoa-don`):** Xem chi tiết, tạo hóa đơn dịch vụ tách (`SPLIT`, kiểm tra & trừ tồn kho), thanh toán hóa đơn `SPLIT` (chặn thanh toán `MAIN` trực tiếp ở đây), hủy hóa đơn chưa thanh toán.
* **Duyệt đặt sân (`/manager/dat-san`), Xử lý Hoàn tiền (`/manager/hoan-tien`), Mã QR sân (`/manager/ma-qr-san`), Điều chỉnh điểm uy tín (`/manager/adjust-reputation`: `scoreDelta ∈ [-100, 100] \ {0}`, `reason ≥ 5` ký tự), Thư viện ảnh cơ sở (`/manager/co-so-gallery`: tối đa 10 ảnh), Thùng rác (`/manager/thung-rac`: chỉ khôi phục, cấm xóa vĩnh viễn).**

---

### 2.8. Nhóm Vận hành Lễ tân (Staff — `RoleID = 4`, phạm vi `CoSoID`)

* **Dashboard (`/staff/dashboard`):** Thống kê chi nhánh, 5 hóa đơn gần nhất.
* **Mở sân, Check-in & Thanh toán (`/staff/checkin`):**
  * Yêu cầu `RoleID ∈ {2, 4}` và cơ sở đang hoạt động (`403 "Cơ sở đã ngừng hoạt động."`).
  * `checkInPreBooked`: chống double-click bằng session lock, xác nhận nhận sân cho đơn đặt trước.
  * `checkInWalkIn`: mở sân vãng lai (`FIXED` hoặc `OPEN`), yêu cầu sân `Sẵn sàng`, `duration > 0`, tự tính đơn giá từ `CourtPricingService`.
  * `cancelNoShow`: đánh dấu không đến và trừ `-20` điểm uy tín khách hàng.
  * `addServices`, `previewSessionExtension`/`extendSession`, `stopOpenSession`, `processPayment` (`Tiền mặt` / `Chuyển khoản`), `initBankTransfer`/`confirmBankTransfer`/`cancelBankTransfer`, `createPayOSPayment`.
  * `applyEarlyCheckoutAdjustment`: chỉ `RoleID ∈ {1, 2}` và **chỉ cho khách vãng lai**, cấm áp dụng cho khách đặt trước (`FIXED_BOOKING`).
* **Nghiệp vụ Staff khác:** Duyệt/từ chối đơn đặt sân (`/staff/dat-san`, từ chối bắt buộc `reason` `1–255` ký tự), tách hóa đơn dịch vụ (`/staff/split-bill`), xác nhận thu tiền mặt chia tiền nhóm (`/staff/chia-tien-xac-nhan`, chống thu trùng `409`), giao/hủy dịch vụ đặt trước (`/staff/booking-service/update-status`), xử lý hoàn tiền (`/staff/hoan-tien`), xem/đổi ca làm (`/staff/ca-lam`), tiếp nhận yêu cầu QR tại sân (`/staff/yeu-cau-qr`), xem & in hóa đơn 80mm (`/staff/hoa-don/detail`, `/staff/hoa-don/in`).

---

## 3. DỮ LIỆU GIẢ LẬP & TÀI KHOẢN KIỂM THỬ (MOCK DATA)

* **Nguồn dữ liệu:** `mock-data/vsport-mock-data.json` (tự động nạp vào H2 In-Memory khi chạy `LocalServerLauncher` hoặc khi không có `DB_URL`; mọi thay đổi INSERT/UPDATE/DELETE được ghi ngược lại file JSON sau 800ms).
* **Mật khẩu chung cho toàn bộ tài khoản mẫu:** `123456` (băm BCrypt).
* **Mã OTP mặc định ở chế độ Mock Mode:** `123456`.

### 3.1. Danh sách Tài khoản mẫu

| AccountID | Username | Email | Số điện thoại | RoleID | Vai trò | CoSoID | Điểm uy tín |
| :---: | :--- | :--- | :--- | :---: | :--- | :---: | :---: |
| `1` | `admin` | `admin@vsport.vn` | `0901000001` | `1` | Admin | `null` | `100` |
| `2` | `manager1` | `manager1@vsport.vn` | `0901000002` | `2` | Quản lý | `1` | `100` |
| `3` | `khach1` | `khach1@vsport.vn` | `0901000003` | `3` | Khách hàng | `null` | `96` |
| `4` | `staff1` | `staff1@vsport.vn` | `0901000004` | `4` | Lễ tân | `1` | `100` |
| `5` | `manager2` | `manager2@vsport.vn` | `0901000005` | `2` | Quản lý | `2` | `100` |
| `6` | `khach2` | `khach2@vsport.vn` | `0901000006` | `3` | Khách hàng | `null` | `88` |

### 3.2. Tóm tắt dữ liệu nghiệp vụ sẵn có trong `vsport-mock-data.json`
* **Cơ sở (3):**
  1. `CoSoID=1`: **V-SPORT Arena Quận 1** (`06:00–23:00`, Quản lý `manager1`, 5 sân: `Sân 5A`, `Sân 5B`, `Sân 7A`, `Sân Cầu lông 01`, `Sân Cầu lông 02`, 6 sản phẩm/dịch vụ).
  2. `CoSoID=2`: **V-SPORT Center Thủ Đức** (`05:30–22:30`, Quản lý `manager2`, 3 sân: `Pickleball Court 01`, `Pickleball Court 02`, `Sân Cầu lông VIP 01`, 3 sản phẩm/dịch vụ).
  3. `CoSoID=3`: **V-SPORT Club Vũng Tàu** (`06:00–22:00`, Quản lý `manager1`, 1 sân: `Sân Tennis Biển 01`, 1 dịch vụ).
* **Môn thể thao (6):** `1` Bóng đá, `2` Cầu lông, `3` Pickleball, `4` Tennis, `5` Bóng rổ, `6` Bóng chuyền.
* **Mã khuyến mãi (3, đều đang `Hoạt động`):**
  * `VSPORT10`: CoSo 1, `PERCENT 10%`, đơn tối thiểu `200,000đ`, giảm tối đa `100,000đ`.
  * `PICKLE50K`: CoSo 2, `FIXED 50,000đ`, đơn tối thiểu `250,000đ`, giảm tối đa `50,000đ`.
  * `WELCOME20`: Toàn hệ thống (`coSoId=null`), `PERCENT 20%`, đơn tối thiểu `150,000đ`, giảm tối đa `80,000đ`.
* **Dữ liệu giao dịch mẫu:** 6 đơn đặt sân (`Đã hoàn thành`, `Đang sử dụng`, `Đã xác nhận`, `Chờ xác nhận`, `Quá hạn`), 3 hóa đơn, 10 sản phẩm/dịch vụ (4 danh mục), 2 đánh giá 5★, 2 đội nhóm, 1 kèo ghép đang mở, 3 ca làm việc `Published` cho `staff1`, 3 mã QR sân đang `ACTIVE` (`SAN5A1`, `CAUL01`, `PKB001`).

---

## 4. BẢN ĐỒ MÀN HÌNH, ENDPOINT & LOCATOR PHỤC VỤ SELENIUM POM

| Màn hình / Luồng | URL truy cập | Các Locator chính trên DOM (JSP thực tế) |
| :--- | :--- | :--- |
| **Đăng nhập** | `/index.jsp?auth=login` (hoặc `/dangnhap`) | Form `#loginForm`; input `#loginEmail`, `#loginPassword`; nút `#loginSubmitBtn`; lỗi trường `#loginEmailError`, `#loginPasswordError`; toast `#successToast` / `#toastMessage` |
| **Đăng ký & OTP** | `/index.jsp?auth=register` → bấm tab `.auth-tab-btn[data-target="registerCol"]` | Form `#registerForm`; input `#regName`, `#regPhone`, `#regEmail`, `#regPassword`, `#regConfirmPassword`, checkbox `#agreeTerms`; nút `#registerSubmitBtn`. Modal OTP `#otpModal`: 6 ô `#otpBoxRow input.otp-digit`, nút `#otpSubmitBtn`, gửi lại `#otpResendLink` |
| **Quên mật khẩu** | Bấm `a#forgotBtn` trên `#loginForm` | Modal `#forgotModal`, form `#forgotForm`, input `#forgotEmail`, nút `#forgotSubmitBtn` |
| **Đăng ký Chủ cơ sở** | `/owner/register` | `#formStep1` (`#ownerName`, `#regEmail`, `#regPhone`, `#regAddress`) → `#formStep2` (`#otpInputs input.otp-box`, `#btnVerifyOtp`) → `#formStep3` (`#sportsPopup`, `#openTime`, `#closeTime`, `#regDescription`) |
| **Hồ sơ & Đổi mật khẩu** | `/customer/ho-so`<br>`/customer/doi-mat-khau` | Hồ sơ: `#profileForm` (`#pfFullName`, `#pfEmail`, `#pfPhone`, `#pfHeight`, `#pfWeight`, `#pfNote`, `#pfSaveBtn`), OTP `#pfOtpModal`.<br>Đổi MK: `#currentPassword`, `#newPassword`, `#confirmPassword`, `#cpSaveBtn`, alert `#cpAlert` |
| **Tìm kiếm & Đặt sân** | `/customer/tim-kiem`<br>`/customer/dat-lich-truc-quan`<br>`/customer/dat-lich-truc-quan/xac-nhan` | Form lọc `q`, `sportId`, `openNow`; lưới chọn khung giờ 30 phút; xác nhận chọn `paymentMethod` (`payos`/`sau`), `ghiChu`, nút đặt sân |
| **Giỏ hàng & Hủy/Hoàn tiền** | `/customer/gio-hang`<br>`/customer/hoan-tien?id=<id>` | Danh sách đơn chờ xác nhận/thanh toán, modal thêm dịch vụ, modal hủy đơn (`lyDoHuy`, `refundChannel`), form cập nhật ngân hàng hoàn tiền (`nganHang`, `soTaiKhoan`, `chuTaiKhoan`, `qrImage`) |
| **CRUD Quản lý (Manager)** | `/manager/kho-dich-vu`<br>`/manager/quan-ly-san`<br>`/manager/khuyen-mai` | Bảng danh sách + form thêm/sửa/xóa mềm theo `action` (`add`, `update`, `delete`, `nhap-kho`, `xuat-kho`, `create`, `toggle`) |
| **Quản trị Admin** | `/admin/tong-quan`<br>`/admin/chi-nhanh`<br>`/admin/nhan-su` | Bảng chi nhánh, tab chờ duyệt Owner, form thêm chi nhánh, form thêm/sửa/khóa/xóa mềm nhân sự (kèm modal OTP) |

---

## 5. LƯU Ý ĐẶC THÙ CỦA MÃ NGUỒN PHỤC VỤ KIỂM THỬ (QA / EDGE CASES)

Các điểm thực tế phát hiện khi quét mã nguồn `src/main/` giúp thiết kế Test Case chính xác và tránh hiểu nhầm:
1. **Tab Đăng ký trên `index.jsp`:** Truy cập `/index.jsp?auth=register` chỉ kích hoạt `#authView` với tab mặc định là `#loginCol`; kịch bản Selenium cần click thêm `.auth-tab-btn[data-target="registerCol"]` để hiển thị `#registerForm`.
2. **Input mật khẩu tại `DoiMatKhau.jsp`:** Các ô `#currentPassword`, `#newPassword`, `#confirmPassword` dùng `type="text"` ẩn ký tự bằng CSS (`-webkit-text-security: disc`) thay vì `type="password"`.
3. **Khác biệt giữa Preview khuyến mãi và Tạo đơn đặt sân:** `XacNhanDatSan.jsp` gọi `POST /api/promotion/apply` để hiển thị số tiền giảm dự kiến, nhưng `DatSanServlet.handleDatSan` hiện **chưa đọc** `promoCode`/`khuyenMaiId` khi lưu `LichDatSan`.
4. **Dữ liệu mẫu `giaNhap = 0` cho nhóm dịch vụ thuê:** Trong `vsport-mock-data.json`, các mục thuê (`DV-AO-01`, `DV-VOT-01`, `DV-PKB-01`, `DV-TEN-01`) có `giaNhap = 0.0`, trong khi `KhoDichVuManagerServlet` yêu cầu `giaNhap > 0` khi thêm/sửa — đây là điểm giá trị biên đáng lưu ý khi test sửa sản phẩm thuê.
5. **Chính sách hủy đơn có 2 lớp xử lý:** Luồng UI `GioHang.jsp` gọi `RefundCancelServlet` (4 bậc phí: 24h / 12h / 4h, không trừ điểm uy tín), còn `POST /customer/huy-dat-san` trả `410 Gone` và `BookingCancellationService` (3 bậc: 24h / 6h, trừ `-5` / `-10` điểm uy tín) là lớp service tách biệt phù hợp viết JUnit Unit Test.
6. **Phân quyền ở mức Servlet:** Không có `Filter` phân quyền tập trung hay xác thực `_csrf` ở backend; toàn bộ kiểm tra quyền nằm trong từng Servlet (riêng `QuanLyChiNhanhServlet` và `PayOSConfigAdminServlet` có vài nhánh thiếu kiểm tra chặt `roleId == 1` — có thể đưa vào Test Case kiểm thử bảo mật/phân quyền).
