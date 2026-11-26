package org.example.mock;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chuyển đổi các cú pháp đặc thù của Microsoft SQL Server (T-SQL) sang cú pháp
 * tương thích với H2 (MODE=MSSQLServer) để toàn bộ JDBC/NativeQuery có sẵn chạy
 * trơn tru trong môi trường giả lập cục bộ.
 */
public final class SqlServerSqlTranslator {

    private static final Pattern TABLE_HINT_PATTERN = Pattern.compile(
            "\\bWITH\\s*\\(\\s*(?:UPDLOCK|ROWLOCK|NOLOCK|HOLDLOCK|READPAST|XLOCK|TABLOCK)(?:\\s*,\\s*(?:UPDLOCK|ROWLOCK|NOLOCK|HOLDLOCK|READPAST|XLOCK|TABLOCK))*\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DBO_PREFIX_PATTERN = Pattern.compile(
            "(?i)\\bdbo\\.");

    private static final Pattern SYS_COLUMNS_PARAM_PATTERN = Pattern.compile(
            "(?i)SELECT\\s+1\\s+FROM\\s+sys\\.columns\\s+WHERE\\s+object_id\\s*=\\s*OBJECT_ID\\s*\\(\\s*\\?\\s*\\)\\s+AND\\s+name\\s*=\\s*\\?");

    private static final Pattern SYS_COLUMNS_LITERAL_PATTERN = Pattern.compile(
            "(?i)SELECT\\s+1\\s+FROM\\s+sys\\.columns\\s+WHERE\\s+object_id\\s*=\\s*OBJECT_ID\\s*\\(\\s*N?'([^']+)'\\s*\\)\\s+AND\\s+name\\s*=\\s*N?'([^']+)'");

    private static final Pattern CAST_DATE_PLUS_TIME_PATTERN = Pattern.compile(
            "(?i)CAST\\s*\\(([^()]+?)\\s+AS\\s+DATETIME\\s*\\)\\s*\\+\\s*CAST\\s*\\(([^()]+?(?:\\([^()]*\\)[^()]*)*?)\\s+AS\\s+DATETIME\\s*\\)");

    private static final Pattern CONVERT_108_PATTERN = Pattern.compile(
            "(?i)CONVERT\\s*\\(\\s*VARCHAR\\s*\\(\\s*\\d+\\s*\\)\\s*,\\s*([^,]+?(?:\\([^()]*\\)[^,]*)*?)\\s*,\\s*108\\s*\\)");

    private static final Pattern CONVERT_DATE_PATTERN = Pattern.compile(
            "(?i)CONVERT\\s*\\(\\s*DATE\\s*,\\s*([^,]+?)\\s*\\)");

    private static final Pattern DATEADD_UNQUOTED_PATTERN = Pattern.compile(
            "(?i)\\bDATEADD\\s*\\(\\s*(year|yy|yyyy|quarter|qq|q|month|mm|m|dayofyear|dy|y|day|dd|d|week|wk|ww|hour|hh|minute|mi|n|second|ss|s|millisecond|ms)\\s*,");

    private static final Pattern DATEDIFF_UNQUOTED_PATTERN = Pattern.compile(
            "(?i)\\bDATEDIFF\\s*\\(\\s*(year|yy|yyyy|quarter|qq|q|month|mm|m|dayofyear|dy|y|day|dd|d|week|wk|ww|hour|hh|minute|mi|n|second|ss|s|millisecond|ms)\\s*,");

    private static final Pattern STRING_CONCAT_PLUS_PATTERN = Pattern.compile(
            "(?i)(COALESCE\\s*\\([^()]+\\))\\s*\\+\\s*'([^']*)'");

    private SqlServerSqlTranslator() {
    }

    /**
     * Bỏ tiền tố N của literal chuỗi N'...' và dbo. chỉ ở ngoài chuỗi literal.
     */
    private static String normalizeOutsideStringLiterals(String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        int n = sql.length();
        boolean inString = false;
        for (int i = 0; i < n; i++) {
            char c = sql.charAt(i);
            if (!inString) {
                if (c == '\'' ) {
                    inString = true;
                    sb.append(c);
                } else if ((c == 'N' || c == 'n') && i + 1 < n && sql.charAt(i + 1) == '\''
                        && (i == 0 || !Character.isLetterOrDigit(sql.charAt(i - 1)) && sql.charAt(i - 1) != '_')) {
                    // Bỏ N trước dấu nháy đơn
                } else if ((c == 'd' || c == 'D') && i + 3 < n
                        && sql.regionMatches(true, i, "dbo.", 0, 4)
                        && (i == 0 || !Character.isLetterOrDigit(sql.charAt(i - 1)) && sql.charAt(i - 1) != '_')) {
                    i += 3; // Bỏ qua "dbo."
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
                if (c == '\'') {
                    if (i + 1 < n && sql.charAt(i + 1) == '\'') {
                        sb.append('\'');
                        i++;
                    } else {
                        inString = false;
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String translate(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        String out = sql;

        // 1. Kiểm tra cột qua sys.columns
        if (SYS_COLUMNS_PARAM_PATTERN.matcher(out).find()) {
            return "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE LOWER(TABLE_NAME) = LOWER(REGEXP_REPLACE(?, '^dbo\\.', '')) " +
                    "AND LOWER(COLUMN_NAME) = LOWER(?)";
        }
        Matcher sysLit = SYS_COLUMNS_LITERAL_PATTERN.matcher(out);
        if (sysLit.find()) {
            String tbl = sysLit.group(1).replaceFirst("(?i)^dbo\\.", "");
            String col = sysLit.group(2);
            return "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE LOWER(TABLE_NAME) = LOWER('" + tbl + "') " +
                    "AND LOWER(COLUMN_NAME) = LOWER('" + col + "')";
        }

        // 2. Xóa hint khóa bảng SQL Server
        out = TABLE_HINT_PATTERN.matcher(out).replaceAll("");

        // 3. Bỏ prefix dbo. và N'...' an toàn ngoài string literal
        out = normalizeOutsideStringLiterals(out);

        // 4. TRY_CAST -> CAST, ISNULL -> COALESCE
        out = out.replaceAll("(?i)\\bTRY_CAST\\s*\\(", "CAST(");
        out = out.replaceAll("(?i)\\bISNULL\\s*\\(", "COALESCE(");

        // 5. COALESCE(...) + '...' -> CONCAT(COALESCE(...), '...')
        out = STRING_CONCAT_PLUS_PATTERN.matcher(out).replaceAll("CONCAT($1, '$2')");

        // 6. CAST(date AS DATETIME) + CAST(time AS DATETIME) -> ghép chuỗi rồi CAST AS TIMESTAMP
        Matcher dtPlus = CAST_DATE_PLUS_TIME_PATTERN.matcher(out);
        StringBuffer sbDt = new StringBuffer();
        while (dtPlus.find()) {
            String dateExpr = dtPlus.group(1).trim();
            String timeExpr = dtPlus.group(2).trim();
            String replacement = "CAST(CONCAT(CAST(" + dateExpr + " AS VARCHAR), ' ', SUBSTRING(CAST(" + timeExpr + " AS VARCHAR), 1, 8)) AS TIMESTAMP)";
            dtPlus.appendReplacement(sbDt, Matcher.quoteReplacement(replacement));
        }
        dtPlus.appendTail(sbDt);
        out = sbDt.toString();

        // 7. CONVERT(VARCHAR(8), expr, 108) -> SUBSTRING(CAST(expr AS VARCHAR), 1, 8)
        Matcher conv108 = CONVERT_108_PATTERN.matcher(out);
        StringBuffer sb108 = new StringBuffer();
        while (conv108.find()) {
            String expr = conv108.group(1).trim();
            conv108.appendReplacement(sb108, Matcher.quoteReplacement("SUBSTRING(CAST(" + expr + " AS VARCHAR), 1, 8)"));
        }
        conv108.appendTail(sb108);
        out = sb108.toString();

        // 8. CONVERT(DATE, expr) -> CAST(expr AS DATE)
        out = CONVERT_DATE_PATTERN.matcher(out).replaceAll("CAST($1 AS DATE)");

        // 9. DATEADD(part, ...) / DATEDIFF(part, ...) -> H2 cần dấu nháy đơn quanh đơn vị thời gian
        out = DATEADD_UNQUOTED_PATTERN.matcher(out).replaceAll("DATEADD('$1',");
        out = DATEDIFF_UNQUOTED_PATTERN.matcher(out).replaceAll("DATEDIFF('$1',");

        // 10. ALTER TABLE ADD ... NVARCHAR -> VARCHAR
        out = out.replaceAll("(?i)\\bNVARCHAR\\s*\\(\\s*MAX\\s*\\)", "CLOB");
        out = out.replaceAll("(?i)\\bNVARCHAR\\b", "VARCHAR");

        return out;
    }
}
