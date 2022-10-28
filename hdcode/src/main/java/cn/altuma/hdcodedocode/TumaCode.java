package cn.altuma.hdcodedocode;

import cn.altuma.hdcodedocode.binarizer.HybridBinarizer;

public class TumaCode {
    final int maxWidth = 150;
    int[] normalPixs;
    int imageWidth;
    int imageHeight;

    byte[] _binPixs;// 二值化图片数组

    int standardCodeWidth;
    int[] leftup = new int[2];
    int[] rightup = new int[2];
    int[] leftdown = new int[2];
    int[] rightdown = new int[2];
    int[] LocationWidth = new int[4];// 四个定位点的宽度，顺序左上，右上，左下，右下
    int[] LocationHeight = new int[4];// 四个定位点的高度，顺序左上，右上，左下，右下

    NormalImage StandardCode = new NormalImage();

    public TumaCode() {
    }

    public TumaCode(NormalImage normalImage) {
        normalPixs = normalImage.Pixs;
        imageWidth = normalImage.Width;
        imageHeight = normalImage.Height;
    }

    boolean MakeStandardCode(boolean denoise) {

        _binPixs =ConventToBinImage(normalPixs, imageWidth, imageHeight);

        if (denoise) {
            Denoise(_binPixs, imageWidth, imageHeight);
        }
        if (!searchAllLocation())// 20190221添加
        {
            return false;
        }
        if (leftup[0] == 0 || leftdown[0] == 0 || rightup[0] < leftup[0] + 20 || rightdown[0] < leftdown[0] + 20)// 寻找定位点失败，返回
            return false;
        int standardWidthUp = getWidth(leftup, rightup, LocationWidth[0], LocationWidth[1]);
        int standardWidthDown = getWidth(leftdown, rightdown, LocationWidth[2], LocationWidth[3]);
        int standardHeightLeft = getHeight(leftup, leftdown, LocationHeight[0], LocationHeight[2]);
        int standardHeightRight = getHeight(rightup, rightdown, LocationHeight[1], LocationHeight[3]);

        int max = standardWidthUp;
        int min = standardWidthUp;
        max = max > standardWidthDown ? max : standardWidthDown;
        max = max > standardHeightLeft ? max : standardHeightLeft;
        max = max > standardHeightRight ? max : standardHeightRight;
        min = min < standardWidthDown ? min : standardWidthDown;
        min = min < standardHeightLeft ? min : standardHeightLeft;
        min = min < standardHeightRight ? min : standardHeightRight;

        if (min == -1)// 返回-1说明定位点有误
            return false;

        standardCodeWidth = (standardWidthUp + standardWidthDown + standardHeightLeft + standardHeightRight - max
                - min) >> 1;// 去掉最大值和最小值，使求边长误差更小

        if (standardCodeWidth < 40)// 20190220添加
            return false;

        int lastNum = standardCodeWidth % 10;// 个位数字
        int temp = standardCodeWidth / 10;
        if (lastNum > 4)
            temp += 1;
        standardCodeWidth = temp * 10;

        if (standardCodeWidth <= 0 || standardCodeWidth > maxWidth)// 高和宽计算有误，返回
            return false;
        return calculateStandCode();
    }

    private boolean calculateStandCode() {
        int n = 8;
        double[] xishu = new double[8];
        double x1, y1, u1, v1; // x、y表示原图中定位点位置，u、v表示还原后定位点位置
        double x2, y2, u2, v2;
        double x3, y3, u3, v3;
        double x4, y4, u4, v4;

        u1 = leftup[0];
        v1 = leftup[1];
        x1 = 4;
        y1 = 4;
        u2 = rightup[0];
        v2 = rightup[1];
        x2 = standardCodeWidth - 5;
        y2 = 4;
        u3 = leftdown[0];
        v3 = leftdown[1];
        x3 = 4;
        y3 = standardCodeWidth - 5;
        u4 = rightdown[0];
        v4 = rightdown[1];
        x4 = standardCodeWidth - 5;
        y4 = standardCodeWidth - 5;

        double[][] juzhen = {{x1, y1, 1, 0, 0, 0, -x1 * u1, -y1 * u1, u1},
                {0, 0, 0, x1, y1, 1, -x1 * v1, -y1 * v1, v1}, {x2, y2, 1, 0, 0, 0, -x2 * u2, -y2 * u2, u2},
                {0, 0, 0, x2, y2, 1, -x2 * v2, -y2 * v2, v2}, {x3, y3, 1, 0, 0, 0, -x3 * u3, -y3 * u3, u3},
                {0, 0, 0, x3, y3, 1, -x3 * v3, -y3 * v3, v3}, {x4, y4, 1, 0, 0, 0, -x4 * u4, -y4 * u4, u4},
                {0, 0, 0, x4, y4, 1, -x4 * v4, -y4 * v4, v4}};

        Gauss(n, juzhen, xishu);// 高斯消元法，解方程组

        int x, y;
        double a, b;
        int[] pixOut = new int[standardCodeWidth * standardCodeWidth];// 用于输出的还原后的图片图片数组
        for (int i = 0; i < standardCodeWidth; i++) {
            b = i + 0.4;
            for (int j = 0; j < standardCodeWidth; j++) {
                a = j + 0.4;
                x = (int) ((xishu[0] * a + xishu[1] * b + xishu[2]) / (xishu[6] * a + xishu[7] * b + 1));
                y = (int) ((xishu[3] * a + xishu[4] * b + xishu[5]) / (xishu[6] * a + xishu[7] * b + 1));
                if (x < 0 || y < 0 || x > imageWidth - 1 || y > imageHeight - 1)
                    return false;

                pixOut[i * standardCodeWidth + j] = normalPixs[y * imageWidth + x];
            }
        }
        StandardCode.Pixs = pixOut;
        StandardCode.Width = standardCodeWidth;
        StandardCode.Height = standardCodeWidth;
        return true;
    }

    private boolean searchAllLocation() {
        int w = imageWidth;// 定义拆分后的图片的宽高
        int h = imageHeight;

        byte[] pixFirst = _binPixs;

        int[] locationWH1 = new int[2];
        int[] location1 = searchEveryLocation(pixFirst, w, h, 0, locationWH1);
        if (location1[0] * location1[1] == 0)
            return false;
        removeLocation(location1, tempLocationLength);

        int[] locationWH2 = new int[2];
        int[] location2 = searchEveryLocation(pixFirst, w, h, location1[1], locationWH2);
        if (location2[0] * location2[1] == 0)
            return false;
        removeLocation(location2, tempLocationLength);

        int[] locationWH3 = new int[2];
        int[] location3 = searchEveryLocation(pixFirst, w, h, location2[1], locationWH3);
        if (location3[0] * location3[1] == 0)
            return false;
        removeLocation(location3, tempLocationLength);

        int[] locationWH4 = new int[2];
        int[] location4 = searchEveryLocation(pixFirst, w, h, location3[1], locationWH4);
        if (location4[0] * location4[1] == 0)
            return false;
        removeLocation(location4, tempLocationLength);

        int centreX = location1[0] + location2[0] + location3[0] + location4[0] >> 2;
        int centreY = location1[1] + location2[1] + location3[1] + location4[1] >> 2;

        judgePosition(location1, centreX, centreY, locationWH1);
        judgePosition(location2, centreX, centreY, locationWH2);
        judgePosition(location3, centreX, centreY, locationWH3);
        judgePosition(location4, centreX, centreY, locationWH4);

        return true;
    }

    private int[] searchEveryLocation(byte[] p, int w, int h, int row, int[] locationWH) {
        int locationY, locationX;
        int count = 1;

        int[] outNum = new int[w];
        int bin = row * imageWidth;
        for (int i = row; i < h; i++) {
            int index = 0;
            for (int j = 1; j < w; j++) {
                bin++;
                if (p[bin] == p[bin - 1]) {
                    count++;
                } else {
                    outNum[index] = count;
                    index++;
                    count = 1;
                }
            }
            bin++;
            outNum[index] = count;
            locationY = i;
            locationX = searchOnLine(outNum, index + 1);

            count = 1;
            if (locationX != -1) {
                int tempLocationWidth = tempLocationLength;
                int unitLength = tempLocationLength / 9;
                for (int x = locationX; x < (locationX + (unitLength >> 1) + 1); x++) {
                    int count2 = 1;
                    int[] outnum2 = new int[h];

                    int index2 = 0;

                    int j = locationY - 5 * unitLength;
                    j = j > 0 ? j : 0;
                    int h2 = j + 11 * unitLength;
                    h2 = h2 < h ? h2 : h;
                    int bin2 = imageWidth * j;
                    bin2 += x;

                    for (int j2 = j + 1; j2 < h; j2++) {
                        bin2 += imageWidth;
                        if (p[bin2] == p[bin2 - imageWidth]) {
                            count2++;
                        } else {
                            outnum2[index2] = count2;
                            index2++;
                            count2 = 1;
                        }
                    }
                    outnum2[index2] = count2;
                    int temp = searchOnLine(outnum2, index2 + 1) + j;
                    if (locationY - temp < unitLength && locationY >= temp && temp != -1
                            && tempLocationLength < (tempLocationWidth << 1)
                            && (tempLocationLength << 1) > tempLocationWidth) {
                        locationWH[0] = tempLocationWidth;
                        locationWH[1] = tempLocationLength;
                        locationX = x;
                        int[] zuobiao = new int[]{locationX, locationY};
                        return zuobiao;
                    }
                }
            }
        }
        locationWH[0] = locationWH[1] = -1;
        return new int[]{0, 0};
    }

    private int getWidth(int[] locationLeft, int[] locationRight, int locationWidthLeft, int locationWidthRight)// 求出原图形宽度
    {
        long x = locationRight[0] - locationLeft[0];// 数字经过乘法运算后过大，可能超出int范围，因此使用long
        long y = locationRight[1] - locationLeft[1];
        int locationWidth = (locationWidthLeft + locationWidthRight) >> 1;
        if (x == 0)
            return -1;
        int width = (int) ((9216 * (x * x + y * y) / (locationWidth * x) + 9216) >> 10);// 9216=9<<10，放大
        // 避免浮点
        return width;
    }

    private int getHeight(int[] locationUp, int[] locationDown, int locationWidthUp, int locationWidthDown)// 求出原图形高度
    {
        long x = locationDown[0] - locationUp[0];// 数字经过乘法运算后过大，可能超出int范围，因此使用long
        long y = locationDown[1] - locationUp[1];
        int locationHeight = (locationWidthUp + locationWidthDown) >> 1;
        if (y == 0)
            return -1;
        int height = (int) ((9216 * (x * x + y * y) / (locationHeight * y) + 9216) >> 10);// 9216=9<<10，放大
        // 避免浮点
        return height;
    }

    /// <summary>
    /// 查找符合1:1:2:1:2:1:1比例的数组
    /// </summary>
    /// <param name="num">数组</param>
    /// <param name="length"></param>
    /// <returns></returns>
    private int searchOnLine(int[] num, int length)// 根据比例1:1:2:1:2:1:1返回中心坐标
    {
        int l = 0;
        if (num.length > 6)
            l = num[0] + num[1] + num[2];
        else
            return -1;
        for (int i = 3; i < length - 3; i++) {
            if (judge(num, i)) {
                return l;
            }
            l += num[i];
        }
        return -1;
    }

    /// <summary>
    /// 定位点宽度临时变量，用于存储每次符合条件的临时定位点宽度
    /// </summary>
    int tempLocationLength;

    private boolean judge(int[] num, int index)// 判断比例1:1:2:1:2:1:1
    {
        int a_3 = num[index - 3] << 10;
        int a_2 = a_3 + (num[index - 2] << 10);
        int a_1 = a_2 + (num[index - 1] << 10);
        int a1 = a_1 + (num[index] << 10);
        int a2 = a1 + (num[index + 1] << 10);
        int a3 = a2 + (num[index + 2] << 10);
        tempLocationLength = a3 + (num[index + 3] << 10);

        int tempUnitLength = tempLocationLength / 9;// 临时放大后的单位长度
        int deviation = tempUnitLength / 2;// 允许误差
        if (a_3 > tempUnitLength - deviation && a_3 < tempUnitLength + deviation
                && a_2 > (tempUnitLength << 1) - deviation && a_2 < (tempUnitLength << 1) + deviation
                && a_1 > (tempUnitLength << 2) - deviation && a_1 < (tempUnitLength << 2) + deviation
                && a1 > tempLocationLength - (tempUnitLength << 2) - deviation
                && a1 < tempLocationLength - (tempUnitLength << 2) + deviation
                && a2 > tempLocationLength - (tempUnitLength << 1) - deviation
                && a2 < tempLocationLength - (tempUnitLength << 1) + deviation
                && a3 > tempLocationLength - tempUnitLength - deviation
                && a3 < tempLocationLength - tempUnitLength + deviation) {
            tempLocationLength >>= 10;
            return true;
        } else {
            return false;
        }
    }

    private void removeLocation(int[] location, int allLength) {
        int x = location[0] - (allLength >> 1);
        int y = location[1] - (allLength >> 1);
        x = x > 0 ? x : 0;
        y = y > 0 ? y : 0;
        for (int ii = 0, i = y; ii < allLength; i++, ii++) {
            i = i < imageHeight ? i : imageHeight - 1;
            for (int jj = 0, j = x; jj < allLength; j++, jj++) {
                j = j < imageWidth ? j : imageWidth - 1;
                _binPixs[i * imageWidth + j] = 0;
            }
        }

    }

    /// <summary>
    /// 判断当前点正确位置，左上还是右下
    /// </summary>
    /// <param name="location">当前点坐标</param>
    /// <param name="centreX">中心点横坐标</param>
    /// <param name="centreY">中心点纵坐标</param>
    private void judgePosition(int[] location, int centreX, int centreY, int[] locationWH) {
        int locationWidth = locationWH[0];
        int locationHeight = locationWH[1];
        if (location[1] < centreY) {
            if (location[0] < centreX) {
                leftup = location;
                LocationWidth[0] = locationWidth;
                LocationHeight[0] = locationHeight;
            } else {
                rightup = location;
                LocationWidth[1] = locationWidth;
                LocationHeight[1] = locationHeight;
            }
        } else {
            if (location[0] < centreX) {
                leftdown = location;
                LocationWidth[2] = locationWidth;
                LocationHeight[2] = locationHeight;
            } else {
                rightdown = location;
                LocationWidth[3] = locationWidth;
                LocationHeight[3] = locationHeight;
            }
        }
    }

    /// <summary>
    /// 快速二值化算法
    /// </summary>
    /// <param name="rgbPixs">图片数组</param>
    /// <param name="width">图片宽度</param>
    /// <param name="height">图片高度</param>
    /// <param name="binarizeMethodIndex">0,1,2性能越来越差，二值化效果越来越好</param>
    /// <returns>二值化结果</returns>
    private byte[] ConventToBinImage(int[] rgbPixs, int width, int height) {
        return HybridBinarizer.BinarizeEntireImage(rgbPixs, width, height);
    }

    /// <summary>
    /// 降噪算法
    /// </summary>
    /// <param name="_binPixs"></param>
    /// <param name="w"></param>
    /// <param name="h"></param>
    private void Denoise(byte[] binPixs, int w, int h) {
        int DenoiseWidth = (w < h ? w : h) >> 9;
        int firstPix = 0;
        {
            int pix = firstPix;
            for (int i = 0; i < h; i++) {
                int count = 1;
                for (int j = 1; j < w; j++) {
                    pix++;
                    if (binPixs[pix] == binPixs[pix - 1]) {
                        count++;
                    } else {
                        if (count <= DenoiseWidth) {
                            for (int k = 0; k < count; k++) {
                                binPixs[pix - k - 1] = -1;
                            }
                        }
                        count = 1;
                    }
                }
                pix++;
            }
            int bin = firstPix;
            for (int i = 0; i < w; i++) {
                int count = 1;
                for (int j = 1; j < h; j++) {
                    bin += w;
                    if (binPixs[bin] == binPixs[bin - w]) {
                        count++;
                    } else {
                        if (count <= DenoiseWidth) {
                            for (int k = 0; k < count; k++) {
                                binPixs[bin - (k + 1) * w] = -1;
                            }
                        }
                        count = 1;
                    }
                }
                bin = bin - (w * h) + w + 1;
            }

            bin = firstPix;
            for (int i = 0; i < h; i++) {
                int count = 1;
                for (int j = 1; j < w; j++) {
                    bin++;
                    if (binPixs[bin] == binPixs[bin - 1]) {
                        count++;
                    } else {
                        if (count <= DenoiseWidth) {
                            for (int k = 0; k < count; k++) {
                                binPixs[bin - k - 1] = binPixs[bin];
                            }
                        }
                        count = 1;
                    }

                }
                bin++;
            }

            bin = firstPix;
            for (int i = 0; i < w; i++) {
                int count = 1;
                for (int j = 1; j < h; j++) {
                    bin += w;
                    if (binPixs[bin] == binPixs[bin - w]) {
                        count++;
                    } else {
                        if (count <= DenoiseWidth) {
                            for (int k = 0; k < count; k++) {
                                binPixs[(bin - (k + 1) * w)] = binPixs[bin];
                            }
                        }
                        count = 1;
                    }
                }
                bin = bin - (w * h) + w + 1;
            }
        }

    }

    NormalImage getStandCode() {
        return StandardCode;
    }

    GrayImage getStandBinCode() {
        GrayImage binCode = new GrayImage();
        binCode.Width = StandardCode.Width;
        binCode.Height = StandardCode.Height;
        binCode.Pixs=ConventToBinImage(StandardCode.Pixs,  StandardCode.Width, StandardCode.Height);
        return binCode;
    }


    private void Gauss(int n, double[][] a, double[] x)// 高斯消元法解方程组,n为未知数个数，a为方程组增广矩阵
    {
        double d;

        // 消元
        for (int k = 0; k < n; k++) {
            selectMainElement(n, k, a); // 选择主元素

            // for (int j = k; j <= n; j++ ) a[k, j] = a[k, j] / a[k, k];
            // 若将下面两个语句改为本语句，则程序会出错，因为经过第1次循环
            // 后a[k,k]=1，a[k,k]的值发生了变化，所以在下面的语句中先用d
            // 将a[k,k]的值保存下来
            d = a[k][k];
            for (int j = k; j <= n; j++)
                a[k][j] = a[k][j] / d;

            for (int i = k + 1; i < n; i++) {
                d = a[i][k]; // 这里使用变量d将a[i,k]的值保存下来的原理与上面注释中说明的一样
                for (int j = k; j <= n; j++)
                    a[i][j] = a[i][j] - d * a[k][j];
            }

        }

        // 回代
        x[n - 1] = a[n - 1][n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = a[i][n];
            for (int j = i + 1; j < n; j++)
                x[i] = x[i] - a[i][j] * x[j];
        }
    }

    private void selectMainElement(int n, int k, double[][] a)// 选择主元素
    {
        // 寻找第k列的主元素以及它所在的行号
        double t, mainElement; // mainElement用于保存主元素的值
        int l; // 用于保存主元素所在的行号

        // 从第k行到第n行寻找第k列的主元素，记下主元素mainElement和所在的行号l
        mainElement = Math.abs(a[k][k]); // 注意别忘了取绝对值
        l = k;
        for (int i = k + 1; i < n; i++) {
            if (mainElement < Math.abs(a[i][k])) {
                mainElement = Math.abs(a[i][k]);
                l = i; // 记下主元素所在的行号
            }
        }

        // l是主元素所在的行。将l行与k行交换，每行前面的k个元素都是0，不必交换
        if (l != k) {
            for (int j = k; j <= n; j++) {
                t = a[k][j];
                a[k][j] = a[l][j];
                a[l][j] = t;
            }
        }
    }

}
