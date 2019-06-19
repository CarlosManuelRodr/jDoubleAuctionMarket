package tools;

import jason.JasonException;
import jason.asSemantics.DefaultArithFunction;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;

import flanagan.math.PsRandom;

/** 
<p>Function: <b><code>tools.beta(A,B)</code></b>: Returns a Beta random deviate 
from a Beta distribution of shape parameters, A, [alpha] and B, [beta] and interval [0, 1].

<p>Examples:<ul>
<li> <code>tools.beta(1,2)</code>: returns 1.</li>
</ul>
 
@author Francisco Grimaldo 

@see jason.functions.Random

*/
public class beta extends DefaultArithFunction  {
	
	private static final long serialVersionUID = 1L;
	
	private static PsRandom ran = null; 
	
    static {
		ran = new PsRandom();
    }

	@Override
    public String getName() {
        return "tools.beta";
    }
    
    @Override
    public double evaluate(TransitionSystem ts, Term[] args) throws Exception {		
		if (args[0].isNumeric()) {
			if (args[1].isNumeric()) {
				return ran.nextBeta(((NumberTerm)args[0]).solve(),((NumberTerm)args[1]).solve());
			} else {
				throw new JasonException("The beta argument '"+args[1]+"' is not numeric!");
			}
        } else {
            throw new JasonException("The alpha argument '"+args[0]+"' is not numeric!");
        }
    }

    @Override
    public boolean checkArity(int a) {
        return a == 2;
    }
}
