import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ScaledCanvas extends JLayeredPane {
    private double scale = 1.0;
    private Map<Component, Rectangle> originalBounds = new HashMap<>();
    private ImageComponent selectedImage = null; // New field to track selected image

    @Override
    public void remove(Component comp) {
        this.originalBounds.remove(comp);
        if (comp == selectedImage) {
            selectedImage = null; // Clear selection if removed
            updateEditMenu(); // Update menu state
        }
        super.remove(comp);
    }

    public void setScale(double scale) {
        this.scale *= scale;
    }

    public double getScale() {
        return scale;
    }

    public void updateOriginalBounds(ImageComponent ic, Rectangle bounds) {
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
                this.setComponentZOrder(ic, 0);
                selectImage(ic); // Select new image by default
                repaint();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error opening image.");
        }
    }

    public void selectImage(ImageComponent image) {
        if (selectedImage != null && selectedImage != image) {
            selectedImage.setSelected(false); // Deselect previous image
        }
        selectedImage = image;
        if (image != null) {
            image.setSelected(true);
        }
        updateEditMenu(); // Update menu state
        repaint();
    }

    public ImageComponent getSelectedImage() {
        return selectedImage;
    }

    // New method to update Edit menu state
    public void updateEditMenu() {
        Container parent = getParent();
        while (parent != null && !(parent instanceof JFrame)) {
            parent = parent.getParent();
        }
        if (parent instanceof JFrame) {
            JFrame frame = (JFrame) parent;
            JMenuBar menuBar = frame.getJMenuBar();
            if (menuBar != null) {
                for (int i = 0; i < menuBar.getMenuCount(); i++) {
                    JMenu menu = menuBar.getMenu(i);
                    if (menu != null && menu.getText().equals("Edit")) {
                        for (int j = 0; j < menu.getItemCount(); j++) {
                            JMenuItem item = menu.getItem(j);
                            if (item != null) {
                                item.setEnabled(selectedImage != null);
                            }
                        }
                    }
                }
            }
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

    public void bringToFront(ImageComponent imageComponent) {
        this.setComponentZOrder(imageComponent, 0);
        repaint();
    }

    public void sendToBack(ImageComponent imageComponent) {
        this.setComponentZOrder(imageComponent, getComponentCount() - 1);
        repaint();
    }

    public void moveForward(ImageComponent imageComponent) {
        int currentIndex = getComponentZOrder(imageComponent);
        if (currentIndex > 0) {
            setComponentZOrder(imageComponent, currentIndex - 1);
            repaint();
        }
    }

    public void moveBackward(ImageComponent imageComponent) {
        int currentIndex = getComponentZOrder(imageComponent);
        if (currentIndex < getComponentCount() - 1) {
            setComponentZOrder(imageComponent, currentIndex + 1);
            repaint();
        }
    }
}