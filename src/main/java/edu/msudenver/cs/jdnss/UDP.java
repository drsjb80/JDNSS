package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.lang.AssertionError;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

/**
 * This class is used by UDP and for extended for MC queries.
 */
class UDP extends Thread
{
    protected DatagramSocket dsocket;
    protected JDNSS dnsService;
    protected Logger logger = JDNSS.getLogger();
    protected int threadPoolSize = JDNSS.getJargs().threads;
    protected int port = JDNSS.getJargs().port;
    protected String ipaddress = JDNSS.getJargs().IPaddress;

    public UDP() {} // don't do anything, let MC() do all the work.

    public UDP(JDNSS dnsService) throws SocketException, UnknownHostException
    {
        logger.traceEntry(new ObjectMessage(dnsService));
        this.dnsService = dnsService;

        try
        {
            if (ipaddress != null)
            {
                dsocket = new DatagramSocket(port,
                    InetAddress.getByName(ipaddress));
            }
            else
            {
                dsocket = new DatagramSocket(port);
            }
        }
        catch (UnknownHostException uhe)
        {
            logger.catching(uhe);
            throw uhe;
        }
        catch (SocketException se)
        {
            logger.catching(se);
            throw se;
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
        if (this instanceof MC)
        {
            // logger.fatal("In MC run");
            // logger.fatal(Utils.toString((MulticastSocket)dsocket));
        }

        byte[] buffer = new byte[size];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);

        while (true)
        {
            Query q = null;
            try
            {
                dsocket.receive(packet);
                q = new Query(Utils.trimByteArray(packet.getData(),
                    packet.getLength()));
            }
            catch (IOException ioe)
            {
                logger.catching(ioe);
                continue;
            }
            catch (AssertionError ae)
            {
                logger.catching(ae);
                continue;
            }

            // logger.fatal("Port: " + packet.getPort());
            // logger.fatal("Address: " + packet.getAddress());
            Future f = pool.submit(new UDPThread(q, dsocket, packet.getPort(),
                packet.getAddress(), dnsService));

            // if we're only supposed to answer once, and we're the first,
            // bring everything down with us.
            if (JDNSS.getJargs().once)
            {   
                try 
                {   
                    f.get();
                }
                catch (InterruptedException ie)
                {   
                    logger.catching(ie);
                }
                catch (ExecutionException ee)
                {   
                    logger.catching(ee);
                }

                System.exit(0);
            }
        }
    }
}
