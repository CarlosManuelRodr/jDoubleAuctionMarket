/* Include generic trader behavior */

{ include("./generic_trader.asl") 
	
}
/* Initial beliefs and rules */
operation_beta(1,1).
sell_beta(1,1).
cash_beta(1,1).
min_cash(10).
max_cash(10).
shares_beta(1,1).
min_shares(10).
max_shares(10).
variability_beta(1,1).
min_variability(0.1).
max_variability(0.1).

/* Plans */

+!init :
	operation_beta(OpA,OpB) & sell_beta(SellA,SellB) & variability_beta(VarA,VarB) & min_variability(MinVar) & max_variability(MaxVar)
  <- +probability(operation, tools.beta(OpA,OpB));
     +probability(sell, tools.beta(SellA,SellB));
     +variability(math.floor(tools.beta(VarA,VarB) * (MaxVar-MinVar)) + MinVar);
     !generic_init.

+!sell :
	shares(Shares) & Shares > 0 & variability(Variability) & (market_price(MarketPrice) | (init_cash(InitCash) & init_shares(InitShares) & MarketPrice = InitShares/InitCash) )
   <- none.

+!sell : shares(0)
   <- .println("Not enought shares to sell");
      none.

// Buy plans
+!buy :
	cash(Cash) & Cash > 0 & variability(Variability) &
	(market_price(MarketPrice) | (init_cash(InitCash) &
		init_shares(InitShares) & MarketPrice = InitShares/InitCash) )
 	<- TotalPrice = tools.uniform_int(0,Cash);
     none.

+!buy : cash(0)
   <- .println("Not enought cash to buy");
      none.