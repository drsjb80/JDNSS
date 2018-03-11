package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

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
        this.header = query.getHeader();
        //this.numAdditionals = query.getHeader().getNumAdditionals();
        // pass these in
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
            v = zone.get(RRCode.A, host);
            createAdditionals(v, host);

            if (DNSSEC) {
                addRRSignature(RRCode.A, name, additional, ResponseSection.ADDITIONAL);
            }
        } catch (AssertionError AE1) {
            // try the AAAA
        }

        try {
            v = zone.get(RRCode.AAAA, host);
            createAdditionals(v, host);

            if (DNSSEC) {
                addRRSignature(RRCode.AAAA, name, additional, ResponseSection.ADDITIONAL);
            }
        } catch (AssertionError AE2) {
            // maybe we found an A
        }
    }

    // put the possible authorities in, but don't add to response until we know there is room for them.
    private void createAuthorities(String name) {
        Vector<RR> v = zone.get(RRCode.NS, zone.getName());
        logger.trace(v);

        for (RR nsrr : v) {
            logger.trace(nsrr);
            authority = Utils.combine(authority, nsrr.getBytes(nsrr.getName(), minimum));
            numAuthorities++;
            createAorAAAA(nsrr.getString(), name);
        }

        if (DNSSEC) {
            addRRSignature(RRCode.NS, name, authority, ResponseSection.AUTHORITY);
        }
    }

    private void createResponses(Vector<RR> v, String name, final RRCode which) {
        Assertion.aver(zone != null, "zone == null");
        Assertion.aver(v != null, "v == null");
        Assertion.aver(name != null, "name == null");

        logger.traceEntry(new ObjectMessage(v));
        logger.traceEntry(name);
        logger.traceEntry(which.toString());

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

            if (firsttime && which != RRCode.NS) {
                createAuthorities(name);
            }

            firsttime = false;

            if (which == RRCode.MX) {
                createAorAAAA(rr.getHost(), name);
            }

            if (which == RRCode.NS) {
                createAorAAAA(rr.getString(), name);
            }
        }
        logger.traceExit();
    }

    public void addDNSKeys(final String host) {
        Vector v = zone.get(RRCode.DNSKEY, host);
        createAdditionals(v, host);

        addRRSignature(RRCode.DNSKEY, host, additional, ResponseSection.ADDITIONAL);
    }


    private void addRRSignature(final RRCode type, final String name, byte[] destination, ResponseSection section) {
        Vector<RR> rrsigv = zone.get(RRCode.RRSIG, zone.getName());

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

    private void addNSECRecords(final String name) {
        Vector<RR> nsecv = zone.get(RRCode.NSEC, zone.getName());

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
    private void dealWithOther(final RRCode type, final String name) {
        RRCode other = type == RRCode.A ? RRCode.AAAA : RRCode.A;

        try {
            zone.get(other, name);
        } catch (AssertionError AE) {
            logger.debug(type.toString() + " lookup of " +
                    name + " failed");

            header.setRcode(ErrorCodes.NAMEERROR.getCode());
            throw (AE);
        }

        logger.debug(type.toString() +
                " lookup of " + name + " failed but " +
                other.toString() + " record found");

        header.setRcode(ErrorCodes.NOERROR.getCode());
    }

    // Just keeping it DRY.
    private void errLookupFailed(final RRCode type, final String name, final int rcode) {
        logger.debug("'" + type.toString() + "' lookup of " + name + " failed");
        // FIXME
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
            w = zone.get(RRCode.SOA, zone.getName());
            SOA = (SOARR) w.elementAt(0);
            minimum = SOA.getMinimum();
        } catch (AssertionError AE) {
            logger.debug("SOA lookup in " + zone.getName() + " failed");
            header.setRcode(ErrorCodes.SERVFAIL.getCode());
            throw (AE);
        }
    }

    private void nameNotFound(final RRCode type, final String name) {
        logger.debug(name + " not A or AAAA, giving up");
        errLookupFailed(type, name, ErrorCodes.NOERROR.getCode());
        addSOA(SOA);

        if (DNSSEC) {
            addNSECRecords(name);
            addRRSignature(RRCode.NSEC, name, authority, ResponseSection.AUTHORITY);
        }

        addAuthorities();
    }

    private Map<String, Vector> lookForCNAME(final RRCode type, final String name) {
        logger.debug("Looking for a CNAME for " + name);

        try {
            Vector<RR> u = zone.get(RRCode.CNAME, name);

            // grab the first one as they all should work. maybe we should
            // round-robin?
            String s = u.elementAt(0).getString();
            Assertion.aver(s != null);

            Vector<RR> v = zone.get(type, s);

            // yes, so first put in the CNAME
            createResponses(u, name, RRCode.CNAME);

            // then continue the lookup on the original type
            // with the new name
            Map<String, Vector> stringAndVector = new ConcurrentHashMap<>();
            stringAndVector.put(s, v);
            return stringAndVector;

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
                    addRRSignature(RRCode.NSEC, name, authority, Utils.AUTHORITY);
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

    private Map<String, Vector> findRR(final RRCode type, String name) {
        Vector v;
        try {
            v = zone.get(type, name);

            // is this where this belongs?
            if (DNSSEC) {
                addNSECRecords(name);
                addRRSignature(RRCode.NSEC, name, authority, ResponseSection.AUTHORITY);
            }

            Map<String, Vector> stringAndVector = new ConcurrentHashMap<>();
            stringAndVector.put(name, v);
            return stringAndVector;
        } catch (AssertionError AE) {
            logger.debug("Didn't find: " + name);

            if (type != RRCode.AAAA && type != RRCode.A) {
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
            final RRCode type = q.getType();

            logger.trace(name);
            logger.trace(type.toString());

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
                Map<String, Vector> stringAndVector = findRR(type, name);
                Assertion.aver(stringAndVector.size() == 1);
                name = ((String) stringAndVector.keySet().toArray()[0]);
                v = stringAndVector.get(name);
            } catch (AssertionError AE2) {
                return query.getBuffer();
            }

            // addDNSKeys(name);

            createResponses(v, name, type);
        }

        addAuthorities();
        addAdditionals();
        header.build();
        logger.trace(header.getNumAnswers());
        logger.trace(header.getNumAdditionals());

        byte abc[] = new byte[0];
        abc = Utils.combine(abc, header.getHeader());
        abc = Utils.combine(abc, query.buildResponseQueries());
        abc = Utils.combine(abc, responses);

        if(query.getOptrr()!= null)
            abc = Utils.combine(abc, query.getOptrr().getBytes());

        return abc;

    }
}
