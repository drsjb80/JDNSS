package edu.msudenver.cs.jdnss;

import java.net.*;
import java.io.*;
import edu.msudenver.cs.javaln.JavaLN;

/**
 * The threads for getting and responding to TCP requests
 *
 * @author Steve Beaty
 */
public class TCPThread extends Thread
{
    private final Socket socket;
    private final JavaLN logger = JDNSS.logger;
    private final JDNSS dnsService;

    /**
     * @param socket	the socket to talk to
     * @param dnsService the JDNSS service to use for zone info
     */
    public TCPThread (Socket socket, JDNSS dnsService)
    {
        logger.entering(new Object[]{socket, dnsService});

        this.socket = socket;
        this.dnsService = dnsService;
    }

    public void run()
    {
        logger.entering();

        InputStream is = null;
        OutputStream os = null;

        try
        {
            is = socket.getInputStream();
            os = socket.getOutputStream();

            // in TCP, the first two bytes signify the length of the request
            byte buffer[] = new byte[2];

            is.read (buffer, 0, 2);

            byte query[] = new byte [Utils.addThem (buffer[0], buffer[1])];

            is.read (query);

            Query q = new Query (query);
            byte b[] = q.makeResponses (dnsService, false);

            int count = b.length;
            buffer[0] = Utils.getByte (count, 2);
            buffer[1] = Utils.getByte (count, 1);

            os.write (Utils.combine (buffer, b));

            is.close ();
            os.close();
            socket.close ();
        }
        catch (Throwable t)
        {
            logger.throwing (t);
        }
    }
}
