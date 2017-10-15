package edu.msudenver.cs.jdnss;
/**
 * @author Steve Beaty
 * @version $Id: BindZone.java,v 1.1 2011/03/03 22:35:14 drb80 Exp $
 */

import java.util.*;

import lombok.Getter;
import org.apache.logging.log4j.Logger;

class BindZone implements Zone {
    @Getter private String name;

    /*
    ** might not be the best to have different tables for each, but
    ** otherwise would have to find a way to differentiate the types
    ** in the single table...
    */

    private final Map<String, Vector<RR>> hA = new Hashtable<>();
    private final Map<String, Vector<RR>> hAAAA = new Hashtable<>();
    private final Map<String, Vector<RR>> hNS = new Hashtable<>();
    private final Map<String, Vector<RR>> hMX = new Hashtable<>();
    private final Map<String, Vector<RR>> hCNAME = new Hashtable<>();
    private final Map<String, Vector<RR>> hPTR = new Hashtable<>();
    private final Map<String, Vector<RR>> hTXT = new Hashtable<>();
    private final Map<String, Vector<RR>> hHINFO = new Hashtable<>();
    private final Map<String, Vector<RR>> hSOA = new Hashtable<>();
    private final Map<String, Vector<RR>> hDNSKEY = new Hashtable<>();
    private final Map<String, Vector<RR>> hDNSRRSIG = new Hashtable<>();
    private final Map<String, Vector<RR>> hNSEC = new Hashtable<>();
    private final Map<String, Vector<RR>> hNSEC3 = new Hashtable<>();
    private final Map<String, Vector<RR>> hNSEC3PARAM = new Hashtable<>();

    private final Map<Integer, Map<String, Vector<RR>>> tableOfTables = new Hashtable<>();

    private final Logger logger = JDNSS.getLogger();

    public BindZone(final String name) {
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
        tableOfTables.put(Utils.NSEC, hNSEC);
        tableOfTables.put(Utils.NSEC3, hNSEC3);
        tableOfTables.put(Utils.NSEC3PARAM, hNSEC3PARAM);
    }

    /**
     * Create a printable String.
     *
     * @param h a Hashtable
     * @return the contents in String form
     */
    private String dumphash(final Map<String, Vector<RR>> h) {
        String s = "";

        for (String foo : h.keySet()) {
            s += foo + ": " + h.get(foo) + " ";
        }

        return s;
    }

    /**
     * A printable version of the Zone.
     *
     * @return the string
     */
    public String toString() {
        String s = "---- Zone " + name + " -----" + '\n';

        for (int i : tableOfTables.keySet()) {
            s += Utils.mapTypeToString(i) + ": " + dumphash(tableOfTables.get(i)) + "\n";
        }

        s += "--------";

        return s;
    }

    private Map<String, Vector<RR>> getTable(final int type) {
        logger.traceEntry(Utils.mapTypeToString(type));

        final Map<String, Vector<RR>> ret = tableOfTables.get(type);

        Assertion.aver(ret != null);

        return ret;
    }

    /**
     * Add an address to a name.  There may be multiple addresses per name.
     *
     * @param name the name
     * @param rr   the resource record
     */
    public void add(final String name, final RR rr) {
        logger.traceEntry(name);
        logger.traceEntry(rr.toString());

        if ((rr instanceof SOARR) && !name.equals(this.name)) {
            this.name = name;
        }

        Map<String, Vector<RR>> h = getTable(rr.getType());

        logger.trace(h.get(name));

        Vector<RR> value = h.get(name);

        /*
        ** if there isn't already a entry
        */
        if (value == null) {
            value = new Vector<>();
            h.put(name, value);
            value.add(rr);
        } else if (!value.contains(rr)) {
            value.add(rr);
        } else {
            logger.info(rr + " already present");
        }
    }

    /**
     * Get the Vector for a particular name.
     *
     * @param type the query type
     * @param name the name
     * @return a Vector with the appropriate addresses for the given name
     */
    public Vector<RR> get(final int type, final String name) {
        logger.traceEntry(Utils.mapTypeToString(type));
        logger.traceEntry(name);

        final Map<String, Vector<RR>> h = getTable(type);
        Assertion.aver(h != null);
        logger.trace(h);

        Vector v = h.get(name);
        logger.traceExit(v);

        Assertion.aver(v != null);
        return v;
    }
}
