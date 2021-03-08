package com.pdlpdl.minecraft.packetlog.model;

import com.github.steveice10.packetlib.packet.Packet;

public class TracedPacket {
    private final Packet packet;
    private final long timestamp;

    public TracedPacket(Packet packet, long timestamp) {
        this.packet = packet;
        this.timestamp = timestamp;
    }

    public Packet getPacket() {
        return packet;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
