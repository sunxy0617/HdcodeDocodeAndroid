package cn.altuma.hdcodedocode;

public class EightHdCodeDecode {
	int t;//向右转次数
    int w, totalLength;
    int mm, tt, nn;
    int[] rscode;
    int index;
    HdCodeRules hdCodeRules = new HdCodeRules();

    int[] pixs;
    public byte[] decode(NormalImage normalImage, HdcodeInfo hdcodeInfo)//构造函数
    {
        this.pixs = normalImage.Pixs;
      
        this.w = normalImage.Width;
        return getBytes(hdcodeInfo);
    }

    public byte[] getBytes( HdcodeInfo hdcodeInfo)
    {
        t = hdcodeInfo.RotateTime;
        int checkNum = hdcodeInfo.CheckNum;
        int bdataLength = hdcodeInfo.DataLength;

        mm = 8;
        if (bdataLength > 127 && bdataLength <= 568)  //tt=length>>1 分界127,568,2000
            mm = 10;                                  //tt=length>>2 分界160,750,3000
        else if (bdataLength > 568 && bdataLength <= 2000)
            mm = 12;
        tt = bdataLength >> 1;
        if (tt < 2)
            tt = 2;
        nn = (1 << mm) - 1;
        if ((mm != 8 && mm != 10 && mm != 12) || tt > 1000)
        {
            return null;
        }

        totalLength = (bdataLength << 3) + (mm * tt << 1);
        if (totalLength % mm != 0)
            totalLength = (totalLength / mm + 1) * mm;
        rscode = new int[nn];/////////
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
        if (index >= nn * mm || index >= totalLength) return;/////////////////////////////////////////////////////////////
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
            int colorNum = pixs[m * w + n];
            String hex = Integer.toHexString(colorNum);
            if (colorNum != -8355712)
            {
                rscode[index / mm] <<= 1;
                if (((colorNum >> 16) & 0xff) == 255)
                    rscode[index / mm] += 1;
                index++;

                rscode[index / mm] <<= 1;
                if (((colorNum >> 8) & 0xff) == 255)
                    rscode[index / mm] += 1;
                index++;

                rscode[index / mm] <<= 1;
                if ((colorNum & 0xff) == 255)
                    rscode[index / mm] += 1;
                index++;
                pixs[w * m + n] = -8355712;
            }
        }
    }
}
