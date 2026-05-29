package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

// IPV6 addresses: http://www.faqs.org/rfcs/rfc1884.html
// IPV6 DNS: http://www.faqs.org/rfcs/rfc1886.html

class Parser {
    private static final class RecordProgress {
        private final RRCode token;
        private final boolean done;

        private RecordProgress(final RRCode token, final boolean done) {
            this.token = token;
            this.done = done;
        }
    }

    private static final int NOTOK = -1;

    private static final Set<RRCode> RR_TOKENS = EnumSet.of(
            RRCode.A,
            RRCode.NS,
            RRCode.CNAME,
            RRCode.SOA,
            RRCode.PTR,
            RRCode.HINFO,
            RRCode.MX,
            RRCode.TXT,
            RRCode.AAAA,
            RRCode.A6,
            RRCode.DNAME,
            RRCode.DS,
            RRCode.RRSIG,
            RRCode.NSEC,
            RRCode.DNSKEY,
            RRCode.INCLUDE,
            RRCode.ORIGIN,
            RRCode.TTL,
            RRCode.NSEC3,
            RRCode.NSEC3PARAM);

    private static final int DAYSINWEEK = 7;
    private static final int HOURSINDAY = 24;
    private static final int MINUTESINHOUR = 60;
    private static final int SECONDSINMINUTE = 60;

    private int intValue;
    private String origin;
    private String stringValue;

    private String currentName;
    private int SOATTL = -1;
    private int SOAMinimumTTL = -1;
    private int globalTTL = -1;
    private int currentTTL = -1;

    private StreamTokenizer st;
    private final Map<String, RRCode> tokens = new Hashtable<>();

    private final BindZone zone;
    private final Logger logger = JDNSS.logger;
    private boolean inBase64;

    /**
     * The main parsing routine.
     *
     * @param in where the information is coming from
     */
    Parser(final InputStream in, final BindZone zone) throws UnsupportedEncodingException {
        this.zone = zone;

        /*
        ** set up the tokenizer
        */
        st = new StreamTokenizer(new InputStreamReader(in, "UTF-8"));

        initTokenizer(st);

        tokens.put("SOA", RRCode.SOA);
        tokens.put("IN", RRCode.IN);
        tokens.put("MX", RRCode.MX);
        tokens.put("NS", RRCode.NS);
        tokens.put("A", RRCode.A);
        tokens.put("AAAA", RRCode.AAAA);
        tokens.put("A6", RRCode.A6);
        tokens.put("CNAME", RRCode.CNAME);
        tokens.put("PTR", RRCode.PTR);
        tokens.put("TXT", RRCode.TXT);
        tokens.put("HINFO", RRCode.HINFO);
        tokens.put("RRSIG", RRCode.RRSIG);
        tokens.put("NSEC", RRCode.NSEC);
        tokens.put("DNSKEY", RRCode.DNSKEY);
        tokens.put("NSEC3", RRCode.NSEC3);
        tokens.put("NSEC3PARAM", RRCode.NSEC3PARAM);
        tokens.put("DS", RRCode.DS);
        tokens.put("$INCLUDE", RRCode.INCLUDE);
        tokens.put("$ORIGIN", RRCode.ORIGIN);
        tokens.put("$TTL", RRCode.TTL);

        origin = zone.getName();
    }

    private void initTokenizer(final StreamTokenizer st) {
        st.commentChar(';');

        /*
        ** putting 0-9 into wordChars doesn't work unless one
        ** first makes them ordinary.  weird.
        */
        st.ordinaryChars('0', '9');
        st.wordChars('0', '9');
        st.wordChars('.', '.');
        st.wordChars(':', ':');
        st.wordChars('$', '$');
        st.wordChars('\\', '\\');
        st.wordChars('[', '[');
        st.wordChars(']', ']');
        st.wordChars('/', '/');
        st.wordChars('+', '+');
        st.wordChars('=', '=');
        st.wordChars('_', '_');

        st.quoteChar('"');

        st.slashSlashComments(true);
        st.slashStarComments(true);
    }

    private RRCode matcher(final String a) {
        logger.traceEntry(new ObjectMessage(a));

        /*
        ** \\d matches digits
        */
        if (a.matches("(\\d+\\.){3}+\\d+")) {
            stringValue = a;
            logger.traceExit("IPV4ADDR");
            return RRCode.IPV4ADDR;
        }

        /*
        ** any number of hex digits separated by colons or
        ** any number of hex digits separated by colons that
        ** end in an IPv4 address
        */
        if (a.matches("(\\p{XDigit}*:)+\\p{XDigit}+")
                || a.matches("(\\p{XDigit}*:)+(\\d+\\.){3}+\\d+")) {
            stringValue = a.replaceFirst("(:0+)+", ":");
            stringValue = stringValue.replaceFirst("^0+:", ":");
            logger.trace(stringValue);
            logger.traceExit("IPV6ADDR");
            return RRCode.IPV6ADDR;
        }

        final String b = a.toLowerCase();
        if (b.matches("(\\d+\\.){4}+in-addr\\.arpa\\.")
                || b.matches("(\\d+\\.){32}+in-addr\\.arpa\\.")
                || b.matches("(\\d+\\.){32}+ip6\\.int\\.")) {
            stringValue = b.replaceFirst("\\.$", "");
            logger.trace(stringValue);
            logger.traceExit("INADDR");
            return RRCode.INADDR;
        }

        if (a.matches("\\d{14}")) {
            calculateDate(a);
            logger.traceExit("DATE");
            return RRCode.DATE;
        }

        if (a.matches("\\d+")) {
            intValue = Integer.parseInt(a);
            logger.trace(intValue);
            logger.traceExit("INT");
            return RRCode.INT;
        }

        /*
        ** "(Newer versions of BIND(named) will accept the suffixes
        ** 'M','H','D' or 'W', indicating a time-interval of minutes,
        ** hours, days and weeks respectively.)"
        */
        if (a.matches("\\d+[MHDW]")) {
            calculateMDWM(a);
            logger.trace(intValue);
            logger.traceExit("INT");
            return RRCode.INT;
        }

        if (a.matches("[0-9A-Fa-f]+")) {
            stringValue = a;
            return RRCode.HEX;
        }

        // FQDN's end with a dot
        if (a.matches("([-a-zA-Z0-9_]+\\.)+")) {
            // remove the dot
            stringValue = a.replaceFirst("\\.$", "");
            logger.trace(stringValue);
            logger.traceExit("DN");
            return RRCode.DN;
        }

        // PQDN's don't
        if (!inBase64 && a.matches("[-a-zA-Z0-9_]+(\\.[-a-zA-Z0-9_]+)*")) {
            stringValue = a + "." + origin;
            logger.trace(stringValue);
            logger.traceExit("PQDN");
            return RRCode.DN;
        }

        // if (a.matches("[a-zA-Z0-9/\\+]+(==?)?"))
        String pattern = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$";
        if (inBase64 && a.matches(pattern)) {
            stringValue = a.trim();
            logger.trace(stringValue);
            logger.traceExit("BASE64");
            return RRCode.BASE64;
        }

        logger.fatal("Unknown token on line " + st.lineno() + ": " + a);
        return RRCode.NOTOK;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    private void calculateMDWM(final String a) {
        intValue = Integer.parseInt(a.substring(0, a.length() - 1));


        char MDWM = a.charAt(a.length() - 1);
        switch (MDWM) {
            case 'W':
                intValue *= DAYSINWEEK;    // fall through
            case 'D':
                intValue *= HOURSINDAY;   // fall through
            case 'H':
                intValue *= MINUTESINHOUR;   // fall through
            case 'M':
                intValue *= SECONDSINMINUTE;
                break;
            default:
                logger.error("Shouldn't get here");
        }
    }

    private void calculateDate(final String a) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss:z");
            Date dt = sdf.parse(a + ":UTC");
            logger.trace(dt);
            long epoch = dt.getTime();
            intValue = (int) (epoch / 1000);
            logger.trace(Integer.toHexString(intValue));
        }
        catch(ParseException e){
            assert false;
        }
    }

    private int getOneWord() {
        final int t;

        try {
            t = st.nextToken();
        } catch (IOException e) {
            logger.info("Error while reading token on line "
                    + st.lineno() + ": " + e);
            logger.traceExit("NOTOK");
            return NOTOK;
        }

        logger.traceExit(t);
        return t;
    }

    private RRCode getNextToken() {
        final int t = getOneWord();
        logger.trace("t = " + t);

        switch (t) {
            case '"':
                stringValue = st.sval;
                logger.trace(stringValue);
                logger.traceExit("STRING");
                return RRCode.STRING;
            case StreamTokenizer.TT_EOF:
                logger.traceExit("EOF");
                return RRCode.EOF;
            case StreamTokenizer.TT_NUMBER:
                // numbers are counted as words...
                logger.traceExit("NOTOK");
                return RRCode.NOTOK;
            case StreamTokenizer.TT_WORD: {
                final String a = st.sval;
                logger.trace("a = " + a);

                /*
                ** is it in the tokens hash?
                */
                final RRCode i = tokens.get(a);
                if (i != null) {
                    return i;
                }

                final RRCode k = matcher(a);
                logger.traceExit(k);
                return k;
            }
            case '@':
                stringValue = origin;
                logger.trace(stringValue);
                logger.traceExit("AT");
                return RRCode.DN;
            case '{':
                logger.traceExit("LCURLY");
                return RRCode.LCURLY;
            case '}':
                logger.traceExit("RCURLY");
                return RRCode.RCURLY;
            case '(':
                logger.traceExit("LPAREN");
                return RRCode.LPAREN;
            case ')':
                logger.traceExit("RPAREN");
                return RRCode.RPAREN;
            case '*':
                logger.traceExit("STAR");
                return RRCode.STAR;
            default:
                logger.info("Unknown token at line " + st.lineno() + ": " + t);
                logger.traceExit("NOTOK");
                return RRCode.NOTOK;
        }
    }

    private void doInclude() throws UnsupportedEncodingException {
        logger.traceEntry();

        // do this at a low level so that the rest of parsing isn't messed
        // up.

        final int t = getOneWord();
        assert t == StreamTokenizer.TT_WORD;

        // save the old one so we can get back to it.  if we're called
        // recursively, we're still good to go...
        final StreamTokenizer old = st;

        final FileInputStream in;
        try {
            in = new FileInputStream(st.sval);
        } catch (FileNotFoundException e) {
            logger.info("Cannot open $INCLUDE file at line " + st.lineno()
                    + ": " + st.sval);
            return;
        }

        st = new StreamTokenizer(new InputStreamReader(in, "UTF-8"));
        initTokenizer(st);

        RRs();

        // restore the old one.
        st = old;

        logger.traceExit();
    }

    private void doSOA() {
        origin = currentName;

        final String server = getDomain();
        final String contact = getDomain();
        getLeftParen();
        final int serial = getInt("serial");
        final int refresh = getInt("refresh");
        final int retry = getInt("retry");
        final int expire = getInt("expire");
        final int minimum = getInt("minimum");
        getRightParen();

        SOAMinimumTTL = minimum;
        SOATTL = currentTTL;

        zone.add(origin, new SOARR(origin, server, contact, serial,
                refresh, retry, expire, minimum,
                (SOATTL != -1) ? SOATTL : minimum));
    }


    private boolean isARR(final RRCode which) {
        final boolean isRecordToken = RR_TOKENS.contains(which);
        logger.traceExit(isRecordToken);
        return isRecordToken;
    }

    private void doNSEC() {
        logger.traceEntry();

        boolean paren = false;

        String nextDomainName = getDomain();

        RRCode a = getNextToken();

        // if there's more than one, they start with a paren
        if (a == RRCode.LPAREN) {
            paren = true;
            a = getNextToken();
        }

        Set<RRCode> resourceRecords = EnumSet.noneOf(RRCode.class);
        while (isARR(a)) {
            resourceRecords.add(a);
            a = getNextToken();
        }

        if (paren) {
            getRightParen();
        }

        zone.add(currentName, new NSECRR(currentName, currentTTL, nextDomainName,
                resourceRecords));
        logger.traceExit(false);
    }

    private void doDNSKEY() {
        logger.traceEntry();

        final int flags = getInt("flags");
        final int protocol = getInt("protocol");
        assert protocol == 3;
        final int algorithm = getInt("algorithm");
        getLeftParen();

        final StringBuilder publicKeyBuilder = new StringBuilder();
        inBase64 = true;
        RRCode tok;
        while ((tok = getNextToken()) == RRCode.BASE64) {
            publicKeyBuilder.append(stringValue);
        }
        inBase64 = false;

        assert tok == RRCode.RPAREN;

        zone.add(currentName,
                new DNSKEYRR(currentName, currentTTL, flags, protocol,
                        algorithm, publicKeyBuilder.toString()));
        logger.traceExit(false);
    }

    private void doNSEC3PARAM() {
        logger.traceEntry();

        final int hashAlgorithm = getInt("hash algorithm");
        final int flags = getInt("flags");
        final int iterations = getInt("interations");

        final String salt = getHex();

        final NSEC3PARAMRR d = new NSEC3PARAMRR(currentName, currentTTL,
                hashAlgorithm, flags, iterations, salt);
        zone.add(currentName, d);
        logger.traceExit();
    }

    private void doNSEC3() {
        logger.traceEntry();

        final int hashAlgorithm = getInt("hash algorithm");
        final int flags = getInt("flags");
        final int iterations = getInt("iterations");

        final String salt = getHex();
        getLeftParen();

        // this is probably cheating as this is supposed to be Base 32.
        final StringBuilder nextHashedOwnerNameBuilder = new StringBuilder();
        inBase64 = true;
        while (getNextToken() == RRCode.BASE64) {
            nextHashedOwnerNameBuilder.append(stringValue);
        }
        inBase64 = false;

        final Set<RRCode> types = EnumSet.noneOf(RRCode.class);
        RRCode rrcode;
        while ((rrcode = getNextToken()) != RRCode.RPAREN) {
            types.add(rrcode);
        }

        final NSEC3RR d = new NSEC3RR(currentName, currentTTL, hashAlgorithm,
        flags, iterations, salt, nextHashedOwnerNameBuilder.toString(), types);
        zone.add(currentName, d);
        logger.traceExit();
    }


    private void doRRSIG() {
        logger.traceEntry();

        final RRCode typeCovered = getNextToken();
        assert isARR(typeCovered): typeCovered + " not covered with RRSIG on line " + st.lineno();

        final int algorithm = getInt("algorithm");
        final int labels = getInt("labels");
        final int originalTTL = getInt("original TTL");

        // https://tools.ietf.org/html/rfc4034#section-3.3

        getLeftParen();

        final int expiration = getDate();
        final int inception = getDate();
        final int keyTag = getInt("key tag");
        final String signersName = getDomain();

        final StringBuilder signatureBuilder = new StringBuilder();
        RRCode tok;

        inBase64 = true;
        while ((tok = getNextToken()) == RRCode.BASE64) {
            signatureBuilder.append(stringValue);
        }
        inBase64 = false;
        assert tok == RRCode.RPAREN;

        final RRSIG d = new RRSIG(currentName, currentTTL, typeCovered,
                algorithm, labels, originalTTL, expiration, inception,
                keyTag, signersName, signatureBuilder.toString());
        zone.add(currentName, d);
        logger.traceExit();
    }

    private void switches(final RRCode t) {
        logger.traceEntry(new ObjectMessage(t));

        switch (t) {
            case A:
                zone.add(currentName, new ARR(currentName, currentTTL,
                        getIPV4ADDR()));
                break;
            case A6:
                // deprecated
                getNextToken();
                break;
            case AAAA:
                zone.add(currentName, new AAAARR(currentName, currentTTL,
                        getIPV6ADDR()));
                break;
            case NS:
                zone.add(currentName, new NSRR(currentName, currentTTL,
                        getDomain()));
                break;
            case CNAME:
                zone.add(currentName, new CNAMERR(currentName, currentTTL,
                        getDomain()));
                break;
            case TXT:
                zone.add(currentName, new TXTRR(currentName, currentTTL,
                        getString()));
                break;
            case HINFO:
                final String first = getString();
                final String second = getString();
                zone.add(currentName, new HINFORR(currentName, currentTTL,
                        first, second));
                break;
            case MX:
                final int preference = getInt("preference");
                final String exchanger = getDomain();
                zone.add(currentName, new MXRR(currentName, currentTTL,
                        exchanger, preference));
                break;
            case PTR:
                zone.add(currentName, new PTRRR(currentName, currentTTL,
                        getDomain()));
                break;
            case RRSIG:
                doRRSIG();
                break;
            case NSEC:
                doNSEC();
                break;
            case DNSKEY:
                doDNSKEY();
                break;
            case NSEC3:
                doNSEC3();
                break;
            case NSEC3PARAM:
                doNSEC3PARAM();
                break;
            default:
                logger.info("At line " + st.lineno() + ", didn't recognize: "
                        + t);
                break;
        }
    }

    private int CalcTTL() {
        if (currentTTL != -1) {
            return Math.max(SOATTL, currentTTL);
        } else if (globalTTL != -1) {
            return globalTTL;
        } else if (SOATTL != -1) {
            return SOATTL;
        } else {
            return SOAMinimumTTL;
        }
    }

    private boolean isReverseLookupOrigin() {
        return origin.endsWith(".arpa") || origin.endsWith(".int");
    }

    private boolean handleDirectiveToken(final RRCode token) throws UnsupportedEncodingException {
        switch (token) {
            case INCLUDE:
                doInclude();
                return true;
            case ORIGIN:
                origin = getDomain();
                return true;
            case TTL:
                globalTTL = getInt("TTL");
                return true;
            default:
                return false;
        }
    }

    private void parseOneRecord(final RRCode startToken) {
        boolean done = false;
        boolean first = true;
        RRCode token = startToken;

        currentTTL = -1;

        // read to the end of this RR
        while (!done) {
            logger.trace(token);

            if (isImmediateRecordToken(token)) {
                handleImmediateRecordToken(token);
                done = true;
                first = false;
                continue;
            }

            switch (token) {
                case DN:
                case INADDR:
                    token = handleNameToken();
                    break;
                case IN:
                    token = handleInToken();
                    break;
                case INT: {
                    final RecordProgress progress = handleIntegerToken(first);
                    token = progress.token;
                    done = progress.done;

                    break;
                }
                case SOA:
                    doSOA();
                    done = true;
                    break;
                default:
                    logger.info("At line " + st.lineno()
                            + ", didn't recognize: " + token);
                    done = true;
                    break;
            }
            first = false;
        }
    }

    private RRCode handleNameToken() {
        currentName = stringValue;
        return getNextToken();
    }

    private RRCode handleInToken() {
        RRCode token = getNextToken();
        if (token == RRCode.INT) {
            currentTTL = intValue;
            token = getNextToken();
        }
        return token;
    }

    private boolean isImmediateRecordToken(final RRCode token) {
        switch (token) {
            case A:
            case AAAA:
            case NS:
            case CNAME:
            case TXT:
            case HINFO:
            case MX:
            case A6:
            case PTR:
            case RRSIG:
            case NSEC:
            case NSEC3:
            case NSEC3PARAM:
            case DNSKEY:
                return true;
            default:
                return false;
        }
    }

    private void handleImmediateRecordToken(final RRCode token) {
        currentTTL = CalcTTL();
        switches(token);
    }

    private RecordProgress handleIntegerToken(final boolean firstRecord) {
        final int temp = intValue;
        RRCode nextToken = getNextToken();
        logger.trace("t = " + nextToken);

        // ptr ttl in
        // ptr in ttl
        if (firstRecord && isReverseLookupOrigin()) {
            currentName = "" + temp + "." + origin;
            logger.trace(currentName);

            if (nextToken == RRCode.INT) {
                currentTTL = intValue;
                nextToken = getNextToken();
            }
            return new RecordProgress(nextToken, false);
        }

        if (nextToken == RRCode.IN) {
            currentTTL = temp;
            return new RecordProgress(nextToken, false);
        }

        if (isARR(nextToken)) {
            currentTTL = temp;
            switches(nextToken);
            return new RecordProgress(nextToken, true);
        }

        currentName = "" + temp + "." + origin;
        return new RecordProgress(nextToken, false);
    }

    void RRs() throws UnsupportedEncodingException {
        currentName = origin;

        RRCode t = getNextToken();

        try {
            while (t != RRCode.EOF) {
                logger.trace(t);

                t = parseTokenAndGetNext(t);
            }
        } catch (IllegalArgumentException IAE) {
            logger.catching(IAE);
            logger.fatal("Skipping: " + zone.getName());
        }
    }

    private RRCode parseTokenAndGetNext(final RRCode token)
            throws UnsupportedEncodingException {
        if (handleDirectiveToken(token)) {
            return getNextToken();
        }

        // 5.1. Format
        // [name]        [ttl]        [class]        type        data
        // [name]        [class]        [ttl]        type        data
        // 1                IN        1H        PTR        @
        // 1) name ttl class type data
        // 2) name class type data
        // 3) name type data
        // 4) ttl class type data
        // 5) ttl type data
        // 6) class type data
        // 7) type data
        parseOneRecord(token);
        return getNextToken();
    }

    private int getInt(final String message) {
        RRCode token = getNextToken();
        assert token == RRCode.INT: "Expecting " + message + " at line " + st.lineno();
        return intValue;
    }

    private void getLeftParen() {
        RRCode token = getNextToken();
        assert token == RRCode.LPAREN: "Expecting left paren at line " + st.lineno();
    }

    private void getRightParen() {
        RRCode token = getNextToken();
        assert token == RRCode.RPAREN: "Expecting right paren at line " + st.lineno();
    }

    private String getString() {
        RRCode token = getNextToken();
        assert token == RRCode.STRING: "Expecting string at line " + st.lineno();
        return stringValue;
    }

    private String getDomain() {
        RRCode token = getNextToken();
        assert token == RRCode.DN: "Expecting domain at line " + st.lineno();
        return stringValue;
    }

    private String getHex() {
        RRCode token = getNextToken();
        assert token == RRCode.HEX: "Expecting hexadecimal at line " + st.lineno();
        return stringValue;
    }

    private int getDate() {
        RRCode token = getNextToken();
        assert token == RRCode.DATE: "Expecting date at line " + st.lineno();
        return intValue;
    }

    private String getIPV4ADDR() {
        RRCode token = getNextToken();
        assert token == RRCode.IPV4ADDR: "Expecting IPV4ADDR at line " + st.lineno();
        return stringValue;
    }

    private String getIPV6ADDR() {
        RRCode token = getNextToken();
        assert token == RRCode.IPV6ADDR: "Expecting IPV6ADDR at line " + st.lineno();
        return stringValue;
    }
}
