package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
class SOARR extends RR {
    private final String domain;
    private final String server;
    private final String contact;
    private final int serial;
    private final int refresh;
    private final int retry;
    private final int expire;
    private final int minimum;

    SOARR(final String domain, final String server, final String contact,
          final int serial, final int refresh, final int retry, final int expire,
          final int minimum, int ttl) {
        super(domain, RRCode.SOA, ttl);

        this.domain = domain;
        this.server = server;
        this.contact = contact;
        this.serial = serial;
        this.refresh = refresh;
        this.retry = retry;
        this.expire = expire;
        this.minimum = minimum;
    }

    public int getMinimum() {
        return minimum;
    }

    @Override
    protected byte[] getBytes() {
        byte[] a = Utils.convertString(server);
        a = Utils.combine(a, Utils.convertString(contact));
        a = Utils.combine(a, Utils.getBytes(serial));
        a = Utils.combine(a, Utils.getBytes(refresh));
        a = Utils.combine(a, Utils.getBytes(retry));
        a = Utils.combine(a, Utils.getBytes(expire));
        a = Utils.combine(a, Utils.getBytes(minimum));
        return a;
    }
}
