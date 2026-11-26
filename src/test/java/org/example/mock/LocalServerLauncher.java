package org.example.mock;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Trình khởi chạy Tomcat 10.1 nhúng cục bộ ở chế độ giả lập (không cần SQL Server).
 */
public class LocalServerLauncher {

    public static void main(String[] args) throws Exception {
        System.setProperty("vsport.mock.mode", "true");

        int port = Integer.parseInt(System.getProperty("server.port", "8080"));
        Path webappDir = Paths.get("src", "main", "webapp").toAbsolutePath();
        Path classesDir = Paths.get("target", "classes").toAbsolutePath();
        Path workDir = Paths.get("target", "tomcat-work").toAbsolutePath();
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
        ctx.setParentClassLoader(LocalServerLauncher.class.getClassLoader());
        org.apache.catalina.loader.WebappLoader webappLoader = new org.apache.catalina.loader.WebappLoader();
        webappLoader.setDelegate(true);
        ctx.setLoader(webappLoader);

        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(
                resources, "/WEB-INF/classes", classesDir.toString(), "/"));
        ctx.setResources(resources);

        // Kích hoạt log chi tiết nếu có lỗi khởi động
        File tempDir = workDir.resolve("tmp").toFile();
        tempDir.mkdirs();

        tomcat.start();

        System.out.println("============================================================");
        System.out.println("  V-SPORT Local Mock Server đã khởi động thành công!");
        System.out.println("  URL: http://localhost:" + port + "/");
        System.out.println("  File JSON dữ liệu mẫu: " + MockConfig.getMockDataFilePath());
        System.out.println("  Mã OTP mặc định (Mock Mode): " + MockConfig.DEFAULT_MOCK_OTP);
        System.out.println("  Tài khoản mẫu đã kích hoạt sẵn (mật khẩu chung: 123456):");
        System.out.println("    - Admin    : admin@vsport.vn    | 0901000001 | user: admin");
        System.out.println("    - Manager1 : manager1@vsport.vn | 0901000002 | user: manager1");
        System.out.println("    - Manager2 : manager2@vsport.vn | 0901000005 | user: manager2");
        System.out.println("    - Staff1   : staff1@vsport.vn   | 0901000004 | user: staff1");
        System.out.println("    - Khach1   : khach1@vsport.vn   | 0901000003 | user: khach1");
        System.out.println("    - Khach2   : khach2@vsport.vn   | 0901000006 | user: khach2");
        System.out.println("============================================================");

        tomcat.getServer().await();
    }
}
