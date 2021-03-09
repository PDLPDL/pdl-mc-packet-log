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

import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import com.github.steveice10.packetlib.packet.Packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * FILE FORMAT:
 *      PACKET_RECORD ...
 *
 *      PACKET_RECORD =
 *          SIZE (4 bytes - length of the serialized packet that follows)
 *          TIMESTAMP (8 bytes)
 *          PACKET_CLASS_NAME (string: see packetlib)
 *          SERIALIZED_PACKET
 *
 * TODO:
 *      * Consider a dictionary for class names (e.g. "REGISTER_CLASSNAME_RECORD" with RECORD_ID and PACKET_NAME)
 */
public class PacketFileWriter implements AutoCloseable {

    private final OutputStream downstream;
    private final StreamNetOutput writer;

    public PacketFileWriter(OutputStream downstream) {
        this.downstream = downstream;
        this.writer = new StreamNetOutput(this.downstream);
    }

    @Override
    public void close() throws IOException {
        this.writer.flush();
        this.writer.close();
    }

    public void write(Packet packet, long timestamp) throws IOException {
        byte[] rawPacket = this.formatRawPacket(packet, timestamp);

        //
        // Write the packet length, then the raw packet content
        //
        this.writer.writeInt(rawPacket.length);
        this.writer.write(rawPacket);
    }

    public void flush() throws IOException {
        this.writer.flush();
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
        ByteArrayOutputStream bufferOutput = new ByteArrayOutputStream();
        NetOutput netToBuffer = new StreamNetOutput(bufferOutput);


        //
        // Write the timestamp
        //
        netToBuffer.writeLong(timestamp);

        //
        // Write the PACKET_CLASS_NAME
        //
        String packetName = packet.getClass().getName();
        netToBuffer.writeString(packetName);

        //
        // Write the PACKET_SERIALIZED
        //
        packet.write(netToBuffer);

        // Make sure the buffered output is complete.
        netToBuffer.flush();

        //
        // Return the raw data.
        //
        return bufferOutput.toByteArray();
    }
}
