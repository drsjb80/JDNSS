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
            NetworkBinding binding = NetworkBinding.fromParts(parts);

            MulticastSocket msocket = createMulticastSocket(binding.getPort());
            configureMulticastSocket(msocket, binding.getHost(), binding.getPort());

            // now UDP and MC are the same, so use the run from UDP
            dsocket = msocket;
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
    }

    MulticastSocket createMulticastSocket(final int port) throws IOException {
        return new MulticastSocket(port);
    }

    void configureMulticastSocket(final MulticastSocket msocket, final String address,
                                  final int port) throws IOException {
        msocket.setTimeToLive(255);

        // Use the modern joinGroup method with NetworkInterface
        InetAddress group = InetAddress.getByName(address);
        InetSocketAddress socketAddress = new InetSocketAddress(group, port);
        NetworkInterface netIf = getNetworkInterface();

        msocket.joinGroup(socketAddress, netIf);
    }
    
    /**
     * Gets an appropriate network interface for multicast.
     * Tries to find a non-loopback, active interface first.
     */
    NetworkInterface getNetworkInterface() throws SocketException {
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
