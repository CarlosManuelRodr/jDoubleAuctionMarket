(* ::Package:: *)

Strategy[window1_, window2_, priceList_] := Block[{mAvg1, mAvg2},
	If[Length[priceList] < window1 || Length[priceList] < window2,
		Return[True]
	];

	mAvg1 = Mean[Take[priceList, -window1]];
	mAvg2 = Mean[Take[priceList, -window2]];
	
	If[mAvg1 < mAvg2,
		Return[True]
		,
		Return[False]
	];
];
