package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class Instructions extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);
        Button vidButton = findViewById(R.id.mVideoButton);
        vidButton.setOnClickListener(v -> {
            Intent startRecordActivityIntent = new Intent(Instructions.this,
                    VideoActivity.class);
            startActivity(startRecordActivityIntent);
        });
    }
}
