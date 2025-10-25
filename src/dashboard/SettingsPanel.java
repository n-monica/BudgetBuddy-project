package dashboard;

import expensetracker.DatabaseManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SettingsPanel extends JPanel {

    private final Color primaryGreen = new Color(76, 175, 80);
    private final Color textColor = new Color(51, 51, 51);
    private final Color lightGray = new Color(240, 240, 240);

    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    private expensetracker.DatabaseManager dbManager = new expensetracker.DatabaseManager();
    // NOTE: This should be dynamically fetched from UserSession in a real app.
    private String currentUsername = "user1";

    private Dashboard parentFrame;

    public SettingsPanel(Dashboard parent) {
        this.parentFrame = parent;

        setLayout(new BorderLayout(20, 20));
        setBackground(lightGray);
        setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // --- Title ---
        JLabel titleLabel = new JLabel("Application Settings", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(textColor);
        add(titleLabel, BorderLayout.NORTH);

        // --- Center Content: Account Info & Password Form ---
        JPanel infoPanel = createAccountInfoPanel();
        add(infoPanel, BorderLayout.CENTER);

        // Add Logout button panel to the bottom
        add(createLogoutPanel(), BorderLayout.SOUTH);
    }

    private JPanel createAccountInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(primaryGreen),
                "User Account Details",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 18), primaryGreen));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;

        // --- DISPLAY USER INFO ---

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(createDetailLabel(currentUsername), gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(createDetailLabel("test@budgetbuddy.com"), gbc);

        // Member Since
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Member Since:"), gbc);
        gbc.gridx = 1;
        panel.add(createDetailLabel("October 2025"), gbc);

        // --- PASSWORD CHANGE FORM ---

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 20, 0);
        panel.add(separator, gbc);

        JLabel changeTitle = new JLabel("Change Password:", SwingConstants.CENTER);
        changeTitle.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridy = 4; gbc.gridwidth = 2; gbc.insets = new Insets(10, 15, 5, 15);
        panel.add(changeTitle, gbc);

        // Old Password
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("Old Password:"), gbc);
        oldPasswordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(oldPasswordField, gbc);

        // New Password
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        panel.add(new JLabel("New Password:"), gbc);
        newPasswordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(newPasswordField, gbc);

        // Confirm New Password
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0;
        panel.add(new JLabel("Confirm New:"), gbc);
        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(confirmPasswordField, gbc);

        // Update Button
        JButton updatePasswordButton = new JButton("UPDATE PASSWORD");
        updatePasswordButton.setBackground(Color.RED);
        updatePasswordButton.setForeground(Color.WHITE);
        updatePasswordButton.addActionListener(e -> changePassword());

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 15, 10, 15);
        panel.add(updatePasswordButton, gbc);

        return panel;
    }

    private JPanel createLogoutPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setOpaque(false);

        JButton logoutButton = new JButton("LOGOUT & EXIT");
        logoutButton.setBackground(Color.DARK_GRAY);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFont(new Font("Arial", Font.BOLD, 14));

        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to log out and exit the application?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Clears the session and closes the entire application
                expensetracker.UserSession.clearSession();
                parentFrame.dispose();
            }
        });

        panel.add(logoutButton);
        return panel;
    }

    private JLabel createDetailLabel(String text) {
        JLabel label = new JLabel("<html><b>" + text + "</b></html>");
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        return label;
    }

    private void changePassword() {
        String oldPass = new String(oldPasswordField.getPassword());
        String newPass = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        if (newPass.length() < 4) {
            JOptionPane.showMessageDialog(this, "New password must be at least 4 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (dbManager.authenticateUser(currentUsername, oldPass)==-1) {
            JOptionPane.showMessageDialog(this, "The old password entered is incorrect.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newPass);
            pstmt.setString(2, currentUsername);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Password updated successfully! Please log in again.", "Success", JOptionPane.INFORMATION_MESSAGE);
                oldPasswordField.setText("");
                newPasswordField.setText("");
                confirmPasswordField.setText("");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: Could not update password.", "SQL Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}