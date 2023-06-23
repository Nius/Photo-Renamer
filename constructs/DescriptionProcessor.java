package constructs;

import java.util.ArrayList;

import constructs.ConfigurationManager.Config;
import constructs.ConfigurationManager.OverlengthBehavior;
import constructs.ConfigurationManager.ReplacementCharacter;
import constructs.Photo.PhotoStatus;
import engine.Engine;

/**
 * Provides functionality for processing photo descriptions.
 * 
 * @author Nicholas Harrell
 */
public class DescriptionProcessor
{
	/**
	 * Process the photos' original descriptions into new descriptions
	 * considering the configured options.
	 * 
	 * @param PHOTOS	The collection of photos whose descriptions to process.
	 * @param ENGINE	Central program instance, necessary for the following calls:
	 * 					{@link Engine#getConfiguration()},
	 * 					{@link Engine#updateFilePrefix(String,boolean)},
	 * 					{@link Engine#updateFileSuffix(String,boolean)},
	 * 					{@link Engine#updateUndescribed(String, boolean)}.
	 */
	public static void processDescriptions(ArrayList<Photo> PHOTOS, Engine ENGINE)
	{
		Config CONFIG = ENGINE.getConfiguration();
		
		// How wide does the index number need to be?
		// If there are 99 or fewer photos, then two digits is fine.
		// If there are 100 or more, then we need three digits.
		// Limitations set by HTMLProcessor guarantee that there will not be
		// four digits' worth of photos.
		final int INDEX_WIDTH = PHOTOS.size() >= 100 ? 3 : 2;
		
		IndexManager indexer = new IndexManager(INDEX_WIDTH);
		
		// Which character will replace any invalid characters is determined by configuration.
		// Even though we're using only one character, we need a String for two reasons:
		// - String.replaceAll requries a String parameter, not a char
		// - There could actually be zero characters as the replacement, and there is no way to
		//   represent zero characters with a char without getting dirty with null checks.
		String replacement;
		switch(CONFIG.REPLACEMENT_CHAR)
		{
			case HYPHEN:
				replacement = "-";
				break;
			case COMMA:
				replacement = ",";
				break;
			case NOTHING:
			default:
				replacement = "";
				break;
		}
		
		// The prefix, suffix, and default undescribed name need to be treated before being
		// applied to any descriptions, but only once, so do them before the loop.
		// Invalid characters must be removed (see notes inside the loop) and the related UI
		// controls need to be updated.
		// Do not trim the prefix, suffix, or unnamed because the user might want to have spaces
		// at the begining or end of these - especially at the end of the prefix or the beginning
		// of the suffix.
		String treatedPrefix = CONFIG.PREFIX.replaceAll(ConfigurationManager.INVALID_CHARS,replacement);
		String treatedSuffix = CONFIG.SUFFIX.replaceAll(ConfigurationManager.INVALID_CHARS,replacement);
		String treatedUnnamed = CONFIG.UNDESCRIBED.replaceAll(ConfigurationManager.INVALID_CHARS,replacement);
		if(CONFIG.REPLACEMENT_CHAR == ReplacementCharacter.NOTHING)
		{
			treatedPrefix = stripDuplicateWhitespaces(treatedPrefix);
			treatedSuffix = stripDuplicateWhitespaces(treatedSuffix);
			treatedUnnamed = stripDuplicateWhitespaces(treatedUnnamed);
		}
		ENGINE.updateFilePrefix(treatedPrefix,false);
		ENGINE.updateFileSuffix(treatedSuffix,false);
		ENGINE.updateUndescribed(treatedUnnamed,false);
		
		// For each photo...
		for(Photo photo : PHOTOS)
		{
			String localTreatedPrefix = treatedPrefix;
			String localTreatedSuffix = treatedSuffix;
			String localTreatedUnnamed = treatedUnnamed;
			
			// If the user has specified a custom description for this photo
			// (by clicking on the table and typing one in) then don't change it.
			if(photo.customized)
				continue;
			
			String description = photo.ORIGINAL_DESCRIPTION;
			
			/* Remove Trailing Numbers
			 * =======================
			 * 
			 * Depending on the device used to upload photos to DASH, there might be numbers after the
			 * description. For example, a series of kitchen photos might be called "Kitchen (1)",
			 * "Kitchen (2)", and "Kitchen (3)". Sometimes there are parentheses and sometimes not.
			 * 
			 * First we'll remove any numbers from the end that have no parentheses.
			 * Then we'll remove any numbers that have parentheses.
			 * 
			 * Note that these numbers were detected and recorded as preferred indexes in the constructor
			 * of the Photo object when it was instantiated.
			 * 
			 * Numbers at the begining of the description are not modified.
			 */
			if(CONFIG.REMOVE_TRAILING_NUMBERS)
			{
				// Naked - Do first in case there is some kind of non-indexing number
				// before a parenthetical index (such as "Bathroom 4 (51)")
				description = description.replaceAll("[0-9]+$","");
				
				// Parentheticals
				description = description.replaceAll(" *\\([0-9]+\\)$","");
			}
			
			/* Capitalization Correction
			 * =========================
			 * 
			 * All characters can be converted to integers according to the ASCII table. This is an in-built
			 * capability of Java. According to the ASCII table the lower-case letters "a" through "z" have,
			 * in order, the values from 97 to 122. Capital letters "A" through "Z" have values 65 through
			 * 90. Each lower-case letter's counterpart is exactly 32 positions below it, so in order to
			 * capitalize a specific character we do the following:
			 * 1. Convert the char to an int
			 * 2. Subtract 32 from the int
			 * 3. Convert the int back to a char
			 * 
			 * To do this inside a String, we do the following:
			 * 1. Take the substring from 0 to just before the char to be capitalized
			 * 2. Convert the char as described above
			 * 3. Append the rest of the String
			 */
			if(CONFIG.CORRECT_CAPS && description.length() > 0)
			{
				// Start with all lowercase.
				description = description.toLowerCase();
				
				// Capitalize the first character.
				description = (char)((int)(description.charAt(0)) - 32) + description.substring(1);
				
				// Capitalize the first character of each word. This will be any character for which:
				// - The current character is a lowercase letter (between 97 and 122 inclusive)
				// - The previous character is a space.
				for(int i = 1; i < description.length(); i ++)
					if(description.charAt(i - 1) == ' ' && (int)(description.charAt(i)) >= 97 && (int)(description.charAt(i)) <= 122)
						description =	description.substring(0,i) +
										(char)((int)(description.charAt(i)) - 32) +
										description.substring(i + 1);
			}
			
			/* Replace Invalid Characters
			 * ==========================
			 * 
			 * Invalid characters must be removed from the description because the description will
			 * eventually end up being the photo's file name. Invalid characters are any characters
			 * that are recognized on any major platform as being invalid for use in a file name.
			 * 
			 * The ConfigurationManager has a static field containing a regex pattern of all invalid
			 * characters.
			 * 
			 * The prefix, suffix, and undescribed default name entered by the user must also be
			 * treated, because they will be part of the filename too.
			 */

			// While replacing invalid characters, trim any leading or trailing whitespaces too.
			description = description.replaceAll(ConfigurationManager.INVALID_CHARS,replacement).trim();
			
			// Trim duplicate whitespaces, probable to exist after dropping invalid characters
			if(CONFIG.REPLACEMENT_CHAR == ReplacementCharacter.NOTHING)
				description = stripDuplicateWhitespaces(description);
			
			// If the description is empty, use the undescribed name.
			// Do this now, after all this other work, in case the original
			// description was something like "                ".
			
			if(description.length() == 0)
				description = localTreatedUnnamed;
			
			/* Length Enforcement
			 * ==================
			 * 
			 * There are three factors constraining the length of a description.
			 * For the purposes of this step, the description is considered to be the
			 * description PLUS the prefix, suffix, and index.
			 * 
			 * - The description's length must be at least 7. This is to provide enough
			 * 	 space for " - 001", which has a length of 6, plus at least one character
			 *   in the description. This limit cannot be overridden by the user.
			 * - The user can define a length limit via the UI controls. How this limit
			 *   is observed depends on the Over-Length Behavior setting; it may be
			 *   forcibly adhered to or it may be ignored.
			 * - The operating system defines an absolute limit to the length of a file's
			 *   path. Therefore, when an output directory is chosen, the difference
			 *   between the length of the output directory path and this absolute limit
			 *   is calculated and becomes the operating system-defined maximum file
			 *   length. This limit cannot be overridden by the user.
			 */
			
			// Length = prefix + description + suffix + 5 or 6 for the index.
			int currentTotalLength =
				localTreatedPrefix.length() +
				description.length() +
				localTreatedSuffix.length() +
				(PHOTOS.size() > 99 ? 6 : 5);
			int limit = Math.min(CONFIG.USER_MAX_FILE_LENGTH,CONFIG.OS_MAX_FILE_LENGTH);

			// Shorten the description using increasingly drastic measures until
			// the length is under the limit.
			shorteningLoop:
			while(currentTotalLength > limit)
			{
				// How much needs to be truncated, if truncating is going to happen.
				int truncateAmount = currentTotalLength - limit;
				
				// Depending on the user-defined behavior for over-length descriptions...
				switch(CONFIG.OVER_LENGTH)
				{
					// Drop vowels from all three parts of the description.
					case DROP_VOWELS:
						if(hasVowels(description))
						{
							description = dropVowels(description);
							break;
						}
						else if(hasVowels(localTreatedSuffix))
						{
							localTreatedSuffix = dropVowels(localTreatedSuffix);
							break;
						}
						else if(hasVowels(localTreatedPrefix))
						{
							localTreatedPrefix = dropVowels(localTreatedPrefix);
							break;
						}
						// NO BREAK HERE
						// If dropping vowels proves insufficient,
						// move on to truncation.
					
					// Forcibly truncate the description. This happens in three passes:
					// 1. Truncate the description first,
					// 2. Truncate the suffix second,
					// 3. Truncate the prefix last.
					// If, after completeing all three of these steps (in three separate
					// iterations of this loop), the whole description is still too long
					// then break the (now-infinite) loop. The limit violation will be
					// detected after the loop and handled appropriately.
					case TRUNCATE:
						if(description.length() > 0)
							description = truncateBy(description,truncateAmount);
						else if(localTreatedSuffix.length() > 0)
							localTreatedSuffix = truncateBy(localTreatedSuffix,truncateAmount);
						else if(localTreatedPrefix.length() > 0)
							localTreatedPrefix = truncateBy(localTreatedPrefix,truncateAmount);
						else
							break shorteningLoop;
						break;
					
					// In the cases of REFUSE, WARN, or DO_NOTHING, the program will not
					// make any automated changes. It's up to the user to correct the
					// descriptions before executing, or not.
					case REFUSE:
					case WARN:
					case DO_NOTHING:
					default:
						break shorteningLoop;
				}

				// Recalculate the current total length, considering the changes that were
				// made during this iteration.
				currentTotalLength =
					localTreatedPrefix.length() +
					description.length() +
					localTreatedSuffix.length() +
					(PHOTOS.size() > 99 ? 6 : 5); // " - 001" vs " - 01"
			}
			
			// If the description is still too long, then create a warning.
			
			// If longer than the absolute maximum length limit, set the photo to the REFUSE_LENGTH
			// state which will prevent the renamer from executing. This behavior cannot be overridden
			// by the user.
			if(currentTotalLength > ConfigurationManager.OS_MAX_PATH)
				photo.status = PhotoStatus.REFUSE_LENGTH;
			
			// If longer than the limit and over-length behavior is set to REFUSE, set the photo to
			// the REFUSE_LENGTH state which will prevent the renamer from executing.
			else if(currentTotalLength > limit && CONFIG.OVER_LENGTH == OverlengthBehavior.REFUSE)
				photo.status = PhotoStatus.REFUSE_LENGTH;
			
			// If longer than the limit and over-length behavior is set to anything other than DO_NOTHING
			// (or REFUSE, from the previous check), set the photo to the WARNING_LENGTH state which will
			// generate a visible warning but will not prevent execution.
			else if(currentTotalLength > limit && CONFIG.OVER_LENGTH != OverlengthBehavior.DO_NOTHING)
				photo.status = PhotoStatus.WARNING_LENGTH;
			
			// If the description's length is too short, refuse it.
			else if(currentTotalLength < ConfigurationManager.MINIMUM_PATH)
				photo.status = PhotoStatus.REFUSE_LENGTH;
			
			// If the description's length is under the limit, set its state to READY.
			else
				photo.status = PhotoStatus.READY;

			//
			// Index Registration
			// Register the photo with the index manager, which will
			// assign an index after all photos have been registered.
			// Actual appending of indexes should not be run until
			// this loop has iterated over all photos.
			//
			
			photo.description = localTreatedPrefix + description + localTreatedSuffix;
			indexer.registerPhoto(photo);
		}
		
		// Commit indexes to the photos.
		indexer.appendAllIndexes(CONFIG.INDEX_UNIQUE);
	}
	
	/**
	 * Remove all vowels from the subject String.
	 * 
	 * @param subject	The string to disemvowel.
	 * @return			The subject string, without vowels.
	 */
	private static String dropVowels(String subject)
	{
		return subject.replaceAll("[AaEeIiOoUu]","");
	}
	
	/**
	 * Determine whether a subject string contains vowels.
	 * 
	 * @param subject	The subject to search for vowels.
	 * @return			Whether vowels were found.
	 */
	private static boolean hasVowels(String subject)
	{
		String upper = subject.toUpperCase();
		return
			upper.contains("A") ||
			upper.contains("E") ||
			upper.contains("I") ||
			upper.contains("O") ||
			upper.contains("U");
	}
	
	/**
	 * Remove duplicated whitespaces from the subject String.
	 * Specifically, instances of "  "(2) are reduced to " "(1)
	 * repeatedly until there are no remaining instances of "  "(2).
	 * Therefore, series of spaces of any length greater than 1 are
	 * reduced to a length of 1.
	 * 
	 * @param subject	The text to strip of duplicate whitepsaces.
	 * @return			A new String resulting from the whitespace strip.
	 */
	private static String stripDuplicateWhitespaces(String subject)
	{
		// Search the string for any occurrences of "  "(2) and
		// replace them with " "(1). This has to be done multiple times
		// because on the first pass "   "(3) would be reduced to "  "(2),
		// which still needs to be reduced to " "(1).
		
		boolean clean = false;
		while(!clean)
		{
			String pre = subject;
			subject = subject.replaceAll("  "," ");
			clean = pre == subject;
		}
		return subject;
	}
	
	/**
	 * Removes the specified number of characters from the end of the given string.
	 * 
	 * @param subject			The string to be trunacted.
	 * @param truncateAmount	How many characters to remove.
	 * @return					A truncated string. If the truncate amount is more
	 * 							than the length of the string, an empty string is
	 * 							returned.
	 */
	private static String truncateBy(String subject, int truncateAmount)
	{
		if(subject.length() <= truncateAmount)
			return "";
		else
			return subject.substring(0,subject.length() - truncateAmount);
	}
}
