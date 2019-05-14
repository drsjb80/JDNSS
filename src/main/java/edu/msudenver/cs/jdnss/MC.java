package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

class MC extends UDP {
    public MC(String[] parts) {


        try {
            String address = parts[1];
            int port = Integer.parseInt(parts[2]);

            MulticastSocket msocket = new MulticastSocket(port);
            msocket.setTimeToLive(255);
            msocket.joinGroup(InetAddress.getByName(address));

            // now UDP and MC are the same, so use the run from UDP
            dsocket = msocket;
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
    }
}
