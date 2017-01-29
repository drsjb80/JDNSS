import edu.msudenver.cs.jclo.JCLO;

class ExampleArgs
{
    private int a;
    private boolean b;
    private float c;
    private String d;
    private String[] additional;
}

public class Example
{

    public static void main (String args[])
    {
        JCLO jclo = new JCLO (new ExampleArgs());
        jclo.parse (args);
	System.out.println ("a = " + jclo.getInt ("a"));
	System.out.println ("b = " + jclo.getBoolean ("b"));
	System.out.println ("c = " + jclo.getFloat ("c"));
	System.out.println ("d = " + jclo.getString ("d"));
	System.out.println ("additional = " +
	    java.util.Arrays.toString (jclo.getStrings ("additional")));
    }
}
