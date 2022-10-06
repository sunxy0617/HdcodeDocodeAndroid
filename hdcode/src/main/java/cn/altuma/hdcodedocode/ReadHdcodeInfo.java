package cn.altuma.hdcodedocode;

/// <summary>
/// 智慧码通用信息
/// </summary>
class HdcodeInfo {
	/// <summary>
	/// 方案号
	/// </summary>
	long PlanNum;
	/// <summary>
	/// 信息CRC16校验码
	/// </summary>
	int CheckNum;
	/// <summary>
	/// 智慧码版本
	/// </summary>
	int Version;
	/// <summary>
	/// 被旋转次数
	/// </summary>
	int RotateTime;
	/// <summary>
	/// 智慧码中信息的长度
	/// </summary>
	int DataLength;
	/// <summary>
	/// 解码规则量化图形
	/// </summary>
	int[][][] RulePixs;

}

/// <summary>
/// 本类用于统一读取智慧码解码信息，加快多色吗读取速度
/// </summary>
class ReadHdcodeInfo {
	int t;// 向右转次数
	int codeWidth;
	byte[] pixs;//
	HdCodeRules hdCodeRules = new HdCodeRules();

	public HdcodeInfo getHdcodeInfo(GrayImage binImage) {
		pixs = binImage.Pixs;
		codeWidth = binImage.Width;

		return GetInfo();
	}

	private HdcodeInfo GetInfo() {
		t = 0;// 旋转次数
		int versionAndPlanNum = readVersionAndPlanNum(); // 解读版本号和部分方案号
		int checkNum = readCheckNum(); // 解读校验号
		if (versionAndPlanNum == -1 || checkNum == -1 || versionAndPlanNum == 65535 || checkNum == 65535) {
			t = 1;
			versionAndPlanNum = readVersionAndPlanNum2();
			checkNum = readCheckNum2();
		}
		if (versionAndPlanNum == -1 || checkNum == -1 || versionAndPlanNum == 65535 || checkNum == 65535) {
			t = 2;
			versionAndPlanNum = readVersionAndPlanNum3();
			checkNum = readCheckNum3();
		}
		if (versionAndPlanNum == -1 || checkNum == -1 || versionAndPlanNum == 65535 || checkNum == 65535) {
			t = 3;
			versionAndPlanNum = readVersionAndPlanNum4();
			checkNum = readCheckNum4();
		}
		if (versionAndPlanNum == -1 || checkNum == -1 || versionAndPlanNum == 65535 || checkNum == 65535) {
			return null;
		}

		int versionNum = versionAndPlanNum >> 9;
		long tempPlanNum = versionAndPlanNum & 255;// 去最后8位
		boolean isTopDigit = ((versionAndPlanNum >> 8) & 1) == 0;
		int planRowCount = 0;
		long planNum = tempPlanNum;
		if (!isTopDigit) {
			do {
				tempPlanNum = readPlanNumAndIsTopDigit(planRowCount, t);
				planNum = ((tempPlanNum & 0x7fff) << (8 + planRowCount * 15)) | planNum;
				isTopDigit = ((tempPlanNum >> 15) & 1) == 0;
				planRowCount++;
				if (planRowCount >= 4)
					break;
			} while (!isTopDigit);
		}

		// int backup = readBackup(t);备用值，暂时无意义，需要读取是解除注释

		int bdataLength = readLength(t);

		byte[] rules = readPlans(planNum);
		if (rules == null)
			return null;
		int rulesCount = rules.length >> 13;
		int[][][] rulePixs = new int[rulesCount][][];
		for (int i = rulesCount - 1; i >= 0; i--) {
			int[][] pixs = readPlan(rules, i);
			rulePixs[i] = changeRuleSize(pixs, codeWidth, planRowCount);
		}

		HdcodeInfo hdcodeInfo = new HdcodeInfo();

		hdcodeInfo.PlanNum = planNum;
		hdcodeInfo.CheckNum = checkNum;
		hdcodeInfo.Version = versionNum;
		hdcodeInfo.RotateTime = t;
		hdcodeInfo.DataLength = bdataLength;
		hdcodeInfo.RulePixs = rulePixs;

		return hdcodeInfo;
	}

	private int readVersionAndPlanNum()// 解读方案号
	{
		int num = 0, num2 = 0;
		int p = codeWidth * 2 + 10;
		int p2 = codeWidth * 3 + 10;
		for (int i = 10; i < 26; i++) {
			num <<= 1;
			num2 <<= 1;
			if (pixs[p] == -1)
				num += 1;
			if (pixs[p2] == -1)
				num2 += 1;
			p++;
			p2++;
		}
		if (num == num2)
			return num;
		else
			return -1;
	}

	private int readVersionAndPlanNum2()// 解读方案号
	{
		int num = 0;
		int num2 = 0;
		int p = codeWidth * 11 - 3;
		int p2 = codeWidth * 11 - 4;
		for (int i = 10; i < 26; i++) {
			num <<= 1;
			num2 <<= 1;
			if (pixs[p] == -1)
				num += 1;
			if (pixs[p2] == -1)
				num2 += 1;
			p += codeWidth;
			p2 += codeWidth;
		}
		if (num == num2)
			return num;
		else
			return -1;
	}

	private int readVersionAndPlanNum3()// 解读方案号
	{
		int num = 0;
		int num2 = 0;
		int p = codeWidth * (codeWidth - 2) - 11;
		int p2 = codeWidth * (codeWidth - 3) - 11;
		for (int i = 10; i < 26; i++) {
			num <<= 1;
			num2 <<= 1;
			if (pixs[p] == -1)
				num += 1;
			if (pixs[p2] == -1)
				num2 += 1;
			p--;
			p2--;
		}

		if (num == num2)
			return num;
		else
			return -1;
	}

	private int readVersionAndPlanNum4()// 解读方案号
	{
		int num = 0;
		int num2 = 0;
		int p = codeWidth * (codeWidth - 11) + 2;
		int p2 = codeWidth * (codeWidth - 11) + 3;
		for (int i = 10; i < 26; i++) {
			num <<= 1;
			num2 <<= 1;
			if (pixs[p] == -1)
				num += 1;
			if (pixs[p2] == -1)
				num2 += 1;

			p -= codeWidth;
			p2 -= codeWidth;
		}

		if (num == num2)
			return num;
		else
			return -1;
	}

	private int readCheckNum()// 解读校验号
	{
		int num = 0, num2 = 0;
		int p = 10;
		int p2 = codeWidth + 10;
		for (int i = 10; i < 26; i++) {
			num <<= 1;
			num2 <<= 1;
			if (pixs[p] == -1)
				num += 1;
			if (pixs[p2] == -1)
				num2 += 1;
			p++;
			p2++;
		}
		if (num == num2)
			return num;
		else
			return -1;
	}

	private int readCheckNum2()// 解读校验号
	{
		int num = 0, num2 = 0;
		int p = codeWidth * 11 - 1;
		int p2 = codeWidth * 11 - 2;
		for (int i = 10; i < 26; i++) {
			num <<= 1;
			num2 <<= 1;
			if (pixs[p] == -1)
				num += 1;
			if (pixs[p2] == -1)
				num2 += 1;
			p += codeWidth;
			p2 += codeWidth;
		}
		if (num == num2)
			return num;
		else
			return -1;
	}

	private int readCheckNum3()// 解读校验号
	{
		int num = 0, num2 = 0;
		int p = codeWidth * codeWidth - 11;
		int p2 = codeWidth * (codeWidth - 1) - 11;
		for (int i = 10; i < 26; i++) {
			num <<= 1;
			num2 <<= 1;
			if (pixs[p] == -1)
				num += 1;
			if (pixs[p2] == -1)
				num2 += 1;
			p--;
			p2--;
		}
		if (num == num2)
			return num;
		else
			return -1;
	}

	private int readCheckNum4()// 解读校验号
	{
		int num = 0, num2 = 0;
		int p = codeWidth * (codeWidth - 11);
		int p2 = codeWidth * (codeWidth - 11) + 1;
		for (int i = 10; i < 26; i++) {
			num <<= 1;
			num2 <<= 1;
			if (pixs[p] == -1)
				num += 1;
			if (pixs[p2] == -1)
				num2 += 1;

			p -= codeWidth;
			p2 -= codeWidth;
		}
		if (num == num2)
			return num;
		else
			return -1;
	}

	private long readPlanNumAndIsTopDigit(int planRowCount, int t) {
		int num = 0;
		int p;
		if (t == 0) {
			p = codeWidth * (codeWidth - 1 - planRowCount) + 10;
			for (int i = 10; i < 26; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p++;
			}
		} else if (t == 1) {
			p = codeWidth * 10 + planRowCount;
			for (int i = 10; i < 26; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p += codeWidth;
			}
		} else if (t == 2) {
			p = codeWidth * planRowCount + codeWidth - 11;
			for (int i = 10; i < 26; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p--;
			}
		} else {
			p = codeWidth * (codeWidth - 10) - 1 - planRowCount;
			for (int i = 10; i < 26; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;

				p -= codeWidth;
			}
		}
		return num;
	}

	private int readLength(int t) {
		int num = 0;
		int p;
		if (t == 0) {
			p = codeWidth * 4 + 10;
			for (int i = 10; i < 26; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p++;
			}
		} else if (t == 1) {
			p = codeWidth * 11 - 5;
			for (int i = 10; i < 26; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p += codeWidth;
			}
		} else if (t == 2) {
			p = codeWidth * (codeWidth - 4) - 11;
			for (int i = 10; i < 26; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p--;
			}
		} else {
			p = codeWidth * (codeWidth - 11) + 4;
			for (int i = 10; i < 26; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;

				p -= codeWidth;
			}
		}
		return num;
	}

	/// <summary>
	/// 备用空间，目前其中数据无意义
	/// </summary>
	/// <param name="t"></param>
	/// <returns>备用空间中的值</returns>
	private int readBackup(int t) {
		int num = 0;
		int p;
		if (t == 0) {
			p = codeWidth * 5 + 10;
			for (int i = 10; i < 20; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p++;
			}
		} else if (t == 1) {
			p = codeWidth * 11 - 6;
			for (int i = 10; i < 20; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p += codeWidth;
			}
		} else if (t == 2) {
			p = codeWidth * (codeWidth - 5) - 11;
			for (int i = 10; i < 20; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;
				p--;
			}
		} else {
			p = codeWidth * (codeWidth - 11) + 5;
			for (int i = 10; i < 20; i++) {
				num <<= 1;
				if (pixs[p] == -1)
					num += 1;

				p -= codeWidth;
			}
		}
		return num;
	}

	private void removeLocation(int t) {
		int pzs = 0;
		int pys = codeWidth - 10;
		int pzx = codeWidth * (codeWidth - 10);
		int pyx = (codeWidth + 1) * (codeWidth - 10);
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				pixs[pzs] = -128;
				pzs++;
				pixs[pys] = -128;
				pys++;
				pixs[pzx] = -128;
				pzx++;
				pixs[pyx] = -128;
				pyx++;
			}
			pzs += (codeWidth - 10);
			pys += (codeWidth - 10);
			pzx += (codeWidth - 10);
			pyx += (codeWidth - 10);
		}
		if (t == 0)
			for (int i = 10; i < 26; i++) // 方向1
			{
				pixs[i] = -128;
				pixs[i + codeWidth] = -128;
				pixs[i + codeWidth + codeWidth] = -128;
				pixs[i + codeWidth + codeWidth + codeWidth] = -128;
				pixs[i + codeWidth + codeWidth + codeWidth + codeWidth] = -128;
				pixs[i + codeWidth + codeWidth + codeWidth + codeWidth + codeWidth] = -128;
			}
		else if (t == 1)
			for (int i = 10; i < 26; i++) // 方向2
			{
				pixs[codeWidth * (1 + i) - 1] = -128;
				pixs[codeWidth * (1 + i) - 2] = -128;
				pixs[codeWidth * (1 + i) - 3] = -128;
				pixs[codeWidth * (1 + i) - 4] = -128;
				pixs[codeWidth * (1 + i) - 5] = -128;
				pixs[codeWidth * (1 + i) - 6] = -128;
			}
		else if (t == 2)
			for (int i = 10; i < 26; i++) // 方向1
			{
				pixs[codeWidth * codeWidth - i - 1] = -128;
				pixs[codeWidth * (codeWidth - 1) - i - 1] = -128;
				pixs[codeWidth * (codeWidth - 2) - i - 1] = -128;
				pixs[codeWidth * (codeWidth - 3) - i - 1] = -128;
				pixs[codeWidth * (codeWidth - 4) - i - 1] = -128;
				pixs[codeWidth * (codeWidth - 5) - i - 1] = -128;
			}
		else if (t == 3)
			for (int i = 10; i < 26; i++) // 方向2
			{
				pixs[codeWidth * (codeWidth - i - 1)] = -128;
				pixs[codeWidth * (codeWidth - i - 1) + 1] = -128;
				pixs[codeWidth * (codeWidth - i - 1) + 2] = -128;
				pixs[codeWidth * (codeWidth - i - 1) + 3] = -128;
				pixs[codeWidth * (codeWidth - i - 1) + 4] = -128;
				pixs[codeWidth * (codeWidth - i - 1) + 5] = -128;
			}
	}

	private byte[] readPlans(long planNum) {
		byte[] data = hdCodeRules.getPlan(planNum);
		return data;
	}

	private int[][] readPlan(byte[] rules, int index)// 将数据库字节翻译为图片并显示
	{
		byte[] data = rules;
		int[][] pixs = new int[256][256];

		if (data != null) {
			int p = 0;
			for (int i = 65536 * index, j = 0; j < 65536; i++, j++) {
				pixs[p >> 8][p & 255] = ((data[i >> 3] >> (7 - i % 8)) & 0x1) * 254 + 1;

				p++;
			}

			return pixs;
		} else {
			return null;
		}
	}

	/// <summary>
	/// 变更rule到标准码大小
	/// </summary>
	/// <param name="newWidth">新的边长</param>
	/// <param name="newSize">新的容量</param>
	/// <returns></returns>
	private int[][] changeRuleSize(int[][] pixs, int newWidth, int planRowCount)//w为新宽度
    {
		int[][] newPixs = new int[newWidth][newWidth];
        int a, b;
        for (int i = 0; i < newWidth; i++) {
            a = i * 256 / newWidth;
            for (int j = 0; j < newWidth; j++) {
                b = j * 256 / newWidth;
                newPixs[i][j] = pixs[a][b];
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                newPixs[i][j] = -128;
                newPixs[i][j + newWidth - 10] = -128;
                newPixs[i + newWidth - 10][j] = -128;
                newPixs[i + newWidth - 10][j + newWidth - 10] = -128;
            }
        }
        for (int i = 0; i < 16; i++) {
            newPixs[0][i + 10] = -128;
            newPixs[1][i + 10] = -128;
            newPixs[2][i + 10] = -128;
            newPixs[3][i + 10] = -128;
            newPixs[4][i + 10] = -128;
            newPixs[5][i + 10] = -128;
        }
        for (int i = 0; i < planRowCount; i++)
        {
            for (int j = 0; j < 16; j++)
            {
                newPixs[newWidth - i - 1][ j + 10] = -128;
            }
        }
        return newPixs;
    }
}
