package constructs;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Provides functionality for extracting photos from .MHTML files.
 * 
 * @author Nicholas Harrell
 */
public class HTMLProcessor
{	
	/**
	 * Process the HTML data in the file at the specified path.
	 * 
	 * @param filePath The path at which to parse the file. 
	 * @return A collection of photos extracted from the HTML.
	 * @throws HTMLParseException An exception is thrown if parsing fails for any reason.
	 */
	public static ArrayList<Photo> processHTML(Path filePath) throws HTMLParseException
	{	
		if(!Files.exists(filePath) || !Files.isReadable(filePath))
			throw new HTMLParseException("Error: HTML file no longer exists or is not readable.");
		
		final ArrayList<Photo> PHOTOS = new ArrayList<Photo>();
		
		// Read the HTML file.
		Document source;
		try
		{
			/*  LOADING DASH HTML
			 *  =================
			 *  
			 *  Dash pages use quoted-printable attributes, which suffix all
			 *  equals symbols with "3D". Jsoup can't read this properly, so
			 *  the HTML needs to be treated before parsing.
			 *  
			 *  A single String has a maximum length of 2,147,483,647.
			 *  A test file representing 39 photos had a length of 3,323,596.
			 *  A single photo takes less than 3323596/39 to represent, but
			 *  even if it took as much as 3323596/39 we can still process
			 *  25,198 photos in one file before we have hit the String length
			 *  limit.
			 *  Whatever tech creates a photo album with 25 thousand photos
			 *  is in the wrong business.
			 */
			
			// Read HTML from the file.
			String html = Files.readString(filePath);
			
			// Strip newlines that will confuse Jsoup and mess up URL keys.
			html = html.replaceAll("=\\r?\\n","");
			
			// Strip attr=3D"value" notations down to attr="value"
			html = html.replace("3D\"","\"");
			
			// Strip values inside attributes (such as URL keys)
			html = html.replace("=3D","=");
			
			/* PARSING DASH HTML
			 * =================
			 * 
			 * Each album photo is represented by, on average, 33 lines of HTML. A sample
			 * DASH page representing 36 photos had over 60,000 lines. Very little of that
			 * is necessary, so a regex pattern is used to extract the relevant chunk of
			 * HTML.
			 * 
			 * The anatomy of the pattern is as follows:
			 * 
			 * <div class="RadAjaxPanel".*DivPhotosPanel" style="display: block;">
			 * This is the definition of the div containing all of the relevant photos on the page.
			 * 
			 * ([\S\s]*
			 * This searches for any text, on any number of lines, that occurs before the final
			 * instance of the next token. Note that a capture group has started.
			 * 
			 * DateUploaded">.*</div>)
			 * This finds the last occurrence of "DateUploaded" and continues to the end of the
			 * parent div, but only on the same line. Note that the capture group has ended.
			 * 
			 * In summary, this searches for everything between:
			 * - The declaration of the photo album div
			 * - The end of the line on which is the last occurrence of "DateUploaded".
			 */
			final String MATCH_PATTERN = "<div class=\\\"RadAjaxPanel\\\".*DivPhotosPanel\\\" style=\\\"display: block;\\\">([\\S\\s]*DateUploaded\\\">.*</div>)";
			Pattern photoMatchPattern = Pattern.compile(MATCH_PATTERN);
			Matcher photoMatcher = photoMatchPattern.matcher(html);
			if(!photoMatcher.find())
				throw new MatchNotFoundException();
			html = photoMatcher.group(1);
			
			// Jsoup handles the actual parsing of the HTML.
			source = Jsoup.parse(html);
		}
		catch (MatchNotFoundException e)
		{
			throw new HTMLParseException("The HTML file was opened, but the structure of the code within was unrecognizable. Has DASH changed the format of its webpages?");
		}
		catch (IOException e)
		{
			throw new HTMLParseException("Error reading HTML file.");
		}
		
		Elements photos = source.select(".album_photo");
		for(Element photo : photos)
		{
			// The photos are elements of type "img", but their addresses point to
			// thumbnails. The parent elements of type "a" have references to the
			// full-size photos.
			Element a = photo.parent();
			String src = a.attr("href").replace("&amp;","&");
			URL srcURL;
			try
			{
				srcURL = new URL(src);
			}
			catch(MalformedURLException e)
			{
				// Don't throw an exception here because we don't want to stop execution for one bad photo.
				// Commented Engine#addOutput call is a holdover from previous architecture; it would be appropriate to provide
				// non-stopping output for this but current architecture does not permit that.
				//addOutput("Invalid photo URL detected: \"" + src + "\". Did DASH change the structure of its webpages?");
				continue;
			}
			
			// The description and date data we want is held in escaped HTML stored
			// as an attribute of the "a" element. We'll need to extract and parse
			// the contents of the attribute as HTML.
			String photoData = a.attr("data-title").replace("&amp;","&");
			Document subHTML = Jsoup.parse(photoData);
			
			// Get a span whose ID contains "description". Each photo's description
			// span ID is prefixed with a unique string of numbers, so we can't
			// search by the whole ID.
			Elements ids = subHTML.select("span[id*=description]");
			if(ids.size() == 0)
				continue;
			String description = ids.get(0).html();
			
			// Date uploaded works the same way as the description.
			Elements dates = subHTML.select("span[id*=DateUploaded]");
			if(dates.size() == 0)
				continue;
			String date = dates.get(0).html();
			
			PHOTOS.add(new Photo(srcURL,description,date));
		}
		
		return PHOTOS;
	}
	
	/**
	 * A simple vehicle for providing negative feedback from a failed HTML parse operation.
	 * 
	 * @author Nicholas Harrell
	 */
	@SuppressWarnings("serial")
	public static class HTMLParseException extends Exception
	{
		/** The reason this exception was thrown. **/
		public final String MESSAGE;
		
		/**
		 * @param message Maps to {@link #MESSAGE}.
		 */
		public HTMLParseException(String message)
		{
			MESSAGE = message;
		}
	}
}
