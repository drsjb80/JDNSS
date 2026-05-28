package edu.msudenver.cs.jdnss;

class TXTRR extends STRINGRR {
    TXTRR(final String name, final int ttl, final String text) {
        super(name, RRCode.TXT, ttl);
        this.string = text;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.toCS(string);
    }
}
