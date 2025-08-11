package cn.altuma.hdcodedocode;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button myButton = findViewById(R.id.myButton);
        myButton.setOnClickListener(v -> {
            DetailActivity.isOpenByMain = true;
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            startActivity(intent);

        });

        findViewById(R.id.textAbout).setOnClickListener(v -> {
            Intent intent=new Intent(this,AboutActivity.class);
            startActivity(intent);
        });
    }
}
