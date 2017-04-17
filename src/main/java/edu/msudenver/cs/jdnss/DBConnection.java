package edu.msudenver.cs.jdnss;

import java.util.Vector;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

class DBConnection
{
    private Connection conn;
    private Statement stmt;
    private static Logger logger = JDNSS.getLogger();

    // com.mysql.jdbc.Driver
    // jdbc:mysql://localhost/JDNSS
    DBConnection(final String dbClass, final String dbURL, final String dbUser,
        final String dbPass)
    {
        String user = dbUser == null ? "" : dbUser;
        String pass = dbPass == null ? "" : dbPass;

        // load up the class
        try
        {
            Class.forName(dbClass);
        }
        catch (ClassNotFoundException cnfe)
        {
            logger.catching(cnfe);
        }

        try
        {
            conn = DriverManager.getConnection(dbURL, user, pass);
        }
        catch (SQLException sqle)
        {
            logger.catching(sqle);
            Assertion.aver(false);
        }

        try
        {
            stmt = conn.createStatement();
        }
        catch (SQLException sqle)
        {
            try
            {
                stmt.close();
                conn.close();
            }
            catch (SQLException sqle2)
            {
                logger.catching(sqle);
                Assertion.aver(false);
            }
        }
    }

    public DBZone getZone(final String name)
    {
        logger.traceEntry(new ObjectMessage(name));

        Vector v = new Vector();

        // first, get them all
        ResultSet rs = null;
        try
        {
            rs = stmt.executeQuery("SELECT * FROM domains");

            while (rs.next())
            {
                v.add(rs.getString("name"));
            }
        }
        catch (SQLException sqle)
        {
            try
            {
                stmt.close();
                conn.close();
            }
            catch (SQLException sqle2)
            {
                logger.catching(sqle);
                Assertion.aver(false);
            }
        }

        Assertion.aver(v.size() != 0);

        // then, find the longest that matches
        String s = Utils.findLongest(v.elements(), name);
        logger.trace(s);

        // then, populate a DBZone with what we found.
        try
        {
            rs = stmt.executeQuery
               ("SELECT * FROM domains WHERE name = '" + s + "'");

            rs.next();
            final int domainId = rs.getInt("id");
            logger.trace(domainId);

            Assertion.aver(! rs.next());

            logger.traceExit(s);
            return new DBZone(s, domainId, this);
        }
        catch (SQLException sqle)
        {
            try
            {
                rs.close();
                stmt.close();
                conn.close();
            }
            catch (SQLException sqle2)
            {
                logger.catching(sqle);
                Assertion.aver(false);
            }
        }

        Assertion.aver(false, "DBConnection failed");
        return null;    // have to have this or javac complains
    }

    public Vector get(final int type, final String name, final int domainId)
    {
        logger.traceEntry(new ObjectMessage(type));
        logger.traceEntry(new ObjectMessage(name));
        logger.traceEntry(new ObjectMessage(domainId));

        try
        {
            String stype = Utils.mapTypeToString(type);
            logger.trace(stype);
            Vector ret = new Vector();
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM records where domain_id = " + domainId +
                " AND name = \"" + name + "\"" +
                " AND type = \"" + stype + "\"");

            while (rs.next())
            {
                String dbname = rs.getString("name");
                String dbcontent = rs.getString("content");
                String dbtype = rs.getString("type");
                int dbttl = rs.getInt("ttl");
                int dbprio = rs.getInt("prio");

                switch (type)
                {
                    case Utils.SOA:
                    {
                        String s[] = dbcontent.split("\\s+");
                        ret.add(new SOARR(dbname, s[0], s[1],
                            Integer.parseInt(s[2]), Integer.parseInt(s[3]),
                            Integer.parseInt(s[4]), Integer.parseInt(s[5]),
                            Integer.parseInt(s[6]), dbttl));
                        break;
                    }
                    case Utils.NS:
                    {
                        ret.add(new NSRR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case Utils.A:
                    {
                        ret.add(new ARR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case Utils.AAAA:
                    {
                        ret.add(new AAAARR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case Utils.MX:
                    {
                        ret.add(new MXRR(dbname, dbttl, dbcontent, dbprio));
                        break;
                    }
                    case Utils.TXT:
                    {
                        ret.add(new TXTRR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case Utils.CNAME:
                    {
                        ret.add(new CNAMERR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case Utils.PTR:
                    {
                        ret.add(new PTRRR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case Utils.HINFO:
                    {
                        final String s[] = dbcontent.split("\\s+");
                        ret.add(new HINFORR(dbname, dbttl, s[0], s[1]));
                        break;
                    }
                    default:
                    {
                        logger.warn("requested type " + type +
                        " for " + name + " not found");
                        Assertion.aver(false);
                    }
                }
            }

            Assertion.aver(ret.size() != 0);
            return ret;
        }
        catch (SQLException sqle)
        {
            logger.catching(sqle);
            Assertion.aver(false);
        }

        Assertion.aver(false);
        return null;    // have to have this or javac complains
    }
}
