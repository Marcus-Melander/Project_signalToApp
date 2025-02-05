package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Random;

public class VideoActivity extends AppCompatActivity {

    private ImageView processedImageView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private Handler mainHandler;
    private TextView txtViewMimic;
    private TextView txtViewGuess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mainHandler = new Handler(Looper.getMainLooper());
        processedImageView = findViewById(R.id.processedImageView);
        openCamera();
        setEmojiToMimic();
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[1];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Size[] supportedSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(SurfaceTexture.class);
            // Select the smallest size available
            Size previewSize = supportedSizes[supportedSizes.length - 4];

            // Find a specific size (e.g., 640x480 which is widely supported)
            Size targetSize = null;
            for (Size size : supportedSizes) {
                if (size.getWidth() == 640 && size.getHeight() == 480) {
                    targetSize = size;
                    break;
                }
            }
            if (targetSize != null) {
                previewSize = targetSize;
            }

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(this::onImageAvailable, backgroundHandler);

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraCaptureSession() {
        try {
            Surface imageSurface = imageReader.getSurface();

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(imageSurface);

            // Set the desired frame rate range
            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[1];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

            // Select a suitable FPS range (e.g., [30, 30] for 30 FPS)
            Range<Integer> targetFpsRange = null;
            for (Range<Integer> range : fpsRanges) {
                if (range.getLower() == 30 && range.getUpper() == 30) { // Example: 30 FPS
                    targetFpsRange = range;
                    break;
                }
            }

            if (targetFpsRange != null) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, targetFpsRange);
            } else {
                Log.w("VideoActivity", "Target FPS range not found, using default.");
            }
            cameraDevice.createCaptureSession(Collections.singletonList(imageSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            try {
                                cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void onImageAvailable(ImageReader reader) {
        backgroundHandler.post(() -> {
            try (Image image = reader.acquireLatestImage()) {
                if (image != null) {
                    processImage(image); // Process the image on the background thread
                }
            } catch (Exception e) {
                Log.e("VideoActivity", "Error processing image", e);
            }
        });
    }

    private void processImage(Image image) {

        // Process the image data by first converting into an RGB Bitmap
        Bitmap bitmap = yuvToRgbBitmap(image);

        // Update the ImageView on the UI thread
        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(180);
            matrix.preScale(-1.0f, 1.0f);

            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

            mainHandler.post(() -> processedImageView.setImageBitmap(rotatedBitmap));
        }
    }
    private Bitmap yuvToRgbBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer(); // Y plane
        ByteBuffer uBuffer = planes[1].getBuffer(); // U plane
        ByteBuffer vBuffer = planes[2].getBuffer(); // V plane

        // Extract row strides and pixel strides
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        int[] rgbArray = new int[width * height];

        // Pre-calculate the coordinates for the centered 300x300 square.
        int squareSize = 300;
        int cornerLineLength = 70;
        int thickness = 3;
        int squareLeft = width / 2 - squareSize / 2;
        int squareTop = height / 2 - squareSize / 2;
        int squareRight = squareLeft + squareSize;
        int squareBottom = squareTop + squareSize;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Y plane index
                int yIndex = y * yRowStride + x;

                // UV plane indices
                int uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride;

                // YUV to RGB conversion
                int yValue = (yBuffer.get(yIndex) & 0xFF);
                int uValue = (uBuffer.get(uvIndex) & 0xFF) - 128;
                int vValue = (vBuffer.get(uvIndex) & 0xFF) - 128;

                int r = (int) (yValue + 1.370705 * vValue);
                int g = (int) (yValue - 0.337633 * uValue - 0.698001 * vValue);
                int b = (int) (yValue + 1.732446 * uValue);

                // Clamp RGB values to 0-255
                r = Math.min(Math.max(r, 0), 255);
                g = Math.min(Math.max(g, 0), 255);
                b = Math.min(Math.max(b, 0), 255);


                // Determine if the current pixel falls within any of the 2-pixel thick corner outline segments.
                boolean isTopLeftHorizontal = (y >= squareTop && y < squareTop + thickness &&
                        x >= squareLeft && x < squareLeft + cornerLineLength);
                boolean isTopLeftVertical   = (x >= squareLeft && x < squareLeft + thickness &&
                        y >= squareTop && y < squareTop + cornerLineLength);

                boolean isTopRightHorizontal = (y >= squareTop && y < squareTop + thickness &&
                        x >= squareRight - cornerLineLength && x < squareRight);
                boolean isTopRightVertical   = (x >= squareRight - thickness && x < squareRight &&
                        y >= squareTop && y < squareTop + cornerLineLength);

                boolean isBottomLeftHorizontal = (y >= squareBottom - thickness && y < squareBottom &&
                        x >= squareLeft && x < squareLeft + cornerLineLength);
                boolean isBottomLeftVertical   = (x >= squareLeft && x < squareLeft + thickness &&
                        y >= squareBottom - cornerLineLength && y < squareBottom);

                boolean isBottomRightHorizontal = (y >= squareBottom - thickness && y < squareBottom &&
                        x >= squareRight - cornerLineLength && x < squareRight);
                boolean isBottomRightVertical   = (x >= squareRight - thickness && x < squareRight &&
                        y >= squareBottom - cornerLineLength && y < squareBottom);

                // If the pixel lies on any of these segments, set it to white.
                if (isTopLeftHorizontal || isTopLeftVertical ||
                        isTopRightHorizontal || isTopRightVertical ||
                        isBottomLeftHorizontal || isBottomLeftVertical ||
                        isBottomRightHorizontal || isBottomRightVertical) {
                    rgbArray[y * width + x] = 0xFFFFFFFF; // White pixel (ARGB)
                } else {
                    // Otherwise, set the pixel to the calculated RGB value.
                    rgbArray[y * width + x] = (0xFF << 24) | (r << 16) | (g << 8) | b;
                }


                // Set RGB pixel in array
                // rgbArray[y * width + x] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }

        // Create a Bitmap from the RGB array
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(rgbArray, 0, width, 0, 0, width, height);

        //Rotate the bitmap 90 degrees (required for correct display on Xperia XA1)
        int totalRotation=90;
        Bitmap rotatedBitmap = rotateBitmap(bitmap, totalRotation);

        return rotatedBitmap;
    }
    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @SuppressLint("SetTextI18n")
    private void setEmojiToMimic() {
        TextView txtViewMimic = (TextView)findViewById(R.id.mMimic);

        Random rand = new Random();
        int num = rand.nextInt(3);

        if(num == 0) {
            txtViewMimic.setText("Sad");
        } else if (num == 1) {
            txtViewMimic.setText("Happy");
        } else if (num == 2) {
            txtViewMimic.setText("Angry");
        }
        this.txtViewMimic = txtViewMimic;
    }

    private void setGuessedEmoji(){
        // TODO: call method every time a new guess is made!
        TextView txtViewGuess = (TextView) findViewById(R.id.mGuess);
        txtViewGuess.setText(guessEmoji());
        this.txtViewGuess = txtViewGuess;
    }

    private void guessEqualsMimic(){
        //TextView txtViewPoints = (TextView) findViewById(R.id.mPoints);
        //TODO: call every time new guess is made!
        int counter = 0;
        if(txtViewGuess.getText().equals(txtViewMimic.getText())){
            counter ++;

        }
    }

    private String guessEmoji(){
        // TODO: implement method in working thread!
        return "hej";
    }


    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
