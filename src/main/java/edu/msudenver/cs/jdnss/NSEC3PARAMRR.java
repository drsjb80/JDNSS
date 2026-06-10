package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
class NSEC3PARAMRR extends RR {
    private final int hashAlgorithm;
    private final int flags;
    private final int iterations;
    private final String salt;

    NSEC3PARAMRR(final String domain, final int ttl, final int hashAlgorithm,
                 final int flags, final int iterations, final String salt) {
        super(domain, RRCode.NSEC3PARAM, ttl);
        this.hashAlgorithm = hashAlgorithm;
        this.flags = flags;
        this.iterations = iterations;
        this.salt = salt;
    }

    @Override
    protected byte[] getBytes() {
        byte[] a = new byte[0];
        a = Utils.combine(a, Utils.getByte(hashAlgorithm, 1));
        a = Utils.combine(a, Utils.getByte(flags, 1));
        a = Utils.combine(a, Utils.getTwoBytes(iterations, 2));
        a = Utils.combine(a, Utils.getByte(salt.length(), 1));
        if (salt != null && !salt.isEmpty()) {
            a = Utils.combine(a, DnsNameCodec.convertString(salt));
        }
        return a;
    }
}
