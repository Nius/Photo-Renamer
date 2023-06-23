package constructs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import constructs.ConfigurationManager.Config;
import constructs.InputFile.FileStatus;
import constructs.Photo.PhotoStatus;
import engine.Engine;

/**
 * The PhotoDownloader handles downloading and writing of photo files given a
 * collection of input files from which to read photo URLs. This class also
 * handles cleanup after downloading, such as removal of source and temporary
 * files.
 * 
 * @author Nicholas Harrell
 */
public class PhotoDownloader
{
	/**
	 * Executes the download of photos and deletion of temporary files.
	 * Also deletes the source HTML file if configuration allows. 
	 *
	 * @param inputFiles	A reference to the collection of input files, to be
	 * 						used in conjunction with <code>fileIndex</code> to
	 * 						operate on a single file. The entire collection is
	 * 						required so that the collection can be notified when
	 * 						the file's status is updated after downloading, via
	 * 						{@link InputFileCollection#updateFileStatus(int, FileStatus)}.
	 * @param fileIndex		The specific file from the filesTable from which to
	 * 						write photos.
	 * @param outputDir		The path at which to write the photos, or null if
	 * 						the default path (the input file's location) should
	 * 						be used.
	 * @param engine		The central program instance, for the purpose of
	 * 						sending output to the CLI via
	 * 						{@link Engine#addOutput(String)}.
	 * 
	 * @return How many photos were successfully written.
	 */
	public static synchronized int downloadPhotos(InputFileCollection inputFiles, int fileIndex, Path outputDir, Engine engine)
	{
		// If the output directory is not specified, use the same directory as the input file.
		if(outputDir == null)
			outputDir = inputFiles.getInputFilePath(fileIndex).getParent();
		
		// Final verification that the output directory still exists.
		if(outputDir == null || !Files.exists(outputDir) || !Files.isWritable(outputDir))
		{
			engine.addOutput
			(
				"Could not copy photos: the destination folder no longer exists or is no longer writable: "
				+ outputDir.toString()
			);
			return 0;
		}
		
		final Path INPUT_PATH = inputFiles.getInputFilePath(fileIndex);
		final PhotoCollection PHOTOS_MODEL = inputFiles.getInputFilePhotoCollection(fileIndex);
		final Config CONFIG = engine.getConfiguration();
		
		// Clear statuses on all photos: they are about to become either saved or errored.
		PHOTOS_MODEL.clearAllStatuses();
		
		//
		// PROCESS PHOTOS
		//
		
		// Under normal circumstances, the input file will be deleted after execution if the configuration option says
		// to do so. If any error occurs, this boolean will become true and prevent the source file from being deleted.
		boolean preventDeletion = false;
		int written = 0;
		for(int i = 0; i < PHOTOS_MODEL.getSize(); i ++)
		{
			// Where to write the file.
			Path targetAddress = Paths.get(
				outputDir + "/" + PHOTOS_MODEL.getPhotoDescription(i) + ".jpg");
			
			// The exepectation is that we're creating new files, so if there is a file in the way then either something
			// is wrong or this is a duplicate or follow-up execution and we shouldn't be overwriting anyway.
			if(Files.exists(targetAddress))
			{
				engine.addOutput("Could not copy to " + targetAddress.toString() + " because a file already exists at that address.");
				PHOTOS_MODEL.setPhotoStatus(i,PhotoStatus.ERROR_SEVERE);
				preventDeletion = true;
				continue;
			}
			
			// Execute file write.
			try
			{
				// Prepare to read from URL.
				ReadableByteChannel in = Channels.newChannel(PHOTOS_MODEL.getPhotoURL(i).openStream());
				
				// Prepare to write to file.
				FileOutputStream out = new FileOutputStream(targetAddress.toString());
				
				// Execute write.
				out.getChannel().transferFrom(in,0,Long.MAX_VALUE);
				
				// A memory leak happens if the stream isn't closed.
				out.close();
				
				// Nothing has gone wrong, so mark this photo as a success!
				PHOTOS_MODEL.setPhotoStatus(i,PhotoStatus.SAVED);
				written ++;
				
				// Change the photo's Created, Modified, and Accessed dates to match the upload date from DASH.
				if(CONFIG.MODIFY_DATE)
				{
					// Convert the String date from Dash to a FileTime usable by the OS.
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/uuuu h:m:s a");
					LocalDateTime locdata = LocalDateTime.parse(PHOTOS_MODEL.getPhotoDate(i),formatter);
					Instant inst = locdata.toInstant(ZoneOffset.UTC);
				    FileTime time = FileTime.from(inst);
				    
				    // Get and rewrite the file's dates.
					BasicFileAttributeView attributes = Files.getFileAttributeView(targetAddress, BasicFileAttributeView.class);
				    attributes.setTimes(time, time, time);
				}
			}
			catch (IllegalArgumentException e)
			{
				// Error during date operations.
				engine.addOutput("Could not modify date for " + targetAddress.toString() + " because the date is mis-formatted.");
				PHOTOS_MODEL.setPhotoStatus(i,PhotoStatus.ERROR_MINOR);
				preventDeletion = true;
			}
			catch (IOException e)
			{
				// Error during read or write operations.
				engine.addOutput("Could not copy to " + targetAddress.toString() + " because of a file system error.");
				PHOTOS_MODEL.setPhotoStatus(i,PhotoStatus.ERROR_SEVERE);
				preventDeletion = true;
			}
			catch (Exception e)
			{
				// Error during read or write operations.
				engine.addOutput("Could not copy to " + targetAddress.toString() + " because of an unknown error.");
				PHOTOS_MODEL.setPhotoStatus(i,PhotoStatus.ERROR_SEVERE);
				preventDeletion = true;
			}
			
			// Notify the engine of the current count of photos that have been
			// successfully downloaded, including this one.
			engine.updateExecuteCount(i + 1,PHOTOS_MODEL.getSize());
			
			// Redraw the photos table immediately to show the new status of the
			// photo that was just downloaded (or failed).
			engine.redrawPhotosTable(i);
		}
		
		// Write statistics to file.
		try
		{
			Path statsPath = Paths.get(System.getProperty("user.dir") + "/statistics.txt");
			List<String> statsLines = Collections.emptyList();
			
			// Load existing stats, if available.
			if(Files.exists(statsPath))
				statsLines = Files.readAllLines(statsPath);
			
			statsLines = new ArrayList<String>(statsLines);
			
			int statsTotalJobs = 1;
			int statsTotalPhotos = written;
			
			// Add up previous jobs.
			// Skip the first line, because it's always a summary.
			for(int i = 1; i < statsLines.size(); i++)
			{
				// Format is PhotoCount,Date,IndexName[,user]
				// Jobs executed before version 1.2.2 did not list the user that executed them.
				String[] splat = statsLines.get(i).split(",");
				if(splat.length < 3)
					continue;
				
				try
				{
					statsTotalPhotos += Integer.valueOf(splat[1]);
					statsTotalJobs ++;
				}
				catch (NumberFormatException e2) { continue; }
			}
			
			// Update the summary
			String summaryString = "Total: " + statsTotalPhotos + " photos across " + statsTotalJobs + " executions."; 
			if(statsLines.size() > 0)
				statsLines.set(0,summaryString);
			else
				statsLines.add(summaryString);
			
			// Add the current job to the end of the list
			statsLines.add
			(
				System.currentTimeMillis() + "," +
				written + "," +
				INPUT_PATH.getFileName().toString().replaceAll(".mhtml$","") + "," +
				System.getProperty("user.name")
			);
			
			// Write to file.
			Files.write(statsPath,statsLines,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
		}
		catch (IOException e1)
		{
			engine.addOutput("Failed to write statistics to file.");
		}
		
		boolean hasErrors = preventDeletion;
		
		// Temporary & source file deletion
		try
		{
			/*
				Delete all files inside the temporary file directory.
				For safety reasons, if any sub-directory is found inside the temporary file
				directory, nothing will be deleted at all. 
			 */
			Stream<Path> entries = Files.list(CONFIG.TEMP_FILE_DIR);
			Iterator<Path> i = entries.iterator();
			while(i.hasNext())
			{
				Path entry = i.next();
				if(Files.isDirectory(entry))
				{
					entries.close();
					return written;
				}
			}
			entries.close();
			Files.walkFileTree(CONFIG.TEMP_FILE_DIR,new FileDeleter(engine));
			
			// If the file should be deleted and there were no errors...
			if(CONFIG.DELETE_HTML && !preventDeletion)
				Files.delete(INPUT_PATH);
			
			// If the file should be deleted but there were errors...
			else if(CONFIG.DELETE_HTML)
				engine.addOutput("Source file not deleted because there were errors during download.");
		}
		catch (IOException e1)
		{
			engine.addOutput("Failed to delete input files.");
			hasErrors = true;
		}
		
		inputFiles.updateFileStatus
		(
				fileIndex,
				hasErrors ? FileStatus.ERROR_SEVERE : FileStatus.SAVED
		);
		
		return written;
	}
	
	/**
	 * Deletes files upon visit.
	 * 
	 * @author Nicholas Harrell
	 */
	private static class FileDeleter extends SimpleFileVisitor<Path>
	{
		/** The engine to call back to in case of an error. **/
		private final Engine ENGINE;
		
		/**
		 * Creates a new file deleter capable of sending error messages
		 * back to the attached Engine.
		 * 
		 * @param engine	Maps to {@link #ENGINE}.
		 */
		public FileDeleter(Engine engine)
		{
			ENGINE = engine;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr)
		{
			if(attr.isDirectory())
			{
				/* Last Line of Defense
				 * ====================
				 * 
				 * If, for whatever reason,
				 * the attempt to pre-emptively detect subfolders in the doomed
				 * directory fails and the process of walking the file tree
				 * encounters a subdirectory, terminate the entire operation
				 * instead of deleting the encountered subdirectory.
				 * 
				 * Note that any files deleted before this directory was
				 * encountered will remain deleted.
				 */
				return FileVisitResult.TERMINATE;
			}
			
			try
			{
				Files.delete(file);
			}
			catch (IOException e)
			{
				ENGINE.addOutput("Failed to delete file \"" + file.toString() + "\".");
			}
			
			return FileVisitResult.CONTINUE;
		}
	}
}
