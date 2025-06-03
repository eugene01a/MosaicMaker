import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.css.Rect;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImageComponentMouseAdapterTest {

    private ScaledCanvas canvas;

    @Test
    void testProcessMoveEvent() {
        BufferedImage dummyImage1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageComponent ic1 = new ImageComponent(dummyImage1);
        canvas = new ScaledCanvas();
        canvas.add(ic1);
        ImageComponentMouseAdapter adapter1 = new ImageComponentMouseAdapter(ic1);
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
        ImageComponent ic1 = new ImageComponent(dummyImage1);
        canvas = new ScaledCanvas();
        canvas.add(ic1);
        ImageComponentMouseAdapter adapter1 = new ImageComponentMouseAdapter(ic1);
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
        assertEquals(expectedUnscaledBounds, ic1.getUnscaledImageBounds());
    }
}
