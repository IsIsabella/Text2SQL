package org.texttosql;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Пользовательская кнопка с кастомным стилем и эффектами наведения/нажатия
 */
public class CustomButton extends JButton {
    /**
     * Цвет кнопки по умолчанию
     */
    private Color defaultColor = new Color(0, 120, 215);
    /**
     * Цвет кнопки при наведении
     */
    private Color hoverColor = new Color(0, 100, 185);
    /**
     * Цвет кнопки при нажатии
     */
    private Color pressedColor = new Color(0, 80, 150);
    /**
     * Флаг, указывающий, находится ли курсор над кнопкой
     */
    private boolean isHovered = false;
    /**
     * Флаг, указывающий, нажата ли кнопка
     */
    private boolean isPressed = false;

    /**
     * Конструктор пользовательской кнопки
     *
     * @param text текст кнопки
     */
    public CustomButton(String text) {
        super(text);
        setFont(new Font("Roboto", Font.BOLD, 14));
        setForeground(Color.WHITE);
        setContentAreaFilled(true);
        setFocusPainted(false);
        setBorderPainted(false);
        setBackground(this.defaultColor);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                CustomButton.this.isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                CustomButton.this.isHovered = false;
                CustomButton.this.isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                CustomButton.this.isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                CustomButton.this.isPressed = false;
                repaint();
            }
        });
    }

    /**
     * Отрисовывает кнопку с учетом состояния (наведение, нажатие)
     *
     * @param g графический контекст для отрисовки
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (CustomButton.this.isPressed) {
            g2.setColor(this.pressedColor);
        } else if (CustomButton.this.isHovered) {
            g2.setColor(this.hoverColor);
        } else {
            g2.setColor(this.defaultColor);
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        super.paintComponent(g);
        g2.dispose();
    }
}