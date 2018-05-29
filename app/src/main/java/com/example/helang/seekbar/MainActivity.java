package com.example.helang.seekbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private CustomSeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBar = findViewById(R.id.seekbar);

        /*使用addBackPressure,因为拖动的事件过快，会导致在处理某些较为耗时的操作时，可能会发生内存泄漏，
        这里使用RxJava的背压操作，主动降低被观察者的发送频率。当然这个要结合你实际的业务需求*/
        seekBar.addBackPressure();


        seekBar.setOnProgressListener(new CustomSeekBar.OnProgressListener() {
            @Override
            public void onProgress(float progress) {
                Log.e("Progress",""+progress);
            }
        });
    }
}
