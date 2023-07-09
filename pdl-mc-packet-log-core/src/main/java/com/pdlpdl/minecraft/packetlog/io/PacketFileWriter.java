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

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.packetlib.packet.Packet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * FILE FORMAT:
 *      PACKET_RECORD ...
 *
 *      PACKET_RECORD =
 *          SIZE (4 bytes - length of the rest of the packet excluding the size itself)
 *          TIMESTAMP (8 bytes)
 *          PACKET_CLASS_NAME (string: see packetlib)
 *          SERIALIZED_PACKET
 *
 * TODO:
 *      * Consider a dictionary for class names (e.g. "REGISTER_CLASSNAME_RECORD" with RECORD_ID and PACKET_NAME)
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;


public class PacketFileWriter implements AutoCloseable {

    private final OutputStream downstream;
    private final MinecraftCodecHelper minecraftCodecHelper;
    private final DataOutputStream dataOutputStream;

    public PacketFileWriter(OutputStream downstream) {
        this.downstream = downstream;
        this.minecraftCodecHelper = new MinecraftCodecHelper(Int2ObjectMaps.EMPTY_MAP, Collections.EMPTY_MAP);
        this.dataOutputStream = new DataOutputStream(downstream);
    }

    @Override
    public void close() throws IOException {
        this.dataOutputStream.flush();
        this.dataOutputStream.close();
    }

    public void write(Packet packet, long timestamp) throws IOException {
        byte[] rawPacket = this.formatRawPacket(packet, timestamp);

        //
        // Write the packet length, then the raw packet content
        //
        this.dataOutputStream.writeInt(rawPacket.length);
        this.dataOutputStream.write(rawPacket);
    }

    public void flush() throws IOException {
        this.dataOutputStream.flush();
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Format the raw packet, which contains all of the data EXCEPT for the packet length.
     *
     * @param packet
     * @param timestamp
     * @return
     * @throws IOException
     */
    private byte[] formatRawPacket(Packet packet, long timestamp) throws IOException {
        //
        // Prepare writing to buffer
        //
        ByteBuf buf = Unpooled.buffer();

        try {
            //
            // Write the timestamp
            //
            buf.writeLong(timestamp);

            //
            // Write the PACKET_CLASS_NAME
            //
            String packetName = packet.getClass().getName();
            buf.writeInt(packetName.length());
            buf.writeCharSequence(packetName, StandardCharsets.UTF_8);

            //
            // Write the PACKET_SERIALIZED
            //
            MinecraftPacket mp = (MinecraftPacket) packet;
            mp.serialize(buf, this.minecraftCodecHelper);

            // Copy out
            byte[] result = Arrays.copyOfRange(buf.array(), 0, buf.readableBytes());

            //
            // Return the raw data.
            //
            return result;
        } finally {
            buf.release();
        }
    }
}
