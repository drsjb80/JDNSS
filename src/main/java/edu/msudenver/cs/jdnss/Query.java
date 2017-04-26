package edu.msudenver.cs.jdnss;
/**
 * parses the incoming DNS queries
 * @author Steve Beaty
 * @version $Id: Query.java,v 1.29 2011/03/14 19:07:22 drb80 Exp $
 */

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import javax.rmi.CORBA.Util;
import java.util.Vector;
import java.util.Arrays;
import java.net.DatagramPacket;

public class Query
{
    private String qnames[];
    private int qtypes[];
    private int qclasses[];
    private Logger logger = JDNSS.getLogger();

    // http://www.networksorcery.com/enp/protocol/dns.htm
    private int id;
    private int opcode = 0;     // standard query

    private int numQuestions;
    private int numAnswers;
    private int numAuthorities;
    private int numAdditionals;
    private int rcode;
    private boolean TC = false; // truncation
    private boolean QR = false; // query
    private boolean AA = true;  // authoritative answer
    private boolean RD = false; // recursion desired
    private boolean RA = false; // recursion available
    private boolean AD = false; // authenticated data
    private boolean CD = false; // checking disabled
    private boolean QU = false; // unicast

    private boolean firsttime = true;

    public boolean getQR() { return QR; }
    public int getId() { return id; }

    private boolean UDP;
    private int minimum;

    private byte[] buffer;
    private byte[] additional = new byte[0];
    private byte[] authority = new byte[0];

    private byte[] savedAdditional;
    private int savedNumAdditionals;

    public OPTRR optrr;
    private int maximumPayload = 512;
    private boolean doDNSSEC = false;

    /**
     * Rebuild the byte array from the object data
     */
    private void rebuild()
    {
        logger.trace(toString());
        logger.trace("\n" + Utils.toString(buffer));

        buffer[0] = Utils.getByte(id, 2);
        buffer[1] = Utils.getByte(id, 1);
        buffer[2] =(byte)
        (
            (QR ? 128 : 0) |
            (opcode << 3) |
            (AA ? 4 : 0) |
            (TC ? 2 : 0) |
            (RD ? 1 : 0)
        );
        buffer[3] =(byte)
        (
            (RA ? 128 : 0) |
            (AD ? 32 : 0) |
            (CD ? 16 : 0) |
            rcode
        );
        buffer[4] = Utils.getByte(numQuestions, 2);
        buffer[5] = Utils.getByte(numQuestions, 1);
        buffer[6] = Utils.getByte(numAnswers, 2);
        buffer[7] = Utils.getByte(numAnswers, 1);
        buffer[8] = Utils.getByte(numAuthorities, 2);
        buffer[9] = Utils.getByte(numAuthorities, 1);
        buffer[10] = Utils.getByte(numAdditionals, 2);
        buffer[11] = Utils.getByte(numAdditionals, 1);

        logger.trace(toString());
        logger.trace("\n" + Utils.toString(buffer));
    }

    Query(int id, String[] qnames, int qtypes[], int qclasses[])
    {
        // internal representation
        this.id = id;
        this.numQuestions = qnames.length;
        this.qnames = new String[this.numQuestions];
        this.qtypes = new int[this.numQuestions];
        this.qclasses = new int[this.numQuestions];

        // set aside room for the header; it is created later via rebuild()
        buffer = new byte[12];

        for (int i = 0; i < numQuestions; i++)
        {
            this.qnames[i] = qnames[i];
            this.qtypes[i] = qtypes[i];
            this.qclasses[i] = qclasses[i];

            buffer = Utils.combine(buffer, Utils.convertString(qnames[i]));
            buffer = Utils.combine(buffer, Utils.getTwoBytes(qtypes[i], 2));
            buffer = Utils.combine(buffer, Utils.getTwoBytes(qclasses[i], 2));
        }
        rebuild();
    }

    /**
     * creates a Query from a packet
     */
    public Query(byte b[])
    {
        buffer =         Arrays.copyOf(b, b.length);
        id =             Utils.addThem(buffer[0], buffer[1]);
        numQuestions =   Utils.addThem(buffer[4], buffer[5]);
        numAnswers =     Utils.addThem(buffer[6], buffer[7]);
        numAuthorities = Utils.addThem(buffer[8], buffer[9]);
        numAdditionals = Utils.addThem(buffer[10], buffer[11]);

        Assertion.aver(numAnswers == 0);
        Assertion.aver(numAuthorities == 0);

        int flags = Utils.addThem(buffer[2], buffer[3]);
        QR =      (flags & 0x00008000) != 0;
        opcode =  (flags & 0x00007800) >> 11;
        AA =      (flags & 0x00000400) != 0;
        TC =      (flags & 0x00000200) != 0;
        RD =      (flags & 0x00000100) != 0;
        RA =      (flags & 0x00000080) != 0;
        AD =      (flags & 0x00000020) != 0;
        CD =      (flags & 0x00000010) != 0;
        rcode =   flags & 0x0000000f;

        qnames = new String[numQuestions];
        qtypes = new int[numQuestions];
        qclasses = new int[numQuestions];
    }

    public byte[] getBuffer()
    {
        return Arrays.copyOf(buffer, buffer.length);
    }

    /**
     * Evaluates and saves all questions
     */
    private void parseQueries()
    {
        logger.traceEntry();

        /*
        The question section is used to carry the "question" in most queries,
        i.e., the parameters that deinfo what is being asked.  The section
        contains QDCOUNT(usually 1) entries, each of the following format:

        1  1  1  1  1  1
        0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                                               |
        /                     QNAME                     /
        /                                               /
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                     QTYPE                     |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                     QCLASS                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        */

        int location = 12;

        for (int i = 0; i < numQuestions; i++)
        {
            StringAndNumber sn = null;
            try
            {
                sn = Utils.parseName(location, buffer);
            }
            catch (AssertionError ae)
            {
                logger.catching(ae);
                throw ae;
            }

            location = sn.getNumber();
            qnames[i] = sn.getString();
            qtypes[i] = Utils.addThem(buffer[location], buffer[location + 1]);
            location += 2;

            /*
            ** Multicast DNS defines the top bit in the class field of a
            ** DNS question as the unicast-response bit.  When this bit is
            ** set in a question, it indicates that the querier is willing
            ** to accept unicast replies in response to this specific
            ** query, as well as the usual multicast responses.  These
            ** questions requesting unicast responses are referred to as
            ** "QU" questions, to distinguish them from the more usual
            ** questions requesting multicast responses ("QM" questions).
            */
            qclasses[i] = Utils.addThem(buffer[location], buffer[location + 1]);
            QU = (qclasses[i] & 0xc000) == 0xc000;
            location += 2;
        }

        if (numAdditionals > 0)
        {
            int length = buffer.length - location;
            savedNumAdditionals = numAdditionals;
            savedAdditional = new byte[length];
            System.arraycopy(buffer, location, savedAdditional, 0, length);
            parseAdditional(savedAdditional, savedNumAdditionals);
            buffer = Utils.trimByteArray(buffer, location);
            numAdditionals = 0;
            rebuild();
        }
    }

    public void parseAdditional(byte[] additional, int rrCount)
    {
        try {
            int rrLocation = 0;
            for (int i = 0; i < rrCount; i++) {
                byte[] bytes = new byte[additional.length - rrLocation];
                System.arraycopy(additional, rrLocation, bytes, 0, additional.length - rrLocation);
                OPTRR tempRR = new OPTRR(bytes);

                if (tempRR.isValid()) {
                    optrr = new OPTRR(bytes);
                    maximumPayload = optrr.getPayloadSize();
                    doDNSSEC = optrr.dnssecAware();
                }
                rrLocation = rrLocation + tempRR.getByteSize() + 1;
            }
        } catch(Exception ex)
        {
            //RETURN Invalid
        }
    }

    public String toString()
    {
        String s = "Id: 0x" + Integer.toHexString(id) + "\n";
        s += "Questions: " + numQuestions + "\t";
        s += "Answers: " + numAnswers + "\n";
        s += "Authority RR's: " + numAuthorities + "\t";
        s += "Additional RR's: " + numAdditionals + "\n";

        s += "QR: " + QR + "\t";
        s += "AA: " + AA + "\t";
        s += "TC: " + TC + "\n";
        s += "RD: " + RD + "\t";
        s += "RA: " + RA + "\t";
        s += "AD: " + AD + "\n";
        s += "CD: " + CD + "\t";
        s += "QU: " + QU + "\n";
        s += "opcode: " + opcode + "\n";
        s += "rcode: " + rcode;

        for (int i = 0; i < numQuestions; i++)
        {
            s += "\nName: " + qnames[i] +
                " Type: " + qtypes[i] +
                " Class: " + qclasses[i];
        }
        return s;
    }

    private void addThem(Vector<RR>v, String host, int type)
    {
        Assertion.aver(v != null, "v == null");
        Assertion.aver(host != null, "host == null");

        for (int i = 0; i < v.size(); i++)
        {
            RR rr = v.elementAt(i);
            additional =
                Utils.combine(additional, rr.getBytes(host, minimum));
            numAdditionals++;
        }
    }

    /**
     * Given a zone and an MX or NS hostname, see if there is an A or AAAA
     * record we can also send back...
     */
    private void createAdditional(Zone zone, String host, String name)
    {
        Vector v = null;
        try
        {
            v = zone.get(Utils.A, host);
            addThem(v, host, Utils.A);

            if (doDNSSEC)
            {
                //additional = addRRSignature(zone, Utils.A, name, additional, Utils.ADDITIONAL);
            }
        }
        catch (AssertionError AE1)
        {
            // try the AAAA
        }

        try
        {
            v = zone.get(Utils.AAAA, host);
            addThem(v, host, Utils.AAAA);

            if (doDNSSEC)
            {
                //additional = addRRSignature(zone, Utils.AAAA, name, additional, Utils.ADDITIONAL);
            }
        }
        catch (AssertionError AE2)
        {
            // maybe we found an A
        }
    }

    private void createAuthorities(Zone zone, String name)
    {
        Vector<NSRR> v = zone.get(Utils.NS, zone.getName());
        logger.trace(v);

        if (v != null)
        {
            for (int i = 0; i < v.size(); i++)
            {
                NSRR rr = v.elementAt(i);
                logger.trace(rr);
                authority = Utils.combine(authority, rr.getBytes(rr.getName(),
                    minimum));
                numAuthorities++;

                // add address if known
                createAdditional(zone, rr.getString(), name);
            }
        }
    }

    private void createResponses(Zone zone, Vector<RR> v, String name,
        int which)
    {
        System.out.println("Create Responses");
        Assertion.aver(zone != null, "zone == null");
        Assertion.aver(v != null, "v == null");
        Assertion.aver(name != null, "name == null");

        logger.traceEntry(new ObjectMessage(v));
        logger.traceEntry(new ObjectMessage(name));
        logger.traceEntry(new ObjectMessage(which));

        for (int i = 0; i < v.size(); i++) {
            RR rr = v.elementAt(i);
            byte add[] = rr.getBytes(name, minimum);

            if (UDP && (buffer.length + add.length > maximumPayload)) {
                TC = true;
                rebuild();
                return;
            }
            buffer = Utils.combine(buffer, add);
            numAnswers++;

            //Add RRSIG Records Corresponding to Type
            if (doDNSSEC) {
                System.out.println("BUFFER BEFORE: " + buffer.length);
                //addRRSignature(zone, rr.getType(), name, buffer, Utils.ANSWER);
                System.out.println("BUFFER AFTER: " + buffer.length);

            }

            if (firsttime && which != Utils.NS) {
                createAuthorities(zone, name);
            }

            firsttime = false;

            if (which == Utils.MX) {
                createAdditional(zone, ((MXRR) v.elementAt(i)).getHost(), name);

            } else if (which == Utils.NS) {
                createAdditional(zone, ((NSRR) v.elementAt(i)).getString(), name);
            }
        }

        rebuild();
    }

    public void addDNSKeys(Zone zone, String host){
        System.out.println("Adding DNSKEYS");
        Vector v = null;
        try {
            v = zone.get(Utils.DNSKEY, host);
            System.out.println("adding them");
            addThem(v, host, Utils.DNSKEY);
        } catch (AssertionError AE1)
        {
        }
    }

    private void addRRSignature(Zone zone, int type, String name, byte[] destination, int section) {
        Vector<DNSRRSIGRR> rrsigv = null;
        try {
            rrsigv = zone.get(Utils.RRSIG, zone.getName());
        } catch (AssertionError ex) {
            return;
        }

        System.out.println("RRSIGV.SIZE: " + rrsigv.size());
        for (int i = 0; i < rrsigv.size(); i++) {
            DNSRRSIGRR rrsig = rrsigv.elementAt(i);
            if (rrsig.getTypeCovered() == type) {
                System.out.println("RRSIG match found. Type Covered: " + rrsig.getTypeCovered() + " Size: " + rrsig.getBytes().length);

                byte add[] = rrsig.getBytes();
                switch(section)
                {
                    case Utils.ANSWER:
                        if (UDP && (buffer.length + add.length > maximumPayload))
                        {
                            TC = true;
                            rebuild();
                            return;
                        }
                        System.out.println("BUFFER LENGTH: " + buffer.length + "Destination Length: " + destination.length);
                        buffer = Utils.combine(destination, add);
                        numAnswers++;
                        break;
                    case Utils.ADDITIONAL:
                        additional = Utils.combine(destination, add);
                        numAdditionals++;
                        break;
                    case Utils.AUTHORITY:
                        authority = Utils.combine(destination, add);
                        numAuthorities++;
                        break;
                }
            }
        }
    }

    private void addNSECRecords(Zone zone, String name)
    {
        Vector<RR> nsecv = null;
        try {
            nsecv = zone.get(Utils.NSEC, zone.getName());
        } catch (AssertionError ex){
            return;
        }
        DNSNSECRR nsec = (DNSNSECRR)nsecv.get(0);
        byte add[] = nsec.getBytes(name, minimum);
        authority = Utils.combine(authority, add);
        numAuthorities++;
    }

    private void addAuthorities()
    {
        logger.trace(UDP);
        logger.trace(buffer.length);
        logger.trace(authority.length);

        if (!UDP ||(UDP &&(buffer.length + authority.length < maximumPayload)))
        {
            logger.trace("adding in authorities");
            buffer = Utils.combine(buffer, authority);
        }
        else
        {
            logger.trace("NOT adding in authorities");
            numAuthorities = 0;
        }
    }

    private void addAdditionals()
    {
        if (savedNumAdditionals > 0)
        {
            additional = Utils.combine(additional, savedAdditional);
            numAdditionals += savedNumAdditionals;
        }

        if (numAdditionals > 0)
        {
            if (!UDP || (UDP &&(buffer.length + additional.length < maximumPayload)))
            {
                buffer = Utils.combine(buffer, additional);
            }
            else
            {
                numAdditionals = 0;
            }
        }
    }

    private void addSOA(Zone zone, SOARR SOA)
    {
        authority = Utils.combine (authority, SOA.getBytes(zone.getName(),
            minimum));
        numAuthorities++;
    }

    /*
    Suppose that an authoritative server has an A RR but has no AAAA RR for a
    host name.  Then, the server should return a response to a query for an
    AAAA RR of the name with the response code(RCODE) being 0(indicating no
    error) and with an empty answer section(see Sections 4.3.2 and 6.2.4 of
    [1]).  Such a response indicates that there is at least one RR of a
    different type than AAAA for the queried name, and the stub resolver can
    then look for A RRs.

    This way, the caching server can cache the fact that the queried name has
    no AAAA RR(but may have other types of RRs), and thus improve the response
    time to further queries for an AAAA RR of the name.
    */
    private void dealWithOther(Zone zone, int type, String name)
    {
        int other = type == Utils.A ? Utils.AAAA : Utils.A;

        Vector v = null;
        try
        {
            v = zone.get(other, name);
        }
        catch (AssertionError AE)
        {
            logger.debug(Utils.mapTypeToString(type) + " lookup of " +
                name + " failed");

            rcode = Utils.NAMEERROR;
            return;
        }

        logger.debug(Utils.mapTypeToString(type) +
            " lookup of " + name + " failed but " +
            Utils.mapTypeToString(other) + " record found");

        rcode = Utils.NOERROR;
    }

    // Keeping it DRY.
    private void errLookupFailed(int type, String name, int rcode)
    {
        logger.debug("'" + Utils.mapTypeToString(type) +
            "' lookup of " + name + " failed");
        this.rcode = rcode;
    }

    /**
     * See if we have an cononical name associated with a name and return
     * if so.
     */
    private StringAndVector checkForCNAME(int type, String name,
        Zone zone, SOARR SOA)
    {
        logger.traceEntry(Integer.toString(type));
        logger.traceEntry(name);
        logger.traceEntry(zone.toString());

        Vector<RR> u = zone.get(Utils.CNAME, name);

        // grab the first one as they all should work.
        String s =  u.elementAt(0).getString();
        Assertion.aver(s != null);

        Vector<RR> v = zone.get(type, s);

        // yes, so first put in the CNAME
        createResponses(zone, u, name, Utils.CNAME);

        // then continue the lookup on the original type
        // with the new name
        return new StringAndVector(s, v);
    }

    /**
     * create a byte array that is a Response to a Query
     */
    public byte[] makeResponses(JDNSS dnsService, boolean UDP)
    {
        System.out.println("Make Responses");
        parseQueries();

        this.UDP = UDP;

        QR = true;  // response
        AA = true;  // we are authoritative
        RA = false; // recursion not available

        for (int i = 0; i < qnames.length; i++)
        {
            String name = qnames[i];
            int type = qtypes[i];

            logger.trace(name);
            logger.trace(type);

            Zone zone = null;
            try
            {
                zone = dnsService.getZone(name);
            }
            catch (AssertionError AE)
            {
                logger.debug("Zone lookup of " + name + " failed");
                rcode = Utils.REFUSED;
                AA = false;
                rebuild();
                return Arrays.copyOf(buffer, buffer.length);
            }

            logger.trace(zone);

            // always need to get the SOA to find the default minimum
            Vector<SOARR> w = null;
            try
            {
                w = zone.get(Utils.SOA, zone.getName());
            }
            catch (AssertionError AE)
            {
                logger.debug("SOA lookup in " + zone.getName() + " failed");
                rcode = Utils.SERVFAIL;
                rebuild();
                return Arrays.copyOf(buffer, buffer.length);
            }

            SOARR SOA = w.elementAt(0);
            minimum = SOA.getMinimum();

            if (doDNSSEC) {
                /*System.out.println("Add RRSIG Records.  Authority size = " + authority.length);
                addRRSignature(zone, Utils.NSEC, name, authority, Utils.AUTHORITY);
                System.out.println("Add RRSIG Records DONE.  Authority size = " + authority.length);

                //byte[] myArray= addRRSignature(zone, Utils.NSEC, name, authority, Utils.AUTHORITY);

                //OPTRR rr = new OPTRR(myArray);
                */

            }

            Vector v = null;
            try
            {
                v = zone.get(type, name);

                if (doDNSSEC) {
                    System.out.println("ADDING NSEC RECORDS");
                    addNSECRecords(zone, name);
                    //addNSEC3Records
                }
            }
            catch (AssertionError AE)
            {
                logger.debug("Didn't find: " + name);
                if (type != Utils.AAAA && type != Utils.A)
                {
                    logger.debug(name + " not A or AAAA, giving up");
                    errLookupFailed(type, name, Utils.NOERROR);
                    addSOA(zone, SOA);
                    if (doDNSSEC) {
                        System.out.println("Add NSEC Records.  Additional size = " + authority.length);
                        addNSECRecords(zone, name);
                       // authority = addRRSignature(zone, Utils.NSEC, name, authority, Utils.AUTHORITY);
                        System.out.println("Add NSEC Records complete.  Additional size = " + authority.length);
                    }
                    addAuthorities();
                    rebuild();
                    return Arrays.copyOf(buffer, buffer.length);
                }
                else
                {
                    logger.debug("Looking for a CNAME for " + name);
                    StringAndVector StringAndVector = null;
                    try
                    {
                        StringAndVector = checkForCNAME(type, name, zone,
                            SOA);
                    }
                    catch (AssertionError AE2)
                    {
                        logger.debug("Didn't find a CNAME for " + name);
                        // look for AAAA if A and vice versa
                        dealWithOther(zone, type, name);
                        addSOA(zone, SOA);
                        if (doDNSSEC) {
                            System.out.println("Add NSEC Records.  Additional size = " + authority.length);
                            addNSECRecords(zone, name);
                   //         authority = addRRSignature(zone, Utils.NSEC, name, authority, Utils.AUTHORITY);
                            System.out.println("Add NSEC Records complete.  Additional size = " + authority.length);
                        }
                        addAuthorities();
                        rebuild();
                        return Arrays.copyOf(buffer, buffer.length);
                    }

                    v = StringAndVector.getVector();
                    name = StringAndVector.getString();
                }
            }

            if (numAdditionals != 0 && dnsService.getJdnssArgs().RFC2671)
            {
                logger.debug("Additionals not understood");
                rcode = Utils.NOTIMPL;
                rebuild();
                return Arrays.copyOf(buffer, buffer.length);
            }

            addDNSKeys(zone, name);

            createResponses(zone, v, name, type);
        }

        if (numAuthorities > 0)
        {
            addAuthorities();
        }

        addAdditionals();
        rebuild();
        return Arrays.copyOf(buffer, buffer.length);
    }
}
