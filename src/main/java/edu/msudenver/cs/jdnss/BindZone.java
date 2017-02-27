package edu.msudenver.cs.jdnss;
/**
 * @author Steve Beaty
 * @version $Id: BindZone.java,v 1.1 2011/03/03 22:35:14 drb80 Exp $
 */

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import edu.msudenver.cs.javaln.JavaLN;

class BindZone implements Zone
{
    private String name;

    /*
    ** might not be the best to have different tables for each, but
    ** otherwise would have to find a way to differentiate the types
    ** in the single table...
    */

    private Hashtable hA;
    private Hashtable hAAAA;
    private Hashtable hNS;
    private Hashtable hMX;
    private Hashtable hCNAME;
    private Hashtable hPTR;
    private Hashtable hTXT;
    private Hashtable hHINFO;
    private Hashtable hSOA;
    private Hashtable hDNSKEY;
    private Hashtable hDNSRRSIG;
    private Hashtable hDNSNSEC;

    private JavaLN logger = JDNSS.logger;

    public BindZone (String name)
    {
        this.name = name;

        hA = new Hashtable();
        hAAAA = new Hashtable();
        hNS = new Hashtable();
        hMX = new Hashtable();
        hCNAME = new Hashtable();
        hPTR = new Hashtable();
        hTXT = new Hashtable();
        hHINFO = new Hashtable();
        hSOA = new Hashtable();
        hDNSKEY = new Hashtable();
        hDNSRRSIG = new Hashtable();
        hDNSNSEC = new Hashtable();
    }

    /**
     * @return the domain name
     */
    public String getName() { return (name); }

    /**
     * Create a printable String
     * @param h        a Hashtable
     * @return        the contents in String form
     */
    private String dumphash (Hashtable h)
    {
        String s = "";
        Enumeration e = h.keys();

        while (e.hasMoreElements())
        {
            Object o = e.nextElement();
            s += o + ": " + h.get (o) + " ";
        }
        return s;
    }

    /**
     * A printable version of the Zone
     * @return the string
     */
    public String toString ()
    {
        String s = "---- Zone " + name + " -----" + '\n';

        s += "SOA: " + dumphash (hSOA) + "\n";
        s += "A: " + dumphash (hA) + "\n";
        s += "AAAA: " + dumphash (hAAAA) + "\n";
        s += "CNAME: " + dumphash (hCNAME) + "\n";
        s += "MX: " + dumphash (hMX) + "\n";
        s += "NS: " + dumphash (hNS) + "\n";
        s += "PTR: " + dumphash (hPTR) + "\n";
        s += "TXT: " + dumphash (hTXT) + "\n";
        s += "HINFO: " + dumphash (hHINFO) + "\n";
        s += "DNSKEY: " + dumphash (hDNSKEY) + "\n";
        s += "DNSRRSIG: " + dumphash (hDNSRRSIG) + "\n";
        s += "DNSNSEC: " + dumphash (hDNSNSEC) + "\n";
        s += "--------";

        return (s);
    }

    private Hashtable getTable (int type)
    {
        logger.entering (type);

        switch (type)
        {
            case Utils.A:        return (hA);
            case Utils.AAAA:        return (hAAAA);
            case Utils.NS:        return (hNS);
            case Utils.MX:        return (hMX);
            case Utils.CNAME:        return (hCNAME);
            case Utils.PTR:        return (hPTR);
            case Utils.TXT:        return (hTXT);
            case Utils.HINFO:        return (hHINFO);
            case Utils.SOA:        return (hSOA);
            case Utils.NSEC:        return (hDNSNSEC);
            case Utils.RRSIG:        return (hDNSRRSIG);
            case Utils.DNSKEY:        return (hDNSKEY);
            default:
            {
                logger.config ("type not found: " + type);
                logger.exiting ("null");
                return (null);
            }
        }
    }

    /**
     * Add an address to a name.  There may be multiple addresses per name
     * @param name        the name
     * @param rr        the resource record
     */
    public void add (String name, RR rr)
    {
        logger.entering (new Object[]{name, rr});

        if ((rr instanceof SOARR) && name != this.name)
        {
            this.name = name;
        }

        Hashtable h = getTable (rr.getType());
        Vector value;

        logger.finest (h.get (name));

        /*
        ** if there isn't already a entry
        */
        if ((value = (Vector) h.get (name)) == null)
        {
            value = new Vector();
            h.put (name, value);
            value.add (rr);
        }
        else if (! value.contains (rr))
        {
            value.add (rr);
        }
        else
        {
            logger.info (rr + " already present");
        }
    }

    /**
     * Get the Vector for a particular name
     * @param type        the query type
     * @param name        the name
     * @return a Vector with the appropriate addresses for the given name
     */
    public Vector get (int type, String name)
    {
        logger.entering (new Object[]{new Integer (type), name});

        Hashtable h = getTable (type);
        logger.finest (h);

        Vector v = null;

        if (h != null)
        {
            v = (Vector) h.get (name);
        }

        logger.exiting  (v);
        return (v);
    }
}
