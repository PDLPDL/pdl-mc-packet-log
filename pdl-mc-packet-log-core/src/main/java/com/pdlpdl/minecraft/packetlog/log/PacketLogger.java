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

package com.pdlpdl.minecraft.packetlog.log;

import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.minecraft.packetlog.io.ClockBasedPacketFileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * TO CONSIDER:
 *      - Log file rollover
 */
public class PacketLogger implements AutoCloseable {

    public static final int QUEUE_LIMIT = 10000;
    public static final int DEFAULT_THREAD_COUNT = 3;
    public static final int DEFAULT_IDLE_DELAY = 100;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(PacketLogger.class);

    private Logger log = DEFAULT_LOGGER;


    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private int idleDelay = DEFAULT_IDLE_DELAY;

    private final BlockingDeque<Packet> packetQueue = new LinkedBlockingDeque<>(QUEUE_LIMIT);

    private final ClockBasedPacketFileWriter writer;

    private boolean aborted = false;
    private boolean closed = false;

//========================================
// Constructor
//----------------------------------------

    public PacketLogger(ClockBasedPacketFileWriter writer) {
        this.writer = writer;
    }


//========================================
// Getters and Setters
//----------------------------------------

    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        return scheduledThreadPoolExecutor;
    }

    public void setScheduledThreadPoolExecutor(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
    }

//========================================
// Lifecycle
//----------------------------------------

    public void init() {
        if (this.scheduledThreadPoolExecutor == null) {
            this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                    DEFAULT_THREAD_COUNT
            );
        }

        this.scheduledThreadPoolExecutor.schedule(this::writeOnePacketFromQueue, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        // Don't immediately close the writer - allow the contents to flush
        this.closed = true;
    }

//========================================
// Interface
//----------------------------------------

    public void logPacket(Packet packet) {
        if ((! this.aborted) && (! this.closed)) {
            this.packetQueue.add(packet);
        } else {
            this.log.debug("discarding log packet: aborted={}; closed={}", this.aborted, this.closed);
        }
    }

//========================================
// Internals
//----------------------------------------

    private ScheduledFuture writeOnePacketFromQueue() {
        try {
            //
            // Don't loop here - use the scheduler to give "fairness" to all the tasks using these threads.
            //
            Packet nextPacket = this.packetQueue.poll();
            int delay = this.idleDelay;
            if (nextPacket != null) {
                this.writer.write(nextPacket);
                delay = 0;
            } else {
                //
                // If we are closed, now we can close the downstream and stop scheduling.
                //
                if (closed) {
                    this.log.debug("Closing the writer - the logger is closed and the queue is empty");

                    this.writer.close();
                    return null;
                }
            }

            //
            // Schedule the next iteration.
            //
            return this.scheduledThreadPoolExecutor.schedule(this::writeOnePacketFromQueue, delay, TimeUnit.MILLISECONDS);
        } catch (IOException ioExc) {
            //
            // Don't schedule any more iterations.
            //
            this.log.error("Error writing packet to log file; aborting the logger", ioExc);
            throw new RuntimeException("Failed logger on IO Exception", ioExc);
        }
    }
}
