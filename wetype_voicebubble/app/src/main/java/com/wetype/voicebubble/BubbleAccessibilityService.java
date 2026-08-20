package com.wetype.voicebubble;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.List;

public class BubbleAccessibilityService extends AccessibilityService {
    private static volatile BubbleAccessibilityService instance;

    private WindowManager wm;
    private MicBubbleView bubble;
    private WindowManager.LayoutParams lp;
    private SharedPreferences prefs;

    private float downRawX, downRawY;
    private int downWinX, downWinY;
    private long downAt;
    private boolean dragging;
    private int originalX, originalY;
    private int bubbleSize;

    private static final float DEFAULT_MIC_X = 351f / 720f;
    private static final float DEFAULT_MIC_Y = 1462f / 1560f;

    public static BubbleAccessibilityService get() { return instance; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        prefs = getSharedPreferences("bubble", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showBubble();
    }

    @Override
    public void onDestroy() {
        removeBubble();
        if (instance == this) instance = null;
        super.onDestroy();
    }

    private void showBubble() {
        if (bubble != null) return;
        bubbleSize = Ui.dp(this, 64);
        bubble = new MicBubbleView(this);

        Point screen = screenSize();
        int startX = prefs.getInt("home_x", Math.max(0, screen.x - bubbleSize - Ui.dp(this, 18)));
        int startY = prefs.getInt("home_y", Math.max(Ui.dp(this, 100), screen.y / 2 - bubbleSize / 2));

        lp = new WindowManager.LayoutParams(
                bubbleSize, bubbleSize,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = startX;
        lp.y = startY;
        lp.alpha = 1f;
        wm.addView(bubble, lp);
        makeTouchable();
    }

    private void makeTouchable() {
        if (bubble == null || lp == null) return;
        lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        lp.alpha = 1f;
        bubble.setDocked(false);
        bubble.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = e.getRawX();
                    downRawY = e.getRawY();
                    downWinX = lp.x;
                    downWinY = lp.y;
                    downAt = android.os.SystemClock.elapsedRealtime();
                    dragging = false;
                    bubble.setPressedVisual(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - downRawX;
                    float dy = e.getRawY() - downRawY;
                    int slop = ViewConfiguration.get(this).getScaledTouchSlop();
                    if (!dragging && (Math.abs(dx) > slop || Math.abs(dy) > slop)) dragging = true;
                    if (dragging) {
                        lp.x = downWinX + Math.round(dx);
                        lp.y = downWinY + Math.round(dy);
                        clampHomePosition();
                        safeUpdate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    bubble.setPressedVisual(false);
                    if (dragging) {
                        prefs.edit().putInt("home_x", lp.x).putInt("home_y", lp.y).apply();
                    } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                        long duration = android.os.SystemClock.elapsedRealtime() - downAt;
                        if (duration < 850) launchCapture();
                    }
                    return true;
            }
            return true;
        });
        safeUpdate();
    }

    private void launchCapture() {
        if (AppState.captureOpen || AppState.calibrationOpen) return;
        AppState.resetVoice();
        Intent i = new Intent(this, VoiceCaptureActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    public void dockOverWechatMic() {
        if (bubble == null || lp == null) return;
        originalX = lp.x;
        originalY = lp.y;
        Point s = screenSize();
        float rx = prefs.getFloat("mic_x_ratio", DEFAULT_MIC_X);
        float ry = prefs.getFloat("mic_y_ratio", DEFAULT_MIC_Y);
        int dockSize = Ui.dp(this, 56);
        lp.width = dockSize;
        lp.height = dockSize;
        lp.x = Math.round(s.x * rx - dockSize / 2f);
        lp.y = Math.round(s.y * ry - dockSize / 2f);
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        lp.alpha = 0.72f;
        bubble.setOnTouchListener(null);
        bubble.setDocked(true);
        safeUpdate();
        Toast.makeText(this, "麦克风已就位：按住悬浮麦克风说话，松手结束", Toast.LENGTH_SHORT).show();
    }

    public void restoreHome() {
        if (bubble == null || lp == null) return;
        lp.width = bubbleSize;
        lp.height = bubbleSize;
        if (originalX != 0 || originalY != 0) {
            lp.x = originalX;
            lp.y = originalY;
        } else {
            lp.x = prefs.getInt("home_x", lp.x);
            lp.y = prefs.getInt("home_y", lp.y);
        }
        makeTouchable();
    }

    public void beginCalibration() {
        if (bubble == null || lp == null) return;
        originalX = lp.x;
        originalY = lp.y;
        Point s = screenSize();
        float rx = prefs.getFloat("mic_x_ratio", DEFAULT_MIC_X);
        float ry = prefs.getFloat("mic_y_ratio", DEFAULT_MIC_Y);
        int sz = Ui.dp(this, 56);
        lp.width = sz;
        lp.height = sz;
        lp.x = Math.round(s.x * rx - sz / 2f);
        lp.y = Math.round(s.y * ry - sz / 2f);
        lp.alpha = 0.82f;
        lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        bubble.setDocked(true);
        bubble.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = e.getRawX(); downRawY = e.getRawY(); downWinX = lp.x; downWinY = lp.y;
                    bubble.setPressedVisual(true); return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x = downWinX + Math.round(e.getRawX() - downRawX);
                    lp.y = downWinY + Math.round(e.getRawY() - downRawY);
                    safeUpdate(); return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    bubble.setPressedVisual(false); return true;
            }
            return true;
        });
        safeUpdate();
    }

    public void saveCalibration() {
        if (lp == null) return;
        Point s = screenSize();
        float cx = (lp.x + lp.width / 2f) / Math.max(1f, s.x);
        float cy = (lp.y + lp.height / 2f) / Math.max(1f, s.y);
        prefs.edit().putFloat("mic_x_ratio", cx).putFloat("mic_y_ratio", cy).apply();
        Toast.makeText(this, "麦克风位置已保存", Toast.LENGTH_SHORT).show();
    }

    private void clampHomePosition() {
        Point s = screenSize();
        lp.x = Math.max(0, Math.min(lp.x, Math.max(0, s.x - lp.width)));
        lp.y = Math.max(0, Math.min(lp.y, Math.max(0, s.y - lp.height)));
    }

    private Point screenSize() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.graphics.Rect b = wm.getCurrentWindowMetrics().getBounds();
            return new Point(b.width(), b.height());
        }
        Point p = new Point();
        wm.getDefaultDisplay().getRealSize(p);
        return p;
    }

    private void safeUpdate() {
        try { if (bubble != null && bubble.isAttachedToWindow()) wm.updateViewLayout(bubble, lp); }
        catch (Exception ignored) {}
    }

    private void removeBubble() {
        try { if (bubble != null) wm.removeView(bubble); } catch (Exception ignored) {}
        bubble = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!AppState.captureOpen) return;
        boolean active = false;
        boolean recognizing = false;
        boolean normalKeyboard = false;

        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            for (AccessibilityWindowInfo window : windows) {
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) continue;
                TextFlags flags = new TextFlags();
                scan(root, flags);
                active |= flags.active;
                recognizing |= flags.recognizing;
                normalKeyboard |= flags.normalKeyboard;
                root.recycle();
            }
        } catch (Exception ignored) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                TextFlags flags = new TextFlags();
                scan(root, flags);
                active = flags.active;
                recognizing = flags.recognizing;
                normalKeyboard = flags.normalKeyboard;
            }
        }

        if (active) {
            AppState.markVoiceSeen();
            notifyCaptureState("正在听…松开悬浮麦克风即可结束");
        } else if (AppState.voiceSeen && (recognizing || normalKeyboard)) {
            AppState.markVoiceEnded();
            notifyCaptureState(recognizing ? "正在识别…" : "识别完成，正在自动复制…");
            VoiceCaptureActivity a = VoiceCaptureActivity.getCurrent();
            if (a != null) a.maybeFinishAfterRecognition();
        }
    }

    private static final class TextFlags {
        boolean active, recognizing, normalKeyboard;
    }

    private void scan(AccessibilityNodeInfo node, TextFlags flags) {
        if (node == null) return;
        CharSequence t = node.getText();
        CharSequence d = node.getContentDescription();
        checkText(t, flags);
        checkText(d, flags);
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = node.getChild(i);
            if (c != null) {
                scan(c, flags);
                c.recycle();
            }
        }
    }

    private void checkText(CharSequence cs, TextFlags flags) {
        if (cs == null) return;
        String s = cs.toString();
        if (s.contains("语音转文字中")) flags.active = true;
        if (s.contains("识别中")) flags.recognizing = true;
        if (s.contains("灵动表达")) flags.normalKeyboard = true;
    }

    private void notifyCaptureState(String text) {
        VoiceCaptureActivity a = VoiceCaptureActivity.getCurrent();
        if (a != null) a.setStatus(text);
    }

    @Override
    public void onInterrupt() {}
}
