package edu.msudenver.cs.jdnss;

/**
 * @author Steve Beaty
 * @version $Id: Zone.java,v 1.20 2011/02/14 16:30:32 drb80 Exp $
 */

import java.util.ArrayList;

interface Zone
{
    ArrayList<RR> get(RRCode type, String name);
    String getName();
}
