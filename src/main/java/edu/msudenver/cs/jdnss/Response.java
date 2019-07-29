package edu.msudenver.cs.jdnss;

import lombok.NonNull;
import lombok.ToString;
import org.apache.logging.log4j.Logger;

import java.util.*;

enum ResponseSection {
    ANSWER, ADDITIONAL, AUTHORITY
}

@ToString
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
    private final boolean UDP;
    private final Query query;

    Response(final Query query, final boolean UDP) {
        this.query = query;
        this.header = query.getHeader();
        this.UDP = UDP;
        header.setQR(true);
        header.setAA(true);
        header.setRA(false);

        for (Queries q : query.getQueries()) {
            String name = q.getName();
            final RRCode type = q.getType();
            List<RR> v = new ArrayList<>();

            if (! setZone(name)) {
                break;
            }

            if (! setMinimum()) {
                break;
            }

            if (query.getOptrr() != null) {
                DNSSEC = query.getOptrr().isDNSSEC();
                maximumPayload = query.getOptrr().getPayloadSize();
            }


            final Map.Entry<String, List<RR>> stringAndArrayList = findRR(type, name);
            if (stringAndArrayList.getValue().isEmpty()) {
                noResourceRecord();
            } else {
                name = stringAndArrayList.getKey();
                v = stringAndArrayList.getValue();
            }


            boolean firstTime = true;
            for (RR rr : v) {
                doOneRR(name, type, v, firstTime, rr);
                firstTime = false;
            }

            addAuthorities();
            addAdditionals();
        }

        if (query.getOptrr() != null && header.getNumAdditionals() > 1) {
            header.setNumAdditionals(header.getNumAdditionals() + 1);
        }
        header.build();
    }

    private void noResourceRecord() {
        logger.traceEntry();
        authority = Utils.combine(authority, SOA.getBytes(zone.getName(), minimum));
        numAuthorities = 1;
        if (DNSSEC) {
            numAuthorities++;
            addRRSignature(RRCode.SOA, zone.getName(), authority, ResponseSection.AUTHORITY);
            addNSECRecords(zone.getName());
            addRRSignature(RRCode.NSEC, zone.getName(), authority, ResponseSection.AUTHORITY);
        }
        logger.traceExit();
    }

    private void doOneRR(final String name, final RRCode type,
                         final List<RR> v, final boolean firstTime, final RR rr) {
        logger.traceEntry();

        final byte[] add = rr.getBytes(name, minimum);

        if (UDP && (responses != null)
                && ((responses.length + add.length) > maximumPayload)) {
            header.setTC(true);
        }

        responses = Utils.combine(responses, add);
        header.incrementNumAnswers();

        //Add RRSIG Records Corresponding to Type
        //seems right to add answers somewhere close but we only want to do it once on last
        //TODO Check the stuff to assure its doing what I want it to
        if ((v.indexOf(rr) + 1 == v.size()) && DNSSEC) {
            addRRSignature(rr.getType(), name, responses, ResponseSection.ANSWER);
        }

        if (firstTime && type != RRCode.NS && type != RRCode.DNSKEY) {
            logger.trace("Before calling createAuthorities");
            createAuthorities(name);
            logger.trace("After calling createAuthorities");
        }

        if (type == RRCode.MX) {
            createAorAAAA(rr.getHost(), name);
        }

        if (type == RRCode.NS) {
            createAorAAAA(rr.getString(), name);
        }
        if (DNSSEC && type == RRCode.SOA) {
            final List<RR> dnsKeyArrayList = zone.get(RRCode.DNSKEY, name);
            createAdditionals(dnsKeyArrayList, name);
        }

        logger.traceExit();
    }

    private boolean setZone(@NonNull final String name) {
        zone = JDNSS.getZone(name);
        if (zone.isEmpty()) {
            logger.debug("Zone lookup of " + name + " failed");
            header.setRcode(ErrorCodes.REFUSED.getCode());
            header.setAA(false);
            return false;
        }
        return true;
    }

    private boolean setMinimum() {
        String name = zone.getName();
        assert name != null;

        final List<RR> w = zone.get(RRCode.SOA, name);
        if (w.isEmpty()) {
            logger.debug("SOA lookup of " + name + " failed");
            header.setAA(false);
            header.setRcode(ErrorCodes.REFUSED.getCode());
            return false;
        }
        SOA = (SOARR) w.get(0);
        minimum = SOA.getMinimum();
        return true;
    }

    private void addAuthorities() {
        logger.traceEntry();
        logger.trace(numAuthorities);
        if (numAuthorities > 0) {
            if (!UDP || responses.length + authority.length < maximumPayload) {
                responses = Utils.combine(responses, authority);
                header.setNumAuthorities(numAuthorities);
            } else if (responses.length + authority.length >= maximumPayload) {
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
            } else if (responses.length + additional.length >= maximumPayload) {
                header.setTC(true);
            }
        }
    }

    /**
     * Given a zone and an MX or NS hostname, see if there is an A or AAAA
     * record we can also send back...
     */
    private void createAorAAAA(@NonNull final String host, @NonNull final String name) {
        logger.traceEntry();

        for (RRCode rrCode: Arrays.asList(RRCode.A, RRCode.AAAA)) {
            final List<RR> v = zone.get(rrCode, host);
            if (! v.isEmpty()) {
                createAdditionals(v, host);
            }
        }
    }

    // put the possible additionals in, but don't add to response until we know there is room for them.
    private void createAdditionals(final List<RR> v, final String host) {
        logger.traceEntry();
        final RRCode type = v.get(0).getType();

        for (RR rr : v) {
            additional = Utils.combine(additional, rr.getBytes(host, minimum));
            numAdditionals++;
        }

        if (DNSSEC) {
            addRRSignature(type, host, additional, ResponseSection.ADDITIONAL);
        }
    }

    // put the possible authorities in, but don't add to response until we know
    // there is room for them.
    private void createAuthorities(final String name) {
        logger.traceEntry(name);
        final List<RR> v = zone.get(RRCode.NS, zone.getName());

        for (RR nsrr : v) {
            authority = Utils.combine(authority, nsrr.getBytes(nsrr.getName(), minimum));
            numAuthorities++;
            createAorAAAA(nsrr.getString(), name);
        }

        if (DNSSEC) {
            addRRSignature(RRCode.NS, zone.getName(), authority, ResponseSection.AUTHORITY);
        }
    }


    private void addRRSignature(final RRCode type, final String name, final byte[] destination,
                                final ResponseSection section) {
        logger.traceEntry(name);
        final List<RR> rrsigv = zone.get(RRCode.RRSIG, name);
        for (RR foo : rrsigv) {
            final RRSIG rrsig = (RRSIG) foo;
            if (rrsig.getTypeCovered() == type) {
                final byte[] add = rrsig.getBytes(name, minimum);
                foo(section, add, destination);
            }
        }
    }

    private void foo(final ResponseSection section, final byte[] add, final byte[] destination) {
        switch (section) {
            case ANSWER:
                if (UDP && (responses.length + add.length > maximumPayload)) {
                    header.setTC(true);
                    break;
                }
                responses = Utils.combine(destination, add);
                header.incrementNumAnswers();
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
                    break;
                } else {
                    additional = Utils.combine(destination, add);
                    numAdditionals++;
                    break;
                }
            default:
                logger.error("Shouldn't get here.");
                break;
        }
    }

    private void addNSECRecords(final String name) {
        logger.traceEntry();
        final List<RR> nsecv = zone.get(RRCode.NSEC, zone.getName());

        final NSECRR nsec = (NSECRR) nsecv.get(0);
        final byte[] add = nsec.getBytes(name, minimum);
        authority = Utils.combine(authority, add);
        numAuthorities++;
    }

    private Map.Entry<String, List<RR>> findRR(final RRCode type, final String name) {
        logger.traceEntry();
        List<RR> list = zone.get(type, name);
        if (list.isEmpty()) {
            logger.debug("Didn't find: " + name);
            if (type != RRCode.AAAA && type != RRCode.A) {
                nameNotFound(type, name);
                return Map.entry("", Collections.emptyList());
            } else {
                return lookForCNAME(type, name);
            }
        }
        Map.Entry<java.lang.String, List<edu.msudenver.cs.jdnss.RR>> ret =
                Map.entry(name, list);
        logger.traceExit(ret);
        return ret;
    }

    private void nameNotFound(final RRCode type, final String name) {
        logger.traceEntry();
        if (DNSSEC) {
            throw new AssertionError();
        }

        if (type == RRCode.MX) {
            logger.debug("'" + type.toString() + "' lookup of " + name + " failed");
            header.setRcode(ErrorCodes.NOERROR.getCode());
        } else {
            logger.debug("'" + type.toString() + "' lookup of " + name + " failed");
            header.setRcode(ErrorCodes.NAMEERROR.getCode());
        }
    }

    private Map.Entry<String, List<RR>> lookForCNAME(final RRCode type, final String name) {
        logger.traceEntry();
        logger.debug("Looking for a CNAME for " + name);

        final Map.Entry<String, List<RR>> empty = Map.entry("", Collections.emptyList());

        final List<RR> u = zone.get(RRCode.CNAME, name);
        if (u.isEmpty()) {
            dealWithOther(type, name);
            return empty;
        }

        final String s = u.get(0).getString();
        final List<RR> v = zone.get(type, s);
        if (! v.isEmpty()) {
            responses = Utils.combine(responses, u.get(0).getBytes(name, minimum));
            header.incrementNumAnswers();
            return Map.entry(s, v);
        }
        return empty;
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
        final RRCode other = type == RRCode.A ? RRCode.AAAA : RRCode.A;

        final List<RR> v = zone.get(other, name);
        if (v.isEmpty()) {
            logger.debug(type.toString() + " lookup of " + name + " failed");
            header.setRcode(ErrorCodes.NAMEERROR.getCode());
            return;
        }

        if (DNSSEC) {
            addNSECRecords(name);
        }
    }

    byte[] getBytes() {
        logger.traceEntry();
        byte[] abc = new byte[0];
        abc = Utils.combine(abc, header.getHeader());
        abc = Utils.combine(abc, query.buildResponseQueries());
        abc = Utils.combine(abc, responses);

        /*
        If an OPT record is present in a received request, compliant
        responders MUST include an OPT record in their respective responses.
         */

        if (query.getOptrr() != null) {
            abc = Utils.combine(abc, query.getOptrr().getBytes());
        }

        return abc;
    }

}
