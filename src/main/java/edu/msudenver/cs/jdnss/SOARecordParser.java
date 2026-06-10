package edu.msudenver.cs.jdnss;

final class SOARecordParser implements RecordParser {
    @Override
    public void parse(final Parser parser, final ParsingContext context) throws Exception {
        final int ttl = context.getCurrentTTL();
        context.setSOATTL(ttl);

        final String mname = parser.getDomain();
        final String rname = parser.getDomain();
        parser.getLeftParen();
        final int serial = parser.getInt("serial");
        final int refresh = parser.getInt("refresh");
        final int retry = parser.getInt("retry");
        final int expire = parser.getInt("expire");
        final int minimum = parser.getInt("minimum");
        parser.getRightParen();

        context.setSOAMinimumTTL(minimum);

        context.getZone().add(context.getCurrentName(),
            new SOARR(context.getCurrentName(), mname, rname, serial, refresh, retry, expire, minimum, ttl));
    }
}
