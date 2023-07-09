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
import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.minecraft.packetlog.model.TracedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

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
public class PacketFileReader implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PacketFileReader.class);

    private final InputStream upstream;
    private final MinecraftCodecHelper minecraftCodecHelper;
    private final DataInputStream dataInputStream;

    public PacketFileReader(InputStream upstream) {
        this.upstream = upstream;
        this.minecraftCodecHelper = new MinecraftCodecHelper(Int2ObjectMaps.EMPTY_MAP, Collections.EMPTY_MAP);
        this.dataInputStream = new DataInputStream(upstream);
    }

    @Override
    public void close() throws IOException {
        this.dataInputStream.close();
    }

    public TracedPacket read() throws IOException {
        TracedPacket result = null;

        byte[] rawPayload = this.readRawPacket();

        if (rawPayload != null) {
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeBytes(rawPayload);

            try {
                long timestamp = byteBuf.readLong();
                String className = this.readString(byteBuf);

                Packet packet = this.processPayload(className, byteBuf);

                if (byteBuf.readableBytes() != 0) {
                    LOG.warn("PACKET appears to have only been partially read: class-name={}; total-size={}; remaining={}",
                            className, rawPayload.length, byteBuf.readableBytes());
                }

                result = new TracedPacket(packet, timestamp);
            } finally {
                byteBuf.release();
            }
        }

        return result;
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Format the raw packet, which contains all of the data EXCEPT for the packet length.
     *
     * @return
     * @throws IOException
     */
    private byte[] readRawPacket() throws IOException {
        int size;

        try {
            size = this.dataInputStream.readInt();
        } catch (EOFException eofExc) {
            // Happens normally on EOF.  NOTE we don't distinguish 0 bytes from < 4 here.
            return null;
        }

        try {
            byte[] rawPacket = this.readFully(this.upstream, size);

            return rawPacket;
        } catch (EOFException eofExc) {
            throw new IOException("Malformed packet; appears to be short: expected size = " + size);
        }
    }

    private String readString(ByteBuf byteBuf) {
        int len = byteBuf.readInt();
        byte[] copyBuf = new byte[len];
        byteBuf.readBytes(copyBuf);

        return new String(copyBuf, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("rawtypes")
    private Packet processPayload(String packetClassName, ByteBuf byteBuf) throws IOException {
        try {
            Class packetClass = Class.forName(packetClassName);
            Packet packet = createPacketInstance(packetClass, byteBuf);

            return packet;
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException exc) {
            throw new RuntimeException("Problem processing packet with class name " + packetClassName, exc);
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Packet createPacketInstance(Class clazz, ByteBuf byteBuf) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor constructor = clazz.getDeclaredConstructor(ByteBuf.class, MinecraftCodecHelper.class);
        if(!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }

        return (Packet) constructor.newInstance(byteBuf, this.minecraftCodecHelper);
    }

    private byte[] readFully(InputStream inputStream, int size) throws IOException {
        byte[] buffer;

        buffer = new byte[size];

        int remaining = size;
        int offset = 0;
        while (remaining > 0) {
            int readCount = inputStream.read(buffer, offset, remaining);

            if (readCount == -1) {
                throw new EOFException("Required " + size + " bytes; only read " + (size - remaining));
            }

            remaining -= readCount;
            offset += readCount;
        }

        return buffer;
    }
}
