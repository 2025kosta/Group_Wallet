package main.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbUtil {

	// 환경변수 우선 → 없으면 기본값
	private static final String HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
	private static final String PORT = System.getenv().getOrDefault("DB_PORT", "3306");
	private static final String NAME = System.getenv().getOrDefault("DB_NAME", "group_wallet");
	private static final String USER = System.getenv().getOrDefault("DB_USER", "wallet");
	private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "wallet");

	private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + NAME +
			"?useLegacyDatetimeCode=false&serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=UTF-8";

	public static Connection getConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver"); // JDBC 드라이버
			return DriverManager.getConnection(URL, USER, PASSWORD);
		} catch (ClassNotFoundException e) {
			System.out.println("❗ JDBC 드라이버를 찾을 수 없습니다.");
			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println("❗ 데이터베이스 연결에 실패했습니다. URL=" + URL);
			e.printStackTrace();
		}
		return null;
	}

	public static void close(AutoCloseable... resources) {
		for (AutoCloseable r : resources) {
			if (r != null) try { r.close(); } catch (Exception ignored) {}
		}
	}
}
