package listeners;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import constructs.ConfigurationManager;
import engine.Engine;

/**
 * Defines behavior for when a checkbox is clicked.
 * 
 * @author Nicholas Harrell
 */
public class NameOptionTextListener implements DocumentListener
{
	/** How the listener should behave when the checkbox is clicked. **/
	public final TLMode MODE;

	/** The individual checkbox that called the event. **/
	public final JTextField CALLER;
	
	/** The program instance. **/
	public final Engine ENGINE;
	
	/** The delay mechanism for waiting a full second after update before committing changes. **/
	private final ScheduledExecutorService CLOCK = Executors.newSingleThreadScheduledExecutor();
	
	/** The current clock task, used for cancellation. **/
	private ScheduledFuture<?> task;
	
	/**
	 * Creates a new checkbox listener with the specified mode.
	 * 
	 * @param mode	Maps to {@link #MODE}.
	 * @param caller Maps to {@link #CALLER}.
	 * @param engine Maps to {@link #ENGINE}.
	 */
	public NameOptionTextListener(TLMode mode, JTextField caller, Engine engine)
	{
		MODE = mode;
		CALLER = caller;
		ENGINE = engine;
	}
	
	/**
	 * Essentially identifies the text area individually, so as to determine how to behave.
	 * 
	 * @author Nicholas Harrell
	 */
	public enum TLMode
	{
		/** Call {@link Engine#updateFilePrefix(String, boolean)}. **/
		PREFIX,
		
		/** Call {@link Engine#updateFileSuffix(String, boolean)}. **/
		SUFFIX,
		
		/** Call {@link Engine#updateUndescribed(String, boolean)}. **/
		UNNAMED,
		
		/** Call {@link Engine#updateMaxLength(int)}. **/
		LENGTH
	}
	
	/** Start or reset the delayed update clock. **/
	private void setClock()
	{
		if(task != null)
			task.cancel(false);
		task = CLOCK.schedule(this::update,1,TimeUnit.SECONDS);
	}
	
	/** Commit changes made to the textbox. **/
	public void update()
	{
		switch(MODE)
		{
			case PREFIX: ENGINE.updateFilePrefix(CALLER.getText(),true); break;
			case SUFFIX: ENGINE.updateFileSuffix(CALLER.getText(),true); break;
			case UNNAMED: ENGINE.updateUndescribed(CALLER.getText(),true); break;
			case LENGTH:
				try
				{
					/*
					 * It is possible for the textbox to have an invalid value
					 * because the formatting mechanism only refuses and reverts
					 * the value after the box loses focus, which could be after
					 * the clock expires and calls this method.
					 */
					
					int min = Integer.valueOf(CALLER.getText());
					if(min >= ConfigurationManager.MINIMUM_PATH)
						ENGINE.updateMaxLength(min);
				}
				catch (NumberFormatException e) { /* Do nothing. */ }
				break;
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		setClock();
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		setClock();
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
		setClock();
	}
}