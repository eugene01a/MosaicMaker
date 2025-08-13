package mosaicmaker;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import static mosaicmaker.AppDefaults.*;

public class ScaledComponent extends JComponent {

    private BufferedImage image;
    public Point imageLocation;
    public Dimension imageDimension;
    public Rectangle getImageBounds(){
        return new Rectangle(imageLocation.x, imageLocation.y, imageDimension.width, imageDimension.height);
    }
    public void setImageBounds(int x, int y, int width, int height){
        imageLocation.x = x;
        imageLocation.y = y;
        imageDimension.width = width;
        imageDimension.height = height;

    }
    public int getImageWidth(){
        return imageDimension.width;
    };
    public int getImageHeight(){
        return imageDimension.height;
    };
    public int getImageX(){
        return imageLocation.x;
    }
    public int getImageY(){
        return imageLocation.y;
    }
    public String name;
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
        BufferedImage scaledImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g2d.dispose();
        return scaledImage;
    }

    public Point getStartLocation() {
        return startLocation;
    }

    public ScaledComponent(BufferedImage image, String name) {
        this.name = name;
        this.image = image;
        Rectangle imgBounds = new Rectangle(0,0,image.getWidth(), image.getHeight());
        this.imageLocation = new Point(0,0);
        this.imageDimension = new Dimension(image.getWidth(), image.getHeight());
        setBounds(imgBounds);
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
        if (image == null || getParent() == null) return;

        double scaleX = (double) image.getWidth() / getWidth();
        int imgSplitX = Calc.multiplyAndRound(splitX, scaleX);

        // Guard against invalid splits
        if (imgSplitX <= 0 || imgSplitX >= image.getWidth()) return;

        //Create separate components
        JLayeredPane canvas = (JLayeredPane) getParent();

        BufferedImage left = image.getSubimage(0, 0, imgSplitX, image.getHeight());
        ScaledComponent leftComponent = new ScaledComponent(left, this.name + "_left");
        leftComponent.setBounds(getX(), getY(), splitX, getHeight());
        canvas.add(leftComponent, JLayeredPane.DEFAULT_LAYER);
        leftComponent.imageLocation =this.imageLocation;

        BufferedImage right = image.getSubimage(imgSplitX, 0, image.getWidth() - imgSplitX, image.getHeight());
        ScaledComponent rightComponent = new ScaledComponent(right, this.name+"_right");
        rightComponent.setBounds(getX() + splitX, getY(), getWidth() - splitX, getHeight());
        canvas.add(rightComponent, JLayeredPane.DEFAULT_LAYER);
        rightComponent.imageLocation =
                new Point(this.getImageX() + imgSplitX, 0);

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
        if (image == null || getParent() == null) return;
        double scaleY = (double) image.getHeight() / getHeight();
        int imgSplitY = Calc.multiplyAndRound(splitY, scaleY);

        // Guard against invalid splits
        if (imgSplitY <= 0 || imgSplitY >= image.getHeight()) return;

        BufferedImage top = image.getSubimage(0, 0, image.getWidth(), imgSplitY);
        BufferedImage bottom = image.getSubimage(0, imgSplitY, image.getWidth(), image.getHeight() - imgSplitY);

        int topHeight = splitY;
        int bottomHeight = getHeight() - splitY;

        JLayeredPane canvas = (JLayeredPane) getParent();

        ScaledComponent topComponent = new ScaledComponent(top, this.name + "_top");
        topComponent.setBounds(getX(), getY(), getWidth(), topHeight);
        canvas.add(topComponent, JLayeredPane.DEFAULT_LAYER);

        ScaledComponent bottomComponent = new ScaledComponent(bottom, this.name + "_bottom");
        bottomComponent.setBounds(getX(), getY() + splitY, getWidth(), bottomHeight);
        bottomComponent.imageLocation =
                new Point(0, this.getImageY() + imgSplitY);
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

        if (cropRectW + cropRectX > getWidth()) {
            cropRectW = getWidth() - cropRectX;
        }
        if (cropRectH + cropRectY > getHeight()) {
            cropRectH = getHeight() - cropRectY;
        }

        int sc_x=cropRectX+getX();
        int sc_y=cropRectY+getY();
        double ic_scale = getSize().getWidth() / getImageWidth();

        cropImage(
                Calc.divideAndRound(sc_x, ic_scale) - getImageX(),
                Calc.divideAndRound(sc_y, ic_scale) - getImageY(),
                Calc.divideAndRound(cropRectW, ic_scale),
                Calc.divideAndRound(cropRectH, ic_scale));

        Rectangle origBounds = getImageBounds();
        int newX = Calc.multiplyAndRound(origBounds.x, ic_scale);
        int newY = Calc.multiplyAndRound(origBounds.y, ic_scale);
        int newWidth = Calc.multiplyAndRound(origBounds.getWidth(), ic_scale);
        int newHeight = Calc.multiplyAndRound(origBounds.getHeight(), ic_scale);
        setBounds(newX, newY, newWidth, newHeight);
        revalidate();
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
        int newX = Calc.multiplyAndRound(this.getLocation().x, scale);
        int newY = Calc.multiplyAndRound(this.getLocation().y, scale);
        int newWidth = Calc.multiplyAndRound(this.getWidth(), scale);
        int newHeight = Calc.multiplyAndRound(this.getHeight(), scale);
        this.setBounds(newX, newY, newWidth, newHeight);
    }

    public void setLocationFromScaledMove(Point startLoc, Point endLoc) {
        int dx = endLoc.x - startLoc.x;
        int dy = endLoc.y - startLoc.y;
        double scale = (double) getWidth() / getImageWidth();
        int imageDx = Calc.divideAndRound(dx * getImageWidth(), getWidth());
        int imageDy = Calc.divideAndRound(dy, scale);
        imageLocation = new Point(getImageX() + imageDx, getImageY() + imageDy);
        snapImageBoundsToOtherImages();
    }

    public void resizeImage(double scale){
        int resizedUnscaledWidth = Calc.multiplyAndRound(getImageWidth(), scale);
        int resizedUnscaledHeight = Calc.multiplyAndRound(getImageHeight(), scale);
        Dimension resizedUnscaledDim = new Dimension(resizedUnscaledWidth, resizedUnscaledHeight);
        imageDimension = resizedUnscaledDim;
    }

    public void cropImage(int x, int y, int w, int h) {
        double img_scale = (double) getImageWidth() / image.getWidth();

        // Convert scaled values to raw image-space coordinates
        int ix = Calc.divideAndRound(x, img_scale);
        int iy = Calc.divideAndRound(y, img_scale);
        int iw = Calc.divideAndRound(w, img_scale);
        int ih = Calc.divideAndRound(h, img_scale);

        // Clamp negative positions
        if (ix < 0) {
            iw += ix;
            ix = 0;
        }
        if (iy < 0) {
            ih += iy;
            iy = 0;
        }

        // Clamp to image bounds
        if (ix + iw > image.getWidth()) {
            iw = image.getWidth() - ix;
        }
        if (iy + ih > image.getHeight()) {
            ih = image.getHeight() - iy;
        }

        // Final safety check
        iw = Math.max(iw, 1);
        ih = Math.max(ih, 1);

        BufferedImage cropped = image.getSubimage(ix, iy, iw, ih);
        BufferedImage copy = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(cropped, 0, 0, null);
        g2.dispose();

        // Replace image, preserve scaling logic
        image = copy;

        // Note: this still uses x, y, w, h in scaled space for image bounds
        setImageBounds(getImageX() + x, getImageY() + y, w, h);
    }


    public void snapImageBoundsToOtherImages() {
        if (getParent() == null) return;

        int snapThreshold = AppDefaults.SNAP_THRESHOLD;

        int imgX = imageLocation.x;
        int imgY = imageLocation.y;
        int imgW = imageDimension.width;
        int imgH = imageDimension.height;

        //Snap to other images
        if (getParent() instanceof ScaledCanvas canvas) {
            for (Component comp : canvas.getComponents()) {
                if (comp == this || !(comp instanceof ScaledComponent other)) continue;

                Rectangle r = other.getImageBounds();
                if (r == null) continue;

                if (Math.abs(imgX - r.x) < snapThreshold) imgX = r.x;
                if (Math.abs(imgX + imgW - r.x) < snapThreshold) imgX = r.x - imgW;
                if (Math.abs(imgX - (r.x + r.width)) < snapThreshold) imgX = r.x + r.width;
                if (Math.abs(imgX + imgW - (r.x + r.width)) < snapThreshold) imgX = r.x + r.width - imgW;

                if (Math.abs(imgY - r.y) < snapThreshold) imgY = r.y;
                if (Math.abs(imgY + imgH - r.y) < snapThreshold) imgY = r.y - imgH;
                if (Math.abs(imgY - (r.y + r.height)) < snapThreshold) imgY = r.y + r.height;
                if (Math.abs(imgY + imgH - (r.y + r.height)) < snapThreshold) imgY = r.y + r.height - imgH;
            }
        }

        // Translate back to component-local coordinates
        setImageBounds(imgX, imgY, imgW, imgH);
        repaint();
    }

    public void snapBounds() {
        if (getParent() == null) return;

        int snapThreshold = AppDefaults.SNAP_THRESHOLD;

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        //Snap to other images
        if (getParent() instanceof ScaledCanvas canvas) {
            for (Component comp : canvas.getComponents()) {
                if (comp == this || !(comp instanceof ScaledComponent other)) continue;

                Rectangle r = other.getBounds();
                if (r == null) continue;

                if (Math.abs(x - r.x) < snapThreshold) x = r.x;
                if (Math.abs(x + w - r.x) < snapThreshold) x = r.x - w;
                if (Math.abs(x - (r.x + r.width)) < snapThreshold) x = r.x + r.width;
                if (Math.abs(x + w - (r.x + r.width)) < snapThreshold) x = r.x + r.width - w;

                if (Math.abs(y - r.y) < snapThreshold) y = r.y;
                if (Math.abs(y + h - r.y) < snapThreshold) y = r.y - h;
                if (Math.abs(y - (r.y + r.height)) < snapThreshold) y = r.y + r.height;
                if (Math.abs(y + h - (r.y + r.height)) < snapThreshold) y = r.y + r.height - h;
            }
        }

        // Translate back to component-local coordinates
        setBounds(x, y, w, h);
        repaint();
    }
}
