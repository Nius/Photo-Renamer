package listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButton;

import constructs.ConfigurationManager.OverlengthBehavior;
import constructs.ConfigurationManager.ReplacementCharacter;
import engine.Engine;

/**
 * Defines behavior for when a radio button is clicked.
 * 
 * @author Nicholas Harrell
 */
public class NameOptionRadioListener implements ActionListener
{
	/** How the listener should behave when the checkbox is clicked. **/
	public final RLMode MODE;

	/** The individual checkbox that called the event. **/
	public final JRadioButton CALLER;
	
	/** The program instance. **/
	public final Engine ENGINE;
	
	/**
	 * Creates a new checkbox listener with the specified mode.
	 * 
	 * @param mode	Maps to {@link #MODE}.
	 * @param caller Maps to {@link #CALLER}.
	 * @param engine Maps to {@link #ENGINE}.
	 */
	public NameOptionRadioListener(RLMode mode, JRadioButton caller, Engine engine)
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
			case REFUSE:
				if(CALLER.isSelected())
					ENGINE.updateOverLengthBehavior(OverlengthBehavior.REFUSE);
				break;
			case WARN:
				if(CALLER.isSelected())
					ENGINE.updateOverLengthBehavior(OverlengthBehavior.WARN);
				break;
			case TRUNCATE:
				if(CALLER.isSelected())
					ENGINE.updateOverLengthBehavior(OverlengthBehavior.TRUNCATE);
				break;
			case DROP_VOWELS:
				if(CALLER.isSelected())
					ENGINE.updateOverLengthBehavior(OverlengthBehavior.DROP_VOWELS);
				break;
			case ALLOW:
				if(CALLER.isSelected())
					ENGINE.updateOverLengthBehavior(OverlengthBehavior.DO_NOTHING);
				break;
				
			case HYPHEN:
				if(CALLER.isSelected())
					ENGINE.updateReplacementChar(ReplacementCharacter.HYPHEN);
				break;
			case COMMA:
				if(CALLER.isSelected())
					ENGINE.updateReplacementChar(ReplacementCharacter.COMMA);
				break;
			case NOTHING:
				if(CALLER.isSelected())
					ENGINE.updateReplacementChar(ReplacementCharacter.NOTHING);
				break;
		}
	}

	/**
	 * Essentially identifies the radio button individually, so as to determine how to behave.
	 * 
	 * @author Nicholas Harrell
	 */
	public enum RLMode
	{
		/** Call {@link Engine#updateOverLengthBehavior(OverlengthBehavior)}. **/
		REFUSE,
		
		/** Call {@link Engine#updateOverLengthBehavior(OverlengthBehavior)}. **/
		WARN,
		
		/** Call {@link Engine#updateOverLengthBehavior(OverlengthBehavior)}. **/
		TRUNCATE,
		
		/** Call {@link Engine#updateOverLengthBehavior(OverlengthBehavior)}. **/
		DROP_VOWELS,
		
		/** Call {@link Engine#updateOverLengthBehavior(OverlengthBehavior)}. **/
		ALLOW,
		
		/** Call {@link Engine#updateReplacementChar(ReplacementCharacter)}. **/
		HYPHEN,
		
		/** Call {@link Engine#updateReplacementChar(ReplacementCharacter)}. **/
		COMMA,
		
		/** Call {@link Engine#updateReplacementChar(ReplacementCharacter)}. **/
		NOTHING,
	}
}
