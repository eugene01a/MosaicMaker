import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.*;
import javax.imageio.ImageIO;

public class ScaledCanvasTest {

    private ScaledCanvas canvas;
    private File tempImageFile;
    private int imageWidth;
    private int imageHeight;

    @BeforeEach
    public void setUp() throws IOException {
        canvas = new ScaledCanvas();

        // Define dimensions once
        imageWidth = 120;
        imageHeight = 75;

        // Create the test image
        BufferedImage testImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, imageWidth, imageHeight);
        g.dispose();

        // Write to temp file
        tempImageFile = File.createTempFile("test-image", ".png");
        ImageIO.write(testImage, "png", tempImageFile);
    }

    @AfterEach
    public void tearDown() {
        if (tempImageFile != null && tempImageFile.exists()) {
            tempImageFile.delete();
        }
    }

    @Test
    public void testImageComponentSizeFromFile() {
        canvas.addImageToCanvas(tempImageFile);
        assertEquals(1, canvas.getComponentCount(), "Canvas should have 1 component");

        Component comp = canvas.getComponent(0);

        assertInstanceOf(ScaledComponent.class, comp, "Component should be an ImageComponent");
        assertEquals(imageWidth, comp.getWidth(), "Initial Component Width should match the original image");
        assertEquals(imageHeight, comp.getHeight(), "Initial Component Height should match the original image");
    }

    @Test
    public void testExportOffsetsImagesByCollageBounds() {
        // Saving should crop to the collage bounds even when content starts away from (0,0).
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        ScaledComponent component = new ScaledComponent(image);
        component.setImageLocation(new Point(100, 50));
        canvas.add(component);

        BufferedImage mosaic = canvas.createUnscaledMosaicImage();

        assertEquals(20, mosaic.getWidth());
        assertEquals(10, mosaic.getHeight());
    }
}
