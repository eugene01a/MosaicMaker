import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ScaledComponentMouseAdapter extends MouseAdapter {
    private final ScaledComponent sc;

    public ScaledComponentMouseAdapter(ScaledComponent imageComponent) {
        this.sc = imageComponent;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        sc.requestFocusInWindow();

        if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
            if (sc.getParent() instanceof ScaledCanvas) {
                ((ScaledCanvas) sc.getParent()).selectComponent(sc);
            }
        }

        if (sc.isHorizontalSplitMode()) {
            Rectangle lineBounds = new Rectangle(0, sc.getHorizontalSplitY() - 5, sc.getWidth(), 10);
            if (lineBounds.contains(e.getPoint())) {
                sc.setDraggingSplitLine(true);
            }
            return;
        }
        else if (sc.isVerticalSplitMode()) {
            Rectangle lineBounds = new Rectangle(sc.getVerticalSplitX() - 5, 0, 10, sc.getHeight());
            if (lineBounds.contains(e.getPoint())) {
                sc.setDraggingVerticalSplitLine(true);
            }
            return;
        }
        else if (sc.isCropMode()) {
            sc.setCropStart(e.getPoint());
            sc.setCropRect(new Rectangle(sc.getCropStart()));
            sc.repaint();
            return;
        }

        Rectangle bounds = sc.getBounds();

        // Check resizing
        Rectangle resizeTopLeftHandle = new Rectangle(0, 0, ScaledComponent.HANDLE_SIZE, ScaledComponent.HANDLE_SIZE);

        Rectangle resizeBottomRightHandle = new Rectangle(bounds.width - ScaledComponent.HANDLE_SIZE, bounds.height - ScaledComponent.HANDLE_SIZE, ScaledComponent.HANDLE_SIZE, ScaledComponent.HANDLE_SIZE);


        if (resizeTopLeftHandle.contains(e.getPoint())) {
            sc.setResizingCorner(Corner.TOP_LEFT);
            sc.setResizing(true);
            sc.setResizeStartSizeToCurrent();
            int startX = sc.getX()+e.getPoint().x;
            int startY = sc.getY()+e.getPoint().y;
            sc.setResizingStart(new Point(startX, startY));

        } else if (resizeBottomRightHandle.contains(e.getPoint())) {
            sc.setResizingCorner(Corner.BOTTOM_RIGHT);
            sc.setResizing(true);
            sc.setResizingStart(e.getPoint());
            sc.setResizeStartSizeToCurrent();
        } else {
            // Move mode
            sc.setResizing(false);
            sc.setDragOffset(e.getPoint());
            sc.setStartLocation(sc.getLocation());

        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (sc.isHorizontalSplitMode() && sc.isDraggingSplitLine()) {
            sc.setHorizontalSplitY(Math.max(10, Math.min(sc.getHeight() - 10, e.getY())));
            sc.repaint();
            return;
        }
        if (sc.isVerticalSplitMode() && sc.isDraggingVerticalSplitLine()) {
            sc.setVerticalSplitX(Math.max(10, Math.min(sc.getWidth() - 10, e.getX())));
            sc.repaint();
            return;
        }
        if (sc.isCropMode()) {
            if (sc.getCropStart() != null) {
                int x = Math.min(sc.getCropStart().x, e.getX());
                int y = Math.min(sc.getCropStart().y, e.getY());
                int w = Math.abs(e.getX() - sc.getCropStart().x);
                int h = Math.abs(e.getY() - sc.getCropStart().y);
                sc.getCropRect().setBounds(x, y, w, h);
                sc.repaint();
            }
            return;
        }
        if (sc.isResizing()) {
            if (sc.getImage() != null && sc.getImage().getHeight() != 0) {
                process_resize_drag_event(e);
            }
        } else {
            // Move mode
            Point parentPoint = SwingUtilities.convertPoint(sc, e.getPoint(), sc.getParent());
            int x = parentPoint.x - sc.getDragOffset().x;
            int y = parentPoint.y - sc.getDragOffset().y;

            int gridSize = AppDefaults.GRID_SIZE;
            int snapThreshold = 10;

            int snappedX = (x + gridSize / 2) / gridSize * gridSize;
            int snappedY = (y + gridSize / 2) / gridSize * gridSize;

            int width = sc.getWidth();
            int height = sc.getHeight();

            if (sc.getParent() instanceof ScaledCanvas canvas) {
                for (Component comp : canvas.getComponents()) {
                    if (comp == sc) continue;

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

            sc.setLocation(snappedX, snappedY);
        }
        sc.getParent().repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (sc.isHorizontalSplitMode() && sc.isDraggingSplitLine()) {
            sc.setDraggingSplitLine(false);
            sc.confirmHorizontalSplit();
            return;
        }
        if (sc.isVerticalSplitMode() && sc.isDraggingVerticalSplitLine()) {
            sc.setDraggingVerticalSplitLine(false);
            sc.confirmVerticalSplit();
            return;
        }
        if (sc.isCropMode()) {
            int x = Math.min(sc.getCropStart().x, e.getX());
            int y = Math.min(sc.getCropStart().y, e.getY());
            int w = Math.abs(e.getX() - sc.getCropStart().x);
            int h = Math.abs(e.getY() - sc.getCropStart().y);
            sc.getCropRect().setBounds(x, y, w, h);
            sc.showCropPopup();
        }
        if (sc.isResizing()) {
            process_resize_release_event(e);
            sc.setResizing(false);
        }
        if (sc.getStartLocation() != null && (sc.getLocation().x != sc.getStartLocation().x || sc.getLocation().y != sc.getStartLocation().y)) {
            // Move mode
            Point start = sc.getStartLocation();
            Point end = sc.getLocation();
            sc.setUnscaledLocationFromScaledMove(start, end);
            sc.setStartLocation(sc.getLocation());
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (sc.isHorizontalSplitMode() && sc.isDraggingSplitLine()) {
            sc.setHorizontalSplitY(Math.max(10, Math.min(sc.getHeight() - 10, e.getY())));
            sc.repaint();
        }
        if (sc.isCropMode()) return;
        Corner c = sc.getCornerUnderPoint(e.getPoint());
        switch (c) {
            case TOP_LEFT, BOTTOM_RIGHT ->
                    sc.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
            case TOP_RIGHT, BOTTOM_LEFT ->
                    sc.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
            default -> sc.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            if (sc.isHorizontalSplitMode()) return;

            JPopupMenu menu = sc.buildContextMenu();
            if (menu != null) {
                menu.show(sc, e.getX(), e.getY());
            }
        }
    }

    public void process_resize_release_event(MouseEvent e) {
        if (sc.getResizingCorner() == Corner.TOP_LEFT) {

        }
        else {
            // Compute uniform scale factor
            int oldScaledWidth = sc.getResizeStartSize().width;
            Dimension newScaledDim = computeBRResizedDim(e);
            double scale = (double) newScaledDim.width / oldScaledWidth;

            // Scale the image component bounds
            Rectangle unscaledBounds = sc.getImageBounds();
            int resizedUnscaledWidth = (int) Math.round(unscaledBounds.width * scale);
            int resizedUnscaledHeight = (int) Math.round(unscaledBounds.height * scale);
            Dimension resizedUnscaledDim = new Dimension(resizedUnscaledWidth, resizedUnscaledHeight);
            sc.setImageDimension(resizedUnscaledDim);
            sc.setResizedScale(scale);
        }
    }
    public void process_resize_drag_event(MouseEvent e) {
        if (sc.getResizingCorner() == Corner.TOP_LEFT) {
            Rectangle newbounds = computeTLResizedBounds(e);
            sc.setBounds(newbounds);
        }
        else {
            Dimension newDim = computeBRResizedDim(e);
            sc.setSize(newDim.width, newDim.height);
        }
        sc.revalidate();
        sc.repaint();
    }

    public Rectangle computeTLResizedBounds(MouseEvent e) {
        float aspectRatio = (float) sc.getImage().getWidth() / sc.getImage().getHeight();

        // Convert current mouse point to parent coordinates
        Point current = SwingUtilities.convertPoint(sc, e.getPoint(), sc.getParent());
        Point start = sc.getResizingStart();
        Dimension originalSize = sc.getResizeStartSize();

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
        float aspectRatio = (float) sc.getImage().getWidth() / sc.getImage().getHeight();
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
