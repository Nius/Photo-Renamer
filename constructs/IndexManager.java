package constructs;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages indexing of photos with respect to their "preferred" index.
 * For photos that end with a number (parenthetical or naked) the IndexManager
 * will attempt to assign them the same index, and photos that don't have a
 * preferred index will be given an arbitrary index.
 * 
 * In all cases, indexes are sequential and no two photos can have the same
 * index. If two photos have the same preferred index then the first to be
 * registered will receive it while the other will receive an arbitrary
 * index.
 * 
 * @author Nicholas Harrell
 */
public class IndexManager
{
	/** All unique labels. **/
	private ArrayList<PhotoLabel> LABELS = new ArrayList<PhotoLabel>();
	
	/**
	 * How many digits (how "wide") the indexes must be.
	 * Currently only has meaning if set to 2 or 3.
	 */
	private final int WIDTH;
	
	/**
	 * Creates a new IndexManager with the specified index width.
	 * 
	 * @param indexWidth Maps to {@link #WIDTH}.
	 */
	public IndexManager(int indexWidth)
	{
		WIDTH = indexWidth;
	}
	
	/**
	 * Add a photo to be considered for indexing. Note that indexes are not
	 * actually applied until call of {@link #appendAllIndexes(boolean)}.
	 * 
	 * Photos are stored in what is effectively a two-dimensional array where
	 * the primary dimension is keyed by the description and the secondary is
	 * keyed arbitrarily. For example, all photos whose descriptions are
	 * "Kitchen" are stored in their own array, which is stored in an array of
	 * such arrays.
	 * 
	 * At this point we don't care about the order of photos in the second-
	 * dimensional array.
	 * 
	 * @param photo	The photo to add.
	 */
	@SuppressWarnings("unlikely-arg-type")
	public void registerPhoto(Photo photo)
	{
		// See PhotoLabel#equals(Object other) which can accommodate a String.
		int index = -1;
		for(int i = 0; i < LABELS.size(); i ++)
			if(LABELS.get(i) != null && LABELS.get(i).equals(photo.description))
				index = i;
		
		if(index == -1)
		{
			PhotoLabel label = new PhotoLabel(photo.description, WIDTH);
			LABELS.add(label);
			label.register(photo);
		}
		else
			LABELS.get(index).register(photo);
	}
	
	/**
	 * For every registered photo, appends its determined index to the description. 
	 * @param indexUnique	Whether to append indices to unique descriptions.
	 **/
	public void appendAllIndexes(boolean indexUnique)
	{
		for(PhotoLabel label : LABELS)
			label.appendAllIndexes(indexUnique);
	}
	
	/**
	 * Keeps track of photos (and their preferred indexes) for a given
	 * description.
	 * 
	 * @author Nicholas Harrell
	 */
	private class PhotoLabel
	{
		/** Photos with preferred indexes. **/
		private final HashMap<Integer,Photo> WITH_PREF = new HashMap<Integer,Photo>();
		
		/** Photos without preferred indexes. **/
		private final ArrayList<Photo> NON_PREF = new ArrayList<Photo>();
		
		/** The description of all photos in this list. **/
		private final String LABEL;
		
		/** How many digits the indexes must be. **/
		private final int WIDTH;
		
		/**
		 * Sets up index tracking for the given label.
		 * 
		 * @param label	The label or photo description.
		 * @param width Maps to {@link #WIDTH}.
		 */
		public PhotoLabel(String label, int width)
		{
			LABEL = label;
			WIDTH = width;
		}
		
		/**
		 * Add a photo, sensitive to its {@link Photo#PREFERRED_INDEX}.
		 * If the preferred index has not already been taken, this photo
		 * will occupy it.
		 * If there is no preferred index, or the preferred index has
		 * already been taken, this photo will be assigned a random
		 * index.
		 * 
		 * @param photo The photo to index.
		 */
		public void register(Photo photo)
		{
			if(photo.PREFERRED_INDEX < 0)
				NON_PREF.add(photo);
			else
			{
				Integer index = Integer.valueOf(photo.PREFERRED_INDEX);
				if(WITH_PREF.get(index) == null)
					WITH_PREF.put(index,photo);
				else
					NON_PREF.add(photo);
			}
		}
		
		/**
		 * For every registered photo, appends its determined index to the description.
		 * 
		 * @param indexUnique Whether to append indices to unique descriptions.
		 **/
		public void appendAllIndexes(boolean indexUnique)
		{
			int totalPhotos = NON_PREF.size() + WITH_PREF.size();
			
			if(!indexUnique && totalPhotos == 1)
				return;
			
			int currentIndex = 1;
			
			//
			// PHASE 1
			// Count up from 0. If there is a photo that prefers an index,
			// use it. Otherwise use the next non-preferential photo. This phase
			// ends when there are no more non-preferential photos (or when there
			// are no more photos at all, signified by running out of
			// non-preferential photos).
			//
			
			int nextNonPrefPhoto = 0;
			while(currentIndex <= totalPhotos)
			{
				String indexString = " - " +
						(WIDTH == 3 && currentIndex < 100 ? "0" : "") +
						(currentIndex < 10 ? "0" : "") +
						currentIndex;
				
				// First, try to pull a photo that prefers the current index.
				Photo preferred = WITH_PREF.get(Integer.valueOf(currentIndex));
				if(preferred != null)
					preferred.description += indexString;
				
				// If no photo prefers the current index, use the next non-preferential one.
				else
				{
					// ...BUT if we're out of non-preferential photos, move on to the next phase.
					if(nextNonPrefPhoto >= NON_PREF.size())
						break;
					
					Photo nonPref = NON_PREF.get(nextNonPrefPhoto);
					nonPref.description += indexString;
					nextNonPrefPhoto ++;
				}
				currentIndex ++;
			}
			
			//
			// PHASE 2
			// Continue counting upwards, consuming all preferential photos
			// in order and, ignoring their preferences, assigning them the
			// next index. This phase ends when there are no more photos.
			//
			
			int nextPreferredPhoto = currentIndex + 1;
			while(currentIndex <= totalPhotos)
			{
				String indexString = " - " +
						(WIDTH == 3 && currentIndex < 100 ? "0" : "") +
						(currentIndex < 10 ? "0" : "") +
						currentIndex;
				
				// Starting immediately after the previously found preferred
				// photo, count up until the next preferred photo is found.
				Photo preferred = null;
				while(preferred == null)
				{
					preferred = WITH_PREF.get(Integer.valueOf(nextPreferredPhoto));
					nextPreferredPhoto ++;
				}
				
				preferred.description += indexString;
				
				currentIndex ++;
			}
		}

		/**
		 * A PhotoLabel is equal to another PhotoLabel if their
		 * {@link PhotoLabel#LABEL LABELs} match via {@link String#equalsIgnoreCase(String)}.
		 * 
		 * A PhotoLabel is equal to a String if its {@link PhotoLabel#LABEL} matches
		 * the String via {@link String#equalsIgnoreCase(String)}.
		 */
		@Override
		public boolean equals(Object other)
		{
			if(other == null)
				return false;
			
			if(other instanceof PhotoLabel)
				return ((PhotoLabel)other).LABEL.equalsIgnoreCase(LABEL);
			else if(other instanceof String)
				return ((String)other).equalsIgnoreCase(LABEL);
			else
				return false;
		}
	}
}
