package cn.altuma.hdcodedocode;

public class HdcodeDecode {
	GrayImage standBinImage;

	TumaCode twoDimensionalCode = new TumaCode();
	ReadHdcodeInfo readHdInfo = new ReadHdcodeInfo();
	BinHdCodeDecode binDecode = new BinHdCodeDecode();
	FourHdCodeDecode fourDecode = new FourHdCodeDecode();

	public String getWords(int[] pixs, int W, int H, boolean denoise,String hdRulePath) {
		twoDimensionalCode.normalPixs = pixs;
		twoDimensionalCode.imageWidth = W;
		twoDimensionalCode.imageHeight = H;
		if (!twoDimensionalCode.MakeStandardCode(denoise))
			return null;
		standBinImage = twoDimensionalCode.getStandBinCode();
		if (standBinImage == null)
            return null;

        HdcodeInfo hdInfo = readHdInfo.getHdcodeInfo(standBinImage,hdRulePath);
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
		NormalImage standCode = twoDimensionalCode.getStandCode();
        if (standCode != null)
        {
            byte[] data = fourDecode.decode(standCode,hdInfo);
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
