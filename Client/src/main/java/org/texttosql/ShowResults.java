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
     * @param host         хост базы данных
     * @param username     имя пользователя
     * @param password     пароль пользователя
     * @param databaseName название базы данных
     * @param results      результаты выполнения SQL-запроса в текстовом формате
     */
    public ShowResults(String host, String username, String password, String databaseName, String results) {
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
        headerPanel.setPreferredSize(new Dimension(getWidth(), 70));

        JLabel titleLabel = new JLabel("Книжный магазин", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Roboto", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        try {
            ImageIcon icon = new ImageIcon("Client/src/main/resources/IconBookStore.png");
            Image scaledImage = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));
            headerPanel.add(iconLabel, BorderLayout.WEST);
        } catch (Exception e) {
            System.err.println("Иконка не найдена: " + e.getMessage());
        }

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        this.resultsTable = new JTable();
        this.resultsTable.setFont(new Font("Roboto", Font.PLAIN, 14));
        this.resultsTable.setRowHeight(28);
        this.resultsTable.setGridColor(new Color(220, 220, 220));
        this.resultsTable.setShowGrid(true);
        this.resultsTable.setFillsViewportHeight(true);
        this.resultsTable.setSelectionBackground(new Color(0, 120, 215));
        this.resultsTable.setSelectionForeground(Color.WHITE);
        this.resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        JTableHeader header = this.resultsTable.getTableHeader();
        header.setBackground(new Color(0, 120, 215));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Roboto", Font.BOLD, 15));
        header.setPreferredSize(new Dimension(header.getWidth(), 35));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);

        JScrollPane scroll = new JScrollPane(this.resultsTable);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

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
                    JOptionPane.showMessageDialog(null, "Ошибка: " + ex.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

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
                    JOptionPane.showMessageDialog(this, "Результат пуст", "Информация", JOptionPane.INFORMATION_MESSAGE));
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
        if (this.resultsTable.getColumnCount() == 0) return;

        int tableWidth = getWidth() - 60;
        int columnCount = this.resultsTable.getColumnCount();
        if (columnCount == 0) return;

        int baseWidth = tableWidth / columnCount;

        for (int column = 0; column < columnCount; column++) {
            this.resultsTable.getColumnModel().getColumn(column).setPreferredWidth(baseWidth);
        }

        this.resultsTable.revalidate();
        this.resultsTable.repaint();
    }
}