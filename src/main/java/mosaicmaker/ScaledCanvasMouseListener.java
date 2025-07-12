package mosaicmaker;

import java.awt.event.*;
import javax.swing.JLabel;

public class ScaledCanvasMouseListener implements MouseWheelListener, MouseMotionListener {
    private final ScaledCanvas canvas;
    private final JLabel coordLabel;
    private final double zoomFactor;

    public ScaledCanvasMouseListener(ScaledCanvas canvas, JLabel coordLabel) {
        this(canvas, coordLabel, 1.1);
    }

    public ScaledCanvasMouseListener(ScaledCanvas canvas, JLabel coordLabel, double zoomFactor) {
        this.canvas = canvas;
        this.coordLabel = coordLabel;
        this.zoomFactor = zoomFactor;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double currentScale = canvas.getScale();
        if (e.getPreciseWheelRotation() < 0) {
            currentScale *= zoomFactor;
        } else {
            currentScale /= zoomFactor;
        }
        canvas.setScale(currentScale);
        canvas.updateChildrenBounds();
        canvas.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateCoordinates(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updateCoordinates(e);
    }

    private void updateCoordinates(MouseEvent e) {
        double scale = canvas.getScale();
        int x = (int) (e.getX() / scale);
        int y = (int) (e.getY() / scale);
        coordLabel.setText("x: " + x + ", y: " + y);
    }
}
