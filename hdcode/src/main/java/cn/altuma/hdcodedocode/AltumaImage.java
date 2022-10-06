package cn.altuma.hdcodedocode;

class AltumaImage {

}

class NormalImage
{
    public int Width ;
    public int Height ;
    public int[] Pixs ;

    public NormalImage()
    { }

    public NormalImage(int[] pixs,int width,int height)
    {
        if (pixs.length < width * height)
            try {
                throw new Exception("参数不匹配,pixs的长度不能小于宽度和高度相乘");
            } catch (Exception e) {
                e.printStackTrace();
            }
        else
        {
            Width = width;
            Height = height;
            Pixs = pixs;
        }
    }
}

/// <summary>
/// 8位灰度图片
/// </summary>
class GrayImage
{
    public int Width ;
    public int Height ;
    public byte[] Pixs ;

    public GrayImage()
    { }

    public GrayImage(int width, int height)
    {
        byte[] pixs = new byte[width * height];
        Width = width;
        Height = height;
        Pixs = pixs;
    }

    public GrayImage(byte[] pixs, int width, int height)
    {
        if (pixs.length < width * height)
            try {
                throw new Exception("参数不匹配,pixs的长度不能小于宽度和高度相乘");
            } catch (Exception e) {
                e.printStackTrace();
            }
        else
        {
            Width = width;
            Height = height;
            Pixs = pixs;
        }
    }
}
