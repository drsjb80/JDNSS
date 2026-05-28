package edu.msudenver.cs.jdnss;

class CNAMERR extends STRINGRR {
    CNAMERR(final String alias, final int ttl, final String canonical) {
        super(alias, RRCode.CNAME, ttl);
        this.string = canonical;
    }
}
