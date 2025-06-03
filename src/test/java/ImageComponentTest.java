//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import javax.imageio.ImageIO;
//import javax.swing.*;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//public class ImageComponentTest {
//    @Test
//    public void testHorizontalSplitProducesTwoChildren() {
//        // Create dummy image
//        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
//        ImageComponent original = new ImageComponent(img);
//        original.setBounds(0, 0, 100, 100);
//
//        // Create parent canvas
//        JLayeredPane canvas = new JLayeredPane();
//        canvas.setLayout(null);
//        canvas.setSize(200, 200);
//        canvas.add(original);
//
//        // Force horizontal split at Y = 50
//        original.setHorizontalSplitY(50);
//        original.confirmHorizontalSplit();
//
//        // Ensure original was removed
//        boolean originalStillExists = false;
//        for (var comp : canvas.getComponents()) {
//            if (comp == original) {
//                originalStillExists = true;
//                break;
//            }
//        }
//        assertFalse(originalStillExists, "Original image component should be removed after split");
//
//        // Ensure exactly 2 new components were added
//        long count = canvas.getComponentCount();
//        assertEquals(2, count, "Canvas should contain 2 new split components");
//
//        // Check size and bounds
//        Component[] children = canvas.getComponents();
//        assertTrue(children[0] instanceof ImageComponent);
//        assertTrue(children[1] instanceof ImageComponent);
//
//        ImageComponent top = (ImageComponent) children[0];
//        ImageComponent bottom = (ImageComponent) children[1];
//
//        assertEquals(100, top.getWidth());
//        assertEquals(50, top.getHeight());
//
//        assertEquals(100, bottom.getWidth());
//        assertEquals(50, bottom.getHeight());
//
//        assertEquals(0, top.getY());
//        assertEquals(50, bottom.getY());
//    }
//
//}
