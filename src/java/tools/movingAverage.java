package tools;

import jason.JasonException;
import jason.asSemantics.DefaultArithFunction;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;
import java.util.List;

public class movingAverage extends DefaultArithFunction
{
	
	private static final long serialVersionUID = 1L;

	@Override
    public String getName()
	{
        return "tools.moving_average";
    }
    
    @Override
    public double evaluate(TransitionSystem ts, Term[] args) throws Exception
    {		
		if (args[0].isNumeric() && args[1].isList())
		{
			int window = (int)((NumberTerm)args[0]).solve();
			List<Term> prices = ((ListTerm)args[1]).getAsList();
			
			if (prices.size() - window < 0)
				return 0.0;
			
			double average = 0.0;
			
			for (int i = prices.size() - window; i < prices.size(); i++)
			{
				average += ((NumberTerm)prices.get(i)).solve();
			}
			
			return average / prices.size();
        }
		else
        {
            throw new JasonException("Bad arguments");
        }
    }

    @Override
    public boolean checkArity(int a)
    {
        return a == 2;
    }
}
