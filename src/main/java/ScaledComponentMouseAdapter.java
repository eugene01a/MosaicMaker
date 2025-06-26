import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ScaledComponentMouseAdapter extends MouseAdapter {
    private final ScaledComponent scaledComponent;

    public ScaledComponentMouseAdapter(ScaledComponent imageComponent) {
        this.scaledComponent = imageComponent;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        scaledComponent.requestFocusInWindow();

        if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
            if (scaledComponent.getParent() instanceof ScaledCanvas) {
                ((ScaledCanvas) scaledComponent.getParent()).selectComponent(scaledComponent);
            }
        }

        if (scaledComponent.isHorizontalSplitMode()) {
            Rectangle lineBounds = new Rectangle(0, scaledComponent.getHorizontalSplitY() - 5, scaledComponent.getWidth(), 10);
            if (lineBounds.contains(e.getPoint())) {
                scaledComponent.setDraggingSplitLine(true);
            }
            return;
        }
        else if (scaledComponent.isVerticalSplitMode()) {
            Rectangle lineBounds = new Rectangle(scaledComponent.getVerticalSplitX() - 5, 0, 10, scaledComponent.getHeight());
            if (lineBounds.contains(e.getPoint())) {
                scaledComponent.setDraggingVerticalSplitLine(true);
            }
            return;
        }
        else if (scaledComponent.isCropMode()) {
            scaledComponent.setCropStart(e.getPoint());
            scaledComponent.setCropRect(new Rectangle(scaledComponent.getCropStart()));
            scaledComponent.repaint();
            return;
        }

        Rectangle bounds = scaledComponent.getBounds();

        // Check resizing
        Rectangle resizeTopLeftHandle = new Rectangle(0, 0, ScaledComponent.HANDLE_SIZE, ScaledComponent.HANDLE_SIZE);

        Rectangle resizeBottomRightHandle = new Rectangle(bounds.width - ScaledComponent.HANDLE_SIZE, bounds.height - ScaledComponent.HANDLE_SIZE, ScaledComponent.HANDLE_SIZE, ScaledComponent.HANDLE_SIZE);


        if (resizeTopLeftHandle.contains(e.getPoint())) {
            scaledComponent.setResizingCorner(Corner.TOP_LEFT);
            scaledComponent.setResizing(true);
            scaledComponent.setResizeStartSizeToCurrent();
            int startX = scaledComponent.getX()+e.getPoint().x;
            int startY = scaledComponent.getY()+e.getPoint().y;
            scaledComponent.setResizingStart(new Point(startX, startY));

        } else if (resizeBottomRightHandle.contains(e.getPoint())) {
            scaledComponent.setResizingCorner(Corner.BOTTOM_RIGHT);
            scaledComponent.setResizing(true);
            scaledComponent.setResizingStart(e.getPoint());
            scaledComponent.setResizeStartSizeToCurrent();
        } else {
            // Move mode
            scaledComponent.setResizing(false);
            scaledComponent.setDragOffset(e.getPoint());
            scaledComponent.setStartLocation(scaledComponent.getLocation());

        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (scaledComponent.isHorizontalSplitMode() && scaledComponent.isDraggingSplitLine()) {
            scaledComponent.setHorizontalSplitY(Math.max(10, Math.min(scaledComponent.getHeight() - 10, e.getY())));
            scaledComponent.repaint();
            return;
        }
        if (scaledComponent.isVerticalSplitMode() && scaledComponent.isDraggingVerticalSplitLine()) {
            scaledComponent.setVerticalSplitX(Math.max(10, Math.min(scaledComponent.getWidth() - 10, e.getX())));
            scaledComponent.repaint();
            return;
        }
        if (scaledComponent.isCropMode()) {
            if (scaledComponent.getCropStart() != null) {
                int x = Math.min(scaledComponent.getCropStart().x, e.getX());
                int y = Math.min(scaledComponent.getCropStart().y, e.getY());
                int w = Math.abs(e.getX() - scaledComponent.getCropStart().x);
                int h = Math.abs(e.getY() - scaledComponent.getCropStart().y);
                scaledComponent.getCropRect().setBounds(x, y, w, h);
                scaledComponent.repaint();
            }
            return;
        }
        if (scaledComponent.isResizing()) {
            if (scaledComponent.getImage() != null && scaledComponent.getImage().getHeight() != 0) {
                process_resize_drag_event(e);
            }
        } else {
            // Move mode
            Point parentPoint = SwingUtilities.convertPoint(scaledComponent, e.getPoint(), scaledComponent.getParent());
            int x = parentPoint.x - scaledComponent.getDragOffset().x;
            int y = parentPoint.y - scaledComponent.getDragOffset().y;

            int gridSize = AppDefaults.GRID_SIZE;
            int snapThreshold = 10;

            int snappedX = (x + gridSize / 2) / gridSize * gridSize;
            int snappedY = (y + gridSize / 2) / gridSize * gridSize;

            int width = scaledComponent.getWidth();
            int height = scaledComponent.getHeight();

            if (scaledComponent.getParent() instanceof ScaledCanvas canvas) {
                for (Component comp : canvas.getComponents()) {
                    if (comp == scaledComponent) continue;

                    Rectangle r = comp.getBounds();

                    if (Math.abs(x - r.x) < snapThreshold) snappedX = r.x;
                    if (Math.abs(x + width - r.x) < snapThreshold) snappedX = r.x - width;
                    if (Math.abs(x - (r.x + r.width)) < snapThreshold) snappedX = r.x + r.width;
                    if (Math.abs(x + width - (r.x + r.width)) < snapThreshold) snappedX = r.x + r.width - width;

                    if (Math.abs(y - r.y) < snapThreshold) snappedY = r.y;
                    if (Math.abs(y + height - r.y) < snapThreshold) snappedY = r.y - height;
                    if (Math.abs(y - (r.y + r.height)) < snapThreshold) snappedY = r.y + r.height;
                    if (Math.abs(y + height - (r.y + r.height)) < snapThreshold) snappedY = r.y + r.height - height;
                }
            }

            scaledComponent.setLocation(snappedX, snappedY);
        }
        scaledComponent.getParent().repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (scaledComponent.isHorizontalSplitMode() && scaledComponent.isDraggingSplitLine()) {
            scaledComponent.setDraggingSplitLine(false);
            scaledComponent.confirmHorizontalSplit();
            return;
        }
        if (scaledComponent.isVerticalSplitMode() && scaledComponent.isDraggingVerticalSplitLine()) {
            scaledComponent.setDraggingVerticalSplitLine(false);
            scaledComponent.confirmVerticalSplit();
            return;
        }
        if (scaledComponent.isCropMode()) {
            int x = Math.min(scaledComponent.getCropStart().x, e.getX());
            int y = Math.min(scaledComponent.getCropStart().y, e.getY());
            int w = Math.abs(e.getX() - scaledComponent.getCropStart().x);
            int h = Math.abs(e.getY() - scaledComponent.getCropStart().y);
            scaledComponent.getCropRect().setBounds(x, y, w, h);
            scaledComponent.showCropPopup();
        }
        if (scaledComponent.isResizing()) {
            process_resize_release_event(e);
            scaledComponent.setResizing(false);
        }
        if (scaledComponent.getStartLocation() != null && (scaledComponent.getLocation().x != scaledComponent.getStartLocation().x || scaledComponent.getLocation().y != scaledComponent.getStartLocation().y)) {
            // Move mode
            Point start = scaledComponent.getStartLocation();
            Point end = scaledComponent.getLocation();
            scaledComponent.setUnscaledLocationFromScaledMove(start, end);
            scaledComponent.setStartLocation(scaledComponent.getLocation());
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (scaledComponent.isHorizontalSplitMode() && scaledComponent.isDraggingSplitLine()) {
            scaledComponent.setHorizontalSplitY(Math.max(10, Math.min(scaledComponent.getHeight() - 10, e.getY())));
            scaledComponent.repaint();
        }
        if (scaledComponent.isCropMode()) return;
        Corner c = scaledComponent.getCornerUnderPoint(e.getPoint());
        switch (c) {
            case TOP_LEFT, BOTTOM_RIGHT ->
                    scaledComponent.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
            case TOP_RIGHT, BOTTOM_LEFT ->
                    scaledComponent.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
            default -> scaledComponent.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            if (scaledComponent.isHorizontalSplitMode()) return;

            JPopupMenu menu = scaledComponent.buildContextMenu();
            if (menu != null) {
                menu.show(scaledComponent, e.getX(), e.getY());
            }
        }
    }

    public void process_resize_release_event(MouseEvent e) {
        if (scaledComponent.getResizingCorner() == Corner.TOP_LEFT) {

        }
        else {
            // Compute uniform scale factor
            int oldScaledWidth = scaledComponent.getResizeStartSize().width;
            Dimension newScaledDim = computeBRResizedDim(e);
            double scale = (double) newScaledDim.width / oldScaledWidth;

            // Scale the image component bounds
            Rectangle unscaledBounds = scaledComponent.getImageBounds();
            int resizedUnscaledWidth = (int) Math.round(unscaledBounds.width * scale);
            int resizedUnscaledHeight = (int) Math.round(unscaledBounds.height * scale);
            Dimension resizedUnscaledDim = new Dimension(resizedUnscaledWidth, resizedUnscaledHeight);
            scaledComponent.setImageDimension(resizedUnscaledDim);
            scaledComponent.setResizedScale(scale);
        }
    }
    public void process_resize_drag_event(MouseEvent e) {
        if (scaledComponent.getResizingCorner() == Corner.TOP_LEFT) {
            Rectangle newbounds = computeTLResizedBounds(e);
            scaledComponent.setBounds(newbounds);
        }
        else {
            Dimension newDim = computeBRResizedDim(e);
            scaledComponent.setSize(newDim.width, newDim.height);
        }
        scaledComponent.revalidate();
        scaledComponent.repaint();
    }

    public Rectangle computeTLResizedBounds(MouseEvent e) {
        float aspectRatio = (float) scaledComponent.getImage().getWidth() / scaledComponent.getImage().getHeight();

        // Convert current mouse point to parent coordinates
        Point current = SwingUtilities.convertPoint(scaledComponent, e.getPoint(), scaledComponent.getParent());
        Point start = scaledComponent.getResizingStart();
        Dimension originalSize = scaledComponent.getResizeStartSize();

        // Calculate new raw dimensions assuming bottom-right stays fixed
        int rawNewWidth = Math.max(20, start.x + originalSize.width - current.x);
        int rawNewHeight = Math.max(20, start.y + originalSize.height - current.y);

        // Enforce aspect ratio
        int newScaledWidth, newScaledHeight;
        if (rawNewWidth / (float) rawNewHeight > aspectRatio) {
            newScaledHeight = rawNewHeight;
            newScaledWidth = (int) (rawNewHeight * aspectRatio);
        } else {
            newScaledWidth = rawNewWidth;
            newScaledHeight = (int) (rawNewWidth / aspectRatio);
        }

        // Pin the bottom-right corner based on original bounds
        int newX = start.x + originalSize.width - newScaledWidth;
        int newY = start.y + originalSize.height - newScaledHeight;

        return new Rectangle(newX, newY, newScaledWidth, newScaledHeight);
    }



    public Dimension computeBRResizedDim(MouseEvent e) {
        float aspectRatio = (float) scaledComponent.getImage().getWidth() / scaledComponent.getImage().getHeight();
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
