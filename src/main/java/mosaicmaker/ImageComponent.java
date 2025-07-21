package mosaicmaker;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageComponent extends JComponent {
    private BufferedImage image;
    public BufferedImage getImage() { return image; }
    public void setImage(BufferedImage img) { image = img; }

    public ImageComponent(BufferedImage image){
        this.image = image;
        Rectangle bounds = new Rectangle(0,0,image.getWidth(), image.getHeight());
        setBounds(bounds);
    }

    public void crop(int x, int y, int w, int h) {
        Point origImageLocation = getLocation();
        double img_scale = getSize().getWidth() / image.getWidth();
        if ((x+w)/img_scale > image.getWidth()) {
            w = (int) (image.getWidth()*img_scale-x);
        }
        if ((y+h)/img_scale > image.getHeight()){
            h = (int) (image.getHeight()*img_scale -y);
        }

        BufferedImage cropped = image.getSubimage(
                Calc.divideAndRound(x, img_scale),
                Calc.divideAndRound(y, img_scale),
                Calc.divideAndRound(w, img_scale),
                Calc.divideAndRound(h, img_scale));
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(cropped, 0, 0, null);
        g2.dispose();

        // Replace image, but preserve scale
        setImage(copy);
        setBounds(new Rectangle(origImageLocation.x + x, origImageLocation.y + y, w, h));
    }

    public void setLocationFromScaledMove(Rectangle bounds, Point startLoc) {
        int dx = bounds.x - startLoc.x;
        int dy = bounds.y - startLoc.y;
        double scale = bounds.getWidth() / getWidth();
        int unscaledDx = Calc.divideAndRound(dx, scale);
        int unscaledDy = Calc.divideAndRound(dy, scale);
        Point newUnscaledLocation = new Point(getX() + unscaledDx, getY() + unscaledDy);
        setLocation(newUnscaledLocation);
    }
    public void resize(double scale){
        Rectangle unscaledBounds = getBounds();
        int resizedUnscaledWidth = Calc.multiplyAndRound(unscaledBounds.width, scale);
        int resizedUnscaledHeight = Calc.multiplyAndRound(unscaledBounds.height, scale);
        Dimension resizedUnscaledDim = new Dimension(resizedUnscaledWidth, resizedUnscaledHeight);
        setSize(resizedUnscaledDim);
    }
}
