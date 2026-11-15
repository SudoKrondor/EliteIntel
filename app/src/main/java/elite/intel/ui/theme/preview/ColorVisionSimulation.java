package elite.intel.ui.theme.preview;

import java.awt.image.BufferedImage;

/**
 * How a picture looks to someone with a colour vision deficiency, for the Color Themes tab's "View as" switch: a
 * commander can check a theme before sharing it, the same check that tuned the Default theme.
 * <p>
 * The Machado, Oliveira and Fernandes (2009) model at full severity, applied in linear light. It is an
 * approximation of how a type of colour blindness shifts colours, not a diagnosis, and the same for everyone with
 * that type.
 */
public enum ColorVisionSimulation {
    NORMAL(null),
    DEUTERANOPIA(new double[][]{
            {0.367322, 0.860646, -0.227968},
            {0.280085, 0.672501, 0.047413},
            {-0.011820, 0.042940, 0.968881}}),
    PROTANOPIA(new double[][]{
            {0.152286, 1.052583, -0.204868},
            {0.114503, 0.786281, 0.099216},
            {-0.003882, -0.048116, 1.051998}}),
    TRITANOPIA(new double[][]{
            {1.255528, -0.076749, -0.178779},
            {-0.078411, 0.930809, 0.147602},
            {0.004733, 0.691367, 0.303900}});

    private static final double[] TO_LINEAR = toLinearTable();
    private static final int ENCODE_STEPS = 4096;
    private static final int[] TO_SRGB = toSrgbTable();

    private final double[][] matrix;

    ColorVisionSimulation(double[][] matrix) {
        this.matrix = matrix;
    }

    /** The picture as this kind of vision sees it. {@link #NORMAL} returns the picture itself. */
    public BufferedImage apply(BufferedImage image) {
        if (matrix == null) return image;
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                out.setRGB(x, y, apply(image.getRGB(x, y)));
            }
        }
        return out;
    }

    /** One colour, {@code 0xRRGGBB}, as this kind of vision sees it. */
    public int apply(int rgb) {
        if (matrix == null) return rgb & 0xFFFFFF;
        double r = TO_LINEAR[rgb >> 16 & 0xFF];
        double g = TO_LINEAR[rgb >> 8 & 0xFF];
        double b = TO_LINEAR[rgb & 0xFF];
        return encode(matrix[0], r, g, b) << 16 | encode(matrix[1], r, g, b) << 8 | encode(matrix[2], r, g, b);
    }

    private static int encode(double[] row, double r, double g, double b) {
        double linear = Math.clamp(row[0] * r + row[1] * g + row[2] * b, 0.0, 1.0);
        return TO_SRGB[(int) Math.round(linear * (ENCODE_STEPS - 1))];
    }

    private static double[] toLinearTable() {
        double[] table = new double[256];
        for (int i = 0; i < table.length; i++) {
            double c = i / 255.0;
            table[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return table;
    }

    /**
     * WHY a table of 4096 steps: linear light crowds the dark end, where 256 steps would band the near-black
     * backgrounds the app is drawn on.
     */
    private static int[] toSrgbTable() {
        int[] table = new int[ENCODE_STEPS];
        for (int i = 0; i < table.length; i++) {
            double c = i / (double) (ENCODE_STEPS - 1);
            double encoded = c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
            table[i] = (int) Math.round(encoded * 255);
        }
        return table;
    }
}
