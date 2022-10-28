package cn.altuma.hdcodedocode.binarizer;

/**
 * 混合二值化算法
 */
public class HybridBinarizer {
    private final static int BlockSizePower = 3;
    private final static int BlockSize = 1 << BlockSizePower; // ...0100...00
    private final static int BlockSizeMask = BlockSize - 1; // ...0011...11
    /**
     * 最小支持的二值化尺寸
     */
    private final static int MinimumDimension = 40;
    private final static int MinDynamicRange = 24;


    /// <summary>
    /// 二值化整个图片
    /// </summary>
    /// <param name="rgbImage">rgb原图</param>
    /// <param name="width">宽度</param>
    /// <param name="height">高度</param>
    /// <returns>二值化图片</returns>
    public static byte[] BinarizeEntireImage(int[] rgbImage, int width, int height) {
        byte[] luminances = GrayDiagram.CalculateLuminanceRgb24(rgbImage);
        return BinarizeEntireImage(luminances, width, height);
    }

    /// <summary>
    /// 二值化整个图片
    /// </summary>
    /// <param name="grayImage">灰度图</param>
    /// <param name="width">宽度</param>
    /// <param name="height">高度</param>
    /// <returns>二值化图片</returns>
    public static byte[] BinarizeEntireImage(byte[] grayImage, int width, int height) {
        if (width < MinimumDimension || height < MinimumDimension)
            return null;
        int subWidth = width >> BlockSizePower;
        if ((width & BlockSizeMask) != 0) {
            subWidth++;
        }

        int subHeight = height >> BlockSizePower;
        if ((height & BlockSizeMask) != 0) {
            subHeight++;
        }

        int[][] blackPoints = CalculateBlackPoints(grayImage, subWidth, subHeight, width, height);

        byte[] binImage = new byte[width * height];
        CalculateThresholdForBlock(grayImage, subWidth, subHeight, width, height, blackPoints, binImage);
        return binImage;
    }


    private static int[][] CalculateBlackPoints(byte[] grayImage, int subWidth, int subHeight, int width,
                                                int height) {
        int[][] blackPoints = new int[subHeight][];
        for (int i = 0; i < subHeight; i++) {
            blackPoints[i] = new int[subWidth];
        }

        for (int y = 0; y < subHeight; y++) {
            int yOffset = y << BlockSizePower;
            int maxYOffset = height - BlockSize;
            if (yOffset > maxYOffset) {
                yOffset = maxYOffset;
            }

            for (int x = 0; x < subWidth; x++) {
                int xOffset = x << BlockSizePower;
                int maxXOffset = width - BlockSize;
                if (xOffset > maxXOffset) {
                    xOffset = maxXOffset;
                }

                int sum = 0;
                int min = 0xFF;
                int max = 0;
                for (int yy = 0, offset = yOffset * width + xOffset; yy < BlockSize; yy++, offset += width) {
                    for (int xx = 0; xx < BlockSize; xx++) {
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
                    if (max - min > MinDynamicRange) {
                        // finish the rest of the rows quickly
                        for (yy++, offset += width; yy < BlockSize; yy++, offset += width) {
                            for (int xx = 0; xx < BlockSize; xx++) {
                                sum += grayImage[offset + xx] & 0xFF;
                            }
                        }
                    }
                }

                int average = sum >> (BlockSizePower * 2);
                if (max - min <= MinDynamicRange) {
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


    private static void CalculateThresholdForBlock(byte[] grayImage, int subWidth, int subHeight, int width,
                                                   int height, int[][] blackPoints, byte[] binImage) {
        for (int y = 0; y < subHeight; y++) {
            int yOffset = y << BlockSizePower;
            int maxyOffset = height - BlockSize;
            if (yOffset > maxyOffset) {
                yOffset = maxyOffset;
            }

            for (int x = 0; x < subWidth; x++) {
                int xOffset = x << BlockSizePower;
                int maxXOffset = width - BlockSize;
                if (xOffset > maxXOffset) {
                    xOffset = maxXOffset;
                }

                int left = Cap(x, 2, subWidth - 3);
                int top = Cap(y, 2, subHeight - 3);
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
                ThresholdBlock(grayImage, xOffset, yOffset, average, width, binImage);
            }
        }
    }

    private static void ThresholdBlock(byte[] grayImage, int xOffset, int yOffset, int threshold, int stride,
                                       byte[] binImage) {
        int offset = (yOffset * stride) + xOffset;
        byte grayMin = 0;
        byte grayMax = -1;
        for (int y = 0; y < BlockSize; y++, offset += stride) {
            for (int x = 0; x < BlockSize; x++) {
                int pixel = grayImage[offset + x] & 0xff;
                // Comparison needs to be <= so that black == 0 pixels are black even if the threshold is 0.
                // matrix[xOffset + x, yOffset + y] = (pixel <= threshold);
                binImage[offset + x] = pixel <= threshold ? grayMin : grayMax;
            }
        }
    }

    private static int Cap(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }
}
