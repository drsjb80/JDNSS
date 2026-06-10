package edu.msudenver.cs.jdnss;

import java.util.EnumSet;
import java.util.Set;

final class NSEC3RecordParser implements RecordParser {
    @Override
    public void parse(final Parser parser, final ParsingContext context) throws Exception {
        final int hashAlgorithm = parser.getInt("hash algorithm");
        final int flags = parser.getInt("flags");
        final int iterations = parser.getInt("iterations");

        final String salt = parser.getHex();
        parser.getLeftParen();

        final StringBuilder nextHashedOwnerNameBuilder = new StringBuilder();
        parser.setInBase64(true);
        while (parser.getNextToken() == RRCode.BASE64) {
            nextHashedOwnerNameBuilder.append(parser.getStringValue());
        }
        parser.setInBase64(false);

        final Set<RRCode> types = EnumSet.noneOf(RRCode.class);
        RRCode rrcode;
        while ((rrcode = parser.getNextToken()) != RRCode.RPAREN) {
            types.add(rrcode);
        }

        final NSEC3RR d = new NSEC3RR(context.getCurrentName(), context.getCurrentTTL(), hashAlgorithm,
            flags, iterations, salt, nextHashedOwnerNameBuilder.toString(), types);
        context.getZone().add(context.getCurrentName(), d);
    }
}
