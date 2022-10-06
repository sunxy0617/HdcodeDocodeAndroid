package cn.altuma.hdcodedocode;

public class BinHdCodeDecode {
	int t;//向右转次数
    int w, totalLength;
    int mm, tt, nn;
    int[] rscode;
    int index;

    HdCodeRules hdCodeRules = new HdCodeRules();
    byte[] pixs;
    public byte[] decode(GrayImage binImage, HdcodeInfo hdcodeInfo)//构造函数
    {
        pixs = binImage.Pixs;
        w = binImage.Width;

        return getBytes(hdcodeInfo);
    }

    private byte[] getBytes(HdcodeInfo hdcodeInfo)
    {
        t = hdcodeInfo.RotateTime;
        int checkNum = hdcodeInfo.CheckNum;
        
        int bdataLength = hdcodeInfo.DataLength;

        mm = 8;
        if (bdataLength > 160 && bdataLength <= 750)
            mm = 10;
        else if (bdataLength > 750 && bdataLength <=3000)
            mm = 12;

        tt = bdataLength >> 2;
        if (tt < 2)
            tt = 2;
        nn = (1 << mm) - 1;
        
        int kk = nn - (t << 1);
        if (kk <= 0 || nn < kk)
            return null;

        if ((mm != 8 && mm != 10 && mm != 12 && mm != 14 && mm != 16) || tt > 1000)
        {
            return null;
        }

        totalLength = (bdataLength << 3) + (mm * tt << 1);
        if (totalLength % mm != 0)
            totalLength = (totalLength / mm + 1) * mm;
        rscode = new int[nn];
        //removeLocation(t);
        
        for (int i = hdcodeInfo.RulePixs.length - 1; i >= 0; i--)
        {
            rscode = new int[nn];
            int[][] newPixs = hdcodeInfo.RulePixs[i];
            picToWords(newPixs);

            byte[] bdata = new Reed_Solomon(mm, tt).RSDecode(rscode);

            if ((CRC_Check.CRC16(bdata)) == checkNum)//检测校验号是否正确
                return bdata;
        }
        return null;
    }
    
    private void picToWords(int[][] pixs)
    {
        index = 0;
        for (int i = 0; i < w; i++)
        {
            for (int j = 0; j < w; j++)
            {
                if (pixs[i][j] == 1)
                {
                    ReadPoint(j, i);
                }
            }
        }
    }

    private void ReadPoint(int x, int y)//根据点的颜色，读取对应的“0”、“1”，写入out2， 并出去已经解读过的脏点  
    {
        int m, n;
        if (index >= nn * mm || index >= totalLength) return;
        if (t == 0)
        {
            m = y;
            n = x;
        }
        else if (t == 1)
        {
            m = x;
            n = w - 1 - y;

        }
        else if (t == 2)
        {
            m = w - 1 - y;
            n = w - 1 - x;
        }
        else
        {
            m = w - 1 - x;
            n = y;
        }
        if (x >= 0 && x < w && y >= 0 && y < w)
        {
            if (pixs[m * w + n] != -128)
            {
                rscode[index / mm] <<= 1;
                if (pixs[w * m + n] == -1)
                    rscode[index / mm] += 1;
                index++;
            }
        }
    }
}