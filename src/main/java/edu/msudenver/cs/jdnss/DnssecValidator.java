package edu.msudenver.cs.jdnss;

import java.nio.charset.StandardCharsets;
import java.security.spec.ECParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DnssecValidator {
    private static final Map<Integer, DnssecAlgorithm> ALGORITHMS = new HashMap<>();

    static {
        registerRSAAlgorithm(5, "SHA1withRSA");
        registerRSAAlgorithm(7, "SHA256withRSA");
        registerRSAAlgorithm(8, "SHA512withRSA");
        registerRSAAlgorithm(10, "SHA1withRSA");

        registerECDSAAlgorithm(13, "SHA256withECDSA", "secp256r1");
        registerECDSAAlgorithm(14, "SHA384withECDSA", "secp384r1");
    }

    private DnssecValidator() {
    }

    public static boolean validate(final RRSIG rrsig, final List<? extends RR> rrset,
                                   final List<DNSKEYRR> dnssecKeys) throws DnssecValidationException {
        validateTimestamp(rrsig);

        final DNSKEYRR signingKey = findSigningKey(rrsig, dnssecKeys);
        if (signingKey == null) {
            throw new DnssecValidationException(
                    "No signing key found for RRSIG (keyTag=" + rrsig.getKeyTag() + ")"
            );
        }

        final DnssecAlgorithm algorithm = getAlgorithm(signingKey.getAlgorithm());
        if (algorithm == null) {
            throw new DnssecValidationException(
                    "Unsupported DNSSEC algorithm: " + signingKey.getAlgorithm()
            );
        }

        final byte[] canonicalForm = RRsetCanonicalizer.buildCanonicalForm(rrsig, rrset);

        byte[] signature;
        try {
            signature = Base64.getDecoder().decode(rrsig.getSignature().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new DnssecValidationException("Invalid Base64 encoding in RRSIG signature", e);
        }

        byte[] publicKeyMaterial;
        try {
            publicKeyMaterial = Base64.getDecoder().decode(signingKey.getPublicKey().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new DnssecValidationException("Invalid Base64 encoding in DNSKEY public key", e);
        }

        return algorithm.verify(publicKeyMaterial, canonicalForm, signature);
    }

    private static void validateTimestamp(final RRSIG rrsig) throws DnssecValidationException {
        final long now = System.currentTimeMillis() / 1000;

        if (rrsig.getSignatureInception() > now) {
            throw new DnssecValidationException(
                    "Signature not yet valid (inception: " + rrsig.getSignatureInception() + ", now: " + now + ")"
            );
        }

        if (rrsig.getSignatureExpiration() < now) {
            throw new DnssecValidationException(
                    "Signature expired (expiration: " + rrsig.getSignatureExpiration() + ", now: " + now + ")"
            );
        }
    }

    private static DNSKEYRR findSigningKey(final RRSIG rrsig, final List<DNSKEYRR> dnssecKeys) {
        for (final DNSKEYRR key : dnssecKeys) {
            if (key.getAlgorithm() == rrsig.getAlgorithm() &&
                key.calculateKeyTag() == rrsig.getKeyTag()) {
                return key;
            }
        }
        return null;
    }

    private static DnssecAlgorithm getAlgorithm(final int algorithmCode) {
        return ALGORITHMS.get(algorithmCode);
    }

    private static void registerRSAAlgorithm(final int code, final String signatureAlgorithm) {
        ALGORITHMS.put(code, new DnssecAlgorithmRSA(code, signatureAlgorithm));
    }

    private static void registerECDSAAlgorithm(final int code, final String signatureAlgorithm,
                                               final String curveName) {
        try {
            final java.security.spec.ECGenParameterSpec ecSpec = new java.security.spec.ECGenParameterSpec(curveName);
            final java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC");
            kpg.initialize(ecSpec);
            final ECParameterSpec parameterSpec = ((java.security.interfaces.ECKey) kpg.generateKeyPair().getPublic()).getParams();
            ALGORITHMS.put(code, new DnssecAlgorithmECDSA(code, signatureAlgorithm, parameterSpec));
        } catch (Exception e) {
            throw new RuntimeException("Failed to register ECDSA algorithm " + code, e);
        }
    }
}
