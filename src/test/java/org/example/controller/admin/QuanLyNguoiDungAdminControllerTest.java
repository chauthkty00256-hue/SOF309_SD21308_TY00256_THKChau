package org.example.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.controller.MockTestEnvironment;
import org.example.model.TaiKhoan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * [Y3] Unit Test JUnit 5 cho các Controller tầng Admin:
 * - QuanLyNguoiDungServlet (/admin/nhan-su)
 * - QuanLyChiNhanhServlet (/admin/chi-nhanh/*)
 * - PayOSConfigAdminServlet (/admin/chi-nhanh/payos)
 * - AdminDashboardServlet (/admin/tong-quan)
 */
@DisplayName("Admin Controllers (Nhân sự, Chi nhánh, PayOS & RBAC) - Unit Tests")
public class QuanLyNguoiDungAdminControllerTest {

    private QuanLyNguoiDungServlet nguoiDungServlet;
    private QuanLyChiNhanhServlet chiNhanhServlet;
    private PayOSConfigAdminServlet payosServlet;
    private AdminDashboardServlet dashboardServlet;

    @BeforeAll
    static void initMockDb() {
        MockTestEnvironment.initIsolatedMockDb();
    }

    @BeforeEach
    void setUp() {
        nguoiDungServlet = new QuanLyNguoiDungServlet();
        chiNhanhServlet = new QuanLyChiNhanhServlet();
        payosServlet = new PayOSConfigAdminServlet();
        dashboardServlet = new AdminDashboardServlet();
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @DisplayName("TC_ADM_03: Kiểm tra biên & ràng buộc tạo Nhân sự tại QuanLyNguoiDungServlet qua @CsvSource")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @CsvSource(nullValues = "NULL", value = {
            // email, phone, roleId, coSoId, password, expectedError, caseLabel
            "'', 0912345678, 4, 1, Strong@123, Email không được để trống!, Email rỗng",
            "'bad email@vsport.vn', 0912345678, 4, 1, Strong@123, Định dạng Email không hợp lệ, Email chứa khoảng trắng",
            "admin@vsport.vn, 0912345678, 4, 1, Strong@123, Email đã tồn tại!, Trùng email admin@vsport.vn",
            "newstaff01@vsport.vn, 0123456789, 4, 1, Strong@123, Định dạng Số điện thoại không hợp lệ, SĐT sai đầu số VN",
            "newstaff02@vsport.vn, 0912345678, 1, '', Strong@123, Không được tạo tài khoản Quản trị viên!, Cấm tạo RoleID=1 (Admin)",
            "newstaff03@vsport.vn, 0912345678, 2, '', Strong@123, Quản lý cần phải có Cơ sở!, Tạo Manager thiếu CoSoID",
            "newstaff04@vsport.vn, 0912345678, 4, 1, weak123, Mật khẩu do admin tạo phải có tối thiểu 8 ký tự, Mật khẩu yếu (< 8 ký tự/thiếu ký tự đặc biệt)"
    })
    void testAdminAddUserValidationBoundaries(
            String email,
            String phone,
            String roleId,
            String coSoId,
            String password,
            String expectedError,
            String caseLabel
    ) throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        sessionMap.put("user", MockTestEnvironment.buildUser(1, 1, null, "admin", "admin@vsport.vn"));
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, true);
        when(req.getParameter("action")).thenReturn("add");
        when(req.getParameter("email")).thenReturn(email);
        when(req.getParameter("phoneNumber")).thenReturn(phone);
        when(req.getParameter("fullName")).thenReturn("Nhân sự Test");
        when(req.getParameter("roleId")).thenReturn(roleId);
        when(req.getParameter("coSoId")).thenReturn(coSoId);
        when(req.getParameter("password")).thenReturn(password);

        StringWriter sw = new StringWriter();
        HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);

        nguoiDungServlet.doPost(req, resp);

        String body = sw.toString();
        assertTrue(body.contains("\"success\": false"), caseLabel + " phải trả success=false: " + body);
        assertTrue(body.contains(expectedError), caseLabel + " phải chứa [" + expectedError + "], thực tế: " + body);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @DisplayName("TC_ADM_05: Kiểm tra biên cấu hình PayOS tại PayOSConfigAdminServlet qua @CsvSource")
    @CsvSource(nullValues = "NULL", value = {
            // coSoId, clientId, apiKey, checksumKey, caseLabel, expectedError
            "abc, cid-1, key-1, csum-1, CoSoID không phải số, CoSoID không hợp lệ.",
            "1, ••••••, key-1, csum-1, Client ID chứa ký tự che placeholder, Client ID không hợp lệ.",
            "1, '', key-1, csum-1, Client ID để trống, Client ID không được để trống.",
            "1, cid-1, '', csum-1, API Key để trống, API Key không được để trống.",
            "1, cid-1, key-1, '', Checksum Key để trống, Checksum Key không được để trống."
    })
    void testPayOSConfigValidationBoundaries(
            String coSoId,
            String clientId,
            String apiKey,
            String checksumKey,
            String caseLabel,
            String expectedError
    ) throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        sessionMap.put("user", MockTestEnvironment.buildUser(1, 1, null, "admin", "admin@vsport.vn"));
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, true);
        when(req.getParameter("coSoId")).thenReturn(coSoId);
        when(req.getParameter("clientId")).thenReturn(clientId);
        when(req.getParameter("apiKey")).thenReturn(apiKey);
        when(req.getParameter("checksumKey")).thenReturn(checksumKey);

        StringWriter sw = new StringWriter();
        HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);

        payosServlet.doPost(req, resp);

        String body = sw.toString();
        assertTrue(body.contains("\"success\":false"), caseLabel + " phải trả success=false: " + body);
        assertTrue(body.contains(expectedError), caseLabel + " phải chứa [" + expectedError + "], thực tế: " + body);
    }

    @Test
    @DisplayName("TC_ADM_02 & TC_ADM_07 (RBAC): Admin bị chặn sửa chi nhánh (403); Customer & Manager bị chặn khỏi /admin/tong-quan")
    void testAdminRoutePermissionsAndBranchEditForbidden() throws Exception {
        // 1. Admin không có quyền sửa chi nhánh -> 403
        Map<String, Object> adminMap = MockTestEnvironment.newSessionMap();
        adminMap.put("user", MockTestEnvironment.buildUser(1, 1, null, "admin", "admin@vsport.vn"));
        HttpSession adminSession = MockTestEnvironment.createInMemorySession(adminMap);
        HttpServletRequest editReq = MockTestEnvironment.createBaseRequest(adminSession, false);
        when(editReq.getServletPath()).thenReturn("/admin/chi-nhanh/sua");
        HttpServletResponse editResp = mock(HttpServletResponse.class);

        chiNhanhServlet.doGet(editReq, editResp);
        verify(editResp).sendError(HttpServletResponse.SC_FORBIDDEN, "Admin không có quyền chỉnh sửa cơ sở.");

        // 2. Customer (Role 3) truy cập /admin/tong-quan -> redirect /dangnhap
        Map<String, Object> custMap = MockTestEnvironment.newSessionMap();
        custMap.put("user", MockTestEnvironment.buildUser(3, 3, null, "khach1", "khach1@vsport.vn"));
        HttpSession custSession = MockTestEnvironment.createInMemorySession(custMap);
        HttpServletRequest custReq = MockTestEnvironment.createBaseRequest(custSession, false);
        HttpServletResponse custResp = mock(HttpServletResponse.class);

        dashboardServlet.doGet(custReq, custResp);
        verify(custResp).sendRedirect("/dangnhap");

        // 3. Manager (Role 2) truy cập /admin/nhan-su -> redirect /dangnhap
        Map<String, Object> mgrMap = MockTestEnvironment.newSessionMap();
        mgrMap.put("user", MockTestEnvironment.buildUser(2, 2, 1, "manager1", "manager1@vsport.vn"));
        HttpSession mgrSession = MockTestEnvironment.createInMemorySession(mgrMap);
        HttpServletRequest mgrReq = MockTestEnvironment.createBaseRequest(mgrSession, false);
        HttpServletResponse mgrResp = mock(HttpServletResponse.class);

        nguoiDungServlet.doGet(mgrReq, mgrResp);
        verify(mgrResp).sendRedirect("/dangnhap");
    }

    @Test
    @DisplayName("TC_ADM_03b: Admin không thể xóa mềm tài khoản Quản trị viên (RoleID=1)")
    void testAdminCannotSoftDeleteAdminAccount() throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        sessionMap.put("user", MockTestEnvironment.buildUser(1, 1, null, "admin", "admin@vsport.vn"));
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, false);
        when(req.getParameter("action")).thenReturn("softDelete");
        when(req.getParameter("id")).thenReturn("1");
        HttpServletResponse resp = mock(HttpServletResponse.class);

        nguoiDungServlet.doPost(req, resp);

        assertEquals("Không thể xóa tài khoản Quản trị viên.", sessionMap.get("error"));
    }

    @Test
    @DisplayName("Exception Testing (assertThrows): QuanLyNguoiDungServlet ném NumberFormatException khi tham số id / accountId không phải số")
    void testAssertThrowsNumberFormatExceptionOnInvalidIds() {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        sessionMap.put("user", MockTestEnvironment.buildUser(1, 1, null, "admin", "admin@vsport.vn"));
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        // GET ?action=delete&id=abc -> NumberFormatException
        HttpServletRequest getReq = MockTestEnvironment.createBaseRequest(session, false);
        when(getReq.getParameter("action")).thenReturn("delete");
        when(getReq.getParameter("id")).thenReturn("abc");
        HttpServletResponse getResp = mock(HttpServletResponse.class);

        assertThrows(
                NumberFormatException.class,
                () -> nguoiDungServlet.doGet(getReq, getResp),
                "Tham số id='abc' phải ném NumberFormatException"
        );

        // POST action=update&accountId=invalid -> NumberFormatException
        HttpServletRequest postReq = MockTestEnvironment.createBaseRequest(session, false);
        when(postReq.getParameter("action")).thenReturn("update");
        when(postReq.getParameter("accountId")).thenReturn("invalid");
        HttpServletResponse postResp = mock(HttpServletResponse.class);

        assertThrows(
                NumberFormatException.class,
                () -> nguoiDungServlet.doPost(postReq, postResp),
                "Tham số accountId='invalid' phải ném NumberFormatException"
        );
    }
}
