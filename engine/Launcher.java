package engine;

/**
 * The main entry point for the PhotoRenamer program.
 * 
 * The purpose of this program is to drastically shorten the amount of time
 * Duraclean employees spend renaming photos downloaded from Dash.
 * 
 * @author	Nicholas Harrell
 * @date	2022-02-25
 */
public class Launcher
{
	/**
	 * This is the main entry point for execution from the OS.
	 * 
	 * @param args	System arguments.
	 */
	public static void main(String[] args)
	{
		Engine engine = new Engine();
		engine.run();
	}
}
