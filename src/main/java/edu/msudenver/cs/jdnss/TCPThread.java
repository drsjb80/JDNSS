package edu.msudenver.cs.jdnss;

import java.net.*;
import java.io.*;

import org.apache.logging.log4j.Logger;

class TCPThread implements Runnable {
    private final Socket socket;
    private final Logger logger = JDNSS.logger;

    /**
     * @param socket the socket to talk to
     */
    public TCPThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        logger.traceEntry();

        InputStream is;
        OutputStream os;

        try {
            is = socket.getInputStream();
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        try {
            os = socket.getOutputStream();
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        // in TCP, the first two bytes signify the length of the request
        byte buffer[] = new byte[2];

        try {
            Assertion.aver(is.read(buffer, 0, 2) == 2);
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        byte query[] = new byte[Utils.addThem(buffer[0], buffer[1])];

        try {
            Assertion.aver(is.read(query) == query.length);
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        Query q = new Query(query);
        q.parseQueries(socket.getInetAddress().toString());

        Response r = new Response(q, false);
        byte b[] = r.getBytes();

        int count = b.length;
        buffer[0] = Utils.getByte(count, 2);
        buffer[1] = Utils.getByte(count, 1);

        try {
            os.write(Utils.combine(buffer, b));
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        try {
            is.close();
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        try {
            os.close();
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        try {
            socket.close();
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }
        // }
        /*
        catch (Throwable t)
        {
            logger.catching(t);
        }
        */
    }
}
