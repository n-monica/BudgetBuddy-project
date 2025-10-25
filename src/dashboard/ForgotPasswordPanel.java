package dashboard;

import expensetracker.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ForgotPasswordPanel extends JPanel {

    private final Color primaryGreen = new Color(76, 175, 80);
    private final Color textColor = new Color(51, 51, 51);

    private JTextField usernameField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton resetButton;

    private DatabaseManager dbManager = new DatabaseManager();
    private Login parentFrame;

    public ForgotPasswordPanel(Login parent) {
        this.parentFrame = parent;

        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel titleLabel = new JLabel("Reset Your Password", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(textColor);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 15, 0);
        add(Box.createVerticalStrut(15), gbc);
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Username:"), gbc);
        usernameField = new JTextField(20);
        gbc.gridx = 1;
        add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("New Password:"), gbc);
        newPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        add(newPasswordField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        add(new JLabel("Confirm New Password:"), gbc);
        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        add(confirmPasswordField, gbc);

        resetButton = new JButton("RESET PASSWORD");
        resetButton.setFont(new Font("Arial", Font.BOLD, 16));
        resetButton.setBackground(primaryGreen);
        resetButton.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 10, 10, 10);
        resetButton.addActionListener(e -> attemptPasswordReset());

        add(resetButton, gbc);

        JButton backButton = new JButton("<< Back to Login");
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setForeground(primaryGreen);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        backButton.addActionListener(e -> parentFrame.switchContent(parentFrame.createLoginFormComponent()));
        add(backButton, gbc);
    }

    private void attemptPasswordReset() {
        String username = usernameField.getText().trim();
        String newPass = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and new password are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;

        }

        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newPass);
            pstmt.setString(2, username);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Password for " + username + " reset successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                // Clear fields and return to login screen
                usernameField.setText("");
                newPasswordField.setText("");
                confirmPasswordField.setText("");

                parentFrame.switchContent(parentFrame.createLoginFormComponent());
            } else {
                JOptionPane.showMessageDialog(this, "User '" + username + "' not found.", "Reset Failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error during reset: " + e.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}