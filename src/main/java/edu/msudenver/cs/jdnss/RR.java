package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.logging.log4j.Logger;

import java.util.Base64;
import java.util.Set;

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
    @Getter private final int rrclass = 1;
    @Getter private final int ttl;

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
        final byte[] name = Utils.convertString(question);
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

    /*
    ** 1035:
    ** "... SOA records are always distributed with a zero
    ** TTL to prohibit caching."
    **
    ** 2182:
    ** It may be observed that in section 3.2.1 of RFC1035, which defines
    ** the format of a Resource Record, that the definition of the TTL field
    ** contains a throw away line which states that the TTL of an SOA record
    ** should always be sent as zero to prevent caching.  This is mentioned
    ** nowhere else, and has not generally been implemented.
    ** Implementations should not assume that SOA records will have a TTL of
    ** zero, nor are they required to send SOA records with a TTL of zero.
    **
    ** this however does not say what SHOULD be sent as the TTL...
    */

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

@ToString
@EqualsAndHashCode(callSuper = true)
class HINFORR extends RR {
    private final String CPU;
    private final String OS;

    HINFORR(final String name, final int ttl, final String CPU, final String OS) {
        super(name, RRCode.HINFO, ttl);
        this.CPU = CPU;
        this.OS = OS;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.combine(Utils.toCS(CPU), Utils.toCS(OS));
    }
}

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

        return Utils.combine(c, Utils.convertString(host));
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
        return Utils.convertString(string);
    }
}

class TXTRR extends STRINGRR {
    TXTRR(final String name, final int ttl, final String text) {
        super(name, RRCode.TXT, ttl);
        this.string = text;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.toCS(string);
    }
}

class NSRR extends STRINGRR {
    NSRR(final String domain, final int ttl, final String nameserver) {
        super(domain, RRCode.NS, ttl);
        this.string = nameserver;
    }

    @Override
    public String getString() {
        return string;
    }
}

class CNAMERR extends STRINGRR {
    CNAMERR(final String alias, final int ttl, final String canonical) {
        super(alias, RRCode.CNAME, ttl);
        this.string = canonical;
    }
}

class PTRRR extends STRINGRR {
    PTRRR(final String address, final int ttl, final String host) {
        super(address, RRCode.PTR, ttl);
        this.string = host;
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
            a = Utils.combine(a, Base64.getDecoder().decode(publicKey.getBytes()));
        } catch (Exception e ){
            assert false;
        }
        return a;
    }
}

// https://www.iana.org/assignments/dns-sec-alg-numbers/dns-sec-alg-numbers.xhtml
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
        a = Utils.combine(a,
                Utils.getByte(this.nextHashedOwnerName.length(), 1));
        a = Utils.combine(a,
                Utils.convertString(nextHashedOwnerName));

        assert false;
        return a;
    }
}

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
        assert false;
        return null;
    }
}

@ToString
@EqualsAndHashCode(callSuper = true)
class RRSIG extends RR {
    @Getter
    private final RRCode typeCovered;
    private final int algorithm;
    private final int labels;
    private final int originalttl;
    private final int signatureExpiration; //32 bit unsigned
    private final int signatureInception; // 32 bit unsigned
    private final int keyTag;
    private final String signersName;
    private final String signature;

    RRSIG(final String domain, final int ttl, final RRCode typeCovered,
               final int algorithm, final int labels, final int originalttl,
               final int expiration, final int inception, final int keyTag,
               final String signersName, final String signature) {
        super(domain, RRCode.RRSIG, ttl);

        this.typeCovered = typeCovered;
        this.algorithm = algorithm;
        this.labels = labels;
        this.originalttl = originalttl;
        this.signatureExpiration = expiration;
        this.signatureInception = inception;
        this.keyTag = keyTag;
        this.signersName = signersName;
        this.signature = signature;
    }

    @Override
    protected byte[] getBytes() {
        byte[] a = new byte[0];
        a = Utils.combine(a, Utils.getTwoBytes(typeCovered.getCode(), 2));
        a = Utils.combine(a, Utils.getByte(algorithm, 1));
        a = Utils.combine(a, Utils.getByte(labels, 1));
        a = Utils.combine(a, Utils.getBytes(originalttl));
        a = Utils.combine(a, Utils.getTwoBytes(signatureExpiration, 4));
        a = Utils.combine(a, Utils.getTwoBytes(signatureExpiration, 2));
        a = Utils.combine(a, Utils.getTwoBytes(signatureInception, 4));
        a = Utils.combine(a, Utils.getTwoBytes(signatureInception, 2));
        a = Utils.combine(a, Utils.getTwoBytes(keyTag, 2));
        a = Utils.combine(a, Utils.convertString(signersName));
        try {
            a = Utils.combine(a, Base64.getDecoder().decode(signature.getBytes()));
        } catch (Exception e ){
            assert false;
        }
        return a;
    }
}

@ToString
@EqualsAndHashCode(callSuper = true)
class NSECRR extends RR {
    private final String nextDomainName;
    private final Set<RRCode> resourceRecords; //map more appopriate <RRCode, RR> ??

    NSECRR(final String domain, final int ttl, final String nextDomainName,
              final Set<RRCode> resourceRecords) {
        super(domain, RRCode.NSEC, ttl);

        this.nextDomainName = nextDomainName;
        this.resourceRecords = resourceRecords;
    }

    @Override
    protected byte[] getBytes() {
        byte[] a= new byte[0];
        a = Utils.combine(a, Utils.convertString(nextDomainName));
        a = Utils.combine(a, buildBitMap());
        return a;
    }

    private byte[] buildBitMap() {
        int largestRcode = 0;
        for(RRCode rr: resourceRecords) {
            if(rr.getCode() > largestRcode) {
                largestRcode = rr.getCode();
            }
        }
        int length = (largestRcode + 8)/8;
        byte[] bitMap = new byte[length];
        byte[] a = {0x00};
        a = Utils.combine(a ,(byte) length);
        a = Utils.combine(a, setBits(bitMap));
        return a;
    }

    private byte[] setBits(byte[] bitMap) {
        for (RRCode rr : resourceRecords) {
            switch (rr) {
                case A:
                    bitMap[0] = (byte) (bitMap[0] + 64);
                    break;
                case NS:
                    bitMap[0] = (byte) (bitMap[0] + 32);
                    break;
                case CNAME:
                    bitMap[0] = (byte) (bitMap[0] + 4);
                    break;
                case SOA:
                    bitMap[0] = (byte) (bitMap[0] + 2);
                    break;
                case PTR:
                    bitMap[1] = (byte) (bitMap[1] + 8);
                    break;
                case HINFO:
                    bitMap[1] = (byte) (bitMap[1] + 4);
                    break;
                case MX:
                    bitMap[1] = (byte) (bitMap[1] + 1);
                    break;
                case TXT:
                    bitMap[2] = (byte) (bitMap[2] + 128);
                    break;
                case AAAA:
                    bitMap[3] = (byte) (bitMap[3] + 8);
                    break;
                case A6:
                    bitMap[4] = (byte) (bitMap[4] + 2);
                    break;
                case DNAME:
                    bitMap[4] = (byte) (bitMap[4] + 1);
                    break;
                case DS:
                    bitMap[5] = (byte) (bitMap[5] + 16);
                    break;
                case RRSIG:
                    bitMap[5] = (byte) (bitMap[5] + 2);
                    break;
                case NSEC:
                    bitMap[5] = (byte) (bitMap[5] + 1);
                    break;
                case DNSKEY:
                    bitMap[6] = (byte) (bitMap[6] + 128);
                    break;
                case NSEC3:
                    bitMap[6] = (byte) (bitMap[6] + 32);
                    break;
                case NSEC3PARAM:
                    bitMap[6] = (byte) (bitMap[6] + 16);
                    break;
                default:
                    logger.error("Couldn't add/find "+ rr + " to NSEC bit map");
                    break;
            }
        }
        return bitMap;
    }
}

