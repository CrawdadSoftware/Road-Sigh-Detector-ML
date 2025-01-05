package com.example.roadsigndetector.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.roadsigndetector.data.RoadSignLabel;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoadSignDetector {

    private static final String TAG = "RoadSignDetector";
    private Interpreter interpreter;
    private List<String> labelList;
    private static final int INPUT_SIZE = 300; // Rozmiar wejściowy dla detekcji
    private static final int PIXEL_SIZE = 3; // RGB
    private static final int FLOAT_SIZE = 4; // Rozmiar float w bajtach

    public RoadSignDetector(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context, "detect.tflite"));
            labelList = loadLabelList(context, "detect_labelmap.txt");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing RoadSignDetector: " + e.getMessage());
        }
    }

    /**
     * Metoda do detekcji obiektów w obrazie.
     * Zwraca listę wykrytych obiektów.
     */
    public List<Detection> detectObjects(Bitmap bitmap) {
        List<Detection> detections = new ArrayList<>();

        if (interpreter == null || labelList == null) {
            Log.e(TAG, "Interpreter lub labelList jest null");
            return detections;
        }

        // 1. Skalowanie bitmapy
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        // 2. Konwersja do ByteBuffer
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        // 3. Bufory wyjściowe
        // Zakładamy, że model detekcji zwraca 1x10x4 (lokalizacje), 1x10 (klasy), 1x10 (pewności)
        float[][][] outputLocations = new float[1][10][4];
        float[][] outputClasses = new float[1][10];
        float[][] outputScores = new float[1][10];
        float[] numDetections = new float[1];

        // 4. Przygotowanie mapy wyjściowej
        Map<Integer, Object> outputMap = getOutputMap(outputLocations, outputClasses, outputScores, numDetections);

        // 5. Uruchomienie inferencji
        interpreter.runForMultipleInputsOutputs(new Object[]{inputBuffer}, outputMap);

        // 6. Przetwarzanie wyników
        int detectionsCount = Math.min((int) numDetections[0], 10);
        for (int i = 0; i < detectionsCount; i++) {
            float score = outputScores[0][i];
            if (score > 0.5) { // Próg pewności
                int classIndex = (int) outputClasses[0][i];
                if (classIndex >= labelList.size()) {
                    Log.e(TAG, "Invalid class index: " + classIndex);
                    continue;
                }
                String label = RoadSignLabel.detectLabels[classIndex];
                float[] location = outputLocations[0][i]; // [ymin, xmin, ymax, xmax]
                Detection detection = new Detection(label, score, location);
                detections.add(detection);
            }
        }

        return detections;
    }

    /**
     * Konwertuje Bitmap na ByteBuffer typu float (0..1).
     */
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                FLOAT_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        int pixelIndex = 0;
        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < INPUT_SIZE; j++) {
                final int val = pixels[pixelIndex++];

                // Rozbicie piksela ARGB na składowe R, G, B i normalizacja
                float r = ((val >> 16) & 0xFF) / 255.0f;
                float g = ((val >> 8) & 0xFF) / 255.0f;
                float b = (val & 0xFF) / 255.0f;

                byteBuffer.putFloat(r);
                byteBuffer.putFloat(g);
                byteBuffer.putFloat(b);
            }
        }

        return byteBuffer;
    }

    /**
     * Wczytuje model TensorFlow Lite z folderu assets.
     */
    private ByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        InputStream inputStream = context.getAssets().open(modelPath);
        byte[] modelBytes = new byte[inputStream.available()];
        inputStream.read(modelBytes);
        inputStream.close();

        ByteBuffer buffer = ByteBuffer.allocateDirect(modelBytes.length);
        buffer.order(ByteOrder.nativeOrder());
        buffer.put(modelBytes);
        buffer.rewind();
        return buffer;
    }

    /**
     * Wczytuje listę etykiet z pliku labelmap.
     */
    private List<String> loadLabelList(Context context, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        InputStream inputStream = context.getAssets().open(labelPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    /**
     * Tworzy mapę wyjściową dla modelu detekcji.
     */
    private Map<Integer, Object> getOutputMap(float[][][] locations, float[][] classes,
                                              float[][] scores, float[] numDetections) {
        java.util.Map<Integer, Object> outputMap = new java.util.HashMap<>();
        outputMap.put(0, locations);
        outputMap.put(1, classes);
        outputMap.put(2, scores);
        outputMap.put(3, numDetections);
        return outputMap;
    }

    /**
     * Zwolnienie zasobów interpretera.
     */
    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    /**
     * Klasa reprezentująca pojedyncze wykrycie.
     */
    public static class Detection {
        public String label;
        public float confidence;
        public float[] location; // [ymin, xmin, ymax, xmax]

        public Detection(String label, float confidence, float[] location) {
            this.label = label;
            this.confidence = confidence;
            this.location = location;
        }
    }
}
