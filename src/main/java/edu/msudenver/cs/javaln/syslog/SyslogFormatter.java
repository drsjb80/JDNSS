package edu.msudenver.cs.javaln.syslog;

// http://www.faqs.org/rfcs/rfc3164.html

import java.util.logging.*;

/**
 * Format a message so that it is acceptable to syslogd.
 */

public abstract class SyslogFormatter extends Formatter
{
    private final static int Emergency = 0;
    private final static int Alert = 1;
    private final static int Critical = 2;
    private final static int Error = 3;
    private final static int Warning = 4;
    private final static int Notice = 5;
    private final static int Informational = 6;
    private final static int Debug = 7;

    public final static int Kern = 0;
    public final static int User = 1;
    public final static int Mail = 2;
    public final static int Daemon = 3;
    public final static int Auth = 4;
    public final static int Syslog = 5;
    public final static int LPR = 6;
    public final static int News = 7;
    public final static int UUCP = 8;
    public final static int Cron = 9;
    public final static int Authpriv = 10;
    public final static int FTP = 11;
    public final static int Local0 = 16;
    public final static int Local1 = 17;
    public final static int Local2 = 18;
    public final static int Local3 = 19;
    public final static int Local4 = 20;
    public final static int Local5 = 21;
    public final static int Local6 = 22;
    public final static int Local7 = 23;

    protected int facility = User;

    public SyslogFormatter ()
    {
        super();

	String f = LogManager.getLogManager().
	    getProperty (getClass().getName() + ".facility");

	if (f != null)
	    facility = mapFacility (f);
    }

    public SyslogFormatter (int facility)
    {
        super();
	this.facility = facility;
    }

    protected static int mapLevel (Level level)
    {
	// these really don't match up well...
        if (level == Level.SEVERE) return (Emergency);
	if (level == Level.WARNING) return (Alert);
	if (level == Level.INFO) return (Critical);
	if (level == Level.CONFIG) return (Error);
	if (level == Level.FINE) return (Warning);
	if (level == Level.FINER) return (Notice);
	if (level == Level.FINEST) return (Informational);
	return (Debug);
    }

    protected int mapFacility (String facility)
    {
	if (facility.equalsIgnoreCase ("Kern")) return (0);
	if (facility.equalsIgnoreCase ("User")) return (1);
	if (facility.equalsIgnoreCase ("Mail")) return (2);
	if (facility.equalsIgnoreCase ("Daemon")) return (3);
	if (facility.equalsIgnoreCase ("Auth")) return (4);
	if (facility.equalsIgnoreCase ("Syslog")) return (5);
	if (facility.equalsIgnoreCase ("LPR")) return (6);
	if (facility.equalsIgnoreCase ("News")) return (7);
	if (facility.equalsIgnoreCase ("UUCP")) return (8);
	if (facility.equalsIgnoreCase ("Cron")) return (9);
	if (facility.equalsIgnoreCase ("Authpriv")) return (10);
	if (facility.equalsIgnoreCase ("FTP")) return (11);
	if (facility.equalsIgnoreCase ("Local0")) return (16);
	if (facility.equalsIgnoreCase ("Local1")) return (17);
	if (facility.equalsIgnoreCase ("Local2")) return (18);
	if (facility.equalsIgnoreCase ("Local3")) return (19);
	if (facility.equalsIgnoreCase ("Local4")) return (20);
	if (facility.equalsIgnoreCase ("Local5")) return (21);
	if (facility.equalsIgnoreCase ("Local6")) return (22);
	if (facility.equalsIgnoreCase ("Local7")) return (23);
	return (1);
    }

    public abstract String getPrefix (int level);

    public String format (LogRecord rec)
    {
	String ret = getPrefix (mapLevel (rec.getLevel())) + " "
	    + rec.getSourceClassName() + " " + rec.getSourceMethodName() + " "
	    + rec.getLevel() + ": " + formatMessage (rec);
	return (ret);
    }
}
