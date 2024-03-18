package com.example.captive_portal_analyzer;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class MyVpnService extends VpnService {
    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    // Configure a builder for the interface.
    Builder builder = new Builder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runVpnConnection();
                } catch (Exception e) {
                    // Catch any exceptions
                }
            }
        }, "MyVpnRunnable");

        mThread.start();
        return START_STICKY;
    }

    private void runVpnConnection() throws Exception {
        mInterface = builder.setSession("MyVPNService")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .establish();

        FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());

        // Initialize FileOutputStream for the pcap file
        String pcapFilePath = "path/to/your/pcapfile.pcap"; // Update this path
        FileOutputStream pcapFileOutputStream = new FileOutputStream(pcapFilePath);

        // Continuously capture packets
        byte[] packetBuffer = new byte[4096]; // Adjust buffer size as needed
        while (true) {
            int bytesRead = in.read(packetBuffer);
            if (bytesRead > 0) {
                // Prepare the PCAP packet header
                long timestamp = System.currentTimeMillis() / 1000; // Convert to seconds
                int originalPacketLength = bytesRead; // Original packet length
                int actualDataLength = bytesRead; // Actual data length (no truncation)

                // Create the PCAP packet header (total 16 bytes)
                ByteBuffer pcapHeader = ByteBuffer.allocate(16);
                pcapHeader.putInt((int) timestamp); // 4 bytes: Timestamp (seconds)
                pcapHeader.putInt(0); // 4 bytes: Timestamp microseconds (set to 0)
                pcapHeader.putInt(actualDataLength); // 4 bytes: Actual data length
                pcapHeader.putInt(originalPacketLength); // 4 bytes: Original packet length

                // Write the pcap header
                pcapFileOutputStream.write(pcapHeader.array());

                // Write the actual packet data
                pcapFileOutputStream.write(packetBuffer, 0, actualDataLength);
            }
        }
    }
}
