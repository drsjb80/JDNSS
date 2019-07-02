package edu.msudenver.cs.jdnss;

import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

class OPTRR {
    @Getter private boolean DNSSEC;
    final static Logger logger = JDNSS.logger;
    @Getter private int payloadSize;
    private int type;
    @Getter private int rdLength;
    private byte extendedrcode; // extended RCODE of 0 indicates use of a regular RCODE
    private byte version;
    private int flags;
    private int optionCode;
    @Getter private int optionLength;
    @Getter private byte[] clientCookie;
    @Getter private byte[] serverCookie;

    /*
    If a query message with more than one
   OPT RR is received, a FORMERR (RCODE=1) MUST be returned.

   If an OPT record is present in a received request, compliant
   responders MUST include an OPT record in their respective responses.


        constructs a new OPTRR from a query
     */
    OPTRR(byte[] bytes) {
        logger.traceEntry();
        Assertion.aver(bytes[0] == 0);

        int location = 1;

        type = Utils.addThem(bytes[location++], bytes[location++]);
        Assertion.aver(type == 41);

        payloadSize = Utils.addThem(bytes[location++], bytes[location++]);
        logger.trace(payloadSize);

        extendedrcode = bytes[location++];
        logger.trace(extendedrcode);

        version = bytes[location++];
        Assertion.aver(version == 0);

        flags = Utils.addThem(bytes[location++], bytes[location++]);

        DNSSEC = flags >> 15 == 1; // DNSSEC OK bit as defined by [RFC3225].
        logger.trace(DNSSEC);

        rdLength = Utils.addThem(bytes[location++], bytes[location++]);
        logger.trace(rdLength);

        int total_length = location + rdLength;
        while (location < total_length) {
            optionCode = Utils.addThem(bytes[location++], bytes[location++]);
            logger.trace(optionCode);

            optionLength = Utils.addThem(bytes[location++], bytes[location++]);
            logger.trace(optionLength);

            // https://www.iana.org/assignments/dns-parameters/dns-parameters.xhtml#dns-parameters-11

            switch (optionCode) {
                case 10:
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
                    break;
                case 12:
                    location += optionLength;
                    break;
            }
        }
    }

    boolean hasCookie(){
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
    boolean hasFormErr(){
        return this.getOptionLength() < 8
            || (this.getOptionLength() > 8 && this.getOptionLength() < 16)
            || this.getOptionLength() > 40;
    }

    byte[] getBytes(){
        byte a[] = {(byte) 0x00};
        a = Utils.combine(a, Utils.getTwoBytes(this.type, 2));
        a = Utils.combine(a, Utils.getTwoBytes(this.payloadSize, 2));
        a = Utils.combine(a, this.extendedrcode);
        a = Utils.combine(a, this.version);
        a = Utils.combine(a, Utils.getTwoBytes(this.flags, 2));
        a = Utils.combine(a, Utils.getTwoBytes(this.rdLength, 2));
        if(rdLength >=5){
            a = Utils.combine(a, Utils.getTwoBytes(this.optionCode, 2));
            a = Utils.combine(a, Utils.getTwoBytes(this.optionLength, 2));
            a = Utils.combine(a, this.clientCookie);
            a = Utils.combine(a, this.serverCookie);
        }
        return a;
    }


    /*
     Modifies an OPTRR by creating a new server cookie
     from a valid client cookie
     adds this serverCookie to this OPTRR
     */
    void createServerCookie(String clientIPaddress, Header header) {
        ServerCookie sCookie = new ServerCookie(clientCookie, clientIPaddress);

        if (!Arrays.equals(sCookie.getBytes(), serverCookie)
                && serverCookie != null) {
            this.extendedrcode = ((byte) 0x01);
            header.setRcode(ErrorCodes.YXRRSET.getCode());
        }

        this.serverCookie = sCookie.getBytes();
        this.optionLength = (serverCookie.length + clientCookie.length);
        this.rdLength = (optionLength + 4);
    }
}
