import javax.swing.*;
import java.awt.*;

public class MagicTilesGame {
    public static int W = 1366, H = 768;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Magic Tiles Indonesia");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //  frame.setUndecorated(true); // fullscreen borderless

            // Set ukuran layar penuh
        //  GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        //  DisplayMode dm = gd.getDisplayMode();
        //  W = dm.getWidth();
        //  H = dm.getHeight();

            MenuScreen menu = new MenuScreen(frame);
            frame.add(menu);
            frame.setSize(W, H);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // ESC untuk keluar fullscreen tiap screen
        });
    }
}
