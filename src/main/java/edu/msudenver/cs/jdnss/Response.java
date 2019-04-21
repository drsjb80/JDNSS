package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;

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

    Response(Query query, final boolean UDP) {
        this.query = query;
        this.header = query.getHeader();
        this.UDP = UDP;
        header.setQR(true);
        header.setAA(true);
        header.setRA(false);

        for (Queries q : query.getQueries()) {
            String name = q.getName();
            final RRCode type = q.getType();
            ArrayList<RR> v = new ArrayList<>();

            try {
                setZone(name);
                setMinimum();
            } catch (AssertionError AE) {
                logger.catching(AE);
                break;
            }

            if(query.getOptrr() != null) {
                DNSSEC = query.getOptrr().isDNSSEC();
                maximumPayload = query.getOptrr().getPayloadSize();
            }

            try {
                Map.Entry<String, ArrayList<RR>> stringAndArrayList = findRR(type, name);
                name = stringAndArrayList.getKey();
                v = stringAndArrayList.getValue();
            } catch (AssertionError AE2) {
                logger.catching(AE2);
                authority = Utils.combine(authority, SOA.getBytes(zone.getName(), minimum));
                numAuthorities = 1;
                if (DNSSEC) {
                    numAuthorities++;
                    addRRSignature(RRCode.SOA, zone.getName(), authority, ResponseSection.AUTHORITY);
                    addNSECRecords(zone.getName());
                    addRRSignature(RRCode.NSEC, zone.getName(), authority, ResponseSection.AUTHORITY);
                }
            }

            boolean firstTime = true;
            for (RR rr : v) {
                doOneRR(UDP, name, type, v, firstTime, rr);
                firstTime = false;
            }

            addAuthorities();
            addAdditionals();
        }

        if (query.getOptrr() != null && header.getNumAdditionals() > 1)
            header.setNumAdditionals(header.getNumAdditionals() + 1);
        header.build();
    }

    private void doOneRR(boolean UDP, String name, RRCode type, ArrayList<RR> v, boolean firstTime, RR rr) {
        byte add[] = rr.getBytes(name, minimum);
        // will we be too big and need to switch to TCP?
        if (UDP && responses != null && (responses.length + add.length > maximumPayload)) {
            header.setTC(true);
        }

        responses = Utils.combine(responses, add);
        header.setNumAnswers(header.getNumAnswers() + 1); // incrementNumAnswers

        //Add RRSIG Records Corresponding to Type
        //seems right to add answers somewhere close but we only want to do it once on last
        //TODO Check the stuff to assure its doing what I want it to
        if((v.indexOf(rr) + 1 == v.size()) && DNSSEC){
            addRRSignature(rr.getType(), name, responses, ResponseSection.ANSWER);
        }

        if (firstTime && type != RRCode.NS && type != RRCode.DNSKEY) {
            createAuthorities(name);
        }

        if (type == RRCode.MX) {
            createAorAAAA(rr.getHost(), name);
        }

        if (type == RRCode.NS) {
            createAorAAAA(rr.getString(), name);
        }
        if(DNSSEC && type == RRCode.SOA) {
            ArrayList<RR> dnsKeyArrayList = zone.get(RRCode.DNSKEY, name);
            createAdditionals(dnsKeyArrayList, name);
        }
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
        ArrayList<RR> w;
        try {
            w = zone.get(RRCode.SOA, zone.getName());
            SOA = (SOARR) w.get(0);
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

    private void addAdditionals() {
        logger.traceEntry();
        logger.trace(numAdditionals);
        if (numAdditionals > 0) {
            if (!UDP || responses.length + additional.length < maximumPayload) {
                responses = Utils.combine(responses, additional);
                header.setNumAdditionals(numAdditionals);
            }
            else if(responses.length + additional.length >= maximumPayload){
                header.setTC(true);
            }
        }
    }

    /**
     * Given a zone and an MX or NS hostname, see if there is an A or AAAA
     * record we can also send back...
     */
    private void createAorAAAA(final String host, final String name) {
        logger.traceEntry();
        Assertion.aver(host != null);
        Assertion.aver(name != null);

        for (RRCode rrCode: Arrays.asList(RRCode.A, RRCode.AAAA)) {
            try {
                ArrayList<RR> v = zone.get(rrCode, host);
                createAdditionals(v, host);

            /*
            if (DNSSEC) {
                addRRSignature(RRCode.A, name, additional, ResponseSection.ADDITIONAL);
            }
            */
            } catch (AssertionError AE) {
            }
        }
    }

    // put the possible additionals in, but don't add to response until we know there is room for them.
    private void createAdditionals(ArrayList<RR> v, String host) {
        logger.traceEntry();
        RRCode type = v.get(0).getType();

        for (RR rr : v) {
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
        ArrayList<RR> v = zone.get(RRCode.NS, zone.getName());
        logger.trace(v);

        for (RR nsrr : v) {
            authority = Utils.combine(authority, nsrr.getBytes(nsrr.getName(), minimum));
            numAuthorities++;
            createAorAAAA(nsrr.getString(), name);
        }

        if (DNSSEC) {
            addRRSignature(RRCode.NS, zone.getName(), authority, ResponseSection.AUTHORITY);
        }
    }


    private void addRRSignature(final RRCode type, final String name, byte[] destination, ResponseSection section) {
        logger.traceEntry(name);
        ArrayList<RR> rrsigv = zone.get(RRCode.RRSIG, name);
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
                            return;
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
        ArrayList<RR> nsecv = zone.get(RRCode.NSEC, zone.getName());

        NSECRR nsec = (NSECRR) nsecv.get(0);
        byte add[] = nsec.getBytes(name, minimum);
        authority = Utils.combine(authority, add);
        numAuthorities++;
    }

    private Map.Entry<String, ArrayList<RR>> findRR(final RRCode type, final String name) {
        logger.traceEntry();
        try {
            return Map.entry(name, zone.get(type, name));
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

    private Map.Entry<String, ArrayList<RR>> lookForCNAME(final RRCode type, final String name) {
        logger.traceEntry();
        logger.debug("Looking for a CNAME for " + name);

        try {
            ArrayList<RR> u = zone.get(RRCode.CNAME, name);
            String s = u.get(0).getString();
            Assertion.aver(s != null);

            ArrayList<RR> v = zone.get(type, s);
            responses = Utils.combine(responses, u.get(0).getBytes(name, minimum));
            header.setNumAnswers(header.getNumAnswers() + 1);

            Assertion.aver(v != null) ;
            return Map.entry(s, v);
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
        ArrayList<RR> v;
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
