package org.oldsend.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
        
        // 1. 换背景
        if (rootView != null) {
            rootView.setBackgroundResource(R.drawable.bg_md3_card_dark);
        }
        
        // 2. 换标题颜色
        TextView title = (TextView) dialog.findViewById(R.id.dlg_title);
        if (title != null) title.setTextColor(Color.parseColor("#E0E0E0"));
        
        // 3. 换按钮颜色
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

        View root = dialog.findViewById(R.id.dlg_content_container).getParent().getParent();
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
            if (posListener != null) {
                btnPos.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { posListener.onClick(dialog, 0); }
                });
            }
        } else btnPos.setVisibility(View.GONE);

        if (negative != null) {
            btnNeg.setText(negative);
            btnNeg.setVisibility(View.VISIBLE);
            if (negListener != null) {
                btnNeg.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { negListener.onClick(dialog, 0); }
                });
            }
        } else btnNeg.setVisibility(View.GONE);

        return dialog;
    }

    public static Dialog showConfirm(Context ctx, String title, String message, String positive, DialogInterface.OnClickListener posListener) {
        TextView msg = new TextView(ctx);
        msg.setText(message);
        msg.setTextSize(14);
        msg.setTextColor(ThemeUtils.isDarkMode(ctx) ? Color.parseColor("#AAAAAA") : Color.parseColor("#49454F"));
        return createBaseDialog(ctx, title, msg, positive, posListener, I18nManager.get("cancel"), null);
    }

    public static Dialog showInput(final Context ctx, String title, String defaultValue, final OnInputListener listener) {
        final EditText input = new EditText(ctx);
        input.setText(defaultValue);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setPadding(24, 16, 24, 16);
        
        // 适配输入框
        if (ThemeUtils.isDarkMode(ctx)) {
            input.setBackgroundColor(Color.parseColor("#2C2C2C"));
            input.setTextColor(Color.parseColor("#E0E0E0"));
            input.setHintTextColor(Color.parseColor("#888888"));
            // 简单粗暴画个绿边
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
        TextView btnNeg = (TextView) dialog.findViewById(R.id.dlg_btn_negative);
        btnNeg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { dialog.dismiss(); }
        });
        
        return dialog;
    }
    
    public interface OnInputListener { void onInput(String text); }

    public static Dialog showList(Context ctx, String title, String[] items, final OnItemSelectListener listener) {
        Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_md3_list);
        setupWindow(ctx, dialog);

        View root = dialog.findViewById(R.id.dlg_list).getParent().getParent();
        applyTheme(ctx, dialog, root);

        ((TextView) dialog.findViewById(R.id.dlg_title)).setText(title);
        ListView listView = (ListView) dialog.findViewById(R.id.dlg_list);
        listView.setAdapter(new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, items));
        
        if (ThemeUtils.isDarkMode(ctx)) {
            listView.setBackgroundColor(Color.parseColor("#2C2C2C"));
            // 给 ListView 里的文字加个深色适配稍微麻烦，先不管，大多数系统会自动反色
        }

        final Dialog d = dialog;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                d.dismiss();
                if (listener != null) listener.onItemSelect(position);
            }
        });

        dialog.findViewById(R.id.dlg_btn_negative).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        return dialog;
    }
    
    public interface OnItemSelectListener { void onItemSelect(int position); }
    
    public static Dialog showCustom(Context ctx, String title, View contentView, String positive, DialogInterface.OnClickListener posListener) {
        return createBaseDialog(ctx, title, contentView, positive, posListener, I18nManager.get("close"), null);
    }
}
