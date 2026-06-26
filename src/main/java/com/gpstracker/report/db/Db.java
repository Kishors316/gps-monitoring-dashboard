package com.gpstracker.report.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Plain JDBC via DriverManager - no connection pool, no extra libraries.
 * The only JAR you must drop into WEB-INF/lib is the MySQL Connector/J driver.
 * Call configure(...) once at startup (done by ApiServlet.init from web.xml params).
 */
public final class Db {

    private static String url;
    private static String user;
    private static String pass;

    public static synchronized void configure(String host, String port, String name,
                                              String u, String p) {
        url = "jdbc:mysql://" + host + ":" + port + "/" + name
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        user = u;
        pass = p;
        // Explicitly load the driver so a missing JAR fails loudly with a clear message.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "MySQL driver not found. Download mysql-connector-j-*.jar and put it "
                + "in WEB-INF/lib, then restart Tomcat.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (url == null) throw new SQLException("Db not configured");
        return DriverManager.getConnection(url, user, pass);
    }

    private Db() {}
}
