package org.oldsend;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.oldsend.util.ThemeUtils;
import org.oldsend.util.I18nManager;
import java.io.File;
import java.util.List;

public class SelectListActivity extends Activity {
    private LinearLayout listContainer;
    private TextView tvFileCount, tvTotalSize;
    private Button btnDeleteAll, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_select_list);
        ThemeUtils.applyTheme(this);

        listContainer = (LinearLayout) findViewById(R.id.list_container);
        tvFileCount = (TextView) findViewById(R.id.tv_file_count);
        tvTotalSize = (TextView) findViewById(R.id.tv_total_size);
        btnDeleteAll = (Button) findViewById(R.id.btn_delete_all);
        btnBack = (Button) findViewById(R.id.btn_back);

        TextView tvSltTitle = (TextView) findViewById(R.id.tv_slt_title);
        if (tvSltTitle != null) tvSltTitle.setText(I18nManager.get("select_title"));
        if (btnDeleteAll != null) btnDeleteAll.setText(I18nManager.get("delete_all"));

        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        btnDeleteAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.pendingUris.clear();
                refreshList();
                Toast.makeText(SelectListActivity.this, I18nManager.get("all_deleted"), Toast.LENGTH_SHORT).show();
            }
        });

        refreshList();
    }

    private void refreshList() {
        listContainer.removeAllViews();
        List<Uri> uris = MainActivity.pendingUris;
        
        if (uris == null || uris.isEmpty()) {
            finish();
            return;
        }

        long totalSize = 0;
        ContentResolver resolver = getContentResolver();

        for (int i = 0; i < uris.size(); i++) {
            final Uri uri = uris.get(i);
            final int index = i;
            
            String name = I18nManager.get("unknown_file");
            long size = 0;
            
            try {
                Cursor cursor = resolver.query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        String n = cursor.getString(nameIndex);
                        if (n != null) name = n;
                    }
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                    cursor.close();
                }
            } catch (Exception ignored) {}
            
            if (size <= 0 && "file".equals(uri.getScheme())) {
                String path = uri.getPath();
                if (path != null) {
                    File f = new File(path);
                    if (f.exists()) {
                        if ("未知文件".equals(name)) name = f.getName();
                        size = f.length();
                    }
                }
            }

            totalSize += (size > 0 ? size : 0);
            String sizeStr = size > 1024*1024 ? String.format("%.1f MB", size/1024.0/1024.0) : String.format("%.1f KB", size/1024.0);

            View itemView = getLayoutInflater().inflate(R.layout.item_selected_file, null);
            TextView tvIcon = (TextView) itemView.findViewById(R.id.item_icon);
            TextView itemName = (TextView) itemView.findViewById(R.id.item_name);
            TextView itemSize = (TextView) itemView.findViewById(R.id.item_size);
            ImageView btnDelete = (ImageView) itemView.findViewById(R.id.btn_item_delete);

            if (tvIcon != null) {
                String lowerName = name.toLowerCase();
                if (lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg")) tvIcon.setText("🖼️");
                else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv")) tvIcon.setText("🎬");
                else if (lowerName.endsWith(".mp3") || lowerName.endsWith(".wav")) tvIcon.setText("🎵");
                else if (lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) tvIcon.setText("📦");
                else if (lowerName.endsWith(".apk")) tvIcon.setText("🤖");
                else tvIcon.setText("📄");
                
                if (ThemeUtils.isDarkMode(this)) tvIcon.setTextColor(0xFFE0E0E0);
            }
            
            if (itemName != null) {
                itemName.setText(name);
                if (ThemeUtils.isDarkMode(this)) itemName.setTextColor(0xFFE0E0E0);
            }
            
            if (itemSize != null) {
                itemSize.setText(size > 0 ? sizeStr : I18nManager.get("unknown_size"));
                if (ThemeUtils.isDarkMode(this)) itemSize.setTextColor(0xFFAAAAAA);
            }
            
            if (btnDelete != null) {
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        MainActivity.pendingUris.remove(index);
                        refreshList();
                    }
                });
            }

            listContainer.addView(itemView);
        }

        if (tvFileCount != null) tvFileCount.setText(String.format(I18nManager.get("file_count"), uris.size()));
        String totalSizeStr = totalSize > 1024*1024 ? String.format("%.1f MB", totalSize/1024.0/1024.0) : String.format("%.1f KB", totalSize/1024.0);
        if (tvTotalSize != null) tvTotalSize.setText(String.format(I18nManager.get("size_label"), (totalSize > 0 ? totalSizeStr : I18nManager.get("unknown_size"))));
    }
}