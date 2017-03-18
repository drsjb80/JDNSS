package edu.msudenver.cs.jdnss;

import java.util.logging.Logger;
import edu.msudenver.cs.javaln.JavaLN;

class TestSem extends Thread
{
    private static Semaphore s = new Semaphore(5);

    public void run()
    {
        System.out.println(this + " before P()");
        s.P();
        System.out.println(this + " after P()");

        try { Thread.sleep(10 * 1000); }
        catch (InterruptedException e) { e.printStackTrace(); }

        System.out.println(this + " before V()");
        s.V();
        System.out.println(this + " after V()");
    }
}

public class Semaphore
{
    private int total;
    private int count = 0;
    private JavaLN logger = JDNSS.getLogger();

    public Semaphore(int total)
    {
        this.total = total;
    }

    public synchronized void P()
    {
        String name = Thread.currentThread().getName();
        logger.finest("P: count = " + count + " " +  name);

        while (count >= total)
        {
            logger.finest(name + " blocked");

            try
            {
                wait();
            }
            catch (Exception e)
            {
                logger.throwing(e);
            }

            logger.finest(name + " unblocked");
        }

        count++;
    }

    public synchronized void V()
    {
        logger.finest("V: count = " + count + " " +
        Thread.currentThread().getName());
        count--;
        if (count > 0)
        {
            notify();
        }
    }

    public static void main(String args[])
    {
        for (int i = 0; i < 10; i++)
        {
            new TestSem().start();
        }
    }
}
