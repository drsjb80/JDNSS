package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Assert;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class TCP extends Thread {
    private final Logger logger = JDNSS.logger;
    private String type;
    private int backlog;
    private String address;
    private int port;

    TCP(final String[] parts) {
        type = parts[0];
        backlog = JDNSS.jargs.getBacklog();
        address = parts[1];
        port = Integer.parseInt(parts[2]);
    }

    public void run() {
        logger.traceEntry();

        ServerSocket serverSocket = null;

        switch(type) {
            case "TLS":
                Assertion.aver(JDNSS.jargs.getKeystoreFile() != null);
                Assertion.aver(JDNSS.jargs.getKeystorePassword()!= null);
                System.setProperty("javax.net.ssl.keyStore", JDNSS.jargs.getKeystoreFile());
                System.setProperty("javax.net.ssl.keyStorePassword", JDNSS.jargs.getKeystorePassword());
                if (JDNSS.jargs.isDebugSSL()) {
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
                Assertion.fail("Invalid type: " + type);
                return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(JDNSS.jargs.getThreads());

        while (true) {
            Socket socket = null;

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
                } catch (InterruptedException | ExecutionException e) {
                    logger.catching(e);
                }

                System.exit(0);
            }
        }
    }
}
