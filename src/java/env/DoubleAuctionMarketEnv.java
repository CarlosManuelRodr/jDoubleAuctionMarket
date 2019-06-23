package env;

import jason.NoValueException;
import jason.asSyntax.*;
import jason.environment.TimeSteppedEnvironment;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.Locale;
import java.util.Calendar;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import jason.asSyntax.directives.FunctionRegister;
import tools.*;

/**
 * Types of price policies available to the market maker.
 */
enum PricePolicy {MAX, MIN, AVG, SPREAD};

public class DoubleAuctionMarketEnv extends TimeSteppedEnvironment
{
	static
	{
		FunctionRegister.addFunction(beta.class);
		FunctionRegister.addFunction(uniformInt.class);
		FunctionRegister.addFunction(normal.class);
		FunctionRegister.addFunction(movingAverage.class);
		FunctionRegister.addFunction(mathematicaStrategy.class);
	}

	static Logger logger = Logger.getLogger(DoubleAuctionMarketEnv.class.getName());

	/// Price policy (i.e. min, max, avg, spread)
	private PricePolicy pricePolicy = PricePolicy.MIN;

	private int nbTraders = 10000;	    /// Number of traders
	private int nbSteps = 1000000;      /// Number of steps to be simulated
	private float operationTax;         /// Percentage for the market maker (spread)
	private double managerIncome = 0;   /// Amount of money of the market maker
	private double marketPrice = 0;     /// Market price
	private double oldMarketPrice = 0;  /// Market price at step-1
	private ArrayList<Term> priceList = new ArrayList<Term>();

	/// Number of broken agents
	private AtomicInteger brokensReceived = new AtomicInteger();
	/// Maximum percentage of broken traders (if greater, stop simulation)
	private float brokensAllowed = 1;

	private boolean end = false;        /// End flag activated when reaching the final step
	private Integer endsReceived = 0;	/// Agents ready for the MAS finalization

	private ConcurrentSkipListSet<Operation> listOfBuys = new ConcurrentSkipListSet<Operation>();  // List of BUY operations
	private ConcurrentSkipListSet<Operation> listOfSells = new ConcurrentSkipListSet<Operation>(); // List of SELL operations

	private PrintWriter fmarket;	    /// Market log file
	private PrintWriter ftraders;		/// Traders log file
	private PrintWriter fmarketCSV;		/// Market log file CSV
	private PrintWriter ftradersCSV;	/// Traders log file CSV

	public DoubleAuctionMarketEnv()
	{		
		// Open log files
		File folder=new File("./logs");
		folder.mkdir();
		Calendar date = Calendar.getInstance();
		String dateFolder = date.get(Calendar.YEAR) + "-" + date.get(Calendar.MONTH)
			+ "-" + date.get(Calendar.DAY_OF_MONTH) + "_" + date.get(Calendar.HOUR_OF_DAY)
			+ ":" + date.get(Calendar.MINUTE);
		folder=new File("./logs/" + dateFolder + "/");
		folder.mkdir();
		try
		{
			String header = "Step Price ManagerIncome NbOp NbSh\n";
			fmarket = new PrintWriter(new FileWriter("./logs/" + dateFolder + "/market.txt"));
			fmarket.print(header);
			header = "Step,Price,ManagerIncome,NbOp,NbSh\n";
			fmarketCSV = new PrintWriter(new FileWriter("./logs/" + dateFolder + "/market.csv"));
			fmarketCSV.print(header);

			header = "TraderName InitIncome InitShares OpProb SellProb Income Shares " 
			         + "Wealth NbSellSuccess NbSellFailures NbBuySuccess NbBuyFailures\n";
			ftraders = new PrintWriter(new FileWriter("./logs/" + dateFolder + "/traders.txt"));
			ftraders.print(header);
			header = "TraderName,InitIncome,InitShares,OpProb,SellProb,Income,Shares,"
					+ "Wealth,NbSellSuccess,NbSellFailures,NbBuySuccess,NbBuyFailures\n";
			ftradersCSV = new PrintWriter(new FileWriter("./logs/" + dateFolder + "/traders.csv"));
			ftradersCSV.print(header);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

    @Override
	public void init(String[] args)
    {
		super.init(new String[] { args[5] } );

		logger.info("Initiating simulation");

		nbTraders = Integer.parseInt(args[0]);
		pricePolicy = PricePolicy.valueOf(args[1].toUpperCase());
		operationTax = Float.parseFloat(args[2])/100;
		nbSteps = Integer.parseInt(args[3]);
		brokensAllowed = Float.parseFloat(args[4])/100;
	}

	@Override
	public void stop()
	{
		super.stop();

		// Close log files
		fmarket.close();
		ftraders.close();
		fmarketCSV.close();
		ftradersCSV.close();
	}


	/**
	 * actions to execute at the beginning of an iteration before each agents step
	 *
	 * @param step	number of actual step
	 */
	@Override
	protected void stepStarted(int step)
	{
		clearAllPercepts();

		// End of simulation
		if (end)
		{
			try
			{
				// Jason 2.4 adds time to stopMAS
				// getEnvironmentInfraTier().getRuntimeServices().stopMAS;
				getEnvironmentInfraTier().getRuntimeServices().stopMAS();
			}
			catch (Exception e){
				e.printStackTrace();
				System.exit(1);
			}
		}

		// Close market and set current step
		if (step <= nbSteps && brokensReceived.get() <= brokensAllowed * nbTraders)
		{
			// Add to keep the old market place if no operation is performed
			oldMarketPrice = marketPrice;
			marketPrice = 0;

			// Market matching
			//logger.info("List of buys: " + listOfBuys);
			//logger.info("List of sells: " + listOfSells);
			Operation opBuy = null, opSell = null;
			String buyerName, sellerName;
			int buyShares, sellShares, sharesTraded, sellerIdOperation, buyerIdOperation;
			double priceSell, priceBuy, priceOpSell, priceOpBuy, spread;
			Literal l_success_buy, l_success_sell;
			NumberTerm n_sharesTraded, n_priceOpSell, n_priceOpBuy;
			Iterator<Operation> itSells = listOfSells.iterator();
			Iterator<Operation> itBuys = listOfBuys.iterator();
			boolean priceCrossing = false;
			boolean availableOperations = itSells.hasNext() && itBuys.hasNext();
			int nbOperations = 0, nbSharesTraded = 0;

			if (availableOperations)
			{
				opSell = itSells.next();
				opBuy = itBuys.next();
			}
			
			while (!priceCrossing && availableOperations)
			{
				priceSell = opSell.getPrice();
				priceBuy = opBuy.getPrice();
				
				if (priceSell <= priceBuy)
				{
					nbOperations++;
					sellerName = opSell.getAgentName();
					buyerName = opBuy.getAgentName();
					sellerIdOperation = opSell.getAgentIdOperation();
					buyerIdOperation = opBuy.getAgentIdOperation();
					sellShares = opSell.getShares();
					buyShares = opBuy.getShares();

					// Compute shares exchanged
					if (sellShares < buyShares)
					{
						sharesTraded = sellShares;
						opBuy.setShares(buyShares - sharesTraded);
						
						if (itSells.hasNext())
							opSell = itSells.next();
						else
							availableOperations = false;
					}
					else if (sellShares > buyShares)
					{
						sharesTraded = buyShares;
						opSell.setShares(sellShares - sharesTraded);
						
						if (itBuys.hasNext())
							opBuy = itBuys.next();
						else
							availableOperations = false;
					}
					else // (sellShares == buyShares)
					{
						sharesTraded = sellShares;
						
						if (itSells.hasNext() && itBuys.hasNext())
						{
							opSell = itSells.next();
							opBuy = itBuys.next();
						}
						else
							availableOperations = false;
					}
					n_sharesTraded = ASSyntax.createNumber(sharesTraded);

					// Calculate operation prices
					switch(pricePolicy)
					{
						case MAX:
							priceOpSell = priceBuy;
							priceOpBuy = priceOpSell;
							break;
						case MIN:
							priceOpSell = priceSell;
							priceOpBuy = priceOpSell;
							break;
						case AVG:
							priceOpSell = (priceSell + priceBuy) / 2;
							priceOpBuy = priceOpSell;
							break;
						case SPREAD:
							spread = (priceBuy - priceSell);
							priceOpSell = priceSell + (1 - operationTax) * spread / 2;
							priceOpBuy = priceBuy - (1 - operationTax) * spread / 2;
							managerIncome += sharesTraded * operationTax * spread;
							break;
						default:
							priceOpSell = priceSell;
							priceOpBuy = priceOpSell;
							break;
					}
					n_priceOpSell = ASSyntax.createNumber(priceOpSell);
					n_priceOpBuy = ASSyntax.createNumber(priceOpBuy);

					// Compute market price
					marketPrice += (priceOpSell + priceOpBuy)/2;

					// Notify traders
					l_success_sell = ASSyntax.createLiteral("operation_success",
														    ASSyntax.createNumber(sellerIdOperation),
														    ASSyntax.createAtom(buyerName),
														    n_sharesTraded,
														    n_priceOpSell);
					addPercept(sellerName, l_success_sell);
					l_success_buy = ASSyntax.createLiteral("operation_success",
														   ASSyntax.createNumber(buyerIdOperation),
														   ASSyntax.createAtom(sellerName),
														   n_sharesTraded,
														   n_priceOpBuy);
					addPercept(buyerName, l_success_buy);
					nbSharesTraded += sharesTraded;
				}
				else
					priceCrossing = true;
			}
			
			if (marketPrice > 0)
				marketPrice = marketPrice/nbOperations;
			else
				marketPrice += oldMarketPrice;

			try
			{
				fmarket.printf(Locale.US, "%4d %5.2f %13.2f %4d %4d\n",
								step, marketPrice, managerIncome, nbOperations, nbSharesTraded);
				fmarketCSV.printf(Locale.US, "%d,%.2f,%.2f,%d,%d\n",
								  step, marketPrice, managerIncome, nbOperations, nbSharesTraded);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			// Market clean-up
			listOfBuys.clear();
			listOfSells.clear();

			logger.info("Starting step " + step);
			if (marketPrice > 0)
			{
				Literal l_market_price = ASSyntax.createLiteral("market_price", ASSyntax.createNumber(marketPrice));
				addPercept(l_market_price);
			}
			Literal l_new_step = ASSyntax.createLiteral("step", ASSyntax.createNumber(step));
			addPercept(l_new_step);
			
			// Add market price to list
			priceList.add(ASSyntax.createNumber(marketPrice));
			if (priceList.size() > 100)
				priceList.remove(0);
			
			Literal l_price_list = ASSyntax.createLiteral("price_list", ASSyntax.createList(priceList));
			addPercept(l_price_list);
		}
		else
		{
			logger.info("Ending simulation");
			Literal l_end = ASSyntax.createAtom("end");
			addPercept(l_end);
		}
	}

	/**
	 * actions to execute after each agents step
	 *
	 * @param step		number of current step
	 * @param time		duration of the step
	 * @param timeout	if timeout has been exceeded
	 */
	@Override
    protected void stepFinished(int step, long time, boolean timeout)
	{
		if (timeout)
			logger.info("Timeout reached in step " + step);
	}

    @Override
	public boolean executeAction(String ag, Structure action)
	{
		if (action.getFunctor().equals("sell"))
		{
			int agent_id_operation;
			try
			{
				agent_id_operation = (int)((NumberTerm)action.getTerm(0)).solve();
				int shares = (int)((NumberTerm)action.getTerm(1)).solve();
				double price = ((NumberTerm)action.getTerm(2)).solve();
				listOfSells.add(new Operation(OperationType.SELL, ag, agent_id_operation, shares, price));
			} 
			catch (NoValueException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		else if (action.getFunctor().equals("buy"))
		{
			int agent_id_operation;
			try
			{
				agent_id_operation = (int)((NumberTerm)action.getTerm(0)).solve();
				int shares = (int)((NumberTerm)action.getTerm(1)).solve();
				double price = ((NumberTerm)action.getTerm(2)).solve();
				listOfBuys.add(new Operation(OperationType.BUY, ag, agent_id_operation, shares, price));
			}
			catch (NoValueException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (action.getFunctor().equals("none"))
		{
			// Trader performs no operation
		}
		else if (action.getFunctor().equals("broken"))
		{
			brokensReceived.incrementAndGet();
		}
		else if (action.getFunctor().equals("start"))
		{
			// Trader initialization
		}
		else if (action.getFunctor().equals("end"))
		{
			// Read parameters
			// log_trader(MyName,InitIncome,InitShares,OpProb,SellProb,Inc,Shares,
			// NbSellSucc,NbSellFail,NbBuySucc,NbBuyFail)
			String traderName = ((Atom)action.getTerm(0)).getFunctor();
			double initIncome;
			
			try
			{
				initIncome = ((NumberTerm)action.getTerm(1)).solve();
				int initShares = (int)((NumberTerm)action.getTerm(2)).solve();
				double opProb = ((NumberTerm)action.getTerm(3)).solve();
				double sellProb = ((NumberTerm)action.getTerm(4)).solve();
				double income = ((NumberTerm)action.getTerm(5)).solve();
				int shares = (int)((NumberTerm)action.getTerm(6)).solve();
				int nbSellSuccess = (int)((NumberTerm)action.getTerm(7)).solve();
				int nbSellFailures = (int)((NumberTerm)action.getTerm(8)).solve();
				int nbBuySuccess = (int)((NumberTerm)action.getTerm(9)).solve();
				int nbBuyFailures = (int)((NumberTerm)action.getTerm(10)).solve();

				// Save info
				synchronized(endsReceived)
				{
					try
					{
						ftraders.printf(Locale.US, "%-10s %10.2f %10d %6.2f %8.2f %6.2f "
								+ "%5d %6.2f %13d %14d %12d %13d\n",
						                  traderName, initIncome, initShares, opProb, sellProb,
										  income, shares, income+shares*marketPrice,
										  nbSellSuccess, nbSellFailures, nbBuySuccess, nbBuyFailures);
						ftradersCSV.printf(Locale.US, "%s,%.2f,%d,%.2f,%.2f,%.2f,%d,%.2f,"
								+ "%d,%d,%d,%d\n",
						                  traderName, initIncome, initShares, opProb, sellProb,
										  income, shares, income+shares*marketPrice,
										  nbSellSuccess, nbSellFailures, nbBuySuccess, nbBuyFailures);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						return false;
					}

					endsReceived++;
					if (endsReceived == nbTraders)
					{
						end = true;
					}
				}

			} 
			catch (NoValueException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} 
		else 
		{
		   	return false;
	    }

		return true;
	}
}
