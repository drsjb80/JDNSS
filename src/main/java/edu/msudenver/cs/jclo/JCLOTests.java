package edu.msudenver.cs.jclo;

import java.io.File;
import java.io.IOException;

public class JCLOTests
{
    private static void runTests (String[][] tests)
    {
	for (int i = 0; i < tests.length; i++)
	{
	    System.out.println ("----------");
	    System.out.println ("tests[" + i + "]" +
		java.util.Arrays.toString (tests[i]));

	    try
	    {
		JCLO jclo = new JCLO (new JCLOArgs());
		jclo.parse (tests[i]);
		System.out.println (jclo);
	    }
	    catch (IllegalArgumentException e)
	    {
		System.out.println ("Caught an IllegalArgumentException" + e);
	    }
	}
    }

    private static void doubleDashTests()
    {
	String doubleDashTests[][] =
	{
	    {},
	    {"--debug"},
	    {"--debug=true"},
	    {"--debug=yes"},
	    {"--debug=YES"},
	    {"--debug=false"},
	    {"--font-size"},
	    {"--font-style=BOLD"},
	    {"--font-size=10", "--font-style=BOLD", "--font-name=foo",
		"--debug"},
	    {"--none=none"},
	    {"--font-size=10.5"},
	    {"--1"},
	    {"--debug", "one", "two", "three"},
	};

	runTests (doubleDashTests);
    }

    private static void singleDashTests()
    {
	String singleDashTests[][] =
	{
	    {},
	    {"-debug"},
	    {"-font-size", "10"},
	    {"-font-style", "BOLD"},
	    {"-font-size", "10", "-font-style", "BOLD", "-font-name", "foo",
		"-debug"},
	    {"-none", "none"},
	    {"-font-size", "10.5"},
	    {"-1"},
	    {"-Djava.util.logging.config.file=MethodFilter.props"},
	    {"-debug", "one", "two", "three"},
	};

	runTests (singleDashTests);
    }


    private static void mixedDashTests()
    {
	String singleDashTests[][] =
	{
	    {},
	    {"-debug"},
	    {"--debug"},
	    {"--debug=true"},
	    {"-font-size", "10"},
	    {"--font-size=10"},
	    {"-font-style", "BOLD"},
	    {"-font-size", "10", "--font-style=BOLD", "-font-name", "foo",
		"-debug"},
	    {"-none", "none"},
	    {"-font-size", "10.5"},
	    {"-1"},
	    {"-debug", "one", "two", "three"},
	};

	runTests (singleDashTests);
    }

    private static void JCLOnlyTests()
    {
	System.out.println ("----------");
	try
	{
	    JCLO jclo = new JCLO ("JCLO", new JCLOnly());
	    jclo.parse (new String[]{"--debug=true", "--1"});
	    System.out.println (jclo);
	}
	catch (IllegalArgumentException e)
	{
	    System.out.println ("Caught an IllegalArgumentException" + e);
	}
    }

    /**
     *	Unit tests
     */
    public static void main (String args[]) throws IOException
    {
	// cheesy, i know...
	if (args.length == 1 && args[0].equalsIgnoreCase ("--version"))
	{
	    System.out.println (Version.getVersion());
	}
	else
	{
	    doubleDashTests();
	    singleDashTests();
	    mixedDashTests();
	    JCLOnlyTests();
	}

	String a[] = {"--help", "additional"};
	JCLO j = new JCLO (new JCLOArgs());
	j.parse (a);
	// j.saveXML (new File ("JCLOArgs.xml"));
    }
}

class JCLOArgs
{
    private int font__size;
    private String font__name;
    private String font__style;
    private boolean debug;
    private boolean d;
    private boolean _1;
    private String Accept[];
    private int ints[];
    private boolean help;
    private String Djava_$util_$logging_$config_$file;

    private String additional[];
}

class JCLOnly
{
    private int font__size;
    private String font__name;
    private String font__style;
    private boolean JCLOdebug;
    private boolean JCLO_1;

    private String JCLOadditional[];
}

class Equivalent
{
    int one, two;
    String equivalent[][] = {{"o", "one"}, {"t", "two" }};
}
