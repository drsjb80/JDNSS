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
    private byte[] authority = new byte[0];
    private int numAuthorities;
    private Zone zone;
    private int minimum;
    private boolean DNSSEC = false;
    private byte[] responses = new byte[0];
    private int maximumPayload = 512;
    private SOARR SOA;
    private boolean UDP;
    private final Query query;
    boolean refuseFlag = false;

    public Response(Query query, final boolean UDP) {
        this.query = query;
        this.header = query.getHeader();
        this.UDP = UDP;
        header.setQR(true);
        header.setAA(true);
        header.setRA(false);

        for (Queries q : query.getQueries()) {
            Vector<RR> v;
            String name = q.getName();
            final RRCode type = q.getType();

            logger.trace(DNSSEC);
            logger.trace(name);
            logger.trace(type.toString());
            logger.trace(UDP);

            try {
                setZone(name);
                logger.trace(zone);
                setMinimum();
                logger.trace(minimum);
            } catch (AssertionError AE) {
                logger.catching(AE);
                logger.trace("invalid zone, refusing!.");
                refuseFlag = true;
            }
            finally {
                logger.trace(refuseFlag);
                if (refuseFlag == false) {
                    try {
                        if(query.getOptrr() != null) {
                            DNSSEC = query.getOptrr().isDNSSEC();
                            maximumPayload = query.getOptrr().getPayloadSize();
                        }
                        Map<String, Vector> stringAndVector = findRR(type, name);
                        Assertion.aver(stringAndVector.size() == 1);
                        name = ((String) stringAndVector.keySet().toArray()[0]);
                        v = stringAndVector.get(name);
                        Assertion.aver(zone != null, "zone == null");
                        Assertion.aver(v != null, "v == null");
                        Assertion.aver(name != null, "name == null");
                        logger.traceEntry(new ObjectMessage(v));
                        logger.traceEntry(name);
                        logger.traceEntry(type.toString());

                        boolean firsttime = true;

                        for (RR rr : v) {
                            byte add[] = rr.getBytes(name, minimum);
                            // will we be too big and need to switch to TCP?
                            if (UDP && responses != null && (responses.length + add.length > maximumPayload)) {
                                header.setTC(true);
                            }

                            responses = Utils.combine(responses, add);
                            header.setNumAnswers(header.getNumAnswers() + 1);

                            //Add RRSIG Records Corresponding to Type
                            //seems right to add answers somewhere close but we only want to do it once on last
                            //TODO Check the stuff to assure its doing what I want it to
                            if((v.indexOf(rr) + 1 == v.size()) && DNSSEC){
                                addRRSignature(rr.getType(), name, responses, ResponseSection.ANSWER);
                            }

                            if (firsttime &&
                                    type != RRCode.NS &&
                                    type != RRCode.DNSKEY) {
                                createAuthorities(name);
                                firsttime = false;
                            }

                            if (type == RRCode.MX) {
                                createAorAAAA(rr.getHost(), name);
                            }
                            if (type == RRCode.NS) {
                                createAorAAAA(rr.getString(), name);
                            }
                            if(DNSSEC && type == RRCode.SOA) {
                                Vector<RR> dnsKeyVector = zone.get(RRCode.DNSKEY, name);
                                createAdditionals(dnsKeyVector, name);
                            }
                        }
                        logger.traceExit();
                    } catch (AssertionError AE2) {

                        logger.catching(AE2);
                        logger.trace("unable to respond, name not found.");
                        authority = Utils.combine(authority, SOA.getBytes(zone.getName(), minimum));
                        numAuthorities = 1;
                        if (DNSSEC) {
                            numAuthorities++;
                            addRRSignature(RRCode.SOA, zone.getName(), authority, ResponseSection.AUTHORITY);
                            addNSECRecords(zone.getName());
                            logger.trace("hit");
                            addRRSignature(RRCode.NSEC, zone.getName(), authority, ResponseSection.AUTHORITY);
                            logger.trace("hit");
                        }
                    }
                    addAuthorities();
                    addAdditionals();
                }
            }
        }
        if (query.getOptrr() != null && header.getNumAdditionals() > 1)
            header.setNumAdditionals(header.getNumAdditionals() + 1);
        header.build();
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

    private void addAuthorities() {
        logger.traceEntry();
        logger.trace(numAuthorities);
        logger.trace((responses.length + authority.length));
        logger.trace(maximumPayload);
        if (numAuthorities > 0) {
            if (!UDP || responses.length + authority.length < maximumPayload) {
                responses = Utils.combine(responses, authority);
                header.setNumAuthorities(numAuthorities);
            }
            else if(responses.length + authority.length >= maximumPayload){
                header.setTC(true);
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
            else if(responses.length + authority.length >= maximumPayload){
                header.setTC(true);
            }
        }
    }

    /**
     * Given a zone and an MX or NS hostname, see if there is an A or AAAA
     * record we can also send back...
     */
    private void createAorAAAA(String host, String name) {
        logger.traceEntry();
        Assertion.aver(host != null);
        Assertion.aver(name != null);
        Vector<RR> v;

        try {
            v = zone.get(RRCode.A, host);
            createAdditionals(v, host);

            if (DNSSEC) {
                addRRSignature(RRCode.A, name, additional, ResponseSection.ADDITIONAL);
            }
        } catch (AssertionError AE) {
            // maybe there is an AAAA
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

    // put the possible additionals in, but don't add to response until we know there is room for them.
    private void createAdditionals(Vector<RR> v, String host) {
        logger.traceEntry();
        Assertion.aver(v != null, "v == null");
        Assertion.aver(host != null, "host == null");
        RRCode type = v.get(0).getType();

        for (int i = 0; i < v.size(); i++) {
            RR rr = v.elementAt(i);
            additional = Utils.combine(additional, rr.getBytes(host, minimum));
            numAdditionals++;
        }

        if(DNSSEC) {
            addRRSignature(type,  host, additional, ResponseSection.ADDITIONAL);
        }
    }

    // put the possible authorities in, but don't add to response until we know there is room for them.
    private void createAuthorities(String name) {
        logger.traceEntry(name);
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


    private void addRRSignature(final RRCode type, final String name, byte[] destination, ResponseSection section) {
        logger.traceEntry(name);
        Vector<RR> rrsigv = zone.get(RRCode.RRSIG, name);
       // Assertion.aver(rrsigv != null);
        for (RR foo : rrsigv) {
            RRSIG rrsig = (RRSIG) foo;
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
                    case AUTHORITY:
                        if (UDP && (responses.length + add.length > maximumPayload)) {
                            header.setTC(true);
                        }
                        authority = Utils.combine(destination, add);
                        numAuthorities++;
                        break;
                    case ADDITIONAL:
                        if (UDP && (responses.length + add.length > maximumPayload)) {
                            //if bigger then max payload exit without adding RRSIG
                        } else {
                            additional = Utils.combine(destination, add);
                            numAdditionals++;
                            break;
                        }
                }
            }
        }
    }

    private void addNSECRecords(final String name) {
        logger.traceEntry();
        Vector<RR> nsecv = zone.get(RRCode.NSEC, zone.getName());

        NSECRR nsec = (NSECRR) nsecv.get(0);
        byte add[] = nsec.getBytes(name, minimum);
        authority = Utils.combine(authority, add);
        numAuthorities++;
    }

    private Map<String, Vector> findRR(final RRCode type, String name) {
        logger.traceEntry();
        Vector v;
        try {
            v = zone.get(type, name);

            Map<String, Vector> stringAndVector = new ConcurrentHashMap<>();
            stringAndVector.put(name, v);
            logger.traceExit();
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

    private void nameNotFound(final RRCode type, final String name) {
        logger.traceEntry();
        if(DNSSEC) {
            throw  new AssertionError();
        }
        switch (type){
            case MX:
                logger.debug("'" + type.toString() + "' lookup of " + name + " failed");
                header.setRcode(ErrorCodes.NOERROR.getCode());
                break;
            default:logger.debug("'" + type.toString() + "' lookup of " + name + " failed");
                header.setRcode(ErrorCodes.NAMEERROR.getCode());
                break;
        }
    }

    private Map<String, Vector> lookForCNAME(final RRCode type, final String name) {
        logger.traceEntry();
        logger.debug("Looking for a CNAME for " + name);

        try {
            Vector<RR> u = zone.get(RRCode.CNAME, name);
            String s = u.elementAt(0).getString();
            Assertion.aver(s != null);

            Vector<RR> v = zone.get(type, s);
            responses = Utils.combine(responses, u.get(0).getBytes(name, minimum));
            header.setNumAnswers(header.getNumAnswers() + 1);

            Assertion.aver(v != null) ;
            Map<String, Vector> stringAndVector = new ConcurrentHashMap<>();
            stringAndVector.put(s, v);
            return stringAndVector;

        } catch (AssertionError AE) {
            logger.debug("Didn't find a CNAME for " + name);

            dealWithOther(type, name);
        }
        //Should have already returned or errored by this point
        return null;
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
        logger.traceEntry();
        RRCode other = type == RRCode.A ? RRCode.AAAA : RRCode.A;
        Vector<RR> v;
        try {
            v = zone.get(other, name);
        } catch (AssertionError AE) {
            logger.debug(type.toString() + " lookup of " +
                    name + " failed");
            //TODO I think we should be adding NSEC RR to prove that the record does not exist
            header.setRcode(ErrorCodes.NAMEERROR.getCode());
            throw (AE);
        }
        if(DNSSEC) {
            addNSECRecords(name);
        }
        throw (new AssertionError("lookup other failed"));
    }

    protected byte[] getBytes(){
        logger.traceEntry();
        byte abc[] = new byte[0];
        abc = Utils.combine(abc, header.getHeader());
        abc = Utils.combine(abc, query.buildResponseQueries());
        abc = Utils.combine(abc, responses);

        if(query.getOptrr()!= null)
            abc = Utils.combine(abc, query.getOptrr().getBytes());

        return abc;
    }

}
