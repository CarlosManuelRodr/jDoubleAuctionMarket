package internalAction;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;


public class MovingAverage extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] terms) throws Exception
    {
		return terms;

    }
	
	public static void main(String[] args)
	{
		System.out.println("Jajaja");
	}
}
