package dashboard;

import expensetracker.DatabaseManager;
import expensetracker.UserSession;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

public class Dashboard extends JFrame {

    private final Color primaryGreen = new Color(76, 175, 80);   // #4CAF50
    private final Color primaryDark = new Color(44, 62, 80);     // Navy Blue (Sidebar Look)
    private final Color secondaryGreen = new Color(60, 140, 64);
    private final Color lightGray = new Color(240, 240, 240);
    private final Color textColor = new Color(51, 51, 51);

    private DatabaseManager dbManager = new DatabaseManager();
    private JPanel mainContentPanel;

    public Dashboard() {

        setTitle("BudgetBuddy - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);


        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(createDashboardContent(), BorderLayout.CENTER);
        add(mainContentPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    /**
     * Public method to refresh the main Dashboard view.
     * Called by TransactionForm and ReportsPanel after data changes.
     */
    public void refreshDashboard() {

        mainContentPanel.removeAll();

        mainContentPanel.add(createDashboardContent(), BorderLayout.CENTER);


        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    /**
     * Method to dynamically swap the content panel in the center of the frame.
     */
    public void switchContent(JPanel newPanel) {
        mainContentPanel.removeAll();
        mainContentPanel.add(newPanel, BorderLayout.CENTER);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(primaryDark);
        sidebar.setPreferredSize(new Dimension(220, 700));
        sidebar.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel title = new JLabel("BudgetBuddy", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.insets = new Insets(30, 10, 40, 10);
        sidebar.add(title, gbc);

        gbc.insets = new Insets(10, 10, 10, 10);


        String[] menuItems = {"Dashboard", "Transactions", "Reports", "Budgets", "Settings"};
        for (int i = 0; i < menuItems.length; i++) {
            JButton btn = createSidebarButton(menuItems[i]);
            gbc.gridy = i + 1;
            sidebar.add(btn, gbc);
        }

        gbc.weighty = 1.0;
        gbc.gridy = menuItems.length + 1;
        sidebar.add(Box.createVerticalGlue(), gbc);

        return sidebar;
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(primaryDark);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(secondaryGreen);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(primaryDark);
            }
        });

        button.addActionListener(e -> {
            if (text.equals("Dashboard")) {
                refreshDashboard();
            } else if (text.equals("Transactions")) {
                // Passes 'this' for refresh logic
                switchContent(new TransactionForm(this));
            } else if (text.equals("Reports")) {
                // Passes 'this' for deletion/refresh logic
                switchContent(new ReportsPanel(this));
            } else if (text.equals("Budgets")) {
                switchContent(new BudgetsPanel());
            } else if (text.equals("Settings")) {
                // Passes 'this' for logout/password change logic
                switchContent(new SettingsPanel(this));
            } else {
                JOptionPane.showMessageDialog(this, "Navigating to: " + text);
            }
        });

        return button;
    }


    private double getTransactionSum(String type) {
        double sum = 0.0;
        int userId = UserSession.getCurrentUserId();
        String sql = "SELECT SUM(amount) AS total FROM transactions WHERE type = ? AND user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, type);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    sum = rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching sum for type " + type + ": " + e.getMessage());
        }
        return sum;
    }

    private DefaultPieDataset createChartDataset() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        int userId = UserSession.getCurrentUserId();

        // SQL: Sum expenses by category for the current user
        String sql = "SELECT category, SUM(amount) AS total_spent " +
                "FROM transactions " +
                "WHERE type = 'Expense' AND user_id = ? " +
                "GROUP BY category";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dataset.setValue(rs.getString("category"), rs.getDouble("total_spent"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching chart data: " + e.getMessage());
        }
        return dataset;
    }


    private JPanel createDashboardContent() {

        double totalIncome = getTransactionSum("Income");
        double totalExpenses = getTransactionSum("Expense");
        double netSavings = totalIncome - totalExpenses;

        DecimalFormat df = new DecimalFormat("#,##0.00");
        String incomeStr = String.format("$%s", df.format(totalIncome));
        String expensesStr = String.format("$%s", df.format(totalExpenses));
        String savingsStr = String.format("$%s", df.format(netSavings));

        Color savingsColor = (netSavings >= 0) ? primaryGreen : Color.RED;


        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(lightGray);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        summaryPanel.setOpaque(false);

        summaryPanel.add(createSummaryCard("Total Income", incomeStr, Color.BLUE));
        summaryPanel.add(createSummaryCard("Total Expenses", expensesStr, Color.RED));
        summaryPanel.add(createSummaryCard("Net Savings", savingsStr, savingsColor)); // Dynamic color

        contentPanel.add(summaryPanel, BorderLayout.NORTH);


        JFreeChart chart = ChartFactory.createPieChart(
                "Expense Breakdown by Category",
                createChartDataset(),
                true,
                true,
                false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chartPanel.setBackground(Color.WHITE);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(chartPanel, BorderLayout.CENTER);

        contentPanel.add(centerPanel, BorderLayout.CENTER);

        return contentPanel;
    }

    private JPanel createSummaryCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setForeground(textColor);
        card.add(titleLabel, BorderLayout.NORTH);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setForeground(color);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard());
    }
}