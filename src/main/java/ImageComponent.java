import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ImageComponent extends JComponent {
    private BufferedImage image;
    private static final int HANDLE_SIZE = 10;
    private boolean resizing = false;
    private Point dragOffset;
    private Rectangle resizeHandle;
    private boolean cropMode = false;
    private Rectangle cropRect = null;
    private Point cropStart = null;
    private boolean horizontalSplitMode = false;
    private int horizontalSplitY = -1;
    private boolean draggingSplitLine = false;
    private boolean verticalSplitMode = false;
    private int verticalSplitX = -1;
    private boolean draggingVerticalSplitLine = false;
    private JPopupMenu cropPopup;
    private boolean selected = false; // New field for selection state

    enum Corner {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public ImageComponent(BufferedImage image) {
        this.image = image;
        int w = image.getWidth();
        int h = image.getHeight();
        setSize(w, h);

        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                // Set selection on click or right-click
                if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
                    if (getParent() instanceof ScaledCanvas) {
                        ((ScaledCanvas) getParent()).selectImage(ImageComponent.this);
                    }
                }

                if (horizontalSplitMode) {
                    Rectangle lineBounds = new Rectangle(0, horizontalSplitY - 5, getWidth(), 10);
                    if (lineBounds.contains(e.getPoint())) {
                        draggingSplitLine = true;
                    }
                    return;
                }
                if (verticalSplitMode) {
                    Rectangle lineBounds = new Rectangle(verticalSplitX - 5, 0, 10, getHeight());
                    if (lineBounds.contains(e.getPoint())) {
                        draggingVerticalSplitLine = true;
                    }
                    return;
                }
                if (cropMode) {
                    cropStart = e.getPoint();
                    cropRect = new Rectangle(cropStart);
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
                if (horizontalSplitMode && draggingSplitLine) {
                    horizontalSplitY = Math.max(10, Math.min(getHeight() - 10, e.getY()));
                    repaint();
                    return;
                }
                if (verticalSplitMode && draggingVerticalSplitLine) {
                    verticalSplitX = Math.max(10, Math.min(getWidth() - 10, e.getX()));
                    repaint();
                    return;
                }
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
                if (horizontalSplitMode && draggingSplitLine) {
                    draggingSplitLine = false;
                    confirmHorizontalSplit();
                    return;
                }
                if (verticalSplitMode && draggingVerticalSplitLine) {
                    draggingVerticalSplitLine = false;
                    confirmVerticalSplit();
                    return;
                }
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
                if (horizontalSplitMode && draggingSplitLine) {
                    horizontalSplitY = Math.max(10, Math.min(getHeight() - 10, e.getY()));
                    repaint();
                }
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
                    if (horizontalSplitMode) return;

                    JPopupMenu menu = new JPopupMenu();

                    JMenuItem splitHorizontally = new JMenuItem("Horizontal Split");
                    splitHorizontally.addActionListener(ae -> enterHorizontalSplitMode());
                    splitHorizontally.setEnabled(selected); // Enable only if selected
                    menu.add(splitHorizontally);

                    JMenuItem splitVertically = new JMenuItem("Vertical Split");
                    splitVertically.addActionListener(ae -> enterVerticalSplitMode());
                    splitVertically.setEnabled(selected);
                    menu.add(splitVertically);

                    if (cropMode) return; // disable menu while cropping

                    JMenuItem cropItem = new JMenuItem("Crop Image");
                    cropItem.addActionListener(ae -> enterCropMode());
                    cropItem.setEnabled(selected);
                    menu.add(cropItem);

                    JMenuItem deleteItem = new JMenuItem("Delete Image");
                    deleteItem.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent != null) {
                            parent.remove(ImageComponent.this);
                            if (parent instanceof ScaledCanvas) {
                                ((ScaledCanvas) parent).selectImage(null); // Clear selection
                            }
                            parent.repaint();
                        }
                    });
                    deleteItem.setEnabled(selected);
                    menu.add(deleteItem);

                    JMenuItem bringToFront = new JMenuItem("Bring to Front");
                    bringToFront.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent instanceof ScaledCanvas) {
                            ((ScaledCanvas) parent).bringToFront(ImageComponent.this);
                        }
                    });
                    bringToFront.setEnabled(selected);
                    menu.add(bringToFront);

                    JMenuItem sendToBack = new JMenuItem("Send to Back");
                    sendToBack.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent instanceof ScaledCanvas) {
                            ((ScaledCanvas) parent).sendToBack(ImageComponent.this);
                        }
                    });
                    sendToBack.setEnabled(selected);
                    menu.add(sendToBack);

                    JMenuItem moveForward = new JMenuItem("Move Forward");
                    moveForward.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent instanceof ScaledCanvas) {
                            ((ScaledCanvas) parent).moveForward(ImageComponent.this);
                        }
                    });
                    moveForward.setEnabled(selected);
                    menu.add(moveForward);

                    JMenuItem moveBackward = new JMenuItem("Move Backward");
                    moveBackward.addActionListener(ae -> {
                        Container parent = getParent();
                        if (parent instanceof ScaledCanvas) {
                            ((ScaledCanvas) parent).moveBackward(ImageComponent.this);
                        }
                    });
                    moveBackward.setEnabled(selected);
                    menu.add(moveBackward);
                    menu.show(ImageComponent.this, e.getX(), e.getY());
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    // New method to set selection state
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    public void enterVerticalSplitMode() {
        verticalSplitMode = true;
        draggingVerticalSplitLine = true; // immediately start dragging
        verticalSplitX = getWidth() / 2;
        draggingVerticalSplitLine = false;
        repaint();
    }

    private void confirmVerticalSplit() {
        int result = JOptionPane.showConfirmDialog(this,
                "Split image at X = " + verticalSplitX + "?", "Confirm Split",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            performVerticalSplit(verticalSplitX);
        }
        exitVerticalSplitMode();
    }

    private void exitVerticalSplitMode() {
        verticalSplitMode = false;
        verticalSplitX = -1;
        draggingVerticalSplitLine = false;
        repaint();
    }

    private void performVerticalSplit(int splitX) {
        if (image == null || getParent() == null) return;

        double scaleX = (double) image.getWidth() / getWidth();
        int imgSplitX = (int) (splitX * scaleX);

        // Guard against invalid splits
        if (imgSplitX <= 0 || imgSplitX >= image.getWidth()) return;

        BufferedImage left = image.getSubimage(0, 0, imgSplitX, image.getHeight());
        BufferedImage right = image.getSubimage(imgSplitX, 0, image.getWidth() - imgSplitX, image.getHeight());

        int leftWidth = splitX;
        int rightWidth = getWidth() - splitX;

        JLayeredPane canvas = (JLayeredPane) getParent();

        ImageComponent leftComponent = new ImageComponent(left);
        leftComponent.setBounds(getX(), getY(), leftWidth, getHeight());
        canvas.add(leftComponent, JLayeredPane.DEFAULT_LAYER);

        ImageComponent rightComponent = new ImageComponent(right);
        rightComponent.setBounds(getX() + splitX, getY(), rightWidth, getHeight());
        canvas.add(rightComponent, JLayeredPane.DEFAULT_LAYER);

        canvas.remove(this);
        canvas.repaint();
    }

    public void enterHorizontalSplitMode() {
        horizontalSplitMode = true;
        draggingSplitLine = true; // <-- immediately enter drag mode
        horizontalSplitY = getHeight() / 2;
        draggingSplitLine = false;
        repaint();
    }

    private void confirmHorizontalSplit() {
        int result = JOptionPane.showConfirmDialog(this,
                "Split image at Y = " + horizontalSplitY + "?", "Confirm Split",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            performHorizontalSplit(horizontalSplitY);
        }
        exitHorizontalSplitMode();
    }

    private void exitHorizontalSplitMode() {
        horizontalSplitMode = false;
        horizontalSplitY = -1;
        draggingSplitLine = false;
        repaint();
    }

    private void performHorizontalSplit(int splitY) {
        if (image == null || getParent() == null) return;

        double scaleY = (double) image.getHeight() / getHeight();
        int imgSplitY = (int) (splitY * scaleY);

        // Guard against invalid splits
        if (imgSplitY <= 0 || imgSplitY >= image.getHeight()) return;

        BufferedImage top = image.getSubimage(0, 0, image.getWidth(), imgSplitY);
        BufferedImage bottom = image.getSubimage(0, imgSplitY, image.getWidth(), image.getHeight() - imgSplitY);

        int topHeight = splitY;
        int bottomHeight = getHeight() - splitY;

        JLayeredPane canvas = (JLayeredPane) getParent();

        ImageComponent topComponent = new ImageComponent(top);
        topComponent.setBounds(getX(), getY(), getWidth(), topHeight);
        canvas.add(topComponent, JLayeredPane.DEFAULT_LAYER);

        ImageComponent bottomComponent = new ImageComponent(bottom);
        bottomComponent.setBounds(getX(), getY() + splitY, getWidth(), bottomHeight);
        canvas.add(bottomComponent, JLayeredPane.DEFAULT_LAYER);

        canvas.remove(this);
        canvas.repaint();
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
        int newX = (int) (x * scaleX);
        int newY = (int) (y * scaleY);
        setSize(newW, newH);
        setBounds(newX, newY, newW, newH);
        ScaledCanvas parent = (ScaledCanvas) getParent();
        parent.updateOriginalBounds(this, new Rectangle(x,y,w,h));
        repaint();

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

        // Draw selection outline and handles if selected
        if (selected) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

            // Draw corner handles
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, HANDLE_SIZE, HANDLE_SIZE); // Top-left
            g2.fillRect(getWidth() - HANDLE_SIZE, 0, HANDLE_SIZE, HANDLE_SIZE); // Top-right
            g2.fillRect(0, getHeight() - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE); // Bottom-left
            g2.fillRect(getWidth() - HANDLE_SIZE, getHeight() - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE); // Bottom-right
        } else {
            // Draw default resize handle (bottom-right only when not selected)
            g.setColor(Color.BLACK);
            g.fillRect(getWidth() - HANDLE_SIZE, getHeight() - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);

        if (horizontalSplitMode && horizontalSplitY > 0) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(0, horizontalSplitY, getWidth(), horizontalSplitY);
        }
        if (verticalSplitMode && verticalSplitX > 0) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(verticalSplitX, 0, verticalSplitX, getHeight());
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

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getHeight());
    }
}