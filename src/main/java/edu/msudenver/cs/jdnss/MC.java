package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.lang.AssertionError;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

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

        logger.fatal(port);
        logger.fatal(address);

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
            msocket.setNetworkInterface(NetworkInterface.getByName("en0"));
            msocket.setTimeToLive(255);
            msocket.joinGroup(InetAddress.getByName(address));
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

        // now UDP and MC are the same, so use the run from UDP
        dsocket = msocket;

        logger.traceExit();
    }
}
