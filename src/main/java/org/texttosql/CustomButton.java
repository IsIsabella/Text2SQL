package org.texttosql;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CustomButton extends JButton {
    private Color defaultColor = new Color(0, 120, 215);
    private Color hoverColor = new Color(0, 100, 185);
    private Color pressedColor = new Color(0, 80, 150);
    private boolean isHovered = false;
    private boolean isPressed = false;

    public CustomButton(String text) {
        super(text);
        setFont(new Font("Roboto", Font.BOLD, 14));
        setForeground(Color.WHITE);
        setContentAreaFilled(true);
        setFocusPainted(false);
        setBorderPainted(false);
        setBackground(defaultColor);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isPressed) {
            g2.setColor(pressedColor);
        } else if (isHovered) {
            g2.setColor(hoverColor);
        } else {
            g2.setColor(defaultColor);
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); // Rounded corners
        super.paintComponent(g);
        g2.dispose();
    }
}