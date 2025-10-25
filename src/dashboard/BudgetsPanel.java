package dashboard;

import expensetracker.DatabaseManager;
import expensetracker.UserSession;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;

public class BudgetsPanel extends JPanel {

    private final Color primaryGreen = new Color(76, 175, 80);
    private final Color textColor = new Color(51, 51, 51);

    private DatabaseManager dbManager = new DatabaseManager();

    private JComboBox<String> categoryComboBox;
    private JTextField limitField;
    private JButton saveBudgetButton;
    private JPanel budgetDisplayPanel; // Component to be refreshed

    public BudgetsPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // --- Title ---
        JLabel titleLabel = new JLabel("Manage Monthly Budgets", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(textColor);
        add(titleLabel, BorderLayout.NORTH);

        // Center: Contains Budget Creation Form and Table

        // FIX 1: The Display Panel is created FIRST
        budgetDisplayPanel = createBudgetDisplayPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createBudgetFormPanel(),
                budgetDisplayPanel);
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);

        // FIX 2: Call the refresh at the end to populate the display panel
        refreshBudgetStatus();
    }

    private JPanel createBudgetFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(primaryGreen),
                "Set New Budget Limit", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16), primaryGreen));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Category Label & Combo Box
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Category:"), gbc);

        String[] categories = {"Groceries", "Rent", "Utilities", "Entertainment", "Other"};
        categoryComboBox = new JComboBox<>(categories);
        categoryComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(categoryComboBox, gbc);

        // Limit ($) Label & Text Field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Monthly Limit ($):"), gbc);

        limitField = new JTextField(15);
        limitField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(limitField, gbc);

        // Save Button
        saveBudgetButton = new JButton("SAVE BUDGET");
        saveBudgetButton.setBackground(primaryGreen);
        saveBudgetButton.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);

        saveBudgetButton.addActionListener(e -> saveBudget());

        formPanel.add(saveBudgetButton, gbc);

        return formPanel;
    }

    // This panel is the container where the table is placed.
    private JPanel createBudgetDisplayPanel() {
        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(textColor),
                "Current Budget Status", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16), textColor));

        return displayPanel;
    }

    /**
     * Clears the old table and inserts the new, dynamically generated status table.
     */

    private void refreshBudgetStatus() {
        // 1. Generate the new table
        JTable budgetTable = createBudgetStatusTable();

        // 2. Clear the old content from the display container
        budgetDisplayPanel.removeAll();

        // 3. Add the new table, wrapped in a JScrollPane, to the CENTER
        budgetDisplayPanel.add(new JScrollPane(budgetTable), BorderLayout.CENTER);

        // 4. Force the layout to update
        budgetDisplayPanel.revalidate();
        budgetDisplayPanel.repaint();
    }

    // ====================================================================
    // DATABASE LOGIC
    // ====================================================================

    private void saveBudget() {
        String category = (String) categoryComboBox.getSelectedItem();
        String limitStr = limitField.getText();
        int userId = UserSession.getCurrentUserId();

        try {
            double limit = Double.parseDouble(limitStr);

            // SQL: INSERT OR UPDATE if the category already exists for this user
            String sql = "INSERT INTO budgets (category, monthly_limit, user_id) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE monthly_limit = VALUES(monthly_limit)";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, category);
                pstmt.setDouble(2, limit);
                pstmt.setInt(3, userId);

                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this,
                        category + " budget set to $" + limit,
                        "Budget Saved", JOptionPane.INFORMATION_MESSAGE);
                limitField.setText("");

                // Refresh the display table to show the new limit
                refreshBudgetStatus();

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                        "Database Error saving budget: " + e.getMessage(),
                        "SQL Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid number for the limit.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JTable createBudgetStatusTable() {
        String[] columnNames = {"Category", "Limit ($)", "Spent ($)", "Remaining ($)", "Status"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        DecimalFormat df = new DecimalFormat("#,##0.00");

        // Get the current date details for filtering transactions
        LocalDate now = LocalDate.now();
        String firstDayOfMonth = now.withDayOfMonth(1).toString();
        String lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth()).toString();
        int userId = UserSession.getCurrentUserId();

        // SQL: Complex query to join budgets with transactions for the current month
        String sql = "SELECT b.category, b.monthly_limit, " +
                "SUM(CASE WHEN t.type = 'Expense' AND t.transaction_date BETWEEN ? AND ? THEN t.amount ELSE 0 END) AS total_spent " +
                "FROM budgets b " +
                "LEFT JOIN transactions t ON b.category = t.category AND t.user_id = b.user_id " +
                "WHERE b.user_id = ? " + // Filter budgets by user ID
                "GROUP BY b.category, b.monthly_limit";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, firstDayOfMonth);
            pstmt.setString(2, lastDayOfMonth);
            pstmt.setInt(3, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("category");
                    double limit = rs.getDouble("monthly_limit");
                    double spent = rs.getDouble("total_spent");
                    double remaining = limit - spent;

                    String status = (spent > limit) ? "EXCEEDED!" : "OK";

                    model.addRow(new Object[]{
                            category,
                            df.format(limit),
                            df.format(spent),
                            df.format(remaining),
                            status
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading budget status: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Database Error loading budgets. Check console for details.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        JTable table = new JTable(model);
        return table;
    }
}