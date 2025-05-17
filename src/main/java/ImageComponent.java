import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;

public class ImageComponent extends JComponent {
    private BufferedImage image;

    private static final int HANDLE_SIZE = 10;
    private static final int GRID_SIZE = 20;
    private static final int SNAP_DISTANCE = 10;

    private Point dragStart = null;
    private Point dragOffsetScreen = null;
    private Point origComponentScreen = null;

    private boolean resizing = false;
    private Corner activeCorner = Corner.NONE;

    private int origX, origY, origW, origH;

    private boolean cropMode = false;
    private Rectangle cropRect = null;
    private Point cropStart = null;

    private JPopupMenu cropPopup;

    enum Corner {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public ImageComponent(BufferedImage img) {
        this.image = img;
        setSize(img.getWidth(), img.getHeight());

        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (cropMode) {
                    cropStart = e.getPoint();
                    cropRect = new Rectangle(cropStart);
                    repaint();
                    return;
                }

                activeCorner = getCornerUnderPoint(e.getPoint());
                resizing = activeCorner != Corner.NONE;

                if (resizing) {
                    origX = getX();
                    origY = getY();
                    origW = getWidth();
                    origH = getHeight();
                } else {
                    dragStart = e.getPoint();
                    dragOffsetScreen = e.getLocationOnScreen();
                    origComponentScreen = getLocationOnScreen();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (cropMode) {
                    if (cropStart != null) {
                        int x = Math.min(cropStart.x, e.getX());
                        int y = Math.min(cropStart.y, e.getY());
                        int w = Math.abs(e.getX() - cropStart.x);
                        int h = Math.abs(e.getY() - cropStart.y);
                        cropRect.setBounds(x, y, w, h);
                        repaint();
                    }
                    return;
                }

                if (resizing) {
                    resizeWithAspect(e);
                } else {
                    Point currentMouse = e.getLocationOnScreen();
                    int dx = currentMouse.x - dragOffsetScreen.x;
                    int dy = currentMouse.y - dragOffsetScreen.y;

                    int newX = origComponentScreen.x + dx - getParent().getLocationOnScreen().x;
                    int newY = origComponentScreen.y + dy - getParent().getLocationOnScreen().y;

                    newX = Math.round((float)newX / GRID_SIZE) * GRID_SIZE;
                    newY = Math.round((float)newY / GRID_SIZE) * GRID_SIZE;

                    Point snapped = getSnappedPosition(newX, newY);
                    setLocation(snapped.x, snapped.y);
                }
                forwardMouseEventToParent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (cropMode) {
                    int x = Math.min(cropStart.x, e.getX());
                    int y = Math.min(cropStart.y, e.getY());
                    int w = Math.abs(e.getX() - cropStart.x);
                    int h = Math.abs(e.getY() - cropStart.y);
                    cropRect.setBounds(x, y, w, h);
                    showCropPopup();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (cropMode) return;
                Corner c = getCornerUnderPoint(e.getPoint());
                switch (c) {
                    case TOP_LEFT, BOTTOM_RIGHT -> setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    case TOP_RIGHT, BOTTOM_LEFT -> setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    default -> setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (cropMode) return; // disable menu while cropping
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem cropItem = new JMenuItem("Crop Image");
                    cropItem.addActionListener(ae -> enterCropMode());
                    menu.add(cropItem);
                    menu.show(ImageComponent.this, e.getX(), e.getY());
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void enterCropMode() {
        cropMode = true;
        cropRect = null;
        cropStart = null;
        repaint();
    }

    private void showCropPopup() {

        if (cropPopup == null) {
            cropPopup = new JPopupMenu();

            JMenuItem applyCrop = new JMenuItem("Apply Crop");
            applyCrop.addActionListener(e -> {
                if (cropRect != null && cropRect.width > 0 && cropRect.height > 0) {
                    performCrop();
                }
                exitCropMode();
            });
            cropPopup.add(applyCrop);

            JMenuItem cancelCrop = new JMenuItem("Cancel");
            cancelCrop.addActionListener(e -> {
                exitCropMode();
            });
            cropPopup.add(cancelCrop);
        }
        // Show popup near bottom-right corner of component
        cropPopup.show(this, getWidth() - 100, getHeight() - 50);
    }

    private void exitCropMode() {
        cropMode = false;
        cropRect = null;
        cropStart = null;
        if (cropPopup != null) {
            cropPopup.setVisible(false);
        }
        repaint();
    }

    private void performCrop() {
        if (cropRect == null || image == null) return;

        double scaleX = (double) getWidth() / image.getWidth();
        double scaleY = (double) getHeight() / image.getHeight();

// Crop
        int x = (int) (cropRect.x * (image.getWidth() / (double) getWidth()));
        int y = (int) (cropRect.y * (image.getHeight() / (double) getHeight()));
        int w = (int) (cropRect.width * (image.getWidth() / (double) getWidth()));
        int h = (int) (cropRect.height * (image.getHeight() / (double) getHeight()));

// Clamp to image bounds
        x = Math.max(0, Math.min(x, image.getWidth() - 1));
        y = Math.max(0, Math.min(y, image.getHeight() - 1));
        w = Math.max(1, Math.min(w, image.getWidth() - x));
        h = Math.max(1, Math.min(h, image.getHeight() - y));

// Get cropped image
        BufferedImage cropped = image.getSubimage(x, y, w, h);
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(cropped, 0, 0, null);
        g2.dispose();

// Replace image, but preserve scale
        image = copy;
        int newW = (int) (w * scaleX);
        int newH = (int) (h * scaleY);
        setSize(newW, newH);
        setBounds(getX(), getY(), newW, newH);
        repaint();

    }

    private void forwardMouseEventToParent(MouseEvent e) {
        Container parent = getParent();
        if (parent != null) {
            MouseEvent converted = SwingUtilities.convertMouseEvent(this, e, parent);
            parent.dispatchEvent(converted);
        }
    }

    private Corner getCornerUnderPoint(Point p) {
        Rectangle tl = new Rectangle(0, 0, HANDLE_SIZE, HANDLE_SIZE);
        Rectangle tr = new Rectangle(getWidth() - HANDLE_SIZE, 0, HANDLE_SIZE, HANDLE_SIZE);
        Rectangle bl = new Rectangle(0, getHeight() - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
        Rectangle br = new Rectangle(getWidth() - HANDLE_SIZE, getHeight() - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);

        if (tl.contains(p)) return Corner.TOP_LEFT;
        if (tr.contains(p)) return Corner.TOP_RIGHT;
        if (bl.contains(p)) return Corner.BOTTOM_LEFT;
        if (br.contains(p)) return Corner.BOTTOM_RIGHT;
        return Corner.NONE;
    }

    private void resizeWithAspect(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        int newX = getX();
        int newY = getY();
        int newW = getWidth();
        int newH = getHeight();

        double aspect = (double) origW / origH;

        switch (activeCorner) {
            case TOP_LEFT -> {
                int dx = mx;
                int dy = my;
                if (Math.abs(dx) > Math.abs(dy * aspect)) {
                    dy = (int) (dx / aspect);
                } else {
                    dx = (int) (dy * aspect);
                }
                newX = origX + dx;
                newY = origY + dy;
                newW = origW - dx;
                newH = origH - dy;
            }
            case TOP_RIGHT -> {
                int dx = mx - origW;
                int dy = my;
                if (Math.abs(dx) > Math.abs(dy * aspect)) {
                    dy = (int) (-dx / aspect);
                } else {
                    dx = (int) (-dy * aspect);
                }
                newY = origY + dy;
                newW = origW + dx;
                newH = origH - dy;
            }
            case BOTTOM_LEFT -> {
                int dx = mx;
                int dy = my - origH;
                if (Math.abs(dx) > Math.abs(dy * aspect)) {
                    dy = (int) (dx / aspect);
                } else {
                    dx = (int) (dy * aspect);
                }
                newX = origX + dx;
                newW = origW - dx;
                newH = origH + dy;
            }
            case BOTTOM_RIGHT -> {
                int dx = mx - origW;
                int dy = my - origH;
                if (Math.abs(dx) > Math.abs(dy * aspect)) {
                    dy = (int) (dx / aspect);
                } else {
                    dx = (int) (dy * aspect);
                }
                newW = origW + dx;
                newH = origH + dy;
            }
            default -> {}
        }

        newW = Math.max(newW, 20);
        newH = Math.max(newH, 20);

        setBounds(newX, newY, newW, newH);
        repaint();
    }

    private Point getSnappedPosition(int x, int y) {
        Rectangle movingBounds = new Rectangle(x, y, getWidth(), getHeight());
        int snapX = x;
        int snapY = y;

        for (Component comp : getParent().getComponents()) {
            if (comp == this || !(comp instanceof ImageComponent)) continue;
            Rectangle other = comp.getBounds();

            if (Math.abs(movingBounds.x - other.x) <= SNAP_DISTANCE) {
                snapX = other.x;
            } else if (Math.abs(movingBounds.x + movingBounds.width - (other.x + other.width)) <= SNAP_DISTANCE) {
                snapX = other.x + other.width - movingBounds.width;
            } else if (Math.abs(movingBounds.x + movingBounds.width - other.x) <= SNAP_DISTANCE) {
                snapX = other.x - movingBounds.width;
            } else if (Math.abs(movingBounds.x - (other.x + other.width)) <= SNAP_DISTANCE) {
                snapX = other.x + other.width;
            }

            if (Math.abs(movingBounds.y - other.y) <= SNAP_DISTANCE) {
                snapY = other.y;
            } else if (Math.abs(movingBounds.y + movingBounds.height - (other.y + other.height)) <= SNAP_DISTANCE) {
                snapY = other.y + other.height - movingBounds.height;
            } else if (Math.abs(movingBounds.y + movingBounds.height - other.y) <= SNAP_DISTANCE) {
                snapY = other.y - movingBounds.height;
            } else if (Math.abs(movingBounds.y - (other.y + other.height)) <= SNAP_DISTANCE) {
                snapY = other.y + other.height;
            }
        }

        return new Point(snapX, snapY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);

        if (!cropMode) {
            int w = getWidth();
            int h = getHeight();
            g2.fillRect(0, 0, HANDLE_SIZE, HANDLE_SIZE);
            g2.fillRect(w - HANDLE_SIZE, 0, HANDLE_SIZE, HANDLE_SIZE);
            g2.fillRect(0, h - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
            g2.fillRect(w - HANDLE_SIZE, h - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
        }

        if (cropMode && cropRect != null) {
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 150));
            g2.fillRect(cropRect.x, cropRect.y, cropRect.width, cropRect.height);

            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(cropRect.x, cropRect.y, cropRect.width, cropRect.height);
        }
    }
}
