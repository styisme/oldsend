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

        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        btnDeleteAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.pendingUris.clear();
                refreshList();
                Toast.makeText(SelectListActivity.this, "已全部删除", Toast.LENGTH_SHORT).show();
            }
        });

        refreshList();
    }

    private void refreshList() {
        listContainer.removeAllViews();
        List<Uri> uris = MainActivity.pendingUris;
        
        if (uris.isEmpty()) {
            finish(); // 空了就自动返回主界面
            return;
        }

        long totalSize = 0;
        ContentResolver resolver = getContentResolver();

        for (int i = 0; i < uris.size(); i++) {
            final Uri uri = uris.get(i);
            final int index = i;
            
            String name = "未知文件";
            long size = 0;
            
            try {
                Cursor cursor = resolver.query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) name = cursor.getString(nameIndex);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex);
                    cursor.close();
                }
            } catch (Exception ignored) {}
            
            if (size == 0 && "file".equals(uri.getScheme())) {
                File f = new File(uri.getPath());
                if (f.exists()) { name = f.getName(); size = f.length(); }
            }

            totalSize += size;
            String sizeStr = size > 1024*1024 ? String.format("%.1f MB", size/1024.0/1024.0) : String.format("%.1f KB", size/1024.0);

            // 动态生成条目 View
            View itemView = getLayoutInflater().inflate(R.layout.item_selected_file, null);
            TextView itemName = (TextView) itemView.findViewById(R.id.item_name);
            TextView itemSize = (TextView) itemView.findViewById(R.id.item_size);
            ImageView btnDelete = (ImageView) itemView.findViewById(R.id.btn_item_delete);

            itemName.setText(name);
            itemSize.setText(sizeStr);
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    MainActivity.pendingUris.remove(index);
                    refreshList();
                }
            });

            listContainer.addView(itemView);
        }

        tvFileCount.setText("文件: " + uris.size());
        String totalSizeStr = totalSize > 1024*1024 ? String.format("%.1f MB", totalSize/1024.0/1024.0) : String.format("%.1f KB", totalSize/1024.0);
        tvTotalSize.setText("大小: " + totalSizeStr);
    }
}