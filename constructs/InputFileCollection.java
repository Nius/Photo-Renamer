package constructs;

import java.awt.Image;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import constructs.InputFile.FileStatus;
import engine.Engine;

/**
 * Manages the collection of {@link InputFile InputFiles} and defines behavior
 * for the input files table.
 * 
 * @author Nicholas Harrell
 */
@SuppressWarnings("serial")
public class InputFileCollection extends AbstractTableModel
{
	/** Column names. **/
	private final String[] COLUMN_TITLES = {"","","Input Files"};
	
	/**
	 * The central program instance, required for the following calls:
	 * {@link #reprocessAllFiles()} calls {@link Engine#redrawInputsTable(int)}.
	 */
	private final Engine ENGINE;
	
	/** The list of input files shown in this table. **/
	private ArrayList<InputFile> INPUTS;

	/** Icon for {@link FileStatus#SAVED}. **/
	private final ImageIcon ICON_OK;

	/** Icon for {@link FileStatus#ERROR_SEVERE}. **/
	private final ImageIcon ICON_ERR_SEVERE;
	
	/** Icon for view folder. **/
	private final ImageIcon ICON_FOLDER;
	
	/**
	 * Create a collection with the necessary icons already loaded.
	 * 
	 * @param inputFiles The collection of input files to display.
	 * @param engine Maps to {@link #ENGINE}.
	 */
	public InputFileCollection(ArrayList<InputFile> inputFiles, Engine engine)
	{
		INPUTS = inputFiles;
		ENGINE = engine;
		
		// Load icons for the files table.
		ICON_OK = loadAndResizeIcon("assets/green-check.png");
		ICON_ERR_SEVERE = loadAndResizeIcon("assets/red-x.png");
		ICON_FOLDER = loadAndResizeIcon("assets/folder.png");
	}
	
	/** Set all inputs' status to {@link FileStatus#READY}. **/
	public void clearAllStatuses()
	{
		for(InputFile input : INPUTS)
			input.status = FileStatus.READY;
		fireTableDataChanged();
	}
	
	@Override
	public Class<?> getColumnClass(int col)
	{
		switch(col)
		{
			case 0:
				return ImageIcon.class;
			case 1:
				return ImageIcon.class;
			case 2:
				return String.class;
			default:
				return null;
		}
	}
	
	@Override
	public int getColumnCount()
	{
		return COLUMN_TITLES.length;
	}
	
	@Override
	public String getColumnName(int col)
	{
		return COLUMN_TITLES[col];
	}
	
	/**
	 * Get the file directory for the input file at the specified index (row).
	 * 
	 * @param index	The row from which to retrieve data.
	 * @return The file path associated with the input file on the specified row.
	 */
	public Path getInputFileDirectory(int index)
	{
		return INPUTS.get(index).FILE_PATH.getParent();
	}
	
	/**
	 * Get the file path for the input file at the specified index (row).
	 * 
	 * @param index	The row from which to retrieve data.
	 * @return The file path associated with the input file on the specified row.
	 */
	public Path getInputFilePath(int index)
	{
		return INPUTS.get(index).FILE_PATH;
	}
	
	/**
	 * Get the photo collection for the input file at the specified index (row).
	 * 
	 * @param index	The row from which to retrieve data.
	 * @return The photo collection associated with the input file on the specified row.
	 */
	public PhotoCollection getInputFilePhotoCollection(int index)
	{
		return INPUTS.get(index).getCollection();
	}
	
	@Override
	public int getRowCount()
	{
		return INPUTS.size();
	}

	/**
	 * Determine whether the input files are ready for execution.
	 * 
	 * @return True if no files have errors. False if any one does.
	 */
	public boolean isExecutable()
	{
		for(InputFile input : INPUTS)
			if(input.status == FileStatus.ERROR_SEVERE)
				return false;
		return true;
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		InputFile row = INPUTS.get(rowIndex);
		
		switch(columnIndex)
		{
			case 0:
				switch(row.status)
				{
					case READY: return null;
					case SAVED:	return ICON_OK;
					case ERROR_SEVERE: return ICON_ERR_SEVERE;
				};
			case 1:
				return ICON_FOLDER;
			case 2:
				return row.PATH_SHORTHAND;
			default:
				return null;
		}
	}
	
	@Override
	public boolean isCellEditable(int row, int col)
	{
		return false;
	}
	
	/**
	 * Load an icon from the specified path and resize it to fit in the
	 * files table.
	 * 
	 * @param path The location from which to load the icon.
	 * @return A correctly-sized icon.
	 */
	private ImageIcon loadAndResizeIcon(String path)
	{
		try
		{
			ImageIcon icon = new ImageIcon(this.getClass().getClassLoader().getResource(path));
			Image image = icon.getImage();
			Image resized = image.getScaledInstance(16,16,Image.SCALE_SMOOTH);
			return new ImageIcon(resized);
		}
		catch(NullPointerException e)
		{
			// Icons won't load when debugging from console.
			return null;
		}
	}
	
	/**
	 * Re-process the descriptions of all input files.
	 */
	public void reprocessAllFiles()
	{
		for(int i = 0; i < INPUTS.size(); i++)
		{
			INPUTS.get(i).processDescriptions();
			ENGINE.redrawInputsTable(i);
		}
	}
	
	/**
	 * Update the status of the file at the specified index (row) and immediately
	 * update the table.
	 * 
	 * @param index		The index (row) of the file to update.
	 * @param newStatus	The new status of the specified input file.
	 */
	public void updateFileStatus(int index, FileStatus newStatus)
	{
		INPUTS.get(index).status = newStatus;
		fireTableDataChanged();
	}
}
