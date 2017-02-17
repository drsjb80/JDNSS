package edu.msudenver.cs.jdnss;

/**
* The parser for zone files
*
* @author Steve Beaty
* @version $Id: Parser.java,v 1.25 2011/02/14 16:30:32 drb80 Exp $
*/

import java.util.*;
import java.io.*;
import edu.msudenver.cs.javaln.JavaLN;

// IPV6 addresses: http://www.faqs.org/rfcs/rfc1884.html
// IPV6 DNS: http://www.faqs.org/rfcs/rfc1886.html

public class Parser
{
    /*
    ** the range from 0 to 255 are used for query/response codes
    */
    private static final int EOF	= -1;
    private static final int NOTOK	= 256;
    private static final int IPV4ADDR	= 257;
    private static final int IPV6ADDR	= 258;
    private static final int AT		= 259;
    private static final int LCURLY	= 260;
    private static final int RCURLY	= 261;
    private static final int LPAREN	= 262;
    private static final int RPAREN	= 263;
    private static final int STRING	= 264;
    private static final int IN		= 265;
    private static final int FQDN	= 266;
    private static final int PQDN	= 267;
    private static final int DN		= 268;
    private static final int INT	= 269;
    // private static final int SEMI	= 270;
    private static final int INADDR	= 271;
    private static final int STAR	= 272;

    private int intValue;
    private String origin = "";
    private String StringValue;

    private String currentName;
    private int SOATTL = -1;
    private int SOAMinimumTTL = -1;
    private int globalTTL = -1;
    private int currentTTL = -1;

    private int currentClass;

    private StreamTokenizer st;
    private Hashtable tokens;

    private BindZone zone;

    private JavaLN logger = JDNSS.logger;

    /**
     * The main parsing routine.
     *
     * @param in	where the information is coming from
     */
    public Parser (InputStream in, BindZone zone)
    {
        this.zone = zone;

        /*
        ** set up the tokenizer
        */
        st = new StreamTokenizer (new InputStreamReader (in));

        initTokenizer (st);

        tokens = new Hashtable();
        tokens.put ("SOA", new Integer (Utils.SOA));
        tokens.put ("IN", new Integer (IN));
        tokens.put ("MX", new Integer (Utils.MX));
        tokens.put ("NS", new Integer (Utils.NS));
        tokens.put ("A", new Integer (Utils.A));
        tokens.put ("AAAA", new Integer (Utils.AAAA));
        tokens.put ("A6", new Integer (Utils.A6));
        tokens.put ("CNAME", new Integer (Utils.CNAME));
        tokens.put ("PTR", new Integer (Utils.PTR));
        tokens.put ("TXT", new Integer (Utils.TXT));
        tokens.put ("HINFO", new Integer (Utils.HINFO));
        tokens.put ("$INCLUDE", new Integer (Utils.INCLUDE));
        tokens.put ("$ORIGIN", new Integer (Utils.ORIGIN));
        tokens.put ("$TTL", new Integer (Utils.TTL));

        origin = zone.getName();
    }

    private void initTokenizer (StreamTokenizer st)
    {
        st.commentChar (';');

        /*
        ** putting 0-9 into wordChars doesn't work unless one
        ** first makes them ordinary.  weird.
        */
        st.ordinaryChars ('0', '9');
        st.wordChars ('0', '9');
        st.wordChars ('.', '.');
        st.wordChars (':', ':');
        st.wordChars ('$', '$');
        st.wordChars ('\\', '\\');
        st.wordChars ('[', '[');
        st.wordChars (']', ']');
        st.wordChars ('/', '/');

        st.quoteChar ('"');

        st.slashSlashComments (true);
        st.slashStarComments (true);
    }

    private int matcher (String a)
    {
        logger.entering (a);

        /*
        ** \\d matches digits
        */
        if (a.matches ("(\\d+\\.){3}+\\d+"))
        {
            StringValue = a;
            logger.exiting ("IPV4ADDR");
            return (IPV4ADDR);
        }

        /*
        ** any number of hex digits separated by colons or
        ** any number of hex digits separated by colons that
        ** end in an IPv4 address
        */
        if (a.matches ("(\\p{XDigit}*\\:)+\\p{XDigit}+") ||
        a.matches ("(\\p{XDigit}*\\:)+(\\d+\\.){3}+\\d+"))
        {
            StringValue = a.replaceFirst ("(\\:0+)+", ":");
            StringValue = StringValue.replaceFirst ("^0+\\:", ":");
            logger.finest (StringValue);
            logger.exiting ("IPV6ADDR");
            return (IPV6ADDR);
        }

        String b = a.toLowerCase();
        if (b.matches ("(\\d+\\.){4}+in-addr\\.arpa\\.") ||
        b.matches ("(\\d+\\.){32}+in-addr\\.arpa\\.") ||
        b.matches ("(\\d+\\.){32}+ip6\\.int\\."))
        {
            StringValue = b.replaceFirst ("\\.$", "");
            logger.finest (StringValue);
            logger.exiting ("INADDR");
            return (INADDR);
        }

        if (a.matches ("\\\\\\[x[0-9a-fA-F]+(\\/[0-9]+)?\\]"))
        {
            StringValue = a;
            logger.finest (StringValue);
            System.err.println (StringValue);
            System.exit (0);
        }

        if (a.matches ("\\d+"))
        {
            intValue = Integer.parseInt (a);
            logger.finest (intValue);
            logger.exiting ("INT");
            return (INT);
        }

        /*
        ** "(Newer versions of BIND (named) will accept the suffixes
        ** 'M','H','D' or 'W', indicating a time-interval of minutes,
        ** hours, days and weeks respectively.)"
        ** http://en.wikipedia.org/wiki/Domain_Name_System
        */
        if (a.matches ("\\d+[MHDW]"))
        {
            intValue = Integer.parseInt (a.substring (0, a.length()-1));

            char c = a.charAt (a.length()-1);
            switch (c)
            {
                case 'W': intValue *= 7;	// fall through
                case 'D': intValue *= 24;	// fall through
                case 'H': intValue *= 60;	// fall through
                case 'M': intValue *= 60;
            }

            logger.finest (intValue);
            logger.exiting ("INT");
            return (INT);
        }

        // FQDN's end with a dot
        if (a.matches ("([-a-zA-Z0-9_]+\\.)+"))
        {
            // remove the dot
            StringValue = a.replaceFirst ("\\.$", "");
            logger.finest (StringValue);
            logger.exiting ("DN");
            return (DN);
        }

        // PQDN's don't
        if (a.matches ("[-a-zA-Z0-9_]+(\\.[-a-zA-Z0-9_]+)*"))
        {
            StringValue = a + "." + origin;
            logger.finest (StringValue);
            logger.exiting ("PQDN");
            return (DN);
        }

        logger.severe ("Unknown token on line " + st.lineno() + ": " + a);
        Utils.Assert (false);
        return (NOTOK);
    }

    private int getOneWord()
    {
        int t = NOTOK;

        try
        {
            t = st.nextToken ();
        }
        catch (IOException e)
        {
            logger.info ("Error while reading token on line " +
            st.lineno() + ": " + e);
            logger.exiting ("NOTOK");
            return (NOTOK);
        }

        logger.exiting (t);
        return (t);
    }

    private int getNextToken ()
    {
        int t = getOneWord();
        logger.finest ("t = " + t);

        switch (t)
        {
            case '"' :
            StringValue = st.sval;
            logger.finest (StringValue);
            logger.exiting ("STRING");
            return (STRING);
            case StreamTokenizer.TT_EOF:
            logger.exiting ("EOF");
            return (EOF);
            case StreamTokenizer.TT_NUMBER:
            // numbers are counted as words...
            Utils.Assert (false);
            logger.exiting ("NOTOK");
            return (NOTOK);
            case StreamTokenizer.TT_WORD:
            {
                String a = st.sval;
                logger.finest ("a = " + a);

                /*
                ** is it in the tokens hash?
                */
                Integer i = (Integer) tokens.get (a);
                if (i != null)
                {
                    int j = i.intValue();
                    logger.exiting (j);
                    return (j);
                }

                int k = matcher (a);
                logger.exiting (k);
                return (k);
            }
            case '@':
            {
                StringValue = origin;
                logger.finest (StringValue);
                logger.exiting ("AT");
                return (DN);
            }
            // case ';': return (SEMI);
            case '{': { logger.exiting ("LCURLY"); return (LCURLY); }
            case '}': { logger.exiting ("RCURLY"); return (RCURLY); }
            case '(': { logger.exiting ("LPAREN"); return (LPAREN); }
            case ')': { logger.exiting ("RPAREN"); return (RPAREN); }
            case '*': { logger.exiting ("STAR"); return (STAR); }
            default:
            logger.info ("Unknown token at line " + st.lineno() + ": " + t);
            logger.exiting ("NOTOK");
            return (NOTOK);
        }
    }

    private void doInclude()
    {
        logger.entering();

        // do this at a low level so that the rest of parsing isn't messed
        // up.

        int t = getOneWord();
        Utils.Assert (t == StreamTokenizer.TT_WORD);

        // save the old one so we can get back to it.  if we're called
        // recursively, we're still good to go...
        StreamTokenizer old = st;

        FileInputStream in = null;
        try
        {
            in = new FileInputStream (st.sval);
        }
        catch (FileNotFoundException e)
        {
            logger.info ("Cannot open $INCLUDE file at line " + st.lineno() +
            ": " + st.sval);
            return;
        }

        st = new StreamTokenizer (new InputStreamReader (in));
        initTokenizer (st);

        RRs();

        // restore the old one.
        st = old;

        logger.exiting();
    }

    private void doSOA()
    {
        int serial, refresh, retry, expire, minimum;
        String server = "";

        origin = currentName;

        switch (getNextToken ())
        {
            case DN : server = StringValue; break;
            default :
            logger.info ("Unknown token at line " + st.lineno()); break;
        }

        Utils.Assert (getNextToken() == DN,
        "Expecting contact at line " + st.lineno());
        String contact = StringValue;

        Utils.Assert (getNextToken() == LPAREN,
        "Expecting left paren at line " + st.lineno());
        Utils.Assert (getNextToken() == INT,
        "Expecting serial number at line " + st.lineno());
        serial = intValue;
        Utils.Assert (getNextToken() == INT,
        "Expecting refresh at line " + st.lineno());
        refresh = intValue;
        Utils.Assert (getNextToken() == INT,
        "Expecting retry at line " + st.lineno());
        retry = intValue;
        Utils.Assert (getNextToken() == INT,
        "Expecting expire at line " + st.lineno());
        expire = intValue;
        Utils.Assert (getNextToken() == INT,
        "Expecting minimum at line " + st.lineno());
        minimum = intValue;
        Utils.Assert (getNextToken() == RPAREN,
        "Expecting rightparen at line " + st.lineno());

        SOAMinimumTTL = minimum;
        SOATTL = (currentTTL != -1) ? currentTTL : -1;

        zone.add (origin, new SOARR (origin, server, contact, serial,
        refresh, retry, expire, minimum,
        (SOATTL != -1) ? SOATTL : minimum));
    }

    private void switches (int t)
    {
        logger.entering (t);

        switch (t)
        {
            case Utils.A:
            {
                Utils.Assert (getNextToken () == IPV4ADDR,
                "Expecting IPV4ADDR at line " + st.lineno());
                zone.add (currentName,
                new ARR (currentName, currentTTL, StringValue));
                break;
            }
            case Utils.A6:
            {
                getNextToken();
                break;
            }
            case Utils.AAAA:
            {
                Utils.Assert (getNextToken () == IPV6ADDR,
                "Expecting IPV6ADDR at line " + st.lineno());
                zone.add (currentName,
                new AAAARR (currentName, currentTTL, StringValue));
                break;
            }
            case Utils.NS:
            {
                Utils.Assert (getNextToken () == DN,
                "Expecting domain name at line " + st.lineno());
                zone.add (currentName,
                new NSRR (currentName, currentTTL, StringValue));
                break;
            }
            case Utils.CNAME:
            {
                Utils.Assert (getNextToken () == DN,
                "Expecting domain name at line " + st.lineno());
                zone.add (currentName,
                new CNAMERR (currentName, currentTTL, StringValue));
                break;
            }
            case Utils.TXT:
            {
                Utils.Assert (getNextToken () == STRING,
                "Expecting text at line " + st.lineno());
                zone.add (currentName,
                new TXTRR (currentName, currentTTL, StringValue));
                break;
            }
            case Utils.HINFO:
            {
                Utils.Assert (getNextToken () == STRING,
                "Expecting text at line " + st.lineno());
                String s = StringValue;
                Utils.Assert (getNextToken () == STRING,
                "Expecting text at line " + st.lineno());

                zone.add (currentName,
                new HINFORR (currentName, currentTTL, s, StringValue));
                break;
            }
            case Utils.MX:
            {
                Utils.Assert (getNextToken () == INT,
                "Expecting number at line " + st.lineno());
                Utils.Assert (getNextToken () == DN,
                "Expecting domain at line " + st.lineno());

                zone.add (currentName,
                new MXRR (currentName, currentTTL, StringValue, intValue));
                break;
            }
            case Utils.PTR:
            {
                Utils.Assert (getNextToken () == DN,
                "Expecting domain at line " + st.lineno());

                zone.add (currentName,
                new PTRRR (currentName, currentTTL, StringValue));

                break;
            }
            default:
            {
                logger.info ("At line " + st.lineno() +
                ", didn't recognize: " + t);
                break;
            }
        }
    }

    private int CalcTTL()
    {
        // logger.severe (globalTTL);
        // logger.severe (SOATTL);
        // logger.severe (SOAMinimumTTL);
        // logger.severe (currentTTL);

        if (currentTTL != -1)
        {
            if (SOATTL > currentTTL)
            {
                // logger.severe ("returning SOATTL");
                return (SOATTL);
            }
            else
            {
                // logger.severe ("returning currentTTL");
                return (currentTTL);
            }
        }
        else if (globalTTL != -1)
        {
            // logger.severe ("returning globalTTL");
            return (globalTTL);
        }
        else if (SOATTL != -1)
        {
            // logger.severe ("returning SOATTL");
            return (SOATTL);
        }
        else
        {
            // logger.severe ("returning SOAMinimumTTL");
            return (SOAMinimumTTL);
        }
    }

    private int max (int a, int b)
    {
        return ((a > b) ? a : b);
    }

    void RRs()
    {
        currentName = origin;
        currentClass = 0;

        int t = getNextToken();

        try
        {
            while (t != EOF)
            {
                logger.finest (t);

                if (t == Utils.INCLUDE)
                {
                    doInclude();
                    t = getNextToken();
                    continue;
                }

                if (t == Utils.ORIGIN)
                {
                    t = getNextToken();
                    Utils.Assert (t == DN,
                    "Expecting domain at line " + st.lineno());
                    origin = StringValue;
                    t = getNextToken();
                    continue;
                }

                if (t == Utils.TTL)
                {
                    t = getNextToken();
                    Utils.Assert (t == INT,
                    "Expecting integer at line " + st.lineno());
                    globalTTL = intValue;
                    t = getNextToken();
                    continue;
                }

                // 5.1. Format
                // [name]	[ttl]	[class]	type	data
                // [name]	[class]	[ttl]	type	data
                // 1		IN	1H	PTR	@
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
                    logger.finest (t);

                    switch (t)
                    {
                        case DN: case INADDR:
                        {
                            currentName = StringValue;
                            t = getNextToken ();
                            break;
                        }
                        case IN :
                        {
                            currentClass = t;

                            t = getNextToken ();

                            if (t == INT)
                            {
                                currentTTL = intValue;
                                t = getNextToken ();
                            }

                            break;
                        }
                        case INT :
                        {
                            int temp = intValue;
                            t = getNextToken ();

                            // ptr ttl in
                            // ptr in ttl
                            if (first && (origin.endsWith (".arpa") ||
                            origin.endsWith (".int")))
                            {
                                currentName = "" + temp + "." + origin;
                                logger.finest (currentName);

                                if (t == INT)
                                {
                                    currentTTL = intValue;
                                    t = getNextToken ();
                                }
                            }
                            else if (t == IN)	// ttl in
                            {
                                currentTTL = temp;
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
                            switches (t);
                            done = true;
                            break;
                        }
                        case Utils.SOA:
                        {
                            doSOA();
                            done = true;
                            break;
                        }
                        default:
                        {
                            logger.info ("At line " + st.lineno() +
                            ", didn't recognize: " + t);
                            done = true;
                            break;
                        }
                    }
                    first = false;
                }

                t = getNextToken ();
            }
        }
        catch (IllegalArgumentException IAE)
        {
            logger.throwing (IAE);
            logger.severe ("Skipping: " + zone.getName());
        }
    }


    /**
     * For testing -- creates an instance of Parser and
     * parses for the domain "mpcs.org"
     */
    public static void main (String args[]) throws FileNotFoundException
    {
        BindZone zone = new BindZone ("scopesconf.org");
        new Parser (new FileInputStream ("scopesconf.org"), zone);
        System.out.println (zone);
    }
}
