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
            String address = parts[1];
            int port = Integer.parseInt(parts[2]);
            dsocket = new DatagramSocket(port, InetAddress.getByName(address));
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
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

        int size = this instanceof MC ? 1500 : 512;

        byte[] buffer = new byte[size];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        int threadPoolSize = JDNSS.jargs.getThreads();
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);

        while (true)
        {
            try
            {
                dsocket.receive(packet);
            }
            catch (IOException ioe)
            {
                logger.catching(ioe);
                continue;
            }

            Future f = pool.submit(
                new UDPThread(
                    Utils.trimByteArray(packet.getData(), packet.getLength()),
                    dsocket, packet.getPort(), packet.getAddress()
                )
            );

            // if we're only supposed to answer once, and we're the first,
            // bring everything down with us.
            if (JDNSS.jargs.isOnce()) {
                while (!f.isDone()) {
                    try {
                        sleep(100);

                    } catch (InterruptedException IE) {
                    }
                }
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

                System.exit(0);
            }
        }
    }
}
