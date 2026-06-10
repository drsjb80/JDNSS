package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ToString
@EqualsAndHashCode(callSuper = true)
class DNSKEYRR extends RR {
    @Getter
    private final int flags;
    @Getter
    private final int protocol;
    @Getter
    private final int algorithm;
    @Getter
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
            a = Utils.combine(a, Base64.getDecoder().decode(publicKey.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 encoding in DNSKEY public key", e);
        }
        return a;
    }

    int calculateKeyTag() {
        byte[] rdata = getRdata();
        if (algorithm == 1) {
            // RFC 2537: RSAMD5 uses special calculation (not implemented, deprecated)
            throw new RuntimeException("RSAMD5 (algorithm 1) key tag calculation not supported");
        }

        // RFC 4034: Sum every second octet, weighted if odd length
        int keyTag = 0;
        for (int i = 0; i < rdata.length; ++i) {
            keyTag += (i & 1) == 0 ? (rdata[i] & 0xff) << 8 : (rdata[i] & 0xff);
        }
        keyTag += (keyTag >> 16) & 0xffff;
        return keyTag & 0xffff;
    }

    private byte[] getRdata() {
        byte[] a = new byte[0];
        a = Utils.combine(a, Utils.getTwoBytes(flags, 2));
        a = Utils.combine(a, Utils.getByte(protocol, 1));
        a = Utils.combine(a, Utils.getByte(algorithm, 1));
        try {
            a = Utils.combine(a, Base64.getDecoder().decode(publicKey.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 encoding in DNSKEY public key", e);
        }
        return a;
    }
}
