package com.pdlpdl.minecraft.log.tool.operation;

import com.pdlpdl.minecraft.packetlog.io.PacketFileReader;
import com.pdlpdl.minecraft.packetlog.model.TracedPacket;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class ReadOperation {

    private String filename;
    private boolean compressed;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public void run() {
        try {
            InputStream inputStream = new FileInputStream(this.filename);

            if (this.compressed) {
                inputStream = new GZIPInputStream(inputStream);
            }

            PacketFileReader reader = new PacketFileReader(inputStream);

            TracedPacket tracedPacket = reader.read();
            while (tracedPacket != null) {
                this.printPacket(tracedPacket);
                tracedPacket = reader.read();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

//========================================
// Internals
//----------------------------------------

    private void printPacket(TracedPacket tracedPacket) {
        System.out.println("[" + tracedPacket.getTimestamp() + "] " + tracedPacket.getPacket().toString());
    }
}
