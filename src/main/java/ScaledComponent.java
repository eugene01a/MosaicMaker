import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ScaledComponent extends JComponent {

    ImageComponent ic;
    static final int HANDLE_SIZE = 10;
    private boolean resizing = false;
    private Corner resizingCorner;
    private Point resizingStart;
    private Point dragOffset;
    private Rectangle resizeHandle;
    private boolean cropMode = false;
    private final boolean moveMode = false;
    private Rectangle cropRect = null;
    private Point cropStart = null;
    private boolean horizontalSplitMode = false;
    private int horizontalSplitY = -1;
    private boolean draggingSplitLine = false;
    private boolean verticalSplitMode = false;
    private int verticalSplitX = -1;
    private boolean draggingVerticalSplitLine = false;
    private JPopupMenu cropPopup;
    private boolean selected = false;
    private double resizedScale;
    private Dimension resizeStartSize;
    private Point startLocation;

    public void setResizingStart(Point p){ this.resizingStart = p; }
    public Point getResizingStart(){ return this.resizingStart; };
    public void setStartLocation(Point p) {
        this.startLocation = p;
    }

    public BufferedImage resizedImage(){
        BufferedImage image = ic.getImage();
        if (resizedScale == 1.0) {
            return image;
        } else {
            int newWidth = (int) (image.getWidth() * resizedScale);
            int newHeight = (int) (image.getHeight() * resizedScale);
            BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            return scaledImage;
        }
    }

    public Point getStartLocation() {
        return startLocation;
    }

    public ScaledComponent(BufferedImage image) {
        ic = new ImageComponent(image);
        setBounds(ic.getBounds());
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        ScaledComponentMouseAdapter mouseAdapter = new ScaledComponentMouseAdapter(this);
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        resizedScale = 1.0;
    }
    public void setResizedScale(double scale){
        this.resizedScale = scale;
    }
    public void setResizeStartSizeToCurrent(){
        this.resizeStartSize = new Dimension(getWidth(),getHeight());
    }
    public Dimension getResizeStartSize(){
        return this.resizeStartSize;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }
    public Corner getResizingCorner() {
        return resizingCorner;
    }
    public void setResizing(boolean resizing) { this.resizing = resizing; }
    public void setResizingCorner(Corner corner) { this.resizingCorner = corner; }
    public boolean isResizing() { return resizing; }

    public void setDragOffset(Point dragOffset) { this.dragOffset = dragOffset; }
    public Point getDragOffset() { return dragOffset; }

    public void setCropStart(Point cropStart) { this.cropStart = cropStart; }
    public Point getCropStart() { return cropStart; }

    public void setCropRect(Rectangle cropRect) { this.cropRect = cropRect; }
    public Rectangle getCropRect() { return cropRect; }

    public boolean isCropMode() { return cropMode; }
    public boolean isMoveMode() { return moveMode; }
    public boolean isHorizontalSplitMode() { return horizontalSplitMode; }
    public void setHorizontalSplitY(int y) { this.horizontalSplitY = y; }
    public int getHorizontalSplitY() { return horizontalSplitY; }
    public void setDraggingSplitLine(boolean dragging) { this.draggingSplitLine = dragging; }
    public boolean isDraggingSplitLine() { return draggingSplitLine; }

    public boolean isVerticalSplitMode() { return verticalSplitMode; }
    public void setVerticalSplitX(int x) { this.verticalSplitX = x; }
    public int getVerticalSplitX() { return verticalSplitX; }
    public void setDraggingVerticalSplitLine(boolean dragging) { this.draggingVerticalSplitLine = dragging; }
    public boolean isDraggingVerticalSplitLine() { return draggingVerticalSplitLine; }

    public JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem splitHorizontally = new JMenuItem("Horizontal Split");
        splitHorizontally.addActionListener(ae -> enterHorizontalSplitMode());
        splitHorizontally.setEnabled(selected);
        menu.add(splitHorizontally);

        JMenuItem splitVertically = new JMenuItem("Vertical Split");
        splitVertically.addActionListener(ae -> enterVerticalSplitMode());
        splitVertically.setEnabled(selected);
        menu.add(splitVertically);

        if (!cropMode) {
            JMenuItem cropItem = new JMenuItem("Crop Image");
            cropItem.addActionListener(ae -> enterCropMode());
            cropItem.setEnabled(selected);
            menu.add(cropItem);
        }

        JMenuItem deleteItem = new JMenuItem("Delete Image");
        deleteItem.addActionListener(ae -> {
            Container parent = getParent();
            if (parent != null) {
                parent.remove(this);
                if (parent instanceof ScaledCanvas) {
                    ((ScaledCanvas) parent).selectComponent(null);
                }
                parent.repaint();
            }
        });
        deleteItem.setEnabled(selected);
        menu.add(deleteItem);

        JMenuItem bringToFront = new JMenuItem("Bring to Front");
        bringToFront.addActionListener(ae -> {
            if (getParent() instanceof ScaledCanvas canvas) canvas.bringToFront(this);
        });
        bringToFront.setEnabled(selected);
        menu.add(bringToFront);

        JMenuItem sendToBack = new JMenuItem("Send to Back");
        sendToBack.addActionListener(ae -> {
            if (getParent() instanceof ScaledCanvas canvas) canvas.sendToBack(this);
        });
        sendToBack.setEnabled(selected);
        menu.add(sendToBack);

        JMenuItem moveForward = new JMenuItem("Move Forward");
        moveForward.addActionListener(ae -> {
            if (getParent() instanceof ScaledCanvas canvas) canvas.moveForward(this);
        });
        moveForward.setEnabled(selected);
        menu.add(moveForward);

        JMenuItem moveBackward = new JMenuItem("Move Backward");
        moveBackward.addActionListener(ae -> {
            if (getParent() instanceof ScaledCanvas canvas) canvas.moveBackward(this);
        });
        moveBackward.setEnabled(selected);
        menu.add(moveBackward);

        return menu;
    }

    public void enterVerticalSplitMode() {
        verticalSplitMode = true;
        draggingVerticalSplitLine = true; // immediately start dragging
        verticalSplitX = getWidth() / 2;
        draggingVerticalSplitLine = false;
        repaint();
    }

    void confirmVerticalSplit() {
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
        BufferedImage image = ic.getImage();
        if (image == null || getParent() == null) return;

        double scaleX = (double) image.getWidth() / getWidth();
        int imgSplitX = (int) (splitX * scaleX);

        // Guard against invalid splits
        if (imgSplitX <= 0 || imgSplitX >= image.getWidth()) return;

        //Create separate components
        JLayeredPane canvas = (JLayeredPane) getParent();

        BufferedImage left = image.getSubimage(0, 0, imgSplitX, image.getHeight());
        ScaledComponent leftComponent = new ScaledComponent(left);
        leftComponent.setBounds(getX(), getY(), splitX, getHeight());
        canvas.add(leftComponent, JLayeredPane.DEFAULT_LAYER);
        leftComponent.ic.setLocation(this.ic.getLocation());

        BufferedImage right = image.getSubimage(imgSplitX, 0, image.getWidth() - imgSplitX, image.getHeight());
        ScaledComponent rightComponent = new ScaledComponent(right);
        rightComponent.setBounds(getX() + splitX, getY(), getWidth() - splitX, getHeight());
        canvas.add(rightComponent, JLayeredPane.DEFAULT_LAYER);
        rightComponent.ic.setLocation(
                new Point(this.ic.getLocation().x + imgSplitX, 0));

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

    void confirmHorizontalSplit() {
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
        BufferedImage image = ic.getImage();
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

        ScaledComponent topComponent = new ScaledComponent(top);
        topComponent.setBounds(getX(), getY(), getWidth(), topHeight);
        canvas.add(topComponent, JLayeredPane.DEFAULT_LAYER);

        ScaledComponent bottomComponent = new ScaledComponent(bottom);
        bottomComponent.setBounds(getX(), getY() + splitY, getWidth(), bottomHeight);
        bottomComponent.ic.setLocation(
                new Point(0, this.ic.getLocation().y + imgSplitY));
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

    void showCropPopup() {

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
        BufferedImage image = ic.getImage();
        if (cropRect == null || image == null) return;
        int cropRectX = cropRect.x;
        int cropRectY = cropRect.y;
        int cropRectW = cropRect.width;
        int cropRectH = cropRect.height;
        if (cropRect.x < 0){
            cropRectW += cropRect.x;
            cropRectX = 0;
        }
        if (cropRect.y < 0){
            cropRectH += cropRect.y;
            cropRectY = 0;
        }

        double scaleX = (double) getWidth() / image.getWidth();
        double scaleY = (double) getHeight() / image.getHeight();
        int x = (int) (cropRectX * (image.getWidth() / (double) getWidth()));
        int y = (int) (cropRectY * (image.getHeight() / (double) getHeight()));
        int w = (int) (cropRectW * (image.getWidth() / (double) getWidth()));
        int h = (int) (cropRectH * (image.getHeight() / (double) getHeight()));

        // Clamp to image bounds
        x = Math.max(0, Math.min(x, image.getWidth() - 1));
        y = Math.max(0, Math.min(y, image.getHeight() - 1));
        w = Math.max(1, Math.min(w, image.getWidth() - x));
        h = Math.max(1, Math.min(h, image.getHeight() - y));

        ic.crop(x, y, w, h);

        int newW = (int) (w * scaleX);
        int newH = (int) (h * scaleY);
        int newX = (int) (x * scaleX);
        int newY = (int) (y * scaleY);
        setSize(newW, newH);
        Point origScaledLocation = getLocation();
        setBounds(origScaledLocation.x + newX, origScaledLocation.y + newY, newW, newH);
        repaint();
    }

    Corner getCornerUnderPoint(Point p) {
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
        BufferedImage image = ic.getImage();
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

    public void scaleAndSetBounds(double scale){
        int newX = (int) (this.getLocation().x * scale);
        int newY = (int) (this.getLocation().y * scale);
        int newWidth = (int) (this.getWidth() * scale);
        int newHeight = (int) (this.getHeight() * scale);
        this.setBounds(newX, newY, newWidth, newHeight);
    }
}
