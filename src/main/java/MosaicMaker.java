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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MosaicMaker().createAndShowGUI());
    }


    private void createAndShowGUI() {
        frame = new JFrame("Mosaic Maker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(AppDefaults.FRAME_WIDTH, AppDefaults.FRAME_HEIGHT);
        addTopBar(frame);
        addBottomBar(frame);

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

    private void addTopBar(JFrame frame){
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu viewMenu = new JMenu("View");

        JMenuItem addItem = new JMenuItem("Add Image");
        addItem.addActionListener(e -> selectImageToAdd());
        fileMenu.add(addItem);

        JMenuItem saveItem = new JMenuItem("Save As Image");
        saveItem.addActionListener(e -> saveCanvasAsImage());
        fileMenu.add(saveItem);

        JMenuItem zoomToFitItem = new JMenuItem("Zoom to Fit");
        zoomToFitItem.addActionListener(e -> zoomToFit());
        viewMenu.add(zoomToFitItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        frame.setJMenuBar(menuBar);

    }
    private void addBottomBar(JFrame frame){
        coordLabel = new JLabel("x: 0, y: 0");
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(coordLabel);
        frame.add(bottomPanel, BorderLayout.SOUTH);
    }

private void zoomToFit() {
        Rectangle contentBounds = canvas.getImagesBounds();

        double wFrame = frame.getContentPane().getWidth();
        double hFrame = frame.getContentPane().getHeight();

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
        Rectangle bounds = canvas.getImagesBounds();
        if (bounds.width == 0 || bounds.height == 0) {
            JOptionPane.showMessageDialog(null, "Nothing to save.");
            return;
        }

        BufferedImage output = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = output.createGraphics();
        g2d.translate(-bounds.x, -bounds.y);
        double oldScale = canvas.getScale();
        canvas.setScale(1.0); // Avoid scaling in image export
        canvas.paintAll(g2d);
        canvas.setScale(oldScale); // Restore
        g2d.dispose();

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
