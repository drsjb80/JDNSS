package edu.msudenver.cs.jdnss;

/**
 * @author Steve Beaty
 * @version $Id: DBZone.java,v 1.3 2011/03/14 19:07:22 drb80 Exp $
 */

import java.util.Vector;
import edu.msudenver.cs.javaln.JavaLN;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

class DBZone implements Zone
{
    private static JavaLN logger = JDNSS.logger;
    private String zoneName;
    private int domainId;
    private DBConnection dbConnection;

    // com.mysql.jdbc.Driver
    // jdbc:mysql://localhost/JDNSS
    DBZone (String zoneName, int domainId, DBConnection dbConnection)
    {
        this.zoneName = zoneName;
        this.domainId = domainId;
        this.dbConnection = dbConnection;
    }

    public String getName() { return (zoneName); }

    public Vector get (int type, String name)
    {
        logger.entering (type);
        logger.entering (name);

        return (dbConnection.get (type, name, domainId));
    }
}
