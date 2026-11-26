package org.example.controller.manager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.controller.MockTestEnvironment;
import org.example.exception.ForbiddenException;
import org.example.model.TaiKhoan;
import org.example.util.BranchSecurityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * [Y3] Unit Test JUnit 5 cho KhoDichVuManagerServlet (/manager/kho-dich-vu).
 * Bao phủ: @ParameterizedTest + @CsvSource kiểm tra biên giá bán/giá nhập/tồn kho/tên/SKU,
 * xuất kho vượt tồn, phân quyền RBAC, cô lập chi nhánh và assertThrows().
 */
@DisplayName("KhoDichVuManagerServlet - Unit Tests")
public class KhoDichVuManagerControllerTest {

    private KhoDichVuManagerServlet servlet;

    @BeforeAll
    static void initMockDb() {
        MockTestEnvironment.initIsolatedMockDb();
    }

    @BeforeEach
    void setUp() {
        servlet = new KhoDichVuManagerServlet();
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @DisplayName("TC_MGR_02..03: Kiểm tra biên Form thêm sản phẩm tại KhoDichVuManagerServlet qua @CsvSource")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @CsvSource(nullValues = "NULL", value = {
            // tenSanPham, skuCode, donGia, giaNhap, soLuongTon, expectedError, caseLabel
            "'', SP-NEW-01, 20000, 12000, 10, Tên sản phẩm không được trống., Tên sản phẩm rỗng",
            "NULL, SP-NEW-01, 20000, 12000, 10, Tên sản phẩm không được trống., Tên sản phẩm null",
            "Nước tăng lực, SP-NUOC-01, 20000, 12000, 10, đã tồn tại trong kho của chi nhánh, Trùng SKU SP-NUOC-01 tại CoSo 1",
            "Nước suối biên 0, SP-BVA-01, 0, 10000, 10, Giá bán lẻ và giá nhập phải lớn hơn 0, Biên donGia = 0",
            "Nước suối giá âm, SP-BVA-02, -5000, 10000, 10, Giá bán lẻ và giá nhập phải lớn hơn 0, donGia âm (-5000)",
            "Nước suối nhập 0, SP-BVA-03, 15000, 0, 10, Giá bán lẻ và giá nhập phải lớn hơn 0, Biên giaNhap = 0",
            "Nước suối tồn âm, SP-BVA-04, 15000, 10000, -1, Số lượng tồn không được là số âm, Biên soLuongTon = -1"
    })
    void testAddProductValidationBoundaries(
            String tenSanPham,
            String skuCode,
            String donGia,
            String giaNhap,
            String soLuongTon,
            String expectedError,
            String caseLabel
    ) throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        TaiKhoan manager1 = MockTestEnvironment.buildUser(2, 2, 1, "manager1", "manager1@vsport.vn");
        sessionMap.put("user", manager1);
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, false);
        when(req.getParameter("action")).thenReturn("add");
        when(req.getParameter("tenSanPham")).thenReturn(tenSanPham);
        when(req.getParameter("skuCode")).thenReturn(skuCode);
        when(req.getParameter("danhMucID")).thenReturn("1");
        when(req.getParameter("donGia")).thenReturn(donGia);
        when(req.getParameter("giaNhap")).thenReturn(giaNhap);
        when(req.getParameter("donViTinh")).thenReturn("Chai");
        when(req.getParameter("soLuongTon")).thenReturn(soLuongTon);
        when(req.getParameter("trangThai")).thenReturn("Đang kinh doanh");

        HttpServletResponse resp = mock(HttpServletResponse.class);

        servlet.doPost(req, resp);

        verify(resp).sendRedirect("/manager/kho-dich-vu");
        String actualError = (String) sessionMap.get("errorMsg");
        assertNotNull(actualError, caseLabel + " phải đặt errorMsg trong session");
        assertTrue(actualError.contains(expectedError),
                caseLabel + " phải chứa [" + expectedError + "], thực tế: " + actualError);
    }

    @Test
    @DisplayName("TC_MGR_02b: Tên sản phẩm vượt biên 100 ký tự (101 ký tự) bị từ chối")
    void testAddProductNameExceeds100Chars() throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        sessionMap.put("user", MockTestEnvironment.buildUser(2, 2, 1, "manager1", "manager1@vsport.vn"));
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, false);
        when(req.getParameter("action")).thenReturn("add");
        when(req.getParameter("tenSanPham")).thenReturn("S".repeat(101));
        when(req.getParameter("skuCode")).thenReturn("SP-LONG-101");
        when(req.getParameter("danhMucID")).thenReturn("1");
        when(req.getParameter("donGia")).thenReturn("20000");
        when(req.getParameter("giaNhap")).thenReturn("10000");
        when(req.getParameter("soLuongTon")).thenReturn("5");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        servlet.doPost(req, resp);

        assertEquals("Tên sản phẩm không được vượt quá 100 ký tự.", sessionMap.get("errorMsg"));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @DisplayName("TC_MGR_04: Kiểm tra biên Nhập kho & Xuất kho (<= 0 hoặc vượt tồn kho)")
    @CsvSource({
            // action, productId, amount, caseLabel, expectedError
            "nhap-kho, 1, 0, Nhập kho số lượng 0, Số lượng nhập kho phải lớn hơn 0.",
            "nhap-kho, 1, -5, Nhập kho số lượng âm, Số lượng nhập kho phải lớn hơn 0.",
            "xuat-kho, 1, 0, Xuất kho số lượng 0, Số lượng xuất kho phải lớn hơn 0.",
            "xuat-kho, 1, 999999, Xuất kho vượt tồn kho hiện có, Số lượng xuất kho vượt quá số lượng tồn hiện có"
    })
    void testStockInOutBoundaries(
            String action,
            String productId,
            String amount,
            String caseLabel,
            String expectedError
    ) throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        sessionMap.put("user", MockTestEnvironment.buildUser(2, 2, 1, "manager1", "manager1@vsport.vn"));
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, false);
        when(req.getParameter("action")).thenReturn(action);
        when(req.getParameter("id")).thenReturn(productId);
        when(req.getParameter("amount")).thenReturn(amount);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        servlet.doPost(req, resp);

        String err = (String) sessionMap.get("errorMsg");
        assertNotNull(err, caseLabel + " phải báo lỗi");
        assertTrue(err.contains(expectedError), caseLabel + " kỳ vọng [" + expectedError + "], thực tế: " + err);
    }

    @Test
    @DisplayName("TC_ADM_07 & TC_ADM_08 (RBAC): Chặn Khách hàng (403) và chặn Manager CoSo 1 xóa sản phẩm của CoSo 2")
    void testRbacAndCrossBranchIsolation() throws Exception {
        // Case 1: Customer truy cập /manager/kho-dich-vu -> 403
        Map<String, Object> custMap = MockTestEnvironment.newSessionMap();
        custMap.put("user", MockTestEnvironment.buildUser(3, 3, null, "khach1", "khach1@vsport.vn"));
        HttpSession custSession = MockTestEnvironment.createInMemorySession(custMap);
        HttpServletRequest custReq = MockTestEnvironment.createBaseRequest(custSession, false);
        HttpServletResponse custResp = mock(HttpServletResponse.class);

        servlet.doGet(custReq, custResp);
        verify(custResp).sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập chức năng này.");

        // Case 2: Manager CoSo 1 cố xóa sản phẩm ID=7 thuộc CoSo 2 -> bị từ chối
        Map<String, Object> mgrMap = MockTestEnvironment.newSessionMap();
        mgrMap.put("user", MockTestEnvironment.buildUser(2, 2, 1, "manager1", "manager1@vsport.vn"));
        HttpSession mgrSession = MockTestEnvironment.createInMemorySession(mgrMap);
        HttpServletRequest mgrReq = MockTestEnvironment.createBaseRequest(mgrSession, false);
        when(mgrReq.getParameter("action")).thenReturn("delete");
        when(mgrReq.getParameter("id")).thenReturn("7");
        HttpServletResponse mgrResp = mock(HttpServletResponse.class);

        servlet.doPost(mgrReq, mgrResp);
        assertEquals("Sản phẩm không hợp lệ.", mgrMap.get("errorMsg"));
    }

    @Test
    @DisplayName("Exception Testing (assertThrows): BranchSecurityUtils ném ForbiddenException & KhoDichVuManagerServlet ném NullPointerException khi Manager chưa gắn CoSoID")
    void testAssertThrowsBranchIsolationAndNullCoSo() {
        ForbiddenException forbiddenEx = assertThrows(
                ForbiddenException.class,
                () -> BranchSecurityUtils.checkBranchAccess(2, 1),
                "Truy cập chéo cơ sở (CoSo 2 từ Manager CoSo 1) phải ném ForbiddenException"
        );
        assertTrue(forbiddenEx.getMessage().contains("không có quyền truy cập"));

        assertThrows(
                IllegalArgumentException.class,
                () -> BranchSecurityUtils.checkBranchAccess(1, null),
                "Manager chưa gắn cơ sở phải ném IllegalArgumentException"
        );

        // Manager có coSoId = null gọi doGet -> unbox int ném NullPointerException
        Map<String, Object> badMgrMap = MockTestEnvironment.newSessionMap();
        badMgrMap.put("user", MockTestEnvironment.buildUser(99, 2, null, "orphanMgr", "orphan@vsport.vn"));
        HttpSession badSession = MockTestEnvironment.createInMemorySession(badMgrMap);
        HttpServletRequest req = MockTestEnvironment.createBaseRequest(badSession, false);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        assertThrows(
                NullPointerException.class,
                () -> servlet.doGet(req, resp),
                "Manager có coSoId=null phải ném NullPointerException tại doGet"
        );
    }
}
