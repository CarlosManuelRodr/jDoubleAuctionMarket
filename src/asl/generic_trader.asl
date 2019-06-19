/* Initial beliefs for a Generic Trader*/

operation_id(0).               // Operation id sequence
nb_success(sell,0).            // Number of successful sell operations
nb_success(buy,0).             // Number of successful buy operations
nb_failures(sell,0).           // Number of fail sell operations
nb_failures(buy,0).            // Number of fail buy operations

// A generic trader is broken if he does not have shares
// and he has less cash than the market price for one action.

broken :- shares(0) & cash(C) & market_price(P) & C <= P.

/* Plans */

// Init step
+step(0) <-
	!init.

// Generic set-up plan
+!generic_init :
	cash_beta(CashA,CashB) & min_cash(MinCash) & max_cash(MaxCash) & shares_beta(SharesA,SharesB) & min_shares(MinShares) & max_shares(MaxShares)
	<-
	BetaCash = tools.beta(CashA,CashB);
	InitCash = math.floor(BetaCash * (MaxCash-MinCash)) + MinCash;
  +cash(InitCash);
  +init_cash(InitCash);
  BetaShares = tools.beta(SharesA,SharesB);
	InitShares = math.floor(BetaShares * (MaxShares-MinShares)) + MinShares;
  +shares(InitShares);
  +init_shares(InitShares);
	start.

// Generic end plan
+end :
	.my_name(MyName) & init_cash(InitCash) & init_shares(InitShares) & cash(Cash) & shares(Shares) & probability(operation,OpProb) & probability(sell,SellProb) & nb_success(sell,NbSellSucc) & nb_success(buy,NbBuySucc) & nb_failures(sell,NbSellFail) & nb_failures(buy,NbBuyFail)
  <-
	end(MyName,InitCash,InitShares,OpProb,SellProb,Cash,Shares,NbSellSucc,NbSellFail,NbBuySucc,NbBuyFail).

// Market step
+step(Step) :
	not broken & probability(operation,OpProb) & probability(sell,SellProb)
	<-
	!update_statistics;
	if (broken) {
		broken;
		} else {
				// Launch a new operation
				if (math.random < OpProb) {
					if (math.random < SellProb) {
						!sell;
						} else {
							!buy;
					}
				} else {
					none;
			}
		}.

+step(Step) : broken <-
	.println("::::::none-broken!");
	?shares(Shares);
	none(Shares).

// Update statistics
+!update_statistics : last_operation(OpId,OpType,Quantity,Price)
	<- // Process successful operation
	for (operation_success(OpId,_,RealQuantity,RealPrice)) {
		?shares(Shares);
		?cash(Cash);
		if (OpType == sell) {
			NewShares = Shares - RealQuantity;
			NewCash = Cash + RealPrice * RealQuantity;
		} else { //OpType == buy
			NewShares = Shares + RealQuantity;
			NewCash = Cash - RealPrice * RealQuantity;
	}
	-+shares(NewShares);
	-+cash(NewCash);
	?nb_success(OpType,NbSuccess);
	.abolish(nb_success(OpType,_));
	+nb_success(OpType,NbSuccess+1);
	}
	// Process fail operation
	  if (not operation_success(_,_,_,_)) {
			?nb_failures(OpType,NbFailures);
			.abolish(nb_failures(OpType,_));
			+nb_failures(OpType,NbFailures+1);
		}
		// Check broken
		if (broken) {
			+broken;
		}
		.abolish(last_operation(_,_,_,_)).

+!update_statistics : not last_operation(_,_,_,_).

// Sell plan
@sell[atomic]
+!sell(Quantity,Price) :
	Quantity>0 & Price>0 & operation_id(OpId) & step(CurrentStep)
	<-
	-+operation_id(OpId+1);
	?shares(Shares);
	sell(OpId,Quantity,Price,Shares);
	+last_operation(OpId,sell,Quantity,Price).

+!sell(Quantity,Price) : Quantity <=0 | Price<=0
	<- //.println("Skip order sell: Quantity=", Quantity, " Price=", Price);
	none.

// Buy plan
@buy[atomic]
+!buy(Quantity,Price) :
	Quantity>0 & Price>0 & operation_id(OpId) & step(CurrentStep)
	<-
	-+operation_id(OpId+1);
	?shares(Shares);
	buy(OpId,Quantity,Price,Shares);
	+last_operation(OpId,buy,Quantity,Price).

+!buy(Quantity,Price) : Quantity <=0 | Price<=0
	<- //.println("Skip order buy: Quantity=", Quantity, " Price=", Price);
	none.
