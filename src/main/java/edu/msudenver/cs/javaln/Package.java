package edu.msudenver.cs.javaln;

import java.util.logging.Logger;

class One
{
    public static void One (Logger base)
    {
        Logger logger = JavaLN.getLogger ("Package");

	if (! logger.equals (base))
	    throw new Error ("loggers not the same!");

	logger.severe ("One");
    }
}

class Two
{
    public static void Two (Logger base)
    {
        Logger logger = JavaLN.getLogger ("Package");

	if (logger != base)
	    throw new Error ("loggers not the same!");

	logger.severe ("Two");
    }
}

class Three
{
    public static void Three (Logger base)
    {
        Logger logger = new JavaLN ("Package");

	if (logger == base)
	    throw new Error ("loggers the same!");

	logger.severe ("Three");
    }
}

class Four
{
    public static void Four (Logger base)
    {
        Logger logger = new JavaLN ("Package");

	if (logger == base)
	    throw new Error ("loggers the same!");

	logger.severe ("Four");
    }
}

public class Package
{
    public static void main (String args[])
    {
        Logger logger = JavaLN.getLogger ("Package");

	One.One (logger);
	Two.Two (logger);
	Three.Three (logger);
	Four.Four (logger);
	logger.severe ("Package");
    }
}
