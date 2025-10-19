package org.texttosql;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class CreateQuery extends JFrame {
    private JTextArea naturalLanguageArea;
    private JTextArea sqlArea;
    private Map<String, Boolean> tableSelected = new HashMap<>();
    private boolean showSQLClicked = false;
    private JLabel statusLabel = new JLabel("Готово", SwingConstants.CENTER);

    public CreateQuery(String host, String username, String password, String databaseName) {
        // Настройки окна
        setTitle("Книжный магазин");
        try {
            ImageIcon icon = new ImageIcon("IconBookStore.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Заголовок
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 120, 215));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 80));

        try {
            ImageIcon icon = new ImageIcon("IconBookStore.png");
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

        // Главная панель с содержимым
        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(new Color(240, 240, 240));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Панель формы
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(10, 0, 10, 0);
        formGbc.anchor = GridBagConstraints.WEST;
        formGbc.fill = GridBagConstraints.HORIZONTAL;

        // Заголовок для ввода запроса
        JLabel label1 = new JLabel("Введите запрос на естественном языке:");
        label1.setFont(new Font("Roboto", Font.BOLD, 16));
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.gridwidth = 2;
        contentPanel.add(label1, formGbc);

        // Текстовое поле для естественного языка
        naturalLanguageArea = new JTextArea(10, 50);
        naturalLanguageArea.setFont(new Font("Roboto", Font.PLAIN, 14));
        naturalLanguageArea.setLineWrap(true);
        naturalLanguageArea.setWrapStyleWord(true);
        naturalLanguageArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        JScrollPane scroll1 = new JScrollPane(naturalLanguageArea);
        formGbc.gridy = 1;
        formGbc.gridwidth = 2;
        formGbc.weightx = 1.0;
        formGbc.weighty = 0.3;
        formGbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(scroll1, formGbc);

        // Заголовок для выбора таблиц
        JLabel label2 = new JLabel("Выберите таблицы, которые требуются для запроса:");
        label2.setFont(new Font("Roboto", Font.BOLD, 16));
        formGbc.gridy = 2;
        formGbc.weighty = 0.0;
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(label2, formGbc);

        // Панель для кнопок таблиц
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
            tableSelected.put(table, false);
            btn.addActionListener(e -> {
                boolean isSelected = !tableSelected.get(table);
                tableSelected.put(table, isSelected);
                btn.setBackground(isSelected ? new Color(200, 200, 200) : Color.WHITE);
            });
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!tableSelected.get(table)) btn.setBackground(new Color(230, 230, 230));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!tableSelected.get(table)) btn.setBackground(Color.WHITE);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    btn.setBackground(new Color(180, 180, 180));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    btn.setBackground(tableSelected.get(table) ? new Color(200, 200, 200) : Color.WHITE);
                }
            });
            tablesPanel.add(btn);
        }
        formGbc.gridy = 3;
        formGbc.gridwidth = 2;
        formGbc.weighty = 0.3;
        formGbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(tablesPanel, formGbc);

        // Кнопка генерации SQL
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
            showSQLClicked = true;
            String question = naturalLanguageArea.getText().trim();
            if (question.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Введите запрос на естественном языке.",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            showSQLButton.setEnabled(false);
            statusLabel.setText("Генерация запроса...");

            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() throws Exception {
                    StringBuilder sb = new StringBuilder(question);
                    sb.append("\nИспользуй таблицы: ");
                    for (Map.Entry<String, Boolean> entry : tableSelected.entrySet()) {
                        if (entry.getValue()) {
                            sb.append(entry.getKey()).append(" ");
                        }
                    }
                    ConnectToDeepSeek connect = new ConnectToDeepSeek(sb.toString());
                    return connect.connection();
                }

                @Override
                protected void done() {
                    try {
                        String sql = get();
                        sqlArea.setText(sql);
                        statusLabel.setText("Готово");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Ошибка: " + ex.getMessage(),
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Ошибка при генерации");
                    } finally {
                        showSQLButton.setEnabled(true);
                    }
                }
            };

            worker.execute();
        });

        // Заголовок для SQL
        JLabel label3 = new JLabel("Запрос на языке SQL:");
        label3.setFont(new Font("Roboto", Font.BOLD, 16));
        formGbc.gridy = 5;
        formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label3, formGbc);

        // Текстовое поле для SQL
        sqlArea = new JTextArea(12, 50);
        sqlArea.setFont(new Font("Roboto", Font.PLAIN, 14));
        sqlArea.setLineWrap(true);
        sqlArea.setWrapStyleWord(true);
        sqlArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        JScrollPane scroll2 = new JScrollPane(sqlArea);
        formGbc.gridy = 6;
        formGbc.gridwidth = 2;
        formGbc.weightx = 1.0;
        formGbc.weighty = 0.7;
        formGbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(scroll2, formGbc);

        // Кнопка отправки SQL
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
            String sql = sqlArea.getText();
            if (sql.isEmpty()) {
                JOptionPane.showMessageDialog(null, "SQL-запрос пуст.",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            sendSQLButton.setEnabled(false);
            statusLabel.setText("Выполнение запроса...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    new ShowResults(host, username, password, databaseName, sql).setVisible(true);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        if (showSQLClicked) {
                            for (Component comp : tablesPanel.getComponents()) {
                                if (comp instanceof JButton) {
                                    comp.setBackground(Color.WHITE);
                                    tableSelected.put(((JButton) comp).getText(), false);
                                }
                            }
                            showSQLClicked = false;
                        }
                        naturalLanguageArea.setText("");
                        statusLabel.setText("Готово");
                    } finally {
                        sendSQLButton.setEnabled(true);
                    }
                }
            };

            worker.execute();
        });

        // Метка статуса
        formGbc.gridy = 8;
        formGbc.gridwidth = 2;
        formGbc.weighty = 0.0;
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel.setFont(new Font("Roboto", Font.PLAIN, 14));
        contentPanel.add(statusLabel, formGbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        cardPanel.add(contentPanel, gbc);

        add(cardPanel, BorderLayout.CENTER);
    }
}