package org.oldsend.transfer;

import android.os.Environment;
import java.io.File;

public class FileSaver {
    public static File createTarget(String fileName) {
        File dir = new File(Environment.getExternalStorageDirectory(), "OldSend");
        if (!dir.exists()) dir.mkdirs();
        String safe = sanitize(fileName);
        File f = new File(dir, safe);
        int i = 1;
        while (f.exists()) {
            int dot = safe.lastIndexOf('.');
            String base = dot > 0 ? safe.substring(0, dot) : safe;
            String ext = dot > 0 ? safe.substring(dot) : "";
            f = new File(dir, base + " (" + i + ")" + ext);
            i++;
        }
        return f;
    }

    private static String sanitize(String name) {
        if (name == null || name.length() == 0) return "file";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '/' || c == '\\' || c == ':' || c == '*' || c == '?'
                    || c == '"' || c == '<' || c == '>' || c == '|') {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "file" : sb.toString();
    }
}
