package edu.msudenver.cs.jdnss;

final class DNSKEYRecordParser implements RecordParser {
    @Override
    public void parse(final Parser parser, final ParsingContext context) throws Exception {
        final int flags = parser.getInt("flags");
        final int protocol = parser.getInt("protocol");
        if (protocol != 3) {
            throw new RuntimeException("DNSKEY protocol must be 3");
        }
        final int algorithm = parser.getInt("algorithm");
        parser.getLeftParen();

        final StringBuilder publicKeyBuilder = new StringBuilder();
        parser.setInBase64(true);
        RRCode tok;
        while ((tok = parser.getNextToken()) == RRCode.BASE64) {
            publicKeyBuilder.append(parser.getStringValue());
        }
        parser.setInBase64(false);
        parser.ensureToken(tok, RRCode.RPAREN, "right paren");

        context.getZone().add(context.getCurrentName(),
            new DNSKEYRR(context.getCurrentName(), context.getCurrentTTL(), flags, protocol,
                algorithm, publicKeyBuilder.toString()));
    }
}
