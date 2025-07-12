import static org.junit.jupiter.api.Assertions.*;

import mosaicmaker.MosaicMaker;
import mosaicmaker.ScaledCanvas;
import mosaicmaker.ScaledComponent;
import org.junit.jupiter.api.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MosaicMakerTest {
    private Method initMethod;
    private Field frameField;
    private Method getContentPaneMethod;
    private MosaicMaker maker;
    private Field canvasField;
    private Field scaleField;
    private Method zoomToFitMethod;
    private Field topBarField;
    private Field bottomBarField;
    private JFrame frame;
    private JMenuBar topBar;
    private JPanel bottomBar;
    private ScaledCanvas canvas;

    public Dimension getFrameDimensions(){
        return new Dimension(frame.getContentPane().getWidth(), frame.getHeight() - topBar.getHeight() - bottomBar.getHeight());
    }
    public Dimension scaleDimension(Dimension d, double scale){
        return new Dimension((int) (d.getWidth() * scale), (int) (d.getHeight() * scale));
    }
    public double computeScale(Dimension originDim, Dimension destDim) {
        double widthScale = destDim.getWidth() / originDim.getWidth();
        double heightScale = destDim.getHeight() / originDim.getHeight();
        return Math.min(widthScale, heightScale);
    }

    public Dimension computeZoomToFitDimensions(Dimension imgDim) {
        Dimension frameDim = getFrameDimensions();
        double expectedScale = computeScale(imgDim, frameDim);
        Dimension expectedDim = new Dimension((int) (imgDim.width * expectedScale), (int) (imgDim.height * expectedScale));
        return expectedDim;
    }

    public File createTestImage(Dimension d) throws IOException {
        int h = d.height;
        int w = d.width;
        // Create test image
        BufferedImage testImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, w, h);
        g.dispose();

        // Write to file, appends a random string between them to ensure uniqueness. (ex. test-image12749328326705195721.png)
        File testImageFile = File.createTempFile("test-image", ".png");
        ImageIO.write(testImage, "png", testImageFile);
        return testImageFile;
    }

    @BeforeEach
    public void setUp() throws Exception {
        initMethod = MosaicMaker.class.getDeclaredMethod("createAndShowGUI");
        initMethod.setAccessible(true);

        frameField = MosaicMaker.class.getDeclaredField("frame");
        frameField.setAccessible(true);

        getContentPaneMethod = JFrame.class.getDeclaredMethod("getContentPane");  // Fixed: use JFrame
        getContentPaneMethod.setAccessible(true);

        topBarField = MosaicMaker.class.getDeclaredField("topBar");
        topBarField.setAccessible(true);

        bottomBarField = MosaicMaker.class.getDeclaredField("bottomBar");
        bottomBarField.setAccessible(true);

        zoomToFitMethod = MosaicMaker.class.getDeclaredMethod("zoomToFit");
        zoomToFitMethod.setAccessible(true);

        canvasField = MosaicMaker.class.getDeclaredField("canvas");
        canvasField.setAccessible(true);

        scaleField = ScaledCanvas.class.getDeclaredField("scale");
        scaleField.setAccessible(true);

        maker = new MosaicMaker();
        initMethod.invoke(maker);

        frame = (JFrame) frameField.get(maker);  // Fixed: cast to JFrame
        topBar = (JMenuBar) topBarField.get(maker);
        bottomBar = (JPanel) bottomBarField.get(maker);
        canvas = (ScaledCanvas) canvasField.get(maker);
    }

    @Test
    public void testZoomToFitScaleAndSize() throws Exception {
        int img1W = 100;
        int img1H = 200;

        File testFile1 = createTestImage(new Dimension(img1W, img1H));
        assertInstanceOf(ScaledCanvas.class, canvas, "Canvas must be a mosaicmaker.ScaledCanvas");
        canvas.addImageToCanvas(testFile1);
        zoomToFitMethod.invoke(maker);

        // Get mosaicmaker.ImageComponent
        Component[] components = canvas.getComponents();
        assertEquals(1, components.length, "Canvas should contain 1 component");
        assertInstanceOf(ScaledComponent.class, components[0], "Component should be mosaicmaker.ImageComponent");
        ScaledComponent ic = (ScaledComponent) components[0];

        // Compute expected scale and preferred size
        Container contentPane = (Container) getContentPaneMethod.invoke(frame);
        double wFrame = contentPane.getWidth();
        double hFrame = contentPane.getHeight() - topBar.getHeight() - bottomBar.getHeight();
        double widthScale = wFrame / img1W;
        double heightScale = hFrame / img1H;
        double expectedScale = Math.min(widthScale, heightScale);
        int expectedWidth = (int) (img1W * expectedScale);
        int expectedHeight = (int) (img1H * expectedScale);

        // Verify component is scaled to fit into frame
        Dimension icSize = ic.getSize();
        double actualScale = scaleField.getDouble(canvas);
        assertEquals(expectedWidth, icSize.width, "Preferred width should match expected scaled width");
        assertEquals(expectedHeight, icSize.height, "Preferred height should match expected scaled height");
        assertEquals(expectedScale, actualScale, "Scale field should match computed zoom-to-fit scale");
    }

    @Test
    public void testAddingMultipleImages() throws Exception {
        Dimension d1 = new Dimension(100, 200);
        Dimension d2 = new Dimension(1800, 900);

        File testFile1 = createTestImage(d1);
        canvas.addImageToCanvas(testFile1);
        zoomToFitMethod.invoke(maker);

        File testFile2 = createTestImage(d2);

        // Get mosaicmaker.ImageComponent
        Dimension expectedDim1 = computeZoomToFitDimensions(d1);
        ScaledComponent ic1 = (ScaledComponent) canvas.getComponent(0);
        Dimension ic1Size = ic1.getSize();
        assertEquals(expectedDim1, ic1Size);

        double expectedScale = computeScale(d1, getFrameDimensions());
        double actualScale = scaleField.getDouble(canvas);
        assertEquals(expectedScale, actualScale, "Scale field should match computed zoom-to-fit scale");

        canvas.addImageToCanvas(testFile2);
        Component[] components = canvas.getComponents();
        assertEquals(2, components.length, "Canvas should contain 2 component");
        Dimension expectedDim2 = scaleDimension(d2, expectedScale);
        ScaledComponent ic2 = (ScaledComponent) components[0];
        Dimension ic2Size = ic2.getSize();
        assertEquals(expectedDim2, ic2Size);

        zoomToFitMethod.invoke(maker);
        double expectedScale2 = computeScale(d2, getFrameDimensions());
        double actualScale2 = scaleField.getDouble(canvas);
        assertEquals(expectedScale2, actualScale2);
        assertEquals(scaleDimension(d2, expectedScale2), ic2.getSize());
        assertEquals(scaleDimension(d1, expectedScale2), ic1.getSize());
    }

    @Test
    public void testMovingMultipleImages() throws Exception {
        Dimension d1 = new Dimension(100, 200);
        Dimension d2 = new Dimension(1800, 900);

        File testFile1 = createTestImage(d1);
        File testFile2 = createTestImage(d2);

        canvas.addImageToCanvas(testFile1);
        ScaledComponent sc1 = (ScaledComponent) canvas.getComponents()[0];
        canvas.addImageToCanvas(testFile2);
        ScaledComponent sc2 = (ScaledComponent) canvas.getComponents()[0];
        sc2.ic.setLocation(new Point(sc1.getX()+sc1.getWidth(),sc1.getY()));
        zoomToFitMethod.invoke(maker);
        Point expectedLocation2 = new Point((sc1.getX()+sc1.getWidth()),sc1.getY());
        assertEquals(sc2.getLocation(),expectedLocation2);
    }

}
