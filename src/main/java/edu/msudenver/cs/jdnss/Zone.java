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
}
