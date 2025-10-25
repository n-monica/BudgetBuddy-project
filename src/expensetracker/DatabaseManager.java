package expensetracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/expense_income_db";
    private static final String USER = "root";
    private static final String PASS = "Solamata@18";

    /**
     * Establishes a connection to the MySQL database.
     * @return A live Connection object, or null if the connection fails due to bad credentials/path.
     */
    public static Connection getConnection() {
        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            return DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Please ensure the JAR is correctly added.");
            return null;
        } catch (SQLException e) {
            System.err.println("Database connection failed. Check URL, USER, and PASS variables in DatabaseManager.java.");

            e.printStackTrace();
            return null;
        }
    }

    /**
     * Authenticates a user against the 'users' table.
     * @return The user_id (int) on success, or -1 on failure.
     */
    public int authenticateUser(String username, String password) {

        String sql = "SELECT user_id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) return -1;

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id"); // Return the user's unique ID
                }
                return -1;
            }

        } catch (SQLException e) {
            System.err.println("Authentication query failed.");
            e.printStackTrace();
            return -1;
        }
    }
}