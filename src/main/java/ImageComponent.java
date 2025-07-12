import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageComponent extends JComponent {
    protected BufferedImage image;
    public BufferedImage getImage() { return image; }
    public void setImage(BufferedImage img) { image = img; }
    protected Rectangle imageBounds;
    public Rectangle getImageBounds(){
        return imageBounds;
    }
    public Point getImageLocation(){ return imageBounds.getLocation(); }
    public Dimension getImageDimension(){ return imageBounds.getSize(); }
    public void setImageDimension(Dimension d){ imageBounds.setSize(d); }
    public void setImageBounds(Rectangle b){ imageBounds = b; }
    public void setImageLocation(Point p){ imageBounds.setLocation(p); }

    public ImageComponent(BufferedImage image){
        this.image = image;
        Rectangle bounds = new Rectangle(0,0,image.getWidth(), image.getHeight());
        setImageBounds(bounds);
    }

    public void setUnscaledLocationFromScaledMove(Point scaledStart, Point scaledEnd) {
        int dx = scaledEnd.x - scaledStart.x;
        int dy = scaledEnd.y - scaledStart.y;

        double scaleX = (double) getWidth() / getImageDimension().width;
        double scaleY = (double) getHeight() / getImageDimension().height;

        int unscaledDx = (int) Math.round(dx / scaleX);
        int unscaledDy = (int) Math.round(dy / scaleY);
        Point newUnscaledLocation = new Point(imageBounds.x + unscaledDx, imageBounds.y + unscaledDy);
        setImageLocation(newUnscaledLocation);
    }

}
