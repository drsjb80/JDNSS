package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.lang.AssertionError;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

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

    public UDP() {}	// for MC

    public UDP(JDNSS dnsService) throws SocketException, UnknownHostException
    {
        logger.traceEntry(new ObjectMessage(dnsService));
        this.dnsService = dnsService;

        int port = JDNSS.getJdnssArgs().port;
        String ipaddress = JDNSS.getJdnssArgs().IPaddress;

        logger.trace(port);
        logger.trace(ipaddress);

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

        logger.trace(dsocket);
        logger.traceExit();
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
        */

        int size = this instanceof MC ? 1500 : 512;

        byte[] buffer = new byte[size];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        Query q = null;

        while (true)
        {
            try
            {
                dsocket.receive(packet);
                q = new Query(Utils.trimByteArray
                   (packet.getData(), packet.getLength()));
            }
            catch (IOException e)
            {
                logger.catching(e);
                return;
            }
            catch (AssertionError ae)
            {
                logger.catching(ae);
                return;
            }

            // ignore if response
            Assertion.aver(q.getQR() != true);

            Thread t = new UDPThread(q, dsocket, packet.getPort(),
                packet.getAddress(), dnsService);
            t.start();

            // if we're only supposed to answer once, and we're the first,
            // bring everything down with us.
            if (JDNSS.getJdnssArgs().once)
            {   
                try 
                {   
                    t.join();
                }
                catch (InterruptedException e)
                {   
                    logger.catching(e);
                }
                logger.traceExit(0);
                System.exit(0);
            }
        }
    }
}
