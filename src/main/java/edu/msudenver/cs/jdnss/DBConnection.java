package edu.msudenver.cs.jdnss;

/**
 * @author Steve Beaty
 * @version $Id: DBConnection.java,v 1.1 2011/03/14 19:05:17 drb80 Exp $
 */

import java.util.Vector;
import edu.msudenver.cs.javaln.JavaLN;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

class DBConnection
{
    private Statement stmt;
    private static JavaLN logger = JDNSS.logger;

    // com.mysql.jdbc.Driver
    // jdbc:mysql://localhost/JDNSS
    DBConnection (String DBClass, String DBURL, String DBUser, String DBPass)
    {
	DBUser = DBUser == null ? "" : DBUser;
	DBPass = DBPass == null ? "" : DBPass;

	try
	{
	    Class.forName (DBClass);
	    Connection conn = DriverManager.getConnection (DBURL, DBUser,
	        DBPass);
	    stmt = conn.createStatement();
	}
	catch (ClassNotFoundException cnfe)
	{
	    logger.throwing (cnfe);
	}
	catch (java.sql.SQLException sqle)
	{
	    logger.throwing (sqle);
	}
    }

    public DBZone getZone (String name)
    {
	logger.entering (name);

        Vector v = new Vector();

	// first, search for longest...
	try
	{
	    ResultSet rs = stmt.executeQuery ("SELECT * FROM domains");

	    while (rs.next())
	    {
		v.add (rs.getString ("name"));
	    }
	}
	catch (java.sql.SQLException sqle)
	{
	    logger.throwing (sqle);
	    logger.exiting (null);
	    return (null);
	}

	if (v.size() == 0)
	{
	    logger.exiting (null);
	    return (null);
	}

	String s = Utils.findLongest (v.elements(), name);
	logger.finest (s);

	// then, populate a DBZone with what we found.
	try
	{
	    ResultSet rs = stmt.executeQuery
		("SELECT * FROM domains WHERE name = '" + s + "'");

	    rs.next();
	    int domainId = rs.getInt ("id");
	    logger.finest (domainId);

	    Utils.Assert (! rs.next());

	    logger.exiting (s);
	    return (new DBZone (s, domainId, this));
	}
	catch (java.sql.SQLException sqle)
	{
	    logger.throwing (sqle);
	}
	logger.exiting (null);
	return (null);
    }

    public Vector get (int type, String name, int domainId)
    {
	logger.entering (type);
	logger.entering (name);
	logger.entering (domainId);

	try
	{
	    String stype = Utils.mapTypeToString (type);
	    logger.finest (stype);
	    Vector ret = new Vector();
	    ResultSet rs = stmt.executeQuery
		("SELECT * FROM records where domain_id = " + domainId +
		     " AND name = \"" + name + "\"" +
		     " AND type = \"" + stype + "\"");

	    while (rs.next())
	    {
		String dbname = rs.getString ("name");
		String dbcontent = rs.getString ("content");
		String dbtype = rs.getString ("type");
		int dbttl = rs.getInt ("ttl");
		int dbprio = rs.getInt ("prio");

		switch (type)
		{
		    case Utils.SOA:
		    {
			String s[] = dbcontent.split ("\\s+");
			ret.add (new SOARR (dbname, s[0], s[1],
			    Integer.parseInt (s[2]), Integer.parseInt (s[3]),
			    Integer.parseInt (s[4]), Integer.parseInt (s[5]),
			    Integer.parseInt (s[6]), dbttl));
			break;
		    }
		    case Utils.NS:
		    {
			ret.add (new NSRR (dbname, dbttl, dbcontent));
			break;
		    }
		    case Utils.A:
		    {
			ret.add (new ARR (dbname, dbttl, dbcontent));
			break;
		    }
		    case Utils.AAAA:
		    {
			ret.add (new AAAARR (dbname, dbttl, dbcontent));
			break;
		    }
		    case Utils.MX:
		    {
			ret.add (new MXRR (dbname, dbttl, dbcontent, dbprio));
			break;
		    }
		    case Utils.TXT:
		    {
			ret.add (new TXTRR (dbname, dbttl, dbcontent));
			break;
		    }
		    case Utils.CNAME:
		    {
			ret.add (new CNAMERR (dbname, dbttl, dbcontent));
			break;
		    }
		    case Utils.PTR:
		    {
			ret.add (new PTRRR (dbname, dbttl, dbcontent));
			break;
		    }
		    case Utils.HINFO:
		    {
			String s[] = dbcontent.split ("\\s+");
			ret.add (new HINFORR (dbname, dbttl, s[0], s[1]));
			break;
		    }
		    default:
		    {
			logger.warning ("requested type " + type +
			    " for " + name + " not found");
			return (null);
		    }
		}
	    }

	    return (ret.size() == 0 ? null : ret);
	}
	catch (java.sql.SQLException sqle)
	{
	    logger.throwing (sqle);
	}
	return (null);
    }
}
