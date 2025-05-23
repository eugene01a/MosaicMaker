import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ScaledCanvas extends JLayeredPane {
    private double scale = 1.0;
    private Map<Component, Rectangle> originalBounds = new HashMap<>();

    public void setScale(double scale) {
            this.scale *= scale;
    }

    public double getScale() {
        return scale;
    }
    public void updateOriginalBounds(Component ic, Rectangle bounds) {
        originalBounds.put(ic, bounds);
    }
    public void addImageToCanvas(File fileToOpen) {
        try {
            BufferedImage img = ImageIO.read(fileToOpen);
            if (img != null) {
                ImageComponent ic = new ImageComponent(img);
                int width = img.getWidth();
                int height = img.getHeight();
                ic.setBounds(AppDefaults.GRID_SIZE, AppDefaults.GRID_SIZE, width, height);
                originalBounds.put(ic, ic.getBounds());
                Rectangle orig = ic.getBounds();
                int newX = (int) (orig.x * scale);
                int newY = (int) (orig.y * scale);
                int newWidth = (int) (orig.width * scale);
                int newHeight = (int) (orig.height * scale);
                ic.setBounds(newX, newY, newWidth, newHeight);
                add(ic);
                repaint();

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error opening image.");
        }
    }
    public void updateChildrenBounds() {
        for (Component comp : getComponents()) {
            Rectangle orig = this.originalBounds.get(comp);
            int newX = (int) (comp.getX() * scale);
            int newY = (int) (comp.getY() * scale);
            int newWidth = (int) (orig.width * scale);
            int newHeight = (int) (orig.height * scale);
            comp.setBounds(newX, newY, newWidth, newHeight);
        }
        updatePreferredSize();
        revalidate();
        repaint();
    }

    private void updatePreferredSize() {
        Rectangle bounds = getImagesBounds();
        if (bounds.width == 0 || bounds.height == 0) {
            setPreferredSize(new Dimension(AppDefaults.FRAME_WIDTH, AppDefaults.FRAME_HEIGHT));
        } else {
            int padding = 100;
            bounds.grow(padding, padding);
            setPreferredSize(new Dimension(bounds.x + bounds.width, bounds.y + bounds.height));
        }
    }

    public Rectangle getImagesBounds() {
        Rectangle bounds = null;
        for (Component comp : getComponents()) {
            if (bounds == null) {
                bounds = comp.getBounds();
            } else {
                bounds = bounds.union(comp.getBounds());
            }
        }
        return bounds == null ? new Rectangle(0, 0, 0, 0) : bounds;
    }
}
