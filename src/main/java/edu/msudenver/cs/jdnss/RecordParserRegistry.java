package edu.msudenver.cs.jdnss;

import java.util.HashMap;
import java.util.Map;

final class RecordParserRegistry {
    private static final Map<RRCode, RecordParser> REGISTRY = new HashMap<>();

    static {
        registerComplexParsers();
        registerSimpleParsers();
    }

    private RecordParserRegistry() {
    }

    static RecordParser getParser(final RRCode code) {
        return REGISTRY.get(code);
    }

    private static void registerComplexParsers() {
        REGISTRY.put(RRCode.SOA, new SOARecordParser());
        REGISTRY.put(RRCode.RRSIG, new RRSIGRecordParser());
        REGISTRY.put(RRCode.DNSKEY, new DNSKEYRecordParser());
        REGISTRY.put(RRCode.NSEC, new NSECRecordParser());
        REGISTRY.put(RRCode.NSEC3, new NSEC3RecordParser());
        REGISTRY.put(RRCode.NSEC3PARAM, new NSEC3PARAMRecordParser());
    }

    private static void registerSimpleParsers() {
        REGISTRY.put(RRCode.A, (parser, context) ->
            context.getZone().add(context.getCurrentName(),
                new ARR(context.getCurrentName(), context.getCurrentTTL(), parser.getIPV4ADDR()))
        );

        REGISTRY.put(RRCode.AAAA, (parser, context) ->
            context.getZone().add(context.getCurrentName(),
                new AAAARR(context.getCurrentName(), context.getCurrentTTL(), parser.getIPV6ADDR()))
        );

        REGISTRY.put(RRCode.NS, (parser, context) ->
            context.getZone().add(context.getCurrentName(),
                new NSRR(context.getCurrentName(), context.getCurrentTTL(), parser.getDomain()))
        );

        REGISTRY.put(RRCode.CNAME, (parser, context) ->
            context.getZone().add(context.getCurrentName(),
                new CNAMERR(context.getCurrentName(), context.getCurrentTTL(), parser.getDomain()))
        );

        REGISTRY.put(RRCode.TXT, (parser, context) ->
            context.getZone().add(context.getCurrentName(),
                new TXTRR(context.getCurrentName(), context.getCurrentTTL(), parser.getString()))
        );

        REGISTRY.put(RRCode.PTR, (parser, context) ->
            context.getZone().add(context.getCurrentName(),
                new PTRRR(context.getCurrentName(), context.getCurrentTTL(), parser.getDomain()))
        );

        REGISTRY.put(RRCode.MX, (parser, context) -> {
            final int preference = parser.getInt("preference");
            final String exchanger = parser.getDomain();
            context.getZone().add(context.getCurrentName(),
                new MXRR(context.getCurrentName(), context.getCurrentTTL(), exchanger, preference));
        });

        REGISTRY.put(RRCode.HINFO, (parser, context) -> {
            final String cpu = parser.getString();
            final String os = parser.getString();
            context.getZone().add(context.getCurrentName(),
                new HINFORR(context.getCurrentName(), context.getCurrentTTL(), cpu, os));
        });

        REGISTRY.put(RRCode.SRV, (parser, context) -> {
            final int priority = parser.getInt("priority");
            final int weight = parser.getInt("weight");
            final int port = parser.getInt("port");
            final String target = parser.getDomain();
            context.getZone().add(context.getCurrentName(),
                new SRVRR(context.getCurrentName(), context.getCurrentTTL(), priority, weight, port, target));
        });

        REGISTRY.put(RRCode.TLSA, (parser, context) -> {
            final int usage = parser.getInt("usage");
            final int selector = parser.getInt("selector");
            final int matchingType = parser.getInt("matching_type");
            final String associationData = parser.getHex();
            context.getZone().add(context.getCurrentName(),
                new TLSARR(context.getCurrentName(), context.getCurrentTTL(), usage, selector, matchingType, associationData));
        });

        REGISTRY.put(RRCode.CAA, (parser, context) -> {
            final int flags = parser.getInt("flags");
            final String tag = parser.getString();
            final String value = parser.getString();
            context.getZone().add(context.getCurrentName(),
                new CAARR(context.getCurrentName(), context.getCurrentTTL(), flags, tag, value));
        });
    }
}
