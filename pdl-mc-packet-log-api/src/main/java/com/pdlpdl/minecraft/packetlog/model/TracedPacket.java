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
