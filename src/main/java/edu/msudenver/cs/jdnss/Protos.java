package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.lang.AssertionError;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

/**
 * This class implements threading and thread pools(semaphores) for each
 * of the three protocols(UDP, TCP, and MC).  It is here as escentially a
 * refactor of the previous three disparate classes that looked very
 * similar.  There is a run method in this class that calls "doit" routines
 * in the subclasses.  Those in turn create UDPThreads and TCPThreads that
 * perform the actual work.  I couldn't find an easy way for a super class
 * to call a subclasses' run method, but i'm probably missing somthing.
 * One thing this doesn't do yet is make sure only one instance of each is
 * listening -- should be an easy extension.
 */

public abstract class Protos extends Thread
{
    protected JDNSS dnsService;
    protected Logger logger = JDNSS.getLogger();

    public void run()
    {
        logger.traceEntry();

        Semaphore s = new Semaphore(JDNSS.getJdnssArgs().threads);

        while (true)
        {
            s.P();

            Thread t = null;
            try
            {
                t = doit();             // call down to derived class
            }
            catch (AssertionError AE)   // already dealt with below
            {
                s.V();
                continue;
            }
            catch (IOException ioe)     // already dealt with below
            {
                s.V();
                continue;
            }
                
            t.start();
            s.V();

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

    public abstract Thread doit() throws IOException, AssertionError;
}

/**
 * This class is used by UDP and for extended for MC queries.
 */
class UDP extends Protos
{
    protected DatagramSocket dsocket;

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
    public Thread doit() throws IOException, AssertionError
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

        try
        {
            dsocket.receive(packet);
            q = new Query(Utils.trimByteArray
               (packet.getData(), packet.getLength()));
        }
        catch (IOException e)
        {
            logger.catching(e);
            throw e;
        }
        catch (AssertionError ae)
        {
            logger.catching(ae);
            throw ae;
        }

        // ignore if response
        Assertion.aver(q.getQR() != true);

        Thread t = new UDPThread(q, dsocket, packet.getPort(),
            packet.getAddress(), dnsService);

        logger.traceExit(t);
        return t;
    }
}

class MC extends UDP
{
    public MC(JDNSS dnsService) throws SocketException, UnknownHostException,
        IOException
    {
        logger.traceEntry(new ObjectMessage(dnsService));
        this.dnsService = dnsService;

        MulticastSocket msocket = null;

        /**
        * Here is the difference for MC -- we need to join a group.
        */
        int port = JDNSS.getJdnssArgs().MCport;
        String address = JDNSS.getJdnssArgs().MCaddress;

        logger.trace(port);
        logger.trace(address);

        try
        {
            msocket = new MulticastSocket(port);
        }
        catch (IOException ioe)
        {
            logger.catching(ioe);
            throw ioe;
        }

        try
        {
            msocket.joinGroup(InetAddress.getByName(address));
            msocket.setTimeToLive(255);
        }
        catch (UnknownHostException uhe)
        {
            logger.catching(uhe);
            throw uhe;
        }
        catch (IOException ioe)
        {
            logger.catching(ioe);
            throw ioe;
        }

        dsocket = msocket;

        logger.trace(dsocket);
        logger.traceExit();
    }
}

class TCP extends Protos
{
    private ServerSocket ssocket;

    public TCP(JDNSS dnsService) throws UnknownHostException, IOException
    {
        this.dnsService = dnsService;
        logger.traceEntry(new ObjectMessage(dnsService));

        int port = JDNSS.getJdnssArgs().port;
        int backlog = JDNSS.getJdnssArgs().backlog;
        String ipaddress = JDNSS.getJdnssArgs().IPaddress;

        logger.trace(port);
        logger.trace(backlog);
        logger.trace(ipaddress);

        try
        {
            if (ipaddress != null)
            {
                ssocket = new ServerSocket (port, backlog,
                    InetAddress.getByName(ipaddress));
            }
            else if (backlog != 0)
            {
                ssocket = new ServerSocket(port, backlog);
            }
            else
            {
                ssocket = new ServerSocket(port);
            }
        }
        catch (UnknownHostException uhe)
        {
            logger.catching(uhe);
            throw uhe;
        }
        catch (IOException ioe)
        {
            logger.catching(ioe);
            throw ioe;
        }

        logger.traceExit();
    }

    public Thread doit() throws IOException
    {
        logger.traceEntry();

        Socket socket = null;

        try
        {
            socket = ssocket.accept();
        }
        catch (IOException ioe)
        {
            logger.catching(ioe);
            throw ioe;
        }

        logger.trace("Received TCP packet");

        Thread t = new TCPThread(socket, dnsService);
        logger.traceExit(t);
        return t;
    }
}
