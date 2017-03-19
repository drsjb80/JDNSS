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

import edu.msudenver.cs.javaln.JavaLN;

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
    protected JavaLN logger = JDNSS.getLogger();

    public void run()
    {
        logger.entering();

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
                    logger.throwing(e);
                }
                logger.exiting(0);
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
        logger.entering(dnsService);
        this.dnsService = dnsService;

        int port = JDNSS.getJdnssArgs().port;
        String ipaddress = JDNSS.getJdnssArgs().IPaddress;

        logger.finest(port);
        logger.finest(ipaddress);

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
            logger.throwing(uhe);
            throw uhe;
        }
        catch (SocketException se)
        {
            logger.throwing(se);
            throw se;
        }

        logger.finest(dsocket);
        logger.exiting();
    }

    /**
     * This method is used by both UDP and MC
     */
    public Thread doit() throws IOException, AssertionError
    {
        logger.entering();

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
            logger.throwing(e);
            throw e;
        }
        catch (AssertionError ae)
        {
            logger.throwing(ae);
            throw ae;
        }

        if (q.getQR() == true)
        {
            logger.finest("Response, exiting");
            return null;
        }

        Thread t = new UDPThread(q, dsocket, packet.getPort(),
            packet.getAddress(), dnsService);

        logger.exiting(t);
        return t;
    }
}

class MC extends UDP
{
    public MC(JDNSS dnsService) throws SocketException, UnknownHostException,
        IOException
    {
        logger.entering(dnsService);
        this.dnsService = dnsService;

        MulticastSocket msocket = null;

        /**
        * Here is the difference for MC -- we need to join a group.
        */
        int port = JDNSS.getJdnssArgs().MCport;
        String address = JDNSS.getJdnssArgs().MCaddress;

        logger.finest(port);
        logger.finest(address);

        try
        {
            msocket = new MulticastSocket(port);
        }
        catch (IOException ioe)
        {
            logger.throwing(ioe);
            throw ioe;
        }

        try
        {
            msocket.joinGroup(InetAddress.getByName(address));
            msocket.setTimeToLive(255);
        }
        catch (UnknownHostException uhe)
        {
            logger.throwing(uhe);
            throw uhe;
        }
        catch (IOException ioe)
        {
            logger.throwing(ioe);
            throw ioe;
        }

        dsocket = msocket;

        logger.finest(dsocket);
        logger.exiting();
    }
}

class TCP extends Protos
{
    private ServerSocket ssocket;

    public TCP(JDNSS dnsService) throws UnknownHostException, IOException
    {
        this.dnsService = dnsService;
        logger.entering(dnsService);

        int port = JDNSS.getJdnssArgs().port;
        int backlog = JDNSS.getJdnssArgs().backlog;
        String ipaddress = JDNSS.getJdnssArgs().IPaddress;

        logger.finest(port);
        logger.finest(backlog);
        logger.finest(ipaddress);

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

        logger.exiting();
    }

    public Thread doit() throws IOException
    {
        logger.entering();

        Socket socket = null;

        try
        {
            socket = ssocket.accept();
        }
        catch (IOException ioe)
        {
            logger.throwing(ioe);
            throw ioe;
        }

        logger.finest("Received TCP packet");

        Thread t = new TCPThread(socket, dnsService);
        logger.exiting(t);
        return t;
    }
}
