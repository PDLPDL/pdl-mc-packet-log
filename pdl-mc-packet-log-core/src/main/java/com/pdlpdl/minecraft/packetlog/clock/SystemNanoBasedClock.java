package com.pdlpdl.minecraft.packetlog.clock;

import com.pdlpdl.minecraft.packetlog.api.Clock;

public class SystemNanoBasedClock implements Clock {

    @Override
    public long getTimestamp() {
        return System.nanoTime();
    }
}
