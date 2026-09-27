import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the Google Play Store assets for SpiMp3:
 *   - 512x512  play-icon.png
 *   - 1024x500 feature-graphic.png
 *   - phone screenshots cropped to the 9:16 ratio Play requires
 *
 * Run:  java tools/StoreArtwork.java <shotsDir> <outDir>
 */
public class StoreArtwork {

    static final Color BG = new Color(0x0B, 0x0B, 0x16);
    static final Color BG_SOFT = new Color(0x14, 0x14, 0x24);
    static final Color ACCENT = new Color(0x22, 0xC5, 0x5E);
    static final Color ACCENT_2 = new Color(0x4A, 0xDE, 0x80);
    static final Color TEXT = new Color(0xF8, 0xFA, 0xFC);
    static final Color MUTED = new Color(0x94, 0xA3, 0xB8);

    public static void main(String[] args) throws Exception {
        String shotsDir = args.length > 0 ? args[0] : "shots";
        String outDir = args.length > 1 ? args[1] : "store";
        new File(outDir).mkdirs();

        icon(outDir + "/play-icon.png");
        featureGraphic(outDir + "/feature-graphic.png");
        List<String> shots = screenshots(shotsDir, outDir);
        System.out.println("Generated assets in " + outDir + " (" + (shots.size() + 2) + " files)");
    }

    // ---------------- icon ----------------
    static void icon(String path) throws Exception {
        int s = 512;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(0x12, 0x12, 0x22), 0, s, BG));
        g.fillRect(0, 0, s, s);

        // subtle accent glow
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(s * 0.3f, s * 0.28f), s * 0.55f,
                new float[]{0f, 1f},
                new Color[]{new Color(0x22, 0xC5, 0x5E, 46), new Color(0x22, 0xC5, 0x5E, 0)}));
        g.fillRect(0, 0, s, s);

        // waveform + play glyph, centred
        int cx = s / 2, cy = s / 2;
        float barW = s * 0.052f;
        int[] heights = {90, 150, 210};
        int gap = (int) (s * 0.085);
        int totalW = heights.length * (int) barW + (heights.length - 1) * gap;
        int x = cx - totalW / 2;
        for (int i = 0; i < heights.length; i++) {
            int h = (int) (s * heights[i] / 260.0);
            g.setPaint(i == 1 ? ACCENT_2 : ACCENT);
            g.fillRoundRect(x, cy - h / 2, (int) barW, h, (int) barW, (int) barW);
            x += (int) barW + gap;
        }
        // play triangle
        int t = (int) (s * 0.085);
        g.setPaint(ACCENT_2);
        int tx = cx + totalW / 2 + (int) (s * 0.07);
        g.fillPolygon(new int[]{tx, tx, tx + (int) (t * 0.85)}, new int[]{cy - t / 2, cy + t / 2, cy}, 3);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
    }

    // ---------------- feature graphic 1024x500 ----------------
    static void featureGraphic(String path) throws Exception {
        int w = 1024, h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(0x0F, 0x0F, 0x1F), w, h, BG));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(w * 0.78f, h * 0.25f), h * 0.95f,
                new float[]{0f, 1f},
                new Color[]{new Color(0x22, 0xC5, 0x5E, 40), new Color(0x22, 0xC5, 0x5E, 0)}));
        g.fillRect(0, 0, w, h);

        Font title = loadFont(86f, Font.BOLD);
        Font sub = loadFont(30f, Font.PLAIN);
        Font tag = loadFont(24f, Font.PLAIN);

        g.setColor(TEXT);
        g.setFont(title);
        g.drawString("SpiMp3", 72, 232);

        g.setColor(ACCENT_2);
        g.setFont(tag);
        g.drawString("OFFLINE MUSIC PLAYER", 76, 276);

        g.setColor(MUTED);
        g.setFont(sub);
        g.drawString("Your music never leaves your device.", 76, 330);
        g.drawString("No accounts. No tracking. No internet permission.", 76, 372);

        // waveform mark on the right
        g.setColor(ACCENT);
        int bx = w - 330, by = h / 2;
        int[] hs = {60, 110, 170, 110, 60};
        for (int i = 0; i < hs.length; i++) {
            int bh = hs[i];
            g.fillRoundRect(bx + i * 42, by - bh / 2, 18, bh, 18, 18);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(path));
    }

    // ---------------- screenshots -> compliant 9:16 ----------------
    /**
     * Device captures are 1080x2460 (9:19.5). Play wants 9:16, so instead of a
     * blind crop (which would cut the app header or the bottom bar) we compose
     * the capture onto a branded 1080x1920 canvas.
     */
    static List<String> screenshots(String shotsDir, String outDir) throws Exception {
        File dir = new File(shotsDir);
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
        if (files == null) return new ArrayList<>();
        java.util.Arrays.sort(files);
        List<String> out = new ArrayList<>();
        int index = 1;
        for (File f : files) {
            if (!f.getName().startsWith("store-")) continue;
            BufferedImage src = ImageIO.read(f);
            if (src == null) continue;

            int W = 1080, H = 1920;
            BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g.setPaint(new GradientPaint(0, 0, new Color(0x0F, 0x0F, 0x1F), W, H, new Color(0x0A, 0x0A, 0x12)));
            g.fillRect(0, 0, W, H);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(W * 0.5f, H * 0.32f), H * 0.55f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(0x22, 0xC5, 0x5E, 26), new Color(0x22, 0xC5, 0x5E, 0)}));
            g.fillRect(0, 0, W, H);

            // fit the capture with a small margin
            double margin = 0.035;
            double scale = Math.min(
                    W * (1 - 2 * margin) / src.getWidth(),
                    H * (1 - 2 * margin) / src.getHeight());
            int dw = (int) (src.getWidth() * scale);
            int dh = (int) (src.getHeight() * scale);
            int dx = (W - dw) / 2;
            int dy = (H - dh) / 2;

            // soft shadow + rounded phone frame
            g.setColor(new Color(0, 0, 0, 130));
            g.fillRoundRect(dx - 10, dy - 10, dw + 20, dh + 20, 46, 46);
            g.setClip(new RoundRectangle2D.Float(dx, dy, dw, dh, 36, 36));
            g.drawImage(src, dx, dy, dw, dh, null);
            g.setClip(null);
            g.setColor(new Color(0xFF, 0xFF, 0xFF, 26));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(dx, dy, dw, dh, 36, 36);

            g.dispose();
            String name = String.format("screenshot-%02d.png", index++);
            ImageIO.write(canvas, "png", new File(outDir + "/" + name));
            out.add(name);
        }
        return out;
    }

    // ---------------- font ----------------
    static Font loadFont(float size, int style) throws Exception {
        File[] candidates = {
            new File("app/src/main/res/font/poppins_bold.ttf"),
            new File("app/src/main/res/font/poppins_medium.ttf"),
        };
        for (File c : candidates) {
            if (!c.exists()) continue;
            try {
                return Font.createFont(Font.TRUETYPE_FONT, c).deriveFont(style, size);
            } catch (Exception ignored) {
                // font file unreadable here (e.g. encoding) — fall back below
            }
        }
        return new Font("SansSerif", style, (int) size);
    }
}
