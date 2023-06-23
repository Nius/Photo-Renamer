package constructs;

import javax.swing.JTextField;

import listeners.NameOptionTextListener;

/**
 * A text field whose document listener can be suspended so that
 * the text field can be programatically modified.
 * 
 * @author Nicholas Harrell
 */
@SuppressWarnings("serial")
public class ListenerSuspendableTextField extends JTextField
{
	/** The listener for this text field. **/
	private NameOptionTextListener LISTENER;
	
	/**
	 * Defers to {@link JTextField#JTextField(String)}.
	 * 
	 * @param text	Pre-filled text.
	 */
	public ListenerSuspendableTextField(String text)
	{
		super(text);
	}

	/**
	 * Register a listener to this JTextField's document.
	 * 
	 * @param listener	The listener to add.
	 */
	public void registerDocumentListener(NameOptionTextListener listener)
	{
		LISTENER = listener;
		this.getDocument().addDocumentListener(listener);
	}
	
	@Override
	public void setText(String text)
	{
		super.setText(text);
		setCaretPosition(getText().length());
	}
	
	/**
	 * Set the text of the text field.
	 * 
	 * @param text		The new text field text.
	 * @param suspend	Whether to suspend the document listener while setting text.
	 */
	public void setText(String text, boolean suspend)
	{
		if(suspend && LISTENER != null)
			this.getDocument().removeDocumentListener(LISTENER);
		
		setText(text);
		
		if(suspend && LISTENER != null)
			this.getDocument().addDocumentListener(LISTENER);
	}
}
