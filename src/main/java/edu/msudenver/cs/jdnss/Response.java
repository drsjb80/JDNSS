package edu.msudenver.cs.jdnss;

import lombok.NonNull;
import lombok.ToString;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.IntConsumer;

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
    private final OPTRR optRecord;

    private static final class ResolvedRecords {
        private final String name;
        private final List<RR> records;

        private ResolvedRecords(final String name, final List<RR> records) {
            this.name = name;
            this.records = records;
        }
    }

    Response(final Query query, final boolean UDP) {
        this.query = query;
        this.header = new Header(query.getHeader());
        this.UDP = UDP;
        this.optRecord = query.getOptrr();

        for (Queries q : query.getQueries()) {
            if (!processQuery(q)) {
                break;
            }
        }

        finalizeHeader();
    }

    private boolean processQuery(final Queries q) {
        final String name = q.getName();
        final RRCode type = q.getType();

        if (!prepareQueryContext(name)) {
            return false;
        }

        final ResolvedRecords resolvedRecords = resolveRecords(type, name);
        processResolvedRecords(resolvedRecords.name, type, resolvedRecords.records);

        addAuthorities();
        addAdditionals();
        return true;
    }

    private boolean prepareQueryContext(final String name) {
        if (!setZone(name)) {
            return false;
        }

        if (!setMinimum()) {
            return false;
        }

        applyOptRecordSettings();
        return true;
    }

    private ResolvedRecords resolveRecords(final RRCode type, final String name) {
        final Map.Entry<String, List<RR>> resolved = findRR(type, name);
        if (resolved.getValue().isEmpty()) {
            noResourceRecord();
            return new ResolvedRecords(name, Collections.emptyList());
        }

        return new ResolvedRecords(resolved.getKey(), resolved.getValue());
    }

    private void processResolvedRecords(final String name, final RRCode type,
                                        final List<RR> records) {
        for (int i = 0; i < records.size(); i++) {
            final RR rr = records.get(i);
            final boolean firstRecord = i == 0;
            final boolean lastRecord = i == records.size() - 1;
            doOneRR(name, type, firstRecord, lastRecord, rr);
        }
    }

    private void applyOptRecordSettings() {
        if (optRecord != null) {
            DNSSEC = optRecord.isDNSSEC();
            maximumPayload = optRecord.getPayloadSize();
        }
    }

    private void finalizeHeader() {
        if (optRecord != null && header.getNumAdditionals() > 1) {
            header.incrementAdditionalCount();
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
                         final boolean firstRecord, final boolean lastRecord,
                         final RR rr) {
        logger.traceEntry();

        final byte[] add = rr.getBytes(name, minimum);

        checkAndMarkUdpOverflow(responses.length, add.length);

        responses = Utils.combine(responses, add);
        header.incrementNumAnswers();

        //Add RRSIG Records Corresponding to Type
        //seems right to add answers somewhere close but we only want to do it once on last
        //TODO Check the stuff to assure its doing what I want it to
        if (lastRecord && DNSSEC) {
            addRRSignature(rr.getType(), name, responses, ResponseSection.ANSWER);
        }

        maybeCreateAuthorities(firstRecord, type, name);
        maybeCreateTypeAdditionals(type, rr, name);
        maybeCreateSoaDnssecAdditionals(type, name);

        logger.traceExit();
    }

    private void maybeCreateAuthorities(final boolean firstTime, final RRCode type,
                                        final String name) {
        if (firstTime && type != RRCode.NS && type != RRCode.DNSKEY) {
            logger.trace("Before calling createAuthorities");
            createAuthorities(name);
            logger.trace("After calling createAuthorities");
        }
    }

    private void maybeCreateTypeAdditionals(final RRCode type, final RR rr,
                                            final String name) {
        if (type == RRCode.MX) {
            createAorAAAA(rr.getHost(), name);
        }

        if (type == RRCode.NS) {
            createAorAAAA(rr.getString(), name);
        }
    }

    private void maybeCreateSoaDnssecAdditionals(final RRCode type,
                                                  final String name) {
        if (DNSSEC && type == RRCode.SOA) {
            final List<RR> dnsKeyArrayList = zone.get(RRCode.DNSKEY, name);
            createAdditionals(dnsKeyArrayList, name);
        }
    }

    private boolean setZone(@NonNull final String name) {
        zone = JDNSS.getZone(name);
        if (zone.isEmpty()) {
            logger.debug("Zone lookup of " + name + " failed");
            header.markRefused();
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
            header.markRefused();
            return false;
        }
        SOA = (SOARR) w.get(0);
        minimum = SOA.getMinimum();
        return true;
    }

    private void addAuthorities() {
        addSectionToResponse(authority, numAuthorities, header::setAuthorityCount);
    }

    private void addAdditionals() {
        addSectionToResponse(additional, numAdditionals, header::setAdditionalCount);
    }

    private void addSectionToResponse(final byte[] sectionData, final int sectionCount,
                                      final IntConsumer setHeaderCount) {
        logger.traceEntry();
        logger.trace(sectionCount);

        if (sectionCount <= 0) {
            return;
        }

        if (!UDP || responses.length + sectionData.length < maximumPayload) {
            responses = Utils.combine(responses, sectionData);
            setHeaderCount.accept(sectionCount);
        } else {
            header.markTruncated();
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
                appendSignedRecord(section, add, destination);
            }
        }
    }

    private void appendSignedRecord(final ResponseSection section, final byte[] add,
                                    final byte[] destination) {
        switch (section) {
            case ANSWER:
                appendToAnswerSection(add, destination);
                break;
            case AUTHORITY:
                appendToAuthoritySection(add, destination);
                break;
            case ADDITIONAL:
                appendToAdditionalSection(add, destination);
                break;
            default:
                logger.error("Shouldn't get here.");
                break;
        }
    }

    private void appendToAnswerSection(final byte[] add, final byte[] destination) {
        if (checkAndMarkUdpOverflow(responses.length, add.length)) {
            return;
        }
        responses = Utils.combine(destination, add);
        header.incrementNumAnswers();
    }

    private void appendToAuthoritySection(final byte[] add, final byte[] destination) {
        checkAndMarkUdpOverflow(responses.length, add.length);
        authority = Utils.combine(destination, add);
        numAuthorities++;
    }

    private void appendToAdditionalSection(final byte[] add, final byte[] destination) {
        if (checkAndMarkUdpOverflow(responses.length, add.length)) {
            //if bigger then max payload exit without adding RRSIG
            return;
        }
        additional = Utils.combine(destination, add);
        numAdditionals++;
    }

    private boolean checkAndMarkUdpOverflow(final int currentLength, final int additionalLength) {
        if (!UDP) {
            return false;
        }

        if (currentLength + additionalLength > maximumPayload) {
            header.markTruncated();
            return true;
        }

        return false;
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
            header.markNoError();
        } else {
            logger.debug("'" + type.toString() + "' lookup of " + name + " failed");
            header.markNameError();
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
            header.markNameError();
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

        if (optRecord != null) {
            abc = Utils.combine(abc, optRecord.getBytes());
        }

        return abc;
    }

}
