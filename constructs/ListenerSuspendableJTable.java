package constructs;

import javax.swing.JTable;

import engine.Engine;

/**
 * This is identical to the {@link JTable} but with an extra method for changing
 * the row selection interval without firing a row selection changed event. This
 * allows programatic modification of the selection.
 * 
 * @author Nicholas Harrell
 */
@SuppressWarnings("serial")
public class ListenerSuspendableJTable extends JTable
{
	/** The listener for selection changes. **/
	public final InputsTableSelectionListener LISTENER;
	
	/**
	 * Creates a new JTable with a selection listener that can be suspended.
	 * The listener is automatically created but must be activated by calling
	 * {@link #startListening()}.
	 * 
	 * @param engine	Central program.
	 */
	public ListenerSuspendableJTable(Engine engine)
	{
		LISTENER = new InputsTableSelectionListener(engine);
	}

	/**
	 * Call {@link #setRowSelectionInterval(int, int)} but only after suspending
	 * the selection listener. Resume listening afterwards. 
	 * 
	 * @param start		Start index.
	 * @param end		End index.
	 * @param suspend	Whether to suspend.
	 */
	public synchronized void setRowSelectionInterval(int start, int end, boolean suspend)
	{
		if(suspend)
		{
			stopListening();
			setRowSelectionInterval(start, end);
			startListening();
		}
	}
	
	/**
	 * Activate the selection listener.
	 */
	public void startListening()
	{
		getSelectionModel().addListSelectionListener(LISTENER);
	}
	
	/**
	 * Stop the selection listener.
	 */
	public void stopListening()
	{
		getSelectionModel().removeListSelectionListener(LISTENER);
	}
}
