package tools;

import cartago.*;

/**
 * This class defines an artifact type Timer to measure time lapses in milliseconds, 
 * even when it is based on the Java method <code>System.nanoTime()</code>.
 * 
 * <p><b>Operations:</b></p>
 * 
 * <ul>
 * <li> <code>startTime()</code> : Starts the timer.
 * <li> <code>endTime(T)</code> : Stops the timer, returning returning in T the time
 * elapsed since the last start.
 * </ul>
 * 
 * @author Alejandro Guerra-Hernández (aguerra@uv.mx)
 *
 */

public class Timer extends Artifact {
	private long startTime;
	private long endTime;
		
		
	@OPERATION
    void startTime(){
		 startTime= System.nanoTime();		 
	 }
	 
	@OPERATION
    void endTime(OpFeedbackParam<java.lang.Long> totalTime){
		endTime = System.nanoTime();
		totalTime.set((endTime - startTime)/1000000000); // Time measured in milliseconds (originally nano).		 
	 }
}
