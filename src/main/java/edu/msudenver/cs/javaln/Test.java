import edu.msudenver.cs.javaln.JavaLN;

public class Test
{
    public static void main(String[] args)
    {
	JavaLN logger = new JavaLN();
	logger.useLineNumberFormatter (true);
	logger.severe ("severe");

	logger.useLineNumberFormatter (false);
	logger.severe ("severe");

	/*
	Handler h[] = logger.getHandlers();
	for (int i = 0; i < h.length; i++)
	{
	    System.out.println (h[i]);
	    System.out.println ("level: " + h[i].getLevel());
	    System.out.println ("filter: " + h[i].getFilter());
	}
	*/
    }
}
