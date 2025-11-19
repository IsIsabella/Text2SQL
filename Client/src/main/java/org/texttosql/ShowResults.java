package org.texttosql;

import net.jcip.annotations.NotThreadSafe;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Класс для отображения результатов выполнения SQL-запроса в таблице
 */
@NotThreadSafe
public class ShowResults extends JFrame {
    /**
     * Таблица для отображения результатов
     */
    private JTable resultsTable;

    /**
     * Конструктор окна результатов
     *
     * @param username имя пользователя
     * @param results  результаты выполнения SQL-запроса в текстовом формате
     */
    public ShowResults(String username, String role, String results) {
        setTitle("Книжный магазин");
        try {
            ImageIcon icon = new ImageIcon("Client/src/main/resources/IconBookStore.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 120, 215));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 70));

        // Иконка слева
        try {
            ImageIcon icon = new ImageIcon("Client/src/main/resources/IconBookStore.png");
            Image scaledImage = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));
            headerPanel.add(iconLabel, BorderLayout.WEST);
        } catch (Exception ignored) {
        }

        // Название приложения
        JLabel titleLabel = new JLabel("Книжный магазин", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Roboto", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 20));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Пользователь и роль указываются справа
        String displayName = username != null && !username.isBlank() ? username.trim() : "Гость";
        String displayRole = switch (role) {
            case "admin" -> "Администратор";
            case "seller" -> "Продавец";
            default -> "Покупатель";
        };

        JLabel userInfo = new JLabel(displayName + " (" + displayRole + ")", SwingConstants.RIGHT);
        userInfo.setFont(new Font("Roboto", Font.PLAIN, 18));
        userInfo.setForeground(Color.WHITE);
        userInfo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));
        headerPanel.add(userInfo, BorderLayout.EAST);

        this.add(headerPanel, BorderLayout.NORTH);

        // Таблица результатов
        this.resultsTable = new JTable();
        this.resultsTable.setFont(new Font("Roboto", Font.PLAIN, 14));
        this.resultsTable.setRowHeight(28);
        this.resultsTable.setGridColor(new Color(220, 220, 220));
        this.resultsTable.setShowGrid(true);
        this.resultsTable.setFillsViewportHeight(true);
        this.resultsTable.setSelectionBackground(new Color(0, 120, 215));
        this.resultsTable.setSelectionForeground(Color.WHITE);
        this.resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Чередование цветов строк
        this.resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
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
                this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                this.setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        // Заголовок таблицы
        JTableHeader header = this.resultsTable.getTableHeader();
        header.setBackground(new Color(0, 120, 215));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Roboto", Font.BOLD, 15));
        header.setPreferredSize(new Dimension(header.getWidth(), 35));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);

        JScrollPane scroll = new JScrollPane(this.resultsTable);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        this.add(scroll, BorderLayout.CENTER);

        // Загрузка данных в фоне
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                ShowResults.this.loadResults(results);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShowResults.this,
                            "Ошибка при отображении результатов: " + ex.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        // Автоподгонка колонок при изменении размера окна
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ShowResults.this.adjustColumnWidthsToFitWindow();
            }
        });
    }

    /**
     * Загружает результаты SQL-запроса в таблицу
     *
     * @param result результаты выполнения SQL-запроса в текстовом формате
     */
    private void loadResults(String result) {
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
            this.resultsTable.setModel(model);
            this.resultsTable.setEnabled(false);
            this.adjustColumnWidthsToFitWindow();
        });
    }

    /**
     * Автоматически распределяет ширину колонок таблицы по ширине окна
     */
    private void adjustColumnWidthsToFitWindow() {
        if (resultsTable.getColumnCount() == 0) return;

        // Учитываем ширину JScrollPane, а не всего окна
        int availableWidth = resultsTable.getParent().getWidth(); // это viewport
        if (availableWidth <= 0) availableWidth = getWidth() - 100;

        int columnCount = resultsTable.getColumnCount();
        int totalGaps = 1; // отступы между колонками
        int baseWidth = (availableWidth - totalGaps) / columnCount;

        // Минимум 100 пикселей на колонку, чтобы не было слишком узко
        int minWidth = 120;
        if (baseWidth < minWidth) {
            baseWidth = minWidth;
        }

        for (int i = 0; i < columnCount; i++) {
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(baseWidth);
        }

        resultsTable.revalidate();
        resultsTable.repaint();
    }
}