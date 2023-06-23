package constructs;

import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import constructs.ConfigurationManager.Config;
import constructs.ConfigurationManager.OverlengthBehavior;
import constructs.Photo.PhotoStatus;
import engine.Engine;

/**
 * Manages a collection of photos and defines the behavior of the table that will display it.
 * The photo collection is managed here becuase the collection and the display thereof should
 * be inextricably linked; whenever the collection changes the display should be updated
 * accordingly.
 * 
 * @author Nicholas Harrell
 */
@SuppressWarnings("serial")
public class PhotoCollection extends AbstractTableModel
{
	/** Whether the data in this table should prevent execution of download. **/
	private boolean BLOCK_EXECUTION = false;
	
	/** Column names. **/
	private final String[] COLUMN_TITLES = {"","","","Photo Descriptions"};
	
	/** Central program instance. Necessary so that {@link #checkData()} can read configruation. **/
	private final Engine ENGINE;
	
	/**
	 * The list of photos shown in this table.
	 * Note that the properties of individual photos may change but the collection
	 * itself will never change; where the collection would change a new {@link PhotoCollection}
	 * should be created.
	 **/
	private final ArrayList<Photo> PHOTOS;

	/** Icon for {@link PhotoStatus#SAVED}. **/
	private final ImageIcon ICON_OK;

	/** Icon for {@link PhotoStatus#ERROR_SEVERE}. **/
	private final ImageIcon ICON_ERR_SEVERE;
	
	/** Icon for {@link PhotoStatus#ERROR_MINOR}. **/
	private final ImageIcon ICON_ERR_MINOR;

	/** Icon for photo viewing links. **/
	private final ImageIcon ICON_VIEW;

	/** Icon for {@link PhotoStatus#WARNING_LENGTH}. **/
	private final ImageIcon ICON_WARN_LEN;
	
	/** Icon for {@link PhotoStatus#REFUSE_LENGTH}. **/
	private final ImageIcon ICON_REFUSE_LEN;
	
	/** Icon for {@link PhotoStatus#REFUSE_SYMBOL}. **/
	private final ImageIcon ICON_REFUSE_SYM;
	
	/** Icon for undoing custom edits. **/
	private final ImageIcon ICON_UNDO;
	
	/** Icon for {@link PhotoStatus#REFUSE_DUPLICATE}. **/
	private final ImageIcon ICON_REFUSE_DUPE;
	
	/**
	 * Create a photo collection with the necessary icons already loaded.
	 * 
	 * @param engine Maps to {@link #ENGINE}.
	 * @param photos Maps to {@link #PHOTOS}.
	 */
	public PhotoCollection(Engine engine, ArrayList<Photo> photos)
	{
		ENGINE = engine;
		PHOTOS = photos;
		
		// Load icons for the files table.
		ICON_OK = loadAndResizeIcon("assets/green-check.png");
		ICON_ERR_SEVERE = loadAndResizeIcon("assets/red-x.png");
		ICON_VIEW = loadAndResizeIcon("assets/view-photo.png");
		ICON_ERR_MINOR = loadAndResizeIcon("assets/warning.png");
		ICON_WARN_LEN = loadAndResizeIcon("assets/paper.png");
		ICON_REFUSE_LEN = loadAndResizeIcon("assets/paper-red.png");
		ICON_UNDO = loadAndResizeIcon("assets/undo.png");
		ICON_REFUSE_SYM = loadAndResizeIcon("assets/symbol.png");
		ICON_REFUSE_DUPE = loadAndResizeIcon("assets/Duplicate.png");
	}
	
	/** Check data and update {@link #BLOCK_EXECUTION}. **/
	/**
	 * Check data and update {@link #BLOCK_EXECUTION}.
	 * Specifically, execution is blocked if any photo status
	 * {@link PhotoStatus#isAtLeastAsBadAs(PhotoStatus) is at last as bad as} {@link PhotoStatus#ERROR_MINOR}.
	 */
	private void checkData()
	{
		for(Photo photo : PHOTOS)
			if(photo.status.isAtLeastAsBadAs(PhotoStatus.ERROR_MINOR))
			{
				BLOCK_EXECUTION = true;
				return;
			}
		BLOCK_EXECUTION = false;
	}
	
	/** Set all photos' status to {@link PhotoStatus#READY}. **/
	public void clearAllStatuses()
	{
		for(Photo photo : PHOTOS)
			photo.status = PhotoStatus.READY;
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
				return ImageIcon.class;
			case 3:
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
	 * Get the {@link Photo#URL_LOCATION} for the photo on the specified row.
	 * 
	 * @param row The row from which to get the link address.
	 * @return The link address.
	 */
	public URL getLinkAddress(int row)
	{
		if(row < 0 || row > PHOTOS.size() - 1)
			return null;
		return PHOTOS.get(row).URL_LOCATION;
	}
	
	/**
	 * Get the date of the photo at the specified index.
	 * 
	 * @param index	The index (row) of the photo from which to read.
	 * @return	The photo description.
	 */
	public String getPhotoDate(int index)
	{
		return PHOTOS.get(index).DATE;
	}
	
	/**
	 * Get the description of the photo at the specified index.
	 * This is the same as calling {@link #getValueAt(int,int) getValueAt(index,3)}.
	 * 
	 * @param index	The index (row) of the photo from which to read.
	 * @return	The photo description.
	 */
	public String getPhotoDescription(int index)
	{
		return PHOTOS.get(index).description;
	}
	
	/**
	 * Get whether the photo at the specified index is customized.
	 * 
	 * @param index	The index (row) of the photo from which to read.
	 * @return	Whether the photo has been customized.
	 */
	public boolean getPhotoIsCustomized(int index)
	{
		return PHOTOS.get(index).customized;
	}
	
	/**
	 * Get the URL of the photo at the specified index.
	 * 
	 * @param index	The index (row) of the photo from which to read.
	 * @return	The photo URL.
	 */
	public URL getPhotoURL(int index)
	{
		return PHOTOS.get(index).URL_LOCATION;
	}
	
	@Override
	public int getRowCount()
	{
		return PHOTOS.size();
	}
	
	/**
	 * Alias for {@link #getRowCount()}.
	 * 
	 * @return The number of photos in the collection.
	 */
	public int getSize()
	{
		return getRowCount();
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		Photo row = PHOTOS.get(rowIndex);
		
		switch(columnIndex)
		{
			case 0:
				switch(row.status)
				{
					case READY: return null;
					case SAVED:	return ICON_OK;
					case ERROR_SEVERE: return ICON_ERR_SEVERE;
					case ERROR_MINOR: return ICON_ERR_MINOR;
					case WARNING_LENGTH: return ICON_WARN_LEN;
					case REFUSE_LENGTH: return ICON_REFUSE_LEN;
					case REFUSE_SYMBOL: return ICON_REFUSE_SYM;
					case REFUSE_DUPLICATE: return ICON_REFUSE_DUPE;
				};
			case 1:
				return ICON_VIEW;
			case 2:
				return
					PHOTOS.get(rowIndex).customized ? ICON_UNDO : null;
			case 3:
				return row.description;
			default:
				return null;
		}
	}
	
	/**
	 * Determine if the description on the specified row is duplicated
	 * on any other row.
	 * 
	 * @param row The row to check for duplicates.
	 * @return Whether any duplicates were found.
	 */
	private boolean hasDuplicate(int row)
	{
		for(int i = 0; i < PHOTOS.size(); i ++)
			if(i == row)
				continue;
			else if(PHOTOS.get(i).description.equalsIgnoreCase(PHOTOS.get(row).description))
				return true;
		return false;
	}
	
	@Override
	public boolean isCellEditable(int row, int col)
	{
		return col == 3;
	}
	
	/**
	 * @return Whether the data in this table should block execution.
	 */
	public boolean isExecutionBlocked()
	{
		return BLOCK_EXECUTION;
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
		ImageIcon icon = new ImageIcon(this.getClass().getClassLoader().getResource(path));
		Image image = icon.getImage();
		Image resized = image.getScaledInstance(16,16,Image.SCALE_SMOOTH);
		return new ImageIcon(resized);
	}
	
	/**
	 * Process descriptions for all photos.
	 * @return The highest-error status among all photos.
	 */
	public PhotoStatus processDescriptions()
	{
		PhotoStatus out = PhotoStatus.READY;
		DescriptionProcessor.processDescriptions(PHOTOS,ENGINE);
		for(Photo photo : PHOTOS)
			out = PhotoStatus.worst(out, photo.status);
		return out;
	}
	
	/**
	 * Clear customization of the photo at the specified index (row)
	 * and then reprocess.
	 * 
	 * @param index The index (row) of the photo to decustomize.
	 */
	public void unCustomizePhoto(int index)
	{
		PHOTOS.get(index).customized = false;
		processDescriptions();
	}
	
	/**
	 * Update the status of the photo at the specified index.
	 * 
	 * @param index		The index of the photo to update.
	 * @param status	The new status.
	 */
	public void setPhotoStatus(int index, PhotoStatus status)
	{
		PHOTOS.get(index).status = status;
		fireTableDataChanged();
	}
	
	@Override
	// This is only for manual editing, when a user double-clicks
	// into the description field.
	public void setValueAt(Object value, int row, int col)
	{
		if(col == 3)
		{
			Config CONFIG = ENGINE.getConfiguration();
			
			PHOTOS.get(row).description = value.toString();
			PHOTOS.get(row).customized = true;
			
			// Validate the length of the new description.
			PhotoStatus newStatus = PhotoStatus.READY;
			if(value.toString().length() > ConfigurationManager.OS_MAX_PATH)
				newStatus = PhotoStatus.REFUSE_LENGTH;
			else if(value.toString().length() < ConfigurationManager.MINIMUM_PATH)
				newStatus = PhotoStatus.REFUSE_LENGTH;
			else if(value.toString().length() > CONFIG.USER_MAX_FILE_LENGTH)
			{
				if(CONFIG.OVER_LENGTH == OverlengthBehavior.REFUSE)
						newStatus = PhotoStatus.REFUSE_LENGTH;
				else if(CONFIG.OVER_LENGTH != OverlengthBehavior.DO_NOTHING)
						newStatus = PhotoStatus.WARNING_LENGTH;
			}
			
			// Ensure the new description is not a duplicate.
			else if(hasDuplicate(row))
				newStatus = PhotoStatus.REFUSE_DUPLICATE;
			
			// Ensure the new description does not have invalid characters.
			else
			{
				Pattern pattern = Pattern.compile(ConfigurationManager.INVALID_CHARS);
				Matcher matcher = pattern.matcher(value.toString());
				if(matcher.find())
					newStatus = PhotoStatus.REFUSE_SYMBOL;
			}
			
			
			PHOTOS.get(row).status = newStatus;
			
			checkData();
			fireTableDataChanged();
			ENGINE.INPUTS_MODEL.reprocessAllFiles();
		}
	}
}
