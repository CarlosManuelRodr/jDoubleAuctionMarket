package env;

import jason.NoValueException;
import jason.asSyntax.*;
//import jason.environment.TimeSteppedEnvironment;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jaca.*;

import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import jason.asSyntax.directives.FunctionRegister;
import tools.*;

/**
 * Types of price policies available to the market maker.
 */
enum PricePolicy {MAX, MIN, AVG, SPREAD};

public class DoubleAuctionMarketEnv extends CartagoEnvironment
{
	static
	{
		FunctionRegister.addFunction(beta.class);
		FunctionRegister.addFunction(uniformInt.class);
		FunctionRegister.addFunction(normal.class);
		FunctionRegister.addFunction(movingAverage.class);
		//FunctionRegister.addFunction(mathematicaStrategy.class);
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

	private PrintWriter fmarketCSV;		/// Market log file CSV
	private PrintWriter ftradersCSV;	/// Traders log file CSV
	private PrintWriter orderBook;		/// Order book log file

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
			header = "Step,Price,ManagerIncome,NbOp,NbSh\n";
			fmarketCSV = new PrintWriter(new FileWriter("./logs/" + dateFolder + "/market.csv"));
			fmarketCSV.print(header);

			header = "TraderName,InitIncome,InitShares,OpProb,SellProb,Income,Shares,"
					+ "Wealth,NbSellSuccess,NbSellFailures,NbBuySuccess,NbBuyFailures\n";
			ftradersCSV = new PrintWriter(new FileWriter("./logs/" + dateFolder + "/traders.csv"));
			ftradersCSV.print(header);
			
			orderBook = new PrintWriter(new FileWriter("./logs/" + dateFolder + "/order_book.csv"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

    @Override
	public void init(String[] args)
    {
    	initializeTimeStepped();
    	super.init(new String[] { "standalone" } );

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
		timeoutThread.interrupt();

		// Close log files
		fmarketCSV.close();
		ftradersCSV.close();
		orderBook.close();
	}


	/**
	 * actions to execute at the beginning of an iteration before each agents step
	 *
	 * @param step	number of actual step
	 */
	protected void stepStarted(int step)
	{
		clearAllPercepts();

		// End of simulation
		if (end)
		{
			try
			{
				getEnvironmentInfraTier().getRuntimeServices().stopMAS();
			}
			catch (Exception e)
			{
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
			orderBook.println("Step: " + step);
			if (listOfBuys.size() != 0 || listOfSells.size() != 0)
			{
				for (Operation op : listOfBuys)
					orderBook.println(op.toString());
				for (Operation op : listOfSells)
					orderBook.println(op.toString());
			}
			else
			{
				orderBook.println("Nothing");
			}
			
			
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

    protected void stepFinished(int step, long time, boolean timeout)
	{
		if (timeout)
			logger.info("Timeout reached in step " + step);
	}

    @Override
	public boolean executeAction(String ag, Structure action)
	{
    	super.executeAction(ag, action);
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
    
    /** Policy used when a second action is requested and the agent still has another action pending execution */
    public enum OverActionsPolicy {
        /** Queue the second action request for future execution */
        queue,

        /** Fail the second action */
        failSecond,

        /** Ignore the second action, it is considered as successfully executed */
        ignoreSecond
    };

    private int step = 0;   // step counter
    private int nbAgs = -1; // number of agents acting on the environment
    private Map<String,ActRequest> requests; // actions to be executed
    private Queue<ActRequest> overRequests; // second action tentative in the step
    private TimeOutThread timeoutThread = null;
    private long stepTimeout = 0;
    private int  sleep = 0; // pause time between cycles


    private OverActionsPolicy overActPol = OverActionsPolicy.failSecond;

    /**
     * Resets step counter and scheduled action requests to neutral state, optionally sets a timeout for waiting
     * on agent actions in a step.
     * 
     * @param args either empty, or contains timeout in milliseconds at pos 0
     */
    
    public void initializeTimeStepped()
    {
        stepTimeout = 6000;

        // reset everything
        requests = new HashMap<String,ActRequest>();
        overRequests = new LinkedList<ActRequest>();
        step = 0;
        if (timeoutThread == null) {
            if (stepTimeout > 0) {
                timeoutThread = new TimeOutThread(stepTimeout);
                timeoutThread.start();
            }
        } else {
            timeoutThread.allAgFinished();
        }
        stepStarted(step);
    }

    /** defines the time for a pause between cycles */
    public void setSleep(int s) {
        sleep = s;
    }

    public void setTimeout(int to) {
        stepTimeout = to;
        if (timeoutThread != null)
            timeoutThread.timeout = to;
    }

    /**
     *  Updates the number of agents using the environment, this default
     *  implementation, considers all agents in the MAS as actors in the
     *  environment.
     */
    protected void updateNumberOfAgents() {
        setNbAgs(getEnvironmentInfraTier().getRuntimeServices().getAgentsNames().size());
    }

    /** Returns the number of agents in the MAS (used to test the end of a cycle) */
    public int getNbAgs() {
        return nbAgs;
    }

    /** Set the number of agents */
    public void setNbAgs(int n) {
        nbAgs = n;
    }

    /** returns the current step counter */
    public int getStep() {
        return step;
    }

    /**
     * Sets the policy used for the second ask for an action while another action is not finished yet.
     * If set as queue, the second action is added in a queue for future execution
     * If set as failSecond, the second action fails.
     */
    public void setOverActionsPolicy(OverActionsPolicy p) {
        overActPol = p;
    }

    @Override
    public void scheduleAction(String agName, Structure action, Object infraData) {
        if (!isRunning()) return;
        super.scheduleAction(agName, action, infraData);

        //System.out.println("scheduling "+action+" for "+agName);
        ActRequest newRequest = new ActRequest(agName, action, requiredStepsForAction(agName, action), infraData);

        boolean startNew = false;

        synchronized (requests) { // lock access to requests
            if (nbAgs < 0) { // || timeoutThread == null) {
                // initialise dynamic information
                // (must be in sync part, so that more agents do not start the timeout thread)
                updateNumberOfAgents();
                /*if (stepTimeout > 0 && timeoutThread == null) {
                    timeoutThread = new TimeOutThread(stepTimeout);
                    timeoutThread.start();
                }*/
            }

            // if the agent already has an action scheduled, fail the first
            ActRequest inSchedule = requests.get(agName);
            if (inSchedule != null) {
                logger.fine("Agent " + agName + " scheduled the additional action '" + action.toString() + "' in an "
                        + "occupied time step. Policy: " + overActPol.name());
                if (overActPol == OverActionsPolicy.queue) {
                    overRequests.offer(newRequest);
                } else if (overActPol == OverActionsPolicy.failSecond) {
                    getEnvironmentInfraTier().actionExecuted(agName, action, false, infraData);
                } else if (overActPol == OverActionsPolicy.ignoreSecond) {
                    getEnvironmentInfraTier().actionExecuted(agName, action, true, infraData);
                }
            } else {
                // store the action request
                requests.put(agName, newRequest);

                // test if all agents have sent their actions
                if (testEndCycle(requests.keySet())) {
                    startNew = true;
                }
            }

            if (startNew) {
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {}
                }
            }
        }

        if (startNew) {
            if (timeoutThread != null)
                timeoutThread.allAgFinished();
            else
                startNewStep();
        }
    }

    public Structure getActionInSchedule(String agName) {
        ActRequest inSchedule = requests.get(agName);
        if (inSchedule != null) {
            return inSchedule.action;
        }
        return null;
    }

    /**
     * Returns true when a new cycle can start, it normally
     * holds when all agents are in the finishedAgs set.
     *
     * @param finishedAgs the set of agents' name that already finished the current cycle
     */
    protected boolean testEndCycle(Set<String> finishedAgs) {
        return finishedAgs.size() >= getNbAgs();
    }

    private void startNewStep() {
        if (!isRunning()) return;

        synchronized (requests) {
            step++;

            //logger.info("#"+requests.size());
            //logger.info("#"+overRequests.size());

            try {

                // execute all scheduled actions
                for (ActRequest a: requests.values()) {
                    a.remainSteps--;
                    if (a.remainSteps == 0) {
                        // calls the user implementation of the action
                        a.success = executeAction(a.agName, a.action);
                    }
                }

                // notify the agents about the result of the execution
                Iterator<ActRequest> i = requests.values().iterator();
                while (i.hasNext()) {
                    ActRequest a = i.next();
                    if (a.remainSteps == 0) {
                        getEnvironmentInfraTier().actionExecuted(a.agName, a.action, a.success, a.infraData);
                        i.remove();
                    }
                }

                // clear all requests
                //requests.clear();

                // add actions waiting in over requests into the requests
                Iterator<ActRequest> io = overRequests.iterator();
                while (io.hasNext()) {
                    ActRequest a = io.next();
                    if (requests.get(a.agName) == null) {
                        requests.put(a.agName, a);
                        io.remove();
                    }
                }

                // the over requests could complete the requests
                // so test end of step again
                if (nbAgs > 0 && testEndCycle(requests.keySet())) {
                    startNewStep();
                }

                stepStarted(step);
            } catch (Exception ie) {
                if (isRunning() && !(ie instanceof InterruptedException)) {
                    logger.log(Level.WARNING, "act error!",ie);
                }
            }
        }
    }

    protected int requiredStepsForAction(String agName, Structure action) {
        return 1;
    }

    /** stops perception while executing the step's actions */
    @Override
    public Collection<Literal> getPercepts(String agName) {
        synchronized (requests) {
            return super.getPercepts(agName);
        }
    }

    class ActRequest {
        String agName;
        Structure action;
        Object infraData;
        boolean success;
        int remainSteps; // the number os steps this action have to wait to be executed
        public ActRequest(String ag, Structure act, int rs, Object data) {
            agName = ag;
            action = act;
            infraData = data;
            remainSteps = rs;
        }
        public boolean equals(Object obj) {
            return agName.equals(obj);
        }
        public int hashCode() {
            return agName.hashCode();
        }
        public String toString() {
            return "["+agName+","+action+"]";
        }
    }

    class TimeOutThread extends Thread {
        Lock lock = new ReentrantLock();
        Condition agActCond = lock.newCondition();
        long timeout = 0;
        boolean allFinished = false;

        public TimeOutThread(long to) {
            super("EnvironmentTimeOutThread");
            timeout = to;
        }

        public void allAgFinished() {
            lock.lock();
            allFinished = true;
            agActCond.signal();
            lock.unlock();
        }

        public void run() {
            try {
                while (true) {
                    lock.lock();
                    long lastStepStart = System.currentTimeMillis();
                    boolean byTimeOut = false;
                    if (!allFinished) {
                        byTimeOut = !agActCond.await(timeout, TimeUnit.MILLISECONDS);
                    }
                    allFinished = false;
                    long now  = System.currentTimeMillis();
                    long time = (now-lastStepStart);
                    stepFinished(step, time, byTimeOut);
                    lock.unlock();
                    startNewStep();
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in timeout thread!",e);
            }
        }
    }
}
