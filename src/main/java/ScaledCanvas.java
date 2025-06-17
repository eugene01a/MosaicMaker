import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ScaledCanvas extends JLayeredPane {
    private double scale = 1.0;
    private ScaledComponent selectedComponent = null;

    @Override
    public void remove(Component comp) {
        if (comp == selectedComponent) {
            selectedComponent = null; // Clear selection if removed
            updateEditMenu(); // Update menu state
        }
        super.remove(comp);
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public double getScale() {
        return scale;
    }

    public void addImageToCanvas(File fileToOpen) {
        try {
            BufferedImage img = ImageIO.read(fileToOpen);
            if (img != null) {
                ScaledComponent sc = new ScaledComponent(img);
                sc.scaleAndSetBounds(scale);
                add(sc);
                this.setComponentZOrder(sc, 0);
                selectComponent(sc);
                repaint();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error opening image.");
        }
    }

    public void selectComponent(ScaledComponent image) {
        if (selectedComponent != null && selectedComponent != image) {
            selectedComponent.setSelected(false);
        }
        selectedComponent = image;
        if (image != null) {
            image.setSelected(true);
        }
        updateEditMenu();
        repaint();
    }

    public ScaledComponent getSelectedImage() {
        return selectedComponent;
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
                                item.setEnabled(selectedComponent != null);
                            }
                        }
                    }
                }
            }
        }
    }

    public void updateChildrenBounds() {
        for (Component comp : getComponents()) {
            if (comp instanceof ScaledComponent) {
                ScaledComponent ic = (ScaledComponent) comp;
                Rectangle origBounds = ic.getImageBounds();
                int newX = (int) (origBounds.x * scale);
                int newY = (int) (origBounds.y * scale);
                int newWidth = (int) (origBounds.getWidth() * scale);
                int newHeight = (int) (origBounds.getHeight() * scale);
                comp.setBounds(newX, newY, newWidth, newHeight);
            }
        }
        updatePreferredSize();
        revalidate();
        repaint();
    }

    private void updatePreferredSize() {
        Rectangle bounds = getScaledImagesBounds();
        if (bounds.width == 0 || bounds.height == 0) {
            setPreferredSize(new Dimension(AppDefaults.FRAME_WIDTH, AppDefaults.FRAME_HEIGHT));
        } else {
            setPreferredSize(new Dimension(bounds.x + bounds.width, bounds.y + bounds.height));
        }
    }

    public Rectangle getUnscaledImagesBounds() {
        Rectangle bounds = null;
        for (Component comp : getComponents()) {
            if (comp instanceof ScaledComponent) {
                ScaledComponent ic = (ScaledComponent) comp;
                Rectangle ic_bounds = ic.getImageBounds();
                if (bounds == null) {
                    bounds = ic_bounds;
                } else {
                    bounds = bounds.union(ic_bounds);
                }
            }
        }
        return bounds == null ? new Rectangle(0, 0, 0, 0) : bounds;
    }

    public Rectangle getScaledImagesBounds() {
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

    public void bringToFront(ScaledComponent imageComponent) {
        this.setComponentZOrder(imageComponent, 0);
        repaint();
    }

    public void sendToBack(ScaledComponent imageComponent) {
        this.setComponentZOrder(imageComponent, getComponentCount() - 1);
        repaint();
    }

    public void moveForward(ScaledComponent imageComponent) {
        int currentIndex = getComponentZOrder(imageComponent);
        if (currentIndex > 0) {
            setComponentZOrder(imageComponent, currentIndex - 1);
            repaint();
        }
    }

    public void moveBackward(ScaledComponent imageComponent) {
        int currentIndex = getComponentZOrder(imageComponent);
        if (currentIndex < getComponentCount() - 1) {
            setComponentZOrder(imageComponent, currentIndex + 1);
            repaint();
        }
    }

    public void shiftUnscaledContentBounds(Point unscaledLocation) {
        for (Component comp : getComponents()) {
            if (comp instanceof ScaledComponent) {
                ScaledComponent ic = (ScaledComponent) comp;
                Rectangle origBounds = ic.getImageBounds();
                int newX = origBounds.x + unscaledLocation.x;
                int newY = origBounds.y + unscaledLocation.y;
                ic.setImageLocation(new Point(newX, newY));
            }
        }
        updatePreferredSize();
        revalidate();
        repaint();
    }

    public BufferedImage createUnscaledMosaicImage() {
        Rectangle unscaledBounds = getUnscaledImagesBounds();
        BufferedImage mosaic = new BufferedImage(unscaledBounds.width, unscaledBounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = mosaic.createGraphics();
        for (Component comp : getComponents()) {
            if (comp instanceof ScaledComponent) {
                ScaledComponent ic = (ScaledComponent) comp;
                Point location = ic.getImageLocation();
                g2d.drawImage(ic.resizedImage(), location.x, location.y, null);
            }
        }
        g2d.dispose();
        return mosaic;
    }
}