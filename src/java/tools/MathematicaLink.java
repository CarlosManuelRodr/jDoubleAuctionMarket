package tools;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.MathLinkFactory;

import env.DoubleAuctionMarketEnv;

public class MathematicaLink
{
	private static KernelLink ml = null;
	static Logger logger = Logger.getLogger(DoubleAuctionMarketEnv.class.getName());
	
	public static void StartLink() throws MathLinkException
	{
		logger.info("Mathlink started");
        ml = MathLinkFactory.createKernelLink("-linkmode launch -linkname 'math -mathlink'");
		// Get rid of the initial InputNamePacket the kernel will send
        // when it is launched.
        ml.discardAnswer();
        PrintWorkingDirectory();
        
        ml.evaluate("<< Strategy.wl");
        ml.discardAnswer();
	}
	
	public static void PrintWorkingDirectory() throws MathLinkException
	{
		ml.evaluate("Directory[]");
        ml.waitForAnswer();
        logger.info("Mathematica directory: " + ml.getString());
	}
	
	public static boolean ExecuteStrategy(Integer window1, Integer window2, ArrayList<Double> priceList) throws MathLinkException
	{
		String f = "Strategy["
				+ window1.toString() + ", " 
				+ window2.toString() + ", " 
				+ IntegerListToString(priceList) + "]";
		
		logger.info("WL Evaluating: " + f);
        ml.evaluate(f);
        ml.waitForAnswer();
        boolean result = ml.getBoolean();
		return result;
	}
	
	public static String IntegerListToString(ArrayList<Double> list)
	{
		String output = "{";
		for (int i = 0; i < list.size(); i++)
		{
			output += list.get(i).toString();
			if (i != list.size() - 1)
				output += ", ";
		}
		
		output += "}";
		return output;
	}
	
	public static void Test()
	{
        try
        {        	
            ml.evaluate("SayPuto[]");
            ml.waitForAnswer();
            String asd = ml.getString();
            System.out.println(asd);
            
            ml.evaluate("2+2");
            ml.waitForAnswer();

            int result = ml.getInteger();
            System.out.println("2 + 2 = " + result);


            // Here's how to send the same input, but not as a string:
            /*ArrayList<Integer> lista = new ArrayList<Integer>();
            lista.add(10);
            lista.add(15);
            lista.add(20);
            lista.add(25);
            
            String f = "Fuck[" + IntegerListToString(lista) + "]";
            ml.evaluate(f);
            ml.waitForAnswer();
            double jaja = ml.getDouble();
            System.out.println(jaja);*/
        }
        catch (MathLinkException e)
        {
            System.out.println("MathLinkException occurred: " + e.getMessage());
        }
        finally
        {
            ml.close();
        }
	}
}
