package com.example.formcheck;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.HashMap;
import java.util.Map;

public class OverlayView extends View {

    private static final float STROKE_WIDTH = 8f;
    private static final float CIRCLE_RADIUS = 10f;
    private static final float ALPHA = 0.7f;

    private static final int GOOD_ANGLE = 90;
    private static final int LOW_ANGLE = 120;

    private Pose pose;
    private Paint paint;

    private int imageWidth;
    private int imageHeight;

    private final Map<Integer, float[]> smoothed = new HashMap<>();

    private String exercise = Exercise.SQUATS;

    public interface AngleListener {
        void onAngle(double angle, String status);
    }

    private AngleListener listener;

    public void setAngleListener(AngleListener listener) {
        this.listener = listener;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void setPose(Pose pose, int width, int height) {
        this.pose = pose;
        this.imageWidth = width;
        this.imageHeight = height;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pose == null) return;

        drawBody(canvas);
        calculateAngle();
    }

    private float scaleX(float x) {
        return x * getWidth() / (float) imageHeight;
    }

    private float scaleY(float y) {
        return y * getHeight() / (float) imageWidth;
    }

    private float[] smooth(int id, float x, float y) {

        if (!smoothed.containsKey(id)) {
            smoothed.put(id, new float[]{x, y});
            return new float[]{x, y};
        }

        float[] last = smoothed.get(id);

        float sx = ALPHA * last[0] + (1 - ALPHA) * x;
        float sy = ALPHA * last[1] + (1 - ALPHA) * y;

        smoothed.put(id, new float[]{sx, sy});
        return new float[]{sx, sy};
    }

    private void draw(int a, int b, Canvas canvas) {

        PoseLandmark p1 = pose.getPoseLandmark(a);
        PoseLandmark p2 = pose.getPoseLandmark(b);

        if (p1 == null || p2 == null) return;

        float x1 = scaleX(p1.getPosition().x);
        float y1 = scaleY(p1.getPosition().y);

        float x2 = scaleX(p2.getPosition().x);
        float y2 = scaleY(p2.getPosition().y);

        float[] s1 = smooth(a, x1, y1);
        float[] s2 = smooth(b, x2, y2);

        canvas.drawLine(s1[0], s1[1], s2[0], s2[1], paint);
        canvas.drawCircle(s1[0], s1[1], CIRCLE_RADIUS, paint);
        canvas.drawCircle(s2[0], s2[1], CIRCLE_RADIUS, paint);
    }

    private void drawBody(Canvas canvas) {

        draw(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, canvas);
        draw(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, canvas);

        draw(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, canvas);
        draw(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, canvas);

        draw(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, canvas);
        draw(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, canvas);

        draw(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, canvas);
        draw(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, canvas);

        draw(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, canvas);
        draw(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, canvas);

        draw(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, canvas);
        draw(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, canvas);
    }

    private void calculateAngle() {

        PoseLandmark a = null;
        PoseLandmark b = null;
        PoseLandmark c = null;

        if (exercise.equals(Exercise.SQUATS)) {
            a = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
            b = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
            c = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        } else {
            a = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
            b = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
            c = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        }

        if (a == null || b == null || c == null) return;

        double angle = getAngle(a, b, c);

        String status;
        if (angle < GOOD_ANGLE) status = "GOOD";
        else if (angle < LOW_ANGLE) status = "LOW";
        else status = "BAD";

        if (listener != null) {
            listener.onAngle(angle, status);
        }
    }

    private double getAngle(PoseLandmark a, PoseLandmark b, PoseLandmark c) {

        double abx = a.getPosition().x - b.getPosition().x;
        double aby = a.getPosition().y - b.getPosition().y;

        double cbx = c.getPosition().x - b.getPosition().x;
        double cby = c.getPosition().y - b.getPosition().y;

        double dot = abx * cbx + aby * cby;

        double mag1 = Math.sqrt(abx * abx + aby * aby);
        double mag2 = Math.sqrt(cbx * cbx + cby * cby);

        double cos = dot / (mag1 * mag2);
        cos = Math.max(-1.0, Math.min(1.0, cos));

        return Math.toDegrees(Math.acos(cos));
    }
}