package constructs;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 * Searches through the file tree for files of a given type.
 * 
 * @author Nicholas Harrell
 */
public class FileSearcher extends SimpleFileVisitor<Path>
{	
	/** 
	 * Whether to stop searching after finding the first result.
	 * Useful if you just want to know whether at least one file
	 * of the given type exists in the tree.
	 */
	public final boolean MONO_SEARCH;
	
	/**
	 * The type of file to search for. Preceding dot is optional because
	 * files are checked with a simple {@link String#endsWith(String)}
	 * comparison.
	 */
	public final String TYPE;
	
	/** The list of matching files. **/
	private final ArrayList<Path> MATCHES = new ArrayList<Path>();
	
	/**
	 * Creates a new file searcher.
	 * 
	 * @param mono_search	Maps to {@link #MONO_SEARCH}.
	 * @param type			Maps to {@link #TYPE}.
	 */
	public FileSearcher(boolean mono_search, String type)
	{
		MONO_SEARCH = mono_search;
		TYPE = type;
	}
	
	/**
	 * Check whether {@link #MATCHES} contains any matches at all.
	 * 
	 * @return Whether {@link #MATCHES} contains any matches.
	 */
	public boolean hasMatches()
	{
		return (MATCHES.size() > 0);
	}
	
	/**
	 * Get a copy of the list of matches.
	 * 
	 * @return	A clone of the list of matches.
	 */
	public Path[] getMatches()
	{
		Path[] paths = new Path[MATCHES.size()];
		for(int i = 0; i < MATCHES.size(); i ++)
			paths[i] = MATCHES.get(i);
		return paths;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr)
	{
		if(file.toString().toUpperCase().endsWith(TYPE.toUpperCase()))
		{
			MATCHES.add(file);
			if(MONO_SEARCH)
				return FileVisitResult.TERMINATE;
		}
		return FileVisitResult.CONTINUE;
	}
}
