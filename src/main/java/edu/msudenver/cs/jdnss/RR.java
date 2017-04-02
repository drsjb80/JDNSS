package edu.msudenver.cs.jdnss;
/**
 * Create and manipulate resource records
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc1035.html"> faqs.org </a>
 * @author Steve Beaty
 */

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

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
**        |                   RDLENGTH                    |
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
**        /                     RDATA                     /
**        /                                               /
**        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
*/

public abstract class RR
{
    private String rrname;
    private int rrtype;
    private int rrclass = 1;        // IN
    private int TTL;

    public RR(String rrname, int rrtype, int TTL)
    {
        this.rrname = rrname;
        this.rrtype = rrtype;
        this.TTL = TTL;
    }

    public RR(byte[] bytes)
    {
        int location = 0;

        StringAndNumber sn = null;
        try
        {
            sn = Utils.parseName(location, bytes);
        }
        catch (AssertionError ae)
        {
            Assertion.aver(false);
        }

        location = sn.getNumber();
        this.rrname = sn.getString();

        this.rrtype = Utils.addThem(bytes[location++], bytes[location++]);
        this.rrclass = Utils.addThem(bytes[location++], bytes[location++]);
        this.TTL = Utils.addThem(bytes[location++], bytes[location++],
            bytes[location++], bytes[location++]);
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof RR))
        {
            return false;
        }

        RR rr = (RR) o;
        return rr.rrname.equals(this.rrname) &&
            rr.rrclass == rrclass &&
            rr.rrtype == rrtype &&
            rr.TTL == TTL;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }

    public int getType() { return rrtype; }
    public String getName() { return rrname; }
    protected int getTTL() { return TTL; }
    protected int getRrClass() { return rrclass; }

    // to enhance polymorphism and decrease casting for derived classes
    protected String getString() { Assertion.aver(false); return null; }
    protected String getHost() { Assertion.aver(false); return null; }

    /**
     * converts all the internal data to a byte array
     * @return the resource record to put in the response
     */
    public byte[] getBytes(String question, int TTLminimum)
    {
        int type = getType();
        int ttl = TTL == 0 ? TTLminimum : TTL;

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
        a[where++] = Utils.getByte(ttl, 4);
        a[where++] = Utils.getByte(ttl, 3);
        a[where++] = Utils.getByte(ttl, 2);
        a[where++] = Utils.getByte(ttl, 1);
        a[where++] = Utils.getByte(rdatalen, 2);
        a[where++] = Utils.getByte(rdatalen, 1);
        System.arraycopy(rdata, 0, a, where, rdata.length);

        return a;
    }

    protected abstract byte[] getBytes();

    public String toString()
    {
        return("name = " + rrname + ", type = " +
            Utils.mapTypeToString(rrtype) + ", TTL = " + TTL);
    }
}

/**
 * Just a simple class for queries.
 */
class QRR extends RR
{
    public QRR(String rrname, int rrtype)
    {
        super(rrname, rrtype, 0);
    }

    protected byte[] getBytes() { return new byte[0]; }
}

class SOARR extends RR
{
    private String domain;
    private String server;
    private String contact;
    private int serial;
    private int refresh;
    private int retry;
    private int expire;
    private int minimum;

    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof SOARR))
            {
                return false;
            }

            SOARR soarr =(SOARR) o;
            return(soarr.domain.equals(domain) && 
                soarr.server.equals(server) &&
                soarr.contact.equals(contact) && soarr.serial == serial &&
                soarr.refresh == refresh && soarr.retry == retry &&
                soarr.expire == expire && soarr.minimum == minimum);
        }

        return false;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }

    SOARR(String domain, String server, String contact, int serial,
        int refresh, int retry, int expire, int minimum, int TTL)
    {
        super(domain, Utils.SOA, TTL);

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
    ** It may be observed that in section 3.2.1 of RFC1035, which deinfos
    ** the format of a Resource Record, that the definition of the TTL field
    ** contains a throw away line which states that the TTL of an SOA record
    ** should always be sent as zero to prevent caching.  This is mentioned
    ** nowhere else, and has not generally been implemented.
    ** Implementations should not assume that SOA records will have a TTL of
    ** zero, nor are they required to send SOA records with a TTL of zero.
    **
    ** this however does not say what SHOULD be sent as the TTL...
    */

    public int getMinimum() { return minimum; }

    public String toString()
    {
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

    protected byte[] getBytes()
    {
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

class HINFORR extends RR
{
    private String CPU, OS;

    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof HINFORR))
            {
                return false;
            }

            HINFORR hinforr =(HINFORR) o;
            return hinforr.CPU.equals(CPU) && hinforr.OS.equals(OS);
        }

        return false;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }

    HINFORR(String name, int TTL, String CPU, String OS)
    {
        super(name, Utils.HINFO, TTL);
        this.CPU = CPU;
        this.OS = OS;
    }

    public String toString()
    {
        return("HINFO: CPU = " + CPU + ", OS = " + OS + ", " +
            super.toString());
    }

    protected byte[] getBytes()
    {
        return Utils.combine(Utils.toCS(CPU), Utils.toCS(OS));
    }
}

class MXRR extends RR
{
    private String host;
    private int preference;

    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof MXRR))
            {
                return false;
            }

            MXRR mxrr =(MXRR) o;
            return mxrr.host.equals(host) && mxrr.preference == preference;
        }

        return false;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }

    public String getHost() { return host; }

    MXRR(String name, int TTL, String host, int preference)
    {
        super(name, Utils.MX, TTL);
        this.host = host;
        this.preference = preference;
    }

    public String toString()
    {
        return("MX: host = " + host + ", preference = " + preference +
            ", " + super.toString());
    }

    protected byte[] getBytes()
    {
        byte c[] = new byte[2];
        c[0] = Utils.getByte(preference, 2);
        c[1] = Utils.getByte(preference, 1);

        return Utils.combine(c, Utils.convertString(host));
    }
}

abstract class STRINGRR extends RR
{
    protected String string;

    STRINGRR(String name, int type, int TTL)
    {
        super(name, type, TTL);
    }

    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof STRINGRR))
            {
                return false;
            }

            STRINGRR stringrr =(STRINGRR) o;
            return stringrr.string.equals(string);
        }

        return false;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }

    public String getString() { return string; }

    public String toString()
    {
        return "STRING: string = " + string + ", " + super.toString();
    }

    protected byte[] getBytes() { return Utils.convertString(string); }
}

class TXTRR extends STRINGRR
{
    TXTRR(String name, int TTL, String text)
    {
        super(name, Utils.TXT, TTL);
        this.string = text;
    }

    protected byte[] getBytes() { return Utils.toCS(string); }
}

class NSRR extends STRINGRR
{
    NSRR(String domain, int TTL, String nameserver)
    {
        super(domain, Utils.NS, TTL);
        this.string = nameserver;
    }
}

class CNAMERR extends STRINGRR
{
    CNAMERR(String alias, int TTL, String canonical)
    {
        super(alias, Utils.CNAME, TTL);
        this.string = canonical;
    }
}

class PTRRR extends STRINGRR
{
    PTRRR(String address, int TTL, String host)
    {
        super(address, Utils.PTR, TTL);
        this.string = host;
    }
}

abstract class ADDRRR extends RR
{
    protected String address;

    ADDRRR(String name, int type, int TTL)
    {
        super(name, type, TTL);
    }

    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof ADDRRR))
            {
                return false;
            }

            ADDRRR addrrr =(ADDRRR) o;
            return addrrr.address.equals(address);
        }

        return false;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }

    public String getAddress() { return address; }

    public String toString()
    {
        return "ADDR: address = " + address + ", " + super.toString();
    }
}

class ARR extends ADDRRR
{
    ARR(String name, int TTL, String address)
    {
        super(name, Utils.A, TTL);
        this.address = address;
    }

    protected byte[] getBytes() { return Utils.IPV4(address); }
}

class AAAARR extends ADDRRR
{
    AAAARR(String name, int TTL, String address)
    {
        super(name, Utils.AAAA, TTL);
        this.address = address;
    }

    protected byte[] getBytes() { return Utils.IPV6(address); }
}

class DNSKEYRR extends RR
{
    private int flags;
    private int protocol;
    private int algorithm;
    private String publicKey;

    DNSKEYRR(String domain, int TTL, int flags, int protocol, int algorithm,
        String publicKey)
    {
        super(domain, Utils.DNSKEY, TTL);

        this.flags = flags;
        this.protocol = protocol;
        this.algorithm = algorithm;
        this.publicKey = publicKey;
    }

    protected byte[] getBytes()
    {
        byte a[] = new byte[0];
        a = Utils.combine(a, Utils.getTwoBytes(flags, 2));
        a = Utils.combine(a, Utils.getByte(protocol, 1));
        a = Utils.combine(a, Utils.getByte(algorithm, 1));
        a = Utils.combine(a, publicKey.getBytes(StandardCharsets.US_ASCII));
        return a;
    }

    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof DNSKEYRR))
            {
                return false;
            }

            DNSKEYRR dnskeyrr =(DNSKEYRR) o;
            return(dnskeyrr.flags == flags &&
                dnskeyrr.protocol == protocol &&
                dnskeyrr.algorithm == algorithm &&
                dnskeyrr.publicKey.equals(publicKey));
        }

        return false;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }
}

class DNSRRSIGRR extends RR
{
    private int typeCovered;
    private int algorithm;
    private int labels;
    private int originalTTL;
    private int expiration;
    private int inception;
    private int keyTag;
    private String signersName;
    private String signature;

    DNSRRSIGRR(String domain, int TTL, int typeCovered, int algorithm,
        int labels, int originalTTL, int expiration, int inception,
        String signersName, String signature)
    {
        super(domain, Utils.RRSIG, TTL);

        this.typeCovered = typeCovered;
        this.algorithm = algorithm;
        this.labels = labels;
        this.originalTTL = originalTTL;
        this.expiration = expiration;
        this.inception = inception;
        this.signersName = signersName;
        this.signature = signature;
    }

    protected byte[] getBytes()
    {
        byte a[] = new byte[0];
        a = Utils.combine(a, Utils.getTwoBytes(typeCovered, 2));
        a = Utils.combine(a, Utils.getByte(algorithm, 1));
        a = Utils.combine(a, Utils.getByte(labels, 1));
        a = Utils.combine(a, Utils.getBytes(originalTTL));
        a = Utils.combine(a, Utils.getBytes(expiration));
        a = Utils.combine(a, Utils.getBytes(inception));
        a = Utils.combine(a, Utils.getTwoBytes(keyTag, 2));
        a = Utils.combine(a, signersName.getBytes(StandardCharsets.US_ASCII));
        a = Utils.combine(a, signature.getBytes());
        return a;
    }

    public int getTypeCovered()
    {
        return typeCovered;
    }

    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof DNSRRSIGRR))
            {
                return false;
            }

            DNSRRSIGRR dnsrrsigrr =(DNSRRSIGRR) o;

            return(dnsrrsigrr.typeCovered == typeCovered &&
                dnsrrsigrr.algorithm == algorithm &&
                dnsrrsigrr.labels == labels &&
                dnsrrsigrr.originalTTL == originalTTL &&
                dnsrrsigrr.expiration == expiration &&
                dnsrrsigrr.inception == inception &&
                dnsrrsigrr.keyTag == keyTag &&
                dnsrrsigrr.signersName.equals(signersName) &&
                dnsrrsigrr.signature.equals(signature));
        }

        return false;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }


    public String toString()
    {
        return("DNSRRSIGRR: " +
        " typeCovered = " + typeCovered +
        ", algorithm = " + algorithm +
        ", labels = " + labels +
        ", originalTTL = " + originalTTL +
        ", expiration = " + expiration +
        ", inception = " + inception +
        ", signersName = " + signersName +
        ", signature = " + signature +
        ", " + super.toString());
    }
}

class DNSNSECRR extends RR
{
    private String nextDomainName;
    private byte[] typeBitMaps;

    DNSNSECRR(String domain, int TTL, String nextDomainName,
        byte[] typeBitMaps)
    {
        super(domain, Utils.NSEC, TTL);

        this.nextDomainName = nextDomainName;
        this.typeBitMaps = typeBitMaps;
    }

    protected byte[] getBytes()
    {
        byte a[] = Utils.convertString(nextDomainName);
        a = Utils.combine(a, typeBitMaps);
        return a;
    }

    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof DNSNSECRR))
            {
                return false;
            }

            DNSNSECRR dnsnsecrr =(DNSNSECRR) o;
            return(dnsnsecrr.nextDomainName.equals(nextDomainName) &&
                Arrays.equals(dnsnsecrr.typeBitMaps, typeBitMaps));
        }

        return false;
    }

    public int hashCode()
    {
        Assertion.aver(false);
        return 42;
    }
}

class OPTRR extends RR
{
    public boolean DOBit;
    public int payloadSize;

    OPTRR(byte[] bytes) {
        super(bytes);

        //Assert OPTRR is type 41 (See RFC 6891)
        Assertion.aver(getType() == 41,
                "Expecting OPTRR type to equal 41");

        DOBit = (((getTTL() >> 15) & 1) == 1);
        payloadSize = getRrClass();

        if (payloadSize < 512)
            payloadSize = 512;
    }

    @Override
    protected byte[] getBytes() {
        return this.getBytes();
    }
}

