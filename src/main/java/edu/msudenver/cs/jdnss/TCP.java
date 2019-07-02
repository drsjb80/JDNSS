package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class TCP extends Thread {

    private ServerSocket serverSocket;
    private final Logger logger = JDNSS.logger;

    TCP(final String[] parts) {
        try {
            int backlog = JDNSS.jargs.getBacklog();
            String address = parts[1];
            int port = Integer.parseInt(parts[2]);

            serverSocket = new ServerSocket(port, backlog,
                    InetAddress.getByName(address));
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
    }

    public void run() {
        logger.traceEntry();

        Socket socket;
        int threadPoolSize = JDNSS.jargs.getThreads();
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);

        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException ioe) {
                logger.catching(ioe);
                return;
            }

            logger.trace("Received TCP packet");

            Future f = pool.submit(new TCPThread(socket));

            // if we're only supposed to answer once, and we're the first,
            // bring everything down with us.
            if (JDNSS.jargs.isOnce()) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException ie) {
                    logger.catching(ie);
                }

                System.exit(0);
            }
        }
    }
}
