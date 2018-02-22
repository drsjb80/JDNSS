package edu.msudenver.cs.jdnss;
/**
 * Create and manipulate resource records
 *
 * @author Steve Beaty
 * @see <a href="http://www.faqs.org/rfcs/rfc1035.html"> faqs.org </a>
 */

import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

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

public abstract class RR {
    final static Logger logger = JDNSS.logger;
    @Getter private String name;
    @Getter private int type;
    @Getter private int rrclass = 1;
    @Getter private int ttl;
    @Getter private int length;
    @Getter private int byteSize;

    RR(String name, int type, int ttl) {
        this.name = name;
        this.type = type;
        this.ttl = ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RR)) {
            return false;
        }

        RR rr = (RR) o;
        return rr.name.equals(this.name) &&
                rr.rrclass == rrclass &&
                rr.type == type &&
                rr.ttl == ttl &&
                rr.length == length &&
                rr.byteSize == byteSize;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + type + rrclass + ttl + length +
                byteSize;
    }

    // to enhance polymorphism and decrease casting for derived classes
    String getString() {
        Assertion.fail();
        return null;
    }

    String getHost() {
        Assertion.fail();
        return null;
    }

    /**
     * converts all the internal data to a byte array
     * @return the resource record to put in the response
     */
    public byte[] getBytes(String question, int TTLminimum) {
        int minttl = ttl == 0 ? TTLminimum : ttl;

        byte name[] = Utils.convertString(question);

        byte rdata[] = getBytes();
        int rdatalen = rdata.length;

        int count = name.length + 2 + 2 + 4 + 2 + rdatalen;

        byte a[] = new byte[count];
        System.arraycopy(name, 0, a, 0, name.length);

        int where = name.length;

        a[where++] = Utils.getByte(type, 2);
        a[where++] = Utils.getByte(type, 1);
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

    public String toString() {
        return ("name = " + name + ", type = " +
                Utils.mapTypeToString(type) + ", TTL = " + ttl);
    }
}

/**
 * Just a simple class for queries.
 */
class QRR extends RR {
    public QRR(String name, int type) {
        super(name, type, 0);
    }

    @Override
    protected byte[] getBytes() {
        return new byte[0];
    }
}

class SOARR extends RR {
    private final String domain;
    private final String server;
    private final String contact;
    private final int serial;
    private final int refresh;
    private final int retry;
    private final int expire;
    private final int minimum;

    SOARR(String domain, String server, String contact, int serial,
          int refresh, int retry, int expire, int minimum, int ttl) {
        super(domain, Utils.SOA, ttl);

        this.domain = domain;
        this.server = server;
        this.contact = contact;
        this.serial = serial;
        this.refresh = refresh;
        this.retry = retry;
        this.expire = expire;
        this.minimum = minimum;
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (!(o instanceof SOARR)) {
                return false;
            }

            SOARR soarr = (SOARR) o;
            return (soarr.domain.equals(domain) &&
                    soarr.server.equals(server) &&
                    soarr.contact.equals(contact) && soarr.serial == serial &&
                    soarr.refresh == refresh && soarr.retry == retry &&
                    soarr.expire == expire && soarr.minimum == minimum);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + domain.hashCode() + server.hashCode() +
                contact.hashCode() + serial + refresh + retry + expire + minimum;
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
    public String toString() {
        String s = "SOA:  ";
        s += "domain = " + domain;
        s += ", server = " + server;
        s += ", contact = " + contact;
        s += ", serial = " + serial;
        s += ", refresh = " + refresh;
        s += ", retry = " + retry;
        s += ", expire = " + expire;
        s += ", minimum = " + minimum;
        s += ", " + super.toString();
        return s;
    }

    @Override
    protected byte[] getBytes() {
        byte a[] = Utils.convertString(server);
        a = Utils.combine(a, Utils.convertString(contact));
        a = Utils.combine(a, Utils.getBytes(serial));
        a = Utils.combine(a, Utils.getBytes(refresh));
        a = Utils.combine(a, Utils.getBytes(retry));
        a = Utils.combine(a, Utils.getBytes(expire));
        a = Utils.combine(a, Utils.getBytes(minimum));
        return a;
    }
}

class HINFORR extends RR {
    private final String CPU;
    private final String OS;

    HINFORR(String name, int ttl, String CPU, String OS) {
        super(name, Utils.HINFO, ttl);
        this.CPU = CPU;
        this.OS = OS;
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (!(o instanceof HINFORR)) {
                return false;
            }

            HINFORR hinforr = (HINFORR) o;
            return hinforr.CPU.equals(CPU) && hinforr.OS.equals(OS);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + CPU.hashCode() + OS.hashCode();
    }

    @Override
    public String toString() {
        return ("HINFO: CPU = " + CPU + ", OS = " + OS + ", " +
                super.toString());
    }

    @Override
    protected byte[] getBytes() {
        return Utils.combine(Utils.toCS(CPU), Utils.toCS(OS));
    }
}

class MXRR extends RR {
    private final String host;
    private final int preference;

    MXRR(String name, int ttl, String host, int preference) {
        super(name, Utils.MX, ttl);
        this.host = host;
        this.preference = preference;
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (!(o instanceof MXRR)) {
                return false;
            }

            MXRR mxrr = (MXRR) o;
            return mxrr.host.equals(host) && mxrr.preference == preference;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + host.hashCode() + preference;
    }

    public String getHost() {
        return host;
    }

    @Override
    public String toString() {
        return ("MX: host = " + host + ", preference = " + preference +
                ", " + super.toString());
    }

    @Override
    protected byte[] getBytes() {
        byte c[] = new byte[2];
        c[0] = Utils.getByte(preference, 2);
        c[1] = Utils.getByte(preference, 1);

        return Utils.combine(c, Utils.convertString(host));
    }
}

abstract class STRINGRR extends RR {
    String string;

    STRINGRR(String name, int type, int ttl) {
        super(name, type, ttl);
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (!(o instanceof STRINGRR)) {
                return false;
            }

            STRINGRR stringrr = (STRINGRR) o;
            return stringrr.string.equals(string);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + string.hashCode();
    }

    public String getString() {
        return string;
    }

    @Override
    public String toString() {
        return "STRING: string = " + string + ", " + super.toString();
    }

    @Override
    protected byte[] getBytes() {
        return Utils.convertString(string);
    }
}

class TXTRR extends STRINGRR {
    TXTRR(String name, int ttl, String text) {
        super(name, Utils.TXT, ttl);
        this.string = text;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.toCS(string);
    }
}

class NSRR extends STRINGRR {
    NSRR(String domain, int ttl, String nameserver) {
        super(domain, Utils.NS, ttl);
        this.string = nameserver;
    }
}

class CNAMERR extends STRINGRR {
    CNAMERR(String alias, int ttl, String canonical) {
        super(alias, Utils.CNAME, ttl);
        this.string = canonical;
    }
}

class PTRRR extends STRINGRR {
    PTRRR(String address, int ttl, String host) {
        super(address, Utils.PTR, ttl);
        this.string = host;
    }
}

abstract class ADDRRR extends RR {
    String address;

    ADDRRR(String name, int type, int ttl) {
        super(name, type, ttl);
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (!(o instanceof ADDRRR)) {
                return false;
            }

            ADDRRR addrrr = (ADDRRR) o;
            return addrrr.address.equals(address);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + address.hashCode();
    }

    @Override
    public String toString() {
        return "ADDR: address = " + address + ", " + super.toString();
    }
}

class ARR extends ADDRRR {
    ARR(String name, int ttl, String address) {
        super(name, Utils.A, ttl);
        this.address = address;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.IPV4(address);
    }
}

class AAAARR extends ADDRRR {
    AAAARR(String name, int ttl, String address) {
        super(name, Utils.AAAA, ttl);
        this.address = address;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.IPV6(address);
    }
}

class DNSKEYRR extends RR {
    private final int flags;
    private final int protocol;
    private final int algorithm;
    private final String publicKey;

    DNSKEYRR(String domain, int ttl, int flags, int protocol, int algorithm,
             String publicKey) {
        super(domain, Utils.DNSKEY, ttl);

        this.flags = flags;
        this.protocol = protocol;
        this.algorithm = algorithm;
        this.publicKey = publicKey;
    }

    @Override
    protected byte[] getBytes() {
        byte a[] = new byte[0];
        a = Utils.combine(a, Utils.getTwoBytes(flags, 2));
        a = Utils.combine(a, Utils.getByte(protocol, 1));
        a = Utils.combine(a, Utils.getByte(algorithm, 1));
        a = Utils.combine(a, publicKey.getBytes(StandardCharsets.US_ASCII));
        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (!(o instanceof DNSKEYRR)) {
                return false;
            }

            DNSKEYRR dnskeyrr = (DNSKEYRR) o;
            return dnskeyrr.flags == flags &&
                    dnskeyrr.protocol == protocol &&
                    dnskeyrr.algorithm == algorithm &&
                    dnskeyrr.publicKey.equals(publicKey);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + flags + protocol + algorithm +
                publicKey.hashCode();
    }
}

// https://www.iana.org/assignments/dns-sec-alg-numbers/dns-sec-alg-numbers.xhtml
class NSEC3RR extends RR {
    private final int hashAlgorithm;
    private final int flags;
    private final int iterations;
    private final String salt;
    private final String nextHashedOwnerName;
    private final ArrayList<Integer> types;

    NSEC3RR(String domain, int ttl, int hashAlgorithm, int flags,
            int iterations, String salt, String nextHashedOwnerName,
            ArrayList<Integer> types) {
        super(domain, Utils.NSEC3, ttl);
        this.hashAlgorithm = hashAlgorithm;
        this.flags = flags;
        this.iterations = iterations;
        this.salt = salt;
        this.nextHashedOwnerName = nextHashedOwnerName;
        this.types = types;
    }

    @Override
    protected byte[] getBytes() {
        byte a[] = new byte[0];
        a = Utils.combine(a, Utils.getByte(hashAlgorithm, 1));
        a = Utils.combine(a, Utils.getByte(flags, 2));
        a = Utils.combine(a, Utils.getTwoBytes(iterations, 1));
        // Assertion.aver(Utils.getByte(salt.length() == 24);
        a = Utils.combine(a, Utils.getByte(salt.length(), 1));
        a = Utils.combine(a, Utils.convertString(salt));
        // Assertion.aver(Utils.getByte(salt.length() == 24);
        a = Utils.combine(a,
                Utils.getByte(this.nextHashedOwnerName.length(), 1));
        a = Utils.combine(a,
                Utils.convertString(nextHashedOwnerName));
        for (Integer i : types) {
        }

        return a;
    }

    @Override
    public boolean equals(Object o) {
        Assertion.fail();
        return false;
    }

    @Override
    public int hashCode() {
        Assertion.fail();
        return 42;
    }
}

class NSEC3PARAMRR extends RR {
    private final int hashAlgorithm;
    private final int flags;
    private final int iterations;
    private final String salt;

    NSEC3PARAMRR(String domain, int ttl, int hashAlgorithm, int flags,
                 int iterations, String salt) {
        super(domain, Utils.NSEC3PARAM, ttl);
        this.hashAlgorithm = hashAlgorithm;
        this.flags = flags;
        this.iterations = iterations;
        this.salt = salt;
    }

    @Override
    protected byte[] getBytes() {
        Assertion.fail();
        return null;
    }

    @Override
    public boolean equals(Object o) {
        Assertion.fail();
        return false;
    }

    @Override
    public int hashCode() {
        Assertion.fail();
        return 42;
    }
}

class DNSRRSIGRR extends RR {
    @Getter private final int typeCovered;
    private final int algorithm;
    private final int labels;
    private final int originalttl;
    private final int expiration;
    private final int inception;
    private int keyTag;
    private final String signersName;
    private final String signature;

    DNSRRSIGRR(String domain, int ttl, int typeCovered, int algorithm,
               int labels, int originalttl, int expiration, int inception,
               String signersName, String signature) {
        super(domain, Utils.RRSIG, ttl);

        this.typeCovered = typeCovered;
        this.algorithm = algorithm;
        this.labels = labels;
        this.originalttl = originalttl;
        this.expiration = expiration;
        this.inception = inception;
        this.signersName = signersName;
        this.signature = signature;
    }

    @Override
    protected byte[] getBytes() {
        byte a[] = new byte[0];
        a = Utils.combine(a, Utils.getTwoBytes(typeCovered, 2));
        a = Utils.combine(a, Utils.getByte(algorithm, 1));
        a = Utils.combine(a, Utils.getByte(labels, 1));
        a = Utils.combine(a, Utils.getBytes(originalttl));
        a = Utils.combine(a, Utils.getBytes(expiration));
        a = Utils.combine(a, Utils.getBytes(inception));
        a = Utils.combine(a, Utils.getTwoBytes(keyTag, 2));
        //a = Utils.combine(a, signersName.getBytes(StandardCharsets.US_ASCII));
        a = Utils.combine(a, new byte[1]);
        a = Utils.combine(a, signature.getBytes());

        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (!(o instanceof DNSRRSIGRR)) {
                return false;
            }

            DNSRRSIGRR dnsrrsigrr = (DNSRRSIGRR) o;

            return dnsrrsigrr.typeCovered == typeCovered &&
                    dnsrrsigrr.algorithm == algorithm &&
                    dnsrrsigrr.labels == labels &&
                    dnsrrsigrr.originalttl == originalttl &&
                    dnsrrsigrr.expiration == expiration &&
                    dnsrrsigrr.inception == inception &&
                    dnsrrsigrr.keyTag == keyTag &&
                    dnsrrsigrr.signersName.equals(signersName) &&
                    dnsrrsigrr.signature.equals(signature);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + typeCovered + algorithm + labels +
                originalttl + expiration + inception + keyTag +
                signersName.hashCode() + signature.hashCode();
    }

    @Override
    public String toString() {
        return " typeCovered = " + typeCovered +
                ", algorithm = " + algorithm +
                ", labels = " + labels +
                ", originalttl = " + originalttl +
                ", expiration = " + expiration +
                ", inception = " + inception +
                ", signersName = " + signersName +
                ", signature = " + signature +
                super.toString();
    }
}

class DNSNSECRR extends RR {
    private final String nextDomainName;
    private final byte[] typeBitMaps;

    DNSNSECRR(String domain, int ttl, String nextDomainName,
              byte[] typeBitMaps) {
        super(domain, Utils.NSEC, ttl);

        this.nextDomainName = nextDomainName;
        this.typeBitMaps = typeBitMaps;
    }

    @Override
    protected byte[] getBytes() {
        byte a[] = Utils.convertString(nextDomainName);
        a = Utils.combine(a, typeBitMaps);
        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (!(o instanceof DNSNSECRR)) {
                return false;
            }

            DNSNSECRR dnsnsecrr = (DNSNSECRR) o;
            return dnsnsecrr.nextDomainName.equals(nextDomainName) &&
                    Arrays.equals(dnsnsecrr.typeBitMaps, typeBitMaps);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + nextDomainName.hashCode() +
                Arrays.hashCode(typeBitMaps);
    }
}

// When an OPT RR is included within any DNS message, it MUST be the
// only OPT RR in that message.
class OPTRR {
    @Getter private boolean DNSSEC = false;
    final static Logger logger = JDNSS.logger;
    private int payloadSize;
    private int rcodeAndFlags;
    private int rdLength;
    private byte extendedrcode = 0; // extended RCODE of 0 indicates use of a regular RCODE
    private byte version;
    private int flags;
    private int optionCode;
    @Getter private int optionLength;
    @Getter private byte[] clientCookie;
    @Getter private byte[] serverCookie;

    OPTRR(byte[] bytes) {
        logger.traceEntry();
        Assertion.aver(bytes[0] == 0);

        int location = 1;

        int type = Utils.addThem(bytes[location++], bytes[location++]);
        Assertion.aver(type == 41);

        payloadSize = Utils.addThem(bytes[location++], bytes[location++]);
        logger.trace(payloadSize);

        extendedrcode = bytes[location++];
        logger.trace(extendedrcode);

        version = bytes[location++];
        Assertion.aver(version == 0);

        flags = Utils.addThem(bytes[location++], bytes[location++]);

        // logger.error(flags);
        DNSSEC = flags >> 15 == 1; // DNSSEC OK bit as defined by [RFC3225].
        // logger.error(DNSSEC);
        logger.trace(DNSSEC);

        rdLength = Utils.addThem(bytes[location++], bytes[location++]);
        logger.trace(rdLength);

        if (rdLength >= 5 ) { //data length needs to be minimum of 5 bytes

            optionCode = Utils.addThem(bytes[location++], bytes[location++]);
            Assertion.aver(optionCode == 10);

            optionLength = Utils.addThem(bytes[location++], bytes[location++]);

            /*
            If the COOKIE option is too short to contain a Client Cookie, then
            FORMERR is generated.  If the COOKIE option is longer than that
            required to hold a COOKIE option with just a Client Cookie (8 bytes)
            but is shorter than the minimum COOKIE option with both a
            Client Cookie and a Server Cookie (16 bytes), then FORMERR is
            generated.  If the COOKIE option is longer than the maximum valid
            COOKIE option (40 bytes), then FORMERR is generated.
            */


            clientCookie = Arrays.copyOfRange(bytes, location, location + 8);
            location += 8;


            if (optionLength > 8) { // server cookie returned
                // OPTION-LENGTH >= 16, <= 40 [rfc7873]
                Assertion.aver(optionLength == 16
                    || optionLength == 24
                    || optionLength == 32
                    || optionLength == 40);

                serverCookie = Arrays.copyOfRange(bytes, location, location + optionLength - 8);
            }
        }
    }
}
