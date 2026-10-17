package org.oldsend.protocol;

import android.content.Context;
import org.oldsend.TlsHelper;
import android.content.SharedPreferences;

import java.security.MessageDigest;

public class Identity {
    private static final String PREF = "oldsend";
    private static final String KEY_FP = "fingerprint";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static String fingerprint;

    public static void init(Context ctx) {
        fingerprint = TlsHelper.getFingerprint(ctx);
        if (fingerprint != null) return;
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String fp = sp.getString(KEY_FP, null);
        if (fp == null) {
            fp = generateRandom();
            sp.edit().putString(KEY_FP, fp).commit();
        }
        fingerprint = fp;
    }

    public static String getFingerprint() {
        if (fingerprint == null) {
            fingerprint = "0000000000000000000000000000000000000000";
        }
        return fingerprint;
    }

    public static String generateRandom() {
        try {
            String seed = String.valueOf(System.currentTimeMillis())
                    + String.valueOf(Math.random())
                    + String.valueOf(android.os.Build.MODEL)
                    + String.valueOf(android.os.Build.ID);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(seed.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (int i = 0; i < d.length; i++) {
                int b = d[i] & 0xFF;
                sb.append(HEX[b >> 4]);
                sb.append(HEX[b & 0x0F]);
            }
            return sb.toString();
        } catch (Exception e) {
            return "0000000000000000000000000000000000000000";
        }
    }
}
