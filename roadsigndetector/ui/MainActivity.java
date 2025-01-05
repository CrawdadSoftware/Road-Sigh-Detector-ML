package com.example.roadsigndetector.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.roadsigndetector.R;
import com.example.roadsigndetector.ml.RoadSignClassifier;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_IMAGE = 101;
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1002;

    private Button buttonSelectImage;
    private Button buttonOpenCamera;
    private TextView textViewResult;

    private RoadSignClassifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicjalizacja widoków
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonOpenCamera = findViewById(R.id.buttonOpenCamera);
        textViewResult = findViewById(R.id.textViewResult);

        // Inicjalizacja klasyfikatora
        classifier = new RoadSignClassifier(this);

        // Sprawdzenie uprawnienia do odczytu pamięci
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_READ_EXTERNAL_STORAGE);
        }

        // Obsługa przycisku "Wybierz zdjęcie z galerii"
        buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
            }
        });

        // Obsługa przycisku "Uruchom aparat"
        buttonOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Zwolnij zasoby klasyfikatora
        if (classifier != null) {
            classifier.close();
            classifier = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(), imageUri);
                    // Klasyfikacja obrazu
                    String result = classifier.classifyImage(bitmap);
                    textViewResult.setText(result);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Błąd podczas wczytywania obrazu.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Uprawnienie przyznane
                Toast.makeText(this, "Uprawnienia do odczytu pamięci przyznane.", Toast.LENGTH_SHORT).show();
            } else {
                // Uprawnienie odrzucone
                Toast.makeText(this, "Uprawnienia do odczytu pamięci są wymagane.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
