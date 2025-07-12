import mosaicmaker.ScaledComponent;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ScaledComponentTest {

    @Test
    void testCropWithNegativeCoordinatesAdjustsSize() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int imgWidth = 200;
        int imgHeight = 200;

        BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        ScaledComponent sc = new ScaledComponent(image);
        sc.setSize(imgWidth, imgHeight); // match visual size to image size

        // Set crop mode and a crop rect with negative coordinates
        sc.enterCropMode();
        sc.setCropRect(new Rectangle(-20, -10, 100, 100));

        // Bypass private access of performCrop method
        Method performCropMethod = ScaledComponent.class.getDeclaredMethod("performCrop");
        performCropMethod.setAccessible(true);
        performCropMethod.invoke(sc);

        // Image should now be resized to (80, 90) as (-20, -10) is clamped to (0,0)
        BufferedImage cropped = sc.ic.getImage();

        assertEquals(80, cropped.getWidth(), "Width should adjust by trimming negative x");
        assertEquals(90, cropped.getHeight(), "Height should adjust by trimming negative y");
    }
}
