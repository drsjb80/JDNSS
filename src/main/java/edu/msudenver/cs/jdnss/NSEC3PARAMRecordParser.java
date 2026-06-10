package edu.msudenver.cs.jdnss;

final class NSEC3PARAMRecordParser implements RecordParser {
    @Override
    public void parse(final Parser parser, final ParsingContext context) throws Exception {
        final int hashAlgorithm = parser.getInt("hash algorithm");
        final int flags = parser.getInt("flags");
        final int iterations = parser.getInt("interations");

        final String salt = parser.getHex();

        final NSEC3PARAMRR d = new NSEC3PARAMRR(context.getCurrentName(), context.getCurrentTTL(),
            hashAlgorithm, flags, iterations, salt);
        context.getZone().add(context.getCurrentName(), d);
    }
}
