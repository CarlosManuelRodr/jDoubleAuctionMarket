package tools;
import java.util.ArrayList;
import java.util.List;

import jason.JasonException;
import jason.asSemantics.DefaultArithFunction;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.ListTerm;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;


/** 
	Implements MathematicaLink as an AgentSpeak callable ArithFunction
*/

public class mathematicaStrategy extends DefaultArithFunction
{
	private static final long serialVersionUID = 1L;
	
	public mathematicaStrategy() throws Exception
	{
		MathematicaLink.StartLink();
	}
	
	@Override
    public String getName()
	{
        return "tools.mathematica_strategy";
    }
	
    @Override
    public double evaluate(TransitionSystem ts, Term[] args) throws Exception
    {	
		if (args[0].isNumeric() && args[1].isNumeric() && args[2].isList())
		{
			int window1 = (int)((NumberTerm)args[0]).solve();
			int window2 = (int)((NumberTerm)args[1]).solve();
			List<Term> prices = ((ListTerm)args[2]).getAsList();
			ArrayList<Double> priceList = new ArrayList<Double>();
			
			for (Term priceTerm : prices)
				priceList.add( ((NumberTerm)priceTerm).solve() );
			
			// Return 1 if the agent should buy, 0 if it should sell.
			if (MathematicaLink.ExecuteStrategy(window1, window2, priceList))
				return 1.0;
			else
				return 0.0;
        }
		else
        {
            throw new JasonException("Bad arguments");
        }
    }

    @Override
    public boolean checkArity(int a)
    {
        return a == 3;
    }
}