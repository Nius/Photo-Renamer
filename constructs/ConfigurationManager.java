package constructs;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import engine.Engine;

/**
 * This class reads, holds, manipulates, and writes configuration data
 * according to the configuration file as directed by the {@link Engine}.
 * This object is intended to be visible only to the {@link Engine}; when
 * other objects need to read configuration data they should do so via
 * a {@link Config} object retrieved from {@link #getConfig()}.
 * 
 * @author Nicholas Harrell
 */
public class ConfigurationManager
{
	//
	// Colors
	// Not intended to be configurable by the user,
	// but globalized here to make future changes easier.
	//
	
	/** Window background color. **/
	public final Color BG_COLOR = new Color(50,50,50);
	
	/** Text color. **/
	public final Color TXT_COLOR = new Color(240,240,240);
	
	/** Line color (for borders and the like). **/
	public final Color LINE_COLOR = new Color(150,150,150);
	
	/** Table background color. **/
	public final Color TABLE_BG_COLOR = new Color(0,0,0);
	
	//
	// Reference Constants
	//
	
	/** Default configuration file contentx. **/
	public static final String[] DEFAULT_CONFIG =
	{
		"AutoUseHTMLDir=TRUE",
		"CorrectCaps=TRUE",
		"DeleteInputFiles=TRUE",
		"IndexUniqueDescriptions=TRUE",
		"InputRootPath=",
		"MaximumFilenameLength=64",
		"OutputDirectory=",
		"OverLengthBehavior=WARN",
		"Prefix=",
		"RemoveTrailingNumbers=TRUE",
		"ReplacementCharacter=HYPHEN",
		"Suffix=",
		"UndescribedTitle=",
		"UpdateCreationDate=TRUE"
	};
	
	/** Regex pattern for invalid filename characters. **/
	public static final String INVALID_CHARS = "[#%&{}\\\\<>&?/$!'\":@+`|=]";
	
	/**
	 * Different OSes define different maximum file path lengths, so
	 * the maximum length this program will accept is one less than
	 * the lowest of these limits.
	 *  
	 * Windows: 260
	 * Linux: 255
	 * Mac: 1024
	 */
	public static final int OS_MAX_PATH = 254;
	
	/**
	 * Hard minimum file name length, so as to provide enough space
	 * for an index " - 001" and at least two characters of identification.
	 */
	public static final int MINIMUM_PATH = 8;
	
	/**
	 * The type of file to look for when seeking input files.
	 * May or may not include the preceding dot.
	 */
	public static final String INPUT_FILE_TYPE = ".MHTML";
	
	//
	// Operational Data
	//
	
	/** The location of the configuration file. **/
	private final Path CONFIG_FILE_PATH;
	
	/** Messages generated while loading. **/
	private final ArrayList<String> LOAD_MESSAGES = new ArrayList<String>();
	
	/** Temporary file directory. **/
	public final Path TEMP_FILE_DIRECTORY = Paths.get(System.getProperty("user.dir") + "/temp/");
	
	//
	// Programatically Set Pseudo-Options
	//
	
	/**
	 * Maximum filename length considering the specified output directory
	 * and {@link #OS_MAX_PATH}.
	 * Considers the current working directory by default.
	 */
	private int osMaxFileLength =
		OS_MAX_PATH - System.getProperty("user.dir").length();
	
	//
	// User Options
	//
	
	/** Whether to automatically use the HTML directory for output. Default true. **/
	private boolean autoUseHTMLDir = true;
	
	/** Whether to correct capitalization. Default true. **/
	private boolean correctCaps = true;
	
	/** Whether to delete the source HTML file after downloading. **/
	private boolean deleteHTML;
	
	/** Whether to index unique descriptions. Default true. **/
	private boolean indexUnique = true;
	
	/** Most recently used root directory for input files. Default null. **/
	private Path inputDirectory = null;
	
	/** User-defined maximum filename length. Default 64, minimum of 7.**/
	private int maxLength = 64;
	
	/** Whether to modify the file created date to match Dash. Default true.**/
	private boolean modifyDate = true;
	
	/** Most recently used output directory. Default null. **/
	private Path outputDirectory = Paths.get(System.getProperty("user.home"));
	
	/**
	 * How to handle filenames longer than {@link #maxLength}.
	 * Default {@link OverlengthBehavior#WARN}. 
	 */
	private OverlengthBehavior overLengthBehavior = OverlengthBehavior.WARN;
	
	/** Manual prefix for all files. Default blank.**/
	private String prefix = "";
	
	/** Whether to remove trailing numbers. Default true.**/
	private boolean removeTrailingNumbers = true;
	
	/**
	 * What character to use to replace invalid filename characters.
	 * Default {@link ReplacementCharacter#HYPHEN}.
	 */
	private ReplacementCharacter replacementChar = ReplacementCharacter.HYPHEN;
	
	/** Manual suffix for all files. Default blank.**/
	private String suffix = "";
	
	/** Description to use for photos that are undescribed. Default "Undescribed". **/
	private String undescribed = "Undescribed";

	/**
	 * Initialize the config manager with respect to the current user.
	 * This ensures that there is no collision between config options for multiple users
	 * accessing the program from the same working directory, such as if the program is
	 * saved to a network drive.
	 */
	private ConfigurationManager()
	{
		String CWD = System.getProperty("user.dir");
		String USR = System.getProperty("user.name");
		CONFIG_FILE_PATH = Paths.get(CWD + "/configuration." + USR + ".txt");
	}


	/**
	 * Write all existing config parameters to othe config file, overwriting it.
	 * 
	 * @throws IOException An exception is thrown if the file write operation fails. 
	 */
	public void commitToFile() throws IOException
	{
		String[] lines = new String[]
		{
			"AutoUseHTMLDir=" + autoUseHTMLDir,
			"CorrectCaps=" + correctCaps,
			"DeleteInputFiles=" + deleteHTML,
			"IndexUniqueDescriptions=" + indexUnique,
			"InputRootPath=" + inputDirectory.toString(),
			"MaximumFilenameLength=" + maxLength,
			"OutputDirectory=" + (outputDirectory == null ? "" : outputDirectory.toString()),
			"OverLengthBehavior=" + overLengthBehavior.toString(),
			"Prefix=" + prefix,
			"RemoveTrailingNumbers=" + removeTrailingNumbers,
			"ReplacementCharacter=" + replacementChar.toString(),
			"Suffix=" + suffix,
			"UndescribedTitle=" + undescribed,
			"UpdateCreationDate=" + modifyDate
		};
		
		String output = "";
		for(String line : lines)
			output += line + "\n";
		
		Files.writeString(CONFIG_FILE_PATH,output,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * Create a new default config file.
	 * @param outputPath				Where to write the configuration file.
	 * @throws IOException 				An exception is thrown if an error occurs while writing the file.
	 */
	private static void createDefaultConfig(Path outputPath) throws IOException
	{
		Files.write(outputPath,Arrays.asList(ConfigurationManager.DEFAULT_CONFIG),StandardOpenOption.CREATE_NEW);
	}
	
	/**
	 * Gets {@link #autoUseHTMLDir}.
	 * 
	 * @return {@link #autoUseHTMLDir}.
	 */
	public boolean getAutoUseHTMLDir()
	{
		return autoUseHTMLDir;
	}
	
	/**
	 * Get a read-only copy of the current configuration options.
	 * Note that the returned object will contain a snapshot of the options
	 * as they are at the time of this method call; those options will not
	 * be updated as further changes are made to this ConfigurationManager.
	 * 
	 * @return	A read-only snapshot of the current configuration state.
	 */
	public Config getConfig()
	{
		return new Config(this);
	}

	/**
	 * Get all messages generated while loading the configuration file.
	 * 
	 * @return	A list of messages generated while loading the configuration file.
	 */
	public String[] getLoadMessages()
	{
		String[] result = new String[LOAD_MESSAGES.size()];
		for(int i = 0; i < LOAD_MESSAGES.size(); i++)
			result[i] = LOAD_MESSAGES.get(i);
		
		return result;
	}
	
	/**
	 * Get the input directory location.
	 * 
	 * @return A clone of {@link #inputDirectory}.
	 */
	public Path getInputDirectory()
	{
		return
				inputDirectory == null ? null :
				Paths.get(inputDirectory.toString());
	}
	
	/**
	 * Get the current maximum file length as defined by the OS.
	 * 
	 * @return Returns {@link #osMaxFileLength}.
	 */
	public int getOsMaxFileLength()
	{
		return osMaxFileLength;
	}
	
	/**
	 * Get the current output directory.
	 * 
	 * @return A clone of {@link #outputDirectory}.
	 */
	public Path getOutputDirectory()
	{
		return
				outputDirectory == null ? null :
				Paths.get(outputDirectory.toString());
	}

	/**
	 * Convert a String to a path. If the path is invalid or does not exist
	 * then the user's home directory or null is returned instead.
	 * 
	 * @param input			The string to convert.
	 * @param nullInstead	Whether to return null instead of the user home directory.
	 * @return				A path derived from the string, or else the home directory.
	 */
	private static Path getPathOrHome(String input, boolean nullInstead)
	{
		// Allow pasting of "copy as path" without having to remove quotes.
		input = input.replace("\"","");
		
		if(input.length() == 0)
			return nullInstead ? null : Paths.get(System.getProperty("user.home"));
		
		try
		{
			Path dirPath = Paths.get(input);
			if(Files.exists(dirPath))
				return dirPath;
			else
				return nullInstead ? null : Paths.get(System.getProperty("user.home"));
		}
		catch(InvalidPathException e)
		{
			return nullInstead ? null : Paths.get(System.getProperty("user.home"));
		}
	}
	
	/**
	 * Load configuration. If not exists, create a default one.
	 * 
	 * @return	A populated configuration.
	 */
	public static ConfigurationManager readConfig()
	{
		ConfigurationManager CONFIG = new ConfigurationManager();

		List<String> cfgLines = Collections.emptyList();
		
		CONFIG.LOAD_MESSAGES.add("Reading configuration file from " + CONFIG.CONFIG_FILE_PATH.toString()); 
		
		// Check whether the configuration file exists.
		// If it does not, create a new one.
		if(!Files.exists(CONFIG.CONFIG_FILE_PATH))
		{
			CONFIG.LOAD_MESSAGES.add("The configuration file does not exist. Creating a new default one...");
			try
			{
				ConfigurationManager.createDefaultConfig(CONFIG.CONFIG_FILE_PATH);
				
				// A default config was created, so skip loading the file and just
				// use default values instead.
				return CONFIG;
			}
			catch(IOException e)
			{
				CONFIG.LOAD_MESSAGES.add("Failed to create configuration file. Configuration changes made while using this program will probably not be saved.");
			}
		}
		
		// Check whether the file can be read from and written to.
		boolean fileOK =
			Files.isRegularFile(CONFIG.CONFIG_FILE_PATH) &&
			Files.isReadable(CONFIG.CONFIG_FILE_PATH) &&
			Files.isWritable(CONFIG.CONFIG_FILE_PATH);
		if(!fileOK)
			CONFIG.LOAD_MESSAGES.add("The configuration file exists, but is inaccessible. Configuration changes made while using this program will probably not be saved. Using default values.");
		else
		{
			try
			{
				cfgLines = Files.readAllLines(CONFIG.CONFIG_FILE_PATH);
			}
			catch(IOException e)
			{
				CONFIG.LOAD_MESSAGES.add("The configuration file exists and should have been readable, but something went wrong trying to read it. Using default values.");
				return CONFIG;
			}
			
			// Keep track of the line number, so we can give more helpful error
			// messages in case of a problem.
			int lineNum = 0;
			
			// We'll be doing some checks on the input and output paths after the
			// parsing loop, so declare them here.
			Path outputDir = null;
			Path inputPath = null;
			boolean dirMatch = true;
			
			// Process the lines of text loaded from the config file.
			for(String line : cfgLines)
			{
				/* Configuration Format
				 * ====================
				 * 
				 * Each line should be formatted as follows:
				 * 
				 * Key=Value
				 * Key = Value
				 * 
				 * If there is not exactly one equals sign on a line then
				 * it is not recognized as a valid config option.
				 * 
				 * Blank lines are ignored.
				 * Lines begining with a hash are ignored.
				 * Spaces are trimmed from keys and values.
				 */
				
				// Advance the line count, no matter what this line looks like.
				lineNum ++;
				
				// Ignore blank lines.
				if(line.length() == 0)
					continue;
				
				// Ignore lines begining with a hash.
				if(line.charAt(0) == '#')
					continue;
				
				// Check the line format (exactly one equals sign).
				String[] splat = line.split("=",-1);
				if(splat.length != 2)
					CONFIG.LOAD_MESSAGES.add("Configuration error on line " + lineNum + ": malformed option.");
				
				// Trim the key and the value.
				String key = splat[0].trim();
				String value = splat[1].trim();
				
				if(key.equalsIgnoreCase("AutoUseHTMLDir"))
					dirMatch = Boolean.parseBoolean(value);
				
				else if(key.equalsIgnoreCase("CorrectCaps"))
					CONFIG.correctCaps = Boolean.parseBoolean(value);
				
				else if(key.equalsIgnoreCase("DeleteInputFiles"))
					CONFIG.deleteHTML = Boolean.parseBoolean(value);
				
				else if(key.equalsIgnoreCase("IndexUniqueDescriptions"))
					CONFIG.indexUnique = Boolean.parseBoolean(value);
				
				else if(key.equalsIgnoreCase("InputRootPath"))
					inputPath = getPathOrHome(value,true);
				
				else if (key.equalsIgnoreCase("MaximumFilenameLength"))
					try{CONFIG.maxLength = Integer.parseInt(value);}
					catch(NumberFormatException e) {/* Do Nothing */}
				
				else if(key.equalsIgnoreCase("OutputDirectory"))
				{
					if(value.length() > 0)
						outputDir = getPathOrHome(value,true);
				}
				
				else if(key.equalsIgnoreCase("OverLengthBehavior"))
					try{CONFIG.overLengthBehavior = OverlengthBehavior.valueOf(value);}
					catch(IllegalArgumentException e) {CONFIG.overLengthBehavior = OverlengthBehavior.REFUSE;}
				
				else if(key.equalsIgnoreCase("Prefix"))
					CONFIG.prefix = value;
				
				else if(key.equalsIgnoreCase("RemoveTrailingNumbers"))
					CONFIG.removeTrailingNumbers = Boolean.parseBoolean(value);
				
				else if(key.equalsIgnoreCase("ReplacementCharacter"))
					try{CONFIG.replacementChar = ReplacementCharacter.valueOf(value);}
					catch(IllegalArgumentException e) {CONFIG.replacementChar = ReplacementCharacter.HYPHEN;}
				
				else if(key.equalsIgnoreCase("Suffix"))
					CONFIG.suffix = value;
				
				else if(key.equalsIgnoreCase("UndescribedTitle"))
				{
					if(value.length() > 0)
						CONFIG.undescribed = value;
				}
				
				else if(key.equalsIgnoreCase("UpdateCreationDate"))
					CONFIG.modifyDate = Boolean.parseBoolean(value);
				
				else
					CONFIG.LOAD_MESSAGES.add("Unknown option on line " + lineNum + ": \"" + key + "\"");
			}
			
			// If the input directory or the output directory was specified but does not exist, nullify it.
			if(inputPath != null && !Files.exists(inputPath))
				inputPath = null;
			if(outputDir != null && !Files.exists(outputDir))
				outputDir = null;
			
			// Set the input directory
			try {CONFIG.setInputDirectory(inputPath);}
			catch(NotDirectoryException e)
				{CONFIG.LOAD_MESSAGES.add("The specified input directory either does not exist or is otherwise not an accessible directory.");}

			CONFIG.autoUseHTMLDir = dirMatch;
			
			if(outputDir != null)
				try	{CONFIG.setOutputDirectory(outputDir);}
				catch(NotDirectoryException e)
					{CONFIG.LOAD_MESSAGES.add("The specified output directory either does not exist or is otherwise not an accessible directory.");}
		}
		
		// If no other messages were generated during loading, simply add the "Done" message
		// to the same line that announced loading was staring.
		if(CONFIG.LOAD_MESSAGES.size() == 1)
			CONFIG.LOAD_MESSAGES.set(0,CONFIG.LOAD_MESSAGES.get(0) + ": Done.");
		else
			CONFIG.LOAD_MESSAGES.add("Done.");
		
		return CONFIG;
	}
	
	/**
	 * Set {@link #correctCaps}.
	 * 
	 * @param newStatus New value for {@link #correctCaps}.
	 */
	public void setCorrectCaps(boolean newStatus)
	{
		correctCaps = newStatus;
	}
	
	/**
	 * Set {@link #deleteHTML}.
	 * 
	 * @param newStatus New value for {@link #deleteHTML}.
	 */
	public void setDeleteHTML(boolean newStatus)
	{
		deleteHTML = newStatus;
	}
	
	/**
	 * Set {@link #autoUseHTMLDir}.
	 * 
	 * @param match New value for {@link #autoUseHTMLDir}.
	 */
	public void setDirectoryMatch(boolean match)
	{
		autoUseHTMLDir = match;
	}
	
	/**
	 * Set {@link #indexUnique}.
	 * 
	 * @param newStatus New value for {@link #indexUnique}.
	 */
	public void setIndexUnique(boolean newStatus)
	{
		indexUnique = newStatus;
	}
	
	/**
	 * Set the input directory path.
	 * The {@link #osMaxFileLength} will be set with respect to the longest-pathed subdirectory
	 * of the provided dirPath.
	 * 
	 * @param dirPath The path to the input file.
	 * @throws NotDirectoryException	An exception is thrown if the provided {@link Path} points to
	 * 									a file system location that does not exist or is otherwise
	 * 									not a directory. 
	 */
	public void setInputDirectory(Path dirPath) throws NotDirectoryException
	{
		if(dirPath == null)
			throw new NotDirectoryException("null");
		if(!Files.isDirectory(dirPath))
			throw new NotDirectoryException(dirPath.toString());
		
		inputDirectory = dirPath;
	}
	
	/**
	 * Set {@link #modifyDate}.
	 * 
	 * @param newStatus New value for {@link #modifyDate}.
	 */
	public void setModifyDate(boolean newStatus)
	{
		modifyDate = newStatus;
	}
	
	/**
	 * Sets the output directory and updates {@link #osMaxFileLength}.
	 * 
	 * @param outputDir The directory to set as the output directory.
	 * @throws NotDirectoryException	An exception is thrown if the provided {@link Path} points to
	 * 									a file system location that does not exist or is otherwise
	 * 									not a directory.
	 */
	public void setOutputDirectory(Path outputDir) throws NotDirectoryException
	{
		if(outputDir == null)
			throw new NotDirectoryException("null");
		if(!Files.isDirectory(outputDir))
			throw new NotDirectoryException(outputDir.toString());
		
		outputDirectory = outputDir;
		osMaxFileLength = OS_MAX_PATH - outputDir.toString().length();
	}
	
	/**
	 * Set {@link #overLengthBehavior}.
	 * 
	 * @param olb New value for {@link #overLengthBehavior}.
	 */
	public void setOverLengthBehavior(OverlengthBehavior olb)
	{
		overLengthBehavior = olb;
	}
	
	/**
	 * Set {@link #prefix}.
	 * 
	 * @param newPrefix New value for {@link #prefix}.
	 */
	public void setPrefix(String newPrefix)
	{
		prefix = newPrefix;
	}
	
	/**
	 * Set {@link #removeTrailingNumbers}.
	 * 
	 * @param newStatus New value for {@link #removeTrailingNumbers}.
	 */
	public void setRemoveTrailingNumbers(boolean newStatus)
	{
		removeTrailingNumbers = newStatus;
	}
	
	/**
	 * Set {@link #replacementChar}.
	 * 
	 * @param rc New value for {@link #replacementChar}.
	 */
	public void setReplacementCharacter(ReplacementCharacter rc)
	{
		replacementChar = rc;
	}
	
	/**
	 * Set {@link #suffix}.
	 * 
	 * @param newSuffix New value for {@link #suffix}.
	 */
	public void setSuffix(String newSuffix)
	{
		suffix = newSuffix;
	}
	
	/**
	 * Set {@link #undescribed}.
	 * 
	 * @param newUndescribed Value for {@link #undescribed}.
	 */
	public void setUndescribed(String newUndescribed)
	{
		undescribed = newUndescribed;
	}
	
	/**
	 * Set {@link #maxLength}.
	 * 
	 * @param maxLen New value for {@link #maxLength}.
	 */
	public void setUserMaxLength(int maxLen)
	{
		maxLength = maxLen;
	}

	/**
	 * How to respond to filenames that are too long.
	 * 
	 * @author Nicholas Harrell
	 */
	public static enum OverlengthBehavior
	{
		/** The program will not rename anything if there are any too-long names.**/
		REFUSE,
		/** A warning will be generated if there are any too-long names, but nothing else will happen.**/
		WARN,
		/** Chop off the excess of the file name, enough to then append the {@link ConfigurationManager#suffix} and index. **/
		TRUNCATE,
		/** Remove all vowels from the file name, then {@link #TRUNCATE}.**/
		DROP_VOWELS,
		/** Don't do anything at all.**/
		DO_NOTHING
	}
	
	/**
	 * What character to use to replace invalid filename characters.
	 * 
	 * @author Nicholas Harrell
	 */
	public static enum ReplacementCharacter
	{
		/** Hyhen (-)**/
		HYPHEN,
		/** Comma (,)**/
		COMMA,
		/** Nothing **/
		NOTHING;
		
		/**
		 * Get the actual character indicated by the {@link ReplacementCharacter}.
		 * 
		 * @return	The character, represented as a String. This allows return of
		 * 			an empty String in case of "NOTHING".
		 */
		public String getChar()
		{
			switch(this)
			{
				case HYPHEN: return "-";
				case COMMA: return ",";
				default: return "";
			}
		}
	}
	
	/**
	 * A vehicle for passing configuration data in a read-only manner.
	 * 
	 * @author Nicholas Harrell
	 */
	public class Config
	{
		/** Maps from {@link ConfigurationManager#osMaxFileLength}. **/
		public final int OS_MAX_FILE_LENGTH;
		/** Maps from {@link ConfigurationManager#autoUseHTMLDir}. **/
		public final boolean AUTO_USE_HTML_DIR;
		/** Maps from {@link ConfigurationManager#correctCaps}. **/
		public final boolean CORRECT_CAPS;
		/** Maps from {@link ConfigurationManager#deleteHTML}. **/
		public final boolean DELETE_HTML;
		/** Maps from {@link ConfigurationManager#indexUnique}. **/
		public final boolean INDEX_UNIQUE;
		/** Maps from {@link ConfigurationManager#inputDirectory}. **/
		public final Path INPUT_DIRECTORY_PATH;
		/** Maps from {@link ConfigurationManager#maxLength}. **/
		public final int USER_MAX_FILE_LENGTH;
		/** Maps from {@link ConfigurationManager#modifyDate}. **/
		public final boolean MODIFY_DATE;
		/** Maps from {@link ConfigurationManager#outputDirectory}. **/
		public final Path OUTPUT_DIRECTORY;
		/** Maps from {@link ConfigurationManager#overLengthBehavior}. **/
		public final OverlengthBehavior OVER_LENGTH;
		/** Maps from {@link ConfigurationManager#prefix}. **/
		public final String PREFIX;
		/** Maps from {@link ConfigurationManager#removeTrailingNumbers}. **/
		public final boolean REMOVE_TRAILING_NUMBERS;
		/** Maps from {@link ConfigurationManager#replacementChar}. **/
		public final ReplacementCharacter REPLACEMENT_CHAR;
		/** Maps from {@link ConfigurationManager#suffix}. **/
		public final String SUFFIX;
		/** Maps from {@link ConfigurationManager#TEMP_FILE_DIRECTORY}. **/
		public final Path TEMP_FILE_DIR;
		/** Maps from {@link ConfigurationManager#undescribed}. **/
		public final String UNDESCRIBED;
		
		/**
		 * @param cm An instance of ConfigurationManager of which to take a snapshot.
		 * 			 Note that these values are not updated as the ConfigurationManager
		 * 			 changes; they are set as final upon creation of the Config object.
		 */
		public Config (ConfigurationManager cm)
		{
			OS_MAX_FILE_LENGTH = cm.osMaxFileLength;
			AUTO_USE_HTML_DIR = cm.autoUseHTMLDir;
			CORRECT_CAPS = cm.correctCaps;
			DELETE_HTML = cm.deleteHTML;
			INDEX_UNIQUE = cm.indexUnique;
			
			// Create a copy rather than pass by reference.
			INPUT_DIRECTORY_PATH =
					cm.inputDirectory == null ? null :
					Paths.get(cm.inputDirectory.toString());
			
			USER_MAX_FILE_LENGTH = cm.maxLength;
			MODIFY_DATE = cm.modifyDate;
			
			// Create a copy rather than pass by reference.
			OUTPUT_DIRECTORY =
					cm.outputDirectory == null ? null :
					Paths.get(cm.outputDirectory.toString());
			
			OVER_LENGTH = cm.overLengthBehavior;
			PREFIX = cm.prefix;
			REMOVE_TRAILING_NUMBERS = cm.removeTrailingNumbers;
			REPLACEMENT_CHAR = cm.replacementChar;
			SUFFIX = cm.suffix;
			TEMP_FILE_DIR = cm.TEMP_FILE_DIRECTORY;
			UNDESCRIBED = cm.undescribed;
		}
	}
}