package engine;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.NumberFormatter;

import constructs.ConfigurationManager;
import constructs.ConfigurationManager.Config;
import constructs.ConfigurationManager.OverlengthBehavior;
import constructs.ConfigurationManager.ReplacementCharacter;
import constructs.DescriptionProcessor;
import constructs.FileSearcher;
import constructs.HTMLProcessor.HTMLParseException;
import constructs.InputFile;
import constructs.InputFileCollection;
import constructs.ListenerSuspendableJTable;
import constructs.ListenerSuspendableTextField;
import constructs.Photo;
import constructs.PhotoCollection;
import constructs.PhotoDownloader;
import listeners.OutputDirectoryButtonListener;
import listeners.DirectoryMatchCheckboxListener;
import listeners.ExecuteButtonListener;
import listeners.FileButtonListener;
import listeners.HelpButtonListener;
import listeners.HelpButtonListener.HelpMode;
import listeners.InputDirectoryButtonListener;
import listeners.NameOptionCheckboxListener;
import listeners.NameOptionCheckboxListener.CBLMode;
import listeners.NameOptionRadioListener;
import listeners.NameOptionRadioListener.RLMode;
import listeners.NameOptionTextListener;
import listeners.NameOptionTextListener.TLMode;
import listeners.TableMouseListener;

/** Main instance for the PhotoRenamer program. **/ 
public class Engine
{
	/** Release version. **/
	public static final String VERSION = "2.0.1";
	
	//
	// Operational Objects
	//
	
	/**
	 * The Configuration Manager handles reading, manipulating, and writing of configuration data, including the default
	 * configuration file. Most configuration options are public members, but several require controlled modification and
	 * are therefore only accessible via getter and setter methods.
	 */
	private final ConfigurationManager CONFIG;
	
	/**
	 * This object defines and manages behavior for the photos table. It also manages storage of the photos table's
	 * data. Because all of the data about {@link Photo Photos} being manipulated has to be stored here, it would
	 * be redundant and would present synchronization issues if it was also stored elsewhere. Therefore this
	 * object is global, and the storage of all loaded {@link Photo} objects can be accessed via {@link PhotoCollection}.
	 */
	public PhotoCollection PHOTOS_MODEL;
	
	/**
	 * This object defines and manages behavior for the source files table. It also manages storage of the source files table's
	 * data. Because all of the data about input files being manipulated has to be stored here, it would
	 * be redundant and would present synchronization issues if it was also stored elsewhere. Therefore this
	 * object is global, and the storage of all loaded input files can be accessed via {@link InputFileCollection}.
	 */
	public InputFileCollection INPUTS_MODEL;
	
	//
	// UI Components
	//
	
	/** The exeucte button. Must be global because it needs to be passed to the constructor for {@link #PHOTOS_MODEL}. **/
	private final JButton executeB = new JButton("Execute");
	
	/** The button for choosing the output directory. **/
	private final JButton DIR_BUTTON = new JButton("Browse");
	
	/** The top-level frame for this application. **/
	private final JFrame frame = new JFrame("Photo Renamer");
	
	/** The label that shows the currently selected input file. */
	private final JLabel htmlLabel = new JLabel("");
	
	/** Inputs table. **/
	private final ListenerSuspendableJTable INPUTS_TABLE = new ListenerSuspendableJTable(this);
	
	/** OS-Defined Filename Length Limit **/
	private final JTextField osLimitT = new JTextField();
	
	/** Output text area. Must be global because it is referenced in {@link #addOutput(String, boolean)}. **/
	private final JTextArea OUTPUT = new JTextArea();
	
	/** Label displaying the current output directory. **/
	private final JLabel OUTPUT_DIR_LABEL = new JLabel();
	
	/** The scroll pane for the output box. Must be global because its scroll position is modified in {@link #addOutput(String, boolean)}. **/
	private final JScrollPane outputP = new JScrollPane();
	
	/** Photos table. **/
	private final JTable PHOTOS_TABLE = new JTable();
	
	/** Prefix text area. **/
	private ListenerSuspendableTextField prefixT;
	
	/** Suffix text area. **/
	private ListenerSuspendableTextField suffixT;
	
	/** Unnamed text area. **/
	private ListenerSuspendableTextField unnamedT;
	
	/**
	 * Initializes the Photo Renamer program by performing these steps:
	 * 
	 * <ol>
	 * 	<li> Writes copies of the help and about files to the current working directory
	 * 		 via {@link #writeAssets()}.
	 *    
	 * 	<li> Reads configuration data via {@link ConfigurationManager#readConfig()}.
	 *       Note that this may result in the writing of a default configuration file.
	 *    
	 * 	<li> Instantiates the {@link #PHOTOS_TABLE} with a new instance of {@link PhotoCollection}.
	 * </ol>
	 */
	public Engine()
	{
		// Write assets to the working directory.
		writeAssets();
		
		// Load configuration data.
		CONFIG = ConfigurationManager.readConfig();
	}
	
	/**
	 * Sets up and spawns the Photo Renamer interface by following these steps:
	 * 
	 * <ol>
	 * 	<li> Creates the program window via a {@link JFrame}.
	 * 	<li> Populates controls to the parent window via {@link #addControls(Container, JFrame)}.
	 * 	<li> Instructs the program window to show itself.
	 *  <li> Performs some UI tweaks, such as modifying tooltip delay time.
	 *  <li> Adds introductory output via {@link #addOutput(String)}.
	 *  <li> Sends any output generated by {@link ConfigurationManager#readConfig()} to {@link #addOutput(String)}.
	 *  <li> Attempts to process all loaded HTML files, if they exist.
	 * </ol>
	 */
	public void run()
	{
		// Create and set up the window.
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) { /* Do nothing. */ }
 
		// Set up the content pane.
		JPanel innerMain = new JPanel();
		innerMain.setBorder(new EmptyBorder(10,10,10,10));
		addControls(innerMain,frame);
		frame.getContentPane().add(innerMain);
		
		// Display the window.
		frame.pack();
		frame.setVisible(true);
		frame.setState(JFrame.MAXIMIZED_BOTH);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		// Modify tooltip delays.
		ToolTipManager.sharedInstance().setInitialDelay(0);
		ToolTipManager.sharedInstance().setDismissDelay(10000);
		
		// Say hi!
		addOutput("Photo Renamer version " + VERSION + ", by Nicholas Harrell.");
		addOutput("Copyright © 2022 by Duraclean Restoration Services in Waldorf, Maryland");
		addOutput("Licensed for use by Duraclean employees only.");
		addOutput("");
		
		// Show configuration load messages.
		for(String line : CONFIG.getLoadMessages())
			addOutput(line);
		
		// Redraw to iron out some visual glitches.
		frame.repaint();
		
		// Get all input files in the currently specified input directory.
		updateInputDirectory(CONFIG.getInputDirectory());
	}
	

	/**
	 * Populate controls into the window and add their listeners.
	 * 
	 * @param pane The main pane on which to put controls.
	 * @param top_level_frame The top-level frame for this program instance.
	 */
	private void addControls(Container pane, JFrame top_level_frame)
	{
		Config CFG = CONFIG.getConfig();
		
		pane.setLayout(new GridBagLayout());
		pane.setBackground(CONFIG.BG_COLOR);
		
		// A new instance of this object will be created for each
		// control, but it's easiest to reuse the variable name
		// instead of coming up with a new one each time.
		GridBagConstraints cstr;
	 
		//
		//	HTML file selection
		//
		
		// Bordered panel for HTML file selection.
		// HTML file-related controls should be added to this panel.
		JPanel htmlPanel = new JPanel(new GridBagLayout());
		htmlPanel.setBackground(CONFIG.BG_COLOR);
		htmlPanel.setForeground(CONFIG.TXT_COLOR);
		htmlPanel.setBorder(BorderFactory.createTitledBorder
		(
			new LineBorder(CONFIG.LINE_COLOR),
			"Input File(s)",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			new JLabel().getFont(),
			CONFIG.TXT_COLOR
		));
		
		// HTML browse Button
		JButton htmlB = new JButton("Single File");
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 0;
		cstr.insets = new Insets(0,5,5,0);
		cstr.anchor = GridBagConstraints.FIRST_LINE_START;
		htmlPanel.add(htmlB,cstr);
		
		// Directory (for recursion) browse Button
		JButton recursiveB = new JButton("Whole Directory");
		cstr = new GridBagConstraints();
		cstr.gridx = 1;
		cstr.gridy = 0;
		cstr.insets = new Insets(0,5,5,0);
		cstr.anchor = GridBagConstraints.FIRST_LINE_START;
		htmlPanel.add(recursiveB,cstr);
		
		// Input location Label
		String labelText = CONFIG.getInputDirectory() == null ? "" : CONFIG.getInputDirectory().toString();
		htmlLabel.setText(labelText);
		htmlLabel.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 2;
		cstr.gridy = 0;
		cstr.weightx = 1;
		cstr.insets = new Insets(0,5,5,5);
		cstr.anchor = GridBagConstraints.FIRST_LINE_START;
		cstr.fill = GridBagConstraints.BOTH;
		htmlPanel.add(htmlLabel,cstr);
		
		// Add html pane to main view at (0,0) with span (1,1).
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		pane.add(htmlPanel,cstr);
		
		//
		// Output Directory Selection
		//
		
		// Bordered panel for output directory selection.
		// Directory-related controls should be added to this panel.
		JPanel directoryPanel = new JPanel(new GridBagLayout());
		directoryPanel.setBackground(CONFIG.BG_COLOR);
		directoryPanel.setForeground(CONFIG.TXT_COLOR);
		directoryPanel.setBorder(BorderFactory.createTitledBorder
		(
			new LineBorder(CONFIG.LINE_COLOR),
			"Output Directory",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			new JLabel().getFont(),
			CONFIG.TXT_COLOR
		));
		
		// Directory match CheckBox.
		JCheckBox dirMatchCB = new JCheckBox("Use HTML directory for output");
		dirMatchCB.setBackground(CONFIG.BG_COLOR);
		dirMatchCB.setForeground(CONFIG.TXT_COLOR);
		dirMatchCB.setSelected(CFG.AUTO_USE_HTML_DIR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 0;
		cstr.anchor = GridBagConstraints.FIRST_LINE_START;
		cstr.gridwidth = 99;
		directoryPanel.add(dirMatchCB,cstr);
		
		// Directory browse Button.
		DIR_BUTTON.setEnabled(!CFG.AUTO_USE_HTML_DIR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 1;
		cstr.insets = new Insets(0,5,5,0);
		cstr.anchor = GridBagConstraints.FIRST_LINE_START;
		directoryPanel.add(DIR_BUTTON,cstr);
		
		// Directory Label.
		labelText = CONFIG.getOutputDirectory() == null ? "" : CONFIG.getOutputDirectory().toString();
		OUTPUT_DIR_LABEL.setText(labelText);
		OUTPUT_DIR_LABEL.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 1;
		cstr.gridy = 1;
		cstr.weightx = 1;
		cstr.insets = new Insets(0,5,5,5);
		cstr.anchor = GridBagConstraints.FIRST_LINE_START;
		cstr.fill = GridBagConstraints.BOTH;
		directoryPanel.add(OUTPUT_DIR_LABEL,cstr);
		
		// Add directory pane to main view at position (0,1) with span (1,1).
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 1;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		pane.add(directoryPanel,cstr);
		
		//
		// Input Files Table
		//
		
		INPUTS_TABLE.setFillsViewportHeight(true);
		INPUTS_TABLE.setBackground(CONFIG.TABLE_BG_COLOR);
		INPUTS_TABLE.setForeground(CONFIG.TXT_COLOR);
		INPUTS_TABLE.setIntercellSpacing(new Dimension(5,0));
		INPUTS_TABLE.setRowHeight(20);
		INPUTS_TABLE.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		INPUTS_TABLE.setRowSelectionAllowed(true);
		
		// Add the inputs table to the main panel at position (1,0) with span (2,2).
		JScrollPane tableScroll = new JScrollPane(INPUTS_TABLE);
		cstr = new GridBagConstraints();
		cstr.gridx = 1;
		cstr.gridy = 0;
		cstr.weighty = 0.5;
		cstr.weightx = 1;
		cstr.fill = GridBagConstraints.BOTH;
		cstr.gridheight = 2;
		cstr.gridwidth = 2;
		pane.add(tableScroll,cstr);
		
		//
		// Photos Table
		//
		
		PHOTOS_TABLE.setFillsViewportHeight(true);
		PHOTOS_TABLE.setBackground(CONFIG.TABLE_BG_COLOR);
		PHOTOS_TABLE.setForeground(CONFIG.TXT_COLOR);
		PHOTOS_TABLE.setIntercellSpacing(new Dimension(5,0));
		PHOTOS_TABLE.setRowHeight(20);
		PHOTOS_TABLE.setFocusable(false);
		PHOTOS_TABLE.setRowSelectionAllowed(false);
		PHOTOS_TABLE.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		
		// Add the photos table to the main panel at position (1,2) with span (2,1).
		tableScroll = new JScrollPane(PHOTOS_TABLE);
		cstr = new GridBagConstraints();
		cstr.gridx = 1;
		cstr.gridy = 2;
		cstr.weighty = 0.5;
		cstr.weightx = 1;
		cstr.fill = GridBagConstraints.BOTH;
		cstr.gridheight = 1;
		cstr.gridwidth = 2;
		pane.add(tableScroll,cstr);
		
		//
		// Options
		//
		
		// Bordered panel for program options.
		// Option-related controls and sub-panels should be added to this panel.
		JPanel optsPanel = new JPanel(new GridBagLayout());
		optsPanel.setBackground(CONFIG.BG_COLOR);
		optsPanel.setForeground(CONFIG.TXT_COLOR);
		optsPanel.setBorder(BorderFactory.createTitledBorder
		(
			new LineBorder(CONFIG.LINE_COLOR),
			"Options",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			new JLabel().getFont(),
			CONFIG.TXT_COLOR
		));
		
		// All controls are added to this panel in a vertical arrangement.
		// To make code easier to manage (in cases where a control needs to
		// be added, removed, or relocated) make the Y-coordinate relative
		// to the controls that precede it in the code.
		int gridy = 0;
		
		// Remove trailing numbers checkbox.
		JCheckBox removeNumsCB = new JCheckBox("Remove trailing numbers from descriptions",CFG.REMOVE_TRAILING_NUMBERS);
		removeNumsCB.setToolTipText("If a description ends with a number (including numbers in parentheses) then the number will be removed before comparing the photo's description with others.");
		removeNumsCB.setBackground(CONFIG.BG_COLOR);
		removeNumsCB.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(removeNumsCB,cstr);
		
		// Append numbers to unique descriptions checkbox.
		JCheckBox indexUniqueCB = new JCheckBox("Append numbers to unique descriptions",CFG.INDEX_UNIQUE);
		indexUniqueCB.setToolTipText("If a photo's description is unique then it will still get an index number at the end.");
		indexUniqueCB.setBackground(CONFIG.BG_COLOR);
		indexUniqueCB.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(indexUniqueCB,cstr);
		
		// Correct capitalization checkbox.
		JCheckBox capsCB = new JCheckBox("Correct capitalization in descriptions",CFG.CORRECT_CAPS);
		capsCB.setToolTipText("The photo description will be capitalized like a book title.");
		capsCB.setBackground(CONFIG.BG_COLOR);
		capsCB.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(capsCB,cstr);
		
		// Label for the filename prefix text field.
		JLabel prefixL = new JLabel("Filename Prefix");
		prefixL.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.insets = new Insets(10,0,0,0);
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(prefixL,cstr);
		
		// Text field for a user-supplied prefix.
		prefixT = new ListenerSuspendableTextField(CFG.PREFIX);
		prefixT.setBackground(Color.BLACK);
		prefixT.setForeground(CONFIG.TXT_COLOR);
		prefixT.setCaretColor(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(prefixT,cstr);
		
		// Label for the filename suffix text field.
		JLabel suffixL = new JLabel("Filename Suffix");
		suffixL.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.insets = new Insets(10,0,0,0);
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(suffixL,cstr);
		
		// Text field for a user-supplied suffix.
		suffixT = new ListenerSuspendableTextField(CFG.SUFFIX);
		suffixT.setBackground(Color.BLACK);
		suffixT.setForeground(CONFIG.TXT_COLOR);
		suffixT.setCaretColor(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(suffixT,cstr);
		
		// Label for the undescribed name text field.
		JLabel unnamedL = new JLabel("Label for Undescribed Photos");
		unnamedL.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.insets = new Insets(10,0,0,0);
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(unnamedL,cstr);
		
		// Text field for the name to be applied to undescribed photos.
		unnamedT = new ListenerSuspendableTextField(CFG.UNDESCRIBED);
		unnamedT.setBackground(Color.BLACK);
		unnamedT.setForeground(CONFIG.TXT_COLOR);
		unnamedT.setCaretColor(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(unnamedT,cstr);
		
		// Label for the user-defined maximum file length number box.
		JLabel maxLenL = new JLabel("Maximum Filename Length");
		maxLenL.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy; // Next control is on the same row; no ++
		cstr.weightx = 0.5;
		cstr.insets = new Insets(10,0,0,0);
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(maxLenL,cstr);
		
		// Label for the OS-defined maximum file length number box.
		JLabel osLenL = new JLabel("Length Limit from OS");
		osLenL.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 1;
		cstr.gridy = gridy++;
		cstr.weightx = 0.5;
		cstr.insets = new Insets(10,10,0,0);
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(osLenL,cstr);
		
		// Text field for setting the user-defined maximum description length.
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		NumberFormatter intF = new NumberFormatter(intFormat);
		intF.setAllowsInvalid(true);
		intF.setMinimum(ConfigurationManager.MINIMUM_PATH);
		intF.setMaximum(CONFIG.getOsMaxFileLength());
		JFormattedTextField maxLenT = new JFormattedTextField(intF);
		maxLenT.setBackground(Color.BLACK);
		maxLenT.setForeground(CONFIG.TXT_COLOR);
		maxLenT.setValue(CFG.USER_MAX_FILE_LENGTH);
		maxLenT.setCaretColor(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy; // Next control is on the same row; no ++
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(maxLenT,cstr);
		
		// Text field for setting the OS-defined maximum description length.
		// This field cannot be edited by the user, so it doesen't need any
		// edit-related options or formatting constraints.
		osLimitT.setText("" + CONFIG.getOsMaxFileLength());
		osLimitT.setBackground(Color.BLACK);
		osLimitT.setForeground(CONFIG.TXT_COLOR);
		osLimitT.setEditable(false);
		osLimitT.setCaretColor(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 1;
		cstr.gridy = gridy++;
		cstr.weightx = 0.5;
		cstr.insets = new Insets(0,10,0,0);
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(osLimitT,cstr);
		
		//
		// OverLength Behavior Panel
		//
		
		// Bordered panel for over-length behavior options.
		// OLB-related controls should be added to this panel.
		JPanel overLengthPanel = new JPanel(new GridBagLayout());
		overLengthPanel.setBackground(CONFIG.BG_COLOR);
		overLengthPanel.setForeground(CONFIG.TXT_COLOR);
		overLengthPanel.setBorder(BorderFactory.createTitledBorder
		(
			new LineBorder(CONFIG.LINE_COLOR),
			"Over-Length Behavior",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			new JLabel().getFont(),
			CONFIG.TXT_COLOR
		));
		
		// Radio button for OverlengthBehavior.REFUSE
		JRadioButton refuseRB = new JRadioButton("Refuse");
		refuseRB.setBackground(CONFIG.BG_COLOR);
		refuseRB.setForeground(CONFIG.TXT_COLOR);
		if(CFG.OVER_LENGTH == OverlengthBehavior.REFUSE)
			refuseRB.setSelected(true);
		refuseRB.setToolTipText("If any file name is too long, an error will appear and no files will be renamed.");
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		overLengthPanel.add(refuseRB,cstr);
		
		// Radio button for OverlengthBehavior.WARN
		JRadioButton warnRB = new JRadioButton("Warn");
		warnRB.setBackground(CONFIG.BG_COLOR);
		warnRB.setForeground(CONFIG.TXT_COLOR);
		if(CFG.OVER_LENGTH == OverlengthBehavior.WARN)
			warnRB.setSelected(true);
		warnRB.setToolTipText("If any file name is too long, a warning will appear but files can still be renamed.");
		cstr = new GridBagConstraints();
		cstr.gridx = 1;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		overLengthPanel.add(warnRB,cstr);
		
		// Radio button for OverlengthBehavior.TRUNCATE
		JRadioButton truncateRB = new JRadioButton("Truncate");
		truncateRB.setBackground(CONFIG.BG_COLOR);
		truncateRB.setForeground(CONFIG.TXT_COLOR);
		if(CFG.OVER_LENGTH == OverlengthBehavior.TRUNCATE)
			truncateRB.setSelected(true);
		truncateRB.setToolTipText("If any file name is too long, the end of the filename will be forcibly shortened.");
		cstr = new GridBagConstraints();
		cstr.gridx = 2;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		overLengthPanel.add(truncateRB,cstr);
		
		// Radio button for OverlengthBehavior.DROP_VOWELS
		JRadioButton vowelRB = new JRadioButton("Drop Vowels");
		vowelRB.setBackground(CONFIG.BG_COLOR);
		vowelRB.setForeground(CONFIG.TXT_COLOR);
		if(CFG.OVER_LENGTH == OverlengthBehavior.DROP_VOWELS)
			vowelRB.setSelected(true);
		vowelRB.setToolTipText("If any file name is too long, all vowels will be removed and then the filename will be truncated if necessary.");
		cstr = new GridBagConstraints();
		cstr.gridx = 3;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		overLengthPanel.add(vowelRB,cstr);
		
		// Radio button for OverlengthBehavior.DO_NOTHING
		JRadioButton allowRB = new JRadioButton("Allow");
		allowRB.setBackground(CONFIG.BG_COLOR);
		allowRB.setForeground(CONFIG.TXT_COLOR);
		if(CFG.OVER_LENGTH == OverlengthBehavior.DO_NOTHING)
			allowRB.setSelected(true);
		allowRB.setToolTipText("Nothing happens if a filename is too long; it gets renamed anyway.");
		cstr = new GridBagConstraints();
		cstr.gridx = 4;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		overLengthPanel.add(allowRB,cstr);
		
		// Make the OLB radio buttons mutually exclusive.
		ButtonGroup olbGroup = new ButtonGroup();
		olbGroup.add(refuseRB);
		olbGroup.add(warnRB);
		olbGroup.add(truncateRB);
		olbGroup.add(vowelRB);
		olbGroup.add(allowRB);
		
		// Add the OLB controls to the main options panel.
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		cstr.insets = new Insets(5,0,0,0);
		optsPanel.add(overLengthPanel,cstr);
		
		//
		// Replacement Character Panel
		//
		
		// Bordered panel for replacement character options.
		// Replacement character-related controls should be added to this panel.
		JPanel charPanel = new JPanel(new GridBagLayout());
		charPanel.setBackground(CONFIG.BG_COLOR);
		charPanel.setForeground(CONFIG.TXT_COLOR);
		charPanel.setBorder(BorderFactory.createTitledBorder
		(
			new LineBorder(CONFIG.LINE_COLOR),
			"Invalid Character Replacement",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			new JLabel().getFont(),
			CONFIG.TXT_COLOR
		));
		
		// Radio button for ReplacementCharacter.HYPHEN
		JRadioButton hyphenRB = new JRadioButton("Hyphen (-)");
		hyphenRB.setBackground(CONFIG.BG_COLOR);
		hyphenRB.setForeground(CONFIG.TXT_COLOR);
		if(CFG.REPLACEMENT_CHAR == ReplacementCharacter.HYPHEN)
			hyphenRB.setSelected(true);
		hyphenRB.setToolTipText("Invalid characters, such as slashes, will be replaced with a hyphen.");
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		charPanel.add(hyphenRB,cstr);
		
		// Radio button for ReplacementCharacter.COMMA
		JRadioButton commaRB = new JRadioButton("Comma (,)");
		commaRB.setBackground(CONFIG.BG_COLOR);
		commaRB.setForeground(CONFIG.TXT_COLOR);
		if(CFG.REPLACEMENT_CHAR == ReplacementCharacter.COMMA)
			commaRB.setSelected(true);
		commaRB.setToolTipText("Invalid characters, such as slashes, will be replaced with a comma.");
		cstr = new GridBagConstraints();
		cstr.gridx = 1;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		charPanel.add(commaRB,cstr);
		
		// Radio button for ReplacementCharacter.NOTHING
		JRadioButton nothingRB = new JRadioButton("Nothing");
		nothingRB.setBackground(CONFIG.BG_COLOR);
		nothingRB.setForeground(CONFIG.TXT_COLOR);
		if(CFG.REPLACEMENT_CHAR == ReplacementCharacter.NOTHING)
			nothingRB.setSelected(true);
		nothingRB.setToolTipText("Invalid characters, such as slashes, will be removed and not replaced.");
		cstr = new GridBagConstraints();
		cstr.gridx = 2;
		cstr.gridy = 0;
		cstr.weightx = 0.5;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		charPanel.add(nothingRB,cstr);
		
		// Make the replacement character radio buttons mutually exclusive.
		ButtonGroup rcGroup = new ButtonGroup();
		rcGroup.add(hyphenRB);
		rcGroup.add(commaRB);
		rcGroup.add(nothingRB);
		
		// Add the replacement character controls to the main options panel.
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		cstr.insets = new Insets(5,0,0,0);
		optsPanel.add(charPanel,cstr);
		
		// Update file creation dates checkbox.
		JCheckBox dateCB = new JCheckBox("Change file creation date to match Dash",CFG.MODIFY_DATE);
		dateCB.setToolTipText("The creation date will be changed to the date of upload to Dash.");
		dateCB.setBackground(CONFIG.BG_COLOR);
		dateCB.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		cstr.insets = new Insets(10,0,0,0);
		optsPanel.add(dateCB,cstr);
		
		// Delete source HTML file checkbox.
		JCheckBox deleteCB = new JCheckBox("Delete source HTML file after downloading.",CFG.DELETE_HTML);
		deleteCB.setToolTipText("The source HTML file will be deleted after download, if there are no errors.");
		deleteCB.setBackground(CONFIG.BG_COLOR);
		deleteCB.setForeground(CONFIG.TXT_COLOR);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = gridy++;
		cstr.weightx = 1;
		cstr.gridwidth = 99;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		optsPanel.add(deleteCB,cstr);
		
		// Add options panel to main view at position (0,2) with span (1,1).
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 2;
		cstr.weightx = 0.5;
		cstr.weighty = 1;
		cstr.fill = GridBagConstraints.BOTH;
		pane.add(optsPanel,cstr);
		
		//
		// Bottom Panel
		//
		
		JPanel bottomPanel = new JPanel(new GridBagLayout());
		bottomPanel.setBackground(CONFIG.BG_COLOR);
		
		//
		// Output Panel
		//
		
		// Bordered panel for output.
		// Only the output text area should be added to this panel.
		JPanel outputWrapper = new JPanel(new GridBagLayout());
		outputWrapper.setBackground(CONFIG.BG_COLOR);
		outputWrapper.setForeground(CONFIG.TXT_COLOR);
		outputWrapper.setBorder(BorderFactory.createTitledBorder
		(
			new LineBorder(CONFIG.LINE_COLOR),
			"Output",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			new JLabel().getFont(),
			CONFIG.TXT_COLOR
		));
		
		// Add the output text area to the output scroll panel.
		OUTPUT.setBackground(Color.BLACK);
		OUTPUT.setForeground(CONFIG.TXT_COLOR);
		OUTPUT.setEditable(false);
		OUTPUT.setMargin(new Insets(0,5,0,0));
		outputP.setViewportView(OUTPUT);
		
		// Add the output scroll panel to the output panel.
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 3;
		cstr.weightx = 1;
		cstr.gridwidth = 2;
		cstr.weighty = 0.5;
		cstr.fill = GridBagConstraints.BOTH;
		cstr.insets = new Insets(0,5,5,5);
		outputWrapper.add(outputP,cstr);
		
		// Add the output panel to the bottom panel.
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 0;
		cstr.weightx = 0.75;
		cstr.fill = GridBagConstraints.BOTH;
		bottomPanel.add(outputWrapper,cstr);
		
		//
		// Button Panel
		//
		
		// The panel for the execute button and related controls.
		JPanel goPanel = new JPanel(new GridBagLayout());
		goPanel.setBackground(CONFIG.BG_COLOR);
		
		// The button that opens the About file.
		JButton aboutB = new JButton("About");
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 0;
		cstr.weightx = 1;
		cstr.anchor = GridBagConstraints.FIRST_LINE_END;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		goPanel.add(aboutB,cstr);
		
		// The button that opens the Help file.
		JButton helpB = new JButton("Help");
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 1;
		cstr.weightx = 1;
		cstr.insets = new Insets(5,0,0,0);
		cstr.anchor = GridBagConstraints.FIRST_LINE_END;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		goPanel.add(helpB,cstr);
		
		// The execute button.
		Font font = executeB.getFont();
		font = font.deriveFont(
		    Collections.singletonMap(
		        TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD));
		executeB.setFont(font);
		Color oldC = executeB.getBackground();
		Color newC = new Color(oldC.getRed(),Math.min(oldC.getGreen() + 50,255),oldC.getBlue(),oldC.getAlpha());
		executeB.setBackground(newC);
		executeB.setOpaque(true);
		executeB.setBorderPainted(false);
		executeB.setEnabled(false);
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 2;
		cstr.weighty = 0.5;
		cstr.fill = GridBagConstraints.BOTH;
		cstr.insets = new Insets(5,0,0,0);
		goPanel.add(executeB,cstr);
		
		// Add the button panel to the bottom panel.
		cstr = new GridBagConstraints();
		cstr.gridx = 2;
		cstr.gridy = 0;
		cstr.insets = new Insets(5,5,5,5);
		cstr.fill = GridBagConstraints.VERTICAL;
		bottomPanel.add(goPanel,cstr);
		
		// Add the bottom panel to the main panel at position (0,3) with span (3,1).
		cstr = new GridBagConstraints();
		cstr.gridx = 0;
		cstr.gridy = 3;
		cstr.gridwidth = 3;
		cstr.fill = GridBagConstraints.BOTH;
		pane.add(bottomPanel,cstr);
		
		//
		// Action Listeners
		//
		
		dirMatchCB.addActionListener(new DirectoryMatchCheckboxListener(dirMatchCB,this));
		DIR_BUTTON.addActionListener(new OutputDirectoryButtonListener(top_level_frame,this));
		htmlB.addActionListener(new FileButtonListener(top_level_frame,this));
		recursiveB.addActionListener(new InputDirectoryButtonListener(top_level_frame,this));
		
		removeNumsCB.addActionListener(new NameOptionCheckboxListener(CBLMode.REMOVE_NUMS,removeNumsCB,this));
		indexUniqueCB.addActionListener(new NameOptionCheckboxListener(CBLMode.INDEX_UNIQUE,indexUniqueCB,this));
		capsCB.addActionListener(new NameOptionCheckboxListener(CBLMode.CORRECT_CAPS,capsCB,this));
		dateCB.addActionListener(new NameOptionCheckboxListener(CBLMode.MODIFY_DATE,dateCB,this));
		deleteCB.addActionListener(new NameOptionCheckboxListener(CBLMode.DELETE_HTML,deleteCB,this));
		
		prefixT.registerDocumentListener(new NameOptionTextListener(TLMode.PREFIX,prefixT,this));
		suffixT.registerDocumentListener(new NameOptionTextListener(TLMode.SUFFIX,suffixT,this));
		unnamedT.registerDocumentListener(new NameOptionTextListener(TLMode.UNNAMED,unnamedT,this));
		maxLenT.getDocument().addDocumentListener(new NameOptionTextListener(TLMode.LENGTH,maxLenT,this));
		
		refuseRB.addActionListener(new NameOptionRadioListener(RLMode.REFUSE,refuseRB,this));
		warnRB.addActionListener(new NameOptionRadioListener(RLMode.WARN,warnRB,this));
		truncateRB.addActionListener(new NameOptionRadioListener(RLMode.TRUNCATE,truncateRB,this));
		vowelRB.addActionListener(new NameOptionRadioListener(RLMode.DROP_VOWELS,vowelRB,this));
		allowRB.addActionListener(new NameOptionRadioListener(RLMode.ALLOW,allowRB,this));
		hyphenRB.addActionListener(new NameOptionRadioListener(RLMode.HYPHEN,hyphenRB,this));
		commaRB.addActionListener(new NameOptionRadioListener(RLMode.COMMA,commaRB,this));
		nothingRB.addActionListener(new NameOptionRadioListener(RLMode.NOTHING,nothingRB,this));
		
		aboutB.addActionListener(new HelpButtonListener(this,HelpMode.ABOUT));
		helpB.addActionListener(new HelpButtonListener(this,HelpMode.HELP));

		TableMouseListener tml = new TableMouseListener(this,PHOTOS_TABLE);
		PHOTOS_TABLE.addMouseListener(tml);
		PHOTOS_TABLE.addMouseMotionListener(tml);
		
		executeB.addActionListener(new ExecuteButtonListener(this));
	}
	
	/**
	 * Add a line of output to the output text area, with a new line.
	 * This is the same as calling {@link #addOutput(String, boolean)} with
	 * newLine set to true.
	 * 
	 * @param message The line of text to add. Automatically followed by a newline.
	 */
	public void addOutput(String message)
	{
		addOutput(message,true);
	}
	
	/**
	 * Calls {@link #addOutput(String)} for each item in the provided list.
	 * 
	 * @param outs The list of lines of output.
	 */
	public void addOutput(ArrayList<String> outs)
	{
		for(String out : outs)
			addOutput(out);
	}
	
	/**
	 * Add a line of output to the output text area, immediately redraw
	 * the output text area, and move the parent scroll bar to the bottom.
	 * 
	 * @param message The line of text to add. Automatically followed by a newline.
	 * @param newLine Whether to add a new line at the end of the message.
	 */
	public void addOutput(String message, boolean newLine)
	{
		OUTPUT.append(message + (newLine ? "\n" : ""));
		outputP.validate();
		JScrollBar vert = outputP.getVerticalScrollBar();
		vert.setValue(vert.getMaximum());
		outputP.paintImmediately(outputP.getVisibleRect());
	}
	
	/**
	 * Enable or disable the execute button based on the readiness of all input files.
	 */
	public void checkReadyStatus()
	{
		executeB.setEnabled(INPUTS_MODEL.isExecutable());
	}
	
	/**
	 * Determine the best path at which a file or directory dialog should
	 * start browsing.
	 * 
	 * @param forInput	Whether browsing for an input location. If false,
	 * 					the path will be chosen on the assumption that the
	 * 					dialog is looking for an output directory.
	 * @return			The best path at which to start browsing.
	 */
	public Path determineBestBrowsePath(boolean forInput)
	{
		if(forInput)
		{
			/* 
			 * If an input directory exists, check whether it contains any input files.
			 * If it does not, check whether it contains any photos.
			 * 
			 * If the input directory does not contain input files AND it contains photos,
			 * probably a download took place here and the next input file is most likely
			 * in a sibling directory, so browse the parent.
			 *
			 * If the input directory exists but does not meet the above criteria, browse
			 * the input directory.
			 * 
			 * If an input directory does not exist, use the output directory.
			 * If neither directory exists, use the OS-defined home directory.
			 */
			
			Path inputDir = CONFIG.getInputDirectory();
			
			if(inputDir != null && Files.exists(inputDir))
			{
				if(dirHasPhotosButNoInputFiles(inputDir))
					return inputDir.getParent();
				return inputDir;
			}
			
			Path outputDir = CONFIG.getOutputDirectory();
			
			if(outputDir != null && Files.exists(outputDir))
			{
				if(dirHasPhotosButNoInputFiles(outputDir))
					return outputDir.getParent();
				return outputDir;
			}
			return Paths.get(System.getProperty("user.home"));
			
		}
		else // (if !forInput)
		{
			Path outputDir = CONFIG.getOutputDirectory();
			
			if(outputDir != null && Files.exists(outputDir))
				return outputDir;
			else
				return Paths.get(System.getProperty("user.home"));
		}
	}
	
	/**
	 * Determine whether the specified path has photos and not an input file.
	 * 
	 * @param dir	The directory to check.
	 * @return		True if the directory contains at least one .jpg file and no input files.
	 */
	private boolean dirHasPhotosButNoInputFiles(Path dir)
	{
		try
		{
			FileSearcher JPGsearch = new FileSearcher(true,".jpg");
			Files.walkFileTree(dir, JPGsearch);
			if(JPGsearch.hasMatches())
			{
				FileSearcher HTMsearch = new FileSearcher(true,ConfigurationManager.INPUT_FILE_TYPE);
				Files.walkFileTree(dir, HTMsearch);
				return !HTMsearch.hasMatches();
			}
		}
		catch(IOException ioe)
		{
			return false;
		}
		
		return false;
	}
	
	/**
	 * Execute download of all photos.
	 */
	public synchronized void executeDownload()
	{
		INPUTS_MODEL.clearAllStatuses();
		
		// Save config to file
		try {CONFIG.commitToFile();}
		catch (IOException e)
		{
			addOutput("Failed to commit configuration settings to file.");
		}
		
		// Freeze the execute button
		executeB.setEnabled(false);
		
		// Execute the photo downloads
		for(int i = 0; i < INPUTS_MODEL.getRowCount(); i ++)
		{
			setSelectedInputFile(i,false);
			int count = PhotoDownloader.downloadPhotos
			(
				INPUTS_MODEL,
				i,
				CONFIG.getAutoUseHTMLDir() ? INPUTS_MODEL.getInputFileDirectory(i) : CONFIG.getOutputDirectory(),
				this
			);
			addOutput("Wrote " + count + " photos to file.");
			redrawInputsTable(i);
		}
		
		setSelectedInputFile(INPUTS_MODEL.getRowCount() - 1,false);
		
		// Unfreeze the execute button
		executeB.setEnabled(true);
	}
	
	/**
	 * Get the currently-showing photos table model.
	 * 
	 * @return	The currently-showing photos table model.
	 */
	public PhotoCollection getActivePhotoModel()
	{
		int sel = INPUTS_TABLE.getSelectedRow();
		return INPUTS_MODEL.getInputFilePhotoCollection(sel);
	}
	
	/**
	 * Get a COPY of the program configuration, not a reference.
	 * 
	 * @return	A COPY of the program configuration.
	 */
	public Config getConfiguration()
	{
		return CONFIG.getConfig();
	}
	
	/**
	 * Search the all valid input files (of type {@link ConfigurationManager#INPUT_FILE_TYPE})
	 * in the given directory.
	 * 
	 * @param directory The directory to search for files.
	 * @return All matching files within the directory.
	 */
	public Path[] getInputFilesFromDirectory(Path directory)
	{
		InputDirectoryInspector sdl = new InputDirectoryInspector(ConfigurationManager.INPUT_FILE_TYPE);
		try{Files.walkFileTree(directory,sdl);} catch (IOException e){}
		
		return sdl.getMatches();
	}
	
	/**
	 * Immediately redraw the inputs  table (to reflect data changes) with
	 * respect to the specified row. This will also cause the table to
	 * scroll to show any rows not visible, which is a helpful side-effect.
	 * 
	 * @param row The row to update and redraw.
	 */
	public synchronized void redrawInputsTable(int row)
	{
		int sel = INPUTS_TABLE.getSelectedRow();
		
		INPUTS_TABLE.scrollRectToVisible(new Rectangle(INPUTS_TABLE.getCellRect(row,1,true)));
		INPUTS_TABLE.paintImmediately(INPUTS_TABLE.getVisibleRect());
		checkReadyStatus();
		
		if(sel >= 0)
			INPUTS_TABLE.setRowSelectionInterval(sel,sel);
	}
	
	/**
	 * Immediately redraw the photos table (to reflect data changes) with
	 * respect to the specified row. This will also cause the table to
	 * scroll to show any rows not visible, which is a helpful side-effect.
	 * 
	 * @param row The row to update and redraw.
	 */
	public synchronized void redrawPhotosTable(int row)
	{
		PHOTOS_TABLE.scrollRectToVisible(new Rectangle(PHOTOS_TABLE.getCellRect(row,1,true)));
		PHOTOS_TABLE.paintImmediately(PHOTOS_TABLE.getVisibleRect());
		checkReadyStatus();
	}
	
	/**
	 * Select the input file at the specified index (row) and update the
	 * photos table accordingly.
	 * 
	 * @param index			The index (row) of the file to select.
	 * @param isOriginUser	Whether the selection came from the user (as opposed
	 * 						to being set programatically).
	 */
	public synchronized void setSelectedInputFile(int index, boolean isOriginUser)
	{
		if(!isOriginUser)
			INPUTS_TABLE.setRowSelectionInterval(index,index, true);
		
		PHOTOS_TABLE.setModel(INPUTS_MODEL.getInputFilePhotoCollection(index));

		PHOTOS_TABLE.getColumnModel().getColumn(0).setMaxWidth(20);
		PHOTOS_TABLE.getColumnModel().getColumn(1).setMaxWidth(20);
		PHOTOS_TABLE.getColumnModel().getColumn(2).setMaxWidth(20);
		
		DefaultCellEditor singleClick = new DefaultCellEditor(new JTextField());
		singleClick.setClickCountToStart(1);
		PHOTOS_TABLE.setDefaultEditor(PHOTOS_TABLE.getColumnClass(2),singleClick);
	}
	
	/**
	 * Get the row index of the currently selected input file.
	 * @return The row index of the currently selected input file.
	 */
	public int getSelectedInputFileIndex()
	{
		return INPUTS_TABLE.getSelectedRow();
	}
	
	/**
	 * Update whether capitalization should be corrected.
	 * 
	 * @param newStatus Whether capitalization should be corrected.
	 */
	public void updateCorrectCaps(boolean newStatus)
	{
		CONFIG.setCorrectCaps(newStatus);
		INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update whether the HTML input file should be deleted.
	 * 
	 * @param newStatus	Whether to delete input files.
	 */
	public void updateDeleteHTML(boolean newStatus)
	{
		CONFIG.setDeleteHTML(newStatus);
	}
	
	/**
	 * Update whether photos will be output to the directory
	 * of their input file or a specified directory.
	 * 
	 * @param match	Whether to match directories.
	 */
	public void updateDirectoryMatch(boolean match)
	{
		CONFIG.setDirectoryMatch(match);
		DIR_BUTTON.setEnabled(!match);
	}
	
	/**
	 * Update the execute button's text to reflect the current status of an
	 * ongoing photo download, and redraw immediately.
	 * If the two arguments are the same, the button is returned to its
	 * original state.
	 * 
	 * @param done	How many photos have already been completed.
	 * @param total How many photos there are to do in total.
	 */
	public synchronized void updateExecuteCount(int done, int total)
	{
		if(done >= total)
			executeB.setText("Execute");
		else
			executeB.setText(done + " / " + total);
		
		executeB.paintImmediately(executeB.getVisibleRect());
	}
	
	/**
	 * Update the filename prefix and its textbox.
	 * 
	 * @param newPrefix			The new prefix.
	 * @param isOriginUser		Whether the origin of this change is the user typing in the textbox
	 * 							(as opposed to the {@link DescriptionProcessor} or {@link ConfigurationManager}
	 * 							forcibly causing this change}.
	 */
	public void updateFilePrefix(String newPrefix, boolean isOriginUser)
	{
		if(!isOriginUser)
			prefixT.setText(newPrefix, true);
		
		CONFIG.setPrefix(newPrefix);
		
		if(isOriginUser)
			INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update the filename suffix field.
	 * 
	 * @param newSuffix			The new suffix.
	 * @param isOriginUser		Whether the origin of this change is the user typing in the textbox
	 * 							(as opposed to the {@link DescriptionProcessor} or {@link ConfigurationManager}
	 * 							forcibly causing this change}.
	 */
	public void updateFileSuffix(String newSuffix, boolean isOriginUser)
	{
		if(!isOriginUser)
			suffixT.setText(newSuffix, true);
		
		CONFIG.setSuffix(newSuffix);
		
		if(isOriginUser)
			INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update whether unique descriptions should still be indexed.
	 * 
	 * @param newStatus Whether unique descriptions should still be indexed.
	 */
	public void updateIndexUnique(boolean newStatus)
	{
		CONFIG.setIndexUnique(newStatus);
		INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update the input directory and immediately read it.
	 * 
	 * @param dirPath The input directory.
	 */
	public void updateInputDirectory(Path dirPath)
	{
		if(dirPath == null)
			return;
		
		try {CONFIG.setInputDirectory(dirPath);}
		catch (NotDirectoryException e)
		{
			addOutput("Could not set input directory to \"" + dirPath.toString() + "\": not a directory.");
			return;
		}
		
		osLimitT.setText(CONFIG.getOsMaxFileLength() + "");
		htmlLabel.setText(dirPath.toString());
		
		Path[] paths = getInputFilesFromDirectory(dirPath);
		ArrayList<InputFile> files = new ArrayList<InputFile>(paths.length);
		for(Path path : paths)
		{
			try
			{
				files.add(new InputFile(this,path));
			}
			catch (HTMLParseException e)
			{
				addOutput(e.getMessage());
			}
		}
		
		INPUTS_MODEL = new InputFileCollection(files, this);
		INPUTS_TABLE.setModel(INPUTS_MODEL);

		INPUTS_TABLE.getColumnModel().getColumn(0).setMaxWidth(20);
		INPUTS_TABLE.getColumnModel().getColumn(1).setMaxWidth(20);
		
		INPUTS_TABLE.startListening();
		
		setSelectedInputFile(0,false);
		checkReadyStatus();
	}
	
	/**
	 * Designate a singular input file, rather than a directory tree.
	 * Note that it is possible for the designated file to have siblings that are also valid input
	 * files, but the user has specifically chosen this one.
	 * 
	 * @param inputFile The file to process.
	 */
	public void updateInputFile(Path inputFile)
	{
		try	{CONFIG.setInputDirectory(inputFile.getParent());}
		catch(NotDirectoryException e)
		{ /* It's impossible to select a file that doesn't have a valid directory parent. */ }

		htmlLabel.setText(inputFile.getParent().toString());
		try
		{
			InputFile file = new InputFile(this,inputFile);
			ArrayList<InputFile> files = new ArrayList<InputFile>(1);
			files.add(file);
			INPUTS_MODEL = new InputFileCollection(files, this);
			INPUTS_TABLE.setModel(INPUTS_MODEL);

			INPUTS_TABLE.getColumnModel().getColumn(0).setMaxWidth(20);
			INPUTS_TABLE.getColumnModel().getColumn(1).setMaxWidth(20);
			
			INPUTS_TABLE.startListening();
		}
		catch (HTMLParseException e)
		{
			addOutput(e.getMessage());
		}
		
		setSelectedInputFile(0,false);
		checkReadyStatus();
	}
	
	/**
	 * Update the user-defined maximum description length.
	 * 
	 * @param maxLen The new maximum description length.
	 */
	public void updateMaxLength(int maxLen)
	{
		CONFIG.setUserMaxLength(maxLen);
		INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update whether photo dates should be modified.
	 * 
	 * @param newStatus Whether photo dates should be modified.
	 */
	public void updateModifyDate(boolean newStatus)
	{
		CONFIG.setModifyDate(newStatus);
		INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update the output directory for photos.
	 * 
	 * @param newDir The directory into which to dump photos.
	 */
	public void updateOutputDirectory(Path newDir)
	{
		try {CONFIG.setOutputDirectory(newDir); }
		catch (NotDirectoryException e)
		{
			addOutput("Error: attempted to set the output directory to a non-directory path.");
			return;
		}
		OUTPUT_DIR_LABEL.setText(newDir.toString());
		osLimitT.setText(CONFIG.getOsMaxFileLength() + "");
	}
	
	/**
	 * Update the overlength behavior.
	 * 
	 * @param olb The new overlength behavior.
	 */
	public void updateOverLengthBehavior(OverlengthBehavior olb)
	{
		CONFIG.setOverLengthBehavior(olb);
		INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update whether trailing numbers are to be removed.
	 * 
	 * @param newStatus Whether trailing numbers are to be removed.
	 */
	public void updateRemoveTrailingNumbers(boolean newStatus)
	{
		CONFIG.setRemoveTrailingNumbers(newStatus);
		INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update the replacement character.
	 * 
	 * @param rc The new replacement character.
	 */
	public void updateReplacementChar(ReplacementCharacter rc)
	{
		CONFIG.setReplacementCharacter(rc);
		INPUTS_MODEL.reprocessAllFiles();
	}
	
	/**
	 * Update the undescribed file name field.
	 * 
	 * @param newUndescribed	The new undescribed file name.
	 * @param isOriginUser		Whether the origin of this change is the user typing in the textbox
	 * 							(as opposed to the {@link DescriptionProcessor} or {@link ConfigurationManager}
	 * 							forcibly causing this change}.
	 */
	public void updateUndescribed(String newUndescribed, boolean isOriginUser)
	{
		if(!isOriginUser)
			unnamedT.setText(newUndescribed, true);
		
		CONFIG.setUndescribed(newUndescribed);
		
		if(isOriginUser)
			INPUTS_MODEL.reprocessAllFiles();
	}
	
	/** Write assets to the file system such as the help file. **/
	public void writeAssets()
	{
		try
		{
			String cwd = System.getProperty("user.dir");
			
			InputStream help = this.getClass().getClassLoader().getResourceAsStream("assets/Help.pdf");
			Path helpTarget = Paths.get(cwd + "/help.pdf");
			Files.copy(help,helpTarget,StandardCopyOption.REPLACE_EXISTING);
			
			InputStream about = this.getClass().getClassLoader().getResourceAsStream("assets/Technical Description.pdf");
			Path aboutTarget = Paths.get(cwd + "/about.pdf");
			Files.copy(about,aboutTarget,StandardCopyOption.REPLACE_EXISTING);
		}
		catch(IOException e)
		{ /* Do nothing. */ }
	}
	
	/**
	 * Searches the specified directory for all files of the specified type and
	 * also tracks the longest directory name in the tree.
	 * 
	 * @author Nicholas Harrell
	 */
	private class InputDirectoryInspector extends FileSearcher
	{
		/** The highest directory name length found. **/
		private int highestLength = 0;
		
		/**
		 * Creates a new file searcher with {@link #MONO_SEARCH}
		 * set to false.
		 * 
		 * @param type			Maps to {@link #TYPE}.
		 */
		public InputDirectoryInspector(String type)
		{
			super(false, type);
		}
		
		/**
		 * Get the highest length this visitor has seen.
		 * 
		 * @return {@link #highestLength}
		 */
		public int getHighestLength()
		{
			return highestLength;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr)
		{
			if(attr.isDirectory())
				highestLength = Math.max(highestLength, file.toString().length());
			else
				highestLength = Math.max(highestLength, file.getParent().toString().length());
			
			return super.visitFile(file, attr);
		}
	}
}