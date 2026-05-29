package edu.msudenver.cs.jdnss;

import lombok.NonNull;
import lombok.ToString;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

enum ResponseSection {
    ANSWER, ADDITIONAL, AUTHORITY
}

@ToString
class Response {
    private final Logger logger = JDNSS.logger;
    private static final Set<RRCode> ADDRESS_QUERY_TYPES = EnumSet.of(RRCode.A, RRCode.AAAA);
    private static final Map.Entry<String, List<RR>> EMPTY_RESULT =
            Map.entry("", Collections.emptyList());

    private static final class SectionAppendPolicy {
        private final boolean skipWhenOverflow;
        private final Consumer<byte[]> sectionUpdater;
        private final Runnable counterIncrementer;

        private SectionAppendPolicy(final boolean skipWhenOverflow,
                                    final Consumer<byte[]> sectionUpdater,
                                    final Runnable counterIncrementer) {
            this.skipWhenOverflow = skipWhenOverflow;
            this.sectionUpdater = sectionUpdater;
            this.counterIncrementer = counterIncrementer;
        }
    }

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
        maybeAppendDnssecNegativeResponseMaterial();
        logger.traceExit();
    }

    private void maybeAppendDnssecNegativeResponseMaterial() {
        if (!DNSSEC) {
            return;
        }

        numAuthorities++;
        addDnssecNegativeResponseSignature(RRCode.SOA, zone.getName());
        addNSECRecords(zone.getName());
        addDnssecNegativeResponseSignature(RRCode.NSEC, zone.getName());
    }

    private void addDnssecNegativeResponseSignature(final RRCode type, final String name) {
        addRRSignature(type, name, authority, ResponseSection.AUTHORITY);
    }

    private void doOneRR(final String name, final RRCode type,
                         final boolean firstRecord, final boolean lastRecord,
                         final RR rr) {
        logger.traceEntry();

        final byte[] add = rr.getBytes(name, minimum);
        appendAnswerRecord(add);

        //Add RRSIG Records Corresponding to Type
        //seems right to add answers somewhere close but we only want to do it once on last
        //TODO Check the stuff to assure its doing what I want it to
        maybeAddAnswerSignature(lastRecord, rr.getType(), name);

        maybeCreateAuthorities(firstRecord, type, name);
        maybeCreateTypeAdditionals(type, rr, name);
        maybeCreateSoaDnssecAdditionals(type, name);

        logger.traceExit();
    }

    private void maybeAddAnswerSignature(final boolean lastRecord, final RRCode type,
                                         final String name) {
        if (lastRecord && DNSSEC) {
            addRRSignature(type, name, responses, ResponseSection.ANSWER);
        }
    }

    private void appendAnswerRecord(final byte[] add) {
        checkAndMarkUdpOverflow(responses.length, add.length);
        responses = Utils.combine(responses, add);
        header.incrementNumAnswers();
    }

    private void maybeCreateAuthorities(final boolean firstTime, final RRCode type,
                                        final String name) {
        if (shouldCreateAuthorities(firstTime, type)) {
            logger.trace("Before calling createAuthorities");
            createAuthorities(name);
            logger.trace("After calling createAuthorities");
        }
    }

    private boolean shouldCreateAuthorities(final boolean firstTime, final RRCode type) {
        return firstTime && type != RRCode.NS && type != RRCode.DNSKEY;
    }

    private void maybeCreateTypeAdditionals(final RRCode type, final RR rr,
                                            final String name) {
        final String additionalHost = getAdditionalHost(type, rr);
        if (additionalHost != null) {
            createAorAAAA(additionalHost, name);
        }
    }

    private String getAdditionalHost(final RRCode type, final RR rr) {
        switch (type) {
            case MX:
                return rr.getHost();
            case NS:
                return rr.getString();
            default:
                return null;
        }
    }

    private void maybeCreateSoaDnssecAdditionals(final RRCode type,
                                                  final String name) {
        if (DNSSEC && type == RRCode.SOA) {
            addSoaDnssecAdditionals(name);
        }
    }

    private void addSoaDnssecAdditionals(final String name) {
        final List<RR> dnsKeyArrayList = zone.get(RRCode.DNSKEY, name);
        createAdditionals(dnsKeyArrayList, name);
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

        if (!canAppendWholeSection(sectionData.length)) {
            header.markTruncated();
            return;
        }

        appendToResponsesAndSetCount(sectionData, sectionCount, setHeaderCount);
    }

    private boolean canAppendWholeSection(final int sectionLength) {
        return !UDP || responses.length + sectionLength < maximumPayload;
    }

    private void appendToResponsesAndSetCount(final byte[] sectionData, final int sectionCount,
                                              final IntConsumer setHeaderCount) {
        responses = Utils.combine(responses, sectionData);
        setHeaderCount.accept(sectionCount);
    }

    /**
     * Given a zone and an MX or NS hostname, see if there is an A or AAAA
     * record we can also send back...
     */
    private void createAorAAAA(@NonNull final String host, @NonNull final String name) {
        logger.traceEntry();

        for (RRCode rrCode: ADDRESS_QUERY_TYPES) {
            createAddressAdditionalsForType(rrCode, host);
        }
    }

    private void createAddressAdditionalsForType(final RRCode rrCode, final String host) {
        final List<RR> records = zone.get(rrCode, host);
        if (!records.isEmpty()) {
            createAdditionals(records, host);
        }
    }

    // put the possible additionals in, but don't add to response until we know there is room for them.
    private void createAdditionals(final List<RR> v, final String host) {
        logger.traceEntry();
        final RRCode type = v.get(0).getType();

        appendAdditionalRecords(v, host);
        maybeAddDnssecAdditionalSignature(type, host);
    }

    private void appendAdditionalRecords(final List<RR> records, final String host) {
        for (RR rr : records) {
            additional = Utils.combine(additional, rr.getBytes(host, minimum));
            numAdditionals++;
        }
    }

    private void maybeAddDnssecAdditionalSignature(final RRCode type, final String host) {
        if (DNSSEC) {
            addRRSignature(type, host, additional, ResponseSection.ADDITIONAL);
        }
    }

    // put the possible authorities in, but don't add to response until we know
    // there is room for them.
    private void createAuthorities(final String name) {
        logger.traceEntry(name);
        final List<RR> nsRecords = zone.get(RRCode.NS, zone.getName());

        appendAuthorityRecordsAndAddressAdditionals(nsRecords, name);
        maybeAddDnssecAuthoritySignature();
    }

    private void appendAuthorityRecordsAndAddressAdditionals(final List<RR> nsRecords,
                                                             final String queryName) {
        for (RR nsrr : nsRecords) {
            authority = Utils.combine(authority, nsrr.getBytes(nsrr.getName(), minimum));
            numAuthorities++;
            createAorAAAA(nsrr.getString(), queryName);
        }
    }

    private void maybeAddDnssecAuthoritySignature() {
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
        final SectionAppendPolicy policy = resolveSectionAppendPolicy(section);
        if (policy == null) {
            logger.error("Shouldn't get here.");
            return;
        }

        appendSignedToSection(add, destination, policy.skipWhenOverflow,
                policy.sectionUpdater, policy.counterIncrementer);
    }

    private SectionAppendPolicy resolveSectionAppendPolicy(final ResponseSection section) {
        switch (section) {
            case ANSWER:
                return resolveAnswerAppendPolicy();
            case AUTHORITY:
                return resolveAuthorityAppendPolicy();
            case ADDITIONAL:
                return resolveAdditionalAppendPolicy();
            default:
                return null;
        }
    }

    private SectionAppendPolicy resolveAnswerAppendPolicy() {
        return new SectionAppendPolicy(true,
                combined -> responses = combined,
                header::incrementNumAnswers);
    }

    private SectionAppendPolicy resolveAuthorityAppendPolicy() {
        return new SectionAppendPolicy(false,
                combined -> authority = combined,
                () -> numAuthorities++);
    }

    private SectionAppendPolicy resolveAdditionalAppendPolicy() {
        return new SectionAppendPolicy(true,
                combined -> additional = combined,
                () -> numAdditionals++);
    }

    private void appendSignedToSection(final byte[] add, final byte[] destination,
                                       final boolean skipWhenOverflow,
                                       final Consumer<byte[]> sectionUpdater,
                                       final Runnable counterIncrementer) {
        if (shouldSkipSignedSection(add.length, skipWhenOverflow)) {
            return;
        }

        sectionUpdater.accept(Utils.combine(destination, add));
        counterIncrementer.run();
    }

    private boolean shouldSkipSignedSection(final int additionalLength,
                                            final boolean skipWhenOverflow) {
        final boolean overflow = checkAndMarkUdpOverflow(responses.length, additionalLength);
        return overflow && skipWhenOverflow;
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
        final List<RR> list = zone.get(type, name);
        if (list.isEmpty()) {
            logger.debug("Didn't find: " + name);
            if (!isAddressQueryType(type)) {
                nameNotFound(type, name);
                return emptyResult();
            }

            return lookForCNAME(type, name);
        }

        Map.Entry<java.lang.String, List<edu.msudenver.cs.jdnss.RR>> ret =
                Map.entry(name, list);
        logger.traceExit(ret);
        return ret;
    }

    private Map.Entry<String, List<RR>> emptyResult() {
        return EMPTY_RESULT;
    }

    private boolean isAddressQueryType(final RRCode type) {
        return ADDRESS_QUERY_TYPES.contains(type);
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

        final List<RR> u = zone.get(RRCode.CNAME, name);
        if (u.isEmpty()) {
            dealWithOther(type, name);
            return emptyResult();
        }

        final String s = u.get(0).getString();
        final List<RR> v = zone.get(type, s);
        if (! v.isEmpty()) {
            responses = Utils.combine(responses, u.get(0).getBytes(name, minimum));
            header.incrementNumAnswers();
            return Map.entry(s, v);
        }

        return emptyResult();
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
        byte[] responseBytes = buildResponseBytes();
        return appendOptRecordIfPresent(responseBytes);
    }

    private byte[] buildResponseBytes() {
        byte[] responseBytes = new byte[0];
        responseBytes = Utils.combine(responseBytes, header.getHeader());
        responseBytes = Utils.combine(responseBytes, query.buildResponseQueries());
        responseBytes = Utils.combine(responseBytes, responses);
        return responseBytes;
    }

    private byte[] appendOptRecordIfPresent(final byte[] responseBytes) {
        /*
        If an OPT record is present in a received request, compliant
        responders MUST include an OPT record in their respective responses.
         */
        if (optRecord != null) {
            return Utils.combine(responseBytes, optRecord.getBytes());
        }

        return responseBytes;
    }

}
