package com.example.roadsigndetector.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.roadsigndetector.R;
import com.example.roadsigndetector.ml.RoadSignDetector;
import com.example.roadsigndetector.ml.RoadSignDetector.Detection;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private Button buttonCapture;
    private Camera camera;
    private SurfaceHolder surfaceHolder;

    private RoadSignDetector detector;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        surfaceView = findViewById(R.id.surfaceViewCamera);
        buttonCapture = findViewById(R.id.buttonCapture);

        // Inicjalizacja detektora
        detector = new RoadSignDetector(this);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureAndDetect();
            }
        });
    }

    /**
     * Metoda do przechwytywania obrazu z kamery i detekcji znaków.
     */
    private void captureAndDetect() {
        if (camera != null) {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    // Konwersja bajtów na Bitmap
                    Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);

                    // Detekcja obiektów
                    List<Detection> detections = detector.detectObjects(bitmap);

                    if (detections.isEmpty()) {
                        Toast.makeText(CameraActivity.this, "Nie wykryto żadnych znaków.", Toast.LENGTH_SHORT).show();
                    } else {
                        StringBuilder result = new StringBuilder();
                        for (Detection detection : detections) {
                            result.append(detection.label)
                                    .append(" (")
                                    .append(String.format("%.2f", detection.confidence * 100))
                                    .append("%)\n");
                        }
                        Toast.makeText(CameraActivity.this, "Wynik:\n" + result.toString(), Toast.LENGTH_LONG).show();
                    }

                    // Wznawiamy podgląd z kamery
                    camera.startPreview();
                }
            });
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Sprawdzamy, czy mamy uprawnienia do kamery
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera(holder);
        }
    }

    /**
     * Otwiera kamerę i ustawia podgląd na SurfaceHolder.
     */
    private void openCamera(SurfaceHolder holder) {
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90); // Dopasowanie do pionowego ekranu
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Błąd podczas uruchamiania kamery.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            camera.stopPreview();
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Błąd podczas zmiany podglądu.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) {
            detector.close();
            detector = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(surfaceHolder);
            } else {
                Toast.makeText(this, "Uprawnienia do kamery są wymagane.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
