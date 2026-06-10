package edu.msudenver.cs.jdnss;

/**
 * @author Steve Beaty
 * @version $Id: Zone.java,v 1.20 2011/02/14 16:30:32 drb80 Exp $
 */

import java.util.List;

abstract class Zone {
    abstract boolean isEmpty();
    abstract List<RR> get(RRCode type, String name);
    abstract String getName();
    abstract List<DNSKEYRR> getDNSKEYs();
    abstract boolean isDnssecEnabled();
    abstract void setDnssecEnabled(boolean enabled);
    abstract ValidationResult validate(RRCode type, String name, List<? extends RR> records);
}
