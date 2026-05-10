package com.example.formcheck;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView tvReps = findViewById(R.id.tvReps);
        TextView tvMinAngle = findViewById(R.id.tvMinAngle);
        TextView tvAvgAngle = findViewById(R.id.tvAvgAngle);
        TextView tvRecommendation = findViewById(R.id.tvAdvice);

        int reps = getIntent().getIntExtra("reps", 0);

        double minAngle =
                getIntent().getDoubleExtra("minAngle", 0);

        double avgAngle =
                getIntent().getDoubleExtra("avgAngle", 0);

        String recommendation =
                getIntent().getStringExtra("recommendation");

        tvReps.setText("Reps: " + reps);

        tvMinAngle.setText(
                "Min angle: " + (int) minAngle + "°"
        );

        tvAvgAngle.setText(
                "Average angle: " + (int) avgAngle + "°"
        );

        tvRecommendation.setText(recommendation);
    }
}