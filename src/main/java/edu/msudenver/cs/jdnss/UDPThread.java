package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.logging.log4j.Logger;

public class UDPThread implements Runnable
{
    private final Logger logger = JDNSS.logger;

    private final DatagramSocket socket;
    private final int port;
    private final InetAddress address;
    private final byte[] packet;

    /**
     * @param socket	the socket to respond through
     * @param packet	the query
     */
    public UDPThread(byte[] packet, DatagramSocket socket, int port,
        InetAddress address)
    {
        this.packet = packet;
        this.socket = socket;
        this.port = port;
        this.address = address;

        /*
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
        */
    }

    /**
     * make the response
     */
    public void run()
    {
        logger.traceEntry();

        Query query = new Query (packet);
        query.parseQueries(address.toString());

        Response r = new Response(query, true);
        byte b[] = r.getBytes();

        DatagramPacket reply = new DatagramPacket(b, b.length, address, port);

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
