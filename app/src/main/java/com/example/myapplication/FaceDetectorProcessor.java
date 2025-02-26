package com.example.myapplication;

import android.graphics.PointF;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.util.List;

public class FaceDetectorProcessor {

    private static final String TAG = "FaceDetectorProcessor";
    private final FaceDetector faceDetector;

    public FaceDetectorProcessor() {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();
        faceDetector = FaceDetection.getClient(options);
    }

    public void processImage(InputImage image, FaceDetectionListener listener) {
        faceDetector.process(image)
                .addOnSuccessListener(faces -> handleFaceDetection(faces, listener))
                .addOnFailureListener(e -> Log.e(TAG, "Face detection failed", e));
    }

    private void handleFaceDetection(List<Face> faces, FaceDetectionListener listener) {
        if(!faces.isEmpty()) {

            Face face = faces.get(0);
            float leftEyeOpen = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0;
            float rightEyeOpen = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0;
            float smileProb = face.getSmilingProbability() != null ? face.getSmilingProbability() : 0;

            listener.onFaceDetected(leftEyeOpen, rightEyeOpen, smileProb);
        }
    }

    public interface FaceDetectionListener {
        void onFaceDetected(float leftEyeOpen, float rightEyeOpen, float smileProb);
    }
}
