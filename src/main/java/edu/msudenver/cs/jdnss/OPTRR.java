package edu.msudenver.cs.jdnss;

import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

class OPTRR {
    @Getter
    private boolean DNSSEC = false;
    final static Logger logger = JDNSS.logger;
    private int payloadSize;
    private int rcodeAndFlags;
    @Getter private int rdLength;
    private byte extendedrcode = 0; // extended RCODE of 0 indicates use of a regular RCODE
    private byte version;
    private int flags;
    private int optionCode;
    @Getter private int optionLength;
    @Getter private byte[] clientCookie;
    @Getter private byte[] serverCookie;

    OPTRR(byte[] bytes) {
        logger.traceEntry();
        Assertion.aver(bytes[0] == 0);

        int location = 1;

        int type = Utils.addThem(bytes[location++], bytes[location++]);
        Assertion.aver(type == 41);

        payloadSize = Utils.addThem(bytes[location++], bytes[location++]);
        logger.trace(payloadSize);

        extendedrcode = bytes[location++];
        logger.trace(extendedrcode);

        version = bytes[location++];
        Assertion.aver(version == 0);

        flags = Utils.addThem(bytes[location++], bytes[location++]);

        // logger.error(flags);
        DNSSEC = flags >> 15 == 1; // DNSSEC OK bit as defined by [RFC3225].
        // logger.error(DNSSEC);
        logger.trace(DNSSEC);

        rdLength = Utils.addThem(bytes[location++], bytes[location++]);
        logger.trace(rdLength);

        if (rdLength >= 5) { //data length needs to be minimum of 5 bytes

            optionCode = Utils.addThem(bytes[location++], bytes[location++]);
            Assertion.aver(optionCode == 10);

            optionLength = Utils.addThem(bytes[location++], bytes[location++]);
            logger.trace(optionLength);

            clientCookie = Arrays.copyOfRange(bytes, location, location + 8);
            location += 8;


            if (optionLength > 8) { // server cookie returned
                // OPTION-LENGTH >= 16, <= 40 [rfc7873]
                Assertion.aver(optionLength == 16
                    || optionLength == 24
                    || optionLength == 32
                    || optionLength == 40);

                serverCookie = Arrays.copyOfRange(bytes, location, location + optionLength - 8);
            }
        }
    }

    protected boolean hasCookie(){
        return this.getRdLength() > 0;
    }

     /*
            If the COOKIE option is too short to contain a Client Cookie, then
            FORMERR is generated.  If the COOKIE option is longer than that
            required to hold a COOKIE option with just a Client Cookie (8 bytes)
            but is shorter than the minimum COOKIE option with both a
            Client Cookie and a Server Cookie (16 bytes), then FORMERR is
            generated.  If the COOKIE option is longer than the maximum valid
            COOKIE option (40 bytes), then FORMERR is generated.
            */
    protected boolean hasFormErr(){
        return this.getOptionLength() < 8
            || (this.getOptionLength() > 8 && this.getOptionLength() < 16)
            || this.getOptionLength() > 40;
    }
}
