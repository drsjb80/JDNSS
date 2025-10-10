package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

class MC extends UDP {
    public MC(String[] parts) {
        try {
            String address = parts[1];
            int port = Integer.parseInt(parts[2]);

            MulticastSocket msocket = new MulticastSocket(port);
            msocket.setTimeToLive(255);
            
            // Use the modern joinGroup method with NetworkInterface
            InetAddress group = InetAddress.getByName(address);
            InetSocketAddress socketAddress = new InetSocketAddress(group, port);
            NetworkInterface netIf = getNetworkInterface();
            
            msocket.joinGroup(socketAddress, netIf);

            // now UDP and MC are the same, so use the run from UDP
            dsocket = msocket;
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
    }
    
    /**
     * Gets an appropriate network interface for multicast.
     * Tries to find a non-loopback, active interface first.
     */
    private NetworkInterface getNetworkInterface() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        
        // First pass: try to find a non-loopback, up interface
        while (interfaces.hasMoreElements()) {
            NetworkInterface netIf = interfaces.nextElement();
            if (netIf.isUp() && !netIf.isLoopback() && netIf.supportsMulticast()) {
                return netIf;
            }
        }
        
        // Second pass: accept any interface that supports multicast
        interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface netIf = interfaces.nextElement();
            if (netIf.supportsMulticast()) {
                return netIf;
            }
        }
        
        // Fallback: return null (will use system default)
        return null;
    }
}
