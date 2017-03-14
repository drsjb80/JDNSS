package edu.msudenver.cs.jdnss;

import java.net.*;
import java.io.*;
import edu.msudenver.cs.javaln.JavaLN;

/**
 * The threads for responding to UDP requests
 *
 * @author Steve Beaty
 * @version $Id: UDPThread.java,v 1.18 2010/06/21 21:38:05 drb80 Exp $
 */
public class UDPThread extends Thread
{
    private JavaLN logger = JDNSS.logger;

    /*
    ** is responding through the original socket thread safe, or should we
    ** create another one here?
    */
    private DatagramSocket socket;
    private int port;
    private InetAddress address;
    private Query query;

    private final JDNSS dnsService;

    /**
     * @param socket	the socket to respond through
     * @param packet	the query
     */
    public UDPThread (Query query, DatagramSocket socket, int port,
        InetAddress address, JDNSS dnsService)
    {
        logger.entering (new Object[]{query, socket, address});

        this.query = query;
        this.socket = socket;
        this.port = port;
        this.address = address;
        this.dnsService = dnsService;
    }

    /**
     * make the response
     */
    public void run()
    {
        logger.entering();

        byte b[] = query.makeResponses (dnsService, true);
        logger.finest (Utils.toString (b));

        if (b != null)
        {
            if (socket instanceof MulticastSocket)
            {
                port = 5353;

                try
                {
                    address = InetAddress.getByName ("224.0.0.251");
                }
                catch (UnknownHostException uhe)
                {
                    logger.throwing (uhe);
                    return;
                }
            }

            logger.finest (port);
            logger.finest (address);
            logger.finest (b.length);

            DatagramPacket reply = new DatagramPacket (b, b.length,
                address, port);

            logger.finest ("\n" + Utils.toString (reply.getData()));
            logger.finest (reply.getLength());
            logger.finest (reply.getOffset());
            logger.finest (reply.getAddress());
            logger.finest (reply.getPort());

            try
            {
                socket.send (reply);
            }
            catch (IOException e)
            {
                logger.throwing (e);
            }
        }
    }
}
