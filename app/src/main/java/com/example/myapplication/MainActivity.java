package com.example.myapplication;


import com.example.myapplication.R.id;
import android.annotation.SuppressLint;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Intent recordingActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button instructions = findViewById(R.id.mInstructions);
        Button startGame = findViewById(R.id.mStartGame);
        startGame.setOnClickListener(v -> {
            Intent startrecordingActivityIntent = new Intent(MainActivity.this,
                    recordingActivity.class);
            startActivity(recordingActivity);
        });
    }
}
