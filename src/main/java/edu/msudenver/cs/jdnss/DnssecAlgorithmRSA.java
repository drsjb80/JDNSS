package edu.msudenver.cs.jdnss;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;

class DnssecAlgorithmRSA implements DnssecAlgorithm {
    private final int algorithmCode;
    private final String signatureAlgorithm;

    DnssecAlgorithmRSA(final int algorithmCode, final String signatureAlgorithm) {
        this.algorithmCode = algorithmCode;
        this.signatureAlgorithm = signatureAlgorithm;
    }

    @Override
    public boolean verify(final byte[] publicKeyMaterial, final byte[] signedData,
                          final byte[] signature) throws DnssecValidationException {
        try {
            final RSAKeyComponents keyComponents = parsePublicKey(publicKeyMaterial);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final RSAPublicKeySpec keySpec = new RSAPublicKeySpec(
                    keyComponents.modulus,
                    keyComponents.exponent
            );
            final PublicKey publicKey = keyFactory.generatePublic(keySpec);

            final Signature sig = Signature.getInstance(signatureAlgorithm);
            sig.initVerify(publicKey);
            sig.update(signedData);
            return sig.verify(signature);
        } catch (Exception e) {
            throw new DnssecValidationException("RSA signature verification failed", e);
        }
    }

    @Override
    public int getAlgorithmCode() {
        return algorithmCode;
    }

    private RSAKeyComponents parsePublicKey(final byte[] keyMaterial) {
        int offset = 0;
        final int exponentLength;

        if ((keyMaterial[0] & 0xff) != 0) {
            exponentLength = keyMaterial[0] & 0xff;
            offset = 1;
        } else {
            exponentLength = ((keyMaterial[1] & 0xff) << 8) | (keyMaterial[2] & 0xff);
            offset = 3;
        }

        final BigInteger exponent = new BigInteger(1, subarray(keyMaterial, offset, exponentLength));
        offset += exponentLength;

        final BigInteger modulus = new BigInteger(1, subarray(keyMaterial, offset, keyMaterial.length - offset));

        return new RSAKeyComponents(modulus, exponent);
    }

    private byte[] subarray(final byte[] array, final int offset, final int length) {
        byte[] result = new byte[length];
        System.arraycopy(array, offset, result, 0, length);
        return result;
    }

    private static final class RSAKeyComponents {
        final BigInteger modulus;
        final BigInteger exponent;

        RSAKeyComponents(final BigInteger modulus, final BigInteger exponent) {
            this.modulus = modulus;
            this.exponent = exponent;
        }
    }
}
