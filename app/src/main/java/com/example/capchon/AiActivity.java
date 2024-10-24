package com.example.capchon;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.capchon.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AiActivity extends AppCompatActivity {

    private static final String TAG = "AiActivity";

    TextView result, confidence;
    ImageView imageView;
    Button picture;
    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai);

        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);

        picture.setOnClickListener(view -> {
            Log.d(TAG, "Take Picture button clicked");
            // Launch camera if we have permission
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 1);
            } else {
                Log.d(TAG, "Requesting camera permission");
                // Request camera permission if we don't have it
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        });
    }

    public void classifyImage(Bitmap image) {
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            // get 1D array of 224 * 224 pixels in image
            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

            // Define the classes your model can detect
            String[] classes = new String[]{"Garbage_CigaretteButt", "Garbage_CoffeeCup", "Garbage_PlasticBag", "Garbage_Plastic",
                    "Action_Lying+Sitting", "Action_Hand", "Action_FailedAction",
                    "Object_MNU", "Object_Fountain", "Object_EngineeringBuilding4"};

            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }

            // Process the result based on the recognized object
            String recognizedClass = classes[maxPos];
            result.setText(recognizedClass + " recognized!");

            StringBuilder s = new StringBuilder();
            for (int i = 0; i < classes.length; i++) {
                s.append(String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100));
            }
            confidence.setText(s.toString());

            // Set the result for the quest if confidence is over 90%
            if (maxConfidence >= 0.9f) {
                if (recognizedClass.equals("Garbage_CigaretteButt") || recognizedClass.equals("Garbage_CoffeeCup") ||
                        recognizedClass.equals("Garbage_PlasticBag") || recognizedClass.equals("Garbage_Plastic")) {
                    markQuestAsCompleted("쓰레기 줍기");
                } else if (recognizedClass.equals("Object_Fountain")) {
                    markQuestAsCompleted("분수대 사진");
                } else if (recognizedClass.equals("Object_MNU")) {
                    markQuestAsCompleted("MNU 사진");
                } else if (recognizedClass.equals("Action_Hand")) {
                    markQuestAsCompleted("강아지 손");
                } else if (recognizedClass.equals("Action_Lying+Sitting")) {
                    markQuestAsCompleted("강아지 앉기");
                }
            }

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.e(TAG, "Error classifying image", e);
            result.setText("failure");
            confidence.setText("");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Log.d(TAG, "Image captured");
            if (data != null && data.getExtras() != null) {
                Bitmap image = (Bitmap) data.getExtras().get("data");
                if (image != null) {
                    int dimension = Math.min(image.getWidth(), image.getHeight());
                    image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                    imageView.setImageBitmap(image);

                    image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                    classifyImage(image);
                } else {
                    Log.e(TAG, "Image data is null");
                    result.setText("failure");
                }
            } else {
                Log.e(TAG, "Intent data or extras are null");
                result.setText("failure");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우 카메라 인텐트를 실행합니다.
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 1);
            } else {
                // 권한이 거부된 경우 사용자에게 권한 필요성을 설명합니다.
                Log.d(TAG, "Camera permission denied");
            }
        }
    }

    // Mark a quest as completed
    private void markQuestAsCompleted(String questName) {
        // 퀘스트를 취소선으로 그리거나 완료된 표시를 하는 코드 작성
        new Handler().postDelayed(() -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("recognizedQuest", "success");
            returnIntent.putExtra("questName", questName); // 해당 퀘스트 이름 전달
            setResult(RESULT_OK, returnIntent);
            finish();
        }, 3000); // 3초 후에 결과 반환
    }
}
//추후에 하루에 한 번씩 퀘스트 리스트가 갱신 되는 기능 추가
