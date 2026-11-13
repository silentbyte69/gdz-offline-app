package moscow.cuteslavicboy.gdzoffline.ui.viewer;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/** Fit-center image view with pinch zoom, double-tap zoom and panning. */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MAX_SCALE = 6f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;

    private final Matrix base = new Matrix();
    private final Matrix display = new Matrix();
    private float scale = 1f;
    private float panX;
    private float panY;
    private float baseW;
    private float baseH;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    public ZoomableImageView(@NonNull Context context) {
        this(context, null);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                // Keep parent pagers from stealing the gesture mid-pinch.
                disallowIntercept(true);
                return true;
            }

            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float target = clamp(scale * detector.getScaleFactor(), 1f, MAX_SCALE);
                zoomAt(detector.getFocusX(), detector.getFocusY(), target / scale);
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                float target = scale > 1.01f ? 1f : DOUBLE_TAP_SCALE;
                zoomAt(e.getX(), e.getY(), target / scale);
                return true;
            }

            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2,
                                    float distanceX, float distanceY) {
                if (scale <= 1f) return false;
                panX -= distanceX;
                panY -= distanceY;
                clampPan();
                apply();
                return true;
            }
        });
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        computeBase();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeBase();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (scale > 1f) disallowIntercept(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                disallowIntercept(false);
                break;
            default:
                break;
        }
        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        return true;
    }

    private void disallowIntercept(boolean disallow) {
        android.view.ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    @Override
    public boolean canScrollVertically(int direction) {
        float max = maxPanY();
        if (scale <= 1f || max == 0) return false;
        return direction > 0 ? panY > -max + 0.5f : panY < max - 0.5f;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        float max = maxPanX();
        if (scale <= 1f || max == 0) return false;
        return direction > 0 ? panX > -max + 0.5f : panX < max - 0.5f;
    }

    private void computeBase() {
        Drawable d = getDrawable();
        int w = getWidth();
        int h = getHeight();
        if (d == null || w == 0 || h == 0) return;
        float dw = d.getIntrinsicWidth();
        float dh = d.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) return;
        float s = Math.min(w / dw, h / dh);
        baseW = dw * s;
        baseH = dh * s;
        base.reset();
        base.postScale(s, s);
        base.postTranslate((w - baseW) / 2f, (h - baseH) / 2f);
        scale = 1f;
        panX = 0;
        panY = 0;
        apply();
    }

    private void zoomAt(float x, float y, float k) {
        float newScale = clamp(scale * k, 1f, MAX_SCALE);
        k = newScale / scale;
        if (k == 1f) return;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        panX = (x - cx) * (1 - k) + k * panX;
        panY = (y - cy) * (1 - k) + k * panY;
        scale = newScale;
        if (scale == 1f) {
            panX = 0;
            panY = 0;
        }
        clampPan();
        apply();
    }

    private void apply() {
        display.set(base);
        display.postScale(scale, scale, getWidth() / 2f, getHeight() / 2f);
        display.postTranslate(panX, panY);
        setImageMatrix(display);
    }

    private float maxPanX() {
        return Math.max(0, (baseW * scale - getWidth()) / 2f);
    }

    private float maxPanY() {
        return Math.max(0, (baseH * scale - getHeight()) / 2f);
    }

    private void clampPan() {
        float mx = maxPanX();
        float my = maxPanY();
        panX = clamp(panX, -mx, mx);
        panY = clamp(panY, -my, my);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}