package cn.altuma.hdcodedocode.binarizer;

/**
 * @author Coder_Rain
 */
public class GrayDiagram {
    private final static int R_CHANNEL_WEIGHT = 19562;
    private final static int G_CHANNEL_WEIGHT = 38550;
    private final static int B_CHANNEL_WEIGHT = 7424;
    private final static int CHANNEL_WEIGHT = 16;

    /**
     * 计算灰度
     * @param rgbRawBytes 原图
     * @return 灰度图
     */
    public static byte[] calculateLuminanceRgb24(int[] rgbRawBytes)
    {
        byte[] luminances = new byte[rgbRawBytes.length];
        for (int index = 0; index < rgbRawBytes.length; index++)
        {
            // Calculate luminance cheaply, favoring green.
            int r = (rgbRawBytes[index] >> 16) & 0xff;
            int g = (rgbRawBytes[index] >> 8) & 0xff;
            int b = (rgbRawBytes[index] >> 0) & 0xff;
            luminances[index] =
                    (byte)((R_CHANNEL_WEIGHT * r + G_CHANNEL_WEIGHT * g + B_CHANNEL_WEIGHT * b) >> CHANNEL_WEIGHT);
        }

        return luminances;
    }
}
