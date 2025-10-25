package dashboard;

import expensetracker.DatabaseManager;
import expensetracker.UserSession;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportsPanel extends JPanel {

    private final Color primaryGreen = new Color(76, 175, 80);
    private final Color textColor = new Color(51, 51, 51);

    private DatabaseManager dbManager = new DatabaseManager();
    private JTable transactionTable;
    private JButton deleteButton;
    private Dashboard parentFrame;

    public ReportsPanel(Dashboard parent) {
        this.parentFrame = parent;

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Transaction History", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(textColor);

        String[] columnNames = {"No.", "ID", "Type", "Category", "Amount ($)", "Date", "Description"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        transactionTable = new JTable(model);
        transactionTable.setFont(new Font("Arial", Font.PLAIN, 12));
        transactionTable.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(transactionTable);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setOpaque(false);

        deleteButton = new JButton("DELETE SELECTED TRANSACTION");
        deleteButton.setBackground(Color.RED);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);


        deleteButton.addActionListener(e -> deleteSelectedTransaction(model));

        controlPanel.add(deleteButton);

        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);


        loadTransactionData(model);


        hideIDColumn();
    }

    /** Hides the actual Transaction ID column (index 1) from the user. */
    private void hideIDColumn() {
        if (transactionTable.getColumnModel().getColumnCount() > 1) {
            transactionTable.getColumnModel().getColumn(1).setMinWidth(0);
            transactionTable.getColumnModel().getColumn(1).setMaxWidth(0);
            transactionTable.getColumnModel().getColumn(1).setWidth(0);
        }
    }

    private void deleteSelectedTransaction(DefaultTableModel model) {
        int selectedRow = transactionTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a transaction to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer transactionId = (Integer) model.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete Transaction ID: " + transactionId + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {

            int userId = UserSession.getCurrentUserId();

            String sql = "DELETE FROM transactions WHERE transaction_id = ? AND user_id = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, transactionId);
                pstmt.setInt(2, userId); // Filter by user ID

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Transaction deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);


                    loadTransactionData(model);


                    parentFrame.refreshDashboard();
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database Error during deletion: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /** Loads all transaction data from MySQL into the JTable model, sorting chronologically. */
    private void loadTransactionData(DefaultTableModel model) {
        model.setRowCount(0);

        String[] columnNames = {"No.", "ID", "Type", "Category", "Amount ($)", "Date", "Description"};
        model.setColumnIdentifiers(columnNames);

        int userId = UserSession.getCurrentUserId();

        String sql = "SELECT transaction_id, type, category, amount, transaction_date, description FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, transaction_id DESC";

        int rowNumber = 1;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId); // Set the user ID filter

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    model.addRow(new Object[]{

                            rowNumber++,

                            rs.getInt("transaction_id"),
                            rs.getString("type"),
                            rs.getString("category"),
                            rs.getDouble("amount"),
                            rs.getDate("transaction_date"),
                            rs.getString("description")
                    });
                }
            }

            hideIDColumn();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading reports: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}