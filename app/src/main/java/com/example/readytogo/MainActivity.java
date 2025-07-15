package com.example.readytogo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    Button btnStartStop;
    Button btnSelectBgm;
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartStop = findViewById(R.id.btnStartStop);
        btnSelectBgm = findViewById(R.id.btnSelectBgm);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar now = Calendar.getInstance();
                int hour24 = now.get(Calendar.HOUR_OF_DAY); // 0~23
                int hour12 = now.get(Calendar.HOUR); // 0~11
                if (hour12 == 0) hour12 = 12; //0시는 12시로

                int minute = now.get(Calendar.MINUTE);

                String amPm = now.get(Calendar.AM_PM) == Calendar.AM ? "오전" : "오후";

                String timeText = "지금은 " + amPm +" "+ hour12 + "시 " + minute + "분입니다";
                tts.speak(timeText, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnSelectBgm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "BGM 선택 기능은 아직 개발 중입니다!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
