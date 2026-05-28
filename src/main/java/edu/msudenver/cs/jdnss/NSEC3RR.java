package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@ToString
@EqualsAndHashCode(callSuper = true)
class NSEC3RR extends RR {
    private final int hashAlgorithm;
    private final int flags;
    private final int iterations;
    private final String salt;
    private final String nextHashedOwnerName;
    private final Set<RRCode> types;

    NSEC3RR(final String domain, final int ttl, final int hashAlgorithm,
            final int flags, final int iterations, final String salt,
            final String nextHashedOwnerName, final Set<RRCode> types) {
        super(domain, RRCode.NSEC3, ttl);
        this.hashAlgorithm = hashAlgorithm;
        this.flags = flags;
        this.iterations = iterations;
        this.salt = salt;
        this.nextHashedOwnerName = nextHashedOwnerName;
        this.types = types;
    }

    @Override
    protected byte[] getBytes() {
        byte[] a = new byte[0];
        a = Utils.combine(a, Utils.getByte(hashAlgorithm, 1));
        a = Utils.combine(a, Utils.getByte(flags, 2));
        a = Utils.combine(a, Utils.getTwoBytes(iterations, 1));
        a = Utils.combine(a, Utils.getByte(salt.length(), 1));
        a = Utils.combine(a, Utils.convertString(salt));
        a = Utils.combine(a, Utils.getByte(this.nextHashedOwnerName.length(), 1));
        a = Utils.combine(a, Utils.convertString(nextHashedOwnerName));

        assert false;
        return a;
    }
}
