package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Base64;

@ToString
@EqualsAndHashCode(callSuper = true)
class DNSKEYRR extends RR {
    private final int flags;
    private final int protocol;
    private final int algorithm;
    private final String publicKey;

    DNSKEYRR(final String domain, final int ttl, final int flags,
             final int protocol, final int algorithm, final String publicKey) {
        super(domain, RRCode.DNSKEY, ttl);

        this.flags = Integer.parseUnsignedInt(String.valueOf(flags));
        this.protocol = Integer.parseUnsignedInt(String.valueOf(protocol));
        this.algorithm = Integer.parseUnsignedInt(String.valueOf(algorithm));
        this.publicKey = publicKey;
    }

    @Override
    protected byte[] getBytes() {
        byte[] a = new byte[0];
        a = Utils.combine(a, Utils.getTwoBytes(flags, 2));
        a = Utils.combine(a, Utils.getByte(protocol, 1));
        a = Utils.combine(a, Utils.getByte(algorithm, 1));
        try {
            a = Utils.combine(a, Base64.getDecoder().decode(publicKey.getBytes("UTF8")));
        } catch (Exception e) {
            assert false;
        }
        return a;
    }
}
