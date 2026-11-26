package org.example.mock;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

/**
 * DataSource giả lập dùng H2 in-memory (MODE=MSSQLServer) kết hợp proxy dịch SQL Server -> H2.
 * Mọi thao tác đọc/ghi qua JDBC và Hibernate đều dùng chung pool này khi ở chế độ Mock.
 */
public class SqlServerCompatDataSource implements DataSource {

    private static final Logger logger = LogManager.getLogger(SqlServerCompatDataSource.class);
    public static final String H2_JDBC_URL =
            "jdbc:h2:mem:vsport;MODE=MSSQLServer;DATABASE_TO_LOWER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";

    private final HikariDataSource delegate;
    private volatile Runnable onMutationCallback;

    public SqlServerCompatDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl(H2_JDBC_URL);
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(15000);
        config.setPoolName("VSport-Mock-H2-Pool");
        this.delegate = new HikariDataSource(config);
        logger.info("SqlServerCompatDataSource: Đã khởi tạo pool H2 in-memory (MODE=MSSQLServer).");
    }

    public void setOnMutationCallback(Runnable onMutationCallback) {
        this.onMutationCallback = onMutationCallback;
    }

    /** Trả về kết nối H2 thô (không qua translator) dùng riêng cho khởi tạo schema/seed. */
    public Connection getRawConnection() throws SQLException {
        return delegate.getConnection();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return wrapConnection(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        // HikariDataSource không hỗ trợ getConnection(user, pass); dùng luôn pool H2 đã cấu hình
        return wrapConnection(delegate.getConnection());
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }

    public void close() {
        if (delegate != null && !delegate.isClosed()) {
            delegate.close();
        }
    }

    private Connection wrapConnection(Connection raw) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("prepareStatement".equals(name) && args != null && args.length > 0 && args[0] instanceof String) {
                        String originalSql = (String) args[0];
                        String translated = SqlServerSqlTranslator.translate(originalSql);
                        args[0] = translated;
                        try {
                            PreparedStatement ps = (PreparedStatement) method.invoke(raw, args);
                            return wrapPreparedStatement(ps, isMutatingSql(translated));
                        } catch (InvocationTargetException ex) {
                            throw ex.getTargetException();
                        }
                    }
                    if ("prepareCall".equals(name) && args != null && args.length > 0 && args[0] instanceof String) {
                        args[0] = SqlServerSqlTranslator.translate((String) args[0]);
                        try {
                            CallableStatement cs = (CallableStatement) method.invoke(raw, args);
                            return wrapPreparedStatement(cs, true);
                        } catch (InvocationTargetException ex) {
                            throw ex.getTargetException();
                        }
                    }
                    if ("createStatement".equals(name)) {
                        try {
                            Statement st = (Statement) method.invoke(raw, args);
                            return wrapStatement(st);
                        } catch (InvocationTargetException ex) {
                            throw ex.getTargetException();
                        }
                    }
                    try {
                        return method.invoke(raw, args);
                    } catch (InvocationTargetException ex) {
                        throw ex.getTargetException();
                    }
                });
    }

    private Statement wrapStatement(Statement raw) {
        return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    boolean mutated = false;
                    if (("execute".equals(name) || "executeQuery".equals(name) || "executeUpdate".equals(name)
                            || "executeLargeUpdate".equals(name) || "addBatch".equals(name))
                            && args != null && args.length > 0 && args[0] instanceof String) {
                        String translated = SqlServerSqlTranslator.translate((String) args[0]);
                        args[0] = translated;
                        mutated = isMutatingSql(translated);
                    }
                    try {
                        Object res = method.invoke(raw, args);
                        if (mutated || "executeBatch".equals(name)) {
                            notifyMutation();
                        }
                        return res;
                    } catch (InvocationTargetException ex) {
                        throw ex.getTargetException();
                    }
                });
    }

    private PreparedStatement wrapPreparedStatement(PreparedStatement raw, boolean mutating) {
        Class<?>[] interfaces = (raw instanceof CallableStatement)
                ? new Class<?>[]{CallableStatement.class}
                : new Class<?>[]{PreparedStatement.class};
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                interfaces,
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("setNString".equals(name) && args != null && args.length == 2 && args[0] instanceof Integer) {
                        raw.setString((Integer) args[0], (String) args[1]);
                        return null;
                    }
                    try {
                        Object res = method.invoke(raw, args);
                        if (mutating && ("executeUpdate".equals(name) || "execute".equals(name)
                                || "executeBatch".equals(name) || "executeLargeUpdate".equals(name))) {
                            notifyMutation();
                        }
                        return res;
                    } catch (InvocationTargetException ex) {
                        throw ex.getTargetException();
                    }
                });
    }

    private static boolean isMutatingSql(String sql) {
        if (sql == null) return false;
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("INSERT") || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE") || upper.startsWith("MERGE");
    }

    private void notifyMutation() {
        Runnable cb = this.onMutationCallback;
        if (cb != null) {
            cb.run();
        }
    }
}
