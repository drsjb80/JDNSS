package edu.msudenver.cs.jdnss;
/**
 * @author Steve Beaty
 * @version $Id: BindZone.java,v 1.1 2011/03/03 22:35:14 drb80 Exp $
 */

import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class BindZone implements Zone {
    @Getter private String name;

    /*
    ** might not be the best to have different tables for each, but
    ** otherwise would have to find a way to differentiate the types
    ** in the single table...
    */

    private final Map<RRCode, Map<String, ArrayList<RR>>> tableOfTables = new HashMap<>();

    private final Logger logger = JDNSS.logger;

    BindZone(final String name) {
        this.name = name;

        tableOfTables.put(RRCode.A, new HashMap<>());
        tableOfTables.put(RRCode.AAAA, new HashMap<>());
        tableOfTables.put(RRCode.NS, new HashMap<>());
        tableOfTables.put(RRCode.MX, new HashMap<>());
        tableOfTables.put(RRCode.CNAME, new HashMap<>());
        tableOfTables.put(RRCode.PTR, new HashMap<>());
        tableOfTables.put(RRCode.TXT, new HashMap<>());
        tableOfTables.put(RRCode.HINFO, new HashMap<>());
        tableOfTables.put(RRCode.SOA, new HashMap<>());
        tableOfTables.put(RRCode.DNSKEY, new HashMap<>());
        tableOfTables.put(RRCode.RRSIG, new HashMap<>());
        tableOfTables.put(RRCode.NSEC, new HashMap<>());
        tableOfTables.put(RRCode.NSEC3, new HashMap<>());
        tableOfTables.put(RRCode.NSEC3PARAM, new HashMap<>());
    }

    /**
     * Create a printable String.
     *
     * @param h a HashMap
     * @return the contents in String form
     */
    private String dumphash(final Map<String, ArrayList<RR>> h) {
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

        for (RRCode i : tableOfTables.keySet()) {
            s += i.toString() + ": " + dumphash(tableOfTables.get(i)) + "\n";
        }

        s += "--------";

        return s;
    }

    private Map<String, ArrayList<RR>> getTable(final RRCode type) {

        final Map<String, ArrayList<RR>> ret = tableOfTables.get(type);

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

        Map<String, ArrayList<RR>> h = getTable(rr.getType());

        logger.trace(h.get(name));

        ArrayList<RR> value = h.get(name);

        /*
        ** if there isn't already a entry
        */
        if (value == null) {
            value = new ArrayList<>();
            h.put(name, value);
            value.add(rr);
        } else if (!value.contains(rr)) {
            value.add(rr);
        } else {
            logger.info(rr + " already present");
        }
    }

    /**
     * Get the ArrayList for a particular name.
     *
     * @param type the query type
     * @param name the name
     * @return a ArrayList with the appropriate addresses for the given name
     */
    public ArrayList<RR> get(final RRCode type, final String name) {
        logger.traceEntry(type.toString());
        logger.traceEntry(name);

        final Map<String, ArrayList<RR>> h = getTable(type);
        logger.trace(h);
        Assertion.aver(h != null);

        ArrayList<RR> v = h.get(name);
        logger.trace(v);
        Assertion.aver(v != null);
        return v;
    }
}
