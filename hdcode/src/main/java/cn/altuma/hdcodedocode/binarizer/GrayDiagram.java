package cn.altuma.hdcodedocode.binarizer;

public class GrayDiagram {
    private final static int RChannelWeight = 19562;
    private final static int GChannelWeight = 38550;
    private final static int BChannelWeight = 7424;
    private final static int ChannelWeight = 16;

    /**
     * 计算灰度
     * @param rgbRawBytes 原图
     * @return 灰度图
     */
    public static byte[] CalculateLuminanceRgb24(int[] rgbRawBytes)
    {
        byte[] luminances = new byte[rgbRawBytes.length];
        for (int index = 0; index < rgbRawBytes.length; index++)
        {
            // Calculate luminance cheaply, favoring green.
            int r = (rgbRawBytes[index] >> 16) & 0xff;
            int g = (rgbRawBytes[index] >> 8) & 0xff;
            int b = (rgbRawBytes[index] >> 0) & 0xff;
            luminances[index] =
                    (byte)((RChannelWeight * r + GChannelWeight * g + BChannelWeight * b) >> ChannelWeight);
        }

        return luminances;
    }
}
