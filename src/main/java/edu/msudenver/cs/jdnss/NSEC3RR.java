package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.logging.log4j.Logger;

import java.util.Set;

@ToString
@EqualsAndHashCode(callSuper = true)
class NSEC3RR extends RR {
    private final Logger logger = JDNSS.logger;
    private final int hashAlgorithm;
    private final int flags;
    private final int iterations;
    private final String salt;
    private final String nextHashedOwnerName;
    private final Set<RRCode> types;

    NSEC3RR(final String domain, final int ttl, final int hashAlgorithm,
            final int flags, final int iterations, final String salt,
            final String nextHashedOwnerName, final Set<RRCode> types) {
        super(domain, RRCode.NSEC3, ttl);
        this.hashAlgorithm = hashAlgorithm;
        this.flags = flags;
        this.iterations = iterations;
        this.salt = salt;
        this.nextHashedOwnerName = nextHashedOwnerName;
        this.types = types;
    }

    @Override
    protected byte[] getBytes() {
        byte[] a = new byte[0];
        a = Utils.combine(a, Utils.getByte(hashAlgorithm, 1));
        a = Utils.combine(a, Utils.getByte(flags, 2));
        a = Utils.combine(a, Utils.getTwoBytes(iterations, 1));
        a = Utils.combine(a, Utils.getByte(salt.length(), 1));
        a = Utils.combine(a, DnsNameCodec.convertString(salt));
        a = Utils.combine(a, Utils.getByte(this.nextHashedOwnerName.length(), 1));
        a = Utils.combine(a, DnsNameCodec.convertString(nextHashedOwnerName));
        a = Utils.combine(a, buildBitMap());
        return a;
    }

    private byte[] buildBitMap() {
        int largestRcode = 0;
        for (RRCode rr : types) {
            if (rr.getCode() > largestRcode) {
                largestRcode = rr.getCode();
            }
        }
        int length = (largestRcode + 8) / 8;
        byte[] bitMap = new byte[length];
        byte[] a = {0x00};
        a = Utils.combine(a, (byte) length);
        a = Utils.combine(a, setBits(bitMap));
        return a;
    }

    private byte[] setBits(byte[] bitMap) {
        for (RRCode rr : types) {
            switch (rr) {
                case A:
                    bitMap[0] = (byte) (bitMap[0] + 64);
                    break;
                case NS:
                    bitMap[0] = (byte) (bitMap[0] + 32);
                    break;
                case CNAME:
                    bitMap[0] = (byte) (bitMap[0] + 4);
                    break;
                case SOA:
                    bitMap[0] = (byte) (bitMap[0] + 2);
                    break;
                case PTR:
                    bitMap[1] = (byte) (bitMap[1] + 8);
                    break;
                case HINFO:
                    bitMap[1] = (byte) (bitMap[1] + 4);
                    break;
                case MX:
                    bitMap[1] = (byte) (bitMap[1] + 1);
                    break;
                case TXT:
                    bitMap[2] = (byte) (bitMap[2] + 128);
                    break;
                case AAAA:
                    bitMap[3] = (byte) (bitMap[3] + 8);
                    break;
                case A6:
                    bitMap[4] = (byte) (bitMap[4] + 2);
                    break;
                case DNAME:
                    bitMap[4] = (byte) (bitMap[4] + 1);
                    break;
                case DS:
                    bitMap[5] = (byte) (bitMap[5] + 16);
                    break;
                case RRSIG:
                    bitMap[5] = (byte) (bitMap[5] + 2);
                    break;
                case NSEC:
                    bitMap[5] = (byte) (bitMap[5] + 1);
                    break;
                case DNSKEY:
                    bitMap[6] = (byte) (bitMap[6] + 128);
                    break;
                case NSEC3:
                    bitMap[6] = (byte) (bitMap[6] + 32);
                    break;
                case NSEC3PARAM:
                    bitMap[6] = (byte) (bitMap[6] + 16);
                    break;
                default:
                    logger.error("Couldn't add/find {} to NSEC3 bit map", rr);
                    break;
            }
        }
        return bitMap;
    }
}
