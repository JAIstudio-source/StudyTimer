import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Finds warm saturated clusters (crown/flame emoji orange) and gold bar tops per day column. */
public class CrownQA {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        int w = img.getWidth(), h = img.getHeight();

        int[] goldBox = box();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = img.getRGB(x, y);
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            if (Math.abs(r - 0xF6) <= 18 && Math.abs(g - 0xCB) <= 18 && Math.abs(b - 0x22) <= 18) grow(goldBox, x, y);
        }
        System.out.println("gold bars bbox: " + fmt(goldBox));

        // warm saturated (orange/red) pixels = emoji + red delta text; sat high, not grey
        List<int[]> clusters = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img.getRGB(x, y);
                int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
                int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
                if (mx - mn < 50) continue;
                float hue = hue(r, g, b, mx, mn);
                boolean warm = (hue < 45f || hue >= 330f) && mx > 140;
                if (!warm) continue;
                boolean found = false;
                for (int[] cl : clusters) {
                    if (x >= cl[0] - 30 && x <= cl[2] + 30 && y >= cl[1] - 30 && y <= cl[3] + 30) {
                        grow(cl, x, y); found = true; break;
                    }
                }
                if (!found) clusters.add(new int[]{x, y, x, y});
            }
        }
        clusters.sort((a, b2) -> a[1] - b2[1]);
        for (int[] cl : clusters) {
            int cw = cl[2] - cl[0] + 1, ch = cl[3] - cl[1] + 1;
            if (cw >= 5 && ch >= 5) System.out.println("warm cluster: x" + cl[0] + "-" + cl[2] + " y" + cl[1] + "-" + cl[3] + " (w" + cw + " h" + ch + ")");
        }
    }

    static float hue(int r, int g, int b, int mx, int mn) {
        float d = mx - mn;
        if (d == 0) return 0;
        float h;
        if (mx == r) h = ((g - b) / d) % 6f;
        else if (mx == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        return h * 60f < 0 ? h * 60f + 360 : h * 60f;
    }

    static int[] box() { return new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1}; }
    static void grow(int[] bx, int x, int y) {
        if (x < bx[0]) bx[0] = x;
        if (y < bx[1]) bx[1] = y;
        if (x > bx[2]) bx[2] = x;
        if (y > bx[3]) bx[3] = y;
    }
    static String fmt(int[] bx) { return bx[2] < 0 ? "-" : "x" + bx[0] + "-" + bx[2] + " y" + bx[1] + "-" + bx[3]; }
}
