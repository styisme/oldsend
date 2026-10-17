package org.oldsend.transfer;

import android.os.Environment;
import java.io.File;

public class FileSaver {
    public static File createTarget(String fileName, boolean saveAlbum) {
        File dir = getTargetDir(fileName, saveAlbum);
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

    private static File getTargetDir(String name, boolean saveAlbum) {
        String lower = name.toLowerCase();
        // 图片依然进相册
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OldSend");
        } 
        // 视频进 Movies
        else if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi")) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "OldSend");
        } 
        // 音频进 Music
        else if (lower.endsWith(".mp3") || lower.endsWith(".wav")) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "OldSend");
        }
        // 其他所有文件（包括 apk、txt、zip）默认进系统的 Download/OldSend 目录
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OldSend");
    }

    private static String sanitize(String name) {
        if (name == null || name.length() == 0) return "file";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '/' || c == '\\' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|') sb.append('_');
            else sb.append(c);
        }
        return sb.length() == 0 ? "file" : sb.toString();
    }
}
