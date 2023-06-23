package constructs;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A representation of a photograph within a DASH album.
 * 
 * @author Nicholas Harrell
 */
public class Photo
{
	/** The source URL for the photo (from whence it will be downloaded). **/
	public final URL URL_LOCATION;
	
	/** The original description provided by DASH. **/
	public final String ORIGINAL_DESCRIPTION;
	
	/**
	 * The preferred index derived from the {@link #ORIGINAL_DESCRIPTION} if there was one, or -1.
	 * The maximum possible preferred index is 299; anything above that will be treated as if there
	 * is no preference at all.
	 */
	public final int PREFERRED_INDEX;
	
	/** The date this photo was created, as reported by DASH. **/
	public final String DATE;
	
	/** The current status for this photo. **/
	public PhotoStatus status = PhotoStatus.READY;
	
	/** The current working description created by Photo Renamer. **/
	public String description = "";
	
	/** The index assigned by an {@link IndexManager}. **/
	public int assignedIndex = -1;
	
	/** Whether this photo's description has been customized by the user directly. **/
	public boolean customized = false;
	
	/**
	 * Creates a new Photo. The {@link #PREFERRED_INDEX} is automatically derived, if present.
	 * 
	 * @param url			Maps to {@link #URL_LOCATION}.
	 * @param description	Maps to {@link #ORIGINAL_DESCRIPTION}.
	 * @param date			Maps to {@link #DATE}.
	 */
	public Photo(URL url, String description, String date)
	{
		DATE = date;
		URL_LOCATION = url;
		ORIGINAL_DESCRIPTION = description;
		
		// Pattern for detecting parenthetical numbers at the end of the description.
		// Example: Kitchen(4)
		String parenPatternS = ".*\\(([0-9]+)\\)$";
		Pattern parenPattern = Pattern.compile(parenPatternS);
		
		// Pattern for detecting non-parenthetical numbers at end of description.
		// Example: Kitchen4
		String nakedPatternS = ".*([0-9]+)$";
		Pattern nakedPattern = Pattern.compile(nakedPatternS);
		
		int prefindex = -1;
		try
		{
			Matcher paren = parenPattern.matcher(description);
			if(paren.find())
				prefindex = Integer.parseInt(paren.group(1));
			else
			{
				Matcher naked = nakedPattern.matcher(description);
				if(naked.find())
					prefindex = Integer.parseInt(naked.group(1));
				else
					prefindex = -1;
			}
		}
		catch(NumberFormatException e) {PREFERRED_INDEX = -1;return;}
		
		// If, for some reason, the preferred index is determined to be above 299,
		// then ignore it. The chances of having an album with 300 pictures are almost
		// nil, and the mechanism for marshalling indexes will do terrible things to
		// the heap if asked to handle a huge index like 300 or 2834932.
		if(prefindex > 299)
			prefindex = -1;
		
		PREFERRED_INDEX = prefindex;
	}
	
	/**
	 * A representation of the status of a photo, as to be displayed in the table.
	 * 
	 * @author Nicholas Harrell
	 */
	public enum PhotoStatus
	{
		// Note that the members of this enum are declared in order of severity
		// (of error), from best to worst.
		
		/** Saved to file system. **/
		SAVED,
		
		/** Default, starting state. **/
		READY,
		
		/** Description is too long. **/
		WARNING_LENGTH,
		
		/** Something is wrong. **/
		ERROR_MINOR,
		
		/** Desription is too long. **/
		REFUSE_LENGTH,
		
		/** Description contains invalid symbol. **/
		REFUSE_SYMBOL,
		
		/** Description is a duplicate of another. **/
		REFUSE_DUPLICATE,
		
		/** Error writing to or modifying file. **/
		ERROR_SEVERE;
		
		/**
		 * Deermine whether this PhotoStatus is at least as bad as another,
		 * considering the level of error.
		 * 
		 * @param other	The other status for comparison.
		 * @return	Whether this status is at least as bad as the other.
		 */
		public boolean isAtLeastAsBadAs(PhotoStatus other)
		{
			if(this == other)
				return true;
			return isWorseThan(other);
		}
		
		/**
		 * Deermine whether this PhotoStatus is worse than another, considering
		 * the level of error.
		 * 
		 * @param other	The other status for comparison.
		 * @return	Whether this status is worse than the other.
		 */
		public boolean isWorseThan(PhotoStatus other)
		{
			if(this == other)
				return false;
			
			PhotoStatus[] heirarchy = values();
			
			// The first item encountered is the lesser.
			for(int i = 0; i < heirarchy.length; i++)
				if(heirarchy[i] == this)
					return false;
				else if(heirarchy[i] == other)
					return true;
			
			return false;
		}
		
		/**
		 * Determine which of two statuses has the higher level of error.
		 * 
		 * @param a	A status to compare.
		 * @param b A status to compare.
		 * @return The higher-error status. Returns "a" if they're the same,
		 */
		public static PhotoStatus worst(PhotoStatus a, PhotoStatus b)
		{
			if(a == b)
				return a;
			
			if(a.isWorseThan(b))
				return a;
			return b;
		}
	}
}
