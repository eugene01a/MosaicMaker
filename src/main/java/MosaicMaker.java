import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.datatransfer.DataFlavor;

public class MosaicMaker {
    private JFrame frame;
    private ScaledCanvas canvas;
    private JLabel coordLabel;
    private JPanel bottomBar;
    private JMenuBar topBar;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MosaicMaker().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("Mosaic Maker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(AppDefaults.FRAME_WIDTH, AppDefaults.FRAME_HEIGHT);
        addTopBar();
        addBottomBar();

        canvas = new ScaledCanvas();
        canvas.setLayout(null);

        frame.add(canvas, BorderLayout.CENTER);

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                coordLabel.setText("x: " + (int)(e.getX() / canvas.getScale()) + ", y: " + (int)(e.getY() / canvas.getScale()));
            }

            public void mouseDragged(MouseEvent e) {
                coordLabel.setText("x: " + (int)(e.getX() / canvas.getScale()) + ", y: " + (int)(e.getY() / canvas.getScale()));
            }
        });

        canvas.setTransferHandler(new TransferHandler() {
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    java.util.List<File> files = (java.util.List<File>)
                            support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        canvas.addImageToCanvas(file);
                    }
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        });

        frame.setVisible(true);
    }

    private void addTopBar() {
        topBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");

        JMenuItem addItem = new JMenuItem("Add Image");
        addItem.addActionListener(e -> selectImageToAdd());
        fileMenu.add(addItem);

        JMenuItem saveItem = new JMenuItem("Save As Image");
        saveItem.addActionListener(e -> saveCanvasAsImage());
        fileMenu.add(saveItem);

        JMenuItem deleteItem = new JMenuItem("Delete Image");
        deleteItem.addActionListener(e -> {
            ImageComponent selected = canvas.getSelectedImage();
            if (selected != null) {
                canvas.remove(selected);
                canvas.selectImage(null);
                canvas.repaint();
            }
        });
        deleteItem.setEnabled(false); // Initially disabled
        editMenu.add(deleteItem);

        JMenuItem splitHorizontally = new JMenuItem("Horizontal Split");
        splitHorizontally.addActionListener(e -> {
            ImageComponent selected = canvas.getSelectedImage();
            if (selected != null) {
                selected.enterHorizontalSplitMode();
            }
        });
        splitHorizontally.setEnabled(false);
        editMenu.add(splitHorizontally);

        JMenuItem splitVertically = new JMenuItem("Vertical Split");
        splitVertically.addActionListener(e -> {
            ImageComponent selected = canvas.getSelectedImage();
            if (selected != null) {
                selected.enterVerticalSplitMode();
            }
        });
        splitVertically.setEnabled(false);
        editMenu.add(splitVertically);

        JMenuItem cropItem = new JMenuItem("Crop Image");
        cropItem.addActionListener(e -> {
            ImageComponent selected = canvas.getSelectedImage();
            if (selected != null) {
                selected.enterCropMode();
            }
        });
        cropItem.setEnabled(false);
        editMenu.add(cropItem);

        JMenuItem zoomToFitItem = new JMenuItem("Zoom to Fit");
        zoomToFitItem.addActionListener(e -> zoomToFit());
        viewMenu.add(zoomToFitItem);

        topBar.add(fileMenu);
        topBar.add(editMenu);
        topBar.add(viewMenu);
        frame.setJMenuBar(topBar);
    }

    private void addBottomBar() {
        coordLabel = new JLabel("x: 0, y: 0");
        bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.add(coordLabel);
        frame.add(bottomBar, BorderLayout.SOUTH);
    }

    private void zoomToFit() {
        Rectangle contentBounds = canvas.getUnscaledImagesBounds();
        if (contentBounds.x != 0 || contentBounds.y !=0){
            canvas.shiftUnscaledContentBounds(new Point(-1*contentBounds.x, -1*contentBounds.y));
        }
        double wFrame = frame.getContentPane().getWidth();
        double hFrame = frame.getContentPane().getHeight() - topBar.getHeight() - bottomBar.getHeight();
        if (contentBounds.width == 0 || contentBounds.height == 0) return;
        double widthScale = wFrame / contentBounds.getWidth();
        double heightScale = hFrame / contentBounds.getHeight();
        double scale = Math.min(widthScale, heightScale);
        if (scale != 1.0) {
            canvas.setScale(scale);
            canvas.updateChildrenBounds();
        }
    }
    private void saveCanvasAsImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image");
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String path = fileToSave.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".png")) {
                fileToSave = new File(path + ".png");
            }
            try {
                BufferedImage output = canvas.createUnscaledMosaicImage();
                ImageIO.write(output, "png", fileToSave);
                JOptionPane.showMessageDialog(null, "Image saved to: " + fileToSave.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error saving image.");
            }
        }
    }
    private void selectImageToAdd() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Add Image");
        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            canvas.addImageToCanvas(fileToOpen);
        }
    }
}