package com.example.myapplication;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
public class TFLiteModelLoader {
    private Interpreter tflite;

    /**
     * Constructor that loads the model.
     *
     * @param assetManager AssetManager from the context (e.g., activity.getAssets())
     * @param modelPath    Path to the model file in assets (e.g., "emotion_model.tflite")
     */
    public TFLiteModelLoader(AssetManager assetManager, String modelPath) {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(assetManager, modelPath);
            tflite = new Interpreter(modelBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the TensorFlow Lite Interpreter.
     *
     * @return Interpreter
     */
    public Interpreter getInterpreter() {
        return tflite;
    }

    /**
     * Loads the model file from assets and maps it into memory.
     *
     * @param assetManager AssetManager to access the assets
     * @param modelPath    Path to the model file in the assets folder
     * @return MappedByteBuffer containing the model
     * @throws IOException if file reading fails
     */
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
