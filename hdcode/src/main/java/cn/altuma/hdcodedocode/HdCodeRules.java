package cn.altuma.hdcodedocode;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Looper;
import android.util.Base64;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


/**
 * Created by tuma on 2018/3/29.
 */

class HdCodeRules {
    private static byte[] plan = null;
    private static long currentPlanNum = -1;

    private final String HdRulePath= Environment.getExternalStorageDirectory().getPath()+"/rules/ruleDb.db";
    public HdCodeRules()
    {
        createSqlite(HdRulePath);
    }

    private static boolean threadIsRun=false;
    public byte[]getPlan(long planNum) {
        if(currentPlanNum!=planNum||plan==null)
        {
            currentPlanNum = planNum;
            plan = getPlanFromSqlite(currentPlanNum);
            return plan;
        }
        else
            return plan;

    }

    private void createSqlite(String HdRulePath)//创建sqlite数据库
    {
        File sd = Environment.getExternalStorageDirectory();
        String path = sd.getPath() + "/rules";
        File fileRuleDir = new File(path);
        if (!fileRuleDir.exists())
            fileRuleDir.mkdir();
        File file=new File(HdRulePath);
        if(!file.exists())
        {
            SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(HdRulePath, null);
            database.execSQL("CREATE TABLE [rules] (planNum BIGINT PRIMARY KEY,data IMAGE NOT NULL, time DATETIME )");
            database.close();
        }
    }

    private byte[]getPlanFromSqlite(long planNum)
    {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(HdRulePath, null, SQLiteDatabase.OPEN_READWRITE); //DatabaseOpenFlags.OpenReadwrite);
        Cursor cursor= database.query("rules", new String[] { "data" }, "planNum=?", new String[] { planNum+"" }, null, null, null);
        if (cursor.moveToNext())
        {
            byte[] data = cursor.getBlob(0);
            cursor.close();
            database.close();
            return data;
        }

        else
        {
            if(Looper.getMainLooper().getThread()== Thread.currentThread()) {
                plan = null;
                if (threadIsRun)
                    return null;
                else {
                    threadIsRun = true;
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            plan = getPlanFromSqlserver(currentPlanNum);
                            threadIsRun = false;
                        }
                    });
                    thread.start();

                    int index = 0;
                    while (plan == null) {
                        try {
                            Thread.sleep(100);
                            index++;
                            if (index > 100)
                                break;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
            else {
                plan = getPlanFromSqlserver(currentPlanNum);
                threadIsRun = false;
            }

            byte[] data = plan;//getPlanFromSqlserver(planNum);
            if(data==null||data.length<8192)
                return null;
            DateFormat dateFormatter=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            TimeZone pst = TimeZone.getTimeZone("Etc/GMT+0");

            Date curDate = new Date();
            dateFormatter.setTimeZone(pst);
            String str=dateFormatter.format(curDate);//这就是我们想要获取的值
            ContentValues contentValue = new ContentValues();
            contentValue.put("planNum", planNum);
            contentValue.put("data", data);
            contentValue.put("time", str);
            database.insert("rules", null, contentValue);
            database.close();
            return data;
        }
    }

    public byte[] getPlanFromSqlserver(long planNum)
    {
        Map<String, String> para = new HashMap<>();
        para.put("planNum", planNum+"");
        try {
            String result = MyhttpRequest.net("http://www.altuma.cn:10003/webapi/getrule", para, "GET");
            byte[]data= Base64.decode(result, Base64.DEFAULT);
            return data;
        } catch (Exception e) {
            return null;
        }
    }
}
