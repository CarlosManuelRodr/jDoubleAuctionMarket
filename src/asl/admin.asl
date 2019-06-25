/* Creencias (parámetros fijos) */

operation_beta(1,1).
sell_beta(1,1).
variability_beta(1,1).
min_variability(0.1).
max_variability(0.1).

!start.

/* Planes */

+!start : true <- 
	makeArtifact("gui","tools.ArtefactoGUI",[],Id);
	focus(Id).

+iniciar: zi(N1) & ma(N2) & useWl(0) & operation_beta(OpA,OpB) 
          & sell_beta(SellA,SellB) & variability_beta(VarA,VarB)
          & min_variability(MinVar) & max_variability(MaxVar) <-
	println("Starting simulation");
	for ( .range(X,1,N1) )
	{
		/* Crea agente ZI */
		.concat(zi_trader,X,Nombre);
		.create_agent(Nombre, "zi_trader.asl", [agentArchClass("jason.architecture.AgArch")]);
		
		/* Asigna parámetros a agente ZI */
		OpProb =  tools.beta(OpA,OpB);
		SellProb =  tools.beta(SellA,SellB);
		Var = math.floor(tools.beta(VarA,VarB) * (MaxVar-MinVar)) + MinVar;
		.send(Nombre, tell, probability(operation, OpProb));
		.send(Nombre, tell, probability(sell, SellProb));
		.send(Nombre, tell, variability(Var))
	};
	for ( .range(X,1,N2) )
	{
		/* Crea agente MA */
		.concat(ma_trader,X,Nombre);
		.create_agent(Nombre, "ma_trader.asl", [agentArchClass("jason.architecture.AgArch")]);
		
		/* Asigna parámetros a agente MA */
		OpProb =  tools.beta(OpA,OpB);
		SellProb =  tools.beta(SellA,SellB);
		Var = math.floor(tools.beta(VarA,VarB) * (MaxVar-MinVar)) + MinVar;
		MaWindow1 = tools.uniform_int(10, 100);
		MaWindow2 = math.floor(MaWindow1 / 2);
		.send(Nombre, tell, probability(operation, OpProb));
		.send(Nombre, tell, probability(sell, SellProb));
		.send(Nombre, tell, variability(Var));
		.send(Nombre, tell, ma_window1(MaWindow1));
		.send(Nombre, tell, ma_window2(MaWindow2))
	};
	.kill_agent("admin").
	
+iniciar: zi(N1) & ma(N2) & useWl(1) <-
	println("Starting simulation");
	for ( .range(X,1,N1) )
	{
		/* Crea agente ZI */
		.concat(zi_trader,X,Nombre);
		.create_agent(Nombre, "zi_trader.asl", [agentArchClass("jason.architecture.AgArch")]);
		
		/* Asigna parámetros a agente ZI */
		OpProb =  tools.beta(OpA,OpB);
		SellProb =  tools.beta(SellA,SellB);
		Var = math.floor(tools.beta(VarA,VarB) * (MaxVar-MinVar)) + MinVar;
		.send(Nombre, tell, probability(operation, OpProb));
		.send(Nombre, tell, probability(sell, SellProb));
		.send(Nombre, tell, variability(Var))
	};
	
	/* Crea agente WL y asigna parámetros */
	MaWindow1 = tools.uniform_int(10, 100);
	MaWindow2 = math.floor(MaWindow1 / 2);
	.create_agent(wl_trader, "wl_trader.asl", [agentArchClass("jason.architecture.AgArch")]);
	.send(Nombre, tell, ma_window1(MaWindow1));
	.send(Nombre, tell, ma_window2(MaWindow2));
	
	.kill_agent("admin").