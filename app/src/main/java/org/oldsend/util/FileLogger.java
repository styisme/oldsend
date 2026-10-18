package org.oldsend.util;

import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {
    private static final String LOG_DIR = "OldSend";
    private static final String LOG_FILE = "debug_log.txt";

    public static void log(String tag, String msg) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "Download/" + LOG_DIR);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, LOG_FILE);
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            String line = time + " [" + tag + "] " + msg + "\n";
            FileWriter fw = new FileWriter(file, true);
            fw.write(line);
            fw.close();
        } catch (Exception ignored) {}
    }
}
