import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Objective QA for the weekly summary card screenshot.
 * Reads a PNG and reports: pure-black %, presence/bbox of the gold palette,
 * violet break color, white text, and a hue histogram for unexpected colors.
 *
 * Usage: java CardQA <image.png>
 */
public class CardQA {
    static int TOL = 22;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("usage: java CardQA <image.png>"); return; }
        BufferedImage img = ImageIO.read(new File(args[0]));
        int w = img.getWidth(), h = img.getHeight();
        System.out.println("image: " + w + "x" + h);

        long black = 0, gold = 0, goldLight = 0, violet = 0, amber = 0, white = 0, total = (long) w * h;
        int[] goldBox = box(); int[] goldLightBox = box(); int[] violetBox = box();
        int[] amberBox = box(); int[] whiteBox = box();
        int[] hueHist = new int[12];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img.getRGB(x, y);
                int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
                if (r == 0 && g == 0 && b == 0) { black++; continue; }
                if (near(r, g, b, 0xF6, 0xCB, 0x22)) { gold++; grow(goldBox, x, y); continue; }
                if (near(r, g, b, 0xFD, 0xE6, 0x8A)) { goldLight++; grow(goldLightBox, x, y); continue; }
                if (near(r, g, b, 0x8B, 0x5C, 0xF6)) { violet++; grow(violetBox, x, y); continue; }
                if (near(r, g, b, 0xFB, 0xBF, 0x24)) { amber++; grow(amberBox, x, y); continue; }
                if (r > 235 && g > 235 && b > 235) { white++; grow(whiteBox, x, y); continue; }
                int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
                if (mx - mn > 40) {
                    float hue = hue(r, g, b, mx, mn);
                    hueHist[(int) (hue / 30f) % 12]++;
                }
            }
        }

        System.out.printf("pure black: %.1f%%%n", 100.0 * black / total);
        System.out.printf("gold    #F6CB22 : %d px  box %s%n", gold, fmt(goldBox));
        System.out.printf("goldLt  #FDE68A : %d px  box %s%n", goldLight, fmt(goldLightBox));
        System.out.printf("violet  #8B5CF6 : %d px  box %s%n", violet, fmt(violetBox));
        System.out.printf("amber   #FBBF24 : %d px  box %s%n", amber, fmt(amberBox));
        System.out.printf("white (>235)    : %d px  box %s%n", white, fmt(whiteBox));
        System.out.print("hue histogram [0-30..330 deg], saturated px: ");
        for (int i = 0; i < 12; i++) System.out.print(hueHist[i] + " ");
        System.out.println();
    }

    static boolean near(int r, int g, int b, int tr, int tg, int tb) {
        return Math.abs(r - tr) <= TOL && Math.abs(g - tg) <= TOL && Math.abs(b - tb) <= TOL;
    }

    static float hue(int r, int g, int b, int mx, int mn) {
        float d = mx - mn;
        float h;
        if (d == 0) return 0;
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

    static String fmt(int[] bx) {
        if (bx[2] < 0) return "-";
        return "x" + bx[0] + "-" + bx[2] + " y" + bx[1] + "-" + bx[3] + " (w" + (bx[2] - bx[0] + 1) + " h" + (bx[3] - bx[1] + 1) + ")";
    }
}
