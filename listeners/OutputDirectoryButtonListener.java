package listeners;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjgl.PointerBuffer;

import engine.Engine;

import static org.lwjgl.util.nfd.NativeFileDialog.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Defines the behavior of the output directory button.
 * 
 * @author Nicholas Harrell
 */
public class OutputDirectoryButtonListener implements ActionListener
{
	/** The host engine instance. **/
	private final Engine ENGINE;
	
	/** The parent into which to spawn an open file dialog. **/
	@SuppressWarnings("unused") // Used in lightweight implementation.
	private final Container PARENT_PANEL;
	
	/**
	 * Creates a new listener with a reference to a configuration.
	 * 
	 * @param parentPanel Maps to {@link #PARENT_PANEL}.
	 * @param engine Maps to {@link #ENGINE}.
	 */
	public OutputDirectoryButtonListener(Container parentPanel, Engine engine)
	{
		super();
		ENGINE = engine;
		PARENT_PANEL = parentPanel;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Path browsePath = ENGINE.determineBestBrowsePath(false);
		
		// LIGHTWEIGHT IMPLEMENTATION
		// ==========================
		// Guaranteed to work on any platform, but does not use the
		// OS in-built file dialog so is less comfortable for the user.
		
		/*
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(browsePath.toFile());
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setMultiSelectionEnabled(false);
		
		int result = fileChooser.showOpenDialog(PARENT_PANEL);
		
		if(result == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				File selectedFile = fileChooser.getSelectedFile();
				CONFIG.setOutputDirectory(selectedFile.toPath());
				PATH_LABEL.setText(selectedFile.toString());
				OS_MAX_FIELD.setText(CONFIG.getOsMaxFileLength() + "");
			}
			catch(NotDirectoryException e1)
			{
				// Nothing needed here; the parameters of the JFileChooser
				// guarantee that the Path points to a directory.
			}
		}
		*/
		
		PointerBuffer outPath = memAllocPointer(1);
        try
        {
            checkResult
            (
                NFD_PickFolder(browsePath.toString(), outPath),
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
            	ENGINE.updateOutputDirectory(selectedFile);
                nNFD_Free(path.get(0));
                break;
            case NFD_CANCEL:
                break;
            default: // NFD_ERROR
        }
    }
}
