import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Builds every raster brand asset from the sources in design/brand/: logo.png (transparent) and background.png (or .jpg).
 *
 * Run from the repository root with a bold Roboto font (Apache 2.0), for the name under the logo:
 *
 *   java tools/brand/MakeBrandAssets.java . /path/to/Roboto-Bold.ttf
 *
 * Writes the adaptive launcher icon and the Android TV banner into app/src/main/res, and the Play Store graphics into store/.
 */
public class MakeBrandAssets {
    private static final Color BACKGROUND = new Color(0x07, 0x0C, 0x18);
    private static final Color NAME_COLOR = new Color(0xE9, 0xEE, 0xF8);
    private static final String NAME = "TILFAZ";

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        Font font = Font.createFont(Font.TRUETYPE_FONT, new File(args[1]));
        BufferedImage logo = trim(ImageIO.read(new File(root, "design/brand/logo.png")));
        File backgroundFile = new File(root, "design/brand/background.png");
        if (!backgroundFile.exists()) backgroundFile = new File(root, "design/brand/background.jpg");
        BufferedImage background = ImageIO.read(backgroundFile);
        File res = new File(root, "app/src/main/res");

        // Adaptive icon: 108dp canvas at xxxhdpi, the visible part is the inner 72dp, so the logo stays within about two thirds.
        BufferedImage foreground = new BufferedImage(432, 432, BufferedImage.TYPE_INT_ARGB);
        drawFitted(foreground, logo, 0.60);
        write(foreground, new File(res, "drawable-nodpi/ic_launcher_foreground.png"));
        write(monochrome(foreground), new File(res, "drawable-nodpi/ic_launcher_monochrome.png"));

        // Play Store icon: full square, Play rounds it itself.
        BufferedImage icon = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        fill(icon, BACKGROUND);
        drawFitted(icon, logo, 0.66);
        write(icon, new File(root, "store/icon-512.png"));

        // Android TV banner: 16:9, shipped once without a density, the launcher scales it to its slot.
        write(banner(background, logo, font, 960, 540), new File(res, "drawable-nodpi/tv_banner.png"));
        write(banner(background, logo, font, 1280, 720), new File(root, "store/tv-banner-1280x720.png"));
        write(banner(background, logo, font, 1024, 500), new File(root, "store/feature-graphic-1024x500.png"));
    }

    /** The background cropped to the target's proportions around its centre, scaled, with the logo and the name on it. */
    private static BufferedImage banner(BufferedImage source, BufferedImage logo, Font font, int width, int height) {
        double ratio = (double) width / height;
        int cropW = source.getWidth();
        int cropH = (int) Math.round(cropW / ratio);
        if (cropH > source.getHeight()) {
            cropH = source.getHeight();
            cropW = (int) Math.round(cropH * ratio);
        }
        BufferedImage cropped = source.getSubimage((source.getWidth() - cropW) / 2, (source.getHeight() - cropH) / 2, cropW, cropH);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        scaleInto(out, cropped);

        // The logo sits in the calm centre of the background, the name under it.
        double logoHeight = height * 0.52;
        double logoScale = logoHeight / logo.getHeight();
        BufferedImage scaledLogo = new BufferedImage((int) Math.round(logo.getWidth() * logoScale), (int) Math.round(logoHeight), BufferedImage.TYPE_INT_ARGB);
        scaleInto(scaledLogo, logo);

        Graphics2D g = out.createGraphics();
        hints(g);
        g.drawImage(scaledLogo, (width - scaledLogo.getWidth()) / 2, (int) Math.round(height * 0.11), null);
        g.setFont(font.deriveFont(Font.BOLD, height * 0.115f));
        FontMetrics metrics = g.getFontMetrics();
        double tracking = height * 0.03;
        double total = -tracking;
        for (char c : NAME.toCharArray()) total += metrics.charWidth(c) + tracking;
        double x = (width - total) / 2;
        int baseline = (int) Math.round(height * 0.915);
        g.setColor(NAME_COLOR);
        for (char c : NAME.toCharArray()) {
            g.drawString(String.valueOf(c), (float) x, baseline);
            x += metrics.charWidth(c) + tracking;
        }
        g.dispose();
        return out;
    }

    /** White where the logo is bright (outline, eyes and smile), clear elsewhere: what a themed icon is tinted from. */
    private static BufferedImage monochrome(BufferedImage foreground) {
        BufferedImage out = new BufferedImage(foreground.getWidth(), foreground.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                int argb = foreground.getRGB(x, y);
                int alpha = argb >>> 24;
                double luma = 0.2126 * ((argb >> 16) & 0xFF) + 0.7152 * ((argb >> 8) & 0xFF) + 0.0722 * (argb & 0xFF);
                if (alpha > 0 && luma > 105) out.setRGB(x, y, (alpha << 24) | 0xFFFFFF);
            }
        }
        return out;
    }

    /** Draws [logo] centred, scaled so that its longer side is [fraction] of the canvas. */
    private static void drawFitted(BufferedImage canvas, BufferedImage logo, double fraction) {
        double scale = fraction * canvas.getWidth() / Math.max(logo.getWidth(), logo.getHeight());
        int w = (int) Math.round(logo.getWidth() * scale);
        int h = (int) Math.round(logo.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        scaleInto(scaled, logo);
        Graphics2D g = canvas.createGraphics();
        hints(g);
        g.drawImage(scaled, (canvas.getWidth() - w) / 2, (canvas.getHeight() - h) / 2, null);
        g.dispose();
    }

    /** Halves until close to the target size, then one bicubic step: a single big step blurs or aliases. */
    private static void scaleInto(BufferedImage target, BufferedImage source) {
        BufferedImage current = source;
        while (current.getWidth() / 2 >= target.getWidth() && current.getHeight() / 2 >= target.getHeight()) {
            BufferedImage half = new BufferedImage(current.getWidth() / 2, current.getHeight() / 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = half.createGraphics();
            hints(g);
            g.drawImage(current, 0, 0, half.getWidth(), half.getHeight(), null);
            g.dispose();
            current = half;
        }
        Graphics2D g = target.createGraphics();
        hints(g);
        g.drawImage(current, 0, 0, target.getWidth(), target.getHeight(), null);
        g.dispose();
    }

    /** Crops away the fully transparent border so the logo's size is the logo's, not its canvas's. */
    private static BufferedImage trim(BufferedImage image) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 8) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static void fill(BufferedImage image, Color color) {
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private static void write(BufferedImage image, File file) throws IOException {
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
        System.out.println(file.getPath() + "  " + image.getWidth() + "x" + image.getHeight() + "  " + file.length() / 1024 + " KB");
    }
}
