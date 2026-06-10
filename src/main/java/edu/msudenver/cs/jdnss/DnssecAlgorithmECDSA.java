package edu.msudenver.cs.jdnss;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;

class DnssecAlgorithmECDSA implements DnssecAlgorithm {
    private final int algorithmCode;
    private final String signatureAlgorithm;
    private final int coordinateLength;
    private final ECParameterSpec parameterSpec;

    DnssecAlgorithmECDSA(final int algorithmCode, final String signatureAlgorithm,
                         final ECParameterSpec parameterSpec) {
        this.algorithmCode = algorithmCode;
        this.signatureAlgorithm = signatureAlgorithm;
        this.parameterSpec = parameterSpec;
        this.coordinateLength = parameterSpec.getOrder().bitLength() / 8;
        if (parameterSpec.getOrder().bitLength() % 8 != 0) {
            throw new IllegalArgumentException("Coordinate length must be byte-aligned");
        }
    }

    @Override
    public boolean verify(final byte[] publicKeyMaterial, final byte[] signedData,
                          final byte[] signature) throws DnssecValidationException {
        try {
            if (publicKeyMaterial.length != 2 * coordinateLength) {
                throw new DnssecValidationException("Invalid ECDSA public key length");
            }

            final BigInteger x = new BigInteger(1, subarray(publicKeyMaterial, 0, coordinateLength));
            final BigInteger y = new BigInteger(1, subarray(publicKeyMaterial, coordinateLength, coordinateLength));

            final ECPoint point = new ECPoint(x, y);
            final ECPublicKeySpec keySpec = new ECPublicKeySpec(point, parameterSpec);
            final KeyFactory keyFactory = KeyFactory.getInstance("EC");
            final PublicKey publicKey = keyFactory.generatePublic(keySpec);

            final byte[] derSignature = convertToDER(signature);

            final Signature sig = Signature.getInstance(signatureAlgorithm);
            sig.initVerify(publicKey);
            sig.update(signedData);
            return sig.verify(derSignature);
        } catch (DnssecValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new DnssecValidationException("ECDSA signature verification failed", e);
        }
    }

    @Override
    public int getAlgorithmCode() {
        return algorithmCode;
    }

    private byte[] convertToDER(final byte[] rawSignature) throws DnssecValidationException {
        if (rawSignature.length != 2 * coordinateLength) {
            throw new DnssecValidationException("Invalid ECDSA signature length");
        }

        final BigInteger r = new BigInteger(1, subarray(rawSignature, 0, coordinateLength));
        final BigInteger s = new BigInteger(1, subarray(rawSignature, coordinateLength, coordinateLength));

        return encodeSequence(encodeInteger(r), encodeInteger(s));
    }

    private byte[] encodeInteger(final BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] result = new byte[bytes.length + 2];
        result[0] = 0x02;
        result[1] = (byte) bytes.length;
        System.arraycopy(bytes, 0, result, 2, bytes.length);
        return result;
    }

    private byte[] encodeSequence(final byte[]... elements) {
        int totalLength = 0;
        for (byte[] element : elements) {
            totalLength += element.length;
        }

        byte[] result = new byte[totalLength + 2];
        result[0] = 0x30;
        result[1] = (byte) totalLength;

        int offset = 2;
        for (byte[] element : elements) {
            System.arraycopy(element, 0, result, offset, element.length);
            offset += element.length;
        }

        return result;
    }

    private byte[] subarray(final byte[] array, final int offset, final int length) {
        byte[] result = new byte[length];
        System.arraycopy(array, offset, result, 0, length);
        return result;
    }
}
