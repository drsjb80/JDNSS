package edu.msudenver.cs.jdnss;
/**
 * parses the incoming DNS queries
 * @author Steve Beaty
 * @version $Id: Query.java,v 1.29 2011/03/14 19:07:22 drb80 Exp $
 */

import edu.msudenver.cs.javaln.JavaLN;
import java.util.Vector;
import java.net.DatagramPacket;

public class Query
{   
    private String qname[];
    private int qtype[];
    private int qclass[];
    private JavaLN logger = JDNSS.logger;
   
    // http://www.networksorcery.com/enp/protocol/dns.htm
    private int id;
    private int opcode = 0;        // standard query

    private int numQuestions;
    private int numAnswers;
    private int numAuthorities;
    private int numAdditionals;
    private int rcode;
    private boolean TC = false;        // truncation
    private boolean QR = false;        // query
    private boolean AA = true;        // authoritative answer
    private boolean RD = false;        // recursion desired
    private boolean RA = false;        // recursion available
    private boolean AD = false;        // authenticated data
    private boolean CD = false;        // checking disabled

    private boolean firsttime;

    public boolean getQR() { return (QR); }
    public int getId() { return (id); }

    private boolean UDP;
    private int minimum;

    private byte[] buffer;
    private byte[] additional = new byte[0];
    private byte[] authority = new byte[0];

    private byte[] savedAdditional;
    private int savedNumAdditionals;

    /**
     * Rebuild the byte array from the object data
     */
    private void rebuild()
    {
        logger.finest (toString());
        logger.finest ("\n" + Utils.toString(buffer));

        buffer[0] = Utils.getByte (id, 2);
        buffer[1] = Utils.getByte (id, 1);
        buffer[2] = (byte)
        (
            (QR ? 128 : 0) |
            (opcode << 3) |
            (AA ? 4 : 0) | 
            (TC ? 2 : 0) |
            (RD ? 1 : 0)
        );
        buffer[3] = (byte)
        (
            (RA ? 128 : 0) |
            (AD ? 32 : 0) |
            (CD ? 16 : 0) |
            rcode
        );
        buffer[4] = Utils.getByte (numQuestions, 2);
        buffer[5] = Utils.getByte (numQuestions, 1);
        buffer[6] = Utils.getByte (numAnswers, 2);
        buffer[7] = Utils.getByte (numAnswers, 1);
        buffer[8] = Utils.getByte (numAuthorities, 2);
        buffer[9] = Utils.getByte (numAuthorities, 1);
        buffer[10] = Utils.getByte (numAdditionals, 2);
        buffer[11] = Utils.getByte (numAdditionals, 1);

        logger.finest (toString());
        logger.finest ("\n" + Utils.toString(buffer));
    }

    Query (int id, String[] questions, int type[], int qclass[])
    {
        // internal representation
        this.id = id;
            this.numQuestions = questions.length;
        this.qname = new String[this.numQuestions];
        this.qtype = new int[this.numQuestions];
        this.qclass = new int[this.numQuestions];

        // external representation
        buffer = new byte[12];
        rebuild();

            for (int i = 0; i < questions.length; i++)
        {
            this.qname[i] = questions[i];
            this.qtype[i] = type[i];
            this.qclass[i] = qclass[i];

            buffer = Utils.combine (buffer, Utils.convertString (questions[i]));
            buffer = Utils.combine (buffer, Utils.getTwoBytes (qtype[i], 2));
            buffer = Utils.combine (buffer, Utils.getTwoBytes (qclass[i], 2));
        }
    }

    public byte[] getBuffer() { return (buffer); }

    /**
     * creates a Query from a packet
     */
    public Query (byte b[])
    {
        buffer = b;
        id =                Utils.addThem (buffer[0], buffer[1]);
        numQuestions =        Utils.addThem (buffer[4], buffer[5]);
        numAnswers =        Utils.addThem (buffer[6], buffer[7]);
        numAuthorities = Utils.addThem (buffer[8], buffer[9]);
        numAdditionals = Utils.addThem (buffer[10], buffer[11]);

        Utils.Assert (numAnswers == 0);
        Utils.Assert (numAuthorities == 0);

        int flags =        Utils.addThem (buffer[2], buffer[3]);
        QR =                (flags & 0x00008000) != 0;
        opcode =        (flags & 0x00007800) >> 11;
        AA =                (flags & 0x00000400) != 0;
        TC =                (flags & 0x00000200) != 0;
        RD =                (flags & 0x00000100) != 0;
        RA =                 (flags & 0x00000080) != 0;
        AD =                 (flags & 0x00000020) != 0;
        CD =                 (flags & 0x00000010) != 0;
        rcode =                flags & 0x0000000f;

        qname = new String[numQuestions];
        qtype = new int[numQuestions];
        qclass = new int[numQuestions];
    }

    /**
     * Evaluates and saves all questions
    */
    private void parseQueries ()
    {
        logger.entering();

        /*
        The question section is used to carry the "question" in most queries,
        i.e., the parameters that define what is being asked.  The section
        contains QDCOUNT (usually 1) entries, each of the following format:

                                            1  1  1  1  1  1
              0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                                               |
            /                     QNAME                     /
            /                                               /
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                     QTYPE                     |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                     QCLASS                    |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        */

        int location = 12;

        for (int i = 0; i < numQuestions; i++)
        {
            SandN sn = Utils.parseName (location, buffer);
            if (sn != null)
            {
                location = sn.getNumber();
                qname[i] = sn.getString();
            }
            qtype[i] = Utils.addThem (buffer[location], buffer[location + 1]);
            location += 2;
            qclass[i] = Utils.addThem (buffer[location], buffer[location + 1]);
            location += 2;
        }

        if (numAdditionals > 0)
        {
            int length = buffer.length - location;
            savedNumAdditionals = numAdditionals;
            savedAdditional = new byte[length];
            System.arraycopy (buffer, location, savedAdditional, 0, length);
            buffer = Utils.trimbytearray (buffer, location);
            numAdditionals = 0;
            rebuild ();
        }
    }

    public String toString()
    {
        String s = "Id: 0x" + Integer.toHexString (id) + "\n";
        s += "Questions: " + numQuestions + "\t";
        s += "Answers: " + numAnswers + "\n";
        s += "Authority RR's: " + numAuthorities + "\t";
        s += "Additional RR's: " + numAdditionals + "\n";

        s += "QR: " + QR + "\t";
        s += "AA: " + AA + "\n";
        s += "TC: " + TC + "\t";
        s += "RD: " + RD + "\n";
        s += "RA: " + RA + "\t";
        s += "AD: " + AD + "\t";
        s += "CD: " + CD + "\t";
        s += "opcode: " + opcode + "\n";
        s += "rcode: " + rcode;

        for (int i = 0; i < numQuestions; i++)
        {
            s += "\nName: " + qname[i] +
                 " Number: " + qtype[i] +
                 " Class: " + qclass[i] +
                 " Name: " + Utils.mapTypeToString (qtype[i]);
        }
        return s;
    }

    private void addThem (Vector v, String host, int type)
    {
        for (int i = 0; i < v.size(); i++)
        {
            RR rr = (RR) v.elementAt(i);
            additional =
                Utils.combine (additional, rr.getBytes (host, minimum));
            numAdditionals++;
        }
    }

    /**
     * Given a zone and an MX or NS hostname, see if there is an A or AAAA
     * record we can also send back...
     */
    private void createAdditional (Zone zone, String host)
    {
        Vector v = zone.get (Utils.A, host);
        logger.finest (v);

        if (v != null)
            addThem (v, host, Utils.A);

        v = zone.get (Utils.AAAA, host);
        logger.finest (v);

        if (v != null)
            addThem (v, host, Utils.AAAA);
    }

    private void createAuthorities (Zone zone)
    {
        Vector v = zone.get (Utils.NS, zone.getName());
        logger.finest (v);

        if (v != null)
        {
            for (int i = 0; i < v.size(); i++)
            {
                NSRR rr = (NSRR) v.elementAt(i);
                logger.finest (rr);
                authority = Utils.combine (authority,
                    rr.getBytes (rr.getName(), minimum));
                numAuthorities++;

                // add address if known
                createAdditional (zone, rr.getString());
            }
        }
    }

    private void createResponses (Zone zone, Vector v, String name, int which)
    {
        logger.entering (new Object[]{v, name, new Integer (which)});

        for (int i = 0; i < v.size(); i++)
        {
            RR rr = (RR) v.elementAt(i);
            byte add[] = rr.getBytes (name, minimum);

            if (UDP && (buffer.length + add.length > 512))
            {
                TC = true;
                rebuild();
                return;
            }

            numAnswers++;
            buffer = Utils.combine (buffer, add);

            if (firsttime && which != Utils.NS)
                createAuthorities (zone);

            firsttime = false;

            if (which == Utils.MX)
                createAdditional (zone, ((MXRR) v.elementAt(i)).getHost());
            else if (which == Utils.NS)
                createAdditional (zone, ((NSRR) v.elementAt(i)).getString());
        }

        rebuild();
    }

    private void addAuthorities()
    {
        logger.finest (UDP);
        logger.finest (buffer.length);
        logger.finest (authority.length);
        if (!UDP || (UDP && (buffer.length + authority.length < 512)))
        {
            logger.finest ("adding in authorities");
            buffer = Utils.combine (buffer, authority);
        }
        else
        {
            logger.finest ("NOT adding in authorities");
            numAuthorities = 0;
        }
    }

    public void addAdditionals()
    {
        if (savedNumAdditionals > 0)
        {
            additional = Utils.combine (additional, savedAdditional);
            numAdditionals += savedNumAdditionals;
        }

        if (numAdditionals > 0)
        {
            if (!UDP || (UDP && (buffer.length + additional.length < 512)))
            {
                buffer = Utils.combine (buffer, additional);
            }
            else
            {
                numAdditionals = 0;
            }
        }
    }

    void addSOA (Zone zone, SOARR SOA)
    {
        authority = Utils.combine
            (authority, SOA.getBytes (zone.getName(), minimum));
        numAuthorities++;
        addAuthorities();
        rebuild();
    }

/*
Suppose that an authoritative server has an A RR but has no AAAA RR for a
host name.  Then, the server should return a response to a query for an
AAAA RR of the name with the response code (RCODE) being 0 (indicating no
error) and with an empty answer section (see Sections 4.3.2 and 6.2.4 of
[1]).  Such a response indicates that there is at least one RR of a
different type than AAAA for the queried name, and the stub resolver can
then look for A RRs.

This way, the caching server can cache the fact that the queried name has
no AAAA RR (but may have other types of RRs), and thus improve the response
time to further queries for an AAAA RR of the name.
*/
    private void dealWithOther (Zone zone, int type, String name)
    {
        int other = type == Utils.A ? Utils.AAAA : Utils.A;

        Vector v = zone.get (other, name);
        if (v != null)
        {
            logger.config (Utils.mapTypeToString (type) +
                " lookup of " + name + " failed but " +
                Utils.mapTypeToString (other) + " record found");

            rcode = Utils.NOERROR;
        }
        else
        {
            logger.config (Utils.mapTypeToString (type) + " lookup of " +
                name + " failed");

            rcode = Utils.NAMEERROR;
        }
    }

    private void errLookupFailed (int type, String name, int rcode)
    {
        logger.config ("'" + Utils.mapTypeToString (type) +
            "' lookup of " + name + " failed");
        this.rcode = rcode;
    }

    private SandV lookupFailed (Vector v, int type, String name, Zone zone,
        SOARR SOA)
    {
        logger.entering (v);
        logger.entering (type);
        logger.entering (name);
        logger.entering (zone);

        // is there an associated CNAME?
        Vector u = zone.get (Utils.CNAME, name);
        logger.finer (u);

        if (u == null)
        {
            return (null);
        }
        else
        {
            String s = ((CNAMERR) u.elementAt (0)).getString();
            logger.finer (s);

            if (s == null)
            {
                return (null);
            }
            
            // there is a CNAME, is it of the correct type?
            v = zone.get (type, s); logger.finer (v);
            if (v != null)
            {
                // yes, so first put in the CNAME
                createResponses (zone, u, name, Utils.CNAME);

                // then continue the lookup on the original type
                // with the new name
                return (new SandV (s, v));
            }
            else
            {
                return (null);
            }
        }
    }

    /**
     * create a byte array that is a Response to a Query
     */
    public byte[] makeResponses (JDNSS dnsService, boolean UDP)
    {
        parseQueries();
        firsttime = true;

        logger.finest (toString());
        logger.finest ("\n" + Utils.toString(buffer));

        this.UDP = UDP;

        QR = true;        // response
        AA = true;        // we are authoritative
        RA = false;        // recursion not available

        for (int i = 0; i < qname.length; i++)
        {
            String name = qname[i];
            int type = qtype[i];
            logger.finest (name);
            logger.finest (type);

            Zone zone = dnsService.getZone (name);
            logger.finest (zone);
            if (zone == null)
            {
                logger.config ("Zone lookup of " + name + " failed");
                rcode = Utils.REFUSED;
                AA = false;
                rebuild();
                return (buffer);
            }

            // always need to get the SOA to find the default minimum
            Vector w = zone.get (Utils.SOA, zone.getName());
            if (w == null)
            {
                logger.config ("SOA lookup in " + zone.getName() + " failed");
                rcode = Utils.SERVFAIL;
                rebuild();
                return (buffer);
            }

            SOARR SOA = (SOARR) w.elementAt(0);
            minimum = SOA.getMinimum();

            Vector v = zone.get (type, name);
            logger.config ("v == " + v);

            if (v == null)
            {
                if (type != Utils.AAAA && type != Utils.A)
                {
                    errLookupFailed (type, name, Utils.NOERROR);
                    addSOA (zone, SOA);
                    return (buffer);
                }
                else
                {
                    SandV sandv = lookupFailed (v, type, name, zone, SOA);

                    // if it really failed
                    if (sandv == null)
                    {
                        dealWithOther (zone, type, name);
                        addSOA (zone, SOA);
                        return (buffer);
                    }
                    else
                    {
                        v = sandv.getVector();
                        name = sandv.getString();
                    }
                }
            }

            if (numAdditionals != 0 && dnsService.jargs.RFC2671)
            {
                logger.config ("Additionals not understood");
                rcode = Utils.NOTIMPL;
                rebuild();
                return (buffer);
            }

            createResponses (zone, v, name, type);
        }

        if (numAuthorities > 0)
            addAuthorities();

        addAdditionals();
        rebuild();
        return (buffer);
    }

    public static void main (String args[])
    {
        String questions[] = new String[]{"www.pipes.org"};
        int types[] = new int[]{Utils.A};
        int classes[] = new int[]{1};
        System.out.println (new Query (1000, questions, types, classes));
    }
}
