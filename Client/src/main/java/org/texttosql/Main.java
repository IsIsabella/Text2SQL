package org.texttosql;

import com.google.gson.Gson;
import net.jcip.annotations.NotThreadSafe;
import org.texttosql.common.LoginRequest;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Главный класс клиентского приложения, реализующий интерфейс входа в систему
 */
@NotThreadSafe
public class Main extends JFrame {
    /**
     * Поле для ввода логина
     */
    private JTextField loginTextField;
    /**
     * Поле для ввода пароля
     */
    private JPasswordField passwordField;
    /**
     * Кнопка для входа в систему
     */
    private CustomButton logInButton;
    /**
     * Флажок для запоминания логина
     */
    private JCheckBox rememberMeCheckBox;
    /**
     * Имя файла для сохранения логина
     */
    private static final String SETTINGS_FILE = "settings.txt";

    /**
     * Конструктор окна входа в систему
     */
    public Main() {
        setTitle("Книжный магазин");
        try {
            ImageIcon icon = new ImageIcon("Client/src/main/resources/IconBookStore.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }

        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

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

        this.logInButton.addActionListener(e -> {
            String username = this.loginTextField.getText();
            String password = new String(this.passwordField.getPassword());
            SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
                @Override
                protected Map<String, Object> doInBackground() throws Exception {
                    HttpClient client = HttpClient.newHttpClient();
                    Gson gson = new Gson();
                    LoginRequest reqBody = new LoginRequest(username, password);
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(java.net.URI.create("http://localhost:8080/api/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(reqBody)))
                            .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return gson.fromJson(resp.body(), Map.class);
                }

                @Override
                protected void done() {
                    try {
                        Map<String, Object> response = get();
                        if (Boolean.TRUE.equals(response.get("success"))) {
                            new CreateQuery("localhost", (String) response.get("username"), password, "bookstore").setVisible(true);
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(null, response.get("message"), "Ошибка", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });

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

        this.loadSavedLogin();
    }

    /**
     * Загружает сохраненный логин из файла настроек
     */
    private void loadSavedLogin() {
        File file = new File(Main.SETTINGS_FILE);
        if (file.exists() && file.length() > 0) {
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
     * Сохраняет логин в файл настроек
     *
     * @param login логин для сохранения
     */
    private void saveLogin(String login) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(Main.SETTINGS_FILE), StandardCharsets.UTF_8))) {
            writer.write(login);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении логина: " + e.getMessage());
        }
    }

    /**
     * Очищает сохраненный логин, удаляя файл настроек
     */
    private void clearLogin() {
        File file = new File(Main.SETTINGS_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Точка входа для запуска клиентского приложения
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main main = new Main();
            main.setVisible(true);
            main.setLocationRelativeTo(null);
        });
    }
}