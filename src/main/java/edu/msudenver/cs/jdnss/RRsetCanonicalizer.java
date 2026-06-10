package edu.msudenver.cs.jdnss;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RRsetCanonicalizer {
    private RRsetCanonicalizer() {
    }

    static byte[] buildCanonicalForm(final RRSIG rrsig, final List<? extends RR> rrset) {
        final byte[] rrsigData = buildRrsigSignedPortion(rrsig);

        final List<byte[]> sortedRdata = new ArrayList<>();
        for (final RR rr : rrset) {
            sortedRdata.add(rr.getBytes());
        }

        Collections.sort(sortedRdata, RRsetCanonicalizer::compareRdata);

        byte[] result = rrsigData;
        for (final byte[] rdata : sortedRdata) {
            result = Utils.combine(result, rdata);
        }
        return result;
    }

    private static byte[] buildRrsigSignedPortion(final RRSIG rrsig) {
        byte[] result = new byte[0];

        result = Utils.combine(result, Utils.getTwoBytes(rrsig.getTypeCovered().getCode(), 2));
        result = Utils.combine(result, Utils.getByte(rrsig.getAlgorithm(), 1));
        result = Utils.combine(result, Utils.getByte(rrsig.getLabels(), 1));
        result = Utils.combine(result, Utils.getBytes(rrsig.getOriginalttl()));
        result = Utils.combine(result, Utils.getTwoBytes(rrsig.getSignatureExpiration(), 4));
        result = Utils.combine(result, Utils.getTwoBytes(rrsig.getSignatureInception(), 4));
        result = Utils.combine(result, Utils.getTwoBytes(rrsig.getKeyTag(), 2));
        result = Utils.combine(result, DnsNameCodec.convertString(rrsig.getSignersName()));

        return result;
    }

    private static int compareRdata(final byte[] a, final byte[] b) {
        for (int i = 0; i < Math.min(a.length, b.length); ++i) {
            int cmp = (a[i] & 0xff) - (b[i] & 0xff);
            if (cmp != 0) {
                return cmp;
            }
        }
        return a.length - b.length;
    }
}
