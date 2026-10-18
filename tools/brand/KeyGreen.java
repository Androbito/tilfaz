import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Turns the logo drawn on a flat green background into a transparent PNG.
 *
 *   java tools/brand/KeyGreen.java design/brand/logo-source-green.jpeg design/brand/logo.png
 *
 * A pixel is "green" by how far its green exceeds its red and blue. Fully green pixels become clear, fully non-green ones stay
 * opaque, and the soft edge in between is un-mixed from the green so no green halo is left on the glow.
 */
public class KeyGreen {
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        int w = src.getWidth(), h = src.getHeight();
        double[] bg = cornerAverage(src);
        double bgDominance = bg[1] - Math.max(bg[0], bg[2]);
        double opaqueBelow = 0.25 * bgDominance;
        double clearAbove = 0.85 * bgDominance;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                double r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                double dominance = g - Math.max(r, b);
                double alpha = Math.max(0, Math.min(1, (clearAbove - dominance) / (clearAbove - opaqueBelow)));
                if (alpha <= 0) continue;
                if (alpha < 1) {
                    r = clamp((r - (1 - alpha) * bg[0]) / alpha);
                    g = clamp((g - (1 - alpha) * bg[1]) / alpha);
                    b = clamp((b - (1 - alpha) * bg[2]) / alpha);
                }
                g = Math.min(g, Math.max(r, b) + 4); // whatever green is left over
                out.setRGB(x, y, ((int) Math.round(alpha * 255) << 24) | ((int) r << 16) | ((int) g << 8) | (int) b);
            }
        }
        ImageIO.write(out, "png", new File(args[1]));
        System.out.println("background " + (int) bg[0] + "," + (int) bg[1] + "," + (int) bg[2] + " -> " + args[1] + " " + w + "x" + h);
    }

    private static double[] cornerAverage(BufferedImage image) {
        int n = 24, w = image.getWidth(), h = image.getHeight();
        double[] sum = new double[3];
        int[][] origins = {{0, 0}, {w - n, 0}, {0, h - n}, {w - n, h - n}};
        for (int[] o : origins) {
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    int argb = image.getRGB(o[0] + x, o[1] + y);
                    sum[0] += (argb >> 16) & 0xFF;
                    sum[1] += (argb >> 8) & 0xFF;
                    sum[2] += argb & 0xFF;
                }
            }
        }
        for (int i = 0; i < 3; i++) sum[i] /= 4.0 * n * n;
        return sum;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(255, v));
    }
}
