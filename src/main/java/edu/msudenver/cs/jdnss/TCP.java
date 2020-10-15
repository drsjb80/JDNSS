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
    private final Logger logger = JDNSS.logger;
    private final String type;
    private final int backlog;
    private final String address;
    private final int port;

    TCP(final String[] parts) {
        type = parts[0];
        backlog = JDNSS.jargs.backlog;
        address = parts[1];
        port = Integer.parseInt(parts[2]);
    }

    public void run() {
        logger.traceEntry();

        ServerSocket serverSocket;

        switch(type) {
            case "TLS":
                assert JDNSS.jargs.keystoreFile != null;
                assert JDNSS.jargs.keystorePassword!= null;
                System.setProperty("javax.net.ssl.keyStore", JDNSS.jargs.keystoreFile);
                System.setProperty("javax.net.ssl.keyStorePassword", JDNSS.jargs.keystorePassword);
                if (JDNSS.jargs.debugSSL) {
                    System.setProperty("javax.net.debug", "all");
                }

                try {
                    SSLServerSocketFactory sslServerSocketfactory =
                            (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
                    serverSocket = sslServerSocketfactory.createServerSocket(port, backlog, InetAddress.getByName(address));
                } catch (IOException ioe) {
                    logger.catching(ioe);
                    return;
                }
                break;
            case "TCP":
                try {
                    serverSocket = new ServerSocket(port, backlog, InetAddress.getByName(address));
                } catch (IOException E) {
                    logger.throwing(E);
                    return;
                }
                break;
            default:
                assert false;
                return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(JDNSS.jargs.getThreads());

        while (true) {
            Socket socket;

            try {
                socket = serverSocket.accept();
            } catch (IOException ioe) {
                logger.catching(ioe);
                return;
            }

            logger.trace("Received TCP packet");

            Future<?> f = pool.submit(new TCPThread(socket));

            // if we're only supposed to answer once, and we're the first,
            // bring everything down with us.
            if (JDNSS.jargs.isOnce()) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.catching(e);
                }

                System.exit(0);
            }
        }
    }
}
