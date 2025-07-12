import static org.junit.jupiter.api.Assertions.*;

import mosaicmaker.ScaledCanvas;
import mosaicmaker.ScaledComponent;
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

        assertInstanceOf(ScaledComponent.class, comp, "Component should be an mosaicmaker.ImageComponent");
        assertEquals(imageWidth, comp.getWidth(), "Initial Component Width should match the original image");
        assertEquals(imageHeight, comp.getHeight(), "Initial Component Height should match the original image");
    }
}
