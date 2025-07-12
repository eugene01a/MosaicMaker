import mosaicmaker.ScaledCanvas;
import mosaicmaker.ScaledComponent;
import mosaicmaker.ScaledComponentMouseAdapter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class End2EndTest {

    private ScaledCanvas canvas;

    private void cropSC(ScaledComponent sc, Point startPt, Point endPt) {
        sc.enterCropMode();
        sc.setCropStart(startPt);
        sc.setCropRect(new Rectangle(startPt.x, startPt.y, endPt.x, endPt.y));
        try {
            java.lang.reflect.Method cropMethod = ScaledComponent.class.getDeclaredMethod("performCrop");
            cropMethod.setAccessible(true);
            cropMethod.invoke(sc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke crop", e);
        }
    }

    private void moveSC(ScaledComponent sc, int newX, int newY){
        ScaledComponentMouseAdapter adapter = new ScaledComponentMouseAdapter(sc);

        // Mouse press inside the component.
        Point mousePt = new Point(sc.getWidth()/2, sc.getHeight()/2);
        MouseEvent mockPressEvent = new MouseEvent(sc, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0,
                mousePt.x, mousePt.y, 1, false);
        adapter.mousePressed(mockPressEvent);

        int deltaX = newX - sc.getX();
        int deltaY = newY - sc.getY();
        mousePt.translate(deltaX, deltaY);
        MouseEvent mockDragEvent1 = new MouseEvent(sc, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0,
                mousePt.x, mousePt.y, 1, false);
        adapter.mouseDragged(mockDragEvent1);
        MouseEvent mockReleaseEvent = new MouseEvent(sc, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, mousePt.x, mousePt.y, 1, false);
        adapter.mouseReleased(mockReleaseEvent);
    }

    @Test
    public void testAddTwoImagesAndCropSecondOneThenZoomToFit() {
        ScaledCanvas canvas = new ScaledCanvas();

        BufferedImage img1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        BufferedImage img2 = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);

        ScaledComponent sc1 = new ScaledComponent(img1);
        canvas.add(sc1);

        ScaledComponent sc2 = new ScaledComponent(img2);
        canvas.add(sc2);
        moveSC(sc2,100,0);

        // Crop sc2
        Point startPt = new Point(0,0);
        Point endPt = new Point(100,100);
        cropSC(sc2, startPt, endPt);

        }


}