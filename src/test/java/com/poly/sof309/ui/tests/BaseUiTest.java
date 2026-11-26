package com.poly.sof309.ui.tests;

import com.poly.sof309.ui.listeners.ScreenshotTestListener;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * [Y4 - P2] Lớp nền cho các kịch bản Selenium + TestNG.
 * - Tự động kiểm tra máy chủ http://localhost:8080: nếu chưa chạy (ví dụ trên CI GitHub Actions),
 *   sẽ tự khởi động Embedded Tomcat 10.1 ở chế độ Mock Mode cô lập và tự tắt sau khi xong Suite.
 * - Tạo sẵn thư mục screenshots/ tại gốc dự án.
 */
public abstract class BaseUiTest {

    protected static String baseUrl;
    private static Tomcat embeddedTomcat;

    @BeforeSuite(alwaysRun = true)
    public void setUpSuite() throws Exception {
        Files.createDirectories(ScreenshotTestListener.SCREENSHOT_DIR);

        String configured = System.getProperty("base.url", "http://localhost:8080");
        if (configured == null || configured.isBlank() || configured.startsWith("${")) {
            configured = "http://localhost:8080";
        }
        baseUrl = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;

        if (!isServerReachable(baseUrl + "/")) {
            startIsolatedEmbeddedServer(8080);
        }
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        if (embeddedTomcat != null) {
            try {
                embeddedTomcat.stop();
                embeddedTomcat.destroy();
            } catch (Exception ignored) {
            } finally {
                embeddedTomcat = null;
            }
        }
    }

    private static boolean isServerReachable(String targetUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private static void startIsolatedEmbeddedServer(int port) throws Exception {
        Path sourceJson = Paths.get("mock-data", "vsport-mock-data.json").toAbsolutePath();
        Path tempDir = Paths.get("target", "test-mock-data").toAbsolutePath();
        Files.createDirectories(tempDir);
        Path isolatedJson = tempDir.resolve("vsport-ui-mock-data.json");
        if (Files.exists(sourceJson)) {
            Files.copy(sourceJson, isolatedJson, StandardCopyOption.REPLACE_EXISTING);
        }
        System.setProperty("vsport.mock.mode", "true");
        System.setProperty("vsport.mock.file", isolatedJson.toString());

        Path webappDir = Paths.get("src", "main", "webapp").toAbsolutePath();
        Path classesDir = Paths.get("target", "classes").toAbsolutePath();
        Path workDir = Paths.get("target", "tomcat-ui-work").toAbsolutePath();
        Files.createDirectories(workDir);

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(workDir.toString());
        tomcat.setPort(port);

        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(port);
        connector.setURIEncoding("UTF-8");
        tomcat.setConnector(connector);

        Context ctx = tomcat.addWebapp("", webappDir.toString());
        ctx.setReloadable(false);
        ctx.setParentClassLoader(BaseUiTest.class.getClassLoader());
        org.apache.catalina.loader.WebappLoader webappLoader = new org.apache.catalina.loader.WebappLoader();
        webappLoader.setDelegate(true);
        ctx.setLoader(webappLoader);

        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", classesDir.toString(), "/"));
        ctx.setResources(resources);

        tomcat.start();
        embeddedTomcat = tomcat;
        baseUrl = "http://localhost:" + port;
    }
}
