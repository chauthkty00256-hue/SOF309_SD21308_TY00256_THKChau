package org.example.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.model.TaiKhoan;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tiện ích khởi tạo môi trường Mock Data cô lập (H2 + bản sao JSON tạm trong target/)
 * và tạo HttpServletRequest / HttpServletResponse / HttpSession giả lập bằng Mockito.
 * Đảm bảo việc chạy mvn test không làm bẩn file mock-data/vsport-mock-data.json gốc.
 */
public final class MockTestEnvironment {

    private static volatile boolean initialized = false;

    private MockTestEnvironment() {
    }

    public static synchronized void initIsolatedMockDb() {
        if (initialized) {
            return;
        }
        try {
            Path sourceJson = Paths.get("mock-data", "vsport-mock-data.json").toAbsolutePath();
            Path tempDir = Paths.get("target", "test-mock-data").toAbsolutePath();
            Files.createDirectories(tempDir);
            Path isolatedJson = tempDir.resolve("vsport-mock-data-test.json");
            if (Files.exists(sourceJson)) {
                Files.copy(sourceJson, isolatedJson, StandardCopyOption.REPLACE_EXISTING);
            }
            System.setProperty("vsport.mock.mode", "true");
            System.setProperty("vsport.mock.file", isolatedJson.toString());
            initialized = true;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể khởi tạo file mock-data cô lập cho test", e);
        }
    }

    public static HttpSession createInMemorySession(Map<String, Object> backingStore) {
        HttpSession session = mock(HttpSession.class);
        lenient().when(session.getAttribute(anyString())).thenAnswer(inv -> backingStore.get(inv.getArgument(0)));
        lenient().doAnswer(inv -> {
            backingStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        lenient().doAnswer(inv -> {
            backingStore.remove(inv.getArgument(0));
            return null;
        }).when(session).removeAttribute(anyString());
        lenient().doAnswer(inv -> {
            backingStore.clear();
            return null;
        }).when(session).invalidate();
        return session;
    }

    public static HttpServletRequest createBaseRequest(HttpSession session, boolean isAjax) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        lenient().when(req.getContextPath()).thenReturn("");
        lenient().when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(req.getSession()).thenReturn(session);
        lenient().when(req.getSession(true)).thenReturn(session);
        lenient().when(req.getSession(false)).thenReturn(session);
        if (isAjax) {
            lenient().when(req.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        }
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        lenient().when(req.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        return req;
    }

    public static HttpServletResponse createResponseWithWriter(StringWriter writerOut) throws IOException {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        PrintWriter pw = new PrintWriter(writerOut, true);
        lenient().when(resp.getWriter()).thenReturn(pw);
        return resp;
    }

    public static TaiKhoan buildUser(int accountId, int roleId, Integer coSoId, String username, String email) {
        TaiKhoan user = new TaiKhoan();
        user.setAccountId(accountId);
        user.setRoleId(roleId);
        user.setCoSoId(coSoId);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName("Test User " + username);
        return user;
    }

    public static Map<String, Object> newSessionMap() {
        return new HashMap<>();
    }
}
