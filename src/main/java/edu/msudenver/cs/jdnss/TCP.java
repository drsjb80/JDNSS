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
        NetworkBinding binding = NetworkBinding.fromParts(parts);
        address = binding.getHost();
        port = binding.getPort();
    }

    ServerSocket createServerSocket() throws IOException {
        switch(type) {
            case "TLS":
                assert JDNSS.jargs.keystoreFile != null;
                assert JDNSS.jargs.keystorePassword!= null;
                System.setProperty("javax.net.ssl.keyStore", JDNSS.jargs.keystoreFile);
                System.setProperty("javax.net.ssl.keyStorePassword", JDNSS.jargs.keystorePassword);
                if (JDNSS.jargs.debugSSL) {
                    System.setProperty("javax.net.debug", "all");
                }

                SSLServerSocketFactory sslServerSocketfactory =
                        (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
                return sslServerSocketfactory.createServerSocket(port, backlog, InetAddress.getByName(address));
            case "TCP":
                return new ServerSocket(port, backlog, InetAddress.getByName(address));
            default:
                throw new IOException("Unsupported TCP type: " + type);
        }
    }

    ExecutorService createThreadPool() {
        return Executors.newFixedThreadPool(JDNSS.jargs.getThreads());
    }

    Future<?> submitSocketTask(final ExecutorService pool, final Socket socket) {
        return pool.submit(new TCPThread(socket));
    }

    void waitForCompletion(final Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.catching(e);
        }
    }

    void exitProcess(final int statusCode) {
        System.exit(statusCode);
    }

    public void run() {
        logger.traceEntry();

        ServerSocket serverSocket;

        try {
            serverSocket = createServerSocket();
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        ExecutorService pool = createThreadPool();

        while (true) {
            Socket socket;

            try {
                socket = serverSocket.accept();
            } catch (IOException ioe) {
                logger.catching(ioe);
                return;
            }

            logger.trace("Received TCP packet");

            Future<?> f = submitSocketTask(pool, socket);

            // if we're only supposed to answer once, and we're the first,
            // bring everything down with us.
            if (JDNSS.jargs.isOnce()) {
                waitForCompletion(f);
                exitProcess(0);
            }
        }
    }
}
