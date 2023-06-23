package listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import engine.Engine;

/**
 * Defines behavior for when a checkbox is clicked.
 * 
 * @author Nicholas Harrell
 */
public class NameOptionCheckboxListener implements ActionListener
{
	/** How the listener should behave when the checkbox is clicked. **/
	public final CBLMode MODE;

	/** The individual checkbox that called the event. **/
	public final JCheckBox CALLER;
	
	/** The program instance. **/
	public final Engine ENGINE;
	
	/**
	 * Creates a new checkbox listener with the specified mode.
	 * 
	 * @param mode	Maps to {@link #MODE}.
	 * @param caller Maps to {@link #CALLER}.
	 * @param engine Maps to {@link #ENGINE}.
	 */
	public NameOptionCheckboxListener(CBLMode mode, JCheckBox caller, Engine engine)
	{
		MODE = mode;
		CALLER = caller;
		ENGINE = engine;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		switch(MODE)
		{
			case REMOVE_NUMS: ENGINE.updateRemoveTrailingNumbers(CALLER.isSelected()); break;
			case INDEX_UNIQUE: ENGINE.updateIndexUnique(CALLER.isSelected()); break;
			case CORRECT_CAPS: ENGINE.updateCorrectCaps(CALLER.isSelected()); break;
			case MODIFY_DATE: ENGINE.updateModifyDate(CALLER.isSelected()); break;
			case DELETE_HTML: ENGINE.updateDeleteHTML(CALLER.isSelected()); break;
		}
	}

	/**
	 * Essentially identifies the checkbox individually, so as to determine how to behave.
	 * 
	 * @author Nicholas Harrell
	 */
	public enum CBLMode
	{
		/** Call {@link Engine#updateRemoveTrailingNumbers(boolean)}. **/
		REMOVE_NUMS,
		
		/** Call {@link Engine#updateIndexUnique(boolean)}. **/
		INDEX_UNIQUE,
		
		/** Call {@link Engine#updateCorrectCaps(boolean)}. **/
		CORRECT_CAPS,
		
		/** Call {@link Engine#updateModifyDate(boolean)}. **/
		MODIFY_DATE,
		
		/** Call {@link Engine#updateDeleteHTML(boolean)}. **/
		DELETE_HTML
	}
}
