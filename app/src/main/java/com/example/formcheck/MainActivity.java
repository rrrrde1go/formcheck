package com.example.formcheck;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.video.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;

    private PreviewView previewView;
    private OverlayView overlayView;

    private TextView tvExercise, tvAngle, tvStatus;
    private MaterialButton btnRecord;

    private PoseDetector poseDetector;

    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private boolean isRecording = false;

    private String exercise = Exercise.SQUATS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);

        tvExercise = findViewById(R.id.tvExercise);
        tvAngle = findViewById(R.id.tvAngle);
        tvStatus = findViewById(R.id.tvStatus);
        btnRecord = findViewById(R.id.btnRecord);

        exercise = getIntent().getStringExtra("exercise");
        if (exercise == null) exercise = Exercise.SQUATS;

        tvExercise.setText(exercise);
        overlayView.setExercise(exercise);

        overlayView.setAngleListener((angle, status) -> {
            tvAngle.setText("Angle: " + (int) angle);
            tvStatus.setText(status);

            if (status.equals("GOOD")) {
                tvStatus.setTextColor(0xFF00FF00);
            } else if (status.equals("LOW")) {
                tvStatus.setTextColor(0xFFFFFF00);
            } else {
                tvStatus.setTextColor(0xFFFF0000);
            }
        });

        btnRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                btnRecord.setText("REC");
            } else {
                startRecording();
                btnRecord.setText("STOP");
            }
        });

        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();

        poseDetector = PoseDetection.getClient(options);

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                },
                REQUEST_CODE
        );
    }

    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        this::analyze
                );

                Recorder recorder = new Recorder.Builder().build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                provider.unbindAll();
                provider.bindToLifecycle(
                        this,
                        selector,
                        preview,
                        analysis,
                        videoCapture
                );

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void analyze(ImageProxy proxy) {

        Image image = proxy.getImage();
        if (image == null) {
            proxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                image,
                proxy.getImageInfo().getRotationDegrees()
        );

        poseDetector.process(inputImage)
                .addOnSuccessListener(pose -> {
                    overlayView.setPose(pose, proxy.getWidth(), proxy.getHeight());
                    proxy.close();
                })
                .addOnFailureListener(e -> proxy.close());
    }

    private void startRecording() {

        if (videoCapture == null) return;

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "video_" + System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

        MediaStoreOutputOptions options =
                new MediaStoreOutputOptions.Builder(
                        getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                        .setContentValues(values)
                        .build();

        recording = videoCapture.getOutput()
                .prepareRecording(this, options)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), event -> {

                    if (event instanceof VideoRecordEvent.Start) {
                        isRecording = true;
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
                    }

                    if (event instanceof VideoRecordEvent.Finalize) {
                        isRecording = false;
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void stopRecording() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startCamera();
        }
    }
}