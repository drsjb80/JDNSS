package edu.msudenver.cs.jdnss;

import java.util.EnumSet;
import java.util.Set;

final class NSECRecordParser implements RecordParser {
    @Override
    public void parse(final Parser parser, final ParsingContext context) throws Exception {
        boolean paren = false;

        String nextDomainName = parser.getDomain();

        RRCode a = parser.getNextToken();

        if (a == RRCode.LPAREN) {
            paren = true;
            a = parser.getNextToken();
        }

        Set<RRCode> resourceRecords = EnumSet.noneOf(RRCode.class);
        while (parser.isARR(a)) {
            resourceRecords.add(a);
            a = parser.getNextToken();
        }

        if (paren) {
            parser.getRightParen();
        }

        context.getZone().add(context.getCurrentName(), new NSECRR(context.getCurrentName(), context.getCurrentTTL(), nextDomainName,
            resourceRecords));
    }
}
