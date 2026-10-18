package org.oldsend.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import org.oldsend.R;
import org.oldsend.util.I18nManager;
import org.oldsend.util.ThemeUtils;

public class Md3DialogUtils {

    private static void setupWindow(Context ctx, Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.9);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }

    private static void applyTheme(Context ctx, Dialog dialog, View rootView) {
        if (!ThemeUtils.isDarkMode(ctx)) return;
        if (rootView != null) rootView.setBackgroundResource(R.drawable.bg_md3_card_dark);
        
        TextView title = (TextView) dialog.findViewById(R.id.dlg_title);
        if (title != null) title.setTextColor(Color.parseColor("#E0E0E0"));
        
        TextView btnPos = (TextView) dialog.findViewById(R.id.dlg_btn_positive);
        if (btnPos != null) btnPos.setTextColor(Color.parseColor("#81C784"));
        
        TextView btnNeg = (TextView) dialog.findViewById(R.id.dlg_btn_negative);
        if (btnNeg != null) btnNeg.setTextColor(Color.parseColor("#AAAAAA"));
    }

    public static Dialog createBaseDialog(Context ctx, String title, View contentView, String positive, final DialogInterface.OnClickListener posListener, String negative, final DialogInterface.OnClickListener negListener) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_md3_base);
        setupWindow(ctx, dialog);

        View root = dialog.findViewById(R.id.dlg_root);
        applyTheme(ctx, dialog, root);

        TextView tvTitle = (TextView) dialog.findViewById(R.id.dlg_title);
        tvTitle.setText(title);

        LinearLayout container = (LinearLayout) dialog.findViewById(R.id.dlg_content_container);
        if (contentView != null) container.addView(contentView);

        TextView btnPos = (TextView) dialog.findViewById(R.id.dlg_btn_positive);
        TextView btnNeg = (TextView) dialog.findViewById(R.id.dlg_btn_negative);

        if (positive != null) {
            btnPos.setText(positive);
            btnPos.setVisibility(View.VISIBLE);
            // 确保点击事件绝对不丢失
            btnPos.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (posListener != null) posListener.onClick(dialog, 0);
                    else dialog.dismiss();
                }
            });
        } else btnPos.setVisibility(View.GONE);

        if (negative != null) {
            btnNeg.setText(negative);
            btnNeg.setVisibility(View.VISIBLE);
            btnNeg.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (negListener != null) negListener.onClick(dialog, 0);
                    else dialog.dismiss();
                }
            });
        } else btnNeg.setVisibility(View.GONE);

        return dialog;
    }

    public static Dialog showConfirm(Context ctx, String title, String message, String positive, DialogInterface.OnClickListener posListener) {
        TextView msg = new TextView(ctx);
        msg.setText(message);
        msg.setTextSize(14);
        msg.setTextColor(ThemeUtils.isDarkMode(ctx) ? Color.parseColor("#AAAAAA") : Color.parseColor("#49454F"));
        msg.setLineSpacing(0, 1.2f);
        return createBaseDialog(ctx, title, msg, positive, posListener, I18nManager.get("cancel"), null);
    }

    public static Dialog showInput(final Context ctx, String title, String defaultValue, final OnInputListener listener) {
        final EditText input = new EditText(ctx);
        input.setText(defaultValue);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setPadding(24, 16, 24, 16);
        
        if (ThemeUtils.isDarkMode(ctx)) {
            input.setBackgroundColor(Color.parseColor("#2C2C2C"));
            input.setTextColor(Color.parseColor("#E0E0E0"));
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(Color.parseColor("#2C2C2C"));
            gd.setCornerRadius(16f);
            gd.setStroke(2, Color.parseColor("#4CAF50"));
            input.setBackground(gd);
        } else {
            input.setBackgroundResource(R.drawable.bg_md3_edittext);
            input.setTextColor(Color.parseColor("#333333"));
        }

        final Dialog dialog = createBaseDialog(ctx, title, input, I18nManager.get("confirm"), null, I18nManager.get("cancel"), null);
        
        input.requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        
        TextView btnPos = (TextView) dialog.findViewById(R.id.dlg_btn_positive);
        btnPos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) listener.onInput(input.getText().toString());
                dialog.dismiss();
            }
        });
        
        return dialog;
    }
    
    public interface OnInputListener { void onInput(String text); }

    // 终极重写：抛弃 ListView，手搓安全列表
    public static Dialog showList(Context ctx, String title, String[] items, final OnItemSelectListener listener) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_md3_list);
        setupWindow(ctx, dialog);

        View root = dialog.findViewById(R.id.dlg_root);
        applyTheme(ctx, dialog, root);

        ((TextView) dialog.findViewById(R.id.dlg_title)).setText(title);
        
        LinearLayout container = (LinearLayout) dialog.findViewById(R.id.dlg_list_container);
        container.removeAllViews();
        
        boolean isDark = ThemeUtils.isDarkMode(ctx);

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            TextView tv = new TextView(ctx);
            tv.setText(items[i]);
            tv.setTextSize(16);
            tv.setPadding(32, 32, 32, 32);
            tv.setTextColor(isDark ? Color.parseColor("#E0E0E0") : Color.parseColor("#333333"));
            
            // 添加点击反馈效果（可选）
            android.util.TypedValue outValue = new android.util.TypedValue();
            ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            tv.setBackgroundResource(outValue.resourceId);
            
            tv.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                    if (listener != null) listener.onItemSelect(index);
                }
            });
            container.addView(tv);
            
            // 加个分割线
            if (i < items.length - 1) {
                View divider = new View(ctx);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(isDark ? Color.parseColor("#3E3E3E") : Color.parseColor("#E0E0E0"));
                container.addView(divider);
            }
        }

        dialog.findViewById(R.id.dlg_btn_negative).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { dialog.dismiss(); }
        });
        
        return dialog;
    }
    
    public interface OnItemSelectListener { void onItemSelect(int position); }
    
    public static Dialog showCustom(Context ctx, String title, View contentView, String positive, DialogInterface.OnClickListener posListener) {
        return createBaseDialog(ctx, title, contentView, positive, posListener, I18nManager.get("close"), null);
    }
}
