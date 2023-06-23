package listeners;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import engine.Engine;

/**
 * Defines behavior for the "Help" and "About" buttons.
 * 
 * @author Nicholas Harrell
 */
public class HelpButtonListener implements ActionListener
{
	/** How this button should behave when clicked. **/
	private final HelpMode MODE;
	
	/** The program instance for generating output. **/
	private final Engine ENGINE;
	
	/**
	 * Creates a new help button listener.
	 * 
	 * @param engine Maps to {@link #ENGINE}.
	 * @param mode Maps to {@link #MODE}.
	 */
	public HelpButtonListener(Engine engine, HelpMode mode)
	{
		ENGINE = engine;
		MODE = mode;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String cwd = System.getProperty("user.dir");
		
		switch(MODE)
		{
			case ABOUT:
				try
				{
					Path aboutPath = Paths.get(cwd + "/about.pdf");
					Desktop.getDesktop().open(aboutPath.toFile());
				}
				catch (Exception e1)
				{
					ENGINE.addOutput("Could not open about document.");
				}
				break;
			case HELP:
				try
				{
					Path aboutPath = Paths.get(cwd + "/help.pdf");
					Desktop.getDesktop().open(aboutPath.toFile());
				}
				catch (Exception e1)
				{
					ENGINE.addOutput("Could not open help document.");
				}
				break;
		}
	}

	/**
	 * Essentially identifies the button individually, so as to determine how to behave.
	 * 
	 * @author Nicholas Harrell
	 */
	public enum HelpMode
	{
		/** Help button}. **/
		HELP,
		
		/** About button}. **/
		ABOUT,
	}
}
