package edu.msudenver.cs.jdnss;

interface DnssecAlgorithm {
    boolean verify(byte[] publicKeyMaterial, byte[] signedData, byte[] signature)
            throws DnssecValidationException;

    int getAlgorithmCode();
}
