package com.pdlpdl.minecraft.packetlog.io;

import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.minecraft.packetlog.api.Clock;

import java.io.IOException;
import java.io.OutputStream;

public class ClockBasedPacketFileWriter implements AutoCloseable {

    private final Clock clock;
    private final PacketFileWriter downstream;

    public ClockBasedPacketFileWriter(Clock clock, OutputStream downstream) {
        this.clock = clock;
        this.downstream = new PacketFileWriter(downstream);
    }

    @Override
    public void close() throws IOException {
        this.downstream.close();
    }

    public void flush() throws IOException {
        this.downstream.flush();
    }

    public void write(Packet packet) throws IOException {
        long timestamp = this.clock.getTimestamp();
        this.downstream.write(packet, timestamp);
    }
}
