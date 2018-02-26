package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.Vector;

enum ResponseSection {
    ANSWER, ADDITIONAL, AUTHORITY
}

class Response {
    private final Logger logger = JDNSS.logger;

    private final Header header;
    private byte[] additional;
    private int numAdditionals;
    private byte[] authority;
    private int numAuthorities;
    private Zone zone;
    private int minimum;
    private final boolean DNSSEC = false;
    private byte[] responses;
    private final int maximumPayload = 512;
    private SOARR SOA;
    private boolean UDP = false;
    private final Query query;

    public Response(Query query) {
        this.query = query;
        this.header = query.getHeader(); // pass these in
        // this.responses = query.getBuffer();
        // logger.debug(this.responses);
    }

    // put the possible additionals in, but don't add to response until we know there is room for them.
    private void createAdditionals(Vector<RR> v, String host) {
        logger.traceEntry();
        Assertion.aver(v != null, "v == null");
        Assertion.aver(host != null, "host == null");

        for (int i = 0; i < v.size(); i++) {
            RR rr = v.elementAt(i);
            additional = Utils.combine(additional, rr.getBytes(host, minimum));
            numAdditionals++;
        }
    }

    /**
     * Given a zone and an MX or NS hostname, see if there is an A or AAAA
     * record we can also send back...
     */
    private void createAorAAAA(String host, String name) {
        Assertion.aver(host != null);
        Assertion.aver(name != null);
        Vector<RR> v;

        try {
            v = zone.get(Utils.A, host);
            createAdditionals(v, host);

            if (DNSSEC) {
                addRRSignature(Utils.A, name, additional, ResponseSection.ADDITIONAL);
            }
        } catch (AssertionError AE1) {
            // try the AAAA
        }

        try {
            v = zone.get(Utils.AAAA, host);
            createAdditionals(v, host);

            if (DNSSEC) {
                addRRSignature(Utils.AAAA, name, additional, ResponseSection.ADDITIONAL);
            }
        } catch (AssertionError AE2) {
            // maybe we found an A
        }
    }

    // put the possible authorities in, but don't add to response until we know there is room for them.
    private void createAuthorities(String name) {
        Vector<RR> v = zone.get(Utils.NS, zone.getName());
        logger.trace(v);

        for (RR nsrr : v) {
            logger.trace(nsrr);
            authority = Utils.combine(authority, nsrr.getBytes(nsrr.getName(), minimum));
            numAuthorities++;
            createAorAAAA(nsrr.getString(), name);
        }

        if (DNSSEC) {
            addRRSignature(Utils.NS, name, authority, ResponseSection.AUTHORITY);
        }
    }

    private void createResponses(Vector<RR> v, String name, int which) {
        Assertion.aver(zone != null, "zone == null");
        Assertion.aver(v != null, "v == null");
        Assertion.aver(name != null, "name == null");

        logger.traceEntry(new ObjectMessage(v));
        logger.traceEntry(name);
        logger.traceEntry(Integer.toString(which));

        boolean firsttime = true;

        for (RR rr : v) {
            byte add[] = rr.getBytes(name, minimum);

            // will we be too big and need to switch to TCP?
            if (UDP && responses != null && (responses.length + add.length > maximumPayload)) {
                header.setTC(true);
                return;
            }

            responses = Utils.combine(responses, add);
            header.setNumAnswers(header.getNumAnswers() + 1);

            //Add RRSIG Records Corresponding to Type
            if (DNSSEC) {
                addRRSignature(rr.getType(), name, responses, ResponseSection.ANSWER);
            }

            if (firsttime && which != Utils.NS) {
                createAuthorities(name);
            }

            firsttime = false;

            if (which == Utils.MX || which == Utils.NS) {
                createAorAAAA(rr.getHost(), name);
            }
        }
        logger.traceExit();
    }

    public void addDNSKeys(String host) {
        Vector v = zone.get(Utils.DNSKEY, host);
        createAdditionals(v, host);

        addRRSignature(Utils.DNSKEY, host, additional, ResponseSection.ADDITIONAL);
    }


    private void addRRSignature(int type, String name, byte[] destination, ResponseSection section) {
        Vector<RR> rrsigv = zone.get(Utils.RRSIG, zone.getName());

        for (RR foo : rrsigv) {
            DNSRRSIGRR rrsig = (DNSRRSIGRR) foo;
            // DNSRRSIGRR rrsig = rrsigv.elementAt(i);
            if (rrsig.getTypeCovered() == type) {
                byte add[] = rrsig.getBytes(name, minimum);
                switch (section) {
                    case ANSWER:
                        if (UDP && (responses.length + add.length > maximumPayload)) {
                            header.setTC(true);
                            return;
                        }
                        responses = Utils.combine(destination, add);
                        header.setNumAnswers(header.getNumAnswers() + 1);
                        break;
                    case ADDITIONAL:
                        additional = Utils.combine(destination, add);
                        header.setNumAdditionals(header.getNumAdditionals() + 1);
                        break;
                    case AUTHORITY:
                        authority = Utils.combine(destination, add);
                        header.setNumAuthorities(header.getNumAuthorities() + 1);
                        break;
                }
            }
        }
    }

    private void addNSECRecords(String name) {
        Vector<RR> nsecv = zone.get(Utils.NSEC, zone.getName());

        DNSNSECRR nsec = (DNSNSECRR) nsecv.get(0);
        byte add[] = nsec.getBytes(name, minimum);
        authority = Utils.combine(authority, add);
        header.setNumAuthorities(header.getNumAuthorities() + 1);
    }

    private void addAuthorities() {
        if (numAuthorities > 0) {
            if (!UDP || responses.length + authority.length < maximumPayload) {
                responses = Utils.combine(responses, authority);
                header.setNumAuthorities(numAuthorities);
            }
        }
    }

    // DRY with above?
    private void addAdditionals() {
        logger.traceEntry();
        logger.trace(numAdditionals);
        if (numAdditionals > 0) {
            if (!UDP || responses.length + additional.length < maximumPayload) {
                responses = Utils.combine(responses, additional);
                header.setNumAdditionals(numAdditionals);
            }
        }
    }

    private void addSOA(SOARR SOA) {
        authority = Utils.combine(authority, SOA.getBytes(zone.getName(), minimum));
        header.setNumAuthorities(header.getNumAuthorities() + 1);
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
    private void dealWithOther(int type, String name) {
        int other = type == Utils.A ? Utils.AAAA : Utils.A;

        try {
            zone.get(other, name);
        } catch (AssertionError AE) {
            logger.debug(Utils.mapTypeToString(type) + " lookup of " +
                    name + " failed");

            header.setRcode(ErrorCodes.NAMEERROR.getCode());
            throw (AE);
        }

        logger.debug(Utils.mapTypeToString(type) +
                " lookup of " + name + " failed but " +
                Utils.mapTypeToString(other) + " record found");

        header.setRcode(ErrorCodes.NOERROR.getCode());
    }

    // Just keeping it DRY.
    private void errLookupFailed(int type, String name, int rcode) {
        logger.debug("'" + Utils.mapTypeToString(type) + "' lookup of " + name + " failed");
        header.setRcode(rcode);
    }

    private void setZone(String name) {
        try {
            zone = JDNSS.getZone(name);
        } catch (AssertionError AE) {
            logger.debug("Zone lookup of " + name + " failed");
            header.setRcode(ErrorCodes.REFUSED.getCode());
            header.setAA(false);
            throw (AE);
            // return Arrays.copyOf(responses, responses.length);
        }
    }

    private void setMinimum() {
        Vector<RR> w;
        try {
            w = zone.get(Utils.SOA, zone.getName());
            SOA = (SOARR) w.elementAt(0);
            minimum = SOA.getMinimum();
        } catch (AssertionError AE) {
            logger.debug("SOA lookup in " + zone.getName() + " failed");
            header.setRcode(ErrorCodes.SERVFAIL.getCode());
            throw (AE);
        }
    }

    private void nameNotFound(int type, String name) {
        logger.debug(name + " not A or AAAA, giving up");
        errLookupFailed(type, name, ErrorCodes.NOERROR.getCode());
        addSOA(SOA);

        if (DNSSEC) {
            addNSECRecords(name);
            addRRSignature(Utils.NSEC, name, authority, ResponseSection.AUTHORITY);
        }

        addAuthorities();
    }

    private StringAndVector lookForCNAME(int type, String name) {
        logger.debug("Looking for a CNAME for " + name);

        try {
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
        } catch (AssertionError AE) {
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

    private StringAndVector findRR(int type, String name) {
        Vector v;
        try {
            v = zone.get(type, name);

            // is this where this belongs?
            if (DNSSEC) {
                addNSECRecords(name);
                addRRSignature(Utils.NSEC, name, authority, ResponseSection.AUTHORITY);
            }

            return new StringAndVector(name, v);
        } catch (AssertionError AE) {
            logger.debug("Didn't find: " + name);

            if (type != Utils.AAAA && type != Utils.A) {
                nameNotFound(type, name);
                throw (AE);
            } else {
                return lookForCNAME(type, name);
            }
        }
    }

    /**
     * create a byte array that is a Response to a Query
     */
    public byte[] makeResponses(boolean UDP) {
        this.UDP = UDP;

        header.setQR(true);
        header.setAA(true);
        header.setRA(false);

        for (Queries q : query.getQueries()) {
            String name = q.getName();
            int type = q.getType();

            logger.trace(name);
            logger.trace(Utils.mapTypeToString(type));

            try {
                setZone(name);
                logger.trace(zone);
                setMinimum();
                logger.trace(minimum);
            } catch (AssertionError AE) {
                logger.catching(AE);
                return query.getBuffer();
            }

            Vector v;
            try {
                StringAndVector snv = findRR(type, name);
                name = snv.getString();
                v = snv.getVector();
            } catch (AssertionError AE2) {
                return query.getBuffer();
            }

            // addDNSKeys(name);

            createResponses(v, name, type);
        }

        addAuthorities();
        addAdditionals();
        header.build();

        return Utils.combine(Utils.combine(header.getHeader(), query.getRawQueries()), responses);
    }
}
