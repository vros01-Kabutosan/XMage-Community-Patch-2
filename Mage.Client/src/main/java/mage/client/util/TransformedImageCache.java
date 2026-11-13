package mage.client.util;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 *
 * @author user
 */
public final class TransformedImageCache {

    private static final class Key {

        final int width;
        final int height;
        final double angle;

        public Key(int width, int height, double angle) {
            this.width = width;
            this.height = height;
            this.angle = angle;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + this.width;
            hash = 53 * hash + this.height;
            hash = 53 * hash + (int) (Double.doubleToLongBits(this.angle) ^ (Double.doubleToLongBits(this.angle) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;
            if (this.width != other.width) {
                return false;
            }
            if (this.height != other.height) {
                return false;
            }
            if (Double.doubleToLongBits(this.angle) != Double.doubleToLongBits(other.angle)) {
                return false;
            }
            return true;
        }
    }

    private static final SoftValuesLoadingCache<Key, SoftValuesLoadingCache<BufferedImage, BufferedImage>> TRANSFORMED_IMAGES_CACHE;

    static {
        TRANSFORMED_IMAGES_CACHE = ImageCaches.register(SoftValuesLoadingCache.from(TransformedImageCache::createTransformedImageCache));
    }

    private static SoftValuesLoadingCache<BufferedImage, BufferedImage> createTransformedImageCache(Key key) {
        return SoftValuesLoadingCache.from(image -> {
            if (key.width != image.getWidth() || key.height != image.getHeight()) {
                image = resizeImage(image, key.width, key.height);
            }
            if (key.angle != 0.0) {
                image = rotateImage(image, key.angle);
            }
            return image;           
        });
    }

    private static BufferedImage rotateImage(BufferedImage image, double angle) {
        double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
        int width = image.getWidth(), height = image.getHeight();
        int newWidth = (int) Math.floor(width * cos + height * sin), newHeight = (int) Math.floor(height * cos + width * sin);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();

        BufferedImage result = gc.createCompatibleImage(newWidth, newHeight, Transparency.TRANSLUCENT);
        Graphics2D g2 = result.createGraphics();
        try {
            g2.translate((newWidth - width) / 2, (newHeight - height) / 2);
            g2.rotate(angle, width / 2, height / 2);
            g2.drawRenderedImage(image, null);
        } finally {
            g2.dispose();
        }

        return result;
    }

    private static BufferedImage resizeImage(BufferedImage original, int width, int height) {
        // v33: modern resampling instead of Image.getScaledInstance (old lazy AWT API with
        // mid quality whose work could land on EDT at first paint). Progressive halving for
        // big down-scales keeps detail without aliasing; the last pass uses bicubic.
        BufferedImage source = original;
        if (width < source.getWidth() && height < source.getHeight()) {
            while (source.getWidth() / 2 > width && source.getHeight() / 2 > height) {
                int passWidth = Math.max(source.getWidth() / 2, width);
                int passHeight = Math.max(source.getHeight() / 2, height);
                source = drawScaled(source, passWidth, passHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
        }
        return drawScaled(source, width, height, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static BufferedImage drawScaled(BufferedImage image, int width, int height, Object interpolation) {
        int imageType = image.getType();
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            imageType = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage output = new BufferedImage(width, height, imageType);
        Graphics2D g2 = output.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(image, 0, 0, width, height, null);
        } finally {
            g2.dispose();
        }
        return output;
    }

    public static BufferedImage getResizedImage(BufferedImage image, int width, int height) {
        return getRotatedResizedImage(image, width, height, 0.0);
    }

    public static BufferedImage getRotatedImage(BufferedImage image, double angle) {
        return getRotatedResizedImage(image, -1, -1, angle);
    }

    public static BufferedImage getRotatedResizedImage(BufferedImage image, int width, int height, double angle) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        if (angle == 0.0 && (width < 0 || imageWidth == width) && (height < 0 || imageHeight == height)) {
            return image;
        }

        int resWidth;
        int resHeight;
        if (width < 0 && height < 0) {
            resWidth = imageWidth;
            resHeight = imageHeight;
        } else if ((height < 0) || (width >= 0 && imageHeight * width <= imageWidth * height)) {
            resWidth = width;
            resHeight = imageHeight * width / imageWidth;
        } else {
            resWidth = imageWidth * height / imageHeight;
            resHeight = height;
        }

        if (angle == 0.0 && imageWidth == resWidth && imageHeight == resHeight) {
            return image;
        }
        if (resWidth < 3) {
            resWidth = 3;
        }
        if (resHeight < 3) {
            resHeight = 3;
        }
        return TRANSFORMED_IMAGES_CACHE.getOrThrow(new Key(resWidth, resHeight, angle)).getOrThrow(image);
    }
}
