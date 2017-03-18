package edu.msudenver.cs.jdnss;

/**
 * @author Steve Beaty
 * @version $Id: DBZone.java,v 1.3 2011/03/14 19:07:22 drb80 Exp $
 */

import edu.msudenver.cs.javaln.JavaLN;

import java.util.Vector;

class DBZone implements Zone
{
    private static JavaLN logger = JDNSS.getLogger();
    private String zoneName;
    private int domainId;
    private DBConnection dbConnection;

    // com.mysql.jdbc.Driver
    // jdbc:mysql://localhost/JDNSS
    DBZone(final String zoneName, final int domainId,
        final DBConnection dbConnection)
    {
        this.zoneName = zoneName;
        this.domainId = domainId;
        this.dbConnection = dbConnection;
    }

    public String getName() { return zoneName; }

    public Vector get(final int type, final String name)
    {
        logger.entering(type);
        logger.entering(name);

        return dbConnection.get(type, name, domainId);
    }
}
