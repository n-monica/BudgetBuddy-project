package dashboard;

import expensetracker.DatabaseManager;
import expensetracker.UserSession;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionForm extends JPanel {

    private final Color primaryGreen = new Color(76, 175, 80);
    private final Color lightGray = new Color(240, 240, 240);
    private final Color textColor = new Color(51, 51, 51);

    private JTextField amountField;
    private JComboBox<String> typeComboBox;
    private JComboBox<String> categoryComboBox;
    private JTextArea descriptionArea;
    private JTextField dateField;
    private JButton saveButton;

    private DatabaseManager dbManager = new DatabaseManager();

    // Reference to the parent Dashboard frame
    private Dashboard parentFrame;

    // Constructor accepts the parent Dashboard object
    public TransactionForm(Dashboard parent) {
        this.parentFrame = parent; // Store the reference

        // Set up the panel appearance
        setBackground(lightGray);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Title ---
        JLabel titleLabel = new JLabel("Record New Transaction", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(textColor);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        add(titleLabel, gbc);

        // Add a vertical spacer
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        add(Box.createVerticalStrut(20), gbc);
        gbc.insets = new Insets(10, 10, 10, 10); // Reset padding

        // --- Transaction Type (Income/Expense) ---
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(createLabel("Transaction Type:"), gbc);

        typeComboBox = new JComboBox<>(new String[]{"Expense", "Income"});
        typeComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        add(typeComboBox, gbc);

        // --- Amount ---
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(createLabel("Amount ($):"), gbc);

        amountField = new JTextField(15);
        amountField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        add(amountField, gbc);

        // --- Category ---
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(createLabel("Category:"), gbc);

        String[] categories = {"Groceries", "Salary", "Rent", "Utilities", "Entertainment", "Other"};
        categoryComboBox = new JComboBox<>(categories);
        categoryComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        add(categoryComboBox, gbc);

        // --- Date ---
        gbc.gridx = 0;
        gbc.gridy = 5;
        add(createLabel("Date (YYYY-MM-DD):"), gbc);

        dateField = new JTextField(15);
        dateField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        add(dateField, gbc);

        // --- Description ---
        gbc.gridx = 0;
        gbc.gridy = 6;
        add(createLabel("Description:"), gbc);

        descriptionArea = new JTextArea(3, 15);
        descriptionArea.setFont(new Font("Arial", Font.PLAIN, 14));
        descriptionArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        JScrollPane scrollPane = new JScrollPane(descriptionArea);

        gbc.gridx = 1;
        gbc.weighty = 0.5;
        add(scrollPane, gbc);

        // --- Save Button ---
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(20, 10, 10, 10);

        saveButton = new JButton("SAVE TRANSACTION");
        saveButton.setFont(new Font("Arial", Font.BOLD, 16));
        saveButton.setBackground(primaryGreen);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        // Link action to the save method
        saveButton.addActionListener(e -> saveTransaction());

        add(saveButton, gbc);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text + " ");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(textColor);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private void saveTransaction() {
        // Retrieve all data from the form fields
        String type = (String) typeComboBox.getSelectedItem();
        String category = (String) categoryComboBox.getSelectedItem();
        String description = descriptionArea.getText();
        String date = dateField.getText();

        // 1. Input Validation
        if (amountField.getText().isEmpty() || date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Amount and Date are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Core Logic
        int userId = UserSession.getCurrentUserId(); // Get the active user's ID
        String sql = "INSERT INTO transactions (user_id, type, amount, category, description, transaction_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection is unavailable.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Set the parameters for the SQL query
            pstmt.setInt(1, userId); // FILTER: Secure the transaction by user ID
            pstmt.setString(2, type);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, category);
            pstmt.setString(5, description);
            pstmt.setString(6, date);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Transaction saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                // Clear fields after successful save
                amountField.setText("");
                descriptionArea.setText("");
                dateField.setText("");

                // FINAL FIX: Tell the parent Dashboard to refresh its totals
                parentFrame.refreshDashboard();
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage() + ". Check your date format (YYYY-MM-DD).", "SQL Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}