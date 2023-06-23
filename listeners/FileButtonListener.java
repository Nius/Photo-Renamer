package listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;

import engine.Engine;

import org.lwjgl.*;

import static org.lwjgl.util.nfd.NativeFileDialog.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Defines the behavior of the HTML file button.
 * 
 * @author Nicholas Harrell
 */
public class FileButtonListener implements ActionListener
{
	/** The host engine instance. **/
	private final Engine ENGINE;
	
	/** The parent into which to spawn an open file dialog. **/
	@SuppressWarnings("unused") // Used in lightweight implementation.
	private final JFrame PARENT_FRAME;
	
	/**
	 * Creates a new listener with a reference to a configuration.
	 * 
	 * @param parentFrame Maps to {@link #PARENT_FRAME}.
	 * @param engine Maps to {@link #ENGINE}.
	 */
	public FileButtonListener(JFrame parentFrame, Engine engine)
	{
		super();
		ENGINE = engine;
		PARENT_FRAME = parentFrame;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{		
		Path browsePath = ENGINE.determineBestBrowsePath(true);
		
		// LIGHTWEIGHT IMPLEMENTATION
		// ==========================
		// Guaranteed to work on any platform, but does not use the
		// OS in-built file dialog so is less comfortable for the user.
		
		/*
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(browsePath.toFile());
		fileChooser.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Webpage, Single File ( .mhtml )","mhtml");
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setMultiSelectionEnabled(false);
		
		int result = fileChooser.showOpenDialog(PARENT_FRAME);
		
		if(result == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = fileChooser.getSelectedFile();
			ENGINE.CONFIG.inputHTML = selectedFile.toPath();
			PATH_LABEL.setText(selectedFile.toString());
			
			if(ENGINE.CONFIG.autoUseHTMLDir)
			{
				try
				{
					Path parentPath = selectedFile.toPath().getParent();
					ENGINE.CONFIG.setOutputDirectory(parentPath);
					DIR_LABEL.setText(parentPath.toString());
				}
				catch(NotDirectoryException e1)
				{
					// Do nothing. It is impossible for a valid file to not have a parent directory.
				}
			}
			
			ENGINE.processHTML();
		}
		*/
		
		// OS IN-BUILT DIALOG IMPLEMENTATION
		// =================================
		
		PointerBuffer outPath = memAllocPointer(1);
        try
        {
            checkResult
            (
                NFD_OpenDialog("mhtml", browsePath.toString(), outPath),
                outPath
            );
        }
        finally
        {
            memFree(outPath);
        }
	}
	
	/**
	 * Retrieve and act upon the results of the file open dialog.
	 * 
	 * @param result	The result returned by {@link #NFD_OpenDialog}.
	 * @param path		The selected path from {@link #NFD_OpenDialog}.
	 */
	private void checkResult(int result, PointerBuffer path)
	{
        switch (result)
        {
            case NFD_OKAY:
            	Path selectedFile = Paths.get(path.getStringUTF8(0));
    			ENGINE.updateInputFile(selectedFile);
                nNFD_Free(path.get(0));
                break;
            case NFD_CANCEL:
                break;
            default: // NFD_ERROR
        }
    }
}
