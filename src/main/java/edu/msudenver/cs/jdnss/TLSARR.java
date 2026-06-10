package edu.msudenver.cs.jdnss;

import java.util.logging.Logger;

final class TLSARR extends RR {
    private static final Logger logger = Logger.getLogger(TLSARR.class.getName());

    private final int usage;
    private final int selector;
    private final int matchingType;
    private final byte[] associationData;

    TLSARR(final String name, final int ttl, final int usage, final int selector,
            final int matchingType, final String associationData) {
        super(name, RRCode.TLSA, ttl);
        this.usage = usage;
        this.selector = selector;
        this.matchingType = matchingType;
        this.associationData = hexStringToBytes(associationData);
    }

    @Override
    public byte[] getBytes() {
        final byte[] bytes = new byte[3 + associationData.length];

        int where = 0;
        bytes[where++] = (byte) usage;
        bytes[where++] = (byte) selector;
        bytes[where++] = (byte) matchingType;

        System.arraycopy(associationData, 0, bytes, where, associationData.length);
        return bytes;
    }

    int getUsage() {
        return usage;
    }

    int getSelector() {
        return selector;
    }

    int getMatchingType() {
        return matchingType;
    }

    String getAssociationData() {
        return bytesToHex(associationData);
    }

    private static byte[] hexStringToBytes(final String hex) {
        final int len = hex.length();
        final byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }

        return data;
    }

    private static String bytesToHex(final byte[] bytes) {
        final StringBuilder hex = new StringBuilder();

        for (final byte b : bytes) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}
