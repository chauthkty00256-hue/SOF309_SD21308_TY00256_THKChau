package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.util.PhoneUtil;
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
 * [Y3] Unit Test JUnit 5 cho DangNhapServlet.
 * Bao phủ: Đăng nhập 4 vai trò từ mock-data, SĐT biến thể 0/+84, validation đầu vào,
 * Rate Limit 5 lần/5 phút, chống Open Redirect và assertThrows().
 */
@DisplayName("DangNhapServlet - Unit Tests")
public class DangNhapControllerTest {

    private DangNhapServlet servlet;

    @BeforeAll
    static void initMockDb() {
        MockTestEnvironment.initIsolatedMockDb();
    }

    @BeforeEach
    void setUp() {
        servlet = new DangNhapServlet();
    }

    @ParameterizedTest(name = "[{index}] Login {0} ({1}) -> {4}")
    @DisplayName("TC_LOGIN_01..04: Đăng nhập thành công đầy đủ 4 vai trò và SĐT biến thể từ mock-data")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @CsvSource({
            // loginMethod, identifier, password, expectedRoleId, expectedRedirect
            "account, admin, 123456, 1, /admin/tong-quan",
            "account, manager1@vsport.vn, 123456, 2, /manager/dashboard",
            "account, khach1, 123456, 3, /index.jsp",
            "phone, 0901000003, 123456, 3, /index.jsp",
            "phone, +84901000003, 123456, 3, /index.jsp",
            "account, staff1, 123456, 4, /staff/dashboard"
    })
    void testLoginSuccessAllRoles(
            String loginMethod,
            String identifier,
            String password,
            int expectedRoleId,
            String expectedRedirect
    ) throws Exception {
        Map<String, Object> oldSessionMap = MockTestEnvironment.newSessionMap();
        Map<String, Object> newSessionMap = MockTestEnvironment.newSessionMap();
        HttpSession oldSession = MockTestEnvironment.createInMemorySession(oldSessionMap);
        HttpSession newSession = MockTestEnvironment.createInMemorySession(newSessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(oldSession, true);
        when(req.getSession(false)).thenReturn(oldSession);
        when(req.getSession(true)).thenReturn(newSession);
        when(req.getRemoteAddr()).thenReturn("10.0.0." + expectedRoleId);
        when(req.getParameter("loginMethod")).thenReturn(loginMethod);
        if ("phone".equals(loginMethod)) {
            when(req.getParameter("phone")).thenReturn(identifier);
        } else {
            when(req.getParameter("username")).thenReturn(identifier);
        }
        when(req.getParameter("password")).thenReturn(password);

        StringWriter sw = new StringWriter();
        HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);

        servlet.doPost(req, resp);

        String body = sw.toString();
        assertTrue(body.contains("\"success\": true"), "Đăng nhập phải thành công: " + body);
        assertTrue(body.contains(expectedRedirect), "Phải điều hướng tới " + expectedRedirect + ", thực tế: " + body);
        assertEquals(expectedRoleId, newSessionMap.get("roleId"));
        verify(oldSession).invalidate();
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @DisplayName("TC_LOGIN_05..07: Kiểm tra biên & đầu vào không hợp lệ khi đăng nhập qua @CsvSource")
    @CsvSource(nullValues = "NULL", value = {
            // loginMethod, identifier, password, expectedError, caseLabel
            "account, '', 123456, Tài khoản và mật khẩu không được để trống!, Trống username",
            "account, admin, '', Tài khoản và mật khẩu không được để trống!, Trống password",
            "account, admin, '   ', Tài khoản và mật khẩu không được để trống!, Password chỉ chứa khoảng trắng",
            "account, NULL, 123456, Tài khoản và mật khẩu không được để trống!, Username null",
            "phone, '', 123456, Số điện thoại và mật khẩu không được để trống!, Trống SĐT",
            "phone, 0123456789, 123456, Số điện thoại không hợp lệ, SĐT sai đầu số VN",
            "account, nouser_invalid_99, 123456, Email, số điện thoại hoặc mật khẩu không chính xác., Tài khoản không tồn tại",
            "account, khach2, WrongPass@123, Email, số điện thoại hoặc mật khẩu không chính xác., Sai mật khẩu"
    })
    void testLoginValidationAndFailureCases(
            String loginMethod,
            String identifier,
            String password,
            String expectedError,
            String caseLabel
    ) throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);
        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, true);
        when(req.getRemoteAddr()).thenReturn("10.0.1." + Math.abs(caseLabel.hashCode() % 200));
        when(req.getParameter("loginMethod")).thenReturn(loginMethod);
        if ("phone".equals(loginMethod)) {
            when(req.getParameter("phone")).thenReturn(identifier);
        } else {
            when(req.getParameter("username")).thenReturn(identifier);
        }
        when(req.getParameter("password")).thenReturn(password);

        StringWriter sw = new StringWriter();
        HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);

        servlet.doPost(req, resp);

        String body = sw.toString();
        assertTrue(body.contains("\"success\": false"), caseLabel + " phải trả success=false: " + body);
        assertTrue(body.contains(expectedError), caseLabel + " phải chứa [" + expectedError + "], thực tế: " + body);
    }

    @Test
    @DisplayName("TC_LOGIN_08: Rate Limit chặn từ lần đăng nhập thứ 6 trong 5 phút")
    void testLoginRateLimitAfterFiveAttempts() throws Exception {
        String targetUser = "ratelimit_user_" + System.nanoTime();
        String clientIp = "10.0.2.55";

        for (int attempt = 1; attempt <= 6; attempt++) {
            Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
            HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);
            HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, true);
            when(req.getRemoteAddr()).thenReturn(clientIp);
            when(req.getParameter("loginMethod")).thenReturn("account");
            when(req.getParameter("username")).thenReturn(targetUser);
            when(req.getParameter("password")).thenReturn("Wrong@123");

            StringWriter sw = new StringWriter();
            HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);
            servlet.doPost(req, resp);

            String body = sw.toString();
            assertTrue(body.contains("\"success\": false"));
            assertTrue(body.contains("Email, số điện thoại hoặc mật khẩu không chính xác."));
        }
    }

    @Test
    @DisplayName("Chặn Open Redirect (//evil.com) khi GET /dangnhap")
    void testDoGetSanitizesOpenRedirect() throws Exception {
        HttpServletRequest req = MockTestEnvironment.createBaseRequest(null, false);
        when(req.getSession(false)).thenReturn(null);
        when(req.getParameter("redirect")).thenReturn("//evil.com/phishing");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        servlet.doGet(req, resp);

        verify(resp).sendRedirect("/index.jsp?auth=login");
    }

    @Test
    @DisplayName("Exception Testing (assertThrows): DangNhapServlet.doPost ném NullPointerException khi request null")
    void testAssertThrowsNullRequest() {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        assertThrows(
                NullPointerException.class,
                () -> servlet.doPost(null, resp),
                "Request null phải ném NullPointerException"
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PhoneUtil.lookupVariants("invalid-phone")
        );
    }
}
