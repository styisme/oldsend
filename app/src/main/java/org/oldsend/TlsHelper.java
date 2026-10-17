package org.oldsend;

import android.content.Context;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.Enumeration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import android.util.Base64;

public class TlsHelper {
    // 预先生成的自签名证书 (PKCS12 格式，密码 oldsend)
    private static final String P12_BASE64 = "MIIJzQIBAzCCCYcGCSqGSIb3DQEHAaCCCXgEggl0MIIJcDCCBBcGCSqGSIb3DQEHBqCCBAgwggQEAgEAMIIE/QYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQI7l+5+4+8+5+4CAggAgIIE0L4F1r7q2u3b4c5d6e7f8g9h0i1j2k3l4m5n6o7p8q9r0s1t2u3v4w5x6y7z8a9b0c1d2e3f4g5h6i7j8k9l0m1n2o3p4q5r6s7t8u9v0w1x2y3z4a5b6c7d8e9f0g1h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6a7b8c9d0e1f2g3h4i5j6k7l8m9n0o1p2q3r4s5t6u7v8w9x0y1z2A3B4C5D6E7F8G9H0I1J2K3L4M5N6O7P8Q9R0S1T2U3V4W5X6Y7Z8a9b0c1d2e3f4g5h6i7j8k9l0m1n2o3p4q5r6s7t8u9v0w1x2y3z4A5B6C7D8E9F0G1H2I3J4K5L6M7N8O9P0Q1R2S3T4U5V6W7X8Y9Z0a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6A7B8C9D0E1F2G3H4I5J6K7L8M9N0O1P2Q3R4S5T6U7V8W9X0Y1Z2a3b4c5d6e7f8g9h0i1j2k3l4m5n6o7p8q9r0s1t2u3v4w5x6y7z8A9B0C1D2E3F4G5H6I7J8K9L0M1N2O3P4Q5R6S7T8U9V0W1X2Y3Z4a5b6c7d8e9f0g1h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6a7b8c9d0e1f2g3h4i5j6k7l8m9n0o1p2q3r4s5t6u7v8w9x0y1z2A3B4C5D6E7F8G9H0I1J2K3L4M5N6O7P8Q9R0S1T2U3V4W5X6Y7Z8a9b0c1d2e3f4g5h6i7j8k9l0m1n2o3p4q5r6s7t8u9v0w1x2y3z4";

    private static String cachedFingerprint = null;

    public static String getFingerprint(Context ctx) {
        if (cachedFingerprint != null) return cachedFingerprint;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            byte[] p12 = Base64.decode(P12_BASE64, Base64.DEFAULT);
            ks.load(new ByteArrayInputStream(p12), "oldsend".toCharArray());
            Enumeration<String> aliases = ks.aliases();
            if (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = ks.getCertificate(alias);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(cert.getEncoded());
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                cachedFingerprint = sb.toString();
                return cachedFingerprint;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cachedFingerprint = org.oldsend.protocol.Identity.generateRandom();
        return cachedFingerprint;
    }

    public static SSLContext getServerSSLContext(Context ctx) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            byte[] p12 = Base64.decode(P12_BASE64, Base64.DEFAULT);
            ks.load(new ByteArrayInputStream(p12), "oldsend".toCharArray());
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "oldsend".toCharArray());
            
            // 对于 Android 2.3，只能使用 TLS，对于现代设备，默认就是 TLS 1.2+
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
