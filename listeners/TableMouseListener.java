package listeners;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import javax.swing.JTable;

import constructs.ConfigurationManager.Config;
import constructs.PhotoCollection;
import engine.Engine;

/**
 * Defines behavior for table cell click events.
 * 
 * @author Nicholas Harrell
 */
public class TableMouseListener extends MouseAdapter implements MouseMotionListener
{
	/** The table this mouse listener monitors. **/
	private final JTable TABLE;
	
	/** The program instance to which to provide output if necessary. **/
	private final Engine ENGINE;
	
	/**
	 * Creates a new mouse listener for the specified table.
	 *
	 * @param engine Maps to {@link #ENGINE}.
	 * @param table	Maps to {@link #TABLE}.
	 */
	public TableMouseListener(Engine engine, JTable table)
	{
		TABLE = table;
		ENGINE = engine;
	}
	
	@Override
	public void mouseClicked(MouseEvent event)
	{
		if(!Desktop.isDesktopSupported())
			return;
		
		int row = TABLE.rowAtPoint(event.getPoint());
		int col = TABLE.columnAtPoint(event.getPoint());

		// Protect against mouse events on an empty table.
		if(row < 0 || col < 0)
			return;
		
		if(col == 1)
		{
			try
			{
				Config CONFIG = ENGINE.getConfiguration();
				
				URL photoURL = ((PhotoCollection)(TABLE.getModel())).getLinkAddress(row);
				
				// Create the temp directory if it does not exist.
				Files.createDirectories(CONFIG.TEMP_FILE_DIR);
				
				// Save the photo to the temporary directory.
				String tempFileName = ZonedDateTime.now().toInstant().toEpochMilli() + "_tempPhoto.jpg";
				Path tempFilePath = Paths.get(CONFIG.TEMP_FILE_DIR.toString() + "/" + tempFileName);
				ReadableByteChannel in = Channels.newChannel(photoURL.openStream());
				FileOutputStream out = new FileOutputStream(tempFilePath.toString());
				out.getChannel().transferFrom(in,0,Long.MAX_VALUE);
				out.close();
				
				// Open the temporary photo.
				Desktop.getDesktop().open(tempFilePath.toFile());
			}
			catch (Exception e)
			{
				ENGINE.addOutput("Could not open photo.");
			}
		}
		else if(col == 2)
		{
			ENGINE.getActivePhotoModel().unCustomizePhoto(row);
			ENGINE.redrawPhotosTable(row);
			ENGINE.INPUTS_MODEL.reprocessAllFiles();
		}
	}
	
	@Override
	public void mouseMoved(MouseEvent event)
	{
		int col = TABLE.columnAtPoint(event.getPoint());
		int row = TABLE.rowAtPoint(event.getPoint());
		
		// Protect against mouse events on an empty table.
		if(row < 0 || col < 0)
			return;
		
		if(col == 1 || (col == 2 && ENGINE.getActivePhotoModel().getPhotoIsCustomized(row)))
			TABLE.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		else
			TABLE.setCursor(Cursor.getDefaultCursor());
	}
}
