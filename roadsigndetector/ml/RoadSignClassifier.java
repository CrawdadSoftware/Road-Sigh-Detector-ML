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

public class RoadSignClassifier {

    private static final String TAG = "RoadSignClassifier";
    private Interpreter interpreter;
    private List<String> labelList;
    private static final int INPUT_SIZE = 224; // Rozmiar wejściowy modelu
    private static final int PIXEL_SIZE = 3; // RGB
    private static final int FLOAT_SIZE = 4; // Rozmiar float w bajtach

    public RoadSignClassifier(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context, "classifier_224.tflite"));
            labelList = loadLabelList(context, "classifier_labelmap.txt");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing RoadSignClassifier: " + e.getMessage());
        }
    }

    /**
     * Metoda do klasyfikacji obrazu.
     * Zwraca etykietę z najwyższą pewnością.
     */
    public String classifyImage(Bitmap bitmap) {
        if (interpreter == null || labelList == null) {
            return "Model nie jest załadowany.";
        }

        // 1. Skalowanie bitmapy
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        // 2. Konwersja do ByteBuffer
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        // 3. Bufor wyjściowy
        float[][] output = new float[1][labelList.size()];

        // 4. Uruchomienie inferencji
        interpreter.run(inputBuffer, output);

        // 5. Znalezienie indeksu z najwyższą pewnością
        int maxIndex = 0;
        float maxConfidence = 0f;
        for (int i = 0; i < output[0].length; i++) {
            if (output[0][i] > maxConfidence) {
                maxConfidence = output[0][i];
                maxIndex = i;
            }
        }

        // 6. Zwrócenie etykiety i pewności
        String label = RoadSignLabel.classifierLabels[maxIndex];
        return "Znak: " + label + " (pewność: " + String.format("%.2f", maxConfidence * 100) + "%)";
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
     * Zwolnienie zasobów interpretera.
     */
    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
