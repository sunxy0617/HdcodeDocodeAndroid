package cn.altuma.hdcodedocode;

public class HdcodeDecode {
	GrayImage grayImage = new GrayImage();
	NormalImage normalImage = new NormalImage();
	GrayImage standBinImage;

	TumaCode twoDimensionalCode = new TumaCode();
	ReadHdcodeInfo readHdInfo = new ReadHdcodeInfo();
	BinHdCodeDecode binDecode = new BinHdCodeDecode();
	FourHdCodeDecode fourDecode = new FourHdCodeDecode();
	EightHdCodeDecode eightDecode = new EightHdCodeDecode();

	public String getWords(int[] pixs, int W, int H, boolean denoise) {
		twoDimensionalCode.normalPixs = pixs;
		twoDimensionalCode.imageWidth = W;
		twoDimensionalCode.imageHeight = H;
		if (!twoDimensionalCode.MakeStandardCode(denoise))
			return null;
		standBinImage = twoDimensionalCode.getStandBinCode();
		if (standBinImage == null)
            return null;

        HdcodeInfo hdInfo = readHdInfo.getHdcodeInfo(standBinImage);
        if (hdInfo == null)
            return null;
		if (standBinImage != null) {
			byte[] data = binDecode.decode(standBinImage,hdInfo);
			if (data != null) {
				try {
					String words = new String(data, "GBK");
					return words;
				} catch (Exception e) {
					return null;
				}
			}
		}
		NormalImage standFourCode = twoDimensionalCode.getStandFourCode();
        if (standFourCode != null)
        {
            byte[] data = fourDecode.decode(standFourCode,hdInfo);
            if (data != null)
            {
            	try {
					String words = new String(data, "GBK");
					return words;
				} catch (Exception e) {
					return null;
				}
            }
        }
        
        NormalImage standEightCode = twoDimensionalCode.getStandEightCode();
        if (standEightCode != null)
        {
            byte[] data = eightDecode.decode(standEightCode,hdInfo);
            if (data != null)
            {
            	try {
					String words = new String(data, "GBK");
					return words;
				} catch (Exception e) {
					return null;
				}
            }
        }

		return null;
	}
}
