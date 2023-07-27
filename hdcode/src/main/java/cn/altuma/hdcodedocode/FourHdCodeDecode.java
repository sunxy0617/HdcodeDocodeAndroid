package cn.altuma.hdcodedocode;

/**
 * @author Coder_Rain
 */
public class FourHdCodeDecode {
    private int[] _codePixs;
    private int _rotateTime; //向右转次数
    private int _codeWidth, _totalLength;
    private int _mm, _tt, _nn;
    private int[] _rscode;
    private int _index;

    /**
     * 区分黑白彩色阈值
     */
    public int Threshold = 120;

    /// <summary>
    /// 记录当前解读的彩色点序号，最终值为
    /// </summary>
    private int[] _colorPixs;

    private int _colorPixsIndex = 0;

    public byte[] decode(NormalImage normalImage, HdcodeInfo hdcodeInfo)
    {
        _codePixs = normalImage.Pixs;
        _codeWidth = normalImage.Width;
        _rotateTime = hdcodeInfo.RotateTime;
        int checkNum = hdcodeInfo.CheckNum;
        int dataLength = hdcodeInfo.DataLength;

        _mm = 8;
        if (dataLength > 127 && dataLength <= 568) //tt=length>>1 分界127,568,2000
            _mm = 10; //tt=length>>2 分界160,750,3000
        else if (dataLength > 568 && dataLength <= 2000)
            _mm = 12;
        _tt = dataLength >> 1;
        if (_tt < 2)
            _tt = 2;
        _nn = (1 << _mm) - 1;

        int kk = _nn - (_tt << 1);
        if (kk <= 0 || _nn < kk) {
            return null;
        }

        if ((_mm != 8 && _mm != 10 && _mm != 12 && _mm != 14 && _mm != 16) || _tt > 1000)
        {
            return null;
        }

        _totalLength = (dataLength << 3) + (_mm * _tt << 1);
        if (_totalLength % _mm != 0)
        {
            _totalLength = (_totalLength / _mm + 1) * _mm;
        }

        for (int i = hdcodeInfo.RulePixs.length - 1; i >= 0; i--)
        {
            // 初始化彩色点记录数组
            _colorPixsIndex = 0;
            _colorPixs = new int[(_totalLength + 1) / 2];

            _rscode = new int[_nn];
            int[][] newPixs = hdcodeInfo.RulePixs[i];
            picToWords(newPixs);

            ReadColor(_colorPixs, _colorPixsIndex);


            byte[] bdata = new Reed_Solomon(_mm, _tt).RSDecode(_rscode);

            if ((CRC_Check.CRC16(bdata)) == checkNum) //检测校验号是否正确
                return bdata;
        }

        return null;
    }

    private void picToWords(int[][] pixs)
    {
        _index = 0;
        for (int i = 0; i < _codeWidth; i++)
        {
            for (int j = 0; j < _codeWidth; j++)
            {
                if (pixs[i][j] == 1)
                {
                    ReadPoint(j, i);
                }
            }
        }
    }

    private void ReadPoint(int x, int y) //根据点的颜色，读取对应的“0”、“1”，写入out2， 并出去已经解读过的脏点
    {
        int m, n;
        if (_colorPixsIndex * 2 >= _nn * _mm || _colorPixsIndex * 2 >= _totalLength)
            return;
        if (_rotateTime == 0)
        {
            m = y;
            n = x;
        }
        else if (_rotateTime == 1)
        {
            m = x;
            n = _codeWidth - 1 - y;
        }
        else if (_rotateTime == 2)
        {
            m = _codeWidth - 1 - y;
            n = _codeWidth - 1 - x;
        }
        else
        {
            m = _codeWidth - 1 - x;
            n = y;
        }

        if (x >= 0 && x < _codeWidth && y >= 0 && y < _codeWidth)
        {
            int colorNum = _codePixs[m * _codeWidth + n] & 0xffffff;
            _colorPixs[_colorPixsIndex] = colorNum;
            _colorPixsIndex++;
        }
    }

    private void ReadColor(int[] colorPixs, int colorPixsLength)
    {
        int maxR = 0, maxG = 0, maxB = 0;
        int minR = 255, minG = 255, minB = 255;
        for (int i = 0; i < colorPixsLength; i++)
        {
            int rgb = colorPixs[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;

            maxR = maxR < r ? r : maxR;
            maxG = maxG < g ? g : maxG;
            maxB = maxB < b ? b : maxB;

            minR = minR > r ? r : minR;
            minG = minG > g ? g : minG;
            minB = minB > b ? b : minB;
        }

        for (int i = 0; i < colorPixsLength; i++)
        {
            int rgb = colorPixs[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;

            int newR = (r - minR) * 256 / (maxR - minR + 1);
            int newG = (g - minG) * 256 / (maxG - minG + 1);
            int newB = (b - minB) * 256 / (maxB - minB + 1);

            int[] rgbArr = { newR, newG, newB };
            int[] order = new int[3];
            getMaxColor(rgbArr, order);

            _rscode[_index / _mm] <<= 2;
            if (rgbArr[order[0]] > Threshold)
            {
                _rscode[_index / _mm] += (order[0] + 1);
            }

            _index += 2;
        }

        // throw new System.NotImplementedException();
    }

    /// <summary>
    /// 对三个值进行排序，并返回对应大小的下标
    /// </summary>
    /// <param name="rgb">三个值</param>
    /// <param name="max">最大值的下标</param>
    /// <param name="mid">最小值的下标</param>
    /// <param name="min">中间值的下标</param>
    private void getMaxColor(int[] rgb, int[] order)// 获取三个数大小顺序
    {
        int r = rgb[0];
        int g = rgb[1];
        int b = rgb[2];
        int max, mid, min;
        if (r > g) {
            if (r > b) {
                if (g > b) {
                    max = 0;
                    mid = 1;
                    min = 2;
                } else {
                    max = 0;
                    mid = 2;
                    min = 1;
                }
            } else {
                max = 2;
                mid = 0;
                min = 1;
            }
        } else {
            if (r < b) {
                if (g < b) {
                    max = 2;
                    mid = 1;
                    min = 0;
                } else {
                    max = 1;
                    mid = 2;
                    min = 0;
                }
            } else {
                max = 1;
                mid = 0;
                min = 2;
            }
        }
        order[0] = max;
        order[1] = mid;
        order[2] = min;
    }
}
