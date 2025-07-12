import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ScaledComponentMouseAdapterTest {

    private ScaledCanvas canvas;

    private void resizeSC(ScaledComponent sc, int x1, int y1, int x2, int y2){
        ScaledComponentMouseAdapter adapter = new ScaledComponentMouseAdapter(sc);

        // PRESS at x1,y1 in parent → convert to component-local
        int pressX = x1 - sc.getX();
        int pressY = y1 - sc.getY();

        // DRAG to x2,y2 in parent → simulate mouse moving to local point
        int relX = x2 - sc.getX();
        int relY = y2 - sc.getY();

        MouseEvent mockPressEvent = new MouseEvent(sc, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, pressX, pressY , 1, false);
        adapter.mousePressed(mockPressEvent);
        MouseEvent mockDragEvent = new MouseEvent(sc, MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(), 0, relX, relY, 1, false);
        adapter.mouseDragged(mockDragEvent);
        MouseEvent mockReleaseEvent = new MouseEvent(sc, MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(), 0, relX, relY, 1, false);
        adapter.mouseReleased(mockReleaseEvent);
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
    void testProcessMoveEvent() {
        BufferedImage dummyImage1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ScaledComponent sc1 = new ScaledComponent(dummyImage1);
        canvas = new ScaledCanvas();
        canvas.add(sc1);
        moveSC(sc1,200, 0);
        assertEquals(new Point(200, 0), sc1.getLocation());
    }

    @Test
    void testProcessResizeBREvent() {
        BufferedImage dummyImage1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ScaledComponent sc = new ScaledComponent(dummyImage1);

        canvas = new ScaledCanvas();
        canvas.add(sc);
        sc.setLocation(new Point(10,10));
        resizeSC(sc, sc.getX() + sc.getWidth() - 1, sc.getY() + sc.getHeight() - 1, 210, 210);
        Rectangle expectedUnscaledBounds = new Rectangle(10, 10, 200, 200);
        assertEquals(new Point(10, 10), sc.getLocation());
        assertEquals(new Dimension(200, 200), sc.ic.getImageBounds().getSize());
    }
    @Test
    void testResizeTopLeft() {
        /***
         *
         * NOTE: all x,y values are relative to parent coordinate system unless otherwise noted
         *
         * if given w2, and resize handle is top left corner:
         *
         * original 4 corners are:
         * (x1,y1) (x1+w1,y1)
         * (x1,y1+h1) (x1+w1,y1+h1)
         *
         * new corners will be:
         * (x2,y2) (x2+w2, y2)
         * (x2,y2+h2) (x2+w2, y2+h2)
         *
         * Since BR corner unchanged if resize from TL:
         * x2=x1+w1-w2, y2=y1+h1-h2
         *
         * So, top right corner should move to (x1+w1-w2, y1+h1-h2)
         * where h2 is h1*w2/w1 (assuming proportional resizing)
         */
        int w2 = 400;

        int x1 = 0;
        int y1 = 10;
        int w1 = 100;
        int h1 = 50;

        BufferedImage dummyImage1 = new BufferedImage(w1, h1, BufferedImage.TYPE_INT_RGB);
        ScaledComponent sc = new ScaledComponent(dummyImage1);
        canvas = new ScaledCanvas();
        canvas.add(sc);

        moveSC(sc,x1,y1);

        int h2 = h1*w2/w1;
        int x2 = x1+w1-w2;
        int y2 = y1+h1-h2;
        resizeSC(sc, x1, y1, x2, y2);

        Point expectedLoc = new Point(x2, y2);
        assertEquals(expectedLoc, sc.getLocation());

        assertEquals(w2, sc.getWidth());
        assertEquals(h2, sc.getHeight());
    }

}