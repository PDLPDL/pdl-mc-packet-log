/*
 * Copyright (c) 2021 Playful Digital Learning LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
