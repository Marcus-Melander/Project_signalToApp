package com.example.myapplication;
import android.content.res.AssetManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;
public class EmotionProcessingThread extends Thread {
    private Interpreter tfliteInterpreter;

    /*public EmotionProcessingThread(AssetManager assetManager, workbuffer b, datafile d){
        TFLiteModelLoader modelLoader = new TFLiteModelLoader(assetManager, "assets/Emotion_detection_V1.0.tflite");
        tfliteInterpreter = modelLoader.getInterpreter();
    }*/
}
