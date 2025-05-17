import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.datatransfer.DataFlavor;

public class MosaicMaker {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MosaicMaker().createAndShowGUI());
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Mosaic Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        
        JLayeredPane canvas = new JLayeredPane();
        canvas.setPreferredSize(new Dimension(1000, 650));
        canvas.setLayout(null);

        JLabel coordLabel = new JLabel("x: 0, y: 0");
        coordLabel.setOpaque(true);
        coordLabel.setBackground(Color.WHITE);
        coordLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(coordLabel);

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(canvas), BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Update mouse coordinates
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                coordLabel.setText("x: " + e.getX() + ", y: " + e.getY());
            }

            public void mouseDragged(MouseEvent e) {
                coordLabel.setText("x: " + e.getX() + ", y: " + e.getY());
            }
        });

        // Enable drag-and-drop of image files
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
                        BufferedImage img = ImageIO.read(file);
                        if (img != null) {
                            ImageComponent ic = new ImageComponent(img);
                            ic.setBounds(50, 50, img.getWidth() / 4, img.getHeight() / 4);
                            canvas.add(ic, JLayeredPane.DEFAULT_LAYER);
                            canvas.repaint();
                        }
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
}
