
package tools;

import jason.JasonException;
import jason.asSemantics.DefaultArithFunction;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;

import flanagan.math.PsRandom;

/** 
<p>Function: <b><code>tools.normal(Mean,Std)</code></b>: 
Returns a normal [Gaussian] random deviate from a normal [Gaussian] distribution of mean, Mean, and standard deviation Std.

<p>Examples:<ul>
<li> <code>tools.normal(1,2)</code>: returns 1.</li>
</ul>
 
@author Francisco Grimaldo 

@see jason.functions.Random

*/
public class normal extends DefaultArithFunction  {

	private static final long serialVersionUID = 1L;
	private static PsRandom ran = null; 
	
    static {
		ran = new PsRandom();
    }

	@Override
    public String getName() {
        return "tools.normal";
    }
    
    @Override
    public double evaluate(TransitionSystem ts, Term[] args) throws Exception {		
		if (args[0].isNumeric()) {
			if (args[1].isNumeric()) {
				return ran.nextGaussian(((NumberTerm)args[0]).solve(), ((NumberTerm)args[1]).solve());
			} else {
				throw new JasonException("The RangeLow argument '"+args[1]+"' is not numeric!");
			}
        } else {
            throw new JasonException("The RangeHigh argument '"+args[0]+"' is not numeric!");
        }
    }

    @Override
    public boolean checkArity(int a) {
        return a == 2;
    }
}
