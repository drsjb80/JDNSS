package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class TCPThread implements Runnable {
    private final Socket socket;
    private final Logger logger = JDNSS.logger;
    private InputStream is;
    private OutputStream os;
    /**
     * @param socket the socket to talk to
     */
    TCPThread(Socket socket) { this.socket = socket; }

    private void openStreams() throws IOException {
        is = socket.getInputStream();
        os = socket.getOutputStream();
    }

    private void closeStreams() throws IOException {
        is.close();
        os.close();
        socket.close();
    }

    private int getLength() throws IOException{
        // in TCP, the first two bytes signify the length of the request
        byte buffer[] = readFully(2);

        return Utils.addThem(buffer[0], buffer[1]);
    }

    private Query getQuery() throws IOException{
        byte query[] = readFully(getLength());

        Query q = new Query(query);
        q.parseQueries(socket.getInetAddress().toString());
        return q;
    }

    private byte[] readFully(final int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = is.read(buffer, offset, length - offset);
            if (count == -1) {
                throw new EOFException("Expected " + length + " bytes, read " + offset);
            }
            offset += count;
        }
        return buffer;
    }

    private void sendResponse (Query q) throws IOException{
        Response r = new Response(q, false);
        byte b[] = r.getBytes();

        byte buffer[] = new byte[2];
        int count = b.length;
        buffer[0] = Utils.getByte(count, 2);
        buffer[1] = Utils.getByte(count, 1);

        os.write(Utils.combine(buffer, b));
    }

    public void run() {
        logger.traceEntry();

        try {
            openStreams();
            sendResponse(getQuery());
            closeStreams();
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
    }
}
