package edu.msudenver.cs.javaln.syslog;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.ErrorManager;
import java.util.logging.LogManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;

public abstract class SyslogHandler extends Handler
{
    protected static boolean open = true;

    public SyslogHandler()
    {
	LogManager lm = LogManager.getLogManager();
	String level = lm.getProperty (getClass().getName() + ".level");
	String filter = lm.getProperty (getClass().getName() + ".filter");
	String formatter = lm.getProperty (getClass().getName() + ".formatter");

	if (level != null) setLevel (Level.parse (level));

	try
	{
	    if (filter != null)
	    {
		setFilter ((Filter) ClassLoader.getSystemClassLoader().
		    loadClass (filter).newInstance());
	    }
	    if (formatter != null)
	    {
		setFormatter ((Formatter) ClassLoader.getSystemClassLoader().
		    loadClass (formatter).newInstance());
	    }
	}
	catch (Exception e)
	{
	    reportError (null, e, ErrorManager.OPEN_FAILURE);
	}
    }

    public synchronized void publish (LogRecord record)
    {
	if (!isLoggable(record) || !open) return;

	String message;
	try
	{
 	    message = getFormatter().format (record);
	}
	catch (Exception ex)
	{
	    reportError (null, ex, ErrorManager.FORMAT_FAILURE);
	    return;
	}

	if (message.length() > 1024) message = message.substring (0, 1023);

	sendMessage (message);
    }

    protected abstract void sendMessage (String message);

    public synchronized void close() {}

    public synchronized void flush() {}
}
