package org.oldsend.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ListView;
import android.widget.TextView;
import org.oldsend.R;
import org.oldsend.util.I18nManager;

public class Md3DialogUtils {

    public static Dialog createBaseDialog(Context ctx, String title, View contentView, String positive, DialogInterface.OnClickListener posListener, String negative, DialogInterface.OnClickListener negListener) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_md3_base);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int maxHeight = (int) (ctx.getResources().getDisplayMetrics().heightPixels * 0.85);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            // 强行限制最大高度防止超出屏幕
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
        }

        TextView tvTitle = (TextView) dialog.findViewById(R.id.dlg_title);
        tvTitle.setText(title);

        LinearLayout container = (LinearLayout) dialog.findViewById(R.id.dlg_content_container);
        ScrollView scrollView = (ScrollView) dialog.findViewById(R.id.dlg_scroll);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
        if (contentView != null) {
            container.addView(contentView);
        }

        TextView btnPos = (TextView) dialog.findViewById(R.id.dlg_btn_positive);
        TextView btnNeg = (TextView) dialog.findViewById(R.id.dlg_btn_negative);

        if (positive != null) {
            btnPos.setText(positive);
            btnPos.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { dialog.dismiss(); }
            });
            btnPos.setVisibility(View.VISIBLE);
        } else {
            btnPos.setVisibility(View.GONE);
        }

        if (negative != null) {
            btnNeg.setText(negative);
            btnNeg.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { dialog.dismiss(); }
            });
            btnNeg.setVisibility(View.VISIBLE);
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

    public static Dialog showCustom(Context ctx, String title, View contentView, String positive, DialogInterface.OnClickListener posListener) {
        return createBaseDialog(ctx, title, contentView, positive, posListener, "关闭", null);
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
        input.setSingleLine(true);
        
        Dialog dialog = createBaseDialog(ctx, title, input, I18nManager.get("confirm"), null, I18nManager.get("cancel"), null);
        dialog.findViewById(R.id.dlg_btn_positive).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) listener.onInput(input.getText().toString());
                ((Dialog) v.getTag() == null ? null : (Dialog) v.getTag()).dismiss();
            }
        });
        // 动态绑定 tag 以关闭
        dialog.findViewById(R.id.dlg_btn_positive).setTag(dialog);
        return dialog;
    }

    public interface OnInputListener { void onInput(String text); }

    public static Dialog showList(Context ctx, String title, String[] items, final OnItemSelectListener listener) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_md3_list);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int maxHeight = (int) (ctx.getResources().getDisplayMetrics().heightPixels * 0.85);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            // 强行限制最大高度防止超出屏幕
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
        }

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

        dialog.findViewById(R.id.dlg_btn_negative).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });

        return dialog;
    }
    
    public interface OnItemSelectListener { void onItemSelect(int position); }
}
