package org.oldsend.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.oldsend.R;

public class ThemeUtils {
    public static boolean isDarkMode(Context ctx) {
        return ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE).getBoolean("dark_mode", false);
    }

    public static void applyTheme(Activity activity) {
        boolean isDark = isDarkMode(activity);
        if (!isDark) return;

        // 背景变黑
        activity.getWindow().getDecorView().setBackgroundColor(Color.parseColor("#121212"));

        View root = activity.findViewById(android.R.id.content);
        applyView(root, isDark);
    }

    private static void applyView(View view, boolean isDark) {
        // 1. 处理背景色
        Object tag = view.getTag();
        if ("card".equals(tag)) {
            view.setBackgroundResource(R.drawable.bg_md3_card_dark);
        } else if (view.getBackground() instanceof ColorDrawable) {
            int color = ((ColorDrawable) view.getBackground()).getColor();
            if (color == Color.parseColor("#F4F6F8")) {
                view.setBackgroundColor(Color.parseColor("#121212"));
            } else if (color == Color.parseColor("#FFFFFF")) {
                view.setBackgroundColor(Color.parseColor("#2C2C2C"));
            }
        }

        // 2. 处理文字颜色
        if (view instanceof TextView) {
            int textColor = ((TextView) view).getCurrentTextColor();
            if (textColor == Color.parseColor("#1C1B1F") || textColor == Color.parseColor("#333333")) {
                ((TextView) view).setTextColor(Color.parseColor("#E0E0E0"));
            } else if (textColor == Color.parseColor("#666666") || textColor == Color.parseColor("#49454F") || textColor == Color.parseColor("#888888")) {
                ((TextView) view).setTextColor(Color.parseColor("#AAAAAA"));
            }
        }

        // 3. 递归处理子 View
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyView(group.getChildAt(i), isDark);
            }
        }
    }
}
