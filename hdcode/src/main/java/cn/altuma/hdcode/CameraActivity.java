package cn.altuma.hdcode;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class CameraActivity extends AppCompatActivity implements Camera.PreviewCallback {
    private static final String TAG = "CameraActivity";
    public static String message;
    final int PHOTO_REQUEST_CODE = 101;
    private boolean cameraIsRun = false;//当前相机工作状态
    private android.hardware.Camera camera;
    FrameLayout parentLayout;
    SurfaceView surfView;
    Button butOpenAlbum;
    LinearLayout centerLayout;
    ImageView imgMove = null;

    private SoundPool soundPool;
    private int soundId;//音频id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        parentLayout = findViewById(R.id.parentLayout);
        surfView = findViewById(R.id.surfViewShow);
        butOpenAlbum = findViewById(R.id.butOpenAlbum);
        centerLayout = findViewById(R.id.centerLayout);
        imgMove = findViewById(R.id.imageMove);

        butOpenAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PHOTO_REQUEST_CODE);
            }
        });

        initPermission();

        Hdcode.hdRulePath = Objects.requireNonNull(this.getExternalFilesDir(null)).getAbsolutePath();
    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET};
            List<String> mPermissionList = new ArrayList<>();


            mPermissionList.clear();//清空已经允许的没有通过的权限
            //逐个判断是否还有未通过的权限
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                        PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);//添加还未授予的权限到mPermissionList中
                }
            }
            //申请权限
            if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
                ActivityCompat.requestPermissions(this, permissions, 100);
            }
        }
    }

    private void initSoundPool() {
        if (Build.VERSION.SDK_INT >= 21) {
            SoundPool.Builder builder = new SoundPool.Builder();
            //传入音频的数量
            builder.setMaxStreams(1);
            //AudioAttributes是一个封装音频各种属性的类
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            //设置音频流的合适属性
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            builder.setAudioAttributes(attrBuilder.build());
            soundPool = builder.build();
        } else {
            //第一个参数是可以支持的声音数量，第二个是声音类型，第三个是声音品质
            soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        }
        soundId = soundPool.load(this, R.raw.scan, 1);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            int width = parentLayout.getWidth();
            int height = parentLayout.getHeight();
            int min = width < height ? width : height;
            int centerWidth = min / 4 * 3;//中间滑动框边长
            LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) centerLayout.getLayoutParams();
            param.height = centerWidth;
            param.width = centerWidth;
            centerLayout.setLayoutParams(param);

            TranslateAnimation animation = new TranslateAnimation(TranslateAnimation.ABSOLUTE, 0, TranslateAnimation.ABSOLUTE, 0, TranslateAnimation.RELATIVE_TO_PARENT, 0, TranslateAnimation.RELATIVE_TO_PARENT, 0.9f);
            animation.setDuration(2500);
            animation.setRepeatCount(Animation.INFINITE);
            imgMove.setAnimation(animation);

            OpenCamera();
        } else {
            CloseCamera();
        }
    }

    private void OpenCamera() {
        if (cameraIsRun)
            return;
        try {
            camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            int w = 640, h = 480;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)//旧手机对640*480以外像素支持不好
            {
                for (Camera.Size size : sizes) { //判断是否支持960：540返回值，不支持就用640：480
                    if (size.width == 960 && size.height == 540) {
                        w = 960;
                        h = 540;
                        break;
                    }
                }

                for (Camera.Size size : sizes) { //判断是否支持960：540返回值，不支持就用640：480
                    if (size.width == 1280 && size.height == 720) {
                        w = 1280;
                        h = 720;
                        break;
                    }
                }
            }
            parameters.setPreviewSize(w, h);

            camera.setParameters(parameters);
            camera.setPreviewDisplay(surfView.getHolder());
            camera.startPreview();
            camera.setPreviewCallback(this);
            camera.setDisplayOrientation(90);
            camera.autoFocus(null);
            cameraIsRun = true;
            initSoundPool();

        } catch (Exception e) {
            final String msg = e.getMessage();
            System.out.print(msg);
        }
    }

    private void CloseCamera() {
        if (cameraIsRun) {
            camera.setPreviewCallback(null);//避免camera release 后再次回掉数据
            camera.stopPreview();
            camera.release();
            cameraIsRun = false;

            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        }
    }

    int count = 0;//相机回调次数

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!cameraIsRun)
            return;
        if (data == null)
            return;
        count++;

        if (count % 20 == 0) {
            camera.autoFocus(null);
            count = 0;
        }
        if (count % 8 == 0) {

            int width = camera.getParameters().getPreviewSize().width;
            int height = camera.getParameters().getPreviewSize().height;

            int min = (width < height ? width : height) * 3 / 4;
            int x = (width - min) >> 1;
            int y = (height - min) >> 1;

            x = x >> 1 << 1;
            y = y >> 1 << 1;
            min = min >> 1 << 1;

            byte[] newData = cutYUV(data, width, height, x, y, min, min);

            decodeYUV420(newData, min, min);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == PHOTO_REQUEST_CODE && data != null) {// 打开图片
                Uri uri = data.getData();
                ContentResolver cr = getContentResolver();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(cr.openInputStream(uri), null, options);
                int l = options.outWidth > options.outHeight ? options.outWidth : options.outHeight;
                int i = 1;
                while (l > 1600) {
                    i *= 2;
                    l /= 2;
                }
                BitmapFactory.Options options2 = new BitmapFactory.Options();
                options2.inSampleSize = i;
                Bitmap bmp = BitmapFactory.decodeStream(cr.openInputStream(uri), null, options2);

                int w = bmp.getWidth();
                int h = bmp.getHeight();
                int length = w * h;
                int[] pixs = new int[length];
                bmp.getPixels(pixs, 0, w, 0, 0, w, h);
                String result = Hdcode.decodeImagePixels(pixs, w, h);

                if (result != null) {
                    decodeSuccess(result);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "onActivityResult: " + e.toString());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void decodeYUV420(byte[] data, int width, int height) {
        final byte[] mData = data;
        final int mWidth = width;
        final int mHeight = height;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = Hdcode.decodeYuv420(mData, mWidth, mHeight);
                if (result != null) {
                    decodeSuccess(result);
                }
            }
        }).start();
    }

    boolean isSuccess = false;

    private void decodeSuccess(String result) {
        if (isSuccess)
            return;
        if (result != null) {
            isSuccess = true;
            if (soundPool != null)
                soundPool.play(soundId, 1, 1, 0, 0, 1);
            CloseCamera();
            message = result;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    intent.putExtra("hdcode", message);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }
    }

    private byte[] cutYUV(byte[] data, int srcWidth, int srcHeight, int x, int y, int destWidth, int destHeight) {
        x = x >> 1 << 1;
        y = y >> 1 << 1;
        int destHalfWidth = destWidth >> 1;
        int destHalfHeight = destHeight >> 1;
        destWidth = destHalfWidth << 1;
        destHeight = destHalfHeight << 1;
        int destUVLength = destHalfWidth * destHalfHeight;
        int destLength = destUVLength * 6;
        byte[] destData = new byte[destLength];
        int destIndex = 0;
        for (int i = 0; i < destHeight; i++) {
            int srcIndex = x + (y + i) * srcWidth;
            for (int j = 0; j < destWidth; j++) {
                destData[destIndex++] = data[srcIndex++];
            }
        }

        for (int i = 0; i < destHalfHeight; i++) {
            int srcIndex = x + (srcHeight + y / 2 + i) * srcWidth;
            for (int j = 0; j < destWidth; j++) {
                destData[destIndex++] = data[srcIndex++];
            }
        }
        return destData;
    }

    @Override
    protected void onPause() {
        super.onPause();
        CloseCamera();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        OpenCamera();
    }
}