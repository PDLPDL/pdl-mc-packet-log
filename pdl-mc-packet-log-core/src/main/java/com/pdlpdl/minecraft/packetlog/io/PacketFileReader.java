package com.pdlpdl.minecraft.packetlog.io;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.minecraft.packetlog.model.TracedPacket;
import sun.misc.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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

    private final InputStream upstream;
    private final StreamNetInput reader;

    public PacketFileReader(InputStream upstream) {
        this.upstream = upstream;
        this.reader = new StreamNetInput(this.upstream);
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    public TracedPacket read() throws IOException {
        TracedPacket result = null;

        byte[] rawPayload = this.readRawPacket();

        if (rawPayload != null) {
            ByteArrayInputStream buffer = new ByteArrayInputStream(rawPayload);
            NetInput netInput = new StreamNetInput(buffer);

            long timestamp = netInput.readLong();
            String className = netInput.readString();

            Packet packet = this.processPayload(className, netInput);

            result = new TracedPacket(packet, timestamp);
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
            size = this.reader.readInt();
        } catch (EOFException eofExc) {
            // Happens normally on EOF.  NOTE we don't distinguish 0 bytes from < 4 here.
            return null;
        }

        try {
            byte[] rawPacket = IOUtils.readFully(this.upstream, size, true);

            return rawPacket;
        } catch (EOFException eofExc) {
            throw new IOException("Malformed packet; appears to be short: expected size = " + size);
        }
    }

    @SuppressWarnings("rawtypes")
    private Packet processPayload(String packetClassName, NetInput netInput) throws IOException {
        try {
            Class packetClass = Class.forName(packetClassName);
            Packet packet = createPacketInstance(packetClass);
            packet.read(netInput);

            return packet;
        } catch (EOFException eofExc) {
            throw new RuntimeException("Unexpected EOF while reading packet");
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException exc) {
            throw new RuntimeException("Problem processing packet with class name " + packetClassName, exc);
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Packet createPacketInstance(Class clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor constructor = clazz.getDeclaredConstructor();
        if(!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }

        return (Packet) constructor.newInstance();
    }
}
