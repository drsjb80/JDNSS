package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static edu.msudenver.cs.jdnss.Assertion.aver;

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
        byte buffer[] = new byte[2];

        aver(is.read(buffer, 0, 2) == 2);

        return Utils.addThem(buffer[0], buffer[1]);
    }

    private Query getQuery() throws IOException{
        byte query[] = new byte[getLength()];
        aver(is.read(query) == query.length);

        Query q = new Query(query);
        q.parseQueries(socket.getInetAddress().toString());
        return q;
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
