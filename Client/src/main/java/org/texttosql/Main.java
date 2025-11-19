package org.texttosql;

import net.jcip.annotations.NotThreadSafe;

import javax.swing.*;
import java.awt.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Стартовое окно приложения — выбор режима входа
 */
@NotThreadSafe
public class Main extends JFrame {

    private static Main instance;

    public Main() {
        Main.instance = this;
        setTitle("Книжный магазин");
        try {
            setIconImage(new ImageIcon("Client/src/main/resources/IconBookStore.png").getImage());
        } catch (Exception ignored) {
        }

        this.setSize(520, 380);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 20, 15, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Заголовок
        JLabel title = new JLabel("Книжный магазин", SwingConstants.CENTER);
        title.setFont(new Font("Roboto", Font.BOLD, 34));
        title.setForeground(new Color(0, 120, 215));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        this.add(title, gbc);

        // Подзаголовок
        JLabel subtitle = new JLabel("Выберите способ входа в систему", SwingConstants.CENTER);
        subtitle.setFont(new Font("Roboto", Font.PLAIN, 16));
        subtitle.setForeground(Color.DARK_GRAY);
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 20, 50, 20);
        this.add(subtitle, gbc);

        // Кнопка входа через пользователя Windows
        CustomButton ssoButton = new CustomButton("Войти как пользователь системы");
        ssoButton.setPreferredSize(new Dimension(340, 58));
        ssoButton.setFont(new Font("Roboto", Font.BOLD, 17));
        ssoButton.addActionListener(e -> this.performSSOLogin());
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 20, 15, 20);
        this.add(ssoButton, gbc);

        // Кнопка гость
        CustomButton guestButton = new CustomButton("Войти как покупатель");
        guestButton.setPreferredSize(new Dimension(340, 58));
        guestButton.setFont(new Font("Roboto", Font.BOLD, 17));
        guestButton.addActionListener(e -> this.openQueryWindow("Гость", "buyer"));
        gbc.gridy = 3;
        this.add(guestButton, gbc);

        // Нижняя подпись
        JLabel footer = new JLabel("Книжный магазин", SwingConstants.CENTER);
        footer.setFont(new Font("Roboto", Font.PLAIN, 12));
        footer.setForeground(Color.GRAY);
        gbc.gridy = 4;
        gbc.insets = new Insets(50, 20, 20, 20);
        this.add(footer, gbc);
    }

    /**
     * Выполняет попытку автоматической SSO-авторизации (Single Sign-On) через сервер приложения.
     * Если пользователь уже вошёл в систему — авторизация происходит без ввода пароля;
     * иначе предлагается войти как покупатель
     */
    private void performSSOLogin() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/login"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    var json = new com.google.gson.Gson().fromJson(response.body(), java.util.Map.class);
                    boolean success = Boolean.TRUE.equals(json.get("success"));

                    if (success) {
                        String username = (String) json.get("username");
                        String role = (String) json.get("role");
                        String warning = (String) json.get("warning");

                        SwingUtilities.invokeLater(() -> {
                            if (warning != null && !warning.isBlank()) {
                                JOptionPane.showMessageDialog(Main.this, warning,
                                        "Информация", JOptionPane.INFORMATION_MESSAGE);
                            }
                            Main.this.openQueryWindow(username, role);
                        });
                    } else {
                        String msg = (String) json.getOrDefault("message",
                                "Неизвестная ошибка авторизации");
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(Main.this,
                                        "Авторизация не удалась:\n" + msg,
                                        "Ошибка входа", JOptionPane.ERROR_MESSAGE));
                    }
                } else {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(Main.this,
                                    "Не удалось подключиться к серверу (код " +
                                            response.statusCode() + ")\n\n" +
                                            "Попробуйте режим \"Войти как покупатель\"",
                                    "Сервер недоступен", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                Main.this.setCursor(Cursor.getDefaultCursor());
            }
        };
        worker.execute();
    }

    /**
     * Выполняет открытие следующей панели
     *
     * @param username имя пользователя
     * @param role     роль пользователя
     */
    private void openQueryWindow(String username, String role) {
        SwingUtilities.invokeLater(() -> {
            new CreateQuery(username, role).setVisible(true);
            dispose();
        });
    }

    /**
     * Точка входа
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new Main().setVisible(true);
        });
    }

    /**
     * Показывает окно авторизации. Если оно уже открыто — просто поднимает его наверх,
     * если закрыто — создаёт новое
     */
    public static void showMainWindow() {
        SwingUtilities.invokeLater(() -> {
            if (Main.instance == null || !Main.instance.isDisplayable()) {
                new Main().setVisible(true);  // создаём новое
            } else {
                Main.instance.toFront();     // выводим на передний план
                Main.instance.requestFocus();
                if (Main.instance.getExtendedState() == JFrame.ICONIFIED) {
                    Main.instance.setExtendedState(JFrame.NORMAL);  // разворачиваем, если свёрнуто
                }
            }
        });
    }
}