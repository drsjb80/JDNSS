package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.Vector;
import java.util.Arrays;
import java.net.DatagramPacket;

public class Response
{
    private Logger logger = JDNSS.getLogger();

    private Header header;
    private byte[] additional = new byte[0];
    private byte[] authority = new byte[0];
    private Zone zone;
    private int minimum;
    private boolean DNSSEC = false;
    private byte[] buffer;
    private int maximumPayload = 512;
    private int numAnswers;
    private int numAuthorities;
    private byte[] savedAdditional;
    private int savedNumAdditionals;
    private SOARR SOA;
    private boolean UDP = false;
    private Query query;

    public Response (Query query)
    {
        this.query = query;
        this.header = query.getHeader();
        this.zone = query.getZone();
        this.buffer = query.getBuffer();
        this.numAnswers = header.getNumAnswers();
        this.numAuthorities = header.getNumAuthorities();
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

            if (DNSSEC)
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

            if (DNSSEC)
            {
                addRRSignature(Utils.AAAA, name, additional, Utils.ADDITIONAL);
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

        for (NSRR nsrr: v)
        {
            logger.trace(nsrr);
            authority = Utils.combine(authority, nsrr.getBytes(nsrr.getName(),
                minimum));
            header.incrementNumAuthorities();

            createAdditional(nsrr.getString(), name);
        }

        if (DNSSEC)
        {
            addRRSignature(Utils.NS, name, authority, Utils.AUTHORITY);
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

        boolean firsttime = true;

        for (RR rr: v)
        {
            byte add[] = rr.getBytes(name, minimum);

            // will we be too big and need to switch to TCP?
            if (UDP && (buffer.length + add.length > maximumPayload))
            {
                header.setTC();
                return;
            }

            buffer = Utils.combine(buffer, add);
            numAnswers++;

            //Add RRSIG Records Corresponding to Type
            if (DNSSEC)
            {
                addRRSignature(rr.getType(), name, buffer, Utils.ANSWER);
            }

            if (firsttime && which != Utils.NS)
            {
                createAuthorities(name);
            }

            firsttime = false;

            if (which == Utils.MX)
            {
                createAdditional(((MXRR) rr).getHost(), name);

            }
            else if (which == Utils.NS)
            {
                createAdditional(((NSRR) rr).getString(), name);
            }
        }
    }

    public void addDNSKeys(String host)
    {
        Vector v = zone.get(Utils.DNSKEY, host);
        addThem(v, host, Utils.DNSKEY);

        addRRSignature(Utils.DNSKEY, host, additional, Utils.ADDITIONAL);
    }

    private void addRRSignature(int type, String name,
        byte[] destination, int section)
    {
        Vector<DNSRRSIGRR> rrsigv = zone.get(Utils.RRSIG, zone.getName());

        for (DNSRRSIGRR rrsig: rrsigv)
        // for (int i = 0; i < rrsigv.size(); i++)
        {
            // DNSRRSIGRR rrsig = rrsigv.elementAt(i);
            if (rrsig.getTypeCovered() == type)
            {
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
        Vector<RR> nsecv = zone.get(Utils.NSEC, zone.getName());

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
            throw(AE);
        }

        logger.debug(Utils.mapTypeToString(type) +
            " lookup of " + name + " failed but " +
            Utils.mapTypeToString(other) + " record found");

        header.setRcode(Utils.NOERROR);
    }

    // Just keeping it DRY.
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

        if (DNSSEC)
        {
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

            // no CNAME, but maybe we can look for A <=> AAAA and return no
            // answers, but a NOERROR rcode.
            dealWithOther(type, name);

            /* FIXME -- find out what needs to be added.
            catch (AssertionError AE2)
            {
                if (DNSSEC)
                {
                    addNSECRecords(name);
                    addRRSignature(Utils.NSEC, name, authority, Utils.AUTHORITY);
                }
            }
            addSOA(SOA);
            addAuthorities();
            */
        }

        // should have already returned something good or throw an
        // exception from dealWithOther.
        return null;
    }

    private StringAndVector findRR(int type, String name)
    {
        Vector v = null;
        try
        {
            v = zone.get(type, name);

            // is this where this belongs?
            if (DNSSEC)
            {
                addNSECRecords(name);
                addRRSignature(Utils.NSEC, name, authority, Utils.AUTHORITY);
            }

            return new StringAndVector(name, v);
        }
        catch (AssertionError AE)
        {
            logger.debug("Didn't find: " + name);

            if (type != Utils.AAAA && type != Utils.A)
            {
                nameNotFound(type, name);
                throw(AE);
            }
            else
            {
                return lookForCNAME(type, name);
            }
        }
    }

    /**
     * create a byte array that is a Response to a Query
     */
    public byte[] makeResponses(JDNSS dnsService, boolean UDP)
    {
        this.UDP = UDP;

        header.setResponse();
        header.setAuthoritative();
        header.setNoRecurse();

        for (Queries q: query.getQueries())
        // for (int i = 0; i < qnames.length; i++)
        {
            String name = q.getName();
            int type = q.getType();

            try
            {
                setZone(dnsService, name);
                setMinimum();
            }
            catch (AssertionError AE)
            {
                return Arrays.copyOf(buffer, buffer.length);
            }

            logger.trace(name);
            logger.trace(type);

            Vector v = null;
            try
            {
                StringAndVector snv = findRR(type, name);
                name = snv.getString();
                v = snv.getVector();
            }
            catch (AssertionError AE2)
            {
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
