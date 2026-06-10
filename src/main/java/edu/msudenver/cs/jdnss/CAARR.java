package edu.msudenver.cs.jdnss;

import java.nio.charset.StandardCharsets;

final class CAARR extends RR {
    private final int flags;
    private final String tag;
    private final String value;

    CAARR(final String name, final int ttl, final int flags, final String tag,
            final String value) {
        super(name, RRCode.CAA, ttl);
        this.flags = flags;
        this.tag = tag;
        this.value = value;
    }

    @Override
    public byte[] getBytes() {
        final byte[] tagBytes = Utils.toCS(tag);
        final byte[] valueBytes = value.getBytes(StandardCharsets.US_ASCII);
        final byte[] bytes = new byte[1 + tagBytes.length + valueBytes.length];

        int where = 0;
        bytes[where++] = (byte) flags;

        System.arraycopy(tagBytes, 0, bytes, where, tagBytes.length);
        where += tagBytes.length;

        System.arraycopy(valueBytes, 0, bytes, where, valueBytes.length);
        return bytes;
    }

    int getFlags() {
        return flags;
    }

    String getTag() {
        return tag;
    }

    String getValue() {
        return value;
    }
}
