package dashboard;

import expensetracker.DatabaseManager;
import expensetracker.UserSession;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Login extends JFrame {


    private final Color primaryGreen = new Color(76, 175, 80);
    private final Color secondaryGreen = new Color(60, 140, 64);
    private final Color lightGray = new Color(240, 240, 240);
    private final Color textColor = new Color(51, 51, 51);
    private final Color contrastTextColor = new Color(0, 0, 0);

    private JTextField usernameField = new JTextField(25);
    private JPasswordField passwordField = new JPasswordField(25);
    private DatabaseManager dbManager = new DatabaseManager();

    private JPanel mainContentPanel;

    public Login() {

        setTitle("BudgetBuddy Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel leftPanel = createLeftAestheticPanel();
        add(leftPanel, BorderLayout.WEST);

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(createLoginFormComponent(), BorderLayout.CENTER);
        add(mainContentPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    public void switchContent(JPanel newPanel) {
        mainContentPanel.removeAll();
        mainContentPanel.add(newPanel, BorderLayout.CENTER);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private JPanel createLeftAestheticPanel() {
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(primaryGreen);
        leftPanel.setPreferredSize(new Dimension(320, 550));

        GridBagConstraints gbcLeft = new GridBagConstraints();
        try {
            ImageIcon originalIcon = new ImageIcon(getClass().getResource("/images/logo.png"));
            Image image = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(image));

            gbcLeft.insets = new Insets(10, 10, 25, 10);
            gbcLeft.gridy = 0;
            leftPanel.add(imageLabel, gbcLeft);
        } catch (Exception e) {
            JLabel placeholder = new JLabel("LOGO ERROR");
            placeholder.setForeground(Color.RED);
            gbcLeft.gridy = 0;
            leftPanel.add(placeholder, gbcLeft);
        }

        JLabel projectTitle = new JLabel("BudgetBuddy");
        projectTitle.setFont(new Font("Arial", Font.BOLD, 36));
        projectTitle.setForeground(Color.WHITE);
        gbcLeft.insets = new Insets(10, 10, 10, 10);
        gbcLeft.gridy = 1;
        leftPanel.add(projectTitle, gbcLeft);

        JLabel motto = new JLabel("Your Money, Smarter. Future-Proof.");
        motto.setFont(new Font("Arial", Font.ITALIC, 18));
        motto.setForeground(contrastTextColor);
        gbcLeft.gridy = 2;
        leftPanel.add(motto, gbcLeft);

        return leftPanel;
    }

    public JPanel createLoginFormComponent() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 20, 12, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel welcomeLabel = new JLabel("Welcome Back!");
        welcomeLabel.setFont(new Font("Poppins", Font.BOLD, 30));
        welcomeLabel.setForeground(textColor);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(welcomeLabel, gbc);

        // Setup fields
        JLabel userLabel = new JLabel("USERNAME or EMAIL:");
        JLabel passLabel = new JLabel("PASSWORD:");
        JButton loginButton = new JButton("LOG IN");

        userLabel.setForeground(textColor); gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST; gbc.gridwidth = 2; loginPanel.add(userLabel, gbc);
        gbc.gridy = 3; gbc.insets = new Insets(5, 20, 15, 20); loginPanel.add(usernameField, gbc);

        passLabel.setForeground(textColor); gbc.gridy = 4; gbc.insets = new Insets(12, 20, 12, 20); loginPanel.add(passLabel, gbc);
        gbc.gridy = 5; gbc.insets = new Insets(5, 20, 25, 20); loginPanel.add(passwordField, gbc);

        loginButton.setFont(new Font("Arial", Font.BOLD, 15));
        loginButton.setBackground(primaryGreen);
        loginButton.setForeground(Color.WHITE);
        gbc.gridy = 6; gbc.insets = new Insets(10, 20, 10, 20); loginPanel.add(loginButton, gbc);
        JLabel forgotLabel = new JLabel("<html><u>Forgot Password?</u></html>");
        forgotLabel.setForeground(secondaryGreen);
        forgotLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                switchContent(new ForgotPasswordPanel(Login.this));
            }
        });
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 20, 5, 20);
        loginPanel.add(forgotLabel, gbc);

        JLabel signupLabel = new JLabel("<html>Don't have an account? <u>Sign Up</u></html>");
        signupLabel.setForeground(primaryGreen);
        signupLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signupLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                switchContent(new SignUpPanel(Login.this));
            }
        });
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 20, 10, 20);
        loginPanel.add(signupLabel, gbc);

        loginButton.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            int userId = dbManager.authenticateUser(user, pass); // Get the ID

            if (userId != -1) {

                UserSession.setCurrentUserId(userId);
                UserSession.setCurrentUsername(user);

                JOptionPane.showMessageDialog(this, "Login Successful! Welcome, " + user + ".", "Success", JOptionPane.INFORMATION_MESSAGE);

                new Dashboard().setVisible(true);
                this.dispose();

            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        return loginPanel;
    }

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> new Login());
    }
}