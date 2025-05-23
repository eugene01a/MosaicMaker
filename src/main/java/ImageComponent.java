import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ImageComponent extends JComponent {
    private final BufferedImage originalImage;
    private BufferedImage image; // if user does any modifications apply here
    private Rectangle bounds; // defines subimage of originalImage

    private static final int HANDLE_SIZE = 10;
    private boolean resizing = false;
    private Point dragOffset;
    private Rectangle resizeHandle;

    private boolean cropMode = false;
    private Rectangle cropRect = null;
    private Point cropStart = null;

    private boolean splitMode = false;
    private Rectangle splitRect = null;
    private Point splitStart = null;
    private JPopupMenu cropPopup;

    enum Corner {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public ImageComponent(BufferedImage image) {
        this.originalImage = image;
        this.image = image;
        int w=image.getWidth();
        int h=image.getHeight();
        this.bounds = new Rectangle(0,0,w,h);
        setSize(w,h);

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

                if (splitMode) {
                    splitStart = e.getPoint();
                    splitRect = new Rectangle(splitStart);
                    repaint();
                    return;
                }

                Rectangle bounds = getBounds();
                resizeHandle = new Rectangle(bounds.width - HANDLE_SIZE, bounds.height - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);

                if (resizeHandle.contains(e.getPoint())) {
                    resizing = true;
                } else {
                    resizing = false;
                    dragOffset = e.getPoint();
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
                if (splitMode && splitStart != null) {
                    int x = Math.min(splitStart.x, e.getX());
                    int y = Math.min(splitStart.y, e.getY());
                    int w = Math.abs(e.getX() - splitStart.x);
                    int h = Math.abs(e.getY() - splitStart.y);
                    splitRect.setBounds(x, y, w, h);
                    repaint();
                    return;
                }
                if (resizing) {
                    int newWidth = Math.max(20, e.getX());
                    int newHeight = Math.max(20, e.getY());

                    if (image != null && image.getHeight() != 0) {
                        // Maintain aspect ratio
                        float aspectRatio = (float) ImageComponent.this.image.getWidth() / ImageComponent.this.image.getHeight();
                        if (newWidth / (float) newHeight > aspectRatio) {
                            newWidth = (int) (newHeight * aspectRatio);
                        } else {
                            newHeight = (int) (newWidth / aspectRatio);
                        }
                    }

                    setSize(newWidth, newHeight);
                    revalidate();
                    repaint();
                } else {
                    Point parentPoint = SwingUtilities.convertPoint(ImageComponent.this, e.getPoint(), getParent());
                    int x = parentPoint.x - dragOffset.x;
                    int y = parentPoint.y - dragOffset.y;

                    // Snap to grid
                    int gridSize = AppDefaults.GRID_SIZE;
                    int snapThreshold = 10;  // pixels threshold for snapping to other images

                    int snappedX = (x + gridSize / 2) / gridSize * gridSize;
                    int snappedY = (y + gridSize / 2) / gridSize * gridSize;

                    int width = getWidth();
                    int height = getHeight();

                    // Snap to other image edges
                    if (getParent() instanceof ScaledCanvas canvas) {
                        for (Component comp : canvas.getComponents()) {
                            if (comp == ImageComponent.this) continue;  // skip self

                            Rectangle r = comp.getBounds();

                            // Check X edges snapping
                            if (Math.abs(x - r.x) < snapThreshold) snappedX = r.x;
                            if (Math.abs(x + width - r.x) < snapThreshold) snappedX = r.x - width;
                            if (Math.abs(x - (r.x + r.width)) < snapThreshold) snappedX = r.x + r.width;
                            if (Math.abs(x + width - (r.x + r.width)) < snapThreshold)
                                snappedX = r.x + r.width - width;

                            // Check Y edges snapping
                            if (Math.abs(y - r.y) < snapThreshold) snappedY = r.y;
                            if (Math.abs(y + height - r.y) < snapThreshold) snappedY = r.y - height;
                            if (Math.abs(y - (r.y + r.height)) < snapThreshold) snappedY = r.y + r.height;
                            if (Math.abs(y + height - (r.y + r.height)) < snapThreshold)
                                snappedY = r.y + r.height - height;
                        }
                    }

                    setLocation(snappedX, snappedY);
                }
                getParent().repaint();
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

                if (splitMode && splitRect != null && splitRect.width > 0 && splitRect.height > 0) {
                    promptSplitRegion();
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

                    JMenuItem deleteItem = new JMenuItem("Delete Image");
                    deleteItem.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent != null) {
                            parent.remove(ImageComponent.this);
                            parent.repaint();
                        }
                    });
                    menu.add(deleteItem);

                    JMenuItem splitItem = new JMenuItem("Split Image");
                    splitItem.addActionListener(ae -> enterSplitMode());
                    menu.add(splitItem);

                    JMenuItem bringToFront = new JMenuItem("Bring to Front");
                    bringToFront.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent instanceof ScaledCanvas) {
                            ((ScaledCanvas) parent).bringToFront(ImageComponent.this);
                        }
                    });
                    menu.add(bringToFront);

                    JMenuItem sendToBack = new JMenuItem("Send to Back");
                    sendToBack.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent instanceof ScaledCanvas) {
                            ((ScaledCanvas) parent).sendToBack(ImageComponent.this);
                        }
                    });
                    menu.add(sendToBack);

                    JMenuItem moveForward = new JMenuItem("Move Forward");
                    moveForward.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent instanceof ScaledCanvas) {
                            ((ScaledCanvas) parent).moveForward(ImageComponent.this);
                        }
                    });
                    menu.add(moveForward);

                    JMenuItem moveBackward = new JMenuItem("Move Backward");
                    moveBackward.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent instanceof ScaledCanvas) {
                            ((ScaledCanvas) parent).moveBackward(ImageComponent.this);
                        }
                    });
                    menu.add(moveBackward);
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
        ScaledCanvas parent = (ScaledCanvas) getParent();
        parent.updateOriginalBounds(this, new Rectangle(x,y,w,h));
        repaint();

    }

    private void promptSplitRegion() {
        JTextField rowsField = new JTextField("2");
        JTextField colsField = new JTextField("2");
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Rows:"));
        panel.add(rowsField);
        panel.add(new JLabel("Cols:"));
        panel.add(colsField);
        int result = JOptionPane.showConfirmDialog(this, panel, "Split Region", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int rows = Integer.parseInt(rowsField.getText());
                int cols = Integer.parseInt(colsField.getText());
                performSplit(rows, cols);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number of rows/columns.");
            }
        }
        exitSplitMode();
    }

    private void performSplit(int rows, int cols) {
        if (splitRect == null || image == null || getParent() == null) return;

        double scaleX = (double) image.getWidth() / getWidth();
        double scaleY = (double) image.getHeight() / getHeight();

        int imgX = (int) (splitRect.x * scaleX);
        int imgY = (int) (splitRect.y * scaleY);
        int imgW = (int) (splitRect.width * scaleX);
        int imgH = (int) (splitRect.height * scaleY);

        int cellW = imgW / cols;
        int cellH = imgH / rows;

        int screenX = getX() + splitRect.x;
        int screenY = getY() + splitRect.y;
        int compW = splitRect.width / cols;
        int compH = splitRect.height / rows;

        JLayeredPane canvas = (JLayeredPane) getParent();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                BufferedImage tile = image.getSubimage(imgX + c * cellW, imgY + r * cellH, cellW, cellH);
                ImageComponent piece = new ImageComponent(tile);
                piece.setBounds(screenX + c * compW, screenY + r * compH, compW, compH);
                canvas.add(piece, JLayeredPane.DEFAULT_LAYER);
            }
        }

        canvas.remove(this);
        canvas.repaint();
    }

    public void enterSplitMode() {
        splitMode = true;
        splitRect = null;
        splitStart = null;
        repaint();
    }

    private void exitSplitMode() {
        splitMode = false;
        splitRect = null;
        splitStart = null;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw image scaled to current size
        g.drawImage(image, 0, 0, getWidth(), getHeight(), this);

        // Draw resize handle
        g.setColor(Color.BLACK);
        g.fillRect(getWidth() - HANDLE_SIZE, getHeight() - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);
        if (cropMode && cropRect != null) {
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 150));
            g2.fillRect(cropRect.x, cropRect.y, cropRect.width, cropRect.height);

            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(cropRect.x, cropRect.y, cropRect.width, cropRect.height);
        }

        if (splitMode && splitRect != null) {
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 150));
            g2.fillRect(splitRect.x, splitRect.y, splitRect.width, splitRect.height);

            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(splitRect.x, splitRect.y, splitRect.width, splitRect.height);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getHeight());
    }
}
