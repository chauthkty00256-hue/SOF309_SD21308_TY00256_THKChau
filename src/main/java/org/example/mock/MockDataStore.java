package org.example.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.util.Constants;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kho dữ liệu giả lập (In-memory + lưu trữ JSON cục bộ tại mock-data/vsport-mock-data.json).
 * Cung cấp đầy đủ dữ liệu mẫu thực tế cho toàn bộ luồng Admin, Manager, Staff và Customer,
 * đồng thời đồng bộ lại file JSON khi có thao tác Thêm / Sửa / Xóa trong phiên chạy.
 */
public final class MockDataStore {

    private static final Logger logger = LogManager.getLogger(MockDataStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private static final ScheduledExecutorService SAVE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "vsport-mock-json-saver");
                t.setDaemon(true);
                return t;
            });
    private static ScheduledFuture<?> pendingSave;
    private static final Object SAVE_LOCK = new Object();

    private MockDataStore() {
    }

    public static void seedIfNeeded(SqlServerCompatDataSource ds) {
        try (Connection conn = ds.getRawConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Accounts")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Đã nạp dữ liệu rồi
            }
        } catch (SQLException e) {
            logger.warn("Kiểm tra dữ liệu seed thất bại: {}", e.getMessage());
        }

        Path jsonPath = MockConfig.getMockDataFilePath();
        MockSnapshot snapshot = loadFromJson(jsonPath);
        if (snapshot == null || snapshot.accounts == null || snapshot.accounts.isEmpty()) {
            snapshot = buildDefaultSnapshot();
            saveToJson(jsonPath, snapshot);
            logger.info("MockDataStore: Đã tạo dữ liệu mẫu mới và lưu tại {}", jsonPath);
        } else {
            logger.info("MockDataStore: Đã tải dữ liệu giả lập từ {}", jsonPath);
        }

        applySnapshotToDb(ds, snapshot);
        ds.setOnMutationCallback(() -> scheduleSave(ds));
    }

    public static void scheduleSave(SqlServerCompatDataSource ds) {
        synchronized (SAVE_LOCK) {
            if (pendingSave != null && !pendingSave.isDone()) {
                pendingSave.cancel(false);
            }
            pendingSave = SAVE_EXECUTOR.schedule(() -> saveSnapshotFromDb(ds), 800, TimeUnit.MILLISECONDS);
        }
    }

    public static void saveSnapshotFromDb(SqlServerCompatDataSource ds) {
        try (Connection conn = ds.getRawConnection()) {
            MockSnapshot snap = exportFromDb(conn);
            saveToJson(MockConfig.getMockDataFilePath(), snap);
        } catch (Exception e) {
            logger.warn("MockDataStore: Không thể ghi snapshot JSON: {}", e.getMessage());
        }
    }

    private static MockSnapshot loadFromJson(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, MockSnapshot.class);
        } catch (Exception e) {
            logger.warn("MockDataStore: Lỗi đọc file JSON {} ({}), sẽ tạo lại dữ liệu mặc định.", path, e.getMessage());
            return null;
        }
    }

    private static void saveToJson(Path path, MockSnapshot snapshot) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            }
        } catch (IOException e) {
            logger.warn("MockDataStore: Lỗi ghi file JSON {}: {}", path, e.getMessage());
        }
    }

    private static MockSnapshot buildDefaultSnapshot() {
        MockSnapshot s = new MockSnapshot();
        String defaultPassHash = BCrypt.hashpw("123456", BCrypt.gensalt(10));
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        String tomorrowStr = today.plusDays(1).toString();
        String yesterdayStr = today.minusDays(1).toString();
        String twoDaysAgoStr = today.minusDays(2).toString();
        String utcPlus30m = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30).toString();
        String nowStr = LocalDateTime.now().toString();

        // Roles
        s.roles.add(new RoleRow(1, "Admin"));
        s.roles.add(new RoleRow(2, "Quản lý"));
        s.roles.add(new RoleRow(3, "Khách hàng"));
        s.roles.add(new RoleRow(4, "Lễ tân"));

        // Sports
        s.sports.add(new SportRow(1, "Bóng đá"));
        s.sports.add(new SportRow(2, "Cầu lông"));
        s.sports.add(new SportRow(3, "Pickleball"));
        s.sports.add(new SportRow(4, "Tennis"));
        s.sports.add(new SportRow(5, "Bóng rổ"));
        s.sports.add(new SportRow(6, "Bóng chuyền"));

        // Accounts (Mật khẩu chung cho tất cả tài khoản demo: 123456)
        s.accounts.add(new AccountRow(1, "admin", defaultPassHash, "Quản trị viên V-SPORT", "0901000001", "admin@vsport.vn",
                Constants.ROLE_ADMIN, null, 100, null, "Nam", "/assets/images/vsport/community/community-01.jpg", 1));
        s.accounts.add(new AccountRow(2, "manager1", defaultPassHash, "Nguyễn Minh Quản", "0901000002", "manager1@vsport.vn",
                Constants.ROLE_MANAGER, 1, 100, "VCB", "Nam", "/assets/images/vsport/community/community-02.jpg", 1));
        s.accounts.add(new AccountRow(3, "khach1", defaultPassHash, "Trần Văn Hùng", "0901000003", "khach1@vsport.vn",
                Constants.ROLE_KHACH_HANG, null, 96, null, "Nam", "/assets/images/vsport/community/community-03.jpg", 1));
        s.accounts.add(new AccountRow(4, "staff1", defaultPassHash, "Lê Thị Lan", "0901000004", "staff1@vsport.vn",
                Constants.ROLE_LE_TAN, 1, 100, null, "Nữ", "/assets/images/vsport/community/team-01.jpg", 2));
        s.accounts.add(new AccountRow(5, "manager2", defaultPassHash, "Phạm Hoàng Long", "0901000005", "manager2@vsport.vn",
                Constants.ROLE_MANAGER, 2, 100, "MB", "Nam", "/assets/images/vsport/community/community-02.jpg", 3));
        s.accounts.add(new AccountRow(6, "khach2", defaultPassHash, "Nguyễn Thảo Vy", "0901000006", "khach2@vsport.vn",
                Constants.ROLE_KHACH_HANG, null, 88, null, "Nữ", "/assets/images/vsport/community/community-01.jpg", 2));

        // Favorite sports
        s.favoriteSports.add(new FavoriteSportRow(3, 1));
        s.favoriteSports.add(new FavoriteSportRow(6, 2));

        // Facilities (CoSo)
        s.facilities.add(new FacilityRow(
                1, "V-SPORT Arena Quận 1", "128 Nguyễn Du, Phường Bến Thành, Quận 1, TP.HCM", "02838221100",
                "Đang hoạt động", "06:00:00", "23:00:00", "/assets/images/vsport/courts/court-football.jpg",
                "Tổ hợp sân bóng đá cỏ nhân tạo 5v5, 7v7 và cầu lông chuẩn thi đấu ngay trung tâm Quận 1.",
                "Bóng đá, Cầu lông", 6, 2, 10.7734120, 106.6967250));
        s.facilities.add(new FacilityRow(
                2, "V-SPORT Center Thủ Đức", "45 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP.HCM", "02837223344",
                "Đang hoạt động", "05:30:00", "22:30:00", "/assets/images/vsport/courts/court-pickleball.jpg",
                "Trung tâm Pickleball và Cầu lông hiện đại với mái che cách nhiệt và hệ thống đèn LED chống chói.",
                "Pickleball, Cầu lông, Tennis", 8, 5, 10.8505120, 106.7651100));
        s.facilities.add(new FacilityRow(
                3, "V-SPORT Club Vũng Tàu", "88 Thùy Vân, Phường Thắng Tam, TP. Vũng Tàu", "02543855667",
                "Đang hoạt động", "06:00:00", "22:00:00", "/assets/images/vsport/courts/court-tennis.jpg",
                "Cụm sân Tennis & Bóng đá view biển thoáng mát, có khu vực nghỉ ngơi và canteen đầy đủ.",
                "Tennis, Bóng đá", 4, 2, 10.3365800, 107.0875400));

        // Capabilities
        int capId = 1;
        for (int coSoId = 1; coSoId <= 3; coSoId++) {
            s.capabilities.add(new CapabilityRow(capId++, coSoId, Constants.CAPABILITY_SAN, Constants.CAPABILITY_STATUS_APPROVED));
            s.capabilities.add(new CapabilityRow(capId++, coSoId, Constants.CAPABILITY_SAN_PHAM, Constants.CAPABILITY_STATUS_APPROVED));
            s.capabilities.add(new CapabilityRow(capId++, coSoId, Constants.CAPABILITY_THUE_DUNG_CU, Constants.CAPABILITY_STATUS_APPROVED));
            s.capabilities.add(new CapabilityRow(capId++, coSoId, Constants.CAPABILITY_DO_AN_NUOC_UONG, Constants.CAPABILITY_STATUS_APPROVED));
            s.capabilities.add(new CapabilityRow(capId++, coSoId, Constants.CAPABILITY_DICH_VU_THE_THAO, Constants.CAPABILITY_STATUS_APPROVED));
        }

        // Bank accounts
        s.bankAccounts.add(new BankAccountRow(1, "Vietcombank", "VCB", "V-SPORT ARENA Q1", "0071001234567"));
        s.bankAccounts.add(new BankAccountRow(2, "MB Bank", "MB", "V-SPORT CENTER TD", "0901234567890"));
        s.bankAccounts.add(new BankAccountRow(3, "Techcombank", "TCB", "V-SPORT CLUB VT", "1903456789012"));

        // Court types
        s.courtTypes.add(new CourtTypeRow(1, 1, "Sân Bóng đá 5 người", 300000, 420000, "17:30:00", "23:00:00", 1));
        s.courtTypes.add(new CourtTypeRow(2, 1, "Sân Bóng đá 7 người", 550000, 750000, "17:30:00", "23:00:00", 1));
        s.courtTypes.add(new CourtTypeRow(3, 2, "Sân Cầu lông thảm PVC", 90000, 130000, "17:00:00", "23:00:00", 1));
        s.courtTypes.add(new CourtTypeRow(4, 3, "Sân Pickleball chuẩn Mỹ", 140000, 190000, "17:30:00", "22:30:00", 2));
        s.courtTypes.add(new CourtTypeRow(5, 2, "Sân Cầu lông VIP", 110000, 150000, "17:00:00", "22:30:00", 2));
        s.courtTypes.add(new CourtTypeRow(6, 4, "Sân Tennis cứng ngoài trời", 220000, 320000, "17:30:00", "22:00:00", 3));

        // Courts
        s.courts.add(new CourtRow(1, "Sân 5A - Cỏ FIFA", 1, 1, "Sẵn sàng", "Mặt cỏ nhân tạo êm, thoát nước nhanh", "/assets/images/vsport/courts/court-football.jpg"));
        s.courts.add(new CourtRow(2, "Sân 5B - Cỏ FIFA", 1, 1, "Sẵn sàng", "Gần khu vực khán đài và nước uống", "/assets/images/vsport/courts/court-football.jpg"));
        s.courts.add(new CourtRow(3, "Sân 7A - Trung tâm", 2, 1, "Sẵn sàng", "Sân 7 người tiêu chuẩn thi đấu phong trào", "/assets/images/vsport/courts/court-football.jpg"));
        s.courts.add(new CourtRow(4, "Sân Cầu lông 01", 3, 1, "Sẵn sàng", "Thảm Yonex chống trượt, trần cao 10m", "/assets/images/vsport/courts/court-badminton.jpg"));
        s.courts.add(new CourtRow(5, "Sân Cầu lông 02", 3, 1, "Sẵn sàng", "Có điều hòa và khu thay đồ riêng", "/assets/images/vsport/courts/court-badminton.jpg"));
        s.courts.add(new CourtRow(6, "Pickleball Court 01", 4, 2, "Sẵn sàng", "Mặt sân Acrylic 6 lớp chuẩn USAPA", "/assets/images/vsport/courts/court-pickleball.jpg"));
        s.courts.add(new CourtRow(7, "Pickleball Court 02", 4, 2, "Sẵn sàng", "Mái che toàn phần, cho thuê vợt tại chỗ", "/assets/images/vsport/courts/court-pickleball.jpg"));
        s.courts.add(new CourtRow(8, "Sân Cầu lông VIP 01", 5, 2, "Sẵn sàng", "Sân thảm thi đấu chuyên nghiệp", "/assets/images/vsport/courts/court-badminton.jpg"));
        s.courts.add(new CourtRow(9, "Sân Tennis Biển 01", 6, 3, "Sẵn sàng", "View biển Thùy Vân thoáng mát", "/assets/images/vsport/courts/court-tennis.jpg"));

        // Product Categories
        s.productCategories.add(new CategoryRow(1, "Nước giải khát"));
        s.productCategories.add(new CategoryRow(2, "Dụng cụ thể thao"));
        s.productCategories.add(new CategoryRow(3, "Thuê thiết bị"));
        s.productCategories.add(new CategoryRow(4, "Phụ kiện"));

        // Products & Services
        s.products.add(new ProductRow(1, 1, 1, "Nước điện giải Pocari Sweat 500ml", 20000, "Chai", 120, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "SP-NUOC-01", 12000, "Bù khoáng nhanh khi vận động cường độ cao", "/assets/images/vsport/categories/basketball.jpg"));
        s.products.add(new ProductRow(2, 1, 1, "Nước suối Aquafina 500ml", 10000, "Chai", 200, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "SP-NUOC-02", 5000, "Nước tinh khiết ướp lạnh", "/assets/images/vsport/categories/basketball.jpg"));
        s.products.add(new ProductRow(3, 3, 1, "Thuê áo Pitch (Bộ 7 cái)", 35000, "Bộ", 25, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "DV-AO-01", 0, "Áo chia đội thoáng khí giặt sạch mỗi ngày", "/assets/images/vsport/categories/football.jpg"));
        s.products.add(new ProductRow(4, 3, 1, "Thuê vợt cầu lông Yonex", 40000, "Cây/ca", 18, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "DV-VOT-01", 0, "Căng sẵn lưới 10.5kg", "/assets/images/vsport/categories/badminton.jpg"));
        s.products.add(new ProductRow(5, 2, 1, "Ống cầu lông Thành Công (12 quả)", 245000, "Ống", 40, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "SP-CAU-01", 190000, "Cầu lông tự nhiên bay đầm, bền cánh", "/assets/images/vsport/categories/badminton.jpg"));
        s.products.add(new ProductRow(6, 4, 1, "Quấn cán vợt chống trượt", 25000, "Cuộn", 85, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "SP-PK-01", 12000, "Thấm hút mồ hôi tốt", "/assets/images/vsport/categories/tennis.jpg"));
        s.products.add(new ProductRow(7, 3, 2, "Thuê vợt Pickleball Carbon", 50000, "Cây/ca", 20, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "DV-PKB-01", 0, "Vợt mặt carbon nhám kiểm soát bóng tốt", "/assets/images/vsport/categories/pickleball.jpg"));
        s.products.add(new ProductRow(8, 2, 2, "Bóng Pickleball Franklin X-40 (Hộp 3 quả)", 210000, "Hộp", 35, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "SP-PKB-02", 150000, "Bóng thi đấu tiêu chuẩn ngoài trời", "/assets/images/vsport/categories/pickleball.jpg"));
        s.products.add(new ProductRow(9, 1, 2, "Trà chanh mật ong ướp lạnh", 22000, "Ly", 60, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "SP-NUOC-03", 10000, "Giải nhiệt tức thì sau trận đấu", "/assets/images/vsport/categories/basketball.jpg"));
        s.products.add(new ProductRow(10, 3, 3, "Thuê vợt Tennis Babolat", 60000, "Cây/ca", 12, Constants.TRANG_THAI_SP_DANG_KINH_DOANH, "DV-TEN-01", 0, "Trọng lượng 285g dễ đánh", "/assets/images/vsport/categories/tennis.jpg"));

        // Bookings (LichDatSan) — đủ các trạng thái để test Giỏ hàng, Lịch sử, Check-in, Hóa đơn, Ghép kèo
        // Booking 1: Đã hoàn thành (hôm qua) -> tạo doanh thu + đánh giá
        s.bookings.add(new BookingRow(1, 3, 1, yesterdayStr, "18:00:00", "19:30:00", true, 630000,
                Constants.TRANG_THAI_DAT_SAN_DA_HOAN_THANH, "Đặt sân đá giao hữu cuối tuần", "ONLINE",
                "18:00:00", "19:30:00", null, "PayOS"));
        // Booking 2: Đang sử dụng (hôm nay, ngay bây giờ) -> hiện trên màn Check-in của Staff!
        LocalTime nowHour = LocalTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalTime activeStart = nowHour.isBefore(LocalTime.of(6, 0)) ? LocalTime.of(8, 0) : nowHour;
        LocalTime activeEnd = activeStart.plusHours(1).plusMinutes(30);
        if (activeEnd.isBefore(activeStart)) {
            activeStart = LocalTime.of(19, 0);
            activeEnd = LocalTime.of(20, 30);
        }
        s.bookings.add(new BookingRow(2, 6, 4, todayStr, activeStart.toString(), activeEnd.toString(), true, 195000,
                Constants.TRANG_THAI_DAT_SAN_DANG_SU_DUNG, "Khách đã check-in vào sân", "ONLINE",
                activeStart.toString(), null, null, "Tiền mặt"));
        // Booking 3: Đã xác nhận (ngày mai) -> dùng cho Ghép kèo & lịch sắp tới
        s.bookings.add(new BookingRow(3, 3, 3, tomorrowStr, "19:00:00", "20:30:00", true, 1125000,
                Constants.TRANG_THAI_DAT_SAN_DA_XAC_NHAN, "Tìm thêm đối đá sân 7", "ONLINE",
                null, null, null, "PayOS"));
        // Booking 4: Chờ thanh toán (còn hạn hold) -> hiện trong Giỏ hàng của khach1!
        s.bookings.add(new BookingRow(4, 3, 6, tomorrowStr, "17:30:00", "19:00:00", true, 285000,
                Constants.TRANG_THAI_DAT_SAN_CHO_THANH_TOAN, "Giữ chỗ Pickleball buổi chiều", "ONLINE",
                null, null, utcPlus30m, null));
        // Booking 5: Chờ xác nhận -> hiện trong Giỏ hàng & Quản lý đặt sân
        s.bookings.add(new BookingRow(5, 3, 2, tomorrowStr, "20:00:00", "21:00:00", true, 420000,
                Constants.TRANG_THAI_DAT_SAN_CHO_XAC_NHAN, "Thanh toán tại quầy khi đến sân", "ONLINE",
                null, null, null, null));
        // Booking 6: Đã hoàn thành (2 ngày trước) tại cơ sở 2
        s.bookings.add(new BookingRow(6, 6, 6, twoDaysAgoStr, "16:00:00", "17:30:00", false, 210000,
                Constants.TRANG_THAI_DAT_SAN_DA_HOAN_THANH, "Tập Pickleball cùng bạn", "ONLINE",
                "16:00:00", "17:30:00", null, "PayOS"));

        // Booking Services
        s.bookingServices.add(new BookingServiceRow(1, 3, 1, 4, 20000, 80000, "PENDING", "Ướp lạnh sẵn giúp đội"));
        s.bookingServices.add(new BookingServiceRow(2, 3, 3, 1, 35000, 35000, "PENDING", "Áo màu cam"));

        // Invoices
        s.invoices.add(new InvoiceRow(1, 1, 3, 4, yesterdayStr + " 19:35:00", 630000, 55000, 0, null, 0, 685000,
                Constants.PT_PAYOS, Constants.TRANG_THAI_HOA_DON_DA_TT, "MAIN"));
        s.invoices.add(new InvoiceRow(2, 2, 6, 4, todayStr + " " + activeStart + ":00", 195000, 40000, 0, null, 0, 235000,
                Constants.PT_TIEN_MAT, Constants.TRANG_THAI_HOA_DON_CHUA_TT, "MAIN"));
        s.invoices.add(new InvoiceRow(3, 6, 6, 5, twoDaysAgoStr + " 17:35:00", 210000, 50000, 0, null, 0, 260000,
                Constants.PT_PAYOS, Constants.TRANG_THAI_HOA_DON_DA_TT, "MAIN"));

        // Invoice Details
        s.invoiceDetails.add(new InvoiceDetailRow(1, 1, 1, 1, 20000, 20000));
        s.invoiceDetails.add(new InvoiceDetailRow(2, 1, 3, 1, 35000, 35000));
        s.invoiceDetails.add(new InvoiceDetailRow(3, 2, 4, 1, 40000, 40000));
        s.invoiceDetails.add(new InvoiceDetailRow(4, 3, 7, 1, 50000, 50000));

        // Promotions (KhuyenMai)
        s.promotions.add(new PromotionRow(1, "VSPORT10", "Giảm 10% cho mọi lượt đặt sân tại V-SPORT Arena Q1",
                "PERCENT", 10, today.minusDays(7).toString(), today.plusDays(30).toString(), 200, 18, 1, "Hoạt động", 200000, 100000, true));
        s.promotions.add(new PromotionRow(2, "PICKLE50K", "Giảm ngay 50.000đ khi đặt sân Pickleball từ 250.000đ",
                "FIXED", 50000, today.minusDays(5).toString(), today.plusDays(45).toString(), 150, 9, 2, "Hoạt động", 250000, 50000, true));
        s.promotions.add(new PromotionRow(3, "WELCOME20", "Ưu đãi thành viên mới giảm 20% tối đa 80.000đ",
                "PERCENT", 20, today.minusDays(10).toString(), today.plusDays(60).toString(), 500, 42, null, "Hoạt động", 150000, 80000, true));

        // Reviews (DanhGia)
        s.reviews.add(new ReviewRow(1, 1, 3, null, 5, "Mặt cỏ mới rất êm, đèn sáng rõ, nhân viên hỗ trợ nhiệt tình!", nowStr));
        s.reviews.add(new ReviewRow(2, 6, 6, null, 5, "Sân Pickleball đẹp, có mái che nên chơi chiều không bị nắng.", nowStr));

        // Teams & Members
        s.teams.add(new TeamRow(1, "FC Bến Thành", "Đội bóng đá phong trào 7v7 chơi tối thứ 4 & thứ 7 hàng tuần", 1, 3, "Quận 1, TP.HCM", "/assets/images/vsport/community/team-01.jpg", 14));
        s.teams.add(new TeamRow(2, "Thủ Đức Smashers", "CLB cầu lông & pickleball giao lưu trình độ trung bình - khá", 2, 6, "TP. Thủ Đức, TP.HCM", "/assets/images/vsport/community/community-02.jpg", 12));
        s.teamMembers.add(new TeamMemberRow(1, 1, 3, "CAPTAIN", "ACTIVE"));
        s.teamMembers.add(new TeamMemberRow(2, 1, 6, "MEMBER", "ACTIVE"));
        s.teamMembers.add(new TeamMemberRow(3, 2, 6, "CAPTAIN", "ACTIVE"));

        // Matchmaking (GhepKeo)
        s.matchmaking.add(new MatchRow(1, 3, 3, 1,
                "[VS_META]{\"cap\":6,\"approve\":\"auto\",\"minRep\":60}[/VS_META]Tìm thêm 4-5 anh em đá giao hữu sân 7A tối mai, chia đều tiền sân!",
                "Trung bình - Khá", "Đang mở"));

        // Staff Shifts (CaLamViec)
        s.shifts.add(new ShiftRow(1, 4, 1, todayStr, "06:00:00", "14:00:00", "Ca sáng trực quầy & check-in", 2, true, "Ca sáng", "Lễ tân", "Published"));
        s.shifts.add(new ShiftRow(2, 4, 1, todayStr, "14:00:00", "22:00:00", "Ca chiều tối giờ cao điểm", 2, true, "Ca chiều", "Lễ tân", "Published"));
        s.shifts.add(new ShiftRow(3, 4, 1, tomorrowStr, "14:00:00", "22:00:00", "Trực sân cuối tuần", 3, true, "Ca chiều", "Lễ tân", "Published"));

        // Notifications (ThongBao)
        s.notifications.add(new NotificationRow(1, 3, "Đặt sân thành công #3", "Lịch đặt Sân 7A ngày mai (19:00 - 20:30) đã được xác nhận.", "BOOKING_CONFIRMED", false, "/customer/lich-su-dat-san"));
        s.notifications.add(new NotificationRow(2, 3, "Nhắc thanh toán giữ chỗ #4", "Lịch đặt Pickleball Court 01 đang chờ thanh toán trong giỏ hàng.", "PAYMENT_REMINDER", false, "/customer/gio-hang"));
        s.notifications.add(new NotificationRow(3, 2, "Báo cáo doanh thu V-SPORT", "Cơ sở V-SPORT Arena Quận 1 đã ghi nhận lượt đặt mới.", "SYSTEM", false, "/manager/dashboard"));

        // Court QR
        s.courtQrs.add(new CourtQrRow(1, 1, UUID.randomUUID().toString(), "SAN5A1", "ACTIVE", 2));
        s.courtQrs.add(new CourtQrRow(2, 4, UUID.randomUUID().toString(), "CAUL01", "ACTIVE", 2));
        s.courtQrs.add(new CourtQrRow(3, 6, UUID.randomUUID().toString(), "PKB001", "ACTIVE", 5));

        return s;
    }

    private static void applySnapshotToDb(SqlServerCompatDataSource ds, MockSnapshot s) {
        try (Connection conn = ds.getRawConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Roles (RoleID, RoleName) VALUES (?, ?)")) {
                    for (RoleRow r : s.roles) {
                        ps.setInt(1, r.roleId);
                        ps.setString(2, r.roleName);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO MonTheThao (MonTheThaoID, TenMon) VALUES (?, ?)")) {
                    for (SportRow r : s.sports) {
                        ps.setInt(1, r.monTheThaoId);
                        ps.setString(2, r.tenMon);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO CoSo (CoSoID, TenCoSo, DiaChi, SoDienThoai, TrangThai, GioMoCua, GioDongCua, HinhAnh, MoTa, LoaiHinhKinhDoanh, SoLuongSanDuKien, AccountID_QuanLy, IsDeleted, ViDo, KinhDo) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?)")) {
                    for (FacilityRow r : s.facilities) {
                        ps.setInt(1, r.coSoId);
                        ps.setString(2, r.tenCoSo);
                        ps.setString(3, r.diaChi);
                        ps.setString(4, r.soDienThoai);
                        ps.setString(5, r.trangThai);
                        ps.setTime(6, Time.valueOf(r.gioMoCua));
                        ps.setTime(7, Time.valueOf(r.gioDongCua));
                        ps.setString(8, r.hinhAnh);
                        ps.setString(9, r.moTa);
                        ps.setString(10, r.loaiHinhKinhDoanh);
                        ps.setInt(11, r.soLuongSanDuKien);
                        setIntOrNull(ps, 12, r.accountIdQuanLy);
                        ps.setBigDecimal(13, BigDecimal.valueOf(r.viDo));
                        ps.setBigDecimal(14, BigDecimal.valueOf(r.kinhDo));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Accounts (AccountID, Username, Password, FullName, PhoneNumber, Email, RoleID, CoSoID, DiemUyTin, MaNganHang, GioiTinh, AvatarUrl, MonTheThaoYeuThichID, FailedLoginCount, IsLocked, IsDeleted, NhanThongBaoSOS, NhanThongBaoMarketing) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, FALSE, FALSE, TRUE, TRUE)")) {
                    for (AccountRow r : s.accounts) {
                        ps.setInt(1, r.accountId);
                        ps.setString(2, r.username);
                        ps.setString(3, r.password);
                        ps.setString(4, r.fullName);
                        ps.setString(5, r.phoneNumber);
                        ps.setString(6, r.email);
                        setIntOrNull(ps, 7, r.roleId);
                        setIntOrNull(ps, 8, r.coSoId);
                        ps.setInt(9, r.diemUyTin);
                        ps.setString(10, r.maNganHang);
                        ps.setString(11, r.gioiTinh);
                        ps.setString(12, r.avatarUrl);
                        setIntOrNull(ps, 13, r.monTheThaoYeuThichId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                logger.info("MockDataStore: Đã kích hoạt sẵn {} tài khoản mẫu (IsLocked=FALSE, IsDeleted=FALSE).", s.accounts.size());

                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO MonTheThaoYeuThich (AccountID, MonTheThaoID) VALUES (?, ?)")) {
                    for (FavoriteSportRow r : s.favoriteSports) {
                        ps.setInt(1, r.accountId);
                        ps.setInt(2, r.monTheThaoId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO CoSoCapability (CapabilityID, CoSoID, CapabilityType, TrangThai) VALUES (?, ?, ?, ?)")) {
                    for (CapabilityRow r : s.capabilities) {
                        ps.setInt(1, r.capabilityId);
                        ps.setInt(2, r.coSoId);
                        ps.setString(3, r.capabilityType);
                        ps.setString(4, r.trangThai);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO CoSoNganHang (CoSoID, BankName, BankShortCode, AccountName, AccountNumber) VALUES (?, ?, ?, ?, ?)")) {
                    for (BankAccountRow r : s.bankAccounts) {
                        ps.setInt(1, r.coSoId);
                        ps.setString(2, r.bankName);
                        ps.setString(3, r.bankShortCode);
                        ps.setString(4, r.accountName);
                        ps.setString(5, r.accountNumber);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO LoaiSan (LoaiSanID, MonTheThaoID, TenLoai, GiaKhongDen, GiaCoDen, GioBatDauLenDen, GioKetThucLenDen, CoSoID, IsDeleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE)")) {
                    for (CourtTypeRow r : s.courtTypes) {
                        ps.setInt(1, r.loaiSanId);
                        ps.setInt(2, r.monTheThaoId);
                        ps.setString(3, r.tenLoai);
                        ps.setBigDecimal(4, BigDecimal.valueOf(r.giaKhongDen));
                        ps.setBigDecimal(5, BigDecimal.valueOf(r.giaCoDen));
                        ps.setTime(6, Time.valueOf(r.gioBatDauLenDen));
                        ps.setTime(7, Time.valueOf(r.gioKetThucLenDen));
                        setIntOrNull(ps, 8, r.coSoId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO San (SanID, TenSan, LoaiSanID, CoSoID, TrangThai, MoTa, HinhAnh, IsDeleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, FALSE)")) {
                    for (CourtRow r : s.courts) {
                        ps.setInt(1, r.sanId);
                        ps.setString(2, r.tenSan);
                        ps.setInt(3, r.loaiSanId);
                        ps.setInt(4, r.coSoId);
                        ps.setString(5, r.trangThai);
                        ps.setString(6, r.moTa);
                        ps.setString(7, r.hinhAnh);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO DanhMucSanPham (DanhMucID, TenDanhMuc) VALUES (?, ?)")) {
                    for (CategoryRow r : s.productCategories) {
                        ps.setInt(1, r.danhMucId);
                        ps.setString(2, r.tenDanhMuc);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO SanPham_DichVu (SanPhamID, DanhMucID, CoSoID, TenSanPham, DonGia, DonViTinh, SoLuongTon, TrangThai, SkuCode, GiaNhap, MoTa, HinhAnh, IsDeleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)")) {
                    for (ProductRow r : s.products) {
                        ps.setInt(1, r.sanPhamId);
                        ps.setInt(2, r.danhMucId);
                        ps.setInt(3, r.coSoId);
                        ps.setString(4, r.tenSanPham);
                        ps.setDouble(5, r.donGia);
                        ps.setString(6, r.donViTinh);
                        ps.setInt(7, r.soLuongTon);
                        ps.setString(8, r.trangThai);
                        ps.setString(9, r.skuCode);
                        ps.setDouble(10, r.giaNhap);
                        ps.setString(11, r.moTa);
                        ps.setString(12, r.hinhAnh);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO LichDatSan (DatSanID, AccountID, SanID, NgayDat, GioBatDau, GioKetThuc, ApDungGiaCoDen, TongTienDuKien, TrangThai, GhiChu, NguonDatSan, IsDeleted, actual_start_time, actual_end_time, HoldExpiresAt, PaymentMethodConfirmed) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?, ?, ?)")) {
                    for (BookingRow r : s.bookings) {
                        ps.setInt(1, r.datSanId);
                        setIntOrNull(ps, 2, r.accountId);
                        setIntOrNull(ps, 3, r.sanId);
                        ps.setDate(4, Date.valueOf(r.ngayDat));
                        ps.setTime(5, Time.valueOf(normalizeTime(r.gioBatDau)));
                        ps.setTime(6, Time.valueOf(normalizeTime(r.gioKetThuc)));
                        ps.setBoolean(7, r.apDungGiaCoDen);
                        ps.setBigDecimal(8, BigDecimal.valueOf(r.tongTienDuKien));
                        ps.setString(9, r.trangThai);
                        ps.setString(10, r.ghiChu);
                        ps.setString(11, r.nguonDatSan);
                        if (r.actualStartTime != null) ps.setTime(12, Time.valueOf(normalizeTime(r.actualStartTime))); else ps.setNull(12, Types.TIME);
                        if (r.actualEndTime != null) ps.setTime(13, Time.valueOf(normalizeTime(r.actualEndTime))); else ps.setNull(13, Types.TIME);
                        if (r.holdExpiresAt != null) ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.parse(r.holdExpiresAt))); else ps.setNull(14, Types.TIMESTAMP);
                        ps.setString(15, r.paymentMethodConfirmed);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO LichDatSan_DichVu (Id, DatSanID, SanPhamID, Quantity, UnitPrice, TotalPrice, Status, Note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (BookingServiceRow r : s.bookingServices) {
                        ps.setInt(1, r.id);
                        ps.setInt(2, r.datSanId);
                        ps.setInt(3, r.sanPhamId);
                        ps.setInt(4, r.quantity);
                        ps.setBigDecimal(5, BigDecimal.valueOf(r.unitPrice));
                        ps.setBigDecimal(6, BigDecimal.valueOf(r.totalPrice));
                        ps.setString(7, r.status);
                        ps.setString(8, r.note);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO HoaDon (HoaDonID, DatSanID, AccountID_KhachHang, AccountID_NhanVien, NgayLap, TongTienSan, TongTienDichVu, PhiGuiXe, KhuyenMaiID, GiamGia, TongThanhToan, PhuongThucThanhToan, TrangThaiThanhToan, LoaiHoaDon) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (InvoiceRow r : s.invoices) {
                        ps.setInt(1, r.hoaDonId);
                        setIntOrNull(ps, 2, r.datSanId);
                        setIntOrNull(ps, 3, r.accountIdKhachHang);
                        setIntOrNull(ps, 4, r.accountIdNhanVien);
                        ps.setTimestamp(5, Timestamp.valueOf(normalizeTimestamp(r.ngayLap)));
                        ps.setDouble(6, r.tongTienSan);
                        ps.setDouble(7, r.tongTienDichVu);
                        ps.setDouble(8, r.phiGuiXe);
                        setIntOrNull(ps, 9, r.khuyenMaiId);
                        ps.setDouble(10, r.giamGia);
                        ps.setDouble(11, r.tongThanhToan);
                        ps.setString(12, r.phuongThucThanhToan);
                        ps.setString(13, r.trangThaiThanhToan);
                        ps.setString(14, r.loaiHoaDon);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ChiTietHoaDon (ChiTietID, HoaDonID, SanPhamID, SoLuong, DonGiaTaiThoiDiemBan, ThanhTien) VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (InvoiceDetailRow r : s.invoiceDetails) {
                        ps.setInt(1, r.chiTietId);
                        ps.setInt(2, r.hoaDonId);
                        ps.setInt(3, r.sanPhamId);
                        ps.setInt(4, r.soLuong);
                        ps.setDouble(5, r.donGiaTaiThoiDiemBan);
                        ps.setDouble(6, r.thanhTien);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO KhuyenMai (KhuyenMaiID, MaCode, MoTa, LoaiGiam, GiaTriGiam, NgayBatDau, NgayKetThuc, SoLanToiDa, SoLanDaDung, CoSoID, TrangThai, GiaTriToiThieu, GiamToiDa, HienThiCongKhai) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (PromotionRow r : s.promotions) {
                        ps.setInt(1, r.khuyenMaiId);
                        ps.setString(2, r.maCode);
                        ps.setString(3, r.moTa);
                        ps.setString(4, r.loaiGiam);
                        ps.setDouble(5, r.giaTriGiam);
                        ps.setDate(6, Date.valueOf(r.ngayBatDau));
                        ps.setDate(7, Date.valueOf(r.ngayKetThuc));
                        setIntOrNull(ps, 8, r.soLanToiDa);
                        ps.setInt(9, r.soLanDaDung);
                        setIntOrNull(ps, 10, r.coSoId);
                        ps.setString(11, r.trangThai);
                        ps.setBigDecimal(12, BigDecimal.valueOf(r.giaTriToiThieu));
                        ps.setBigDecimal(13, BigDecimal.valueOf(r.giamToiDa));
                        ps.setBoolean(14, r.hienThiCongKhai);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO DanhGia (DanhGiaID, DatSanID, AccountID_NguoiDanhGia, AccountID_NguoiBiDanhGia, SoSao, BinhLuan, NgayDanhGia) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    for (ReviewRow r : s.reviews) {
                        ps.setInt(1, r.danhGiaId);
                        ps.setInt(2, r.datSanId);
                        ps.setInt(3, r.accountIdNguoiDanhGia);
                        setIntOrNull(ps, 4, r.accountIdNguoiBiDanhGia);
                        ps.setInt(5, r.soSao);
                        ps.setString(6, r.binhLuan);
                        ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.parse(r.ngayDanhGia)));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Teams (TeamID, TeamName, Description, SportID, CaptainAccountID, LocationText, AvatarPath, MaxMembers, Status, IsDeleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', FALSE)")) {
                    for (TeamRow r : s.teams) {
                        ps.setInt(1, r.teamId);
                        ps.setString(2, r.teamName);
                        ps.setString(3, r.description);
                        ps.setInt(4, r.sportId);
                        ps.setInt(5, r.captainAccountId);
                        ps.setString(6, r.locationText);
                        ps.setString(7, r.avatarPath);
                        ps.setInt(8, r.maxMembers);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO TeamMembers (TeamMemberID, TeamID, AccountID, MemberRole, MemberStatus) VALUES (?, ?, ?, ?, ?)")) {
                    for (TeamMemberRow r : s.teamMembers) {
                        ps.setInt(1, r.teamMemberId);
                        ps.setInt(2, r.teamId);
                        ps.setInt(3, r.accountId);
                        ps.setString(4, r.memberRole);
                        ps.setString(5, r.memberStatus);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO GhepKeo (KeoID, DatSanID, AccountID_NguoiTao, MonTheThaoID, MoTa, TrinhDo, TrangThai) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    for (MatchRow r : s.matchmaking) {
                        ps.setInt(1, r.keoId);
                        ps.setInt(2, r.datSanId);
                        ps.setInt(3, r.accountIdNguoiTao);
                        ps.setInt(4, r.monTheThaoId);
                        ps.setString(5, r.moTa);
                        ps.setString(6, r.trinhDo);
                        ps.setString(7, r.trangThai);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO CaLamViec (CaLamViecID, AccountID, CoSoID, NgayLam, GioBatDau, GioKetThuc, GhiChu, Thu, IsPublished, TenCa, ViTri, TrangThai, IsDeleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)")) {
                    for (ShiftRow r : s.shifts) {
                        ps.setInt(1, r.caLamViecId);
                        ps.setInt(2, r.accountId);
                        ps.setInt(3, r.coSoId);
                        ps.setDate(4, Date.valueOf(r.ngayLam));
                        ps.setTime(5, Time.valueOf(normalizeTime(r.gioBatDau)));
                        ps.setTime(6, Time.valueOf(normalizeTime(r.gioKetThuc)));
                        ps.setString(7, r.ghiChu);
                        setIntOrNull(ps, 8, r.thu);
                        ps.setBoolean(9, r.isPublished);
                        ps.setString(10, r.tenCa);
                        ps.setString(11, r.viTri);
                        ps.setString(12, r.trangThai);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ThongBao (ThongBaoID, AccountID, TieuDe, NoiDung, LoaiThongBao, DaDoc, DuongDan, IsDeleted) VALUES (?, ?, ?, ?, ?, ?, ?, FALSE)")) {
                    for (NotificationRow r : s.notifications) {
                        ps.setInt(1, r.thongBaoId);
                        ps.setInt(2, r.accountId);
                        ps.setString(3, r.tieuDe);
                        ps.setString(4, r.noiDung);
                        ps.setString(5, r.loaiThongBao);
                        ps.setBoolean(6, r.daDoc);
                        ps.setString(7, r.duongDan);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO SanQR (SanQRID, SanID, Token, ShortCode, TrangThai, CreatedBy, RegenerateCount) VALUES (?, ?, CAST(? AS UUID), ?, ?, ?, 0)")) {
                    for (CourtQrRow r : s.courtQrs) {
                        ps.setInt(1, r.sanQrId);
                        ps.setInt(2, r.sanId);
                        ps.setString(3, r.token);
                        ps.setString(4, r.shortCode);
                        ps.setString(5, r.trangThai);
                        setIntOrNull(ps, 6, r.createdBy);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Đặt lại bộ đếm IDENTITY để thao tác INSERT mới không bị trùng khóa
                resetIdentity(conn, "Roles", "RoleID");
                resetIdentity(conn, "MonTheThao", "MonTheThaoID");
                resetIdentity(conn, "CoSo", "CoSoID");
                resetIdentity(conn, "Accounts", "AccountID");
                resetIdentity(conn, "CoSoCapability", "CapabilityID");
                resetIdentity(conn, "LoaiSan", "LoaiSanID");
                resetIdentity(conn, "San", "SanID");
                resetIdentity(conn, "DanhMucSanPham", "DanhMucID");
                resetIdentity(conn, "SanPham_DichVu", "SanPhamID");
                resetIdentity(conn, "LichDatSan", "DatSanID");
                resetIdentity(conn, "LichDatSan_DichVu", "Id");
                resetIdentity(conn, "HoaDon", "HoaDonID");
                resetIdentity(conn, "ChiTietHoaDon", "ChiTietID");
                resetIdentity(conn, "KhuyenMai", "KhuyenMaiID");
                resetIdentity(conn, "DanhGia", "DanhGiaID");
                resetIdentity(conn, "Teams", "TeamID");
                resetIdentity(conn, "TeamMembers", "TeamMemberID");
                resetIdentity(conn, "GhepKeo", "KeoID");
                resetIdentity(conn, "CaLamViec", "CaLamViecID");
                resetIdentity(conn, "ThongBao", "ThongBaoID");
                resetIdentity(conn, "SanQR", "SanQRID");

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi nạp dữ liệu giả lập vào H2: " + e.getMessage(), e);
        }
    }

    private static MockSnapshot exportFromDb(Connection conn) throws SQLException {
        MockSnapshot s = new MockSnapshot();
        try (Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT RoleID, RoleName FROM Roles ORDER BY RoleID")) {
                while (rs.next()) s.roles.add(new RoleRow(rs.getInt(1), rs.getString(2)));
            }
            try (ResultSet rs = st.executeQuery("SELECT MonTheThaoID, TenMon FROM MonTheThao ORDER BY MonTheThaoID")) {
                while (rs.next()) s.sports.add(new SportRow(rs.getInt(1), rs.getString(2)));
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT CoSoID, TenCoSo, DiaChi, SoDienThoai, TrangThai, GioMoCua, GioDongCua, HinhAnh, MoTa, LoaiHinhKinhDoanh, SoLuongSanDuKien, AccountID_QuanLy, ViDo, KinhDo FROM CoSo WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY CoSoID")) {
                while (rs.next()) {
                    s.facilities.add(new FacilityRow(
                            rs.getInt("CoSoID"), rs.getString("TenCoSo"), rs.getString("DiaChi"),
                            rs.getString("SoDienThoai"), rs.getString("TrangThai"),
                            rs.getTime("GioMoCua") != null ? rs.getTime("GioMoCua").toString() : "06:00:00",
                            rs.getTime("GioDongCua") != null ? rs.getTime("GioDongCua").toString() : "23:00:00",
                            rs.getString("HinhAnh"), rs.getString("MoTa"), rs.getString("LoaiHinhKinhDoanh"),
                            rs.getInt("SoLuongSanDuKien"), (Integer) rs.getObject("AccountID_QuanLy"),
                            rs.getBigDecimal("ViDo") != null ? rs.getBigDecimal("ViDo").doubleValue() : 10.77,
                            rs.getBigDecimal("KinhDo") != null ? rs.getBigDecimal("KinhDo").doubleValue() : 106.69));
                }
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT AccountID, Username, Password, FullName, PhoneNumber, Email, RoleID, CoSoID, DiemUyTin, MaNganHang, GioiTinh, AvatarUrl, MonTheThaoYeuThichID FROM Accounts WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY AccountID")) {
                while (rs.next()) {
                    s.accounts.add(new AccountRow(
                            rs.getInt("AccountID"), rs.getString("Username"), rs.getString("Password"),
                            rs.getString("FullName"), rs.getString("PhoneNumber"), rs.getString("Email"),
                            (Integer) rs.getObject("RoleID"), (Integer) rs.getObject("CoSoID"),
                            rs.getInt("DiemUyTin"), rs.getString("MaNganHang"), rs.getString("GioiTinh"),
                            rs.getString("AvatarUrl"), (Integer) rs.getObject("MonTheThaoYeuThichID")));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT AccountID, MonTheThaoID FROM MonTheThaoYeuThich")) {
                while (rs.next()) s.favoriteSports.add(new FavoriteSportRow(rs.getInt(1), rs.getInt(2)));
            }
            try (ResultSet rs = st.executeQuery("SELECT CapabilityID, CoSoID, CapabilityType, TrangThai FROM CoSoCapability ORDER BY CapabilityID")) {
                while (rs.next()) s.capabilities.add(new CapabilityRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4)));
            }
            try (ResultSet rs = st.executeQuery("SELECT CoSoID, BankName, BankShortCode, AccountName, AccountNumber FROM CoSoNganHang ORDER BY CoSoID")) {
                while (rs.next()) s.bankAccounts.add(new BankAccountRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            try (ResultSet rs = st.executeQuery("SELECT LoaiSanID, MonTheThaoID, TenLoai, GiaKhongDen, GiaCoDen, GioBatDauLenDen, GioKetThucLenDen, CoSoID FROM LoaiSan WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY LoaiSanID")) {
                while (rs.next()) {
                    s.courtTypes.add(new CourtTypeRow(
                            rs.getInt(1), rs.getInt(2), rs.getString(3),
                            rs.getDouble(4), rs.getDouble(5),
                            rs.getTime(6) != null ? rs.getTime(6).toString() : "17:30:00",
                            rs.getTime(7) != null ? rs.getTime(7).toString() : "23:00:00",
                            (Integer) rs.getObject(8)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT SanID, TenSan, LoaiSanID, CoSoID, TrangThai, MoTa, HinhAnh FROM San WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY SanID")) {
                while (rs.next()) {
                    s.courts.add(new CourtRow(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getString(5), rs.getString(6), rs.getString(7)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT DanhMucID, TenDanhMuc FROM DanhMucSanPham ORDER BY DanhMucID")) {
                while (rs.next()) s.productCategories.add(new CategoryRow(rs.getInt(1), rs.getString(2)));
            }
            try (ResultSet rs = st.executeQuery("SELECT SanPhamID, DanhMucID, CoSoID, TenSanPham, DonGia, DonViTinh, SoLuongTon, TrangThai, SkuCode, GiaNhap, MoTa, HinhAnh FROM SanPham_DichVu WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY SanPhamID")) {
                while (rs.next()) {
                    s.products.add(new ProductRow(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getDouble(5), rs.getString(6), rs.getInt(7), rs.getString(8), rs.getString(9), rs.getDouble(10), rs.getString(11), rs.getString(12)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT DatSanID, AccountID, SanID, NgayDat, GioBatDau, GioKetThuc, ApDungGiaCoDen, TongTienDuKien, TrangThai, GhiChu, NguonDatSan, actual_start_time, actual_end_time, HoldExpiresAt, PaymentMethodConfirmed FROM LichDatSan WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY DatSanID")) {
                while (rs.next()) {
                    s.bookings.add(new BookingRow(
                            rs.getInt("DatSanID"), (Integer) rs.getObject("AccountID"), (Integer) rs.getObject("SanID"),
                            rs.getDate("NgayDat").toString(), rs.getTime("GioBatDau").toString(), rs.getTime("GioKetThuc").toString(),
                            rs.getBoolean("ApDungGiaCoDen"),
                            rs.getBigDecimal("TongTienDuKien") != null ? rs.getBigDecimal("TongTienDuKien").doubleValue() : 0,
                            rs.getString("TrangThai"), rs.getString("GhiChu"), rs.getString("NguonDatSan"),
                            rs.getTime("actual_start_time") != null ? rs.getTime("actual_start_time").toString() : null,
                            rs.getTime("actual_end_time") != null ? rs.getTime("actual_end_time").toString() : null,
                            rs.getTimestamp("HoldExpiresAt") != null ? rs.getTimestamp("HoldExpiresAt").toLocalDateTime().toString() : null,
                            rs.getString("PaymentMethodConfirmed")));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT Id, DatSanID, SanPhamID, Quantity, UnitPrice, TotalPrice, Status, Note FROM LichDatSan_DichVu ORDER BY Id")) {
                while (rs.next()) {
                    s.bookingServices.add(new BookingServiceRow(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5), rs.getDouble(6), rs.getString(7), rs.getString(8)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT HoaDonID, DatSanID, AccountID_KhachHang, AccountID_NhanVien, NgayLap, TongTienSan, TongTienDichVu, PhiGuiXe, KhuyenMaiID, GiamGia, TongThanhToan, PhuongThucThanhToan, TrangThaiThanhToan, LoaiHoaDon FROM HoaDon ORDER BY HoaDonID")) {
                while (rs.next()) {
                    s.invoices.add(new InvoiceRow(
                            rs.getInt(1), (Integer) rs.getObject(2), (Integer) rs.getObject(3), (Integer) rs.getObject(4),
                            rs.getTimestamp(5) != null ? rs.getTimestamp(5).toLocalDateTime().toString() : LocalDateTime.now().toString(),
                            rs.getDouble(6), rs.getDouble(7), rs.getDouble(8), (Integer) rs.getObject(9),
                            rs.getDouble(10), rs.getDouble(11), rs.getString(12), rs.getString(13), rs.getString(14)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT ChiTietID, HoaDonID, SanPhamID, SoLuong, DonGiaTaiThoiDiemBan, ThanhTien FROM ChiTietHoaDon ORDER BY ChiTietID")) {
                while (rs.next()) {
                    s.invoiceDetails.add(new InvoiceDetailRow(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5), rs.getDouble(6)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT KhuyenMaiID, MaCode, MoTa, LoaiGiam, GiaTriGiam, NgayBatDau, NgayKetThuc, SoLanToiDa, SoLanDaDung, CoSoID, TrangThai, GiaTriToiThieu, GiamToiDa, HienThiCongKhai FROM KhuyenMai ORDER BY KhuyenMaiID")) {
                while (rs.next()) {
                    s.promotions.add(new PromotionRow(
                            rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getDouble(5),
                            rs.getDate(6).toString(), rs.getDate(7).toString(),
                            (Integer) rs.getObject(8), rs.getInt(9), (Integer) rs.getObject(10), rs.getString(11),
                            rs.getBigDecimal(12) != null ? rs.getBigDecimal(12).doubleValue() : 0,
                            rs.getBigDecimal(13) != null ? rs.getBigDecimal(13).doubleValue() : 0,
                            rs.getBoolean(14)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT DanhGiaID, DatSanID, AccountID_NguoiDanhGia, AccountID_NguoiBiDanhGia, SoSao, BinhLuan, NgayDanhGia FROM DanhGia ORDER BY DanhGiaID")) {
                while (rs.next()) {
                    s.reviews.add(new ReviewRow(rs.getInt(1), rs.getInt(2), rs.getInt(3), (Integer) rs.getObject(4), rs.getInt(5), rs.getString(6),
                            rs.getTimestamp(7) != null ? rs.getTimestamp(7).toLocalDateTime().toString() : LocalDateTime.now().toString()));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT TeamID, TeamName, Description, SportID, CaptainAccountID, LocationText, AvatarPath, MaxMembers FROM Teams WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY TeamID")) {
                while (rs.next()) {
                    s.teams.add(new TeamRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getString(6), rs.getString(7), rs.getInt(8)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT TeamMemberID, TeamID, AccountID, MemberRole, MemberStatus FROM TeamMembers ORDER BY TeamMemberID")) {
                while (rs.next()) {
                    s.teamMembers.add(new TeamMemberRow(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT KeoID, DatSanID, AccountID_NguoiTao, MonTheThaoID, MoTa, TrinhDo, TrangThai FROM GhepKeo ORDER BY KeoID")) {
                while (rs.next()) {
                    s.matchmaking.add(new MatchRow(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getString(5), rs.getString(6), rs.getString(7)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT CaLamViecID, AccountID, CoSoID, NgayLam, GioBatDau, GioKetThuc, GhiChu, Thu, IsPublished, TenCa, ViTri, TrangThai FROM CaLamViec WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY CaLamViecID")) {
                while (rs.next()) {
                    s.shifts.add(new ShiftRow(
                            rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getDate(4) != null ? rs.getDate(4).toString() : LocalDate.now().toString(),
                            rs.getTime(5).toString(), rs.getTime(6).toString(), rs.getString(7),
                            (Integer) rs.getObject(8), rs.getBoolean(9), rs.getString(10), rs.getString(11), rs.getString(12)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT ThongBaoID, AccountID, TieuDe, NoiDung, LoaiThongBao, DaDoc, DuongDan FROM ThongBao WHERE COALESCE(IsDeleted, FALSE) = FALSE ORDER BY ThongBaoID")) {
                while (rs.next()) {
                    s.notifications.add(new NotificationRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6), rs.getString(7)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT SanQRID, SanID, Token, ShortCode, TrangThai, CreatedBy FROM SanQR ORDER BY SanQRID")) {
                while (rs.next()) {
                    s.courtQrs.add(new CourtQrRow(rs.getInt(1), rs.getInt(2), String.valueOf(rs.getObject(3)), rs.getString(4), rs.getString(5), (Integer) rs.getObject(6)));
                }
            }
        }
        return s;
    }

    private static void resetIdentity(Connection conn, String table, String idCol) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(" + idCol + "), 0) + 1 FROM " + table)) {
            if (rs.next()) {
                long nextVal = rs.getLong(1);
                st.execute("ALTER TABLE " + table + " ALTER COLUMN " + idCol + " RESTART WITH " + nextVal);
            }
        }
    }

    private static void setIntOrNull(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val != null) ps.setInt(idx, val);
        else ps.setNull(idx, Types.INTEGER);
    }

    private static String normalizeTime(String t) {
        if (t == null || t.isBlank()) return "00:00:00";
        return t.length() == 5 ? t + ":00" : t;
    }

    private static String normalizeTimestamp(String ts) {
        if (ts == null || ts.isBlank()) {
            return Timestamp.valueOf(LocalDateTime.now()).toString();
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(ts.trim().replace(' ', 'T'))).toString();
        } catch (Exception e) {
            String s = ts.trim().replace('T', ' ');
            return s.length() == 16 ? s + ":00" : s;
        }
    }

    // ==================== Snapshot POJOs ====================

    public static class MockSnapshot {
        public List<RoleRow> roles = new ArrayList<>();
        public List<SportRow> sports = new ArrayList<>();
        public List<FacilityRow> facilities = new ArrayList<>();
        public List<AccountRow> accounts = new ArrayList<>();
        public List<FavoriteSportRow> favoriteSports = new ArrayList<>();
        public List<CapabilityRow> capabilities = new ArrayList<>();
        public List<BankAccountRow> bankAccounts = new ArrayList<>();
        public List<CourtTypeRow> courtTypes = new ArrayList<>();
        public List<CourtRow> courts = new ArrayList<>();
        public List<CategoryRow> productCategories = new ArrayList<>();
        public List<ProductRow> products = new ArrayList<>();
        public List<BookingRow> bookings = new ArrayList<>();
        public List<BookingServiceRow> bookingServices = new ArrayList<>();
        public List<InvoiceRow> invoices = new ArrayList<>();
        public List<InvoiceDetailRow> invoiceDetails = new ArrayList<>();
        public List<PromotionRow> promotions = new ArrayList<>();
        public List<ReviewRow> reviews = new ArrayList<>();
        public List<TeamRow> teams = new ArrayList<>();
        public List<TeamMemberRow> teamMembers = new ArrayList<>();
        public List<MatchRow> matchmaking = new ArrayList<>();
        public List<ShiftRow> shifts = new ArrayList<>();
        public List<NotificationRow> notifications = new ArrayList<>();
        public List<CourtQrRow> courtQrs = new ArrayList<>();
    }

    public static class RoleRow {
        public int roleId;
        public String roleName;
        public RoleRow() {}
        public RoleRow(int roleId, String roleName) { this.roleId = roleId; this.roleName = roleName; }
    }

    public static class SportRow {
        public int monTheThaoId;
        public String tenMon;
        public SportRow() {}
        public SportRow(int monTheThaoId, String tenMon) { this.monTheThaoId = monTheThaoId; this.tenMon = tenMon; }
    }

    public static class FacilityRow {
        public int coSoId;
        public String tenCoSo, diaChi, soDienThoai, trangThai, gioMoCua, gioDongCua, hinhAnh, moTa, loaiHinhKinhDoanh;
        public int soLuongSanDuKien;
        public Integer accountIdQuanLy;
        public double viDo, kinhDo;
        public FacilityRow() {}
        public FacilityRow(int coSoId, String tenCoSo, String diaChi, String soDienThoai, String trangThai,
                           String gioMoCua, String gioDongCua, String hinhAnh, String moTa, String loaiHinhKinhDoanh,
                           int soLuongSanDuKien, Integer accountIdQuanLy, double viDo, double kinhDo) {
            this.coSoId = coSoId; this.tenCoSo = tenCoSo; this.diaChi = diaChi; this.soDienThoai = soDienThoai;
            this.trangThai = trangThai; this.gioMoCua = gioMoCua; this.gioDongCua = gioDongCua;
            this.hinhAnh = hinhAnh; this.moTa = moTa; this.loaiHinhKinhDoanh = loaiHinhKinhDoanh;
            this.soLuongSanDuKien = soLuongSanDuKien; this.accountIdQuanLy = accountIdQuanLy;
            this.viDo = viDo; this.kinhDo = kinhDo;
        }
    }

    public static class AccountRow {
        public int accountId;
        public String username, password, fullName, phoneNumber, email, maNganHang, gioiTinh, avatarUrl;
        public Integer roleId, coSoId, monTheThaoYeuThichId;
        public int diemUyTin;
        public AccountRow() {}
        public AccountRow(int accountId, String username, String password, String fullName, String phoneNumber,
                          String email, Integer roleId, Integer coSoId, int diemUyTin, String maNganHang,
                          String gioiTinh, String avatarUrl, Integer monTheThaoYeuThichId) {
            this.accountId = accountId; this.username = username; this.password = password;
            this.fullName = fullName; this.phoneNumber = phoneNumber; this.email = email;
            this.roleId = roleId; this.coSoId = coSoId; this.diemUyTin = diemUyTin;
            this.maNganHang = maNganHang; this.gioiTinh = gioiTinh; this.avatarUrl = avatarUrl;
            this.monTheThaoYeuThichId = monTheThaoYeuThichId;
        }
    }

    public static class FavoriteSportRow {
        public int accountId, monTheThaoId;
        public FavoriteSportRow() {}
        public FavoriteSportRow(int accountId, int monTheThaoId) { this.accountId = accountId; this.monTheThaoId = monTheThaoId; }
    }

    public static class CapabilityRow {
        public int capabilityId, coSoId;
        public String capabilityType, trangThai;
        public CapabilityRow() {}
        public CapabilityRow(int capabilityId, int coSoId, String capabilityType, String trangThai) {
            this.capabilityId = capabilityId; this.coSoId = coSoId;
            this.capabilityType = capabilityType; this.trangThai = trangThai;
        }
    }

    public static class BankAccountRow {
        public int coSoId;
        public String bankName, bankShortCode, accountName, accountNumber;
        public BankAccountRow() {}
        public BankAccountRow(int coSoId, String bankName, String bankShortCode, String accountName, String accountNumber) {
            this.coSoId = coSoId; this.bankName = bankName; this.bankShortCode = bankShortCode;
            this.accountName = accountName; this.accountNumber = accountNumber;
        }
    }

    public static class CourtTypeRow {
        public int loaiSanId, monTheThaoId;
        public String tenLoai, gioBatDauLenDen, gioKetThucLenDen;
        public double giaKhongDen, giaCoDen;
        public Integer coSoId;
        public CourtTypeRow() {}
        public CourtTypeRow(int loaiSanId, int monTheThaoId, String tenLoai, double giaKhongDen, double giaCoDen,
                            String gioBatDauLenDen, String gioKetThucLenDen, Integer coSoId) {
            this.loaiSanId = loaiSanId; this.monTheThaoId = monTheThaoId; this.tenLoai = tenLoai;
            this.giaKhongDen = giaKhongDen; this.giaCoDen = giaCoDen;
            this.gioBatDauLenDen = gioBatDauLenDen; this.gioKetThucLenDen = gioKetThucLenDen; this.coSoId = coSoId;
        }
    }

    public static class CourtRow {
        public int sanId, loaiSanId, coSoId;
        public String tenSan, trangThai, moTa, hinhAnh;
        public CourtRow() {}
        public CourtRow(int sanId, String tenSan, int loaiSanId, int coSoId, String trangThai, String moTa, String hinhAnh) {
            this.sanId = sanId; this.tenSan = tenSan; this.loaiSanId = loaiSanId;
            this.coSoId = coSoId; this.trangThai = trangThai; this.moTa = moTa; this.hinhAnh = hinhAnh;
        }
    }

    public static class CategoryRow {
        public int danhMucId;
        public String tenDanhMuc;
        public CategoryRow() {}
        public CategoryRow(int danhMucId, String tenDanhMuc) { this.danhMucId = danhMucId; this.tenDanhMuc = tenDanhMuc; }
    }

    public static class ProductRow {
        public int sanPhamId, danhMucId, coSoId, soLuongTon;
        public String tenSanPham, donViTinh, trangThai, skuCode, moTa, hinhAnh;
        public double donGia, giaNhap;
        public ProductRow() {}
        public ProductRow(int sanPhamId, int danhMucId, int coSoId, String tenSanPham, double donGia, String donViTinh,
                          int soLuongTon, String trangThai, String skuCode, double giaNhap, String moTa, String hinhAnh) {
            this.sanPhamId = sanPhamId; this.danhMucId = danhMucId; this.coSoId = coSoId;
            this.tenSanPham = tenSanPham; this.donGia = donGia; this.donViTinh = donViTinh;
            this.soLuongTon = soLuongTon; this.trangThai = trangThai; this.skuCode = skuCode;
            this.giaNhap = giaNhap; this.moTa = moTa; this.hinhAnh = hinhAnh;
        }
    }

    public static class BookingRow {
        public int datSanId;
        public Integer accountId, sanId;
        public String ngayDat, gioBatDau, gioKetThuc, trangThai, ghiChu, nguonDatSan;
        public String actualStartTime, actualEndTime, holdExpiresAt, paymentMethodConfirmed;
        public boolean apDungGiaCoDen;
        public double tongTienDuKien;
        public BookingRow() {}
        public BookingRow(int datSanId, Integer accountId, Integer sanId, String ngayDat, String gioBatDau, String gioKetThuc,
                          boolean apDungGiaCoDen, double tongTienDuKien, String trangThai, String ghiChu, String nguonDatSan,
                          String actualStartTime, String actualEndTime, String holdExpiresAt, String paymentMethodConfirmed) {
            this.datSanId = datSanId; this.accountId = accountId; this.sanId = sanId;
            this.ngayDat = ngayDat; this.gioBatDau = gioBatDau; this.gioKetThuc = gioKetThuc;
            this.apDungGiaCoDen = apDungGiaCoDen; this.tongTienDuKien = tongTienDuKien;
            this.trangThai = trangThai; this.ghiChu = ghiChu; this.nguonDatSan = nguonDatSan;
            this.actualStartTime = actualStartTime; this.actualEndTime = actualEndTime;
            this.holdExpiresAt = holdExpiresAt; this.paymentMethodConfirmed = paymentMethodConfirmed;
        }
    }

    public static class BookingServiceRow {
        public int id, datSanId, sanPhamId, quantity;
        public double unitPrice, totalPrice;
        public String status, note;
        public BookingServiceRow() {}
        public BookingServiceRow(int id, int datSanId, int sanPhamId, int quantity, double unitPrice, double totalPrice, String status, String note) {
            this.id = id; this.datSanId = datSanId; this.sanPhamId = sanPhamId;
            this.quantity = quantity; this.unitPrice = unitPrice; this.totalPrice = totalPrice;
            this.status = status; this.note = note;
        }
    }

    public static class InvoiceRow {
        public int hoaDonId;
        public Integer datSanId, accountIdKhachHang, accountIdNhanVien, khuyenMaiId;
        public String ngayLap, phuongThucThanhToan, trangThaiThanhToan, loaiHoaDon;
        public double tongTienSan, tongTienDichVu, phiGuiXe, giamGia, tongThanhToan;
        public InvoiceRow() {}
        public InvoiceRow(int hoaDonId, Integer datSanId, Integer accountIdKhachHang, Integer accountIdNhanVien,
                          String ngayLap, double tongTienSan, double tongTienDichVu, double phiGuiXe,
                          Integer khuyenMaiId, double giamGia, double tongThanhToan,
                          String phuongThucThanhToan, String trangThaiThanhToan, String loaiHoaDon) {
            this.hoaDonId = hoaDonId; this.datSanId = datSanId;
            this.accountIdKhachHang = accountIdKhachHang; this.accountIdNhanVien = accountIdNhanVien;
            this.ngayLap = ngayLap; this.tongTienSan = tongTienSan; this.tongTienDichVu = tongTienDichVu;
            this.phiGuiXe = phiGuiXe; this.khuyenMaiId = khuyenMaiId; this.giamGia = giamGia;
            this.tongThanhToan = tongThanhToan; this.phuongThucThanhToan = phuongThucThanhToan;
            this.trangThaiThanhToan = trangThaiThanhToan; this.loaiHoaDon = loaiHoaDon;
        }
    }

    public static class InvoiceDetailRow {
        public int chiTietId, hoaDonId, sanPhamId, soLuong;
        public double donGiaTaiThoiDiemBan, thanhTien;
        public InvoiceDetailRow() {}
        public InvoiceDetailRow(int chiTietId, int hoaDonId, int sanPhamId, int soLuong, double donGiaTaiThoiDiemBan, double thanhTien) {
            this.chiTietId = chiTietId; this.hoaDonId = hoaDonId; this.sanPhamId = sanPhamId;
            this.soLuong = soLuong; this.donGiaTaiThoiDiemBan = donGiaTaiThoiDiemBan; this.thanhTien = thanhTien;
        }
    }

    public static class PromotionRow {
        public int khuyenMaiId, soLanDaDung;
        public Integer soLanToiDa, coSoId;
        public String maCode, moTa, loaiGiam, ngayBatDau, ngayKetThuc, trangThai;
        public double giaTriGiam, giaTriToiThieu, giamToiDa;
        public boolean hienThiCongKhai;
        public PromotionRow() {}
        public PromotionRow(int khuyenMaiId, String maCode, String moTa, String loaiGiam, double giaTriGiam,
                            String ngayBatDau, String ngayKetThuc, Integer soLanToiDa, int soLanDaDung,
                            Integer coSoId, String trangThai, double giaTriToiThieu, double giamToiDa, boolean hienThiCongKhai) {
            this.khuyenMaiId = khuyenMaiId; this.maCode = maCode; this.moTa = moTa; this.loaiGiam = loaiGiam;
            this.giaTriGiam = giaTriGiam; this.ngayBatDau = ngayBatDau; this.ngayKetThuc = ngayKetThuc;
            this.soLanToiDa = soLanToiDa; this.soLanDaDung = soLanDaDung; this.coSoId = coSoId;
            this.trangThai = trangThai; this.giaTriToiThieu = giaTriToiThieu; this.giamToiDa = giamToiDa;
            this.hienThiCongKhai = hienThiCongKhai;
        }
    }

    public static class ReviewRow {
        public int danhGiaId, datSanId, accountIdNguoiDanhGia, soSao;
        public Integer accountIdNguoiBiDanhGia;
        public String binhLuan, ngayDanhGia;
        public ReviewRow() {}
        public ReviewRow(int danhGiaId, int datSanId, int accountIdNguoiDanhGia, Integer accountIdNguoiBiDanhGia,
                         int soSao, String binhLuan, String ngayDanhGia) {
            this.danhGiaId = danhGiaId; this.datSanId = datSanId; this.accountIdNguoiDanhGia = accountIdNguoiDanhGia;
            this.accountIdNguoiBiDanhGia = accountIdNguoiBiDanhGia; this.soSao = soSao;
            this.binhLuan = binhLuan; this.ngayDanhGia = ngayDanhGia;
        }
    }

    public static class TeamRow {
        public int teamId, sportId, captainAccountId, maxMembers;
        public String teamName, description, locationText, avatarPath;
        public TeamRow() {}
        public TeamRow(int teamId, String teamName, String description, int sportId, int captainAccountId,
                       String locationText, String avatarPath, int maxMembers) {
            this.teamId = teamId; this.teamName = teamName; this.description = description;
            this.sportId = sportId; this.captainAccountId = captainAccountId;
            this.locationText = locationText; this.avatarPath = avatarPath; this.maxMembers = maxMembers;
        }
    }

    public static class TeamMemberRow {
        public int teamMemberId, teamId, accountId;
        public String memberRole, memberStatus;
        public TeamMemberRow() {}
        public TeamMemberRow(int teamMemberId, int teamId, int accountId, String memberRole, String memberStatus) {
            this.teamMemberId = teamMemberId; this.teamId = teamId; this.accountId = accountId;
            this.memberRole = memberRole; this.memberStatus = memberStatus;
        }
    }

    public static class MatchRow {
        public int keoId, datSanId, accountIdNguoiTao, monTheThaoId;
        public String moTa, trinhDo, trangThai;
        public MatchRow() {}
        public MatchRow(int keoId, int datSanId, int accountIdNguoiTao, int monTheThaoId,
                        String moTa, String trinhDo, String trangThai) {
            this.keoId = keoId; this.datSanId = datSanId; this.accountIdNguoiTao = accountIdNguoiTao;
            this.monTheThaoId = monTheThaoId; this.moTa = moTa; this.trinhDo = trinhDo; this.trangThai = trangThai;
        }
    }

    public static class ShiftRow {
        public int caLamViecId, accountId, coSoId;
        public Integer thu;
        public boolean isPublished;
        public String ngayLam, gioBatDau, gioKetThuc, ghiChu, tenCa, viTri, trangThai;
        public ShiftRow() {}
        public ShiftRow(int caLamViecId, int accountId, int coSoId, String ngayLam, String gioBatDau, String gioKetThuc,
                        String ghiChu, Integer thu, boolean isPublished, String tenCa, String viTri, String trangThai) {
            this.caLamViecId = caLamViecId; this.accountId = accountId; this.coSoId = coSoId;
            this.ngayLam = ngayLam; this.gioBatDau = gioBatDau; this.gioKetThuc = gioKetThuc;
            this.ghiChu = ghiChu; this.thu = thu; this.isPublished = isPublished;
            this.tenCa = tenCa; this.viTri = viTri; this.trangThai = trangThai;
        }
    }

    public static class NotificationRow {
        public int thongBaoId, accountId;
        public String tieuDe, noiDung, loaiThongBao, duongDan;
        public boolean daDoc;
        public NotificationRow() {}
        public NotificationRow(int thongBaoId, int accountId, String tieuDe, String noiDung,
                               String loaiThongBao, boolean daDoc, String duongDan) {
            this.thongBaoId = thongBaoId; this.accountId = accountId; this.tieuDe = tieuDe;
            this.noiDung = noiDung; this.loaiThongBao = loaiThongBao; this.daDoc = daDoc; this.duongDan = duongDan;
        }
    }

    public static class CourtQrRow {
        public int sanQrId, sanId;
        public Integer createdBy;
        public String token, shortCode, trangThai;
        public CourtQrRow() {}
        public CourtQrRow(int sanQrId, int sanId, String token, String shortCode, String trangThai, Integer createdBy) {
            this.sanQrId = sanQrId; this.sanId = sanId; this.token = token;
            this.shortCode = shortCode; this.trangThai = trangThai; this.createdBy = createdBy;
        }
    }
}
