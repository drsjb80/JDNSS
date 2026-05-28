package edu.msudenver.cs.jdnss;

class PTRRR extends STRINGRR {
    PTRRR(final String address, final int ttl, final String host) {
        super(address, RRCode.PTR, ttl);
        this.string = host;
    }
}
