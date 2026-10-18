package org.oldsend.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class StunClient {
    private static final String STUN_SERVER = "stun.l.google.com";
    private static final int STUN_PORT = 19302;

    public static String getPublicAddress() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(3000);

            // 构建 20 字节的 STUN Binding Request
            ByteBuffer buffer = ByteBuffer.allocate(20);
            buffer.putShort((short) 0x0001); // Binding Request
            buffer.putShort((short) 0x0000); // Message Length
            buffer.putInt(0x2112A442);       // Magic Cookie
            for (int i = 0; i < 12; i++) buffer.put((byte) (Math.random() * 256)); // Transaction ID
            byte[] requestData = buffer.array();

            InetAddress address = InetAddress.getByName(STUN_SERVER);
            DatagramPacket request = new DatagramPacket(requestData, requestData.length, address, STUN_PORT);
            socket.send(request);

            byte[] responseData = new byte[1024];
            DatagramPacket response = new DatagramPacket(responseData, responseData.length);
            socket.receive(response);

            ByteBuffer resBuffer = ByteBuffer.wrap(response.getData(), 0, response.getLength());
            resBuffer.getShort(); // Type
            int msgLen = resBuffer.getShort() & 0xFFFF;
            resBuffer.getInt(); // Magic Cookie
            resBuffer.position(20); // Skip Transaction ID

            // 解析 Attributes 寻找 MAPPED-ADDRESS (0x0001) 或 XOR-MAPPED-ADDRESS (0x0020)
            while (resBuffer.position() < 20 + msgLen) {
                int attrType = resBuffer.getShort() & 0xFFFF;
                int attrLen = resBuffer.getShort() & 0xFFFF;
                
                if (attrType == 0x0020 || attrType == 0x0001) { // XOR-MAPPED-ADDRESS or MAPPED-ADDRESS
                    resBuffer.get(); // Reserved
                    int family = resBuffer.get() & 0xFF;
                    if (family == 0x01) { // IPv4
                        int port = resBuffer.getShort() & 0xFFFF;
                        byte[] ipBytes = new byte[4];
                        resBuffer.get(ipBytes);
                        
                        if (attrType == 0x0020) {
                            port ^= 0x2112;
                            ipBytes[0] ^= 0x21; ipBytes[1] ^= 0x12;
                            ipBytes[2] ^= 0xA4; ipBytes[3] ^= 0x42;
                        }
                        String ip = (ipBytes[0] & 0xFF) + "." + (ipBytes[1] & 0xFF) + "." + (ipBytes[2] & 0xFF) + "." + (ipBytes[3] & 0xFF);
                        return ip + ":" + port;
                    }
                }
                resBuffer.position(resBuffer.position() + attrLen);
            }
        } catch (Exception e) {
            return "探测失败: " + e.getMessage();
        } finally {
            if (socket != null) socket.close();
        }
        return "未知";
    }
}
