package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.lang.AssertionError;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

class TCP extends Thread
{
    private ServerSocket ssocket;
    private JDNSS dnsService;
    protected Logger logger = JDNSS.getLogger();

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

    public void run()
    {
        logger.traceEntry();

        Socket socket = null;

        while (true)
        {
            try
            {
                socket = ssocket.accept();
            }
            catch (IOException ioe)
            {
                logger.catching(ioe);
                return;
            }

            logger.trace("Received TCP packet");

            Thread t = new TCPThread(socket, dnsService);
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
