package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLServerSocketFactory;
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
    private final int defaultDNSPort = 53;
    private final int defaultDNSOverTLSPort = 853;

    public TCP() throws IOException {
        try {
            String ipAddress = JDNSS.jargs.getIPaddress();
            int backlog = JDNSS.jargs.getBacklog();
            int port = JDNSS.jargs.getPort();
            if (ipAddress != null && backlog != 0 && port != 0) {
                if (port == defaultDNSOverTLSPort) {
                    SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                    serverSocket = ssf.createServerSocket(port, backlog, InetAddress.getByName(ipAddress));
                } else {
                    serverSocket = new ServerSocket(port, backlog, InetAddress.getByName(ipAddress));
                }
            } else if (backlog != 0 && port != 0) {
                if (port == defaultDNSOverTLSPort) {
                    SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                    serverSocket = ssf.createServerSocket(port, backlog);
                } else {
                    serverSocket = new ServerSocket(port, backlog);
                }
            } else if (port != 0) {
                if (port == defaultDNSOverTLSPort) {
                    SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                    serverSocket = ssf.createServerSocket(port);
                } else {
                    serverSocket = new ServerSocket(port);
                }
            } else {
                serverSocket = new ServerSocket(defaultDNSPort);
            }
        } catch (IOException ioe) {
            logger.catching(ioe);
            throw ioe;
        }

        logger.traceExit();
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
