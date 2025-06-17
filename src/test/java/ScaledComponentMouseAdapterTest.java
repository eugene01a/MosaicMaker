import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ScaledComponentMouseAdapterTest {

    private ScaledCanvas canvas;

    private void invokePrivateCrop(ScaledComponent sc) {
        try {
            java.lang.reflect.Method cropMethod = ScaledComponent.class.getDeclaredMethod("performCrop");
            cropMethod.setAccessible(true);
            cropMethod.invoke(sc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke crop", e);
        }
    }

    @Test
    void testProcessMoveEvent() {
        BufferedImage dummyImage1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ScaledComponent ic1 = new ScaledComponent(dummyImage1);
        canvas = new ScaledCanvas();
        canvas.add(ic1);
        ScaledComponentMouseAdapter adapter1 = new ScaledComponentMouseAdapter(ic1);
        MouseEvent mockPressEvent1 = new MouseEvent(ic1, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 50, 50, 1, false);
        adapter1.mousePressed(mockPressEvent1);
        MouseEvent mockDragEvent1 = new MouseEvent(ic1, MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(), 0, 250, 50, 1, false);
        adapter1.mouseDragged(mockDragEvent1);
        MouseEvent mockReleaseEvent1 = new MouseEvent(ic1, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, 250, 50, 1, false);
        adapter1.mouseReleased(mockReleaseEvent1);
        assertEquals(new Point(200, 0), ic1.getLocation());
    }

    @Test
    void testProcessResizeEvent() {
        BufferedImage dummyImage1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ScaledComponent ic1 = new ScaledComponent(dummyImage1);
        canvas = new ScaledCanvas();
        canvas.add(ic1);
        ScaledComponentMouseAdapter adapter1 = new ScaledComponentMouseAdapter(ic1);
        MouseEvent mockPressEvent1 = new MouseEvent(ic1, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 99, 99, 1, false);
        adapter1.mousePressed(mockPressEvent1);
        MouseEvent mockDragEvent1 = new MouseEvent(ic1, MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(), 0, 200, 200, 1, false);
        adapter1.mouseDragged(mockDragEvent1);
        MouseEvent mockReleaseEvent1 = new MouseEvent(ic1, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, 200, 200, 1, false);
        adapter1.mouseReleased(mockReleaseEvent1);
        Rectangle expectedUnscaledBounds = new Rectangle(0, 0, 200, 200);
        assertEquals(expectedUnscaledBounds, ic1.getImageBounds());
    }
    @Test
    public void testAddTwoImagesAndCropSecondOneThenZoomToFit() {
        ScaledCanvas canvas = new ScaledCanvas();

        BufferedImage img1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        BufferedImage img2 = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);

        ScaledComponent sc1 = new ScaledComponent(img1);
        sc1.setImageLocation(new Point(0, 0));
        sc1.setBounds(0, 0, 100, 100);
        canvas.add(sc1);

        ScaledComponent sc2 = new ScaledComponent(img2);
        sc2.setImageLocation(new Point(100, 0));
        sc2.setBounds(100, 0, 200, 100);
        canvas.add(sc2);

        // Manually apply crop to sc2
        sc2.enterCropMode();
        sc2.setCropStart(new Point(0, 0));
        sc2.setCropRect(new Rectangle(0, 0, 100, 100));  // Crop left half
        invokePrivateCrop(sc2);  // Simulate clicking "Apply Crop"

        Rectangle boundsBeforeZoom = sc2.getBounds();
        assertEquals(new Rectangle(100, 0, 100, 100), boundsBeforeZoom);
        assertEquals(new Point(100, 0), sc2.getImageLocation());
        assertEquals(new Dimension(100, 100), sc2.getImageDimension());

        // Now zoom to fit
        canvas.setScale(0.5);  // simulate a zoom-out
        canvas.updateChildrenBounds();

        Rectangle scaledBounds = sc2.getBounds();
        assertEquals(new Rectangle(50, 0, 50, 50), scaledBounds); // expect half the size and same relative position
    }


}