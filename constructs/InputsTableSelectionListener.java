package constructs;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import engine.Engine;

/**
 * Defines behavior for changing the selected row in the inputs table.
 * Note that this is not a mouse event listener.
 * 
 * @author Nicholas Harrell
 */
public class InputsTableSelectionListener implements ListSelectionListener
{
	/** Central program instance. **/
	public final Engine ENGINE;
	
	/**
	 * Create a new inputs file table selection listener.
	 * 
	 * @param engine	Maps to {@link #ENGINE}.
	 */
	public InputsTableSelectionListener(Engine engine)
	{
		ENGINE = engine;
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if(!e.getValueIsAdjusting() && ENGINE.getSelectedInputFileIndex() >= 0)
			ENGINE.setSelectedInputFile(ENGINE.getSelectedInputFileIndex(),true);
	}
}
