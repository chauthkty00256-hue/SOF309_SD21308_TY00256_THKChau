package org.example.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JPAUtil {
    private static EntityManagerFactory factory;
    private static final Logger logger = LogManager.getLogger(JPAUtil.class);

    static {
        try {
            java.util.Map<String, Object> properties = new java.util.HashMap<>();
            if (DBUtil.isMockMode()) {
                // Dùng chung SqlServerCompatDataSource (H2 in-memory + SQL translator) với DBUtil
                properties.put("hibernate.connection.provider_class",
                        "org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl");
                properties.put("hibernate.connection.datasource", DBUtil.getMockDataSource());
                properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
                properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                properties.put("hibernate.hbm2ddl.auto", "none");
                properties.put("hibernate.show_sql", "false");
            } else {
                properties.put("jakarta.persistence.jdbc.url", DBUtil.getURL());
                properties.put("jakarta.persistence.jdbc.user", DBUtil.getUSER());
                properties.put("jakarta.persistence.jdbc.password", DBUtil.getPASSWORD());
                // persistence.xml cấu hình hibernate.connection.provider_class = HikariCPConnectionProvider,
                // nghĩa là Hibernate tự dựng MỘT HikariCP pool RIÊNG (không dùng chung dataSource của
                // DBUtil). encrypt/trustServerCertificate phải được khai báo lại ở đây - xem DBUtil.java
                // để biết vì sao không thể chỉ dựa vào property nhúng trong DB_URL.
                properties.put("hibernate.hikari.dataSource.encrypt", DBUtil.getEncrypt());
                properties.put("hibernate.hikari.dataSource.trustServerCertificate", DBUtil.getTrustServerCertificate());
            }

            factory = Persistence.createEntityManagerFactory("SportPU", properties);
            logger.info("JPAUtil: EntityManagerFactory created successfully for SportPU (mockMode={})", DBUtil.isMockMode());
        } catch (Throwable ex) {
            logger.error("JPAUtil: Initial EntityManagerFactory creation failed: {}", ex.getMessage(), ex);
            throw new ExceptionInInitializerError("Failed to create EntityManagerFactory for SportPU: " + ex.getMessage());
        }
    }

    public static EntityManager getEntityManager() {
        if (factory == null || !factory.isOpen()) {
            throw new IllegalStateException("EntityManagerFactory is not initialized or has been closed");
        }
        return factory.createEntityManager();
    }

    public static void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
            System.out.println("JPAUtil: EntityManagerFactory closed");
        }
    }
}
