package listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import engine.Engine;

/**
 * Defines behavior for when the directory match checkbox is clicked.
 * 
 * @author Nicholas Harrell
 */
public class DirectoryMatchCheckboxListener implements ActionListener
{
	/** The individual checkbox that called the event. **/
	private final JCheckBox CALLER;
	
	/** The central program instance. **/
	private final Engine ENGINE;
	
	/**
	 * Creates a new checkbox listener with the specified mode.
	 * 
	 * @param caller			Maps to {@link #CALLER}.
	 * @param engine			Maps to {@link #ENGINE}.
	 */
	public DirectoryMatchCheckboxListener(JCheckBox caller, Engine engine)
	{
		CALLER = caller;
		ENGINE = engine;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		ENGINE.updateDirectoryMatch(CALLER.isSelected());
	}
}
