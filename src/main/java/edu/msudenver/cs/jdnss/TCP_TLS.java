package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class TCP_TLS extends Thread {

    private final Logger logger = JDNSS.logger;
    int intSSLport = 853;

    TCP_TLS(final String[] parts){
        int backlog = JDNSS.jargs.getBacklog();
        String address = parts[1];
        int port = Integer.parseInt(parts[2]);
    }

    public void run() {
        logger.traceEntry();

        System.setProperty("javax.net.ssl.keyStore", "/tmp/testkeystore.ks");
        System.setProperty("javax.net.ssl.keyStorePassword", "testpwd");
        // System.setProperty("javax.net.debug", "all");

        SSLServerSocket sslServerSocket;
        try {
            SSLServerSocketFactory sslServerSocketfactory =
                (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
            sslServerSocket =
                (SSLServerSocket) sslServerSocketfactory.createServerSocket(853);
        } catch (IOException ioe) {
            logger.catching(ioe);
            return;
        }

        SSLSocket sslSocket;
        int threadPoolSize = JDNSS.jargs.getThreads();
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);

        while (true) {
            try {
                // sslServerSocket.setEnabledProtocols(new String[] {"TLSv1.2"});
                sslSocket = (SSLSocket) sslServerSocket.accept();
                sslSocket.setNeedClientAuth(false);
            } catch (IOException ioe) {
                logger.catching(ioe);
                return;
            }

            logger.trace("Received TCP packet");

            Future f = pool.submit(new TCPTLSThread(sslSocket));

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
