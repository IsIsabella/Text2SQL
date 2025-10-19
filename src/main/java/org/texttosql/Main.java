package org.texttosql;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class Main extends JFrame {
    private JTextField loginTextField;
    private JPasswordField passwordField;
    private CustomButton logInButton;
    private JCheckBox rememberMeCheckBox;
    private static final String SETTINGS_FILE = "settings.txt";

    public Main() {
        // Настройки окна
        setTitle("Книжный магазин");
        try {
            ImageIcon icon = new ImageIcon("IconBookStore.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }

        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Панель для входа
        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(new Color(0, 168, 239));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel loginForm = new JPanel(new GridBagLayout());
        loginForm.setBackground(Color.WHITE);
        loginForm.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(10, 0, 10, 0);
        formGbc.anchor = GridBagConstraints.WEST;

        JLabel loginLabel = new JLabel("Авторизация");
        loginLabel.setFont(new Font("Roboto", Font.BOLD, 20));
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.gridwidth = 2;
        loginForm.add(loginLabel, formGbc);

        JLabel label1 = new JLabel("Логин");
        label1.setFont(new Font("Roboto", Font.PLAIN, 14));
        formGbc.gridx = 0;
        formGbc.gridy = 1;
        formGbc.gridwidth = 1;
        loginForm.add(label1, formGbc);

        this.loginTextField = new JTextField(20);
        this.loginTextField.setFont(new Font("Roboto", Font.PLAIN, 14));
        this.loginTextField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        formGbc.gridx = 1;
        formGbc.gridy = 1;
        loginForm.add(this.loginTextField, formGbc);

        JLabel label2 = new JLabel("Пароль ");
        label2.setFont(new Font("Roboto", Font.PLAIN, 14));
        formGbc.gridx = 0;
        formGbc.gridy = 2;
        loginForm.add(label2, formGbc);

        this.passwordField = new JPasswordField(20);
        this.passwordField.setFont(new Font("Roboto", Font.PLAIN, 14));
        this.passwordField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        formGbc.gridx = 1;
        formGbc.gridy = 2;
        loginForm.add(this.passwordField, formGbc);

        this.rememberMeCheckBox = new JCheckBox("Запомнить логин");
        this.rememberMeCheckBox.setFont(new Font("Roboto", Font.PLAIN, 14));
        this.rememberMeCheckBox.setBackground(Color.WHITE);
        this.rememberMeCheckBox.setBorder(BorderFactory.createEmptyBorder());
        formGbc.gridx = 0;
        formGbc.gridy = 3;
        formGbc.gridwidth = 2;
        loginForm.add(this.rememberMeCheckBox, formGbc);

        this.logInButton = new CustomButton("Вход");
        this.logInButton.setBackground(new Color(0, 168, 239));
        this.logInButton.setForeground(Color.WHITE);
        this.logInButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        formGbc.gridx = 0;
        formGbc.gridy = 4;
        formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.CENTER;
        loginForm.add(this.logInButton, formGbc);

        // Слушатель кнопки входа
        this.logInButton.addActionListener(e -> {
            String username = this.loginTextField.getText();
            String password = new String(this.passwordField.getPassword());
            String host = "localhost";
            String databaseName = "bookstore";

            try {
                ConnectWithDb connect = new ConnectWithDb(host, username, password, databaseName);
                String userExists = connect.checkUser();
                if ("1".equals(userExists)) {
                    new CreateQuery(host, username, password, databaseName).setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Пользователь не найден", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Слушатель галочки "Запомнить"
        this.rememberMeCheckBox.addActionListener(e -> {
            if (this.rememberMeCheckBox.isSelected()) {
                this.saveLogin(this.loginTextField.getText());
            } else {
                this.clearLogin();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        cardPanel.add(loginForm, gbc);

        add(cardPanel, BorderLayout.CENTER);

        // Загрузка сохранённого логина после инициализации компонентов
        this.loadSavedLogin();
    }

    /**
     * Загрузка сохранённого логина при старте
     */
    private void loadSavedLogin() {
        File file = new File(this.SETTINGS_FILE);
        if (file.exists() && file.length() > 0) { // Проверка существования и непустоты файла
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8))) {
                String savedLogin = reader.readLine();
                if (savedLogin != null && !savedLogin.trim().isEmpty()) {
                    this.loginTextField.setText(savedLogin);
                    this.rememberMeCheckBox.setSelected(true);
                }
            } catch (IOException e) {
                System.err.println("Ошибка при загрузке логина: " + e.getMessage());
            }
        }
    }

    /**
     * Сохранение логина в файл
     */
    private void saveLogin(String login) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(this.SETTINGS_FILE), StandardCharsets.UTF_8))) {
            writer.write(login);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении логина: " + e.getMessage());
        }
    }

    /**
     * Очистка сохранённого логина
     */
    private void clearLogin() {
        File file = new File(this.SETTINGS_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main main = new Main();
            main.setVisible(true);
            main.setLocationRelativeTo(null);
        });
    }
}