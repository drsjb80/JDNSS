import edu.msudenver.cs.jclo.JCLO;

import java.io.File;
import java.io.IOException;

public class Main
{
    private int JCLOa;
    private boolean JCLOb;
    private int a;
    private boolean b;

    public String toString()
    {
        return ("JCLOa = " + JCLOa + " JCLOb = " + JCLOb +
	    " a = " + a + " b = " + b);
    }

    public static void main (String args[])
    {
        Main main = new Main();
	System.out.println ("before: " + main);
        JCLO jclo = new JCLO ("JCLO", main);
        jclo.parse (args);
	System.out.println (jclo.usage());
	System.out.println ("after: " + main);
    }
}
