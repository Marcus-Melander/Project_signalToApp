package com.example.myapplication; // Ensure this is correct

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLiteModelLoader {
    private Interpreter interpreter;
    private static TFLiteModelLoader instance;

    private TFLiteModelLoader(Context context) {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(context, "v2.0.tflite");
            interpreter = new Interpreter(modelBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized TFLiteModelLoader getInstance(Context context) {
        if (instance == null) {
            instance = new TFLiteModelLoader(context.getApplicationContext());
        }
        return instance;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}