package org.oldsend.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import org.oldsend.R;
import org.oldsend.util.I18nManager;

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

    public static Dialog createBaseDialog(Context ctx, String title, View contentView, String positive, DialogInterface.OnClickListener posListener, String negative, DialogInterface.OnClickListener negListener) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_md3_base);
        setupWindow(ctx, dialog);

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
        msg.setTextColor(0xFF49454F);
        return createBaseDialog(ctx, title, msg, positive, posListener, I18nManager.get("cancel"), null);
    }

    public static Dialog showInput(final Context ctx, String title, String defaultValue, final OnInputListener listener) {
        final EditText input = new EditText(ctx);
        input.setText(defaultValue);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setPadding(24, 16, 24, 16);
        input.setBackgroundResource(R.drawable.bg_md3_edittext);

        final Dialog dialog = createBaseDialog(ctx, title, input, I18nManager.get("confirm"), null, I18nManager.get("cancel"), null);
        
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        
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
    
    public static Dialog showCustom(Context ctx, String title, View contentView, String positive, DialogInterface.OnClickListener posListener) {
        return createBaseDialog(ctx, title, contentView, positive, posListener, I18nManager.get("close"), null);
    }
}
