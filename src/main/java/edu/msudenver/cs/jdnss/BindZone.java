package edu.msudenver.cs.jdnss;
/**
 * @author Steve Beaty
 * @version $Id: BindZone.java,v 1.1 2011/03/03 22:35:14 drb80 Exp $
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

class BindZone implements Zone
{
    private String name;

    /*
    ** might not be the best to have different tables for each, but
    ** otherwise would have to find a way to differentiate the types
    ** in the single table...
    */

    private Hashtable<String, Vector> hA = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hAAAA = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hNS = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hMX = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hCNAME = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hPTR = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hTXT = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hHINFO = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hSOA = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hDNSKEY = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hDNSRRSIG = new Hashtable<String, Vector>();
    private Hashtable<String, Vector> hDNSNSEC = new Hashtable<String, Vector>();
    private Hashtable<Integer, Hashtable> tableOfTables =
        new Hashtable<Integer, Hashtable>();

    private Logger logger = JDNSS.getLogger();

    public BindZone(final String name)
    {
        this.name = name;

        tableOfTables.put(Utils.A, hA);
        tableOfTables.put(Utils.AAAA, hAAAA);
        tableOfTables.put(Utils.NS, hNS);
        tableOfTables.put(Utils.MX, hMX);
        tableOfTables.put(Utils.CNAME, hCNAME);
        tableOfTables.put(Utils.PTR, hPTR);
        tableOfTables.put(Utils.TXT, hTXT);
        tableOfTables.put(Utils.HINFO, hHINFO);
        tableOfTables.put(Utils.SOA, hSOA);
        tableOfTables.put(Utils.DNSKEY, hDNSKEY);
        tableOfTables.put(Utils.RRSIG, hDNSRRSIG);
    }

    /**
     * @return the domain name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Create a printable String.
     * @param h        a Hashtable
     * @return        the contents in String form
     */
    private String dumphash(final Hashtable h)
    {
        String s = "";
        final Enumeration e = h.keys();

        while (e.hasMoreElements())
        {
            final Object o = e.nextElement();
            s += o + ": " + h.get(o) + " ";
        }
        return s;
    }

    /**
     * A printable version of the Zone.
     * @return the string
     */
    public String toString()
    {
        String s = "---- Zone " + name + " -----" + '\n';

        s += "SOA: " + dumphash(hSOA) + "\n";
        s += "A: " + dumphash(hA) + "\n";
        s += "AAAA: " + dumphash(hAAAA) + "\n";
        s += "CNAME: " + dumphash(hCNAME) + "\n";
        s += "MX: " + dumphash(hMX) + "\n";
        s += "NS: " + dumphash(hNS) + "\n";
        s += "PTR: " + dumphash(hPTR) + "\n";
        s += "TXT: " + dumphash(hTXT) + "\n";
        s += "HINFO: " + dumphash(hHINFO) + "\n";
        s += "DNSKEY: " + dumphash(hDNSKEY) + "\n";
        s += "DNSRRSIG: " + dumphash(hDNSRRSIG) + "\n";
        s += "DNSNSEC: " + dumphash(hDNSNSEC) + "\n";
        s += "--------";

        return s;
    }

    private Hashtable<String, Vector> getTable(final int type)
    {
        logger.traceEntry(new ObjectMessage(type));

        final Hashtable ret = tableOfTables.get(type);

        Assertion.aver(ret != null);

        return ret;
    }

    /**
     * Add an address to a name.  There may be multiple addresses per name.
     * @param name        the name
     * @param rr        the resource record
     */
    public void add(final String name, final RR rr)
    {
        logger.traceEntry(new ObjectMessage(name));
        logger.traceEntry(new ObjectMessage(rr));

        if ((rr instanceof SOARR) && ! name.equals(this.name))
        {
            this.name = name;
        }

        Hashtable<String, Vector> h = getTable(rr.getType());

        logger.trace(h.get(name));

        Vector value = h.get(name);

        /*
        ** if there isn't already a entry
        */
        if (value == null)
        {
            value = new Vector();
            h.put(name, value);
            value.add(rr);
        }
        else if (! value.contains(rr))
        {
            value.add(rr);
        }
        else
        {
            logger.info(rr + " already present");
        }
    }

    /**
     * Get the Vector for a particular name.
     * @param type        the query type
     * @param name        the name
     * @return a Vector with the appropriate addresses for the given name
     */
    public Vector get(final int type, final String name)
    {
        logger.traceEntry(new ObjectMessage(type));
        logger.traceEntry(new ObjectMessage(name));

        final Hashtable<String, Vector> h = getTable(type);
        logger.trace(h);

        Vector v = null;

        if (h != null)
        {
            v = h.get(name);
        }

        logger.traceExit (v);
        Assertion.aver(v != null);
        return v;
    }
}
