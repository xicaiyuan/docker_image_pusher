package com.wetype.voicebubble;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CalibrationActivity extends Activity {
    private EditText input;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppState.calibrationOpen = true;
        int p = Ui.dp(this, 18);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p,p,p,p);
        root.setBackgroundColor(Color.rgb(247,248,250));

        TextView title = Ui.text(this, "校准微信麦克风位置", 21, Color.rgb(15,23,42));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);
        TextView hint = Ui.text(this,
                "拖动悬浮麦克风，让它正好盖住微信输入法空格键上的麦克风图标，然后点“保存位置”。",
                14, Color.rgb(100,116,139));
        Ui.margins(hint,0,Ui.dp(this,8),0,Ui.dp(this,12));
        root.addView(hint);

        input = new EditText(this);
        input.setHint("点这里让微信输入法保持显示");
        input.setMinHeight(Ui.dp(this,56));
        input.setBackground(Ui.round(Color.WHITE, 16, this));
        root.addView(input);

        Button save = Ui.button(this, "保存位置", true);
        Ui.margins(save,0,Ui.dp(this,14),0,Ui.dp(this,8));
        save.setOnClickListener(v -> {
            BubbleAccessibilityService s = BubbleAccessibilityService.get();
            if (s != null) s.saveCalibration();
            finish();
        });
        root.addView(save);
        Button cancel = Ui.button(this, "取消", false);
        cancel.setOnClickListener(v -> finish());
        root.addView(cancel);
        setContentView(root);

        input.requestFocus();
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            BubbleAccessibilityService s = BubbleAccessibilityService.get();
            if (s != null) s.beginCalibration();
        }, 450);
    }

    @Override
    protected void onDestroy() {
        AppState.calibrationOpen = false;
        BubbleAccessibilityService s = BubbleAccessibilityService.get();
        if (s != null) s.restoreHome();
        super.onDestroy();
    }
}
