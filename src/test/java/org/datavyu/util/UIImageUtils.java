/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.datavyu.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;


/**
 * Image utilities.
 * Example: capturing screenshot of component, comparing images.
 */
public final class UIImageUtils {

    /** Logger for this class */
    private static final Logger logger = LogManager.getLogger(UIImageUtils.class);

    /** Maximum distance one pixel can be away from another */
    public static final double MAX_PIXEL_DISTANCE = Math.sqrt((255 * 255) + (255 * 255) + (255 * 255));

    // Pixel thresholds
    private final static double PIXEL_THRESHOLD = 0.15; // Percent
    private final static double ERROR_THRESHOLD = 0.15;

    /**
     * Checks if two images are equal within 15%.
     *
     * @param uiImage image1
     * @param refFile referenceImageFile
     * @return true if similar enough
     * @throws IOException on error reading file
     */
    public static Boolean areImagesEqual(final BufferedImage uiImage, final File refFile) throws IOException {
        return areImagesEqual(uiImage, refFile, PIXEL_THRESHOLD, ERROR_THRESHOLD);
    }

    /**
     * Checks if two images are equal within given threshold
     *
     * @param uiImage      image1
     * @param refFile      referenceImageFile
     * @param pixThreshold pixel threshold
     * @param errThreshold final error threshold
     * @return true if similar enough
     * @throws IOException on error reading file
     */
    public static Boolean areImagesEqual(final BufferedImage uiImage, final File refFile, final double pixThreshold,
                                         final double errThreshold) throws IOException {
        final String tempFolder = System.getProperty("java.io.tmpdir");

        String filePrefix;

        int iStack = 1;

        do {
            filePrefix = new Exception().getStackTrace()[iStack].getMethodName();
            iStack++;
        } while ((filePrefix.equals("areImagesEqual")) && (iStack < 5));

        // Load image from file
        BufferedImage refImage = ImageIO.read(refFile);

        // Check that images are the same size
        if (!(uiImage.getHeight() == refImage.getHeight()) || !(uiImage.getWidth() == refImage.getWidth())) {
            ImageIO.write(uiImage, "png",
                    new File(tempFolder + "/" + filePrefix + "sameSize.png"));

            logger.info("Image written to: " + tempFolder + "/" + filePrefix + "sameSize.png");
        }

        Assert.assertEquals(uiImage.getHeight(), refImage.getHeight());
        Assert.assertEquals(uiImage.getWidth(), refImage.getWidth());

        int totalPixels = uiImage.getHeight() * uiImage.getWidth();

        // Number of pixels incorrect
        int errorPixels = 0;

        // For each pixel, calculate distance
        for (int iColumn = 0; iColumn < uiImage.getWidth(); iColumn++) {
            for (int iRow = 0; iRow < uiImage.getHeight(); iRow++) {
                Color col1 = new Color(uiImage.getRGB(iColumn, iRow));
                Color col2 = new Color(refImage.getRGB(iColumn, iRow));
                double pixelDistance = pixelDistance(col1, col2);
                // Check if correct within threshold
                if (pixelDistance > (pixThreshold * MAX_PIXEL_DISTANCE)) {
                    errorPixels++;
                }
            }
        }

        // Check if number of error pixels > threshold
        double error = (double) errorPixels / (double) totalPixels;
        boolean withinThreshold = error < errThreshold;
        logger.info("Error = " + error);

        if (!withinThreshold) {
            ImageIO.write(maskImage(uiImage, refImage), "png",
                    new File(tempFolder + "/" + filePrefix + "maskImage.png"));
            logger.info("Image written to: " + tempFolder + "/" + filePrefix + "maskImage.png");

            ImageIO.write(uiImage, "png",
                    new File(tempFolder + "/" + filePrefix + "capturedImage.png"));
            logger.info("Image written to: " + tempFolder + "/" + filePrefix + "capturedImage.png");
        }

        return withinThreshold;
    }

    /**
     * Calculates the distance between two colors.
     *
     * @param col1 first color
     * @param col2 second color
     * @return distance between colors
     */
    private static double pixelDistance(final Color col1, final Color col2) {
        int r1 = col1.getRed();
        int g1 = col1.getGreen();
        int b1 = col1.getBlue();
        int r2 = col2.getRed();
        int g2 = col2.getGreen();
        int b2 = col2.getBlue();
        return Math.sqrt(((r1 - r2) * (r1 - r2)) + ((g1 - g2) * (g1 - g2)) + ((b1 - b2) * (b1 - b2)));
    }

    /**
     * Captures screenshot of component and saves to a file
     *
     * @param component JComponent to capture screenshot
     * @param saveAs    file name
     */
    public static void captureAsScreenshot(final JComponent component, final File saveAs) {
        try {
            Robot robot = new Robot();
            // Create Rectangle around component
            Point locOnScreen = component.getLocationOnScreen();
            Rectangle bounds = component.getBounds();
            bounds.setLocation(locOnScreen);
            ImageIO.write(robot.createScreenCapture(bounds), "png", saveAs);
        } catch (AWTException e) {
            logger.error("Screen capture failed. Error: ", e);
        } catch (IOException e) {
            logger.error("Screen capture failed due to IO. Error: ", e);
        }
    }

    /**
     * Captures screenshot of component and saves to a file.
     *
     * @param frame  JComponent to capture screenshot
     * @param saveAs file name to be saved
     */
    public static void captureAsScreenshot(final Frame frame, final File saveAs) {
        try {
            Robot robot = new Robot();
            ImageIO.write(robot.createScreenCapture(getInternalRectangle(frame)), "png", saveAs);
        } catch (AWTException e) {
            logger.error("Screen capture failed. Error: ", e);
        } catch (IOException e) {
            logger.error("Screen capture failed due to IO. Error: ", e);
        }
    }

    /**
     * Captures screenshot of component and saves to a file.
     *
     * @param dialog JComponent to capture screenshot
     * @param saveAs file name
     */
    public static void captureAsScreenshot(final Dialog dialog, final File saveAs) {
        try {
            Robot robot = new Robot();
            ImageIO.write(robot.createScreenCapture(getInternalRectangle(dialog)), "png", saveAs);
        } catch (AWTException e) {
            logger.error("Screen capture failed. Error: ", e);
        } catch (IOException e) {
            logger.error("Screen capture failed due to IO. Error: ", e);
        }
    }

    /**
     * Captures screenshot of component and returns bufferedImage.
     *
     * @param dialog JComponent to capture screenshot
     * @return BufferedImage of screenshot
     */
    public static BufferedImage captureAsScreenshot(final Dialog dialog) {
        BufferedImage bufferedImage = null;
        try {
            Robot robot = new Robot();
            bufferedImage = robot.createScreenCapture(getInternalRectangle(dialog));
        } catch (AWTException e) {
            logger.error("Screen capture failed. Error: ", e);
        }
        return bufferedImage;
    }

    /**
     * Returns a rectangle of the inside of the frame i.e. excluding borders.
     *
     * @param frame frame to get rectangle from
     * @return frame Rectangle without borders
     */
    public static Rectangle getInternalRectangle(final Frame frame) {

        // Create Rectangle around component
        Point locOnScreen = frame.getLocationOnScreen();
        Rectangle bounds = frame.getBounds();

        // Compensate for frame boundary
        locOnScreen.setLocation(locOnScreen.x + frame.getInsets().left,
                locOnScreen.y + frame.getInsets().top);
        bounds.setRect(0, 0,
                bounds.getWidth() - frame.getInsets().left - frame.getInsets().right,
                bounds.getHeight() - frame.getInsets().top - frame.getInsets().bottom);

        bounds.setLocation(locOnScreen);

        return bounds;
    }

    /**
     * Returns a rectangle of the inside of the dialog i.e. excluding borders.
     *
     * @param dialog dialog to get rectangle from
     * @return dialog Rectangle without borders
     */
    public static Rectangle getInternalRectangle(final Dialog dialog) {

        // Create Rectangle around component
        Point locOnScreen = dialog.getLocationOnScreen();
        Rectangle bounds = dialog.getBounds();

        // Compensate for frame boundary
        locOnScreen.setLocation(locOnScreen.x + dialog.getInsets().left,
                locOnScreen.y + dialog.getInsets().top);
        bounds.setRect(0, 0,
                bounds.getWidth() - dialog.getInsets().left
                        - dialog.getInsets().right,
                bounds.getHeight() - dialog.getInsets().top
                        - dialog.getInsets().bottom);

        bounds.setLocation(locOnScreen);

        return bounds;
    }

    /**
     * Captures screenshot of component and returns as BufferedImage.
     *
     * @param component JComponent to capture screenshot
     * @return BufferedImage screenshot of component
     */
    public static BufferedImage captureAsScreenshot(final Component component) {
        BufferedImage bufferedImage = null;
        try {
            Robot robot = new Robot();
            // Create Rectangle around component
            Point locationOnScreen = component.getLocationOnScreen();
            Rectangle bounds = component.getBounds();
            bounds.setLocation(locationOnScreen);
            bufferedImage = robot.createScreenCapture(bounds);
        } catch (AWTException e) {
            logger.error("Screen capture failed. Error: ", e);
        }
        return bufferedImage;
    }

    /**
     * Captures screenshot of component and returns as BufferedImage.
     *
     * @param frame JComponent to capture screenshot
     * @return BufferedImage screenshot of component
     */
    public static BufferedImage captureAsScreenshot(final Frame frame) {
        BufferedImage bufferedImage = null;
        try {
            Robot robot = new Robot();
            bufferedImage = robot.createScreenCapture(getInternalRectangle(frame));
        } catch (AWTException e) {
            logger.error("Screen capture failed. Error: ", e);
        }
        return bufferedImage;
    }

    /**
     * Masks second image over the first.
     * From: http://stackoverflow.com/questions/221830/...
     * ...set-bufferedimage-alpha-mask-in-java
     * If images are not the same size, asserts false.
     *
     * @param img1 an image to mask
     * @param img2 a mask image to lay over first
     * @return masked image
     */
    public static BufferedImage maskImage(final BufferedImage img1, final BufferedImage img2) {

        // Assert false if images are not an equal size.
        Assert.assertEquals(img1.getHeight(), img2.getHeight());
        Assert.assertEquals(img1.getWidth(), img2.getWidth());

        BufferedImage result = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Color i1Color, i2Color;

        for (int iColumn = 0; iColumn < img1.getWidth(); iColumn++) {
            for (int iRow = 0; iRow < img1.getHeight(); iRow++) {
                i1Color = new Color(img1.getRGB(iColumn, iRow));
                i2Color = new Color(img2.getRGB(iColumn, iRow));
                // Get normalized difference
                double normDistance = pixelDistance(i1Color, i2Color) / MAX_PIXEL_DISTANCE * 255;
                // Mask away any alpha
                int color = i1Color.getRGB() & 0x00FFFFFF;
                // shift blue (normed pixelDistance) into alpha bits
                int mask = (int) normDistance << 24;
                color |= mask;
                result.setRGB(iColumn, iRow, color);
            }
        }
        return result;
    }

    /**
     * Subtracts 2 images.
     * If images are not the same size, asserts false.
     *
     * @param img1 an image to subtract from.
     * @param img2 an image to subtract.
     * @return difference image
     */
    public static BufferedImage subtractImage(final BufferedImage img1, final BufferedImage img2) {
        // Assert false if images are not an equal size.
        Assert.assertEquals(img1.getHeight(), img2.getHeight());
        Assert.assertEquals(img1.getWidth(), img2.getWidth());

        BufferedImage result = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_RGB);
        int i1Color, i2Color;

        for (int iColumn = 0; iColumn < img1.getWidth(); iColumn++) {
            for (int iRow = 0; iRow < img1.getHeight(); iRow++) {
                i1Color = img1.getRGB(iColumn, iRow);
                i2Color = img2.getRGB(iColumn, iRow);
                result.setRGB(iColumn, iRow, subtractColors(i1Color, i2Color));
            }
        }
        return result;
    }

    /**
     * Subtracts 2 images.
     * If images are not the same size, asserts false.
     *
     * @param img1 an image to subtract from.
     * @param img2 an image to subtract.
     * @return difference image
     */
    public static BufferedImage subtractImage(final RenderedImage img1, final RenderedImage img2) {
        return subtractImage(convertRenderedImage(img1), convertRenderedImage(img2));
    }

    /**
     * Finds the integer difference between two colors, as int.
     *
     * @param rgb1 color1
     * @param rgb2 color2
     * @return int difference between rgb1 and rgb2
     */
    private static int subtractColors(final int rgb1, final int rgb2) {
        Color color1 = new Color(rgb1);
        Color color2 = new Color(rgb2);
        int red = subtractColor(color1.getRed(), color2.getRed());
        int green = subtractColor(color1.getGreen(), color2.getGreen());
        int blue = subtractColor(color1.getBlue(), color2.getBlue());
        return new Color(red, green, blue).getRGB();
    }

    /**
     * Finds the integer difference between two colors, as int.
     *
     * @param color1 color1
     * @param color2 color2
     * @return int difference between rgb1 and rgb2
     */
    private static int subtractColor(final int color1, final int color2) {
        return color1 >= color2 ? (color1 - color2) : (color1 - color2 + 0Xff);
    }

    /**
     * Converts RenderedImage to BufferedImage.
     *
     * @param image RenderedImage
     * @return BufferedImage conversion of RenderedImage image
     */
    private static BufferedImage convertRenderedImage(final RenderedImage image) {

        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        ColorModel colorModel = image.getColorModel();
        int width = image.getWidth();
        int height = image.getHeight();
        WritableRaster raster = colorModel.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = image.getPropertyNames();

        if (keys != null) {
            for (String key : keys) {
                properties.put(key, image.getProperty(key));
            }
        }

        BufferedImage result = new BufferedImage(colorModel, raster,
                isAlphaPremultiplied, properties);
        image.copyData(raster);

        return result;
    }
}
