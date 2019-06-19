/* Include generic trader behavior */

{ include("./generic_trader.asl") }

/* Initial beliefs for a Zero Intelligence (ZI) Trader */

//operation_beta(OpA,OpB)      Parameters of the Beta distribution for probability of operation
//sell_beta(Sell,SellB)        Parameters of the Beta distribution for probability of buy = 1-P(sell)
//cash_beta(CashA,CashB)       Parameters of the Beta distribution for cash
//min_cash(MinCash)            Minimum cash
//max_cash(MaxCash)            Maximum cash
//shares_beta(SharesA,SharesB) Parameters of the Beta distribution for shares
//min_shares(MinShares)        Minimum shares
//max_shares(MaxShares)        Maximum shares
//variability_beta(VarA,VarB)  Parameters of the Beta distribution for variability of shares price
//min_variability(MinVar)      Minimum variability
//max_variability(MaxVar)      Maximum variability

/* Plans */

// Set-up plan

+!init :
	operation_beta(OpA,OpB) & sell_beta(SellA,SellB) & variability_beta(VarA,VarB) & min_variability(MinVar) & max_variability(MaxVar)
  <- +probability(operation, tools.beta(OpA,OpB));
     +probability(sell, tools.beta(SellA,SellB));
     +variability(math.floor(tools.beta(VarA,VarB) * (MaxVar-MinVar)) + MinVar);
     !generic_init.

// Sell plans
+!sell :
	shares(Shares) & Shares > 0 & variability(Variability) & (market_price(MarketPrice) | (init_cash(InitCash) & init_shares(InitShares) & MarketPrice = InitShares/InitCash) )
   <- Quantity = tools.uniform_int(1,Shares);
      Price = tools.normal(MarketPrice, MarketPrice * Variability);
      !sell(Quantity,Price).

+!sell : shares(0)
   <- //.println("Not enought shares to sell");
      none.

// Buy plans
+!buy :
	cash(Cash) & Cash > 0 & variability(Variability) &
	(market_price(MarketPrice) | (init_cash(InitCash) &
		init_shares(InitShares) & MarketPrice = InitShares/InitCash) )
 	<- TotalPrice = tools.uniform_int(0,Cash);
     Price = tools.normal(MarketPrice, MarketPrice * Variability);
	   Quantity = math.floor(TotalPrice/Price);
     !buy(Quantity,Price).

+!buy : cash(0)
   <- //.println("Not enought cash to buy");
      none.
