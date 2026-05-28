package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.logging.log4j.Logger;

/*
**                                        1  1  1  1  1  1
**          0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
**        |                                               |
**        /                                               /
**        /                      NAME                     /
**        |                                               |
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
**        |                      TYPE                     |
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
**        |                     CLASS                     |
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
**        |                      TTL                      |
**        |                                               |
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
**        |                   length                    |
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
**        /                     RDATA                     /
**        /                                               /
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
*/

@ToString
@EqualsAndHashCode
abstract class RR {
    final static Logger logger = JDNSS.logger;

    @Getter private final String name;
    @Getter private final RRCode type;
    @Getter final int rrclass = 1;
    @Getter final int ttl;

    boolean isEmpty() { return false; }

    RR(final String name, final RRCode type, final int ttl) {
        this.name = name;
        this.type = type;
        this.ttl = ttl;
    }

    // to enhance polymorphism and decrease casting for derived classes
    String getString() {
        assert false;
        return null;
    }

    String getHost() {
        assert false;
        return null;
    }

    /**
     * converts all the internal data to a byte array
     * @return the resource record to put in the response
     */
    public byte[] getBytes(final String question, final int TTLminimum) {
        final int minttl = ttl == 0 ? TTLminimum : ttl;
        final byte[] name = DnsNameCodec.convertString(question);
        final byte[] rdata = getBytes();
        final int rdatalen = rdata.length;
        final int count = name.length + 2 + 2 + 4 + 2 + rdatalen;
        final byte[] a = new byte[count];
        System.arraycopy(name, 0, a, 0, name.length);

        int where = name.length;

        a[where++] = Utils.getByte(type.getCode(), 2);
        a[where++] = Utils.getByte(type.getCode(), 1);
        a[where++] = Utils.getByte(rrclass, 2);
        a[where++] = Utils.getByte(rrclass, 1);
        a[where++] = Utils.getByte(minttl, 4);
        a[where++] = Utils.getByte(minttl, 3);
        a[where++] = Utils.getByte(minttl, 2);
        a[where++] = Utils.getByte(minttl, 1);
        a[where++] = Utils.getByte(rdatalen, 2);
        a[where++] = Utils.getByte(rdatalen, 1);
        System.arraycopy(rdata, 0, a, where, rdata.length);

        return a;
    }

    protected abstract byte[] getBytes();
}

class EmptyRR extends RR {
    EmptyRR() {
        super(null, null, -1);
    }

    @Override
    boolean isEmpty() {
        return true;
    }

    @Override
    protected byte[] getBytes() {
        assert false;
        return new byte[0];
    }
}
/**
 * Just a simple class for queries.
 */
class QRR extends RR {
    QRR(final String name, final RRCode type) {
        super(name, type, 0);
    }

    @Override
    protected byte[] getBytes() {
        return new byte[0];
    }
}

@ToString
@EqualsAndHashCode(callSuper = true)
abstract class STRINGRR extends RR {
    @Getter
    String string;

    STRINGRR(final String name, final RRCode type, int ttl) {
        super(name, type, ttl);
    }

    @Override
    protected byte[] getBytes() {
        return DnsNameCodec.convertString(string);
    }
}

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
abstract class ADDRRR extends RR {
    protected String address;

    ADDRRR(final String name, final RRCode type, final int ttl) {
        super(name, type, ttl);
    }
}

