package org.texttosql;

import com.google.gson.Gson;
import net.jcip.annotations.NotThreadSafe;
import org.texttosql.common.QueryRequest;
import org.texttosql.common.ResultsResponse;
import org.texttosql.common.SqlResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс для создания и отправки запросов на естественном языке и отображения SQL
 */
@NotThreadSafe
public class CreateQuery extends JFrame {
    /**
     * Поле ввода запроса на естественном языке
     */
    private final JTextArea naturalLanguageArea = new JTextArea(10, 50);
    /**
     * Поле для отображения сгенерированного SQL-запроса
     */
    private final JTextArea sqlArea = new JTextArea(12, 50);
    /**
     * Хранит состояние выбора таблиц: выбрана ли таблица пользователем
     */
    private final Map<String, Boolean> tableSelected = new HashMap<>();
    /**
     * Панель с чекбоксами для выбора таблиц
     */
    private final JPanel tablesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
    /**
     * Флаг: была ли нажата кнопка "Сгенерировать запрос"
     */
    private boolean showSQLClicked = false;
    /**
     * Строка состояния в нижней части окна
     */
    private final JLabel statusLabel = new JLabel("Готово", SwingConstants.CENTER);
    /**
     * Базовый URL REST API сервера
     */
    private static final String SERVER_URL = "http://localhost:8080/api";
    /**
     * Экземпляр Gson для работы с JSON
     */
    private final Gson gson = new Gson();
    /**
     * Авторизованный пользователь
     */
    private final String username;
    /**
     * Роль пользователя
     */
    private final String role;

    public CreateQuery(String username, String role) {
        this.username = username != null && !username.isBlank() ? username.trim() : "Гость";
        this.role = role != null && !role.isBlank() ? role : "buyer";

        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Книжный магазин");
        try {
            ImageIcon icon = new ImageIcon("Client/src/main/resources/IconBookStore.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // При нажатии на системный крестик — возвращаемся на экран входа
                Main.showMainWindow();
                CreateQuery.this.dispose(); // закрываем текущее окно
            }
        });

        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 120, 215));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 80));

        // Иконка слева
        try {
            ImageIcon icon = new ImageIcon("Client/src/main/resources/IconBookStore.png");
            Image scaledImage = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 15));
            headerPanel.add(iconLabel, BorderLayout.WEST);
        } catch (Exception ignored) {
        }

        // Название приложения по центру
        JLabel titleLabel = new JLabel("Книжный магазин", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Roboto", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel userInfoLabel = this.getJLabel();
        headerPanel.add(userInfoLabel, BorderLayout.EAST);

        this.add(headerPanel, BorderLayout.NORTH);

        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(new Color(240, 240, 240));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(10, 0, 10, 0);
        formGbc.anchor = GridBagConstraints.WEST;
        formGbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel label1 = new JLabel("Введите запрос на естественном языке:");
        label1.setFont(new Font("Roboto", Font.BOLD, 16));
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.gridwidth = 2;
        contentPanel.add(label1, formGbc);

        this.naturalLanguageArea.setFont(new Font("Roboto", Font.PLAIN, 14));
        this.naturalLanguageArea.setLineWrap(true);
        this.naturalLanguageArea.setWrapStyleWord(true);
        this.naturalLanguageArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        JScrollPane scroll1 = new JScrollPane(this.naturalLanguageArea);
        formGbc.gridy = 1;
        formGbc.gridwidth = 2;
        formGbc.weightx = 1.0;
        formGbc.weighty = 0.3;
        formGbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(scroll1, formGbc);

        JLabel label2 = new JLabel("Выберите таблицы, которые требуются для запроса:");
        label2.setFont(new Font("Roboto", Font.BOLD, 16));
        formGbc.gridy = 2;
        formGbc.weighty = 0.0;
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(label2, formGbc);

        this.tablesPanel.setBackground(Color.WHITE);
        String[] tables = {"Authors", "Books", "Cheque", "Circulation", "Contains", "PublishingHouse", "Wrote"};
        for (String table : tables) {
            JButton btn = new JButton(table);
            btn.setPreferredSize(new Dimension(140, 40));
            btn.setBackground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            btn.setOpaque(true);
            btn.setFocusPainted(false);
            btn.setFont(new Font("Roboto", Font.BOLD, 14));
            this.tableSelected.put(table, false);
            btn.addActionListener(e -> {
                boolean isSelected = !this.tableSelected.get(table);
                this.tableSelected.put(table, isSelected);
                btn.setBackground(isSelected ? new Color(200, 200, 200) : Color.WHITE);
            });
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!CreateQuery.this.tableSelected.get(table)) {
                        btn.setBackground(new Color(230, 230, 230));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!CreateQuery.this.tableSelected.get(table)) {
                        btn.setBackground(Color.WHITE);
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    btn.setBackground(new Color(180, 180, 180));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    btn.setBackground(CreateQuery.this.tableSelected.get(table) ?
                            new Color(200, 200, 200) : Color.WHITE);
                }
            });
            this.tablesPanel.add(btn);
        }
        formGbc.gridy = 3;
        formGbc.gridwidth = 2;
        formGbc.weighty = 0.3;
        formGbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(this.tablesPanel, formGbc);

        CustomButton showSQLButton = new CustomButton("Сгенерировать запрос");
        showSQLButton.setBackground(new Color(0, 120, 215));
        showSQLButton.setForeground(Color.WHITE);
        showSQLButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        formGbc.gridy = 4;
        formGbc.gridwidth = 2;
        formGbc.weighty = 0.0;
        formGbc.anchor = GridBagConstraints.CENTER;
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(showSQLButton, formGbc);

        showSQLButton.addActionListener(e -> this.generateSql(showSQLButton));

        // Поле SQL
        JLabel label3 = new JLabel("Запрос на языке SQL:");
        label3.setFont(new Font("Roboto", Font.BOLD, 16));
        formGbc.gridy = 5;
        formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label3, formGbc);

        this.sqlArea.setFont(new Font("Roboto", Font.PLAIN, 14));
        this.sqlArea.setLineWrap(true);
        this.sqlArea.setWrapStyleWord(true);
        this.sqlArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        JScrollPane scroll2 = new JScrollPane(this.sqlArea);
        formGbc.gridy = 6;
        formGbc.gridwidth = 2;
        formGbc.weightx = 1.0;
        formGbc.weighty = 0.7;
        formGbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(scroll2, formGbc);

        CustomButton sendSQLButton = new CustomButton("Отправить запрос в базу");
        sendSQLButton.setBackground(new Color(0, 120, 215));
        sendSQLButton.setForeground(Color.WHITE);
        sendSQLButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        formGbc.gridy = 7;
        formGbc.gridwidth = 2;
        formGbc.weighty = 0.0;
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        formGbc.anchor = GridBagConstraints.CENTER;
        contentPanel.add(sendSQLButton, formGbc);

        sendSQLButton.addActionListener(e -> this.executeSql(sendSQLButton));

        // Статус
        formGbc.gridy = 8;
        formGbc.weighty = 0.0;
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        this.statusLabel.setFont(new Font("Roboto", Font.PLAIN, 14));
        contentPanel.add(this.statusLabel, formGbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        cardPanel.add(contentPanel, gbc);
        this.add(cardPanel, BorderLayout.CENTER);
    }

    /**
     * Определяет подпись для пользователя
     */
    private JLabel getJLabel() {
        String displayName = this.username != null && !this.username.isBlank() ? this.username : "Гость";
        String displayRole = switch (this.role) {
            case "admin" -> "Администратор";
            case "seller" -> "Продавец";
            default -> "Покупатель";
        };

        JLabel userInfoLabel = new JLabel(displayName + " (" + displayRole + ")", SwingConstants.RIGHT);
        userInfoLabel.setFont(new Font("Roboto", Font.PLAIN, 18));
        userInfoLabel.setForeground(Color.WHITE);
        userInfoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));
        return userInfoLabel;
    }

    /**
     * Запускает генерацию запроса
     *
     * @param button кнопка
     * @throws Exception если запрос не введен
     */
    private void generateSql(CustomButton button) {
        this.showSQLClicked = true;
        String question = this.naturalLanguageArea.getText().trim();
        if (question.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Введите запрос на естественном языке.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        button.setEnabled(false);
        this.statusLabel.setText("Генерация запроса...");

        new SwingWorker<SqlResponse, Void>() {
            @Override
            protected SqlResponse doInBackground() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                QueryRequest reqBody = new QueryRequest(question, CreateQuery.this.tableSelected);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(CreateQuery.SERVER_URL + "/generate-sql"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(CreateQuery.this.gson.toJson(reqBody)))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                return CreateQuery.this.gson.fromJson(resp.body(), SqlResponse.class);
            }

            @Override
            protected void done() {
                try {
                    SqlResponse response = get();
                    if (response.getError() == null && response.getSql() != null) {
                        CreateQuery.this.sqlArea.setText(response.getSql());
                        CreateQuery.this.statusLabel.setText("Готово");
                    } else {
                        JOptionPane.showMessageDialog(CreateQuery.this,
                                "Ошибка: " + (response.getError() != null ?
                                        response.getError() : "Неизвестная ошибка"),
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                        CreateQuery.this.statusLabel.setText("Ошибка при генерации");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CreateQuery.this,
                            "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    CreateQuery.this.statusLabel.setText("Ошибка при генерации");
                } finally {
                    button.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Выполнение запроса
     *
     * @param button кнопка
     * @throws Exception если запрос не введен
     */
    private void executeSql(CustomButton button) {
        String sql = this.sqlArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "SQL-запрос пуст.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        button.setEnabled(false);
        this.statusLabel.setText("Выполнение запроса...");

        new SwingWorker<ResultsResponse, Void>() {
            @Override
            protected ResultsResponse doInBackground() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                Map<String, Object> body = new HashMap<>();
                body.put("sql", sql);
                body.put("isGuest", role.equals("buyer") && username.equals("Гость"));

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(CreateQuery.SERVER_URL + "/execute-sql"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(CreateQuery.this.gson.toJson(body)))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                return CreateQuery.this.gson.fromJson(resp.body(), ResultsResponse.class);
            }

            @Override
            protected void done() {
                try {
                    ResultsResponse response = get();
                    if (response.getError() == null) {
                        new ShowResults(CreateQuery.this.username, CreateQuery.this.role,
                                response.getResults()).setVisible(true);

                        if (CreateQuery.this.showSQLClicked) {
                            for (Component comp : CreateQuery.this.tablesPanel.getComponents()) {
                                if (comp instanceof JButton btn) {
                                    btn.setBackground(Color.WHITE);
                                    CreateQuery.this.tableSelected.put(btn.getText(), false);
                                }
                            }
                            CreateQuery.this.showSQLClicked = false;
                        }
                        CreateQuery.this.statusLabel.setText("Готово");
                    } else {
                        JOptionPane.showMessageDialog(CreateQuery.this,
                                "Ошибка: " + response.getError(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                        CreateQuery.this.statusLabel.setText("Ошибка при выполнении");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CreateQuery.this,
                            "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    CreateQuery.this.statusLabel.setText("Ошибка при выполнении");
                } finally {
                    button.setEnabled(true);
                }
            }
        }.execute();
    }
}

