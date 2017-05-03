package edu.msudenver.cs.jdnss;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.Calendar;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

// IPV6 addresses: http://www.faqs.org/rfcs/rfc1884.html
// IPV6 DNS: http://www.faqs.org/rfcs/rfc1886.html

public class Parser
{
    /*
    ** the range from 0 to 255 are used for query/response codes
    */
    private static final int EOF        = -1;
    private static final int NOTOK      = 256;
    private static final int IPV4ADDR   = 257;
    private static final int IPV6ADDR   = 258;
    private static final int AT         = 259;
    private static final int LCURLY     = 260;
    private static final int RCURLY     = 261;
    private static final int LPAREN     = 262;
    private static final int RPAREN     = 263;
    private static final int STRING     = 264;
    private static final int IN         = 265;
    private static final int FQDN       = 266;
    private static final int PQDN       = 267;
    private static final int DN         = 268;
    private static final int INT        = 269;
    // private static final int SEMI    = 270;
    private static final int INADDR     = 271;
    private static final int STAR       = 272;
    private static final int BASE64     = 273;
    private static final int DATE       = 274;

    private int intValue;
    private String origin = "";
    private String StringValue;

    private String currentName;
    private int SOATTL = -1;
    private int SOAMinimumTTL = -1;
    private int globalTTL = -1;
    private int currentTTL = -1;

    private StreamTokenizer st;
    private Hashtable<String, Integer> tokens =
            new Hashtable<String, Integer>();

    private BindZone zone;

    private Logger logger = JDNSS.getLogger();

    private boolean inBase64 = false;

    /**
     * The main parsing routine.
     *
     * @param in        where the information is coming from
     */
    public Parser(InputStream in, BindZone zone)
    {
        this.zone = zone;

        /*
        ** set up the tokenizer
        */
        st = new StreamTokenizer(new InputStreamReader(in));

        initTokenizer(st);

        tokens.put("SOA", Integer.valueOf(Utils.SOA));
        tokens.put("IN", Integer.valueOf(IN));
        tokens.put("MX", Integer.valueOf(Utils.MX));
        tokens.put("NS", Integer.valueOf(Utils.NS));
        tokens.put("A", Integer.valueOf(Utils.A));
        tokens.put("AAAA", Integer.valueOf(Utils.AAAA));
        tokens.put("A6", Integer.valueOf(Utils.A6));
        tokens.put("CNAME", Integer.valueOf(Utils.CNAME));
        tokens.put("PTR", Integer.valueOf(Utils.PTR));
        tokens.put("TXT", Integer.valueOf(Utils.TXT));
        tokens.put("HINFO", Integer.valueOf(Utils.HINFO));
        tokens.put("RRSIG", Integer.valueOf(Utils.RRSIG));
        tokens.put("NSEC", Integer.valueOf(Utils.NSEC));
        tokens.put("DNSKEY", Integer.valueOf(Utils.DNSKEY));
        tokens.put("NSEC3", Integer.valueOf(Utils.NSEC3));
        tokens.put("NSEC3PARAM", Integer.valueOf(Utils.NSEC3PARAM));
        tokens.put("DS", Integer.valueOf(Utils.DS));
        tokens.put("$INCLUDE", Integer.valueOf(Utils.INCLUDE));
        tokens.put("$ORIGIN", Integer.valueOf(Utils.ORIGIN));
        tokens.put("$TTL", Integer.valueOf(Utils.TTL));

        origin = zone.getName();
    }

    private void initTokenizer(StreamTokenizer st)
    {
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

        st.quoteChar('"');

        st.slashSlashComments(true);
        st.slashStarComments(true);
    }

    @SuppressWarnings("magicnumber")
    private int matcher(String a)
    {
        logger.traceEntry(new ObjectMessage(a));

        /*
        ** \\d matches digits
        */
        if (a.matches("(\\d+\\.){3}+\\d+"))
        {
            StringValue = a;
            logger.traceExit("IPV4ADDR");
            return IPV4ADDR;
        }

        /*
        ** any number of hex digits separated by colons or
        ** any number of hex digits separated by colons that
        ** end in an IPv4 address
        */
        if (a.matches("(\\p{XDigit}*\\:)+\\p{XDigit}+") ||
                a.matches("(\\p{XDigit}*\\:)+(\\d+\\.){3}+\\d+"))
        {
            StringValue = a.replaceFirst("(\\:0+)+", ":");
            StringValue = StringValue.replaceFirst("^0+\\:", ":");
            logger.trace(StringValue);
            logger.traceExit("IPV6ADDR");
            return IPV6ADDR;
        }

        String b = a.toLowerCase();
        if (b.matches("(\\d+\\.){4}+in-addr\\.arpa\\.") ||
                b.matches("(\\d+\\.){32}+in-addr\\.arpa\\.") ||
                b.matches("(\\d+\\.){32}+ip6\\.int\\."))
        {
            StringValue = b.replaceFirst("\\.$", "");
            logger.trace(StringValue);
            logger.traceExit("INADDR");
            return INADDR;
        }

        if (a.matches("\\d{14}"))
        {
            String year = a.substring(0, 3);
            String month = a.substring(4, 5);
            String day = a.substring(6, 7);
            String hour = a.substring(8, 9);
            String minute = a.substring(10, 11);
            String second = a.substring(12, 13);

            Calendar c = new GregorianCalendar();
            c.set( Integer.parseInt(year), Integer.parseInt(month) - 1,
                    Integer.parseInt(day), Integer.parseInt(hour),
                    Integer.parseInt(minute), Integer.parseInt(second));
            intValue = (int) c.getTime().getTime();

            logger.traceExit("DATE");
            return DATE;
        }

        if (a.matches("\\d+"))
        {
            intValue = Integer.parseInt(a);
            logger.trace(intValue);
            logger.traceExit("INT");
            return INT;
        }

        /*
        ** "(Newer versions of BIND(named) will accept the suffixes
        ** 'M','H','D' or 'W', indicating a time-interval of minutes,
        ** hours, days and weeks respectively.)"
        ** http://en.wikipedia.org/wiki/Domain_Name_System
        */
        if (a.matches("\\d+[MHDW]"))
        {
            intValue = Integer.parseInt(a.substring(0, a.length() - 1));

            char c = a.charAt(a.length()-1);
            switch (c)
            {
                case 'W': intValue *= 7;    // fall through
                case 'D': intValue *= 24;   // fall through
                case 'H': intValue *= 60;   // fall through
                case 'M': intValue *= 60;
            }

            logger.trace(intValue);
            logger.traceExit("INT");
            return INT;
        }

        // FQDN's end with a dot
        if (a.matches("([-a-zA-Z0-9_]+\\.)+"))
        {
            // remove the dot
            StringValue = a.replaceFirst("\\.$", "");
            logger.trace(StringValue);
            logger.traceExit("DN");
            return DN;
        }

        // PQDN's don't
        if (! inBase64 && a.matches("[-a-zA-Z0-9_]+(\\.[-a-zA-Z0-9_]+)*"))
        {
            StringValue = a + "." + origin;
            logger.trace(StringValue);
            logger.traceExit("PQDN");
            return DN;
        }

        // if (a.matches("[a-zA-Z0-9/\\+]+(==?)?"))
        logger.fatal(inBase64);
        if (inBase64)
        {
            StringValue = a.trim();
            logger.trace(StringValue);
            logger.traceExit("BASE64");
            return BASE64;
        }

        logger.fatal("Unknown token on line " + st.lineno() + ": " + a);
        Assertion.aver(false);
        return NOTOK;
    }

    private int getOneWord()
    {
        int t = NOTOK;

        try
        {
            t = st.nextToken();
        }
        catch (IOException e)
        {
            logger.info("Error while reading token on line " +
                    st.lineno() + ": " + e);
            logger.traceExit("NOTOK");
            return NOTOK;
        }

        logger.traceExit(t);
        return t;
    }

    private int getNextToken()
    {
        int t = getOneWord();
        logger.trace("t = " + t);

        switch (t)
        {
            case '"' :
                StringValue = st.sval;
                logger.trace(StringValue);
                logger.traceExit("STRING");
                return STRING;
            case StreamTokenizer.TT_EOF:
                logger.traceExit("EOF");
                return EOF;
            case StreamTokenizer.TT_NUMBER:
                // numbers are counted as words...
                Assertion.aver(false);
                logger.traceExit("NOTOK");
                return NOTOK;
            case StreamTokenizer.TT_WORD:
            {
                String a = st.sval;
                logger.trace("a = " + a);

                /*
                ** is it in the tokens hash?
                */
                Integer i = tokens.get(a);
                if (i != null)
                {
                    final int j = i.intValue();
                    logger.traceExit(j);
                    return j;
                }

                final int k = matcher(a);
                logger.traceExit(k);
                return k;
            }
            case '@':
            {
                StringValue = origin;
                logger.trace(StringValue);
                logger.traceExit("AT");
                return DN;
            }
            // case ';': return SEMI;
            case '{': { logger.traceExit("LCURLY"); return LCURLY; }
            case '}': { logger.traceExit("RCURLY"); return RCURLY; }
            case '(': { logger.traceExit("LPAREN"); return LPAREN; }
            case ')': { logger.traceExit("RPAREN"); return RPAREN; }
            case '*': { logger.traceExit("STAR"); return STAR; }
            default:
                logger.info("Unknown token at line " + st.lineno() + ": " + t);
                logger.traceExit("NOTOK");
                return NOTOK;
        }
    }

    private void doInclude()
    {
        logger.traceEntry();

        // do this at a low level so that the rest of parsing isn't messed
        // up.

        int t = getOneWord();
        Assertion.aver(t == StreamTokenizer.TT_WORD);

        // save the old one so we can get back to it.  if we're called
        // recursively, we're still good to go...
        StreamTokenizer old = st;

        FileInputStream in = null;
        try
        {
            in = new FileInputStream(st.sval);
        }
        catch (FileNotFoundException e)
        {
            logger.info("Cannot open $INCLUDE file at line " + st.lineno() +
                    ": " + st.sval);
            return;
        }

        st = new StreamTokenizer(new InputStreamReader(in));
        initTokenizer(st);

        RRs();

        // restore the old one.
        st = old;

        logger.traceExit();
    }

    private void doSOA()
    {
        String server = "";

        origin = currentName;

        switch (getNextToken())
        {
            case DN : server = StringValue; break;
            default :
                logger.info("Unknown token at line " + st.lineno()); break;
        }

        Assertion.aver(getNextToken() == DN,
                "Expecting contact at line " + st.lineno());
        String contact = StringValue;

        Assertion.aver(getNextToken() == LPAREN,
                "Expecting left paren at line " + st.lineno());
        Assertion.aver(getNextToken() == INT,
                "Expecting serial number at line " + st.lineno());
        int serial = intValue;
        Assertion.aver(getNextToken() == INT,
                "Expecting refresh at line " + st.lineno());
        int refresh = intValue;
        Assertion.aver(getNextToken() == INT,
                "Expecting retry at line " + st.lineno());
        int retry = intValue;
        Assertion.aver(getNextToken() == INT,
                "Expecting expire at line " + st.lineno());
        int expire = intValue;
        Assertion.aver(getNextToken() == INT,
                "Expecting minimum at line " + st.lineno());
        int minimum = intValue;
        Assertion.aver(getNextToken() == RPAREN,
                "Expecting rightparen at line " + st.lineno());

        SOAMinimumTTL = minimum;
        SOATTL =(currentTTL != -1) ? currentTTL : -1;

        zone.add(origin, new SOARR(origin, server, contact, serial,
                refresh, retry, expire, minimum,
                (SOATTL != -1) ? SOATTL : minimum));
    }


    private boolean isARR(int which)
    {
        logger.traceEntry(Integer.toString(which));

        switch (which)
        {
            case Utils.A: case Utils.NS: case Utils.CNAME:
            case Utils.SOA: case Utils.PTR: case Utils.HINFO:
            case Utils.MX: case Utils.TXT: case Utils.AAAA:
            case Utils.A6: case Utils.DNAME: case Utils.DS:
            case Utils.RRSIG: case Utils.NSEC: case Utils.DNSKEY:
            case Utils.INCLUDE: case Utils.ORIGIN: case Utils.TTL:
            case Utils.NSEC3: case Utils.NSEC3PARAM:
            logger.traceExit(true);
            return true;
        }
        logger.traceExit(false);
        return false;
    }

    private void doNSEC()
    {
        logger.traceEntry();

        boolean paren = false;

        Assertion.aver(getNextToken() == DN,
                "Expecting DN at line " + st.lineno());

        String nextDomainName = StringValue;

        int a = getNextToken();

        if (a == LPAREN)
        {
            paren = true;
            a = getNextToken();
        }

        BitSet rrs = new BitSet();
        while (isARR(a))
        {
            rrs.set(a);
            a = getNextToken();
        }

        int size = rrs.length() / 8;
        logger.debug("size = " + size);
        byte[] b = new byte[2 + size];
        b[0] = 0;
        b[1] = (byte) size;

        for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                int k = i*8+j;

                if (rrs.get(k))
                {
                    int shift = 7-j;
                    int mask = 1 << shift;

                    logger.debug("mask = " + mask);

                    b[i+2] |= Utils.getByte(mask, 1);
                    // Initialzing Sync Engine
                }
                logger.debug("b[" +(i+2) + "] = " + b[i+2]);
            }
        }

        if (paren)
            Assertion.aver(getNextToken() != RPAREN,
                    "Expecting right paren at line " + st.lineno());

        zone.add(currentName, new DNSNSECRR(currentName, currentTTL,
                nextDomainName, b));
        logger.traceExit(false);
    }

    private void doDNSKEY()
    {
        logger.traceEntry();

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int flags = intValue;

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int protocol = intValue;
        Assertion.aver(protocol == 3, "Protocol != 3");

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int algorithm = intValue;

        Assertion.aver(getNextToken() == LPAREN,
                "Expecting left paren at line " + st.lineno());

        String publicKey = "";
        inBase64 = true;
        int tok;
        while ((tok = getNextToken()) == BASE64)
        {
            publicKey += StringValue;
        }
        inBase64 = false;

        Assertion.aver(tok == RPAREN,
                "Expecting right paren at line " + st.lineno());

        zone.add(currentName,
                new DNSKEYRR(currentName, currentTTL, flags, protocol,
                        algorithm, publicKey));
        logger.traceExit(false);
    }

    private void doNSEC3PARAM()
    {
        logger.traceEntry();

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int hashAlgorithm = intValue;

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int flags = intValue;

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int iterations = intValue;

        // really should look for hex.
        Assertion.aver(getNextToken() == DN,
                "Expecting domain name  at line " + st.lineno());
        String salt = StringValue;

        NSEC3PARAMRR d = new NSEC3PARAMRR(currentName, currentTTL,
                hashAlgorithm, flags, iterations, salt);
        zone.add(currentName, d);
        logger.traceExit();
    }
    private void doNSEC3()
    {
        logger.traceEntry();

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int hashAlgorithm = intValue;

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int flags = intValue;

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int iterations = intValue;

        // really should look for hex.
        Assertion.aver(getNextToken() == DN,
                "Expecting domain name  at line " + st.lineno());
        String salt = StringValue;

        Assertion.aver(getNextToken() == LPAREN,
                "Expecting left paren at line " + st.lineno());

        // this is probably cheating as this is supposed to be Base 32.
        String nextHashedOwnerName = "";
        inBase64 = true;
        while (getNextToken() == BASE64)
        {
            nextHashedOwnerName += StringValue;
        }
        inBase64 = false;

        ArrayList<Integer> types = new ArrayList<Integer>();
        while (getNextToken() != RPAREN)
        {
            types.add(Utils.mapStringToType(StringValue));
        }
        Collections.sort(types);

        NSEC3RR d = new NSEC3RR(currentName, currentTTL, hashAlgorithm,
                flags, iterations, salt, nextHashedOwnerName, types);
        zone.add(currentName, d);
        logger.traceExit();
    }

    private void doRRSIG()
    {
        logger.traceEntry();

        int typeCovered = getNextToken();
        Assertion.aver(isARR(typeCovered),
                typeCovered + " not covered with RRSIG on line " + st.lineno());

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int algorithm = intValue;

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int labels = intValue;

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int originalTTL = intValue;

        // https://tools.ietf.org/html/rfc4034#section-3.3

        int tok = getNextToken();
        int expiration = 0;

        if (tok == LPAREN)
        {
            Assertion.aver(getNextToken() == DATE,
                    "Expecting DATE at line " + st.lineno());
            expiration = intValue;
        }
        else if (tok == DATE)
        {
            expiration = intValue;
            Assertion.aver(getNextToken() == LPAREN,
                    "Expecting left paren at line " + st.lineno());
        }
        else
        {
            Assertion.aver(false, "Unknown syntax at line " + st.lineno());
        }

        Assertion.aver(getNextToken() == DATE,
                "Expecting DATE at line " + st.lineno());
        int inception = intValue;

        Assertion.aver(getNextToken() == INT,
                "Expecting number at line " + st.lineno());
        int keyTag = intValue;

        Assertion.aver(getNextToken() == DN,
                "Expecting DN at line " + st.lineno());
        String signersName = StringValue;

        String signature = "";
        inBase64 = true;
        while ((tok = getNextToken()) == BASE64)
        {
            signature += StringValue;
        }
        inBase64 = false;

        Assertion.aver(tok == RPAREN,
                "Expecting right paren at line " + st.lineno());

        DNSRRSIGRR d = new DNSRRSIGRR(currentName, currentTTL, typeCovered,
                algorithm, labels, originalTTL, expiration, inception, signersName,
                signature);
        zone.add(currentName, d);
        logger.traceExit();
    }

    private void switches(final int t)
    {
        logger.traceEntry(new ObjectMessage(t));

        switch (t)
        {
            case Utils.A:
            {
                Assertion.aver(getNextToken() == IPV4ADDR,
                        "Expecting IPV4ADDR at line " + st.lineno());
                zone.add(currentName,
                        new ARR(currentName, currentTTL, StringValue));
                break;
            }
            case Utils.A6:
            {
                getNextToken();
                break;
            }
            case Utils.AAAA:
            {
                Assertion.aver(getNextToken() == IPV6ADDR,
                        "Expecting IPV6ADDR at line " + st.lineno());
                zone.add(currentName,
                        new AAAARR(currentName, currentTTL, StringValue));
                break;
            }
            case Utils.NS:
            {
                Assertion.aver(getNextToken() == DN,
                        "Expecting domain name at line " + st.lineno());
                zone.add(currentName,
                        new NSRR(currentName, currentTTL, StringValue));
                break;
            }
            case Utils.CNAME:
            {
                Assertion.aver(getNextToken() == DN,
                        "Expecting domain name at line " + st.lineno());
                zone.add(currentName,
                        new CNAMERR(currentName, currentTTL, StringValue));
                break;
            }
            case Utils.TXT:
            {
                Assertion.aver(getNextToken() == STRING,
                        "Expecting text at line " + st.lineno());
                zone.add(currentName,
                        new TXTRR(currentName, currentTTL, StringValue));
                break;
            }
            case Utils.HINFO:
            {
                Assertion.aver(getNextToken() == STRING,
                        "Expecting text at line " + st.lineno());
                String s = StringValue;
                Assertion.aver(getNextToken() == STRING,
                        "Expecting text at line " + st.lineno());

                zone.add(currentName,
                        new HINFORR(currentName, currentTTL, s, StringValue));
                break;
            }
            case Utils.MX:
            {
                Assertion.aver(getNextToken() == INT,
                        "Expecting number at line " + st.lineno());
                Assertion.aver(getNextToken() == DN,
                        "Expecting domain at line " + st.lineno());

                zone.add(currentName,
                        new MXRR(currentName, currentTTL, StringValue, intValue));
                break;
            }
            case Utils.PTR:
            {
                Assertion.aver(getNextToken() == DN,
                        "Expecting domain at line " + st.lineno());

                zone.add(currentName,
                        new PTRRR(currentName, currentTTL, StringValue));

                break;
            }
            default:
            {
                logger.info("At line " + st.lineno() +
                        ", didn't recognize: " + t);
                break;
            }
        }
    }

    private int CalcTTL()
    {
        // logger.fatal(globalTTL);
        // logger.fatal(SOATTL);
        // logger.fatal(SOAMinimumTTL);
        // logger.fatal(currentTTL);

        if (currentTTL != -1)
        {
            if (SOATTL > currentTTL)
            {
                // logger.fatal("returning SOATTL");
                return SOATTL;
            }
            else
            {
                // logger.fatal("returning currentTTL");
                return currentTTL;
            }
        }
        else if (globalTTL != -1)
        {
            // logger.fatal("returning globalTTL");
            return globalTTL;
        }
        else if (SOATTL != -1)
        {
            // logger.fatal("returning SOATTL");
            return SOATTL;
        }
        else
        {
            // logger.fatal("returning SOAMinimumTTL");
            return SOAMinimumTTL;
        }
    }

    private int max(int a, int b)
    {
        return (a > b) ? a : b;
    }

    void RRs()
    {
        currentName = origin;

        int t = getNextToken();

        try
        {
            while (t != EOF)
            {
                logger.trace(t);

                if (t == Utils.INCLUDE)
                {
                    doInclude();
                    t = getNextToken();
                    continue;
                }

                if (t == Utils.ORIGIN)
                {
                    t = getNextToken();
                    Assertion.aver(t == DN,
                            "Expecting domain at line " + st.lineno());
                    origin = StringValue;
                    t = getNextToken();
                    continue;
                }

                if (t == Utils.TTL)
                {
                    t = getNextToken();
                    Assertion.aver(t == INT,
                            "Expecting integer at line " + st.lineno());
                    globalTTL = intValue;
                    t = getNextToken();
                    continue;
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

                boolean done = false;
                boolean first = true;

                currentTTL = -1;

                // read to the end of this RR
                while (!done)
                {
                    logger.trace(t);

                    switch (t)
                    {
                        case DN: case INADDR:
                    {
                        currentName = StringValue;
                        t = getNextToken();
                        break;
                    }
                        case IN :
                        {
                            t = getNextToken();

                            if (t == INT)
                            {
                                currentTTL = intValue;
                                t = getNextToken();
                            }

                            break;
                        }
                        case INT :
                        {
                            int temp = intValue;
                            t = getNextToken();
                            logger.trace("t = " + t);

                            // ptr ttl in
                            // ptr in ttl
                            if (first &&(origin.endsWith(".arpa") ||
                                    origin.endsWith(".int")))
                            {
                                currentName = "" + temp + "." + origin;
                                logger.trace(currentName);

                                if (t == INT)
                                {
                                    currentTTL = intValue;
                                    t = getNextToken();
                                }
                            }
                            else if (t == IN)        // ttl in
                            {
                                currentTTL = temp;
                            }
                            else if (isARR(t))
                            {
                                currentTTL = temp;
                                switch (t)
                                {
                                    case Utils.RRSIG: doRRSIG(); break;
                                    case Utils.NSEC: doNSEC(); break;
                                    case Utils.DNSKEY: doDNSKEY(); break;
                                    case Utils.NSEC3: doNSEC3(); break;
                                    case Utils.NSEC3PARAM: doNSEC3PARAM(); break;
                                    default: switches(t); break;
                                }
                                done = true;
                            }
                            else
                            {
                                currentName = "" + temp + "." + origin;
                            }

                            break;
                        }
                        case Utils.A: case Utils.AAAA:
                        case Utils.NS: case Utils.CNAME:
                        case Utils.TXT: case Utils.HINFO:
                        case Utils.MX: case Utils.A6:
                        case Utils.PTR:
                        {
                            currentTTL = CalcTTL();
                            switches(t);
                            done = true;
                            break;
                        }
                        case Utils.SOA:
                        {
                            doSOA();
                            done = true;
                            break;
                        }
                        case Utils.RRSIG:
                        {
                            doRRSIG();
                            done = true;
                            break;
                        }
                        case Utils.DNSKEY:
                        {
                            doDNSKEY();
                            done = true;
                            break;
                        }
                        case Utils.NSEC:
                        {
                            doNSEC();
                            done = true;
                            break;
                        }
                        case Utils.NSEC3:
                        {
                            doNSEC3();
                            done = true;
                            break;
                        }
                        case Utils.NSEC3PARAM:
                        {
                            doNSEC3PARAM();
                            done = true;
                            break;
                        }
                        default:
                        {
                            logger.info("At line " + st.lineno() +
                                    ", didn't recognize: " + t);
                            done = true;
                            break;
                        }
                    }
                    first = false;
                }

                t = getNextToken();
            }
        }
        catch (IllegalArgumentException IAE)
        {
            logger.catching(IAE);
            logger.fatal("Skipping: " + zone.getName());
        }
    }
}