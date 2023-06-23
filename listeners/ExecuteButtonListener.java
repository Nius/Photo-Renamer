package listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import engine.Engine;

/**
 * Defines behavior for the execute button.
 * 
 * @author Nicholas Harrell
 */
public class ExecuteButtonListener implements ActionListener
{
	/** The hosting program instance. **/
	private final Engine ENGINE;
	
	/**
	 * Creates a new listener for the execute button.
	 * 
	 * @param engine	Maps to {@link #ENGINE}.
	 */
	public ExecuteButtonListener(Engine engine)
	{
		ENGINE = engine;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		ENGINE.executeDownload();
	}
}
