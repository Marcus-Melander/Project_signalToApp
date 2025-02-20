package com.example.myapplication;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button vidButton = findViewById(R.id.mVideoButton);
        Button instructionsButton = findViewById(R.id.mInstructions);
        vidButton.setOnClickListener(v -> {
            Intent startRecordActivityIntent = new Intent(MainActivity.this,
                    VideoActivity.class);
            startActivity(startRecordActivityIntent);
        });
        instructionsButton.setOnClickListener(v -> {
            Intent startInstructions = new Intent(MainActivity.this,
                    Instructions.class);
            startActivity(startInstructions);
        });

        Button cameraXButton = findViewById(R.id.mCameraXButton);
        cameraXButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraXActivity.class);
            startActivity(intent);
        });

    }



}
