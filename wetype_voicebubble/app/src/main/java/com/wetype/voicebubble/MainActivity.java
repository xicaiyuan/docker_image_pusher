package com.wetype.voicebubble;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView state;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        int p = Ui.dp(this, 22);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p,p,p,p);
        root.setBackgroundColor(Color.rgb(247,248,250));

        TextView title = Ui.text(this, "微信语音悬浮球", 26, Color.rgb(15,23,42));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);
        TextView sub = Ui.text(this,
                "短按悬浮麦克风 → 打开微信输入法\n悬浮麦克风自动停到语音键上 → 按住说话 → 松手结束 → 自动复制",
                15, Color.rgb(71,85,105));
        Ui.margins(sub,0,Ui.dp(this,10),0,Ui.dp(this,18));
        root.addView(sub);

        state = Ui.text(this, "", 14, Color.rgb(15,157,140));
        Ui.margins(state,0,0,0,Ui.dp(this,14));
        root.addView(state);

        Button access = Ui.button(this, "1. 开启无障碍悬浮球", true);
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);

        Button calibrate = Ui.button(this, "2. 校准微信麦克风位置", false);
        Ui.margins(calibrate,0,Ui.dp(this,10),0,0);
        calibrate.setOnClickListener(v -> {
            if (BubbleAccessibilityService.get() == null) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } else {
                startActivity(new Intent(this, CalibrationActivity.class));
            }
        });
        root.addView(calibrate);

        TextView note = Ui.text(this,
                "建议第一次安装先做一次“校准”。之后悬浮球位置可直接拖动，应用会记住位置。\n\n识别完成后不会真的去点 MacroDroid 的“确定”按钮，而是在本应用内部直接确认并写入 Android 系统剪贴板，速度更快、也更稳定。",
                13, Color.rgb(100,116,139));
        Ui.margins(note,0,Ui.dp(this,18),0,0);
        root.addView(note);
        setContentView(root);
    }

    private void refresh() {
        boolean enabled = BubbleAccessibilityService.get() != null;
        state.setText(enabled ? "状态：已启用，悬浮麦克风应该已经出现" : "状态：未启用无障碍服务");
        state.setTextColor(enabled ? Color.rgb(15,157,140) : Color.rgb(220,38,38));
    }
}
