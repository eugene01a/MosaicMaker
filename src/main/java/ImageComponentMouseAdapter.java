import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ImageComponentMouseAdapter extends MouseAdapter {
    private final ImageComponent imageComponent;
    public ImageComponentMouseAdapter(ImageComponent imageComponent) {
        this.imageComponent = imageComponent;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        imageComponent.requestFocusInWindow();

        if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
            if (imageComponent.getParent() instanceof ScaledCanvas) {
                ((ScaledCanvas) imageComponent.getParent()).selectImage(imageComponent);
            }
        }

        if (imageComponent.isHorizontalSplitMode()) {
            Rectangle lineBounds = new Rectangle(0, imageComponent.getHorizontalSplitY() - 5, imageComponent.getWidth(), 10);
            if (lineBounds.contains(e.getPoint())) {
                imageComponent.setDraggingSplitLine(true);
            }
            return;
        }
        if (imageComponent.isVerticalSplitMode()) {
            Rectangle lineBounds = new Rectangle(imageComponent.getVerticalSplitX() - 5, 0, 10, imageComponent.getHeight());
            if (lineBounds.contains(e.getPoint())) {
                imageComponent.setDraggingVerticalSplitLine(true);
            }
            return;
        }
        if (imageComponent.isCropMode()) {
            imageComponent.setCropStart(e.getPoint());
            imageComponent.setCropRect(new Rectangle(imageComponent.getCropStart()));
            imageComponent.repaint();
            return;
        }

        Rectangle bounds = imageComponent.getBounds();
        Rectangle resizeHandle = new Rectangle(bounds.width - ImageComponent.HANDLE_SIZE, bounds.height - ImageComponent.HANDLE_SIZE, ImageComponent.HANDLE_SIZE, ImageComponent.HANDLE_SIZE);

        if (resizeHandle.contains(e.getPoint())) {
            imageComponent.setResizing(true);
            imageComponent.setResizeStartSizeToCurrent();
        } else {
            // Move mode
            imageComponent.setResizing(false);
            imageComponent.setDragOffset(e.getPoint());
            imageComponent.setStartLocation(imageComponent.getLocation());

        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (imageComponent.isHorizontalSplitMode() && imageComponent.isDraggingSplitLine()) {
            imageComponent.setHorizontalSplitY(Math.max(10, Math.min(imageComponent.getHeight() - 10, e.getY())));
            imageComponent.repaint();
            return;
        }
        if (imageComponent.isVerticalSplitMode() && imageComponent.isDraggingVerticalSplitLine()) {
            imageComponent.setVerticalSplitX(Math.max(10, Math.min(imageComponent.getWidth() - 10, e.getX())));
            imageComponent.repaint();
            return;
        }
        if (imageComponent.isCropMode()) {
            if (imageComponent.getCropStart() != null) {
                int x = Math.min(imageComponent.getCropStart().x, e.getX());
                int y = Math.min(imageComponent.getCropStart().y, e.getY());
                int w = Math.abs(e.getX() - imageComponent.getCropStart().x);
                int h = Math.abs(e.getY() - imageComponent.getCropStart().y);
                imageComponent.getCropRect().setBounds(x, y, w, h);
                imageComponent.repaint();
            }
            return;
        }
        if (imageComponent.isResizing()) {
            if (imageComponent.getImage() != null && imageComponent.getImage().getHeight() != 0) {
                process_resize_drag_event(e);
            }
        } else {
            // Move mode
            Point parentPoint = SwingUtilities.convertPoint(imageComponent, e.getPoint(), imageComponent.getParent());
            int x = parentPoint.x - imageComponent.getDragOffset().x;
            int y = parentPoint.y - imageComponent.getDragOffset().y;

            int gridSize = AppDefaults.GRID_SIZE;
            int snapThreshold = 10;

            int snappedX = (x + gridSize / 2) / gridSize * gridSize;
            int snappedY = (y + gridSize / 2) / gridSize * gridSize;

            int width = imageComponent.getWidth();
            int height = imageComponent.getHeight();

            if (imageComponent.getParent() instanceof ScaledCanvas canvas) {
                for (Component comp : canvas.getComponents()) {
                    if (comp == imageComponent) continue;

                    Rectangle r = comp.getBounds();

                    if (Math.abs(x - r.x) < snapThreshold) snappedX = r.x;
                    if (Math.abs(x + width - r.x) < snapThreshold) snappedX = r.x - width;
                    if (Math.abs(x - (r.x + r.width)) < snapThreshold) snappedX = r.x + r.width;
                    if (Math.abs(x + width - (r.x + r.width)) < snapThreshold)
                        snappedX = r.x + r.width - width;

                    if (Math.abs(y - r.y) < snapThreshold) snappedY = r.y;
                    if (Math.abs(y + height - r.y) < snapThreshold) snappedY = r.y - height;
                    if (Math.abs(y - (r.y + r.height)) < snapThreshold) snappedY = r.y + r.height;
                    if (Math.abs(y + height - (r.y + r.height)) < snapThreshold)
                        snappedY = r.y + r.height - height;
                }
            }

            imageComponent.setLocation(snappedX, snappedY);
        }
        imageComponent.getParent().repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (imageComponent.isHorizontalSplitMode() && imageComponent.isDraggingSplitLine()) {
            imageComponent.setDraggingSplitLine(false);
            imageComponent.confirmHorizontalSplit();
            return;
        }
        if (imageComponent.isVerticalSplitMode() && imageComponent.isDraggingVerticalSplitLine()) {
            imageComponent.setDraggingVerticalSplitLine(false);
            imageComponent.confirmVerticalSplit();
            return;
        }
        if (imageComponent.isCropMode()) {
            int x = Math.min(imageComponent.getCropStart().x, e.getX());
            int y = Math.min(imageComponent.getCropStart().y, e.getY());
            int w = Math.abs(e.getX() - imageComponent.getCropStart().x);
            int h = Math.abs(e.getY() - imageComponent.getCropStart().y);
            imageComponent.getCropRect().setBounds(x, y, w, h);
            imageComponent.showCropPopup();
        }
        if (imageComponent.isResizing()) {
            process_resize_release_event(e);
            imageComponent.setResizing(false);
        }
        if (imageComponent.getStartLocation() != null &&
                (imageComponent.getLocation().x != imageComponent.getStartLocation().x ||
                imageComponent.getLocation().y != imageComponent.getStartLocation().y )
        ) {
            // Move mode
            Point start = imageComponent.getStartLocation();
            Point end = imageComponent.getLocation();
            imageComponent.setUnscaledLocationFromScaledMove(start,end);
            imageComponent.setStartLocation(imageComponent.getLocation());
            }
}

    @Override
    public void mouseMoved(MouseEvent e) {
        if (imageComponent.isHorizontalSplitMode() && imageComponent.isDraggingSplitLine()) {
            imageComponent.setHorizontalSplitY(Math.max(10, Math.min(imageComponent.getHeight() - 10, e.getY())));
            imageComponent.repaint();
        }
        if (imageComponent.isCropMode()) return;
        ImageComponent.Corner c = imageComponent.getCornerUnderPoint(e.getPoint());
        switch (c) {
            case TOP_LEFT, BOTTOM_RIGHT -> imageComponent.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
            case TOP_RIGHT, BOTTOM_LEFT -> imageComponent.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
            default -> imageComponent.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            if (imageComponent.isHorizontalSplitMode()) return;

            JPopupMenu menu = imageComponent.buildContextMenu();
            if (menu != null) {
                menu.show(imageComponent, e.getX(), e.getY());
            }
        }
    }
    public void process_resize_release_event(MouseEvent e){
        // Compute uniform scale factor
        int oldScaledWidth = imageComponent.getResizeStartSize().width;
        Dimension newScaledDim = computeNewScaledDimensions(e);
        double scale = (double) newScaledDim.width / oldScaledWidth;

        // Scale the unscaled image bounds
        Rectangle unscaledBounds = imageComponent.getUnscaledImageBounds();
        int resizedUnscaledWidth = (int) Math.round(unscaledBounds.width * scale);
        int resizedUnscaledHeight = (int) Math.round(unscaledBounds.height * scale);
        Dimension resizedUnscaledDim = new Dimension(resizedUnscaledWidth, resizedUnscaledHeight);
        imageComponent.setUnscaledImageDimension(resizedUnscaledDim);
        imageComponent.setResizedScale(scale);
    }
    public void process_resize_drag_event(MouseEvent e){
        Dimension newDim = computeNewScaledDimensions(e);
        imageComponent.setSize(newDim.width, newDim.height);
        imageComponent.revalidate();
        imageComponent.repaint();
    }

    public Dimension computeNewScaledDimensions(MouseEvent e){
        float aspectRatio = (float) imageComponent.getImage().getWidth() / imageComponent.getImage().getHeight();
        // Raw mouse input
        int rawNewWidth = Math.max(20, e.getX());
        int rawNewHeight = Math.max(20, e.getY());

        // Adjust for aspect ratio
        int newScaledWidth, newScaledHeight;
        if (rawNewWidth / (float) rawNewHeight > aspectRatio) {
            newScaledHeight = rawNewHeight;
            newScaledWidth = (int) (rawNewHeight * aspectRatio);
        } else {
            newScaledWidth = rawNewWidth;
            newScaledHeight = (int) (rawNewWidth / aspectRatio);
        }
        return new Dimension(newScaledWidth, newScaledHeight);
    }
}
