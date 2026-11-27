# BỘ KỊCH BẢN KIỂM THỬ (TEST CASES)

> Sinh từ [TEST_PLAN.md](TEST_PLAN.md) và [SPECIFICATIONS.md](SPECIFICATIONS.md). Mỗi ca truy vết về mục quy tắc nghiệp vụ **BR / NFR** trong đặc tả và mã nguồn thực tế trong `src/main/`. Kết quả mong đợi đối chiếu trực tiếp từ Servlet, Service, DAO và dữ liệu giả lập `mock-data/vsport-mock-data.json`.

* **Dự án:** V-SPORT · **Môi trường:** JDK 17, Embedded Tomcat 10.1 (`LocalServerLauncher`), H2 In-Memory (Mock Mode) + `mock-data/vsport-mock-data.json`, JUnit 5, Selenium 4 + TestNG · **Số ca:** 91
* **Ngày thiết kế:** 27/11/2026 · **Trạng thái hiện tại:** Đã chạy tự động các ca tầng `Unit` (JUnit 5 — 104/104 Pass) và `UI` (Selenium POM + TestNG — 13/13 Pass); các ca `Integ`, `Manual`, `Tĩnh` và phần phụ thuộc dịch vụ ngoài (PayOS/SMTP/Cloudinary thật) ghi rõ trạng thái thực tế.

## CÁCH ĐỌC BẢNG

| Cột | Ý nghĩa |
| :--- | :--- |
| **BR / NFR** | Quy tắc đặc tả (theo mục trong `SPECIFICATIONS.md`) hoặc yêu cầu phi chức năng mà ca kiểm tra |
| **P · Kỹ thuật · Tầng** | **P1** bắt buộc đạt. **P2** hồi quy. **P3** chấp nhận rủi ro. **Kỹ thuật:** `EP` phân vùng tương đương, `BVA` giá trị biên, `EG` đoán lỗi, `SEC` an toàn, `ST` chuyển trạng thái. **Tầng:** `Unit` (Servlet/Service JUnit 5 + Mockito), `Integ` (DAO/Service với H2 Mock DB), `UI` (Selenium POM + TestNG), `Manual` (trình duyệt thủ công), `Tĩnh` (rà soát mã nguồn) |
| **🔶** | Giả thuyết lỗi: ca nhằm xác nhận rủi ro R-xx trong `TEST_PLAN.md` hoặc điểm bất đồng bộ thực tế phát hiện trong mã nguồn `src/main/` |
| **Dữ liệu mẫu** | Dữ liệu gốc nạp từ `mock-data/vsport-mock-data.json` (mật khẩu chung `123456`, Mock OTP mặc định `123456`) |

**Dữ liệu nền** (nạp từ `mock-data/vsport-mock-data.json`, cô lập sang `target/test-mock-data/` khi chạy Unit Test):

| Tài khoản | id | Vai trò (`roleId`) | `coSoId` | SĐT | Điểm uy tín | Ghi chú |
| :--- | :---: | :--- | :---: | :--- | :---: | :--- |
| `admin` (`admin@vsport.vn`) / `123456` | 1 | Admin (`1`) | `null` | `0901000001` | 100 | Toàn hệ thống |
| `manager1` (`manager1@vsport.vn`) / `123456` | 2 | Quản lý (`2`) | 1 | `0901000002` | 100 | Quản lý Cơ sở 1 & 3 |
| `khach1` (`khach1@vsport.vn`) / `123456` | 3 | Khách hàng (`3`) | `null` | `0901000003` | 96 | Có đơn đặt sân 1, 3, 4, 5 |
| `staff1` (`staff1@vsport.vn`) / `123456` | 4 | Lễ tân (`4`) | 1 | `0901000004` | 100 | Có 3 ca làm `Published` |
| `manager2` (`manager2@vsport.vn`) / `123456` | 5 | Quản lý (`2`) | 2 | `0901000005` | 100 | Quản lý Cơ sở 2 |
| `khach2` (`khach2@vsport.vn`) / `123456` | 6 | Khách hàng (`3`) | `null` | `0901000006` | 88 | Có đơn đặt sân 2, 6 |

| Cơ sở (`coSoId`) | Giờ hoạt động | Quản lý | Các sân (`sanId` — Loại sân — Giá không đèn / có đèn) |
| :--- | :---: | :---: | :--- |
| **1. V-SPORT Arena Quận 1** | `06:00–23:00` | `manager1` (2) | `1` Sân 5A, `2` Sân 5B (`LoaiSan 1`: 300k/420k, đèn 17:30–23:00); `3` Sân 7A (`LoaiSan 2`: 550k/750k); `4` Sân Cầu lông 01, `5` Sân Cầu lông 02 (`LoaiSan 3`: 90k/130k, đèn 17:00–23:00) |
| **2. V-SPORT Center Thủ Đức** | `05:30–22:30` | `manager2` (5) | `6` Pickleball Court 01, `7` Pickleball Court 02 (`LoaiSan 4`: 140k/190k, đèn 17:30–22:30); `8` Sân Cầu lông VIP 01 (`LoaiSan 5`: 110k/150k) |
| **3. V-SPORT Club Vũng Tàu** | `06:00–22:00` | `manager1` (2) | `9` Sân Tennis Biển 01 (`LoaiSan 6`: 220k/320k, đèn 17:30–22:00) |

| Sản phẩm / Dịch vụ | id | `coSoId` | SKU | Giá nhập | Đơn giá | Tồn kho | Trạng thái |
| :--- | :---: | :---: | :--- | ---: | ---: | :---: | :--- |
| Nước điện giải Pocari Sweat 500ml | 1 | 1 | `SP-NUOC-01` | 12.000 | 20.000 | 120 | Đang kinh doanh |
| Nước suối Aquafina 500ml | 2 | 1 | `SP-NUOC-02` | 5.000 | 10.000 | 200 | Đang kinh doanh |
| Thuê áo Pitch (Bộ 7 cái) | 3 | 1 | `DV-AO-01` | **0** | 35.000 | 25 | Đang kinh doanh |
| Thuê vợt cầu lông Yonex | 4 | 1 | `DV-VOT-01` | **0** | 40.000 | 18 | Đang kinh doanh |
| Ống cầu lông Thành Công (12 quả) | 5 | 1 | `SP-CAU-01` | 190.000 | 245.000 | 40 | Đang kinh doanh |
| Quấn cán vợt chống trượt | 6 | 1 | `SP-PK-01` | 12.000 | 25.000 | 85 | Đang kinh doanh |
| Thuê vợt Pickleball Carbon | 7 | 2 | `DV-PKB-01` | **0** | 50.000 | 20 | Đang kinh doanh |
| Bóng Pickleball Franklin X-40 (Hộp 3 quả) | 8 | 2 | `SP-PKB-02` | 150.000 | 210.000 | 35 | Đang kinh doanh |
| Trà chanh mật ong ướp lạnh | 9 | 2 | `SP-NUOC-03` | 10.000 | 22.000 | 60 | Đang kinh doanh |
| Thuê vợt Tennis Babolat | 10 | 3 | `DV-TEN-01` | **0** | 60.000 | 12 | Đang kinh doanh |

| Mã khuyến mãi | id | `coSoId` | Loại giảm | Giá trị | Đơn tối thiểu | Giảm tối đa | Đã dùng / Tối đa | Trạng thái |
| :--- | :---: | :---: | :---: | ---: | ---: | ---: | :---: | :--- |
| `VSPORT10` | 1 | 1 | `PERCENT` | 10% | 200.000 | 100.000 | 18 / 200 | Hoạt động |
| `PICKLE50K` | 2 | 2 | `FIXED` | 50.000 | 250.000 | 50.000 | 9 / 150 | Hoạt động |
| `WELCOME20` | 3 | `null` | `PERCENT` | 20% | 150.000 | 80.000 | 42 / 500 | Hoạt động |

| Đơn đặt sân | id | Khách | Sân | Ngày & Giờ | Tiền dự kiến | Trạng thái | Ghi chú hóa đơn |
| :--- | :---: | :---: | :---: | :---: | ---: | :--- | :--- |
| Đơn 1 | 1 | `khach1` (3) | 1 | 2026-11-25 `18:00–19:30` | 630.000 | Đã hoàn thành | Hóa đơn 1 (685.000, PayOS, Đã thanh toán), đã đánh giá 5★ |
| Đơn 2 | 2 | `khach2` (6) | 4 | 2026-11-26 `22:00–23:30` | 195.000 | Đang sử dụng | Hóa đơn 2 (235.000, Tiền mặt, Chưa thanh toán) |
| Đơn 3 | 3 | `khach1` (3) | 3 | 2026-11-27 `19:00–20:30` | 1.125.000 | Đã xác nhận | Có 2 dịch vụ đặt trước `PENDING` & kèo ghép `keoId=1` |
| Đơn 4 | 4 | `khach1` (3) | 6 | 2026-11-27 `17:30–19:00` | 285.000 | Quá hạn | Hết hạn giữ chỗ thanh toán |
| Đơn 5 | 5 | `khach1` (3) | 2 | 2026-11-27 `20:00–21:00` | 420.000 | Chờ xác nhận | Hiển thị trong Giỏ hàng `/customer/gio-hang` |
| Đơn 6 | 6 | `khach2` (6) | 6 | 2026-11-24 `16:00–17:30` | 210.000 | Đã hoàn thành | Hóa đơn 3 (260.000, PayOS, Đã thanh toán), đã đánh giá 5★ |

---

## 1. ĐĂNG NHẬP, PHÂN QUYỀN, CÔNG KHAI (15 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_AUTH_01** | Đăng nhập | Admin đăng nhập đúng bằng Username | Positive | 1. Mở `/index.jsp?auth=login`<br>2. Nhập `#loginEmail`, `#loginPassword`<br>3. Bấm `#loginSubmitBtn` | `admin` / `123456` | Chuyển đến `/admin/tong-quan`. Session có `roleId = 1` | BR-2.1B | P1 · EP · Unit, UI | Pass |
| **TC_AUTH_02** | Đăng nhập | Manager đăng nhập đúng bằng Email | Positive | 1. Mở `/index.jsp?auth=login`<br>2. Nhập email và mật khẩu quản lý<br>3. Gửi form | `manager1@vsport.vn` / `123456` | Chuyển đến `/manager/dashboard`. Session có `roleId = 2`, `coSoId = 1` | BR-2.1B | P1 · EP · Unit, UI | Pass |
| **TC_AUTH_03** | Đăng nhập | Khách hàng đăng nhập đúng bằng SĐT dạng `0...` và `+84...` | Positive | 1. Đăng nhập với `0901000003`<br>2. Đăng nhập với `+84901000003` (`loginMethod=phone`) | `0901000003` và `+84901000003` / `123456` | Chuẩn hóa về `0901000003`, chuyển về `/index.jsp`, `roleId = 3` | BR-2.1B | P1 · EP · Unit, UI | Pass |
| **TC_AUTH_04** | Đăng nhập | Lễ tân đăng nhập đúng | Positive | 1. Mở `/index.jsp?auth=login`<br>2. Nhập tài khoản `staff1`<br>3. Gửi form | `staff1` / `123456` | Chuyển đến `/staff/dashboard`. Session có `roleId = 4`, `coSoId = 1` | BR-2.1B | P1 · EP · Unit, UI | Pass |
| **TC_AUTH_05** | Đăng nhập | Tài khoản hoặc mật khẩu rỗng / chỉ chứa khoảng trắng | Negative | 1. Gửi `POST /dangnhap` lần lượt với username `""`, `null`, password `""`, `"   "` | `admin` / `""`, `""` / `123456`, `admin` / `"   "` | Trả `success: false`, thông báo `"Tài khoản và mật khẩu không được để trống!"` (hoặc `"Số điện thoại và mật khẩu không được để trống!"`) | BR-2.1B | P1 · EP · Unit | Pass |
| **TC_AUTH_06** | Đăng nhập | SĐT VN sai đầu số hoặc thiếu số | Negative | 1. Gửi `POST /dangnhap` với `loginMethod=phone` và SĐT sai định dạng | `0123456789` / `123456` | Trả `success: false`, thông báo `"Số điện thoại không hợp lệ. Vui lòng nhập số di động Việt Nam (0, +84 hoặc 84)."` | BR-2.1B | P1 · EP · Unit | Pass |
| **TC_AUTH_07** | Đăng nhập | Sai mật khẩu hoặc tài khoản không tồn tại (chống dò tài khoản) | Negative | 1. Đăng nhập `khach2` với mật khẩu sai<br>2. Đăng nhập `nouser_ui_99` với mật khẩu bất kỳ | `khach2` / `WrongPass@123`; `nouser_ui_99` / `123456` | Cả 2 trường hợp trả cùng 1 thông báo chung: `"Email, số điện thoại hoặc mật khẩu không chính xác."` | BR-2.1B | P1 · SEC · Unit, UI | Pass |
| **TC_AUTH_08** | Đăng nhập | Rate limit đăng nhập quá 5 lần trong 5 phút | Negative | 1. Gửi liên tiếp 5 lần `POST /dangnhap` sai cho cùng định danh<br>2. Gửi lần thứ 6 trong 5 phút | `ratelimit_user_*` × 6 lần | Lần thứ 6 bị `SimpleRateLimiter` chặn trước khi truy vấn DB, ghi log `LOGIN_RATE_LIMITED` | BR-2.1B | P1 · BVA · Unit | Pass |
| **TC_AUTH_09** | Đăng nhập | Tham số `redirect` trỏ ra ngoài site (`//evil.com`) bị loại bỏ | Negative | 1. Gửi `GET /dangnhap?redirect=//evil.com/phishing` khi chưa đăng nhập | `redirect=//evil.com/phishing` | Chuyển về `/index.jsp?auth=login`, bỏ qua tham số redirect ngoài | BR-2.1B | P1 · SEC · Unit | Pass |
| **TC_AUTH_10** | Đăng xuất | Đăng xuất hủy session và xóa cookie `JSESSIONID` | Positive | 1. Đăng nhập `khach1`<br>2. Gọi `/logout`<br>3. Truy cập lại `/customer/tai-khoan` | `khach1` | Hủy session, xóa `JSESSIONID` (`Max-Age=0`), truy cập lại bị chuyển về trang đăng nhập | BR-2.1B | P1 · ST · Manual | Chưa chạy |
| **TC_AUTH_11** | Phân quyền | Khách chưa đăng nhập (Guest) truy cập trang cần đăng nhập | Negative | 1. Chưa đăng nhập<br>2. Mở `/customer/tai-khoan`, `/customer/gio-hang`, `/admin/tong-quan` | Không có | Chuyển về `/dangnhap?redirect=...` → `/index.jsp?auth=login` | BR-1, BR-2.2B | P1 · SEC · Unit | Pass |
| **TC_AUTH_12** | Phân quyền | Khách hàng (`RoleID=3`) truy cập route Admin, Manager, Staff | Negative | 1. Đăng nhập `khach1`<br>2. Truy cập `/admin/tong-quan`, `/manager/kho-dich-vu`, `/manager/quan-ly-san`, `/staff/dashboard` | `khach1` (`roleId=3`) | `/admin/*` & `/manager/quan-ly-san` chuyển về `/dangnhap`; `/manager/kho-dich-vu` & `/staff/dashboard` trả HTTP 403 | BR-1, BR-2.6, BR-2.7 | P1 · SEC · Unit | Pass |
| **TC_AUTH_13** | Phân quyền | Manager `CoSoID=1` truy cập chéo dữ liệu `CoSoID=2` | Negative | 1. Đăng nhập `manager1` (`coSoId=1`)<br>2. Gửi yêu cầu xóa sản phẩm `id=7` (CoSo 2) hoặc xóa sân `sanId=6` (CoSo 2) | `manager1` tác động `SanPhamID=7`, `SanID=6` | `KhoDichVuManagerServlet` báo `"Sản phẩm không hợp lệ."`; `SanService` ném `ForbiddenException` | BR-2.7 | P1 · SEC · Unit | Pass |
| **TC_AUTH_14** 🔶 | Phân quyền | Rủi ro R-05: kiểm tra thiếu chốt `roleId == 1` tại nhánh `QuanLyChiNhanhServlet` và `PayOSConfigAdminServlet` | Negative | 1. Đăng nhập `khach1` (`roleId=3`)<br>2. Gửi `GET /admin/chi-nhanh` và `GET /admin/chi-nhanh/payos?coSoId=1` | `khach1` (`roleId=3`) | Kỳ vọng chuẩn bảo mật: trả 403 hoặc redirect `/dangnhap`. **Giả thuyết lỗi 🔶**: code hiện tại thiếu guard `roleId == 1` ở 2 nhánh GET này (cần bổ sung filter/guard tập trung) | BR-2.6, R-05 | P1 · SEC · Integ | Chưa chạy |
| **TC_AUTH_15** 🔶 | Phân quyền | Manager chưa gán `coSoId` (`null`) truy cập `/manager/kho-dich-vu` | Negative | 1. Tạo session Manager với `coSoId = null`<br>2. Gọi `KhoDichVuManagerServlet.doGet` | `roleId=2, coSoId=null` | **Phát hiện thực tế 🔶**: `doGet` dòng 65 unbox `user.getCoSoId()` gây `NullPointerException` (trong khi `QuanLySanManagerServlet` trả 403 sạch) | BR-2.7 | P2 · EG · Unit | Pass *(đã xác nhận NPE)* |

---

## 2. ĐĂNG KÝ, XÁC THỰC OTP (MOCK `123456`), QUÊN MẬT KHẨU & ĐỐI TÁC (12 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_REG_01** | Đăng ký & OTP | Đăng ký tài khoản Khách hàng mới và xác thực bằng Mock OTP `123456` | Positive | 1. Mở `/index.jsp?auth=register`<br>2. Điền `#registerForm`, tích `#agreeTerms`, gửi<br>3. Nhập `123456` vào `#otpModal` | Email & SĐT mới, `Test@1234`, OTP `123456` | Tạo `TaiKhoan` (`roleId=3`, `diemUyTin=100`, `diemTrinhDo=1000`), thông báo `"Đăng ký thành công!"` | BR-2.1A | P1 · EP · Unit, UI | Pass |
| **TC_REG_02** | Đăng ký | Mật khẩu xác nhận không khớp hoặc mật khẩu `null` | Negative | 1. Gửi `POST /dangky` với `password != confirm_password` và `password = null` | `Test@1234` vs `Test@1235` | Trả `success: false`, thông báo `"Mật khẩu không trùng khớp!"` | BR-2.1A | P1 · EP · Unit | Pass |
| **TC_REG_03** | Đăng ký | Biên độ dài mật khẩu (7 ký tự) và thiếu nhóm ký tự (hoa/thường/số/đặc biệt) | Negative | 1. Gửi lần lượt `Ab1@567`, `Abcd1234`, `abcd@123`, `Abcd@efg` | 4 bộ mật khẩu yếu | Cả 4 báo `"Mật khẩu phải có tối thiểu 8 ký tự, bao gồm cả chữ hoa, chữ thường, số và ký tự đặc biệt."` | BR-2.1A | P1 · BVA · Unit | Pass |
| **TC_REG_04** | Đăng ký | Chưa tích đồng ý điều khoản (`agree`) | Negative | 1. Điền đủ thông tin nhưng `agree = null`<br>2. Gửi `POST /dangky` | `agree = null` | Trả `"Bạn chưa đồng ý với điều khoản!"` | BR-2.1A | P1 · EP · Unit | Pass |
| **TC_REG_05** | Đăng ký | Email sai định dạng hoặc chứa khoảng trắng; `username` 2 ký tự (`< 3`) | Negative | 1. Thử `uservsport.vn`, `user 01@vsport.vn`, `username = "ab"` | Các chuỗi sai biên | Email báo `"Email không hợp lệ và không được chứa khoảng trắng!"`; username báo `"Tên đăng nhập không hợp lệ (3-50 ký tự, không chứa khoảng trắng)!"` | BR-2.1A | P1 · BVA · Unit | Pass |
| **TC_REG_06** | Đăng ký | Trùng Email (`khach1@vsport.vn`) hoặc trùng SĐT dạng biến thể (`+84901000003`) | Negative | 1. Đăng ký email `khach1@vsport.vn`<br>2. Đăng ký SĐT `+84901000003` | `khach1@vsport.vn`; `+84901000003` | Báo `"Email đã tồn tại!"` và `"Số điện thoại đã được đăng ký. Vui lòng dùng số khác hoặc đăng nhập."` | BR-2.1A | P1 · EG · Unit | Pass |
| **TC_REG_07** | Đăng ký | Biên độ dài Họ và tên: 1 ký tự (`< 2`) và 101 ký tự (`> 100`) | Negative | 1. Gửi `fullname = "A"`<br>2. Gửi `fullname` 101 ký tự | 1 ký tự và 101 ký tự | Cả 2 báo `"Vui lòng nhập họ và tên hợp lệ (2-100 ký tự)."` | BR-2.1A | P1 · BVA · Unit | Pass |
| **TC_REG_08** | Xác thực OTP | Nhập sai mã OTP (`000000`) trong luồng Đăng ký | Negative | 1. Qua bước 1 đăng ký, mở `#otpModal`<br>2. Nhập `000000` và bấm xác nhận | `otp = "000000"` | Báo `"Mã xác thực không chính xác hoặc đã hết hạn."`. Không tạo tài khoản | BR-2.1A | P1 · EP · Unit, UI | Pass |
| **TC_REG_09** | Xác thực OTP | Nhập sai OTP 5 lần ở luồng nội bộ (`ADMIN_ADD`) bị khóa tạm 15 phút | Negative | 1. Session `authType="ADMIN_ADD"`<br>2. Gửi sai OTP `000000` 5 lần liên tiếp | 5 lần `000000` | Lần 5 trả `"lockedOut": true`, `OtpRateLimitUtil.isEmailLockedOut` trả `true` (khóa 15 phút) | BR-2.1A | P1 · ST · Unit | Pass |
| **TC_REG_10** | Gửi lại OTP | Vượt giới hạn gửi lại OTP tối đa 5 lần/phiên (`/resend-otp`) | Negative | 1. Gọi `/resend-otp` 5 lần trong cùng phiên<br>2. Gọi lần thứ 6 | `resendCount = 5` | 5 lần đầu thành công; lần thứ 6 báo `"Bạn đã vượt quá số lần gửi lại mã OTP tối đa (5/5 lần)."` | BR-2.1A | P2 · BVA · Integ | Chưa chạy |
| **TC_REG_11** | Quên mật khẩu | Email sai định dạng, email không tồn tại và vượt Rate Limit 5 lần/15p (`/quenmatkhau`) | Negative | 1. Gửi `bad-email`<br>2. Gửi `notfound@vsport.vn` 6 lần liên tiếp | `bad-email`; `notfound@vsport.vn` | Lần lượt báo `"Email không hợp lệ..."`, `"Không thể gửi mã đến email này..."`, và từ lần 6 `"Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau ít phút."` | BR-2.1C | P2 · BVA · Integ | Chưa chạy |
| **TC_REG_12** | Đối tác Owner | Đăng ký Chủ cơ sở 3 bước (`/owner/register`) với Mock OTP `123456` và biên tọa độ | Positive / Negative | 1. Gửi `/owner/send-otp` → xác thực `/owner/verify-otp` (`123456`)<br>2. Bước 3 thử `viDo=91`, sau đó sửa `10.77 / 106.70` | `viDo=91` (lỗi); `10.77, 106.70` (đúng) | `viDo=91` báo `"Vĩ độ phải nằm trong khoảng -90 đến 90."`; hợp lệ tạo `CoSo` trạng thái `Chờ duyệt` và tài khoản Manager `IsLocked=true` | BR-2.1D | P2 · BVA · Integ | Chưa chạy |

---

## 3. TÀI KHOẢN, HỒ SƠ CÁ NHÂN & ĐIỂM UY TÍN (7 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_ACC_01** | Hồ sơ cá nhân | Cập nhật họ tên, SĐT không đổi email (`/account/update-profile`) | Positive | 1. Đăng nhập `khach1`<br>2. Mở `/customer/ho-so`, sửa `#pfFullName`, giữ nguyên email<br>3. Bấm `#pfSaveBtn` | `fullName`: `Trần Văn Hùng`, `phoneNumber`: `0901000003` | Trả `"Cập nhật hồ sơ cá nhân thành công."`, không yêu cầu OTP | BR-2.2A | P1 · EP · Manual | Chưa chạy |
| **TC_ACC_02** | Đổi Email | Đổi email mới bắt buộc xác thực OTP `123456` (`verifyEmailOtp`) | Positive / Negative | 1. Đổi `#pfEmail` sang email mới → mở `#pfOtpModal`<br>2. Thử nhập `111111`, sau đó nhập `123456` | Email mới, OTP `111111` rồi `123456` | `111111` báo `"Mã OTP không chính xác."`; `123456` lưu email mới thành công | BR-2.2A | P1 · ST · Manual | Chưa chạy |
| **TC_ACC_03** | Hồ sơ thể chất | Biên chiều cao `[50, 260]` cm, cân nặng `[20, 300]` kg và ghi chú `≤ 500` ký tự | Negative / BVA | 1. Gửi `heightCm = 49, 261`, `weightKg = 19, 301`<br>2. Gửi `note` 501 ký tự | `49/261`, `19/301`, 501 ký tự | Trả HTTP 400: `"Chiều cao phải trong khoảng 50-260cm."`, `"Cân nặng phải trong khoảng 20-300kg."`, `"Ghi chú tối đa 500 ký tự."` | BR-2.2B | P2 · BVA · Integ | Chưa chạy |
| **TC_ACC_04** | Đổi mật khẩu | Đổi mật khẩu hợp lệ tại `/customer/doi-mat-khau` (`DoiMatKhau.jsp`) | Positive | 1. Đăng nhập `khach2`<br>2. Nhập `#currentPassword`, `#newPassword`, `#confirmPassword`<br>3. Bấm `#cpSaveBtn` | `123456` → `NewPass@123` | Trả `"Đổi mật khẩu tài khoản thành công."` *(lưu ý DOM dùng `type="text"` ẩn bằng CSS)* | BR-2.2A | P1 · EP · Manual | Chưa chạy |
| **TC_ACC_05** | Đổi mật khẩu | Sai mật khẩu hiện tại, mật khẩu mới trùng cũ, hoặc mật khẩu mới yếu | Negative | 1. Thử 3 ca sai trên `/account/update-profile` (`action=changePassword`) | `Wrong@1`, `123456` trùng cũ, `weak123` | Báo lần lượt `"Mật khẩu hiện tại không chính xác!"`, `"Mật khẩu mới không được trùng với mật khẩu hiện tại."`, `"Mật khẩu mới phải có tối thiểu 8 ký tự..."` | BR-2.2A | P1 · EP · Integ | Chưa chạy |
| **TC_ACC_06** | Ảnh đại diện / bìa | Tải tệp sai MIME (`.txt`) hoặc ảnh giả magic bytes lên avatar/cover | Negative | 1. Gửi `.txt` tới `action=updateAvatar`<br>2. Gửi `.jpg` nội dung text tới `/customer/ho-so/doi-cover` | `note.txt`, `fake.jpg` | Báo `"Chỉ hỗ trợ ảnh JPG, PNG, WEBP hoặc GIF."` và `"Tệp không phải ảnh hợp lệ."` | BR-2.2A, BR-2.2B | P2 · SEC · Integ | Chưa chạy |
| **TC_ACC_07** | Điểm uy tín | Hiển thị điểm uy tín, nhãn phân loại và chip lọc (`/customer/lich-su-diem-uy-tin`) | Positive | 1. Đăng nhập `khach1` (96đ)<br>2. Mở `/customer/lich-su-diem-uy-tin`, lọc `?type=ALL` và `?type=NO_SHOW` | `khach1` (`diemUyTin=96`) | Hiển thị `96`, nhãn `"Uy tín tốt"` (`≥ 80`), đủ điều kiện ghép kèo (`≥ 60`) | BR-2.2B | P2 · EP · Manual | Chưa chạy |

---

## 4. TÌM KIẾM, BẢN ĐỒ, CỬA HÀNG CÔNG KHAI & KHUYẾN MÃI (9 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_CAT_01** | Tìm kiếm sân | Lọc cơ sở theo từ khóa, môn thể thao và giờ mở cửa (`/customer/tim-kiem`) | Positive | 1. Mở `/customer/tim-kiem`<br>2. Lọc `q=Quận 1`, `sportId=1`, `openNow=true` | `q=Quận 1`, `sportId=1` | Hiển thị `V-SPORT Arena Quận 1` kèm các sân Bóng đá (`Sân 5A`, `5B`, `7A`) và mã `VSPORT10` | BR-2.3 | P1 · EP · Manual | Chưa chạy |
| **TC_CAT_02** | Bản đồ cơ sở | Biên tọa độ & bán kính `(0, 200]` km tại `/api/customer/facilities/map` | Positive / Negative | 1. Gọi `lat=10.77&lng=106.70&radiusKm=10`<br>2. Gọi thiếu `lng`, `radiusKm=0`, `radiusKm=201` | `10` km (đúng); `0`, `201`, thiếu `lng` (sai) | Ca hợp lệ trả HTTP 200 xếp tăng dần theo Haversine; các ca sai trả HTTP 400 | BR-2.3 | P2 · BVA · Integ | Chưa chạy |
| **TC_CAT_03** | Shop công khai | `/api/customer/facilities/shop?coSoId=1` ẩn số lượng tồn kho chính xác | Positive | 1. Gọi `GET /api/customer/facilities/shop?coSoId=1` | `coSoId=1` (6 sản phẩm) | Trả `available: true`, nhãn tồn kho `CON_HANG` / `SAP_HET` / `HET_HANG`, không lộ con số `soLuongTon` | BR-2.3 | P2 · SEC · Integ | Chưa chạy |
| **TC_CAT_04** | Khuyến mãi | Áp dụng `VSPORT10` (CoSo 1, 10%, trần `100k`): ca thường (`500k`) và chạm trần (`1.5M`) | Positive | 1. Gửi `POST /api/promotion/apply` với `vsport10`, `500000`, `coSoId=1`<br>2. Gửi với `1500000` | `vsport10`, `500000` & `1500000` | Giảm `50.000` ở ca 1 và chặn trần `100.000` ở ca 2 (`valid: true`) | BR-2.3 | P1 · BVA · Unit | Pass |
| **TC_CAT_05** | Khuyến mãi | Áp dụng mã toàn hệ thống `WELCOME20` (`coSoId=null`, 20%) cho CoSo 2 | Positive | 1. Gửi `POST /api/promotion/apply` với `WELCOME20`, `300000`, `coSoId=2` | `WELCOME20`, `300000`, `coSoId=2` | `valid: true`, giảm `60.000`, còn `240.000` | BR-2.3 | P1 · EP · Unit | Pass |
| **TC_CAT_06** | Khuyến mãi | Từ chối mã `PICKLE50K` (CoSo 2) khi áp dụng tại CoSo 1 | Negative | 1. Gửi `POST /api/promotion/apply` với `PICKLE50K`, `300000`, `coSoId=1` | `PICKLE50K`, `coSoId=1` | `valid: false`, báo `"Mã khuyến mãi không áp dụng cho cơ sở này."` | BR-2.3 | P1 · EP · Unit | Pass |
| **TC_CAT_07** | Khuyến mãi | Biên đơn tối thiểu `200.000` của `VSPORT10`: `199.999` (fail) vs `200.000` (pass) | Positive / Negative | 1. Gửi `originalAmount=199999`<br>2. Gửi `originalAmount=200000` | `199999` và `200000` | `199999` báo `"mới được áp dụng mã này"`; `200000` hợp lệ giảm `20.000` | BR-2.3 | P1 · BVA · Unit | Pass |
| **TC_CAT_08** | Khuyến mãi | Mã không tồn tại, mã rỗng, hoặc số tiền không phải số (`abc`) | Negative | 1. Gửi `KHONGTONTAI`, `""`, `originalAmount="abc"` | Dữ liệu lỗi | Trả `valid: false` kèm thông báo `"không tồn tại"`, `"Vui lòng nhập mã khuyến mãi..."`, `"Số tiền hoặc mã cơ sở không hợp lệ."` | BR-2.3 | P1 · EP · Unit | Pass |
| **TC_CAT_09** 🔶 | Khuyến mãi & Đặt sân | Rủi ro R-03c: `XacNhanDatSan.jsp` preview mã giảm giá nhưng `DatSanServlet` chưa lưu `promoCode` | Negative | 1. Chọn sân, áp dụng `VSPORT10` tại `XacNhanDatSan.jsp`<br>2. Gửi `POST /customer/dat-san` và kiểm tra `TongTienDuKien` trong DB | `VSPORT10`, `sanId=1` | **Giả thuyết lỗi 🔶**: `DatSanServlet.handleDatSan` hiện không đọc `promoCode`/`khuyenMaiId`, tổng tiền lưu vào DB vẫn là giá gốc chưa trừ khuyến mãi | BR-2.3, R-03 | P2 · EG · Integ | Chưa chạy |

---

## 5. ĐẶT LỊCH SÂN, GIỮ CHỖ, GIỎ HÀNG & ĐÁNH GIÁ (12 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_BOOK_01** | Tính giá bật đèn | Tách đoạn tính giá qua mốc `17:30` trên `Sân 5A` (`300k`/`420k`) | Positive | 1. Gọi `CourtPricingService.calculate` cho `17:00–18:30` | `17:00–18:30`, bật đèn `17:30–23:00` | 2 phân đoạn: `30p × 300k/h` (150k) + `60p × 420k/h` (420k) = **570.000** | BR-2.4A | P1 · EP · Unit | Pass |
| **TC_BOOK_02** | Tính giá sân | Tham số thời gian `null`, `end <= start` hoặc giá sân âm ném `IllegalArgumentException` | Negative | 1. Gọi `CourtPricingService.calculate` với `startAt=null`, `endAt < startAt`, `rate=-1` | Tham số lỗi | Cả 3 lần ném `IllegalArgumentException` đúng thông báo | BR-2.4A | P1 · BVA · Unit | Pass |
| **TC_BOOK_03** | Biên thời lượng & ngày | Chặn thời lượng `< 30p` / `> 240p` và ngày quá khứ / `> +30 ngày` | Negative | 1. Gọi `/customer/api/timetable-price` với `29p` và `241p`<br>2. Gửi `POST /customer/dat-san` ngày hôm qua và `+31 ngày` | `29p`, `241p`, `today-1`, `today+31` | Báo `"Thời lượng tối thiểu là 30 phút."`, `"Thời lượng tối đa là 4 giờ (240 phút)."` và chặn đặt quá khứ / quá 30 ngày | BR-2.4A | P1 · BVA · Integ | Chưa chạy |
| **TC_BOOK_04** | Giữ chỗ tạm | `SoftHold` giữ chỗ 120s và chặn người thứ 2 giữ trùng khung giờ | Positive / Negative | 1. `khach1` gọi `POST /customer/giu-cho-tam` cho `sanId=2`<br>2. `khach2` gọi trùng khung giờ trong 120s | `sanId=2`, `09:00–10:00` ngày mai | `khach1` nhận `holdSeconds: 120`; `khach2` bị chặn `SLOT_ALREADY_RESERVED` | BR-2.4B | P1 · ST · Integ | Chưa chạy |
| **TC_BOOK_05** | Đặt sân & Giỏ hàng | Đặt sân thanh toán tại quầy (`paymentMethod=sau`) và kiểm tra `/customer/gio-hang` | Positive | 1. Chọn khung giờ trống trên `Sân 5B`<br>2. Gửi `POST /customer/dat-san` (`paymentMethod=sau`)<br>3. Mở `/customer/gio-hang` & `/customer/api/cart-count` | `khach1`, `sanId=2`, `paymentMethod=sau` | Tạo đơn `Chờ xác nhận`, chuyển `/customer/gio-hang`, badge `cart-count` tăng thêm 1 | BR-2.4B, BR-2.4C | P1 · EP · Manual | Chưa chạy |
| **TC_BOOK_06** | Giới hạn/ngày | Chặn đặt đơn thứ 4 trong cùng ngày (`MAX_ACTIVE_BOOKINGS_PER_DAY = 3`) | Negative | 1. `khach1` có 3 đơn hoạt động cùng ngày `D`<br>2. Đặt đơn thứ 4 trong ngày `D` | 4 đơn trong cùng ngày | Trả mã `BOOKING_LIMIT_REACHED`: `"Bạn đã đạt giới hạn đặt sân tối đa trong ngày hôm nay (tối đa 3 lượt đặt/ngày)."` | BR-2.4A | P1 · BVA · Integ | Chưa chạy |
| **TC_BOOK_07** | Điểm uy tín thấp | Chặn đặt sân khi `DiemUyTin < 20` (`19` fail vs `20` pass) | Negative | 1. Đặt `DiemUyTin = 19`<br>2. Gửi `POST /customer/dat-san` | `DiemUyTin = 19` | Bị chặn với mã `REPUTATION_BLOCKED` | BR-2.4A | P2 · BVA · Integ | Chưa chạy |
| **TC_BOOK_08** | Dịch vụ kèm đơn | Thêm dịch vụ tại `/customer/dat-dich-vu`: hợp lệ với đơn `Đang sử dụng` (`id=2`), chặn với `Chờ xác nhận` (`id=5`) | Positive / Negative | 1. `khach2` thêm 2 `Pocari` vào `datSanId=2`<br>2. `khach1` thêm vào `datSanId=5` | `datSanId=2` & `datSanId=5`, `productId=1` | Đơn 2 thành công và trừ tồn kho; Đơn 5 báo `"Chỉ được phép thêm/cập nhật dịch vụ khi đơn đặt sân ở trạng thái 'Đang sử dụng'."` | BR-2.4C | P1 · ST · Integ | Chưa chạy |
| **TC_BOOK_09** | Hủy đơn & Hoàn tiền | Kiểm tra 4 bậc phí hủy (`24h/12h/4h`) tại `/customer/refund-cancel/preview` & `confirm` | Positive / BVA | 1. Xem trước và xác nhận hủy ở 4 mốc: `25h`, `15h`, `6h`, `2h` trước giờ chơi | Các mốc giờ hủy | Phí hủy lần lượt `0%`, `30%`, `50%`, `100%`; đơn đổi `Đã hủy`; tạo `HoanTien` ở `CHO_BO_SUNG_THONG_TIN` nếu hoàn > 0 | BR-2.5 | P1 · BVA · Integ | Chưa chạy |
| **TC_BOOK_10** | Hoàn tiền | Cập nhật đủ 3 trường ngân hàng (`update-bank`) chuyển trạng thái sang `CHO_XU_LY` | Positive / Negative | 1. Gửi thiếu `soTaiKhoan`<br>2. Gửi đủ `nganHang`, `soTaiKhoan`, `chuTaiKhoan` | `VCB`, `007100999888`, `TRAN VAN HUNG` | Thiếu trường báo lỗi; đủ 3 trường chuyển trạng thái `CHO_XU_LY` | BR-2.5 | P2 · ST · Manual | Chưa chạy |
| **TC_BOOK_11** 🔶 | Đánh giá sân | Biên `soSao` (`0`, `6`), bình luận rỗng, và giả thuyết lỗi lệch trạng thái `'Hoàn thành'` vs `'Đã hoàn thành'` | Negative | 1. Gửi `soSao=0, 6`, `binhLuan=""` tới `/customer/danh-gia-san`<br>2. Gửi hợp lệ cho `datSanId=1` (`Đã hoàn thành`) | `khach1`, `datSanId=1` | Biên `soSao`/`binhLuan` báo đúng lỗi; **phát hiện lỗi thật 🔶**: `DanhGiaDAOImpl` lọc `TrangThai = N'Hoàn thành'` trong khi `mock-data` dùng `'Đã hoàn thành'`, nên nhánh kiểm tra trùng phải test kèm Mockito DAO | BR-2.5 | P1 · BVA, EG · Unit | Pass *(đã xác nhận lệch SQL)* |
| **TC_BOOK_12** | Ghép kèo cá nhân | Biên điểm uy tín `60` khi tham gia `keoId=1` (`khach2` 88đ pass vs 59đ fail) | Positive / Negative | 1. `khach2` (88đ) gọi `POST /customer/api/matches/join?keoId=1`<br>2. Thử với tài khoản 59đ | `keoId=1` (`minRep=60`) | `khach2` tham gia thành công (`auto`); 59đ bị từ chối | BR-2.5 | P2 · BVA · Integ | Chưa chạy |

---

## 6. KHO DỊCH VỤ, SÂN & KHUYẾN MÃI — MANAGER CRUD (13 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_MGR_01** | CRUD Kho dịch vụ | Chuỗi phụ thuộc 5 bước UI: Đăng nhập `manager1` → Mở `/manager/kho-dich-vu` → Thêm → Tìm SKU → Xóa mềm | Positive | 1. Chạy `ManagerCrudFlowTestNGTest` (`dependsOnMethods`) trên Chrome | `manager1` / `123456`, `SKU-POM-*`, giá nhập `12000`, giá bán `22000`, tồn `25` | Tạo mới thành công, lọc đúng 1 dòng theo SKU, xóa mềm qua `#customConfirmSubmitBtn` báo chuyển vào `"Thùng rác"` | BR-2.7A | P1 · ST · UI | Pass |
| **TC_MGR_02** | Kho dịch vụ | Tên sản phẩm rỗng/`null`, tên 101 ký tự (`> 100`), trùng SKU `SP-NUOC-01` | Negative | 1. Gửi `POST /manager/kho-dich-vu` (`action=add`) với 4 bộ dữ liệu sai | `""`, `null`, 101 ký tự, `SP-NUOC-01` | Báo `"Tên sản phẩm không được trống."`, `"Tên sản phẩm không được vượt quá 100 ký tự."`, `"Mã SKU 'SP-NUOC-01' đã tồn tại trong kho của chi nhánh."` | BR-2.7A | P1 · BVA · Unit | Pass |
| **TC_MGR_03** | Kho dịch vụ | Biên giá bán/giá nhập (`0`, âm) và tồn kho (`-1`) | Negative | 1. Gửi `donGia=0`, `donGia=-5000`, `giaNhap=0`, `soLuongTon=-1` | `0`, `-5000`, `0`, `-1` | Cả 4 báo `"Giá bán lẻ và giá nhập phải lớn hơn 0. Số lượng tồn không được là số âm."` | BR-2.7A | P1 · BVA · Unit | Pass |
| **TC_MGR_04** 🔶 | Kho dịch vụ | Rủi ro R-03d: sửa dịch vụ thuê có `giaNhap = 0` trong `mock-data` (`DV-AO-01`, `DV-VOT-01`) | Negative | 1. Mở sửa `DV-AO-01` (`id=3`, `giaNhap=0.0`) giữ nguyên giá nhập `0`<br>2. Lưu | `SanPhamID=3` (`giaNhap=0`) | **Giả thuyết lỗi 🔶**: bị chặn vì rule `giaNhap > 0`, buộc người quản lý phải đổi giá nhập > 0 dù là dịch vụ cho thuê | BR-2.7A, R-03 | P2 · EG · Integ | Chưa chạy |
| **TC_MGR_05** | Nhập / Xuất kho | Biên `amount <= 0` và xuất kho vượt tồn hiện có (`999999`) | Negative | 1. Gửi `nhap-kho` với `0`, `-5`<br>2. Gửi `xuat-kho` với `0`, `999999` cho `SanPhamID=1` | `0`, `-5`, `999999` | Báo `"Số lượng nhập kho phải lớn hơn 0."`, `"Số lượng xuất kho phải lớn hơn 0."`, `"Số lượng xuất kho vượt quá số lượng tồn hiện có..."` | BR-2.7A | P1 · BVA · Unit | Pass |
| **TC_MGR_06** | Thùng rác Manager | Khôi phục sản phẩm đã xóa mềm và chặn xóa vĩnh viễn (`/manager/thung-rac`) | Positive / Negative | 1. Khôi phục `type=sanpham`<br>2. Gửi `action=permanentDelete` | Sản phẩm đã xóa mềm | Khôi phục báo `"Khôi phục thành công."`; xóa vĩnh viễn bị chặn `"Hệ thống không hỗ trợ xóa vĩnh viễn."` | BR-2.7D | P2 · ST · Integ | Chưa chạy |
| **TC_MGR_07** | Quản lý Sân | `SanService.createSan` chặn trùng tên (`Sân 5A - Cỏ FIFA`), tên trống, trạng thái sai | Negative | 1. Gọi `SanService.createSan` với tên trùng & dữ liệu sai | `"Sân 5A - Cỏ FIFA"`, `""`, `"TrangThaiLa"` | Ném `IllegalArgumentException` đúng thông báo | BR-2.7B | P1 · EG · Unit | Pass |
| **TC_MGR_08** | Quản lý Sân | `SanService.deleteLoaiSan` chặn xóa loại sân đang có sân liên kết (`LoaiSanID=1`) & kiểm tra whitelist trạng thái | Negative | 1. Gọi `deleteLoaiSan(1, 1)`<br>2. Gọi `updateSanStatus(1, "TrangThaiKhongHopLe", 1)` | `LoaiSanID=1` (2 sân liên kết) | Ném `IllegalArgumentException` (`"Không thể xóa loại sân này..."`, `"Trạng thái sân không hợp lệ..."`) | BR-2.7B | P1 · ST · Unit | Pass |
| **TC_MGR_09** 🔶 | Quản lý Sân | Tương thích SQL Server → H2 tại `SanService.deleteSan` (`DATEDIFF` với `'00:00:00'`) | Negative | 1. Gọi `SanService.deleteSan(4, 1)` trên H2 Mock Mode | `SanID=4` (đang có `datSanId=2`) | **Phát hiện thực tế 🔶**: trên H2, câu SQL `DATEDIFF(second, '00:00:00', GioKetThuc)` gây `SQLException` → `RuntimeException("Lỗi hệ thống khi kiểm tra lịch đặt sân.")` thay vì `IllegalArgumentException` nghiệp vụ | BR-2.7B | P2 · EG · Integ | Pass *(đã xác nhận lỗi H2)* |
| **TC_MGR_10** | Khuyến mãi | Biên tạo mã khuyến mãi (`KhuyenMaiManagerServlet`): trùng `VSPORT10`, `% > 100`, `giaTriGiam <= 0`, `end < start`, giá trị âm | Negative | 1. Gửi `POST /manager/khuyen-mai` (`action=create`) với 10 bộ dữ liệu vi phạm biên | `VSPORT10`, `101%`, `0`, `-5`, `end < start`, `-1` | Mỗi ca đặt `errorMsg` chính xác trong session, không lưu DB | BR-2.7C | P1 · BVA · Unit | Pass |
| **TC_MGR_11** | Nhân sự chi nhánh | `NhanSuService` chặn nâng quyền Lễ tân lên Manager và chặn xóa tài khoản Manager | Negative | 1. `updateStaff(4, roleId=2, 1)`<br>2. `deleteStaff(2, 1)` | `staff1` (4), `manager1` (2) | Ném `IllegalArgumentException` (`"Không thể nâng cấp..."`, `"Không thể xóa tài khoản có quyền Quản trị hoặc Quản lý!"`) | BR-2.7D | P1 · SEC · Unit | Pass |
| **TC_MGR_12** | Ca làm việc | `CaLamService.createShift` ném `IllegalArgumentException` khi `request=null` và `NotFoundException` khi nhân viên không tồn tại | Negative | 1. Gọi `createShift(null, 1)`<br>2. Gọi với `accountId=99999` | `null`, `accountId=99999` | Ném đúng `IllegalArgumentException` và `NotFoundException("Nhân viên không tồn tại.")` | BR-2.7D | P1 · EP · Unit | Pass |
| **TC_MGR_13** | Điểm uy tín | Biên điều chỉnh điểm uy tín `[-100, 100] \ {0}` và lý do `≥ 5` ký tự (`/manager/adjust-reputation`) | Negative | 1. Gửi `scoreDelta = 0`, `101`, `-101`<br>2. Gửi `reason = "1234"` (4 ký tự) | `targetAccountId=3` (`khach1`) | Báo `"Số điểm điều chỉnh phải từ -100 đến +100 và khác 0."` và `"Dữ liệu không hợp lệ. Lý do phải dài ít nhất 5 ký tự."` | BR-2.7D | P2 · BVA · Integ | Chưa chạy |

---

## 7. QUẢN TRỊ ADMIN & VẬN HÀNH LỄ TÂN (9 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_ADM_01** | Admin Chi nhánh | Chặn Admin sửa chi nhánh (`GET /admin/chi-nhanh/sua` → HTTP 403) | Negative | 1. Đăng nhập `admin`<br>2. Gửi `GET /admin/chi-nhanh/sua?id=1` | `admin`, `id=1` | Trả HTTP 403 `"Admin không có quyền chỉnh sửa cơ sở."` | BR-2.6 | P1 · SEC · Unit | Pass |
| **TC_ADM_02** | Admin Chi nhánh | Thêm chi nhánh kiểm tra cặp tọa độ (`viDo ∈ [-90,90]`, `kinhDo ∈ [-180,180]`) | Negative | 1. Gửi `POST /admin/chi-nhanh/them` chỉ có `viDo=10.77`, trống `kinhDo` | `viDo=10.77`, `kinhDo=""` | Báo `"Vị trí cơ sở chưa hợp lệ. Vui lòng chọn lại vị trí trên bản đồ hoặc nhập đầy đủ tọa độ."` | BR-2.6 | P2 · BVA · Integ | Chưa chạy |
| **TC_ADM_03** | Admin Nhân sự | Kiểm tra 7 ràng buộc tạo nhân sự tại `/admin/nhan-su` (`action=add`) | Negative | 1. Gửi email rỗng/khoảng trắng/trùng `admin@vsport.vn`, SĐT sai, `roleId=1`, `roleId=2` thiếu `coSoId`, mật khẩu yếu | 7 bộ dữ liệu sai | Trả `success: false` đúng từng thông báo (cấm tạo Admin, Manager cần Cơ sở, mật khẩu mạnh ≥ 8 ký tự) | BR-2.6 | P1 · BVA · Unit | Pass |
| **TC_ADM_04** | Admin Nhân sự | Cấm xóa mềm tài khoản Admin (`id=1`) & ném `NumberFormatException` khi `id`/`accountId` không phải số | Negative | 1. Gửi `softDelete` `id=1`<br>2. Gửi `GET ?action=delete&id=abc` và `POST action=update&accountId=invalid` | `id=1`, `"abc"`, `"invalid"` | Báo `"Không thể xóa tài khoản Quản trị viên."`; tham số không phải số ném `NumberFormatException` | BR-2.6 | P1 · EG · Unit | Pass |
| **TC_ADM_05** | Admin PayOS | Kiểm tra biên cấu hình PayOS (`/admin/chi-nhanh/payos`): `coSoId="abc"`, ký tự che `••••••`, thiếu trường bắt buộc | Negative | 1. Gửi `POST /admin/chi-nhanh/payos` với 5 bộ đầu vào lỗi | `abc`, `••••••`, `""` | Trả `success: false` với thông báo chính xác cho từng trường | BR-2.6 | P1 · BVA · Unit | Pass |
| **TC_ADM_06** | Lễ tân Check-in | Mở sân vãng lai từ chối `duration <= 0` và sân không `Sẵn sàng` (`/staff/checkin`) | Negative | 1. Đăng nhập `staff1`<br>2. Gửi `action=checkInWalkIn` với `duration=0` | `staff1`, `sanId=1`, `duration=0` | Báo `"Thời gian chơi phải lớn hơn 0 phút."` | BR-2.8 | P1 · BVA · Integ | Chưa chạy |
| **TC_ADM_07** | Lễ tân Check-in | Chặn Lễ tân (`RoleID=4`) áp dụng giảm trừ trả sân sớm (`applyEarlyCheckoutAdjustment`) | Negative | 1. Đăng nhập `staff1`<br>2. Gửi `POST /staff/checkin` (`action=applyEarlyCheckoutAdjustment`) | `staff1`, `datSanId=2`, `earlyDiscount=50000` | Bị từ chối: `"Chỉ quản lý hoặc admin mới được thực hiện giảm trừ trả sân sớm."` | BR-2.8 | P1 · SEC · Integ | Chưa chạy |
| **TC_ADM_08** | Chia tiền nhóm | Xác nhận thu tiền mặt chia tiền nhóm (`/staff/chia-tien-xac-nhan`) chống thu trùng (`409`) | Negative | 1. `staff1` xác nhận 1 `shareId` đã thanh toán hoặc thiếu `shareId` | `shareId` rỗng / đã `PAID` | Thiếu trả HTTP 400 `"Thiếu shareId."`; thu trùng trả HTTP 409 | BR-2.5, BR-2.8 | P2 · ST · Integ | Chưa chạy |
| **TC_ADM_09** | QR tại sân | Quét mã QR `SAN5A1` (`/qr/SAN5A1`) và chống spam gửi trùng yêu cầu trong 20 giây (`/api/qr/yeu-cau`) | Positive / Negative | 1. Mở `/qr/SAN5A1`<br>2. Gửi 2 lần liên tiếp `CALL_STAFF` cùng `guestToken` trong < 20s | `shortCode=SAN5A1`, `requestType=CALL_STAFF` | Lần 1 thành công; lần 2 trả HTTP 409 `"Yêu cầu của bạn đã được gửi trước đó. Vui lòng chờ nhân viên tiếp nhận."` | BR-2.5 | P2 · ST · Integ | Chưa chạy |

---

## 8. TÍCH HỢP NGOÀI & CHẾ ĐỘ GIẢ LẬP — MOCK MODE (5 CA)

> Nhóm này ghi nhận đúng thực tế các thành phần đang chạy bằng cơ chế giả lập (**H2 In-Memory + JSON**, **Mock OTP `123456`**) hoặc phụ thuộc dịch vụ bên thứ ba (**PayOS**, **SMTP**, **Cloudinary**) và endpoint đã khai tử.

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_MOCK_01** | Mock DB (H2 + JSON) | Tự bật Mock Mode khi không có `DB_URL`, nạp đủ 6 tài khoản từ `vsport-mock-data.json` và cô lập file test | Positive | 1. Chạy với `-Dvsport.mock.mode=true -Dvsport.mock.file=target/test-mock-data/...`<br>2. Kiểm tra file `mock-data/vsport-mock-data.json` gốc sau `mvn test` | `vsport-mock-data.json` | H2 khởi tạo đủ 35 bảng + 6 tài khoản mẫu; file `mock-data/vsport-mock-data.json` gốc không bị sửa đổi | BR-3, R-01 | P1 · EP · Unit, Integ | Pass |
| **TC_MOCK_02** | Mock OTP & Email | Chế độ Mock chấp nhận OTP `123456` và in `[MOCK HTML EMAIL]` thay vì gửi SMTP thật | Positive | 1. Đăng ký tài khoản mới khi `vsport.mock.mode=true`<br>2. Kiểm tra console log và xác thực bằng `123456` | OTP `123456` | Không cần cấu hình SMTP thật; console in `[MOCK HTML EMAIL]` và xác thực thành công | BR-2.1A, BR-3 | P1 · EP · Unit, UI | Pass |
| **TC_MOCK_03** | Thanh toán PayOS (Ngoài phạm vi thật) | Đặt sân `paymentMethod=payos` khi cơ sở chưa cấu hình API key PayOS thật | Negative | 1. Gửi `POST /customer/dat-san` với `paymentMethod=payos` trên môi trường Mock không có key PayOS | `paymentMethod=payos` | Giữ đơn ở trạng thái `Chờ thanh toán` để khách thử lại hoặc chuyển trả tại quầy (`/customer/payos-pay-counter`), trả mã lỗi `PAYOS_NOT_CONFIGURED` | BR-2.4B, R-04 | P2 · EG · Manual | Chưa chạy *(Mock/ngoài phạm vi cổng thật)* |
| **TC_MOCK_04** | Upload Cloudinary (Ngoài phạm vi thật) | Upload ảnh đại diện / thư viện ảnh cơ sở khi không có biến môi trường Cloudinary | Negative | 1. Gửi ảnh JPG hợp lệ tới `/manager/co-so-gallery` (`action=upload`) khi thiếu key Cloudinary | Ảnh `.jpg` < 2MB | Báo lỗi cấu hình upload thay vì crash server *(không test upload thật trên cloud)* | BR-2.7D, R-04 | P3 · EG · Manual | Chưa chạy *(phụ thuộc cloud ngoài)* |
| **TC_MOCK_05** | Endpoint đã khai tử | Gọi `POST /customer/huy-dat-san` (luồng hủy cũ đã thay bằng `/customer/refund-cancel/*`) | Negative | 1. Đăng nhập `khach1`<br>2. Gửi `POST /customer/huy-dat-san` | `datSanId=5` | Trả HTTP `410 Gone` (`"This endpoint is deprecated."`) | BR-2.5, §5.5 | P3 · ST · Integ | Chưa chạy |

---

## 9. PHI CHỨC NĂNG VÀ BẢO MẬT (6 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_NFR_01** | SQL Injection | Chuỗi SQL injection trong ô đăng nhập và tìm kiếm sân | Negative | 1. Nhập `' OR '1'='1` vào `#loginEmail` và ô `q` của `/customer/tim-kiem` | `' OR '1'='1` | Không vượt qua đăng nhập; tìm kiếm không lỗi SQL; dữ liệu bảng `TaiKhoan`/`CoSo` không đổi | NFR-SEC | P1 · SEC · Integ | Chưa chạy |
| **TC_NFR_02** | XSS | Nhập chuỗi `<script>` vào bình luận đánh giá / tên sản phẩm | Negative | 1. Gửi bình luận `<script>document.title='x'</script>`<br>2. Mở trang hiển thị | Chuỗi `<script>` | Văn bản được escape (`c:out`), không thực thi JS, `document.title` không đổi | NFR-SEC | P1 · SEC · Manual | Chưa chạy |
| **TC_NFR_03** | Băm mật khẩu | Mật khẩu tài khoản được lưu dạng BCrypt (`$2a$...`), không lưu văn bản thuần | Positive | 1. Kiểm tra trường `password` trong `mock-data/vsport-mock-data.json` và sau khi đăng ký tài khoản mới | `123456` / `Test@1234` | Chuỗi lưu bắt đầu bằng `$2a$10$` hoặc `$2a$12$`, không chứa mật khẩu gốc | BR-2.1A | P1 · SEC · Unit | Pass |
| **TC_NFR_04** | Mã hóa UTF-8 | Tiếng Việt có dấu được lưu và hiển thị chuẩn qua `EncodingAndCacheControlFilter` | Positive | 1. Tạo sản phẩm `Nước điện giải POM` qua UI Selenium<br>2. Tìm kiếm và đọc lại trên bảng | `Nước điện giải POM` | Không lỗi font `?`, hiển thị đúng dấu tiếng Việt trên trình duyệt | BR-1 | P1 · EP · UI | Pass |
| **TC_NFR_05** | Post-Redirect-Get | Tải lại trang sau khi thêm/xóa sản phẩm tại `/manager/kho-dich-vu` không gửi lại form | Positive | 1. Thêm sản phẩm thành công (nhận redirect 302)<br>2. Nhấn F5 tải lại trang | Sản phẩm mới | Không tạo bản ghi trùng; thông báo `successMsg` tự xóa khỏi session sau khi hiển thị | NFR-UX | P2 · ST · UI | Pass |
| **TC_NFR_06** 🔶 | Bảo vệ CSRF | Giả thuyết lỗi bảo mật: JSP chèn `_csrf` nhưng chưa có Filter backend xác thực token | Negative | 1. Gửi `POST /manager/kho-dich-vu` hoặc `/account/update-profile` không kèm `_csrf` | Request POST thiếu `_csrf` | **Giả thuyết lỗi 🔶**: mã nguồn hiện chưa có Java Filter sinh/kiểm tra `sessionScope.csrfToken`, nên POST thiếu `_csrf` vẫn được xử lý (cần bổ sung CSRF Filter nếu triển khai production) | BR-1, §5.6 | P2 · SEC · Tĩnh | Chưa chạy |

---

## 10. TĨNH: MÃ NGUỒN (3 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_STATIC_01** 🔶 | Bảo mật cấu hình | Kiểm tra mật khẩu DB / SMTP không ghi cứng trong mã nguồn `src/main/` | Negative | 1. Rà soát `DBUtil.java`, `EmailUtil.java`, `CloudinaryUtil.java` | `src/main/java/org/example/util/` | `DBUtil.java` đọc hoàn toàn từ env/property (Đạt); **phát hiện lỗi thật 🔶**: `EmailUtil.java` (dòng 15) vẫn còn chuỗi fallback mật khẩu SMTP ghi cứng khi `SMTP_PASSWORD` chưa đặt | NFR-SEC | P1 · SEC · Tĩnh | Fail *(đã phát hiện ở `EmailUtil.java`)* |
| **TC_STATIC_02** | SQL an toàn | Kiểm tra các truy vấn `PreparedStatement` trong `dao/` không nối trực tiếp input người dùng | Negative | 1. Quét `src/main/java/org/example/dao/` tìm câu SQL có ghép chuỗi biến người dùng | `src/main/java/org/example/dao/` | Các tham số người dùng đều truyền qua `?` trong `PreparedStatement` hoặc tham số đặt tên JPQL | NFR-SEC | P1 · SEC · Tĩnh | Chưa chạy |
| **TC_STATIC_03** | Bộ lọc & Phân quyền | Xác nhận danh sách `@WebFilter` trong `src/main/java/org/example/filter/` và `WEB-INF/web.xml` | Positive | 1. Rà soát thư mục `filter/` và `WEB-INF/web.xml` | `src/main/` | Chỉ có 1 filter `EncodingAndCacheControlFilter` (`/*`); `web.xml` cấu hình `session-timeout=30`, `http-only=true`, `tracking-mode=COOKIE` | BR-1, §5.6 | P2 · SEC · Tĩnh | Pass |

---

## TỔNG HỢP

| Nhóm | Số ca | Ca P1 | Ca 🔶 | Tầng chính |
| :--- | :---: | :---: | :---: | :--- |
| 1. Đăng nhập, phân quyền, công khai | 15 | 14 | 2 | Unit, UI, Integ |
| 2. Đăng ký, OTP (Mock `123456`), Quên MK & Đối tác | 12 | 9 | 0 | Unit, UI, Integ |
| 3. Tài khoản, hồ sơ cá nhân & điểm uy tín | 7 | 4 | 0 | Integ, Manual |
| 4. Tìm kiếm, bản đồ, cửa hàng công khai & khuyến mãi | 9 | 6 | 1 | Unit, Integ, Manual |
| 5. Đặt lịch sân, giữ chỗ, giỏ hàng & đánh giá | 12 | 9 | 1 | Unit, Integ, Manual |
| 6. Kho dịch vụ, sân & khuyến mãi — Manager CRUD | 13 | 9 | 2 | Unit, UI, Integ |
| 7. Quản trị Admin & vận hành Lễ tân | 9 | 6 | 0 | Unit, Integ |
| 8. Tích hợp ngoài & chế độ giả lập (Mock Mode) | 5 | 2 | 0 | Unit, UI, Manual |
| 9. Phi chức năng và bảo mật | 6 | 4 | 1 | Unit, UI, Manual, Tĩnh |
| 10. Tĩnh: mã nguồn | 3 | 2 | 1 | Tĩnh |
| **Tổng** | **91** | **65** | **8** | |

> Các ca tầng `Unit` (JUnit 5 — 104 Pass) và `UI` (Selenium POM + TestNG — 13 Pass) đã chạy thực tế trên môi trường Mock Mode (`H2 + mock-data/vsport-mock-data.json`, OTP `123456`). Các ca `🔶` ghi nhận trung thực các rủi ro/lỗi thật đã phát hiện trong mã nguồn (`EmailUtil.java` fallback password, `DanhGiaDAOImpl` lệch trạng thái `'Hoàn thành'` vs `'Đã hoàn thành'`, `SanService.deleteSan` lỗi `DATEDIFF` trên H2, `KhoDichVuManagerServlet` NPE khi `coSoId=null`, thiếu Filter phân quyền/CSRF tập trung, `DatSanServlet` chưa lưu `promoCode`).
