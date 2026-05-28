package edu.msudenver.cs.jdnss;

class AAAARR extends ADDRRR {
    AAAARR(final String name, final int ttl, final String address) {
        super(name, RRCode.AAAA, ttl);
        this.address = address;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.IPV6(address);
    }
}
