package expensetracker;

public class UserSession {
    private static int currentUserId = -1;
    private static String currentUsername = null;

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }

    public static void clearSession() {
        currentUserId = -1;
        currentUsername = null;
    }
}