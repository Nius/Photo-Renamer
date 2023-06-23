package constructs;

import java.nio.file.Path;

import constructs.HTMLProcessor.HTMLParseException;
import constructs.Photo.PhotoStatus;
import engine.Engine;

/**
 * The InputFile represents the file on the operating system and the photos it
 * contains in their extracted form.
 * 
 * @author Nicholas Harrell
 */
public class InputFile
{
	/** The location of the input file. **/
	public final Path FILE_PATH;
	
	/** The shortened path of the input file (for table display). **/
	public final String PATH_SHORTHAND;
	
	/** The current status for this file. **/
	public FileStatus status = FileStatus.READY;
	
	/**
	 * The collection of photos contained within this file,
	 * in their extracted form.
	 */
	private final PhotoCollection PHOTOS;
	
	/**
	 * Defines a new input file and processes it immediately.
	 *
	 * @param engine		The central program instance. Required for
	 * 						instantiation of {@link #PHOTOS}.
	 * @param filePath		Maps to {@link #FILE_PATH}.
	 * @throws HTMLParseException	An exception is thrown if an error arises while
	 * 								parsing the input file.
	 */
	public InputFile(Engine engine, Path filePath) throws HTMLParseException
	{
		FILE_PATH = filePath;
		PATH_SHORTHAND = getShorthandPath(filePath);
				
		try
		{
			PHOTOS = new PhotoCollection(engine,HTMLProcessor.processHTML(filePath));
			
			
			PHOTOS.processDescriptions();
		}
		catch (HTMLParseException e)
		{
			status = FileStatus.ERROR_SEVERE;
			throw e;
		}
	}
	
	/**
	 * Gets a reference to {@link #PHOTOS}.
	 * 
	 * @return A reference to {@link #PHOTOS}.
	 */
	public PhotoCollection getCollection()
	{
		return PHOTOS;
	}
	
	/**
	 * Express the ancestry of the provided subject as a String but with the
	 * following modifications:
	 * <ul>
	 *  <li> Any ancestor whose name is exactly "Photos" is shortened to "...".
	 *  <li> Any ancestor whose name contains "Customer Files" is omitted.
	 *  <li> Any ancestor that is a parent of an ancestor whose name contains
	 *  	 "Customer Files" is omitted.
	 * </ul>
	 * 
	 * @param subject The file or directory whose ancestors to examine.
	 * @return	A String representation of the subject's ancestors, abbreviated.
	 */
	private String getRelevantAncestorsOf(Path subject)
	{
		// If there is no parent, simply return the root.
		Path parent = subject.getParent();
		if(parent == null)
			return "/";
		
		String parentName = parent.getFileName().toString();
		
		// If the parent is simply called "Photos", shorten it to "..." and
		// continue ascending the tree.
		if(parentName.equalsIgnoreCase("Photos"))
			return ".../" + getRelevantAncestorsOf(parent);
		
		// If this item contains the text "Customer Files" then omit it and
		// all of its parents.
		else if(parentName.toUpperCase().contains("CUSTOMER FILES"))
			return ".../";
			
		// If this item does not match the previous criteria, just return its
		// name and continue ascending the tree.
		else
			return parentName + "/" + getRelevantAncestorsOf(parent);
	}
	
	/**
	 * Return the full path of the provided file but with its ancestry
	 * expressed according to
	 * {@link #getRelevantAncestorsOf(Path) getRelevantAncestorsOf(subject)}.
	 * 
	 * @param subject The Path to re-express.
	 * @return	The full path of the subject, including the subject itself, but
	 * 			with its ancestors shortened.
	 */
	private String getShorthandPath(Path subject)
	{
		return getRelevantAncestorsOf(subject) + subject.getFileName().toString();
	}
	
	/**
	 * Process (or re-process) descriptions for all photos in this file.
	 * 
	 * @return	The least-ready status among all processed photos according
	 * 			to {@link PhotoStatus#isWorseThan(PhotoStatus)}.
	 */
	public PhotoStatus processDescriptions()
	{
		PhotoStatus out = PHOTOS.processDescriptions();
		if(out.isAtLeastAsBadAs(PhotoStatus.ERROR_MINOR))
			status = FileStatus.ERROR_SEVERE;
		else
			status = FileStatus.READY;
		return out;
	}
	
	/**
	 * A representation of the status of an input file, as to be displayed in the table.
	 * 
	 * @author Nicholas Harrell
	 */
	public enum FileStatus
	{
		/** Default, starting state. **/
		READY,
		
		/** Saved to file system. **/
		SAVED,
		
		/** Error writing to or modifying file. **/
		ERROR_SEVERE,
	}
}
