package org.example.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.controller.MockTestEnvironment;
import org.example.model.TaiKhoan;
import org.example.service.pricing.CourtPriceResult;
import org.example.service.pricing.CourtPricingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * [Y3] Unit Test JUnit 5 cho các Controller tầng Customer:
 * - ApDungKhuyenMaiServlet (/api/promotion/apply)
 * - CustomerDanhGiaServlet (/customer/danh-gia-san)
 * - CourtPricingService (tính giá bật đèn được gọi bởi VisualBookingServlet & DatSanServlet)
 */
@DisplayName("Customer Controllers (Khuyến mãi, Đánh giá & Tính giá sân) - Unit Tests")
public class ApDungKhuyenMaiControllerTest {

    private ApDungKhuyenMaiServlet promoServlet;
    private CustomerDanhGiaServlet danhGiaServlet;
    private CourtPricingService pricingService;

    @BeforeAll
    static void initMockDb() {
        MockTestEnvironment.initIsolatedMockDb();
    }

    @BeforeEach
    void setUp() {
        promoServlet = new ApDungKhuyenMaiServlet();
        danhGiaServlet = new CustomerDanhGiaServlet();
        pricingService = new CourtPricingService();
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @DisplayName("TC_CAT_04..08: Kiểm tra biên & nghiệp vụ áp dụng mã khuyến mãi từ mock-data qua @CsvSource")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @CsvSource(nullValues = "NULL", value = {
            // code, originalAmount, coSoId, expectedValid, expectedSubstring, caseLabel
            "vsport10, 500000, 1, true, 50000, VSPORT10 hợp lệ 10% (500k -> giảm 50k)",
            "VSPORT10, 1500000, 1, true, 100000, VSPORT10 chạm trần GiamToiDa 100k",
            "VSPORT10, 200000, 1, true, 20000, VSPORT10 đúng biên tối thiểu 200k",
            "VSPORT10, 199999, 1, false, mới được áp dụng mã này, VSPORT10 dưới biên tối thiểu (199999 < 200000)",
            "WELCOME20, 300000, 2, true, 60000, WELCOME20 toàn hệ thống (CoSo 2 -> giảm 60k)",
            "PICKLE50K, 300000, 1, false, không áp dụng cho cơ sở này, PICKLE50K sai cơ sở (CoSo 1 != 2)",
            "PICKLE50K, 300000, 2, true, 50000, PICKLE50K đúng cơ sở 2 giảm cố định 50k",
            "KHONGTONTAI, 300000, 1, false, không tồn tại, Mã khuyến mãi không tồn tại",
            "'', 300000, 1, false, Vui lòng nhập mã khuyến mãi, Mã khuyến mãi rỗng",
            "VSPORT10, abc, 1, false, Số tiền hoặc mã cơ sở không hợp lệ, Số tiền không phải số"
    })
    void testApplyPromotionBoundaries(
            String code,
            String originalAmount,
            String coSoId,
            boolean expectedValid,
            String expectedSubstring,
            String caseLabel
    ) throws Exception {
        HttpServletRequest req = MockTestEnvironment.createBaseRequest(null, true);
        when(req.getSession(false)).thenReturn(null);
        when(req.getParameter("code")).thenReturn(code);
        when(req.getParameter("originalAmount")).thenReturn(originalAmount);
        when(req.getParameter("coSoId")).thenReturn(coSoId);

        StringWriter sw = new StringWriter();
        HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);

        promoServlet.doPost(req, resp);

        String body = sw.toString();
        assertTrue(body.contains("\"valid\":" + expectedValid), caseLabel + " sai cờ valid, body=" + body);
        assertTrue(body.contains(expectedSubstring), caseLabel + " phải chứa [" + expectedSubstring + "], thực tế: " + body);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @DisplayName("TC_BOOK_11: Kiểm tra biên Đánh giá sân (soSao, binhLuan, trùng đơn) tại CustomerDanhGiaServlet")
    @CsvSource(nullValues = "NULL", value = {
            // datSanId, soSao, binhLuan, caseLabel, expectedFlashError
            "0, 5, Sân đẹp, datSanId bằng 0 không hợp lệ, Yêu cầu không hợp lệ.",
            "-1, 5, Sân đẹp, datSanId âm không hợp lệ, Yêu cầu không hợp lệ.",
            "1, 0, Sân đẹp, Biên số sao 0 (< 1), Số sao phải từ 1 đến 5.",
            "1, 6, Sân đẹp, Biên số sao 6 (> 5), Số sao phải từ 1 đến 5.",
            "1, 5, '', Bình luận rỗng, Bình luận không được để trống.",
            "5, 5, Sân rất sạch sẽ, Đơn datSanId=5 chưa hoàn thành (Chờ xác nhận), Chỉ có thể đánh giá đơn đặt sân đã hoàn thành của bạn."
    })
    void testCustomerReviewBoundaries(
            String datSanId,
            String soSao,
            String binhLuan,
            String caseLabel,
            String expectedFlashError
    ) throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        TaiKhoan khach1 = MockTestEnvironment.buildUser(3, 3, null, "khach1", "khach1@vsport.vn");
        sessionMap.put("user", khach1);
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, false);
        when(req.getParameter("datSanId")).thenReturn(datSanId);
        when(req.getParameter("soSao")).thenReturn(soSao);
        when(req.getParameter("binhLuan")).thenReturn(binhLuan);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        danhGiaServlet.doPost(req, resp);

        verify(resp).sendRedirect("/customer/lich-su-dat-san");
        assertEquals(expectedFlashError, sessionMap.get("flashError"), caseLabel);
    }

    @Test
    @DisplayName("TC_BOOK_11b: Chặn đánh giá trùng lặp bằng Mockito DanhGiaDAO khi đơn đã hoàn thành và đã được đánh giá")
    void testDuplicateReviewRejectedWithMockedDao() {
        org.example.dao.DanhGiaDAO mockDao = mock(org.example.dao.DanhGiaDAO.class);
        when(mockDao.isBookingCompletedByCustomer(1, 3)).thenReturn(true);
        when(mockDao.existsByDatSanAndAccount(1, 3)).thenReturn(true);

        org.example.service.DanhGiaService svc = new org.example.service.DanhGiaService(mockDao);
        org.example.service.DanhGiaService.DgResult res = svc.submitReview(1, 3, 5, "Đánh giá lần 2");

        assertFalse(res.success);
        assertEquals("Bạn đã đánh giá đơn đặt sân này rồi.", res.message);
    }

    @Test
    @DisplayName("TC_ADM_07 (RBAC): Manager (Role 2) bị trả 403 Forbidden khi gửi đánh giá sân thay Customer")
    void testCustomerReviewForbiddenForNonCustomer() throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        TaiKhoan manager = MockTestEnvironment.buildUser(2, 2, 1, "manager1", "manager1@vsport.vn");
        sessionMap.put("user", manager);
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, false);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        danhGiaServlet.doPost(req, resp);

        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("TC_BOOK_01: Tính giá động tách đoạn qua mốc bật đèn 17:30 trên Sân 5A (17:00-18:30 = 570,000đ)")
    void testCourtPricingAcrossLightingBoundary() {
        CourtPriceResult result = pricingService.calculate(
                LocalDateTime.of(2026, 12, 1, 17, 0),
                LocalDateTime.of(2026, 12, 1, 18, 30),
                LocalTime.of(17, 30),
                LocalTime.of(23, 0),
                new BigDecimal("300000"),
                new BigDecimal("420000")
        );

        assertEquals(new BigDecimal("570000"), result.totalCourtAmount());
        assertEquals(2, result.segments().size());
    }

    @Test
    @DisplayName("Exception Testing (assertThrows): CourtPricingService ném IllegalArgumentException khi khung giờ/giá sai")
    void testCourtPricingAssertThrowsOnInvalidArguments() {
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 18, 0);
        LocalDateTime earlierEnd = LocalDateTime.of(2026, 12, 1, 17, 0);

        IllegalArgumentException exNull = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculate(null, start, LocalTime.of(17, 30), LocalTime.of(23, 0),
                        BigDecimal.valueOf(300000), BigDecimal.valueOf(420000))
        );
        assertTrue(exNull.getMessage().contains("không được để trống"));

        IllegalArgumentException exOrder = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculate(start, earlierEnd, LocalTime.of(17, 30), LocalTime.of(23, 0),
                        BigDecimal.valueOf(300000), BigDecimal.valueOf(420000))
        );
        assertTrue(exOrder.getMessage().contains("nhỏ hơn thời gian kết thúc"));

        assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculate(earlierEnd, start, LocalTime.of(17, 30), LocalTime.of(23, 0),
                        BigDecimal.valueOf(-1), BigDecimal.valueOf(420000)),
                "Giá sân âm phải ném IllegalArgumentException"
        );
    }
}
