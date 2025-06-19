import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ScaledCanvasKeyAdapter extends KeyAdapter {
    private final ScaledCanvas canvas;

    public ScaledCanvasKeyAdapter(ScaledCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            ScaledComponent selected = canvas.getSelectedComponent();
            if (selected != null) {
                canvas.remove(selected);
            }
        }
    }
}
