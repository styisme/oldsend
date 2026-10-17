package org.oldsend.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import org.oldsend.R;
import org.oldsend.model.Device;

public class DeviceAdapter extends BaseAdapter {
    private final Context ctx;
    private final List<Device> data = new ArrayList<Device>();

    public DeviceAdapter(Context ctx) { this.ctx = ctx; }

    public void setData(List<Device> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public int getCount() { return data.size(); }
    public Device getItem(int pos) { return data.get(pos); }
    public long getItemId(int pos) { return pos; }

    public View getView(int pos, View cv, ViewGroup parent) {
        ViewHolder h;
        if (cv == null) {
            cv = LayoutInflater.from(ctx).inflate(R.layout.item_device, parent, false);
            h = new ViewHolder();
            h.alias = (TextView) cv.findViewById(R.id.item_alias);
            h.info = (TextView) cv.findViewById(R.id.item_info);
            cv.setTag(h);
        } else {
            h = (ViewHolder) cv.getTag();
        }
        Device d = data.get(pos);
        h.alias.setText(d.alias + (d.https ? " [TLS]" : ""));
        h.info.setText(d.ip + ":" + d.port + "  v" + d.version);
        return cv;
    }

    static class ViewHolder {
        TextView alias;
        TextView info;
    }
}
