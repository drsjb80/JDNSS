package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

public class UDPThread implements Runnable
{
    private Logger logger = JDNSS.getLogger();

    private DatagramSocket socket;
    private int port;
    private InetAddress address;
    private Query query;
    private JDNSS dnsService;

    /**
     * @param socket	the socket to respond through
     * @param packet	the query
     */
    public UDPThread(Query query, DatagramSocket socket, int port,
        InetAddress address, JDNSS dnsService)
    {
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
        logger.traceEntry();
        logger.fatal("In run");

        byte b[] = query.makeResponses(dnsService, true);
        logger.trace(Utils.toString(b));
        logger.fatal(Utils.toString(b));

        if (b != null)
        {
            if (socket instanceof MulticastSocket)
            {
                port = 5353;

                try
                {
                    address = InetAddress.getByName("224.0.0.251");
                }
                catch (UnknownHostException uhe)
                {
                    logger.catching(uhe);
                    return;
                }
            }

            logger.trace(port);
            logger.trace(address);
            logger.trace(b.length);

            DatagramPacket reply = new DatagramPacket(b, b.length,
                address, port);

            logger.trace("\n" + Utils.toString(reply.getData()));
            logger.trace(reply.getLength());
            logger.trace(reply.getOffset());
            logger.trace(reply.getAddress());
            logger.trace(reply.getPort());

            try
            {
                socket.send(reply);
            }
            catch (IOException e)
            {
                logger.catching(e);
            }
        }
    }
}
