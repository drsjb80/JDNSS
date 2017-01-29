public class GetOS
{
    public static void main (String args[])
    {
	System.out.println (System.getProperty ("os.name").
	    replaceAll (" ", "") + "-" + System.getProperty ("os.arch"));
    }
}
