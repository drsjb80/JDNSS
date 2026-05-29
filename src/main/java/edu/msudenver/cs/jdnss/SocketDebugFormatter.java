package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.StandardSocketOptions;
import java.util.Arrays;

final class SocketDebugFormatter {
    private static final Logger logger = JDNSS.logger;

    private SocketDebugFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    static String toString(final DatagramPacket dgp) {
        StringBuilder s = new StringBuilder();

        s.append("getAddress() = ").append(dgp.getAddress()).append("\n");
        s.append("getLength() = ").append(dgp.getLength()).append("\n");
        s.append("getOffset() = ").append(dgp.getOffset()).append("\n");
        s.append("getPort() = ").append(dgp.getPort()).append("\n");
        s.append("getSocketAddress() = ").append(dgp.getSocketAddress()).append("\n");
        s.append("getData() = ").append(Utils.toString(
            Arrays.copyOfRange(dgp.getData(), dgp.getOffset(),
                dgp.getOffset() + dgp.getLength())));

        return s.toString();
    }

    static String toString(final DatagramSocket dgs) {
        StringBuilder s = new StringBuilder();

        try {
            s.append("getBroadcast() = ").append(dgs.getBroadcast()).append("\n");
            s.append("getInetAddress = ").append(dgs.getInetAddress()).append("\n");
            s.append("getLocalAddress = ").append(dgs.getLocalAddress()).append("\n");
            s.append("getLocalPort() = ").append(dgs.getLocalPort()).append("\n");
            s.append("getLocalSocketAddress() = ").append(dgs.getLocalSocketAddress()).append("\n");
            s.append("getReceiveBufferSize() = ").append(dgs.getReceiveBufferSize()).append("\n");
            s.append("getReuseAddress() = ").append(dgs.getReuseAddress()).append("\n");
            s.append("getSendBufferSize() = ").append(dgs.getSendBufferSize()).append("\n");
            s.append("getSoTimeout() = ").append(dgs.getSoTimeout()).append("\n");
            s.append("getTrafficClass() = ").append(dgs.getTrafficClass()).append("\n");
            s.append("isBound() = ").append(dgs.isBound()).append("\n");
            s.append("isClosed() = ").append(dgs.isClosed()).append("\n");
            s.append("isConnected() = ").append(dgs.isConnected());
        } catch (java.net.SocketException SE) {
            logger.catching(SE);
            return null;
        }

        return s.toString();
    }

    static String toString(final MulticastSocket mcs) {
        String socketDetails = toString((DatagramSocket) mcs);
        if (socketDetails == null) {
            return null;
        }
        StringBuilder s = new StringBuilder(socketDetails).append("\n");

        try {
            s.append("getNetworkInterface() = ")
                    .append(mcs.getOption(StandardSocketOptions.IP_MULTICAST_IF))
                    .append("\n");
            s.append("getTimeToLive() = ").append(mcs.getTimeToLive()).append("\n");
            s.append("getLoopbackMode() = ")
                    .append(mcs.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));
        } catch (java.io.IOException ioe) {
            logger.catching(ioe);
            return null;
        }

        return s.toString();
    }
}