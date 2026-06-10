package edu.msudenver.cs.jdnss;

import java.nio.charset.StandardCharsets;

final class SRVRR extends RR {
    private final int priority;
    private final int weight;
    private final int port;
    private final String target;

    SRVRR(final String name, final int ttl, final int priority, final int weight,
            final int port, final String target) {
        super(name, RRCode.SRV, ttl);
        this.priority = priority;
        this.weight = weight;
        this.port = port;
        this.target = target;
    }

    @Override
    public byte[] getBytes() {
        final byte[] targetBytes = DnsNameCodec.convertString(target);
        final byte[] bytes = new byte[6 + targetBytes.length];

        int where = 0;
        bytes[where++] = Utils.getByte(priority, 2);
        bytes[where++] = Utils.getByte(priority, 1);
        bytes[where++] = Utils.getByte(weight, 2);
        bytes[where++] = Utils.getByte(weight, 1);
        bytes[where++] = Utils.getByte(port, 2);
        bytes[where++] = Utils.getByte(port, 1);

        System.arraycopy(targetBytes, 0, bytes, where, targetBytes.length);
        return bytes;
    }

    int getPriority() {
        return priority;
    }

    int getWeight() {
        return weight;
    }

    int getPort() {
        return port;
    }

    String getTarget() {
        return target;
    }
}
