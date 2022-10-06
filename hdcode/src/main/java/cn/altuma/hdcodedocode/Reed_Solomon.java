package cn.altuma.hdcodedocode;

/**
 * Created by tuma on 2018/3/29.
 */

class Reed_Solomon
{
    int mm, nn = 1, tt, kk;
    int[] pp, alpha_to, index_of, gg, recd, bb, data;
    /// <summary>
    /// 每2^m-1个字节中，允许t个错误
    /// </summary>
    /// <param name="m">m为伽罗域长度</param>
    /// <param name="t">允许的最大错误量</param>
    public Reed_Solomon(int m, int t)
    {
        mm = m;
        nn = (1 << mm) - 1;
        tt = t;
        kk = nn - (t << 1);
        if (m == 8)
            pp = new int[] { 1, 0, 1, 1, 1, 0, 0, 0, 1 };//长度为mm+1；
        else if (m == 10)
            pp = new int[] { 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1 };
        else if (m == 12)
            pp = new int[] { 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1 };
        else if (m == 14)
            pp = new int[] { 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1 };
        else if (m == 16)
            pp = new int[] { 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1 };
        else
            pp = null;
        alpha_to = new int[nn + 1];
        index_of = new int[nn + 1];
        gg = new int[nn - kk + 1];
        recd = new int[nn];
        data = new int[kk];
        bb = new int[nn - kk];

        generate_gf();
        gen_poly();
    }

    public int[] RSEncode(byte[] bdata)
    {
        int[] data = bytesToMultiBand(bdata, mm);

        int[] rscode = encode_rs(data);
        return rscode;
    }

    public byte[] RSDecode(int[] rscode)
    {
        int[] data = decode_rs(rscode);
        byte[] bdata = multiBandToBytes(data, mm);

        int length = 0;
        for (byte b : bdata)
        {
            if (b != 0)
                length++;
            else
                break;
        }
        byte[] newb = new byte[length];
        for (int i = 0; i < length; i++)
        {
            newb[i] = bdata[i];
        }
        bdata = null;

        return newb;
    }

    private void generate_gf()// 生成GF(2^m)空间
    {
        int i, mask;
        mask = 1;
        alpha_to[mm] = 0;
        for (i = 0; i < mm; i++)
        {
            alpha_to[i] = mask;
            index_of[alpha_to[i]] = i;
            if (pp[i] != 0)
                alpha_to[mm] ^= mask;
            mask <<= 1;
        }
        index_of[alpha_to[mm]] = mm;
        mask >>= 1;
        for (i = mm + 1; i < nn; i++)
        {
            if (alpha_to[i - 1] >= mask)
                alpha_to[i] = alpha_to[mm] ^ ((alpha_to[i - 1] ^ mask) << 1);
            else
                alpha_to[i] = alpha_to[i - 1] << 1;
            index_of[alpha_to[i]] = i;
        }
        index_of[0] = -1;
    }

    private void gen_poly()//生成---生成多项式
    {
        int i, j;
        gg[0] = 2;    /* primitive element alpha = 2  for GF(2**mm)  */
        gg[1] = 1;    /* g(x) = (X+alpha) initially */
        for (i = 2; i <= nn - kk; i++)
        {
            gg[i] = 1;
            for (j = i - 1; j > 0; j--)
                if (gg[j] != 0)
                    gg[j] = gg[j - 1] ^ alpha_to[(index_of[gg[j]] + i) % nn];
                else
                    gg[j] = gg[j - 1];
            gg[0] = alpha_to[(index_of[gg[0]] + i) % nn];     /* gg[0] can never be zero */
        }
        for (i = 0; i <= nn - kk; i++)
            gg[i] = index_of[gg[i]];
    }

    private int[] decode_rs(int[] recd)//解码
    {
        int i, j, u, q;
            int[][] elp = new int[nn - kk + 2][ nn - kk];
        int[] d = new int[nn - kk + 2], l = new int[nn - kk + 2], u_lu = new int[nn - kk + 2], s = new int[nn - kk + 1];
        int count = 0;
        boolean syn_error = false;
        int[] root = new int[tt], loc = new int[tt], z = new int[tt + 1], err = new int[nn], reg = new int[tt + 1];
            /* first form the syndromes */
        for (i = 0; i < nn; i++) //转换成GF空间的alpha幂次
            if (recd[i] == -1)
                recd[i] = 0;
            else
                recd[i] = index_of[recd[i]];

        for (i = 1; i <= nn - kk; i++)
        {
            s[i] = 0;
            for (j = 0; j < nn; j++)
                if (recd[j] != -1)
                    s[i] ^= alpha_to[(recd[j] + i * j) % nn];      /* recd[j] in index form */
                /* convert syndrome from polynomial form to index form  */
            if (s[i] != 0)
                syn_error = true;        /* set flag if non-zero syndrome => error */
            s[i] = index_of[s[i]];
        }
        if (syn_error)       /* if errors, try and correct */
        {
            d[0] = 0;           /* index form */
            d[1] = s[1];        /* index form */
            elp[0][ 0] = 0;      /* index form */
            elp[1][0] = 1;      /* polynomial form */
            for (i = 1; i < nn - kk; i++)
            {
                elp[0][ i] = -1;   /* index form */
                elp[1][ i] = 0;   /* polynomial form */
            }
            l[0] = 0;
            l[1] = 0;
            u_lu[0] = -1;
            u_lu[1] = 0;
            u = 0;
            do
            {
                u++;
                if (d[u] == -1)
                {
                    l[u + 1] = l[u];
                    for (i = 0; i <= l[u]; i++)
                    {
                        elp[u + 1][ i] = elp[u][ i];
                        elp[u][ i] = index_of[elp[u][ i]];
                    }
                }
                else
                    /* search for words with greatest u_lu[q] for which d[q]!=0 */
                {
                    q = u - 1;
                    while ((d[q] == -1) && (q > 0))
                        q--;
                        /* have found first non-zero d[q]  */
                    if (q > 0)
                    {
                        j = q;
                        do
                        {
                            j--;
                            if ((d[j] != -1) && (u_lu[q] < u_lu[j]))
                                q = j;
                        } while (j > 0);
                    };
                        /* have now found q such that d[u]!=0 and u_lu[q] is maximum */
                        /* store degree of new elp polynomial */
                    if (l[u] > l[q] + u - q)
                        l[u + 1] = l[u];
                    else
                        l[u + 1] = l[q] + u - q;
                        /* form new elp(x) */
                    for (i = 0; i < nn - kk; i++)
                        elp[u + 1][ i] = 0;
                    for (i = 0; i <= l[q]; i++)
                        if (elp[q][ i] != -1)
                    elp[u + 1][ i + u - q] = alpha_to[(d[u] + nn - d[q] + elp[q][ i]) % nn];
                    for (i = 0; i <= l[u]; i++)
                    {
                        elp[u + 1][ i] ^= elp[u][ i];
                        elp[u][ i] = index_of[elp[u][ i]];  /*convert old elp value to index*/
                    }
                }
                u_lu[u + 1] = u - l[u + 1];
                    /* form (u+1)th discrepancy */
                if (u < nn - kk)    /* no discrepancy computed on last iteration */
                {
                    if (s[u + 1] != -1)
                        d[u + 1] = alpha_to[s[u + 1]];
                    else
                        d[u + 1] = 0;
                    for (i = 1; i <= l[u + 1]; i++)
                        if ((s[u + 1 - i] != -1) && (elp[u + 1][ i] != 0))
                    d[u + 1] ^= alpha_to[(s[u + 1 - i] + index_of[elp[u + 1][ i]]) % nn];
                    d[u + 1] = index_of[d[u + 1]];    /* put d[u+1] into index form */
                }
            } while ((u < nn - kk) && (l[u + 1] <= tt));
            u++;
            if (l[u] <= tt)         /* can correct error */
            {
                    /* put elp into index form */
                for (i = 0; i <= l[u]; i++)
                    elp[u][ i] = index_of[elp[u][ i]];
                    /* find roots of the error location polynomial */
                    /*求错误位置多项式的根*/
                for (i = 1; i <= l[u]; i++)
                    reg[i] = elp[u][ i];
                count = 0;
                for (i = 1; i <= nn; i++)
                {
                    q = 1;
                    for (j = 1; j <= l[u]; j++)
                        if (reg[j] != -1)
                        {
                            reg[j] = (reg[j] + j) % nn;
                            q ^= alpha_to[reg[j]];
                        };
                    if (q == 0)  //if(!q)      /* store root and error location number indices */
                    {
                        root[count] = i;
                        loc[count] = nn - i;
                        count++;
                    };
                };
                if (count == l[u])    /* no. roots = degree of elp hence <= tt errors */
                {/* form polynomial z(x) */
                    for (i = 1; i <= l[u]; i++)        /* Z[0] = 1 always - do not need */
                    {
                        if ((s[i] != -1) && (elp[u][ i] != -1))
                        z[i] = alpha_to[s[i]] ^ alpha_to[elp[u][ i]];
                            else if ((s[i] != -1) && (elp[u][ i] == -1))
                        z[i] = alpha_to[s[i]];
                            else if ((s[i] == -1) && (elp[u][ i] != -1))
                        z[i] = alpha_to[elp[u][ i]];
                            else
                        z[i] = 0;
                        for (j = 1; j < i; j++)
                            if ((s[j] != -1) && (elp[u][ i - j] != -1))
                        z[i] ^= alpha_to[(elp[u][ i - j] + s[j]) % nn];
                        z[i] = index_of[z[i]];         /* put into index form */
                    };
                        /* evaluate errors at locations given by error location numbers loc[i] */
                        /*计算错误图样*/
                    for (i = 0; i < nn; i++)
                    {
                        err[i] = 0;       /* convert recd[] to polynomial form */
                        if (recd[i] != -1)        /* convert recd[] to polynomial form */
                            recd[i] = alpha_to[recd[i]];
                        else
                            recd[i] = 0;
                    }
                    for (i = 0; i < l[u]; i++)    /* compute numerator of error term first */
                    {
                        err[loc[i]] = 1;       /* accounts for z[0] */
                        for (j = 1; j <= l[u]; j++)
                            if (z[j] != -1)
                                err[loc[i]] ^= alpha_to[(z[j] + j * root[i]) % nn];
                        if (err[loc[i]] != 0)
                        {
                            err[loc[i]] = index_of[err[loc[i]]];
                            q = 0;     /* form denominator of error term */
                            for (j = 0; j < l[u]; j++)
                                if (j != i)
                                    q += index_of[1 ^ alpha_to[(loc[j] + root[i]) % nn]];
                            q = q % nn;
                            err[loc[i]] = alpha_to[(err[loc[i]] - q + nn) % nn];
                            recd[loc[i]] ^= err[loc[i]];  /*recd[i] must be in polynomial form */
                        }
                    }
                }
                else    /* no. roots != degree of elp => >tt errors and cannot solve */
                {	/*错误太多，无法更正*/
                    for (i = 0; i < nn; i++)        /* convert recd[] to polynomial form*/
                        if (recd[i] != -1)        /* convert recd[] to polynomial form*/
                            recd[i] = alpha_to[recd[i]];
                        else
                            recd[i] = 0;
                }
            }
            else         /* elp has degree has degree >tt hence cannot solve */
            {	/*错误太多，无法更正*/
                for (i = 0; i < nn; i++)       /* could return error flag if desired */
                    if (recd[i] != -1)        /* convert recd[] to polynomial form*/
                        recd[i] = alpha_to[recd[i]];
                    else
                        recd[i] = 0;
            }
        }
        else       /* no non-zero syndromes => no errors: output received codeword */
            for (i = 0; i < nn; i++)
            {
                if (recd[i] != -1)        /* convert recd[] to polynomial form*/
                    recd[i] = alpha_to[recd[i]];
                else
                    recd[i] = 0;
            }
        int[] data = new int[kk];
        for (i = 0; i < kk; i++)
        {
            data[i] = recd[i + nn - kk];
        }
        return data;
    }

    private byte[] multiBandToBytes(int[] data, int m)//上个方法的逆过程
    {
        int li = data.length;
        int li2 = li * m;//data转为二进制后的长度
        int lb = li2 >> 3;
        int lb2 = lb << 3;//2^m进制数转为二进制后的长度，大于等于lb2；
        if ((lb2) % m != 0) li++;
        byte[] bdata = new byte[lb];
        int ib = 0, ii = 0;
        int i;//
        do
        {
            i = data[ii / m] >> (m - ii % m - 1) & 1;
            ii++;
            bdata[ib >> 3] <<= 1;
            bdata[ib >> 3] += (byte)i;
            ib++;

        } while (ib < lb2);
        return bdata;
    }

    private int[] encode_rs(int[] data)//编码
    {
        int i, j;
        int feedback;
        for (i = 0; i < nn - kk; i++) bb[i] = 0;
        for (i = kk - 1; i >= 0; i--)
        {
            //逐步的将下一步要减的，存入bb(i)
            feedback = index_of[data[i] ^ bb[nn - kk - 1]];
            if (feedback != -1)
            {
                for (j = nn - kk - 1; j > 0; j--)
                    if (gg[j] != -1)
                        bb[j] = (bb[j - 1] ^ alpha_to[(gg[j] + feedback) % nn]);		//plus = ^
                    else
                        bb[j] = bb[j - 1];
                bb[0] = alpha_to[(gg[0] + feedback) % nn];
            }
            else
            {
                for (j = nn - kk - 1; j > 0; j--)
                    bb[j] = bb[j - 1];
                bb[0] = 0;
            }
        }

        for (i = 0; i < nn - kk; i++)
            recd[i] = bb[i];
        for (i = 0; i < kk; i++)
        {
            recd[i + nn - kk] = data[i];
        }
        return recd;
    }

    private int[] bytesToMultiBand(byte[] bdata, int m)//byte数组变为2^m进制数组,每一位写入int[]中一位表示，所以m不要大于31
    {
        int lb = bdata.length;
        int lb2 = lb << 3;//bytes转为二进制后的长度
        int li = (lb2) / m;
        if ((lb2) % m != 0) li++;
        int li2 = li * m;//2^m进制数转为二进制后的长度，大于等于lb2；
        int[] data = new int[kk];//////////
        int ib = 0, ii = 0;
        int i;//
        do
        {
            if (ib < lb2)
            {
                i = bdata[ib >> 3] >> (7 - ib % 8) & 1;
                ib++;
            }
            else
                i = 0;
            data[ii / m] <<= 1;
            data[ii / m] += i;
            ii++;

        } while (ii < li2);
        return data;
    }

}
