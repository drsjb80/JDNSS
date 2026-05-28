package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
class MXRR extends RR {
    @Getter
    private final String host;
    @Getter
    private final int preference;

    MXRR(final String name, final int ttl, final String host, final int preference) {
        super(name, RRCode.MX, ttl);
        this.host = host;
        this.preference = preference;
    }

    @Override
    protected byte[] getBytes() {
        byte[] c = new byte[2];
        c[0] = Utils.getByte(preference, 2);
        c[1] = Utils.getByte(preference, 1);

        return Utils.combine(c, DnsNameCodec.convertString(host));
    }
}
