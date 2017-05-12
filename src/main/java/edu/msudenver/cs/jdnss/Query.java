package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.Vector;
import java.util.Arrays;
import java.net.DatagramPacket;

public class Query
{
    private Header header;
    private byte[] buffer;

    private String qnames[];
    private int qtypes[];
    private int qclasses[];

    private Logger logger = JDNSS.getLogger();
    private Zone zone;
    private SOARR SOA;

    private boolean firsttime = true;

    private boolean UDP;
    private boolean QU;     // unicast response requested
    private int minimum;

    private byte[] additional = new byte[0];
    private byte[] authority = new byte[0];

    private int numQuestions;
    private int numAnswers;
    private int numAuthorities;

    private byte[] savedAdditional;
    private int savedNumAdditionals;

    public OPTRR optrr;
    private int maximumPayload = 512;
    private boolean doDNSSEC = false;

    /**
     * creates a Query from a packet
     */
    public Query(byte b[])
    {
        buffer = Arrays.copyOf(b, b.length);
        header = new Header(buffer);
        numQuestions = header.getNumQuestions();
        numAnswers = header.getNumAnswers();
        numAuthorities = header.getNumAuthorities();

        // FIXME: put a bunch of avers here
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
        qnames = new String[numQuestions];
        qtypes = new int[numQuestions];
        qclasses = new int[numQuestions];

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

        if (header.getNumAdditionals() > 0)
        {
            int length = buffer.length - location;
            savedNumAdditionals = header.getNumAdditionals();
            savedAdditional = new byte[length];
            System.arraycopy(buffer, location, savedAdditional, 0, length);
            parseAdditional(savedAdditional, savedNumAdditionals);
            buffer = Utils.trimByteArray(buffer, location);
            header.clearNumAdditionals();
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
        String s = header.toString();

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
            header.incrementNumAdditionals();
        }
    }

    /**
     * Given a zone and an MX or NS hostname, see if there is an A or AAAA
     * record we can also send back...
     */
    private void createAdditional(String host, String name)
    {
        Vector v = null;
        try
        {
            v = zone.get(Utils.A, host);
            addThem(v, host, Utils.A);

            if (doDNSSEC)
            {
                addRRSignature(Utils.A, name, additional, Utils.ADDITIONAL);
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
                addRRSignature(Utils.AAAA, name, additional,
                    Utils.ADDITIONAL);
            }
        }
        catch (AssertionError AE2)
        {
            // maybe we found an A
        }
    }

    private void createAuthorities(String name)
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
                header.incrementNumAuthorities();

                createAdditional(rr.getString(), name);
            }
            if (doDNSSEC){
                addRRSignature(Utils.NS, name, authority, Utils.AUTHORITY);
            }
        }
    }

    private void createResponses(Vector<RR> v, String name, int which)
    {
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
                header.setTC();
                return;
            }
            buffer = Utils.combine(buffer, add);
            numAnswers++;

            //Add RRSIG Records Corresponding to Type
            if (doDNSSEC) {
                addRRSignature(rr.getType(), name, buffer, Utils.ANSWER);
            }

            if (firsttime && which != Utils.NS) {
                createAuthorities(name);
            }

            firsttime = false;

            if (which == Utils.MX) {
                createAdditional(((MXRR) v.elementAt(i)).getHost(), name);

            } else if (which == Utils.NS) {
                createAdditional(((NSRR) v.elementAt(i)).getString(), name);
            }
        }

        header.rebuild();
    }

    public void addDNSKeys(String host){
        Vector v = null;
        try {
            v = zone.get(Utils.DNSKEY, host);
            addThem(v, host, Utils.DNSKEY);

            addRRSignature(Utils.DNSKEY, host, additional,
                Utils.ADDITIONAL);
        } catch (AssertionError AE1)
        {
            // FIXME
        }
    }

    private void addRRSignature(int type, String name,
        byte[] destination, int section) {
        Vector<DNSRRSIGRR> rrsigv = null;
        try {
            rrsigv = zone.get(Utils.RRSIG, zone.getName());
        } catch (AssertionError ex) {
            return;
        }

        for (int i = 0; i < rrsigv.size(); i++) {
            DNSRRSIGRR rrsig = rrsigv.elementAt(i);
            if (rrsig.getTypeCovered() == type) {
                byte add[] = rrsig.getBytes(name, minimum);
                switch(section)
                {
                    case Utils.ANSWER:
                        if (UDP && (buffer.length+add.length > maximumPayload))
                        {
                            header.setTC();
                            return;
                        }
                        buffer = Utils.combine(destination, add);
                        numAnswers++;
                        break;
                    case Utils.ADDITIONAL:
                        additional = Utils.combine(destination, add);
                        header.incrementNumAdditionals();
                        break;
                    case Utils.AUTHORITY:
                        authority = Utils.combine(destination, add);
                        header.incrementNumAuthorities();
                        break;
                }
            }
        }
    }

    private void addNSECRecords(String name)
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
        header.incrementNumAuthorities();
    }

    private void addAuthorities()
    {
        logger.trace(UDP);
        logger.trace(buffer.length);
        logger.trace(authority.length);

        if (!UDP ||(UDP && (buffer.length+authority.length < maximumPayload)))
        {
            logger.trace("adding in authorities");
            buffer = Utils.combine(buffer, authority);
        }
        else
        {
            logger.trace("NOT adding in authorities");
            header.clearNumAuthorities();
        }
    }

    private void addAdditionals()
    {
        if (savedNumAdditionals > 0)
        {
            additional = Utils.combine(additional, savedAdditional);
            header.setNumAdditionals (header.getNumAdditionals()
                + savedNumAdditionals);
        }

        if (header.getNumAdditionals() > 0)
        {
            if (!UDP || 
                (UDP && (buffer.length+additional.length < maximumPayload)))
            {
                buffer = Utils.combine(buffer, additional);
            }
            else
            {
                header.clearNumAdditionals();
            }
        }
    }

    private void addSOA(SOARR SOA)
    {
        authority = Utils.combine (authority, SOA.getBytes(zone.getName(),
            minimum));
        header.incrementNumAuthorities();
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
    private void dealWithOther(int type, String name)
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

            header.setRcode(Utils.NAMEERROR);
            return;
        }

        logger.debug(Utils.mapTypeToString(type) +
            " lookup of " + name + " failed but " +
            Utils.mapTypeToString(other) + " record found");

        header.setRcode(Utils.NOERROR);
    }

    // Keeping it DRY.
    private void errLookupFailed(int type, String name, int rcode)
    {
        logger.debug("'" + Utils.mapTypeToString(type) +
            "' lookup of " + name + " failed");
        header.setRcode(rcode);
    }


    private void setZone(JDNSS dnsService, String name)
    {
        try
        {
            zone = dnsService.getZone(name);
        }
        catch (AssertionError AE)
        {
            logger.debug("Zone lookup of " + name + " failed");
            header.setRcode(Utils.REFUSED);
            header.setNotAuthoritative();
            throw(AE);
            // return Arrays.copyOf(buffer, buffer.length);
        }
    }

    private void setMinimum()
    {
        Vector<SOARR> w = null;
        try
        {
            w = zone.get(Utils.SOA, zone.getName());
            SOA = w.elementAt(0);
            minimum = SOA.getMinimum();
        }
        catch (AssertionError AE)
        {
            logger.debug("SOA lookup in " + zone.getName() + " failed");
            header.setRcode(Utils.SERVFAIL);
            throw(AE);
        }
    }

    private void nameNotFound(int type, String name)
    {
        logger.debug(name + " not A or AAAA, giving up");
        errLookupFailed(type, name, Utils.NOERROR);
        addSOA(SOA);
        if (doDNSSEC) {
            addNSECRecords(name);
            addRRSignature(Utils.NSEC, name, authority, Utils.AUTHORITY);
        }
        addAuthorities();
    }

    private StringAndVector lookForCNAME(int type, String name)
    {
        logger.debug("Looking for a CNAME for " + name);

        try
        {
            Vector<RR> u = zone.get(Utils.CNAME, name);

            // grab the first one as they all should work. maybe we should
            // round-robin?
            String s = u.elementAt(0).getString();
            Assertion.aver(s != null);

            Vector<RR> v = zone.get(type, s);

            // yes, so first put in the CNAME
            createResponses(u, name, Utils.CNAME);

            // then continue the lookup on the original type
            // with the new name
            return new StringAndVector(s, v);
        }
        catch (AssertionError AE)
        {
            logger.debug("Didn't find a CNAME for " + name);

            // look for AAAA if A and vice versa
            dealWithOther(type, name);
            addSOA(SOA);
            if (doDNSSEC) {
                addNSECRecords(name);
                addRRSignature(Utils.NSEC, name, authority, Utils.AUTHORITY);
            }
            addAuthorities();
            throw (AE);
        }
    }

    /**
     * create a byte array that is a Response to a Query
     */
    public byte[] makeResponses(JDNSS dnsService, boolean UDP)
    {
        parseQueries();

        this.UDP = UDP;

        header.setResponse();
        header.setAuthoritative();
        header.setNoRecurse();

        // this assumes one zone per query as we only look at the first
        // name.
        try
        {
            setZone(dnsService, qnames[0]);
            setMinimum();
        }
        catch (AssertionError AE)
        {
            return Arrays.copyOf(buffer, buffer.length);
        }

        for (int i = 0; i < qnames.length; i++)
        {
            String name = qnames[i];
            int type = qtypes[i];

            logger.trace(name);
            logger.trace(type);

            Vector v = null;
            try
            {
                v = zone.get(type, name);

                if (doDNSSEC)
                {
                    addNSECRecords(name);
                    addRRSignature(Utils.NSEC, name, authority,
                        Utils.AUTHORITY);
                }
            }
            catch (AssertionError AE)
            {
                logger.debug("Didn't find: " + name);
                if (type != Utils.AAAA && type != Utils.A)
                {
                    nameNotFound(type, name);
                    return Arrays.copyOf(buffer, buffer.length);
                }
                else
                {
                    try
                    {
                        StringAndVector snv = lookForCNAME(type, name);
                        name = snv.getString();
                        v = snv.getVector();
                    }
                    catch (AssertionError AE2)
                    {
                        return Arrays.copyOf(buffer, buffer.length);
                    }
                }
            }

            if (header.getNumAdditionals() != 0 &&
                dnsService.getJdnssArgs().RFC2671)
            {
                logger.debug("Additionals not understood");
                header.setRcode(Utils.NOTIMPL);
                return Arrays.copyOf(buffer, buffer.length);
            }

            addDNSKeys(name);

            createResponses(v, name, type);
        }

        if (header.getNumAuthorities() > 0)
        {
            addAuthorities();
        }

        addAdditionals();
        header.rebuild();
        return Arrays.copyOf(buffer, buffer.length);
    }
}
