package edu.msudenver.cs.jdnss;

class ARR extends ADDRRR {
    ARR(final String name, final int ttl, final String address) {
        super(name, RRCode.A, ttl);
        this.address = address;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.IPV4(address);
    }
}
