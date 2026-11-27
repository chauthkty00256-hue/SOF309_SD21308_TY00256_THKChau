package org.example.controller.manager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.controller.MockTestEnvironment;
import org.example.exception.ForbiddenException;
import org.example.exception.NotFoundException;
import org.example.model.TaiKhoan;
import org.example.service.manager.CaLamService;
import org.example.service.manager.NhanSuService;
import org.example.service.manager.SanService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * [Y3] Unit Test JUnit 5 cho KhuyenMaiManagerServlet, QuanLySanManagerServlet
 * cùng các Service nghiệp vụ Manager (SanService, NhanSuService, CaLamService).
 * Tập trung kiểm thử biên (@ParameterizedTest + @CsvSource) và ngoại lệ nghiệp vụ (assertThrows).
 */
@DisplayName("Manager Controllers (Khuyến mãi, Sân, Nhân sự & Ca làm) - Unit Tests")
public class QuanLySanAndKhuyenMaiControllerTest {

    private KhuyenMaiManagerServlet khuyenMaiServlet;
    private QuanLySanManagerServlet quanLySanServlet;
    private SanService sanService;
    private NhanSuService nhanSuService;
    private CaLamService caLamService;

    @BeforeAll
    static void initMockDb() {
        MockTestEnvironment.initIsolatedMockDb();
    }

    @BeforeEach
    void setUp() {
        khuyenMaiServlet = new KhuyenMaiManagerServlet();
        quanLySanServlet = new QuanLySanManagerServlet();
        sanService = new SanService();
        nhanSuService = new NhanSuService();
        caLamService = new CaLamService();
    }

    @ParameterizedTest(name = "[{index}] {9}")
    @DisplayName("TC_MGR_10: Kiểm tra biên Form tạo Khuyến mãi tại KhuyenMaiManagerServlet qua @CsvSource")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @CsvSource(nullValues = "NULL", value = {
            // maCode, loaiGiam, giaTriGiam, start, end, soLanToiDa, giaTriToiThieu, giamToiDa, expectedError, caseLabel
            "VSPORT10, PERCENT, 10, 2026-12-01, 2026-12-31, 100, 100000, 50000, đã tồn tại, Trùng mã VSPORT10 toàn hệ thống",
            "NEWCODE01, INVALID, 10, 2026-12-01, 2026-12-31, 100, 100000, 50000, Vui lòng chọn hình thức giảm., Loại giảm không hợp lệ",
            "NEWCODE02, PERCENT, 0, 2026-12-01, 2026-12-31, 100, 100000, 50000, Giá trị giảm phải lớn hơn 0., Biên giaTriGiam = 0",
            "NEWCODE03, PERCENT, -5, 2026-12-01, 2026-12-31, 100, 100000, 50000, Giá trị giảm phải lớn hơn 0., giaTriGiam âm",
            "NEWCODE04, PERCENT, 101, 2026-12-01, 2026-12-31, 100, 100000, 50000, Phần trăm giảm không được vượt quá 100., Biên PERCENT > 100 (101%)",
            "NEWCODE05, FIXED, 20000, 2026-12-15, 2026-12-10, 100, 100000, 50000, Ngày kết thúc phải sau ngày bắt đầu., Ngày kết thúc < ngày bắt đầu",
            "NEWCODE06, FIXED, 20000, '', 2026-12-31, 100, 100000, 50000, Vui lòng nhập đầy đủ ngày bắt đầu và ngày kết thúc., Thiếu ngày bắt đầu",
            "NEWCODE07, FIXED, 20000, 2026-12-01, 2026-12-31, 0, 100000, 50000, Tổng lượt sử dụng phải lớn hơn 0., Biên soLanToiDa = 0",
            "NEWCODE08, FIXED, 20000, 2026-12-01, 2026-12-31, 100, -1, 50000, Giá trị đơn tối thiểu không được âm., Biên giaTriToiThieu = -1",
            "NEWCODE09, FIXED, 20000, 2026-12-01, 2026-12-31, 100, 100000, -1, Mức giảm tối đa không được âm., Biên giamToiDa = -1"
    })
    void testCreatePromotionValidationBoundaries(
            String maCode,
            String loaiGiam,
            String giaTriGiam,
            String ngayBatDau,
            String ngayKetThuc,
            String soLanToiDa,
            String giaTriToiThieu,
            String giamToiDa,
            String expectedError,
            String caseLabel
    ) throws Exception {
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        TaiKhoan manager1 = MockTestEnvironment.buildUser(2, 2, 1, "manager1", "manager1@vsport.vn");
        sessionMap.put("user", manager1);
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, false);
        when(req.getParts()).thenReturn(Collections.emptyList());
        when(req.getParameter("action")).thenReturn("create");
        when(req.getParameter("maCode")).thenReturn(maCode);
        when(req.getParameter("moTa")).thenReturn("Mô tả test");
        when(req.getParameter("loaiGiam")).thenReturn(loaiGiam);
        when(req.getParameter("giaTriGiam")).thenReturn(giaTriGiam);
        when(req.getParameter("ngayBatDau")).thenReturn(ngayBatDau);
        when(req.getParameter("ngayKetThuc")).thenReturn(ngayKetThuc);
        when(req.getParameter("soLanToiDa")).thenReturn(soLanToiDa);
        when(req.getParameter("giaTriToiThieu")).thenReturn(giaTriToiThieu);
        when(req.getParameter("giamToiDa")).thenReturn(giamToiDa);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        khuyenMaiServlet.doPost(req, resp);

        String actualError = (String) sessionMap.get("errorMsg");
        assertNotNull(actualError, caseLabel + " phải đặt errorMsg");
        assertTrue(actualError.contains(expectedError),
                caseLabel + " phải chứa [" + expectedError + "], thực tế: " + actualError);
    }

    @Test
    @DisplayName("TC_AUTH_12: QuanLySanManagerServlet điều hướng /dangnhap với Customer (Role 3) và trả 403 khi Manager thiếu CoSoID")
    void testQuanLySanForbiddenForCustomer() throws Exception {
        // Case 1: Customer (Role 3) -> redirect /dangnhap
        Map<String, Object> sessionMap = MockTestEnvironment.newSessionMap();
        sessionMap.put("user", MockTestEnvironment.buildUser(3, 3, null, "khach1", "khach1@vsport.vn"));
        HttpSession session = MockTestEnvironment.createInMemorySession(sessionMap);

        HttpServletRequest req = MockTestEnvironment.createBaseRequest(session, false);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        quanLySanServlet.doGet(req, resp);
        verify(resp).sendRedirect("/dangnhap");

        // Case 2: Manager không có coSoId -> 403 Forbidden
        Map<String, Object> mgrMap = MockTestEnvironment.newSessionMap();
        mgrMap.put("user", MockTestEnvironment.buildUser(98, 2, null, "orphanMgr", "orphan@vsport.vn"));
        HttpSession mgrSession = MockTestEnvironment.createInMemorySession(mgrMap);
        HttpServletRequest mgrReq = MockTestEnvironment.createBaseRequest(mgrSession, false);
        HttpServletResponse mgrResp = mock(HttpServletResponse.class);

        quanLySanServlet.doGet(mgrReq, mgrResp);
        verify(mgrResp).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    @DisplayName("TC_MGR_07 (assertThrows): SanService.createSan ném IllegalArgumentException khi trùng tên sân, tên trống hoặc trạng thái sai")
    void testSanServiceCreateExceptions() {
        // 1. Trùng tên sân trong CoSo 1
        SanService.SanCreateRequest dupReq = new SanService.SanCreateRequest();
        dupReq.setTenSan("Sân 5A - Cỏ FIFA");
        dupReq.setLoaiSanId(1);
        dupReq.setTrangThai("Sẵn sàng");

        IllegalArgumentException dupEx = assertThrows(
                IllegalArgumentException.class,
                () -> sanService.createSan(dupReq, 1)
        );
        assertTrue(dupEx.getMessage().contains("Tên sân đã tồn tại"));

        // 2. Tên sân rỗng & trạng thái không hợp lệ
        SanService.SanCreateRequest invalidReq = new SanService.SanCreateRequest();
        invalidReq.setTenSan("");
        invalidReq.setLoaiSanId(0);
        invalidReq.setTrangThai("TrangThaiLa");

        assertThrows(
                IllegalArgumentException.class,
                () -> sanService.createSan(invalidReq, 1),
                "Dữ liệu sân sai phải ném IllegalArgumentException"
        );
    }

    @Test
    @DisplayName("TC_MGR_08 (assertThrows): SanService chặn xóa Loại sân đang có sân liên kết (LoaiSanID=1), chặn trạng thái sai và chặn xóa sân chéo chi nhánh (SanID=6)")
    void testSanServiceDeleteBusinessAndBranchExceptions() {
        // LoaiSanID=1 (CoSo 1) đang có 2 sân liên kết (SanID=1, SanID=2) -> IllegalArgumentException
        IllegalArgumentException linkedEx = assertThrows(
                IllegalArgumentException.class,
                () -> sanService.deleteLoaiSan(1, 1)
        );
        assertTrue(linkedEx.getMessage().contains("Không thể xóa loại sân này"));

        // Trạng thái sân ngoài whitelist -> IllegalArgumentException
        IllegalArgumentException statusEx = assertThrows(
                IllegalArgumentException.class,
                () -> sanService.updateSanStatus(1, "TrangThaiKhongHopLe", 1)
        );
        assertTrue(statusEx.getMessage().contains("Trạng thái sân không hợp lệ"));

        // SanID=6 thuộc CoSo 2, Manager CoSo 1 không được xóa -> ForbiddenException
        assertThrows(
                ForbiddenException.class,
                () -> sanService.deleteSan(6, 1)
        );
    }

    @Test
    @DisplayName("TC_MGR_11 (assertThrows): NhanSuService chặn nâng quyền lên Role 1/2 và chặn xóa tài khoản Manager")
    void testNhanSuServicePrivilegeEscalationAndDeleteManagerExceptions() {
        // Nâng cấp staff1 (ID=4, CoSo=1) lên Manager (Role 2)
        NhanSuService.StaffUpdateRequest escalateReq = new NhanSuService.StaffUpdateRequest();
        escalateReq.setRoleId(2);

        IllegalArgumentException escEx = assertThrows(
                IllegalArgumentException.class,
                () -> nhanSuService.updateStaff(4, escalateReq, 1)
        );
        assertTrue(escEx.getMessage().contains("Không thể nâng cấp lên quyền Quản trị hoặc Quản lý"));

        // Xóa tài khoản manager1 (ID=2, CoSo=1)
        IllegalArgumentException delEx = assertThrows(
                IllegalArgumentException.class,
                () -> nhanSuService.deleteStaff(2, 1)
        );
        assertTrue(delEx.getMessage().contains("Không thể xóa tài khoản có quyền Quản trị hoặc Quản lý"));
    }

    @Test
    @DisplayName("TC_MGR_12 (assertThrows): CaLamService.createShift ném NotFoundException & IllegalArgumentException khi dữ liệu ca sai")
    void testCaLamServiceExceptions() {
        // Request null -> IllegalArgumentException
        assertThrows(
                IllegalArgumentException.class,
                () -> caLamService.createShift(null, 1)
        );

        // Nhân viên không tồn tại (accountId=99999) -> NotFoundException
        CaLamService.CaLamRequest notFoundReq = new CaLamService.CaLamRequest();
        notFoundReq.setAccountId(99999);
        notFoundReq.setCoSoId(1);
        notFoundReq.setNgayLam(LocalDate.now().plusDays(2));
        notFoundReq.setGioBatDau(LocalTime.of(6, 0));
        notFoundReq.setGioKetThuc(LocalTime.of(14, 0));

        NotFoundException nfEx = assertThrows(
                NotFoundException.class,
                () -> caLamService.createShift(notFoundReq, 1)
        );
        assertTrue(nfEx.getMessage().contains("Nhân viên không tồn tại"));
    }
}
