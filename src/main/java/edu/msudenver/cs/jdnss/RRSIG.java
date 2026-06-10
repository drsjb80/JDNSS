package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ToString
@EqualsAndHashCode(callSuper = true)
class RRSIG extends RR {
    @Getter
    private final RRCode typeCovered;
    @Getter
    private final int algorithm;
    @Getter
    private final int labels;
    @Getter
    private final int originalttl;
    @Getter
    private final int signatureExpiration;
    @Getter
    private final int signatureInception;
    @Getter
    private final int keyTag;
    @Getter
    private final String signersName;
    @Getter
    private final String signature;

    RRSIG(final String domain, final int ttl, final RRCode typeCovered,
          final int algorithm, final int labels, final int originalttl,
          final int expiration, final int inception, final int keyTag,
          final String signersName, final String signature) {
        super(domain, RRCode.RRSIG, ttl);

        this.typeCovered = typeCovered;
        this.algorithm = algorithm;
        this.labels = labels;
        this.originalttl = originalttl;
        this.signatureExpiration = expiration;
        this.signatureInception = inception;
        this.keyTag = keyTag;
        this.signersName = signersName;
        this.signature = signature;
    }

    @Override
    protected byte[] getBytes() {
        byte[] a = new byte[0];
        a = Utils.combine(a, Utils.getTwoBytes(typeCovered.getCode(), 2));
        a = Utils.combine(a, Utils.getByte(algorithm, 1));
        a = Utils.combine(a, Utils.getByte(labels, 1));
        a = Utils.combine(a, Utils.getBytes(originalttl));
        a = Utils.combine(a, Utils.getTwoBytes(signatureExpiration, 4));
        a = Utils.combine(a, Utils.getTwoBytes(signatureInception, 4));
        a = Utils.combine(a, Utils.getTwoBytes(keyTag, 2));
        a = Utils.combine(a, DnsNameCodec.convertString(signersName));
        try {
            a = Utils.combine(a, Base64.getDecoder().decode(signature.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 encoding in RRSIG signature", e);
        }
        return a;
    }
}
