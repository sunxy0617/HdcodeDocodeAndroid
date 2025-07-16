package cn.altuma.hdcode;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import cn.altuma.hdcodedocode.HdcodeDecode;

public class Hdcode {

    public static String hdRulePath;

    /**
     * @param pixels 图片bgra格式像素点数据集合
     * @param width  宽度
     * @param height 高度
     * @return String格式返回值
     */
    public static String decodeImagePixels(int[] pixels, int width, int height) {
        int length = width * height;
        byte[] grayPixs = new byte[length];
        for (int j = 0; j < length; j++) {
            int r = (pixels[j] >> 16) & 0xff;
            int g = (pixels[j] >> 8) & 0xff;
            int b = pixels[j] & 0xff;
            grayPixs[j] = (byte) ((r + (g << 1) + b) >> 2);
        }

        HdcodeDecode hdcode = new HdcodeDecode();
        String result = hdcode.getWords(pixels, width, height, true, hdRulePath);
        if (result == null) {
            result = QrDecode(grayPixs, width, height, true);
            if (result == null) {
                result = QrDecode(grayPixs, width, height, false);
            }
        }
        return result;
    }

    /**
     * @param data   图片yuv420格式像素点数据
     * @param width  宽度
     * @param height 高度
     * @return 解码结果
     */
    public static String decodeYuv420(byte[] data, int width, int height) {
        String result = HdcodeDecode(data, width, height, false);
        if (result == null) {
            result = QrDecode(data, width, height, true);
            if (result == null) {
                result = QrDecode(data, width, height, false);
            }
        }
        return result;
    }

    private static String HdcodeDecode(byte[] data, int w, int h, boolean denoise) {
        HdcodeDecode hdcode = new HdcodeDecode();
        int[] rgb = new int[w * h];
        YUV420_2_rgb(rgb, data, w, h);
        return hdcode.getWords(rgb, w, h, denoise, hdRulePath);
    }

    private static String QrDecode(byte[] data, int w, int h, boolean rotate) {
        byte[] imgData;
        if (rotate) {
            imgData = new byte[data.length];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++)
                    imgData[x * h + h - y - 1] = data[x + y * w];
            }
            w = w ^ h;
            h = w ^ h;
            w = w ^ h;
        } else {
            imgData = data;
        }

        MultiFormatReader multiFormatReader = new MultiFormatReader();
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(imgData, w, h, 0, 0, w, h, false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result rawResult = null;
        try {
            rawResult = multiFormatReader.decode(bitmap);
        } catch (ReaderException re) {
            //在没有扫图码前 会一直抛出此异常
            //	Toast.makeText(getApplicationContext(), "MyHDcode:"+re.toString(), Toast.LENGTH_SHORT).show();
        } finally {
            multiFormatReader.reset();
        }
        if (rawResult != null)

            return rawResult.getText();
        else
            return null;
    }

    private static void YUV420_2_rgb(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    private static byte[] cutYUV(byte[] data, int srcWidth, int srcHeight, int x, int y, int destWidth, int destHeight) {
        x = x >> 1 << 1;
        y = y >> 1 << 1;
        int destHalfWidth = destWidth >> 1;
        int destHalfHeight = destHeight >> 1;
        destWidth = destHalfWidth << 1;
        destHeight = destHalfHeight << 1;
        int destUVLength = destHalfWidth * destHalfHeight;
        int destLength = destUVLength * 6;
        byte[] destData = new byte[destLength];
        int destIndex = 0;
        for (int i = 0; i < destHeight; i++) {
            int srcIndex = x + (y + i) * srcWidth;
            for (int j = 0; j < destWidth; j++) {
                destData[destIndex++] = data[srcIndex++];
            }
        }

        for (int i = 0; i < destHalfHeight; i++) {
            int srcIndex = x + (srcHeight + y / 2 + i) * srcWidth;
            for (int j = 0; j < destWidth; j++) {
                destData[destIndex++] = data[srcIndex++];
            }
        }
        return destData;
    }
}
