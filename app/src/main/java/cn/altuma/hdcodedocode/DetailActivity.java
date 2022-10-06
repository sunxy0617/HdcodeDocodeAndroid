package cn.altuma.hdcodedocode;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import cn.altuma.hdcode.CameraActivity;

public class DetailActivity extends AppCompatActivity {

    Button butScan;
    TextView textMessage;
    TextToSpeech textToSpeech;
    private Button butSpeech;
    public static boolean isOpenByMain = false;
    final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        butScan = findViewById(R.id.butScan);
        textMessage = findViewById(R.id.textMessage);
        butSpeech = findViewById(R.id.butSpeech);


        butScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DetailActivity.this, CameraActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        butSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = textMessage.getText().toString();
                if (message != null || message.length() > 0)
                    textToSpeech.speak(message, textToSpeech.QUEUE_ADD, null);
            }
        });

        if (isOpenByMain) {
            isOpenByMain = false;
            Intent intent = new Intent(DetailActivity.this, CameraActivity.class);
            startActivityForResult(intent, REQUEST_CODE);
        }

        initTextToSpeech();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_CODE&& resultCode == RESULT_OK) {
            final String message = data.getStringExtra("hdcode");;
            textMessage.setText(message);

            if (message.toLowerCase().startsWith("http://") || message.toLowerCase().startsWith("https://")) {
//                AlertDialog alertDialog = new AlertDialog.Builder(this)
//                        .setTitle("网址提示")
//                        .setMessage("发现网址，是否打开？")
//                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                OpenBrowser(message);
//                            }
//                        })
//                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                            }
//                        })
//                        .create();
//                alertDialog.show();
                OpenBrowser(message);
            }
        }
    }

    private void OpenBrowser(String url) {
        try {
            if (url.length() < 5) {
                Toast.makeText(this, "地址无效", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse(url);
            intent.setData(content_url);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "地址无效", Toast.LENGTH_SHORT).show();
        }
    }

    private void initTextToSpeech(){
        textToSpeech = new TextToSpeech(DetailActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status==TextToSpeech.SUCCESS){
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE
                            && result != TextToSpeech.LANG_AVAILABLE){
                        //Toast.makeText(DetailActivity.this, "TTS暂时不支持这种语音的朗读！",Toast.LENGTH_SHORT).show();
                        butSpeech.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }
}
