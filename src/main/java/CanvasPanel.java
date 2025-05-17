import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CanvasPanel extends JLayeredPane {
    private static final int GRID_SIZE = 20;
    private JLabel coordLabel;

    public CanvasPanel() {
        setLayout(null);
        setBackground(Color.WHITE);
        setOpaque(true);

        coordLabel = new JLabel("X: 0 Y: 0");
        coordLabel.setOpaque(true);
        coordLabel.setBackground(new Color(255, 255, 255, 200));
        coordLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        coordLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        coordLabel.setSize(100, 20);
        add(coordLabel, JLayeredPane.DRAG_LAYER);

        // Update mouse coords globally over this pane and children
        MouseMotionAdapter mma = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateCoordinates(e);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                updateCoordinates(e);
            }
        };

        addMouseMotionListener(mma);

        // Also add to child components dynamically
        addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (e.getChild() instanceof Component) {
                    e.getChild().addMouseMotionListener(mma);
                }
            }
        });
    }

    private void updateCoordinates(MouseEvent e) {
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), this);
        coordLabel.setText("X: " + p.x + " Y: " + p.y);
        // Position label bottom right with 10px padding
        int x = getWidth() - coordLabel.getWidth() - 10;
        int y = getHeight() - coordLabel.getHeight() - 10;
        coordLabel.setLocation(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(220, 220, 220));
        for (int x = 0; x < getWidth(); x += GRID_SIZE) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += GRID_SIZE) {
            g.drawLine(0, y, getWidth(), y);
        }
    }
}
