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
     * Текстовое поле для ввода запроса на естественном языке
     */
    private JTextArea naturalLanguageArea;
    /**
     * Текстовое поле для отображения SQL-запроса
     */
    private JTextArea sqlArea;
    /**
     * Маппинг выбранных таблиц (имя таблицы -> выбрана ли)
     */
    private Map<String, Boolean> tableSelected = new HashMap<>();
    /**
     * Флаг, указывающий, была ли нажата кнопка генерации SQL
     */
    private boolean showSQLClicked = false;
    /**
     * Метка для отображения статуса операции
     */
    private JLabel statusLabel = new JLabel("Готово", SwingConstants.CENTER);

    /**
     * Конструктор окна создания запросов
     *
     * @param host         хост базы данных
     * @param username     имя пользователя
     * @param password     пароль пользователя
     * @param databaseName название базы данных
     */
    public CreateQuery(String host, String username, String password, String databaseName) {
        setTitle("Книжный магазин");
        try {
            ImageIcon icon = new ImageIcon("Client/src/main/resources/IconBookStore.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 120, 215));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 80));

        try {
            ImageIcon icon = new ImageIcon("Client/src/main/resources/IconBookStore.png");
            Image scaledImage = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
            headerPanel.add(iconLabel, BorderLayout.WEST);
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }
        JLabel titleLabel = new JLabel("Книжный магазин");
        titleLabel.setFont(new Font("Roboto", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

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

        this.naturalLanguageArea = new JTextArea(10, 50);
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

        JPanel tablesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        tablesPanel.setBackground(Color.WHITE);
        String[] tables = {"Authors", "Books", "Cheque", "Circulation", "Contains", "PublishingHouse", "Wrote"};
        for (String table : tables) {
            JButton btn = new JButton(table);
            btn.setPreferredSize(new Dimension(140, 40));
            btn.setBackground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            btn.setOpaque(true);
            btn.setFocusPainted(false);
            this.tableSelected.put(table, false);
            btn.addActionListener(e -> {
                boolean isSelected = !this.tableSelected.get(table);
                this.tableSelected.put(table, isSelected);
                btn.setBackground(isSelected ? new Color(200, 200, 200) : Color.WHITE);
            });
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!CreateQuery.this.tableSelected.get(table)) btn.setBackground(new Color(230, 230, 230));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!CreateQuery.this.tableSelected.get(table)) btn.setBackground(Color.WHITE);
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
            tablesPanel.add(btn);
        }
        formGbc.gridy = 3;
        formGbc.gridwidth = 2;
        formGbc.weighty = 0.3;
        formGbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(tablesPanel, formGbc);

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

        showSQLButton.addActionListener(e -> {
            this.showSQLClicked = true;
            String question = this.naturalLanguageArea.getText().trim();
            if (question.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "Введите запрос на естественном языке.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            showSQLButton.setEnabled(false);
            this.statusLabel.setText("Генерация запроса...");
            SwingWorker<SqlResponse, Void> worker = new SwingWorker<>() {
                @Override
                protected SqlResponse doInBackground() throws Exception {
                    HttpClient client = HttpClient.newHttpClient();
                    Gson gson = new Gson();
                    QueryRequest reqBody = new QueryRequest(question, CreateQuery.this.tableSelected);
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(java.net.URI.create("http://localhost:8080/api/generate-sql?username=" + username))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(reqBody)))
                            .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return gson.fromJson(resp.body(), SqlResponse.class);
                }

                @Override
                protected void done() {
                    try {
                        SqlResponse response = get();
                        if (response.getError() == null) {
                            CreateQuery.this.sqlArea.setText(response.getSql());
                            CreateQuery.this.statusLabel.setText("Готово");
                        } else {
                            JOptionPane.showMessageDialog(null,
                                    "Ошибка: " + response.getError(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                            CreateQuery.this.statusLabel.setText("Ошибка при генерации");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null,
                                "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                        CreateQuery.this.statusLabel.setText("Ошибка при генерации");
                    } finally {
                        showSQLButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        JLabel label3 = new JLabel("Запрос на языке SQL:");
        label3.setFont(new Font("Roboto", Font.BOLD, 16));
        formGbc.gridy = 5;
        formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label3, formGbc);

        this.sqlArea = new JTextArea(12, 50);
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

        sendSQLButton.addActionListener(e -> {
            String sql = this.sqlArea.getText();
            if (sql.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "SQL-запрос пуст.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sendSQLButton.setEnabled(false);
            CreateQuery.this.statusLabel.setText("Выполнение запроса...");
            SwingWorker<ResultsResponse, Void> worker = new SwingWorker<>() {
                @Override
                protected ResultsResponse doInBackground() throws Exception {
                    HttpClient client = HttpClient.newHttpClient();
                    Gson gson = new Gson();
                    Map<String, String> body = new HashMap<>();
                    body.put("sql", sql);
                    body.put("username", username);
                    body.put("password", password);
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(java.net.URI.create("http://localhost:8080/api/execute-sql"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                            .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return gson.fromJson(resp.body(), ResultsResponse.class);
                }

                @Override
                protected void done() {
                    try {
                        ResultsResponse response = get();
                        if (response.getError() == null) {
                            new ShowResults("localhost", username, password,
                                    "bookstore", response.getResults()).setVisible(true);
                            if (CreateQuery.this.showSQLClicked) {
                                for (Component comp : tablesPanel.getComponents()) {
                                    if (comp instanceof JButton) {
                                        comp.setBackground(Color.WHITE);
                                        CreateQuery.this.tableSelected.put(((JButton) comp).getText(), false);
                                    }
                                }
                                CreateQuery.this.showSQLClicked = false;
                            }
                            CreateQuery.this.statusLabel.setText("Готово");
                        } else {
                            JOptionPane.showMessageDialog(null,
                                    "Ошибка: " + response.getError(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                            CreateQuery.this.statusLabel.setText("Ошибка при выполнении");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null,
                                "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                        CreateQuery.this.statusLabel.setText("Ошибка при выполнении");
                    } finally {
                        sendSQLButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        formGbc.gridy = 8;
        formGbc.gridwidth = 2;
        formGbc.weighty = 0.0;
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        CreateQuery.this.statusLabel.setFont(new Font("Roboto", Font.PLAIN, 14));
        contentPanel.add(CreateQuery.this.statusLabel, formGbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        cardPanel.add(contentPanel, gbc);

        add(cardPanel, BorderLayout.CENTER);
    }
}