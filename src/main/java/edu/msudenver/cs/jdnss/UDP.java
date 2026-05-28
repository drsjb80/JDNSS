package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class is used by UDP and for extended for MC queries.
 */
class UDP extends Thread
{
    DatagramSocket dsocket;
    final Logger logger = JDNSS.logger;

    UDP() {} // needed for MC subclass

    public UDP(String[] parts) {
        try {
            NetworkBinding binding = NetworkBinding.fromParts(parts);
            dsocket = new DatagramSocket(binding.getPort(), InetAddress.getByName(binding.getHost()));
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
    }

    int packetSize() {
        return this instanceof MC ? 1500 : 512;
    }

    ExecutorService createThreadPool() {
        int threadPoolSize = JDNSS.jargs.getThreads();
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    void receivePacket(final DatagramPacket packet) throws IOException {
        dsocket.receive(packet);
    }

    Future<?> submitPacketTask(final ExecutorService pool, final DatagramPacket packet) {
        return pool.submit(
                new UDPThread(
                        Utils.trimByteArray(packet.getData(), packet.getLength()),
                        dsocket, packet.getPort(), packet.getAddress()
                )
        );
    }

    void waitForCompletion(final Future<?> future) {
        while (!future.isDone()) {
            try {
                sleep(100);

            } catch (InterruptedException IE) {
            }
        }
    }

    void exitProcess(final int statusCode) {
        System.exit(statusCode);
    }

    /**
     * This method is used by both UDP and MC
     */
    public void run()
    {
        logger.traceEntry();

        /*
        "Multicast DNS Messages carried by UDP may be up to the IP MTU of the
        physical interface, less the space required for the IP header(20
        bytes for IPv4; 40 bytes for IPv6) and the UDP header(8 bytes)."

        Ergo, we should probably calculate those values here instead of
        just going with 1500.
        */

        int size = packetSize();

        byte[] buffer = new byte[size];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        ExecutorService pool = createThreadPool();

        while (true)
        {
            try
            {
                receivePacket(packet);
            }
            catch (IOException ioe)
            {
                logger.catching(ioe);
                continue;
            }

            Future<?> f = submitPacketTask(pool, packet);

            // if we're only supposed to answer once, and we're the first,
            // bring everything down with us.
            if (JDNSS.jargs.isOnce()) {
                waitForCompletion(f);
                /*
                try 
                {   
                    f.get();
                }
                catch (InterruptedException | ExecutionException ie)
                {   
                    logger.catching(ie);
                }
                */

                exitProcess(0);
            }
        }
    }
}
