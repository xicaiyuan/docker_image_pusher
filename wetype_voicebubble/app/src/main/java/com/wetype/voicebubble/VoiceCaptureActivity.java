package com.wetype.voicebubble;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class VoiceCaptureActivity extends Activity {
    private static WeakReference<VoiceCaptureActivity> current = new WeakReference<>(null);

    private EditText input;
    private TextView status;
    private final Handler main = new Handler(Looper.getMainLooper());
    private long lastTextChange = 0L;
    private long openedAt = 0L;
    private boolean finished = false;

    public static VoiceCaptureActivity getCurrent() { return current.get(); }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        current = new WeakReference<>(this);
        AppState.captureOpen = true;
        AppState.resetVoice();
        openedAt = SystemClock.elapsedRealtime();
        buildUi();
        showKeyboardAndDock();
    }

    private void buildUi() {
        int pad = Ui.dp(this, 18);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(247, 248, 250));

        TextView title = Ui.text(this, "微信语音输入", 22, Color.rgb(15, 23, 42));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView hint = Ui.text(this,
                "悬浮麦克风会自动移动到微信输入法麦克风上方。\n按住它说话，松手就是结束。",
                14, Color.rgb(100, 116, 139));
        Ui.margins(hint, 0, Ui.dp(this, 8), 0, Ui.dp(this, 14));
        root.addView(hint);

        input = new EditText(this);
        input.setTextSize(18f);
        input.setTextColor(Color.rgb(15, 23, 42));
        input.setHint("识别文字会出现在这里…");
        input.setHintTextColor(Color.rgb(148, 163, 184));
        input.setMinHeight(Ui.dp(this, 96));
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        GradientDrawable box = Ui.round(Color.WHITE, 18, this);
        box.setStroke(Ui.dp(this, 1), Color.rgb(226, 232, 240));
        input.setBackground(box);
        root.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        status = Ui.text(this, "正在打开微信输入法…", 14, Color.rgb(15, 157, 140));
        Ui.margins(status, 0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        root.addView(status);

        Button cancel = Ui.button(this, "取消", false);
        cancel.setOnClickListener(v -> finish());
        root.addView(cancel);

        setContentView(root);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                lastTextChange = SystemClock.elapsedRealtime();
                if (s != null && s.length() > 0) scheduleStableFallback();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    private void showKeyboardAndDock() {
        input.requestFocus();
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        main.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        }, 120);
        main.postDelayed(() -> {
            BubbleAccessibilityService s = BubbleAccessibilityService.get();
            if (s != null) {
                s.dockOverWechatMic();
                setStatus("按住悬浮麦克风说话，松手结束");
            } else {
                setStatus("请先在系统“无障碍”中启用“微信语音悬浮球”");
            }
        }, 520);
    }

    public void setStatus(String text) {
        runOnUiThread(() -> { if (status != null) status.setText(text); });
    }

    public void maybeFinishAfterRecognition() {
        main.postDelayed(() -> {
            if (finished) return;
            String text = input == null ? "" : input.getText().toString().trim();
            long stableFor = SystemClock.elapsedRealtime() - lastTextChange;
            if (AppState.voiceEnded && !text.isEmpty() && stableFor >= 350) {
                confirmAndCopy(text);
            } else if (AppState.voiceEnded) {
                maybeFinishAfterRecognition();
            }
        }, 380);
    }

    private void scheduleStableFallback() {
        main.postDelayed(() -> {
            if (finished || input == null) return;
            String text = input.getText().toString().trim();
            long now = SystemClock.elapsedRealtime();
            long stableFor = now - lastTextChange;
            long openFor = now - openedAt;

            if (text.isEmpty() || openFor < 1800) return;

            if (AppState.voiceEnded && stableFor >= 350) {
                confirmAndCopy(text);
                return;
            }

            if (!AppState.voiceSeen && stableFor >= 1400) {
                confirmAndCopy(text);
                return;
            }

            if (AppState.voiceSeen && !AppState.voiceEnded && stableFor >= 2400) {
                confirmAndCopy(text);
            }
        }, 1500);
    }

    private void confirmAndCopy(String text) {
        if (finished || text.isEmpty()) return;
        finished = true;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("微信语音", text));
        Toast.makeText(this, "已复制：" + text, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        AppState.captureOpen = false;
        AppState.resetVoice();
        BubbleAccessibilityService s = BubbleAccessibilityService.get();
        if (s != null) s.restoreHome();
        current.clear();
        super.onDestroy();
    }
}
