package org.oldsend.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import org.oldsend.R;
import org.oldsend.util.I18nManager;
import org.oldsend.util.ThemeUtils;

public class Md3DialogUtils {

    private static boolean isAnimEnabled(Context ctx) {
        return ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE).getBoolean("use_anim", true);
    }

    private static void applyPressAnimation(View view, boolean animEnabled) {
        if (!animEnabled) return;
        final ScaleAnimation shrink = new ScaleAnimation(1f, 0.95f, 1f, 0.95f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        shrink.setDuration(100);
        shrink.setFillAfter(true);
        final ScaleAnimation grow = new ScaleAnimation(0.95f, 1f, 0.95f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        grow.setDuration(100);
        
        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.startAnimation(shrink);
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.startAnimation(grow);
                }
                return false;
            }
        });
    }

    private static void setupWindow(Context ctx, Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            if (!isAnimEnabled(ctx)) {
                // 如果关闭了动画，直接干掉系统自带的弹窗缩放动画
                window.setWindowAnimations(0); 
            }
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    public static Dialog createBaseDialog(Context ctx, String title, View contentView, String positive, DialogInterface.OnClickListener posListener, String negative, DialogInterface.OnClickListener negListener) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_md3_base);
        
        boolean isDark = ThemeUtils.isDarkMode(ctx);
        if (isDark) {
            View root = dialog.findViewById(R.id.dlg_title).getRootView();
            root.setBackgroundResource(R.drawable.bg_md3_card_dark);
        }
        
        
        setupWindow(ctx, dialog);
        boolean animEnabled = isAnimEnabled(ctx);

        TextView tvTitle = (TextView) dialog.findViewById(R.id.dlg_title);
        tvTitle.setText(title);

        LinearLayout container = (LinearLayout) dialog.findViewById(R.id.dlg_content_container);
        if (contentView != null) {
            container.addView(contentView);
        }

        TextView btnPos = (TextView) dialog.findViewById(R.id.dlg_btn_positive);
        TextView btnNeg = (TextView) dialog.findViewById(R.id.dlg_btn_negative);

        if (positive != null) {
            btnPos.setText(positive);
            btnPos.setVisibility(View.VISIBLE);
            applyPressAnimation(btnPos, animEnabled);
        } else {
            btnPos.setVisibility(View.GONE);
        }

        if (negative != null) {
            btnNeg.setText(negative);
            btnNeg.setVisibility(View.VISIBLE);
            applyPressAnimation(btnNeg, animEnabled);
        } else {
            btnNeg.setVisibility(View.GONE);
        }

        final DialogInterface.OnClickListener finalPos = posListener;
        final DialogInterface.OnClickListener finalNeg = negListener;
        
        if (finalPos != null) {
            btnPos.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { finalPos.onClick(dialog, 0); }
            });
        }
        if (finalNeg != null) {
            btnNeg.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { finalNeg.onClick(dialog, 0); }
            });
        }

        return dialog;
    }

    public static Dialog showConfirm(Context ctx, String title, String message, String positive, DialogInterface.OnClickListener posListener) {
        TextView msg = new TextView(ctx);
        msg.setText(message);
        msg.setTextSize(14);
        msg.setTextColor(0xFF49454F);
        return createBaseDialog(ctx, title, msg, positive, posListener, I18nManager.get("cancel"), null);
    }

    public static Dialog showInput(final Context ctx, String title, String defaultValue, final OnInputListener listener) {
        final EditText input = new EditText(ctx);
        input.setText(defaultValue);
        input.setGravity(android.view.Gravity.CENTER_VERTICAL);
        input.setPadding(20, 12, 20, 12);
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setBackgroundResource(R.drawable.bg_md3_edittext);
        
        Dialog dialog = createBaseDialog(ctx, title, input, I18nManager.get("confirm"), null, I18nManager.get("cancel"), null);
        dialog.findViewById(R.id.dlg_btn_positive).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) listener.onInput(input.getText().toString());
                ((Dialog) v.getTag()).dismiss();
            }
        });
        // 动态绑定 tag 以关闭
        dialog.findViewById(R.id.dlg_btn_positive).setTag(dialog);
        return dialog;
    }

    public interface OnInputListener { void onInput(String text); }

    public static Dialog showList(Context ctx, String title, String[] items, final OnItemSelectListener listener) {
        Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_md3_list);
        
        boolean isDark = ThemeUtils.isDarkMode(ctx);
        if (isDark) {
            View root = dialog.findViewById(R.id.dlg_title).getRootView();
            root.setBackgroundResource(R.drawable.bg_md3_card_dark);
        }
        
        
        setupWindow(ctx, dialog);
        boolean animEnabled = isAnimEnabled(ctx);

        ((TextView) dialog.findViewById(R.id.dlg_title)).setText(title);
        ListView listView = (ListView) dialog.findViewById(R.id.dlg_list);
        listView.setAdapter(new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, items));

        final Dialog d = dialog;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                d.dismiss();
                if (listener != null) listener.onItemSelect(position);
            }
        });

        View btnNeg = dialog.findViewById(R.id.dlg_btn_negative);
        applyPressAnimation(btnNeg, animEnabled);
        btnNeg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });

        return dialog;
    }
    
    public interface OnItemSelectListener { void onItemSelect(int position); }
    
    public static Dialog showCustom(Context ctx, String title, View contentView, String positive, DialogInterface.OnClickListener posListener) {
        return createBaseDialog(ctx, title, contentView, positive, posListener, I18nManager.get("close"), null);
    }
}