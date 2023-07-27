package cn.altuma.hdcodedocode.binarizer;

/**
 * 混合二值化算法
 * @author Coder_Rain
 */
public class HybridBinarizer {
    private final static int BLOCK_SIZE_POWER = 3;
    private final static int BLOCK_SIZE = 1 << BLOCK_SIZE_POWER;
    private final static int BLOCK_SIZE_MASK = BLOCK_SIZE - 1;
    /**
     * 最小支持的二值化尺寸
     */
    private final static int MINIMUM_DIMENSION = 40;
    private final static int MIN_DYNAMIC_RANGE = 24;


    /// <summary>
    /// 二值化整个图片
    /// </summary>
    /// <param name="rgbImage">rgb原图</param>
    /// <param name="width">宽度</param>
    /// <param name="height">高度</param>
    /// <returns>二值化图片</returns>
    public static byte[] binarizeEntireImage(int[] rgbImage, int width, int height) {
        byte[] luminances = GrayDiagram.CalculateLuminanceRgb24(rgbImage);
        return binarizeEntireImage(luminances, width, height);
    }

    /// <summary>
    /// 二值化整个图片
    /// </summary>
    /// <param name="grayImage">灰度图</param>
    /// <param name="width">宽度</param>
    /// <param name="height">高度</param>
    /// <returns>二值化图片</returns>
    public static byte[] binarizeEntireImage(byte[] grayImage, int width, int height) {
        if (width < MINIMUM_DIMENSION || height < MINIMUM_DIMENSION)
            return null;
        int subWidth = width >> BLOCK_SIZE_POWER;
        if ((width & BLOCK_SIZE_MASK) != 0) {
            subWidth++;
        }

        int subHeight = height >> BLOCK_SIZE_POWER;
        if ((height & BLOCK_SIZE_MASK) != 0) {
            subHeight++;
        }

        int[][] blackPoints = calculateBlackPoints(grayImage, subWidth, subHeight, width, height);

        byte[] binImage = new byte[width * height];
        calculateThresholdForBlock(grayImage, subWidth, subHeight, width, height, blackPoints, binImage);
        return binImage;
    }


    private static int[][] calculateBlackPoints(byte[] grayImage, int subWidth, int subHeight, int width,
                                                int height) {
        int[][] blackPoints = new int[subHeight][];
        for (int i = 0; i < subHeight; i++) {
            blackPoints[i] = new int[subWidth];
        }

        for (int y = 0; y < subHeight; y++) {
            int yOffset = y << BLOCK_SIZE_POWER;
            int maxYOffset = height - BLOCK_SIZE;
            if (yOffset > maxYOffset) {
                yOffset = maxYOffset;
            }

            for (int x = 0; x < subWidth; x++) {
                int xOffset = x << BLOCK_SIZE_POWER;
                int maxXOffset = width - BLOCK_SIZE;
                if (xOffset > maxXOffset) {
                    xOffset = maxXOffset;
                }

                int sum = 0;
                int min = 0xFF;
                int max = 0;
                for (int yy = 0, offset = yOffset * width + xOffset; yy < BLOCK_SIZE; yy++, offset += width) {
                    for (int xx = 0; xx < BLOCK_SIZE; xx++) {
                        int pixel = grayImage[offset + xx] & 0xFF;
                        // still looking for good contrast
                        sum += pixel;
                        if (pixel < min) {
                            min = pixel;
                        }

                        if (pixel > max) {
                            max = pixel;
                        }
                    }

                    // short-circuit min/max tests once dynamic range is met
                    if (max - min > MIN_DYNAMIC_RANGE) {
                        // finish the rest of the rows quickly
                        for (yy++, offset += width; yy < BLOCK_SIZE; yy++, offset += width) {
                            for (int xx = 0; xx < BLOCK_SIZE; xx++) {
                                sum += grayImage[offset + xx] & 0xFF;
                            }
                        }
                    }
                }

                int average = sum >> (BLOCK_SIZE_POWER * 2);
                if (max - min <= MIN_DYNAMIC_RANGE) {
                    average = min >> 1;

                    if (y > 0 && x > 0) {
                        int averageNeighborBlackPoint = (blackPoints[y - 1][x] + (2 * blackPoints[y][x - 1]) +
                                blackPoints[y - 1][x - 1]) >> 2;
                        if (min < averageNeighborBlackPoint) {
                            average = averageNeighborBlackPoint;
                        }
                    }
                }

                blackPoints[y][x] = average;
            }
        }

        return blackPoints;
    }


    private static void calculateThresholdForBlock(byte[] grayImage, int subWidth, int subHeight, int width,
                                                   int height, int[][] blackPoints, byte[] binImage) {
        for (int y = 0; y < subHeight; y++) {
            int yOffset = y << BLOCK_SIZE_POWER;
            int maxyOffset = height - BLOCK_SIZE;
            if (yOffset > maxyOffset) {
                yOffset = maxyOffset;
            }

            for (int x = 0; x < subWidth; x++) {
                int xOffset = x << BLOCK_SIZE_POWER;
                int maxXOffset = width - BLOCK_SIZE;
                if (xOffset > maxXOffset) {
                    xOffset = maxXOffset;
                }

                int left = cap(x, 2, subWidth - 3);
                int top = cap(y, 2, subHeight - 3);
                int sum = 0;
                for (int z = -2; z <= 2; z++) {
                    int[] blackRow = blackPoints[top + z];
                    sum += blackRow[left - 2];
                    sum += blackRow[left - 1];
                    sum += blackRow[left];
                    sum += blackRow[left + 1];
                    sum += blackRow[left + 2];
                }

                int average = sum / 25;
                thresholdBlock(grayImage, xOffset, yOffset, average, width, binImage);
            }
        }
    }

    private static void thresholdBlock(byte[] grayImage, int xOffset, int yOffset, int threshold, int stride,
                                       byte[] binImage) {
        int offset = (yOffset * stride) + xOffset;
        byte grayMin = 0;
        byte grayMax = -1;
        for (int y = 0; y < BLOCK_SIZE; y++, offset += stride) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int pixel = grayImage[offset + x] & 0xff;
                binImage[offset + x] = pixel <= threshold ? grayMin : grayMax;
            }
        }
    }

    private static int cap(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }
}
