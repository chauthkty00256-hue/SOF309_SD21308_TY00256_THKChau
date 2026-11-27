package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.model.TaiKhoan;
import org.example.util.OtpRateLimitUtil;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * [Y3] Unit Test JUnit 5 cho DangKyServlet & XacThucOTPServlet.
 * Bao phủ: @ParameterizedTest + @CsvSource kiểm tra biên Form đăng ký,
 * luồng xác thực Mock OTP (123456), khóa tạm OTP 5 lần sai và assertThrows().
 */
@DisplayName("DangKyServlet & XacThucOTPServlet - Unit Tests")
public class DangKyControllerTest {

    private DangKyServlet dangKyServlet;
    private XacThucOTPServlet otpServlet;

    @BeforeAll
    static void initMockDb() {
        MockTestEnvironment.initIsolatedMockDb();
    }

    @BeforeEach
    void setUp() {
        dangKyServlet = new DangKyServlet();
        otpServlet = new XacThucOTPServlet();
    }

    @ParameterizedTest(name = "[{index}] {8}")
    @DisplayName("TC_REG_02..07: Kiểm tra biên & validation đầu vào Form Đăng ký qua @CsvSource")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @CsvSource(nullValues = "NULL", value = {
            // password, confirm, agree, username, email, phone, fullname, expectedMessage, caseLabel
            "Test@1234, Test@1235, Đồng ý, '', valid01@vsport.vn, 0912345601, Nguyễn Văn A, Mật khẩu không trùng khớp!, Mật khẩu xác nhận không khớp",
            "NULL, Test@1234, Đồng ý, '', valid01@vsport.vn, 0912345601, Nguyễn Văn A, Mật khẩu không trùng khớp!, Mật khẩu null",
            "Ab1@567, Ab1@567, Đồng ý, '', valid01@vsport.vn, 0912345601, Nguyễn Văn A, Mật khẩu phải có tối thiểu 8 ký tự, Biên mật khẩu 7 ký tự (< 8)",
            "Abcd1234, Abcd1234, Đồng ý, '', valid01@vsport.vn, 0912345601, Nguyễn Văn A, Mật khẩu phải có tối thiểu 8 ký tự, Mật khẩu 8 ký tự thiếu ký tự đặc biệt",
            "abcd@123, abcd@123, Đồng ý, '', valid01@vsport.vn, 0912345601, Nguyễn Văn A, Mật khẩu phải có tối thiểu 8 ký tự, Mật khẩu thiếu chữ hoa",
            "Abcd@efg, Abcd@efg, Đồng ý, '', valid01@vsport.vn, 0912345601, Nguyễn Văn A, Mật khẩu phải có tối thiểu 8 ký tự, Mật khẩu thiếu chữ số",
            "Test@1234, Test@1234, NULL, '', valid01@vsport.vn, 0912345601, Nguyễn Văn A, Bạn chưa đồng ý với điều khoản!, Chưa tích đồng ý điều khoản",
            "Test@1234, Test@1234, Đồng ý, ab, valid01@vsport.vn, 0912345601, Nguyễn Văn A, Tên đăng nhập không hợp lệ, Biên username 2 ký tự (< 3)",
            "Test@1234, Test@1234, Đồng ý, '', uservsport.vn, 0912345601, Nguyễn Văn A, Email không hợp lệ, Email thiếu ký tự @",
            "Test@1234, Test@1234, Đồng ý, '', 'user 01@vsport.vn', 0912345601, Nguyễn Văn A, Email không hợp lệ, Email chứa khoảng trắng",
            "Test@1234, Test@1234, Đồng ý, '', valid01@vsport.vn, 0281234567, Nguyễn Văn A, Số điện thoại không hợp lệ, SĐT sai đầu số di động VN",
            "Test@1234, Test@1234, Đồng ý, '', valid01@vsport.vn, '', Nguyễn Văn A, Số điện thoại không hợp lệ, SĐT rỗng",
            "Test@1234, Test@1234, Đồng ý, '', valid01@vsport.vn, 0912345601, A, Vui lòng nhập họ và tên hợp lệ (2-100 ký tự), Biên họ tên 1 ký tự (< 2)",
            "Test@1234, Test@1234, Đồng ý, '', valid01@vsport.vn, +84901000003, Nguyễn Văn A, Số điện thoại đã được đăng ký, Trùng SĐT biến thể +84 của khach1",
            "Test@1234, Test@1234, Đồng ý, '', khach1@vsport.vn, 0912345699, Nguyễn Văn A, Email đã tồn tại!, Trùng email khach1@vsport.vn trong mock-data"
    })
    void testRegisterValidationBoundaries(
            String password,
            String confirmPassword,
            String agree,
            String username,
            String email,
            String phone,
            String fullname,
            String expectedMessage,
            String caseLabel
    ) throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);
        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, true);
        StringWriter sw = new StringWriter();
        HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);

        when(req.getParameter("password")).thenReturn(password);
        when(req.getParameter("confirm_password")).thenReturn(confirmPassword);
        when(req.getParameterValues("agree")).thenReturn(agree == null ? null : new String[]{agree});
        when(req.getParameter("username")).thenReturn(username);
        when(req.getParameter("email")).thenReturn(email);
        when(req.getParameter("phone")).thenReturn(phone);
        when(req.getParameter("fullname")).thenReturn(fullname);

        dangKyServlet.doPost(req, resp);

        String body = sw.toString();
        assertTrue(body.contains("\"success\": false"), caseLabel + " phải trả success=false, body=" + body);
        assertTrue(body.contains(expectedMessage), caseLabel + " phải chứa thông báo [" + expectedMessage + "], thực tế: " + body);
    }

    @Test
    @DisplayName("TC_REG_07b: Họ và tên vượt quá biên 100 ký tự (101 ký tự) bị từ chối")
    void testRegisterFullnameExceedsMaxBoundary() throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);
        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, true);
        StringWriter sw = new StringWriter();
        HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);

        when(req.getParameter("password")).thenReturn("Test@1234");
        when(req.getParameter("confirm_password")).thenReturn("Test@1234");
        when(req.getParameterValues("agree")).thenReturn(new String[]{"Đồng ý"});
        when(req.getParameter("email")).thenReturn("maxlen_" + System.nanoTime() + "@vsport.vn");
        when(req.getParameter("phone")).thenReturn("0912345688");
        when(req.getParameter("fullname")).thenReturn("A".repeat(101));

        dangKyServlet.doPost(req, resp);

        String body = sw.toString();
        assertTrue(body.contains("\"success\": false"));
        assertTrue(body.contains("Vui lòng nhập họ và tên hợp lệ (2-100 ký tự)."));
    }

    @Test
    @DisplayName("TC_REG_01: Đăng ký hợp lệ và xác thực thành công với Mock OTP 123456")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRegisterAndVerifyMockOtpSuccess() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6);
        String uniqueEmail = "unit_" + uniqueSuffix + "@vsport.vn";
        String uniquePhone = "098" + String.format("%07d", Math.abs(uniqueSuffix.hashCode()) % 10_000_000);

        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);
        HttpServletRequest regReq = MockTestEnvironment.createBaseRequest(session, true);
        StringWriter regOut = new StringWriter();
        HttpServletResponse regResp = MockTestEnvironment.createResponseWithWriter(regOut);

        when(regReq.getParameter("password")).thenReturn("Test@1234");
        when(regReq.getParameter("confirm_password")).thenReturn("Test@1234");
        when(regReq.getParameterValues("agree")).thenReturn(new String[]{"Đồng ý"});
        when(regReq.getParameter("email")).thenReturn(uniqueEmail);
        when(regReq.getParameter("phone")).thenReturn(uniquePhone);
        when(regReq.getParameter("fullname")).thenReturn("Nguyễn Unit Test");

        dangKyServlet.doPost(regReq, regResp);

        String regBody = regOut.toString();
        assertTrue(regBody.contains("\"success\": true"), "Bước 1 đăng ký phải thành công: " + regBody);
        assertEquals("REGISTER", sessionMap.get("authType"));
        assertNotNull(sessionMap.get("tempAccount"));

        // Bước 2: Nhập Mock OTP 123456 tại XacThucOTPServlet
        HttpServletRequest otpReq = MockTestEnvironment.createBaseRequest(session, true);
        StringWriter otpOut = new StringWriter();
        HttpServletResponse otpResp = MockTestEnvironment.createResponseWithWriter(otpOut);
        when(otpReq.getServletPath()).thenReturn("/nhapma");
        when(otpReq.getParameter("otp")).thenReturn("123456");
        when(otpReq.getParameter("email")).thenReturn(uniqueEmail);

        otpServlet.doPost(otpReq, otpResp);

        String otpBody = otpOut.toString();
        assertTrue(otpBody.contains("\"success\": true"), "Xác thực Mock OTP 123456 phải thành công: " + otpBody);
        assertTrue(otpBody.contains("Đăng ký thành công"));
    }

    @Test
    @DisplayName("TC_REG_08 & TC_REG_09: Sai OTP đăng ký bị từ chối và sai 5 lần luồng ADMIN_ADD bị khóa 15 phút")
    void testWrongOtpAndAdminLockoutAfterFiveAttempts() throws Exception {
        // Case 1: REGISTER sai OTP
        Map<String, Object> regSessionMap = MockTestEnvironment.newSessionMap();
        regSessionMap.put("otp", "654321");
        regSessionMap.put("authType", "REGISTER");
        HttpSession regSession = MockTestEnvironment.createInMemorySession(regSessionMap);
        HttpServletRequest regOtpReq = MockTestEnvironment.createBaseRequest(regSession, true);
        StringWriter regOtpOut = new StringWriter();
        HttpServletResponse regOtpResp = MockTestEnvironment.createResponseWithWriter(regOtpOut);
        when(regOtpReq.getServletPath()).thenReturn("/nhapma");
        when(regOtpReq.getParameter("otp")).thenReturn("000000");

        otpServlet.doPost(regOtpReq, regOtpResp);
        assertTrue(regOtpOut.toString().contains("Mã xác thực không chính xác hoặc đã hết hạn."));

        // Case 2: ADMIN_ADD sai OTP 5 lần -> khóa email 15 phút
        String lockedEmail = "otp_lock_" + System.nanoTime() + "@vsport.vn";
        Map<String, Object> adminSessionMap = MockTestEnvironment.newSessionMap();
        TaiKhoan tempAcc = new TaiKhoan();
        tempAcc.setEmail(lockedEmail);
        adminSessionMap.put("otp", "999999");
        adminSessionMap.put("authType", "ADMIN_ADD");
        adminSessionMap.put("tempAccount", tempAcc);

        HttpSession adminSession = MockTestEnvironment.createInMemorySession(adminSessionMap);
        String lastBody = "";
        for (int i = 1; i <= 5; i++) {
            HttpServletRequest req = MockTestEnvironment.createBaseRequest(adminSession, true);
            StringWriter sw = new StringWriter();
            HttpServletResponse resp = MockTestEnvironment.createResponseWithWriter(sw);
            when(req.getServletPath()).thenReturn("/nhapma");
            when(req.getParameter("otp")).thenReturn("000000");
            otpServlet.doPost(req, resp);
            lastBody = sw.toString();
        }
        assertTrue(lastBody.contains("\"lockedOut\": true"), "Lần sai thứ 5 phải khóa email: " + lastBody);
        assertTrue(OtpRateLimitUtil.isEmailLockedOut(lockedEmail), "OtpRateLimitUtil phải ghi nhận email đang bị khóa");
    }

    @Test
    @DisplayName("Exception Testing (assertThrows): PhoneUtil.lookupVariants ném IllegalArgumentException khi SĐT chưa chuẩn hóa")
    void testAssertThrowsInvalidPhoneLookupVariants() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PhoneUtil.lookupVariants("012345"),
                "SĐT chưa chuẩn hóa phải ném IllegalArgumentException"
        );
        assertTrue(ex.getMessage().contains("chưa được chuẩn hóa"));

        assertThrows(IllegalArgumentException.class, () -> PhoneUtil.lookupVariants(null));
    }

    @Test
    @DisplayName("Exception Testing (assertThrows): XacThucOTPServlet ném NullPointerException khi HttpServletRequest là null")
    void testAssertThrowsNullRequestOnOtpServlet() {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        assertThrows(
                NullPointerException.class,
                () -> otpServlet.doPost(null, resp),
                "Gọi doPost với request null phải ném NullPointerException"
        );
    }
}
