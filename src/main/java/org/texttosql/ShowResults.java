package org.texttosql;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class ShowResults extends JFrame {
    private JTable resultsTable;

    public ShowResults(String host, String username, String password, String databaseName, String sqlQuery) {
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

        // Верхняя панель
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 120, 215));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 70));

        JLabel titleLabel = new JLabel("Книжный магазин", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Roboto", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        try {
            ImageIcon icon = new ImageIcon("IconBookStore.png");
            Image scaledImage = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));
            headerPanel.add(iconLabel, BorderLayout.WEST);
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // Таблица
        resultsTable = new JTable();
        resultsTable.setFont(new Font("Roboto", Font.PLAIN, 14));
        resultsTable.setRowHeight(28);
        resultsTable.setGridColor(new Color(220, 220, 220));
        resultsTable.setShowGrid(true);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setSelectionBackground(new Color(0, 120, 215));
        resultsTable.setSelectionForeground(Color.WHITE);
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Чередование цвета строк
        resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color evenColor = new Color(245, 248, 250);
            private final Color oddColor = Color.WHITE;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground((row % 2 == 0) ? evenColor : oddColor);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        // Заголовки таблицы
        JTableHeader header = resultsTable.getTableHeader();
        header.setBackground(new Color(0, 120, 215));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Roboto", Font.BOLD, 15));
        header.setPreferredSize(new Dimension(header.getWidth(), 35));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);

        // Скролл
        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        // Загрузка данных
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                loadResults(host, username, password, databaseName, sqlQuery);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Ошибка: " + ex.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        // Автоматическая подгонка при изменении размера окна
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                adjustColumnWidthsToFitWindow();
            }
        });
    }

    /**
     * Загружает результаты SQL в таблицу
     */
    private void loadResults(String host, String username, String password,
                             String databaseName, String sqlQuery) {
        try {
            ParseQuery parseQuery = new ParseQuery(sqlQuery, host, username, password,
                    databaseName, "G:\\Учеба ЯрГУ\\ДИПЛОМ\\Text-to-SQL\\src\\main\\java\\resources\\roles.json");
            String parsedSql = parseQuery.parseSql();

            ConnectWithDb showResult = new ConnectWithDb(host, username, password, databaseName);
            String result = showResult.results(parsedSql);

            if (result == null || result.isBlank()) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Результат пуст",
                                "Информация", JOptionPane.INFORMATION_MESSAGE));
                return;
            }

            String[] rows = result.split("\n");
            if (rows.length == 0) return;

            String[] headers = rows[0].trim().split("\\s{2,}|\t");
            DefaultTableModel model = new DefaultTableModel(headers, 0);

            for (int i = 1; i < rows.length; i++) {
                String[] cols = rows[i].trim().split("\\s{2,}|\t");
                model.addRow(cols);
            }

            SwingUtilities.invokeLater(() -> {
                resultsTable.setModel(model);
                resultsTable.setEnabled(false);
                adjustColumnWidthsToFitWindow();
            });

        } catch (Exception ex) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE));
        }
    }

    /**
     * Автоматически распределяет ширину колонок на всю ширину окна
     */
    private void adjustColumnWidthsToFitWindow() {
        if (resultsTable.getColumnCount() == 0) return;

        int tableWidth = getWidth() - 60; // небольшой отступ
        int columnCount = resultsTable.getColumnCount();
        if (columnCount == 0) return;

        int baseWidth = tableWidth / columnCount;

        for (int column = 0; column < columnCount; column++) {
            resultsTable.getColumnModel().getColumn(column).setPreferredWidth(baseWidth);
        }

        resultsTable.revalidate();
        resultsTable.repaint();
    }
}
