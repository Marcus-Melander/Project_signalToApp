package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
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
    private Integer points = 0;
    private int guessedEmoji;
    private int mimicEmoji;

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
        int[][] pixelsToUse = pixelsToUse(bitmap);

        // Update the ImageView on the UI thread
        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
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

            // TODO: avkommentera 4 rader och använd de istället.
            //if(containsFace(pixelsToUse)){
                //int guess = guessEmoji(int[][] pixelsToUse);
                //setGuessedEmoji(int guess);
            //}
            setGuessedEmoji();

        }
    }
    private boolean containsFace(int[][] matrix){
        // TODO: call model to check for face

        return true;
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

                // Check if the pixel belongs to the square
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

                if (isTopLeftHorizontal || isTopLeftVertical ||
                        isTopRightHorizontal || isTopRightVertical ||
                        isBottomLeftHorizontal || isBottomLeftVertical ||
                        isBottomRightHorizontal || isBottomRightVertical) {
                    rgbArray[y * width + x] = 0xFFFFFFFF; // White pixel (ARGB)
                } else {
                    rgbArray[y * width + x] = (0xFF << 24) | (r << 16) | (g << 8) | b;
                }
            }
        }

        // Create a Bitmap from the RGB array
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(rgbArray, 0, width, 0, 0, width, height);

        // TODO: erase
        //Rotate the bitmap 90 degrees (required for correct display on Xperia XA1)
        //int totalRotation=270;
        //Bitmap rotatedBitmap = rotateBitmap(bitmap, totalRotation);

        return bitmap;
    }


    public int[][] pixelsToUse(Bitmap bitmap) {
        // Determine the bitmap dimensions.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Define the centered 300x300 white square.
        int squareSize = 300;
        int squareLeft = width / 2 - squareSize / 2;
        int squareTop = height / 2 - squareSize / 2;

        // Define the inner region (299x299) by offsetting 1 pixel to avoid the white border.
        int regionX = squareLeft + 1;
        int regionY = squareTop + 1;
        int regionWidth = 299;
        int regionHeight = 299;

        // Extract the inner region from the bitmap.
        Bitmap innerBitmap = Bitmap.createBitmap(bitmap, regionX, regionY, regionWidth, regionHeight);

        // Prepare to compute grayscale intensities and the histogram.
        int[][] intensityMatrix = new int[regionHeight][regionWidth];
        int[] histogram = new int[256]; // for intensity values 0-255, initially all zeros.

        // First pass: compute grayscale intensity for each pixel and build histogram.
        for (int y = 0; y < regionHeight; y++) {
            for (int x = 0; x < regionWidth; x++) {
                int pixel = innerBitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                // Calculate luminance using a common formula (Rec. 601)
                int luminance = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                intensityMatrix[y][x] = luminance;
                histogram[luminance]++;
            }
        }

        // Compute the cumulative distribution function (CDF) for the histogram.
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }

        // Find the minimum nonzero value in the CDF.
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cdf[i] != 0) {
                cdfMin = cdf[i];
                break;
            }
        }

        int totalPixels = regionWidth * regionHeight;

        // Build a lookup table using the histogram equalization formula:
        // new_value = round((cdf[value] - cdfMin) / (totalPixels - cdfMin) * 255)
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = Math.round(((float)(cdf[i] - cdfMin) / (totalPixels - cdfMin)) * 255);
        }

        // Apply the lookup table to generate the normalized intensity matrix.
        int[][] normalizedMatrix = new int[regionHeight][regionWidth];
        for (int y = 0; y < regionHeight; y++) {
            for (int x = 0; x < regionWidth; x++) {
                normalizedMatrix[y][x] = lut[intensityMatrix[y][x]];
            }
        }

        // TODO: erase
        // If a binary (black and white) matrix is required after normalization,
        // you could threshold the normalized intensities as follows:
        // for (int y = 0; y < regionHeight; y++) {
        //     for (int x = 0; x < regionWidth; x++) {
        //         normalizedMatrix[y][x] = normalizedMatrix[y][x] > 128 ? 1 : 0;
        //     }
        // }

        return normalizedMatrix;
    }

    // TODO: erase
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
            // undicode for sad
            int unicode = 0x1F622;
            String emoji = getEmojiByUnicode(unicode);
            txtViewMimic.setText(emoji);
            mimicEmoji = 0;
        } else if (num == 1) {
            // unicoe for happy
            int unicode = 0x1F604;
            String emoji = getEmojiByUnicode(unicode);
            txtViewMimic.setText(emoji);
            mimicEmoji = 1;
        } else if (num == 2) {
            // unicode for angry
            int unicode = 0x1F621;
            String emoji = getEmojiByUnicode(unicode);
            txtViewMimic.setText(emoji);
            mimicEmoji = 2;
        }
        this.txtViewMimic = txtViewMimic;
    }

    private int guessEmoji(int[][] pixels){
        int guess = 0;
        // TODO: kalla på modellen och låt den returnera ett värde för guess

        return guess;
    }

    private void setGuessedEmoji(){
        TextView txtViewGuess = (TextView)findViewById(R.id.mGuess);

        // TODO: låt metoden ta en int som input och byt efter det ut random mot den inten. (ta in matris)
        // int num = guessEmoji(int[][] pixels)

        Random rand = new Random();
        int num = rand.nextInt(3);

        if(num == 0) {
            // sad
            int unicode = 0x1F622;
            String emoji = getEmojiByUnicode(unicode);
            txtViewGuess.setText(emoji);
            guessedEmoji = 0;
        } else if (num == 1) {
            // happy
            int unicode = 0x1F604;
            String emoji = getEmojiByUnicode(unicode);
            txtViewGuess.setText(emoji);
            guessedEmoji = 1;
        } else if (num == 2) {
            // angry
            int unicode = 0x1F621;
            String emoji = getEmojiByUnicode(unicode);
            txtViewGuess.setText(emoji);
            guessedEmoji = 2;
        }
        this.txtViewGuess = txtViewGuess;
        guessEqualsMimic();
    }
    public String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }
    private void guessEqualsMimic(){
        TextView txtViewPoints = (TextView) findViewById(R.id.mPoints);

        if(mimicEmoji == guessedEmoji){
            points++;
            String currentPoints = points.toString();
            txtViewPoints.setText(currentPoints);
            setEmojiToMimic();
        }
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
