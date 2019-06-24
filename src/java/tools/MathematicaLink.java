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
	static boolean busy = false;
	static Logger logger = Logger.getLogger(DoubleAuctionMarketEnv.class.getName());
	
	public static void StartLink() throws MathLinkException
	{
		logger.info("Mathlink started");
        ml = MathLinkFactory.createKernelLink("-linkmode launch -linkname 'math -mathlink'");
        
		// Get rid of the initial InputNamePacket the kernel will send
        // when it is launched.
        ml.discardAnswer();
        PrintWorkingDirectory();
        
        // Load strategy as an external Wolfram Language package
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
}
