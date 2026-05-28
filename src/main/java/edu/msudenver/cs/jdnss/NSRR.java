package edu.msudenver.cs.jdnss;

class NSRR extends STRINGRR {
    NSRR(final String domain, final int ttl, final String nameserver) {
        super(domain, RRCode.NS, ttl);
        this.string = nameserver;
    }

    @Override
    public String getString() {
        return string;
    }
}
