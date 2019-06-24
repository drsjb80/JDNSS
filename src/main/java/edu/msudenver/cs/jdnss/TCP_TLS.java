package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.InetAddress;
import java.security.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class TCP_TLS extends Thread {

    private final Logger logger = JDNSS.logger;
    int intSSLport = 853;

    SSLServerSocketFactory sslServerSocketfactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
    SSLServerSocket sslServerSocket;

    TCP_TLS(final String[] parts){
        try {
            int backlog = JDNSS.jargs.getBacklog();
            String address = parts[1];
            int port = Integer.parseInt(parts[2]);
            // Not sure if we need this or if we want to force the port here
            sslServerSocket = (SSLServerSocket) sslServerSocketfactory.createServerSocket(intSSLport,
                    backlog, InetAddress.getByName(address));
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
    }
    public void run() {
        logger.traceEntry();

        SSLSocket sslSocket;
        int threadPoolSize = JDNSS.jargs.getThreads();
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);

        while (true) {
            try {
                sslSocket = (SSLSocket) sslServerSocket.accept();
            } catch (IOException ioe) {
                logger.catching(ioe);
                return;
            }

            logger.trace("Received TCP packet");

            Future f = pool.submit(new TCPThread(sslSocket));

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