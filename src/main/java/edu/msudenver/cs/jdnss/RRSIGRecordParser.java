package edu.msudenver.cs.jdnss;

final class RRSIGRecordParser implements RecordParser {
    @Override
    public void parse(final Parser parser, final ParsingContext context) throws Exception {
        final RRCode typeCovered = parser.getNextToken();
        if (!parser.isARR(typeCovered)) {
            throw new RuntimeException(typeCovered + " not covered with RRSIG");
        }

        final int algorithm = parser.getInt("algorithm");
        final int labels = parser.getInt("labels");
        final int originalTTL = parser.getInt("original TTL");

        parser.getLeftParen();

        final int expiration = parser.getDate();
        final int inception = parser.getDate();
        final int keyTag = parser.getInt("key tag");
        final String signersName = parser.getDomain();

        final StringBuilder signatureBuilder = new StringBuilder();
        RRCode tok;

        parser.setInBase64(true);
        while ((tok = parser.getNextToken()) == RRCode.BASE64) {
            signatureBuilder.append(parser.getStringValue());
        }
        parser.setInBase64(false);
        parser.ensureToken(tok, RRCode.RPAREN, "right paren");

        final RRSIG d = new RRSIG(context.getCurrentName(), context.getCurrentTTL(), typeCovered,
                algorithm, labels, originalTTL, expiration, inception,
                keyTag, signersName, signatureBuilder.toString());
        context.getZone().add(context.getCurrentName(), d);
    }
}
