package com.example.readytogo;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import java.util.Calendar;
import android.widget.AdapterView;


public class MainActivity extends AppCompatActivity {

    Button btnStart;
    Button btnStop;
    Button btnSelectBgm;
    TextToSpeech tts;
    Runnable timeSpeaker;
    Handler handler = new Handler(Looper.getMainLooper());
    Handler tickHandler = new Handler(Looper.getMainLooper());
    Runnable tickRunnable;
    Runnable clockRunnable;
    Handler clockHandler = new Handler(Looper.getMainLooper());
    Spinner spinnerInterval;
    TextView textCurrentTime;
    int speakInterval = 30;     //인터벌 초기설정값

    boolean isStarted = false;

    private AudioTrack lowBeep;
    private AudioTrack highBeep;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textCurrentTime = findViewById(R.id.textCurrentTime);
        btnStart= findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSelectBgm = findViewById(R.id.btnSelectBgm);

        // ① Spinner ID 연결
        spinnerInterval = findViewById(R.id.spinnerInterval);

        // ② 옵션 목록 정의 + Adapter 연결 + 기본값 설정
        String[] intervalOptions = {"5초", "10초", "30초", "60초"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                intervalOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInterval.setAdapter(adapter);
        spinnerInterval.setSelection(2); // 기본값: 30초 선택됨
        spinnerInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                switch (selected) {
                    case "5초":
                        speakInterval = 5;
                        break;
                    case "10초":
                        speakInterval = 10;
                        break;
                    case "30초":
                        speakInterval = 30;
                        break;
                    case "60초":
                        speakInterval = 60;
                        break;
                }
                Toast.makeText(MainActivity.this, selected + " 간격으로 설정됨", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무것도 선택 안했을 경우 무시
            }
        });

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        lowBeep = generateSineWaveTrack(800, 100);   // 저주파 100ms
        highBeep = generateSineWaveTrack(2000, 400); // 고주파 400ms

        btnStart.setOnClickListener(v -> {
            if(!isStarted){
                isStarted = true;
                startTicking();
            }else{
                speakCurrentTime();
            }
        });

        btnStop.setOnClickListener(v-> {
            isStarted = false;
            // 비프음 + 음성 알림 중단
            if (tickHandler != null && tickRunnable != null) {
                tickHandler.removeCallbacks(tickRunnable);
                tickRunnable = null;
            }

            // 혹시 이전에 쓰던 timeSpeaker가 살아있다면 종료
            if (handler != null && timeSpeaker != null) {
                handler.removeCallbacks(timeSpeaker);
                timeSpeaker = null;
            }

            // 사용자에게 알림
            Toast.makeText(MainActivity.this, "음성 안내가 중지되었습니다", Toast.LENGTH_SHORT).show();

        });

        btnSelectBgm.setOnClickListener( v-> {
            Toast.makeText(MainActivity.this, "BGM 선택 기능은 아직 개발 중입니다!", Toast.LENGTH_SHORT).show();
        });

        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeText();
                clockHandler.postDelayed(this, 1000); // 1초마다 반복
            }
        };

        clockHandler.post(clockRunnable); // 처음 한 번 실행
    }

    private void startSpeakingTime(){
        timeSpeaker = new Runnable(){
            @Override
            public void run(){
                speakCurrentTime();
                handler.postDelayed(this, 60 * 1000); //1분마다 반복
            }
        };
        handler.post(timeSpeaker); //처음한번즉시실행
    }

    private void speakCurrentTime(){
        if(tts==null) return; //tts가 null이면 바로 빠져나오기

        Calendar now = Calendar.getInstance();
        int hour24 = now.get(Calendar.HOUR_OF_DAY); // 0~23
        int hour12 = now.get(Calendar.HOUR); // 0~11
        if (hour12 == 0) hour12 = 12; //0시는 12시로

        int minute = now.get(Calendar.MINUTE);

        String amPm = now.get(Calendar.AM_PM) == Calendar.AM ? "오전" : "오후";

        String timeText = "지금은 " + amPm +" "+ hour12 + "시 " + minute + "분입니다";

        try{
            tts.speak(timeText, TextToSpeech.QUEUE_FLUSH, null, null);
        }catch (Exception e){
            Log.e("TTS","TTS 오류 발생: "+ e.getMessage());
        }

    }



    private void startTicking(){
        speakCurrentTime();     //최초에 한번은 알림
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                int second = now.get(Calendar.SECOND);

                updateTimeText(); // 화면 상단 시간 업데이트

                if(second % speakInterval ==0) {
                    speakCurrentTime();
                }
                if(second % 10 ==0){
                    playHighBeep();
                }else{
                    playLowBeep();
                }
                tickHandler.postDelayed(this, 1000);
            }
        };

        tickHandler.post(tickRunnable);
    }
    private AudioTrack generateSineWaveTrack(double freq, int durationMs) {
        int sampleRate = 44100;
        int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
        double[] sample = new double[numSamples];
        byte[] generatedSnd = new byte[2 * numSamples];

        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freq));
        }

        int idx = 0;
        for (double val : sample) {
            short s = (short) (val * 32767);
            generatedSnd[idx++] = (byte) (s & 0x00ff);
            generatedSnd[idx++] = (byte) ((s & 0xff00) >>> 8);
        }

        AudioTrack track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.length,
                AudioTrack.MODE_STATIC
        );

        track.write(generatedSnd, 0, generatedSnd.length);
        return track.getState() == AudioTrack.STATE_INITIALIZED ? track : null;
    }


    private void playLowBeep() {
        AudioTrack low = generateSineWaveTrack(800, 100);
        if (low != null) {
            low.play();
            handler.postDelayed(low::release, 150); // 100ms 후 자원 반환
        }
    }

    private void playHighBeep() {
        AudioTrack high = generateSineWaveTrack(2000, 400);
        if (high != null) {
            high.play();
            handler.postDelayed(high::release, 450); // 400ms 후 자원 반환
        }
    }

    private void updateTimeText() {
        Calendar now = Calendar.getInstance();

        int hour = now.get(Calendar.HOUR);
        if (hour == 0) hour = 12;

        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND); // 👈 초 추가

        String amPm = now.get(Calendar.AM_PM) == Calendar.AM ? "오전" : "오후";

        // 포맷팅해서 문자열 생성
        String timeStr = amPm + " " +
                hour + "시 " +
                String.format("%02d", minute) + "분 " +
                String.format("%02d", second) + "초";

        // TextView에 표시
        textCurrentTime.setText(timeStr);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (tts != null){
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        handler.removeCallbacks(timeSpeaker);
        if(tickHandler != null && tickRunnable != null){
            tickHandler.removeCallbacks(tickRunnable);
        }
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }
}
