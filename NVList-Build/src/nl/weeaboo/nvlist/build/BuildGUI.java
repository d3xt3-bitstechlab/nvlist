package nl.weeaboo.nvlist.build;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import nl.weeaboo.awt.AwtUtil;
import nl.weeaboo.awt.FileBrowseField;
import nl.weeaboo.awt.ProgressDialog;
import nl.weeaboo.common.StringUtil;
import nl.weeaboo.io.FileUtil;
import nl.weeaboo.settings.INIFile;

@SuppressWarnings("serial")
public class BuildGUI extends LogoPanel {

	private enum CreateProjectResult {
		ERROR, UNABLE, REFUSED, EXISTS, CREATED;
	}
	
	private final INIFile ini;
	
	private Build build;
	private final HeaderPanel headerPanel;
	private final FileBrowseField engineBrowseField, projectBrowseField;
	private ProjectPropertyPanel projectProperties;
	private BuildCommandPanel buildCommandPanel;
	private ConsoleOutputPanel consoleOutput;
	
	public BuildGUI() {
		super("header.png");

		ini = new INIFile();
				
		headerPanel = new HeaderPanel(getBackground());

		engineBrowseField = headerPanel.getEngineBrowseField();
		engineBrowseField.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if ("file".equals(evt.getPropertyName())) {
					setEngineFolder((File)evt.getNewValue());
				}
			}
		});

		projectBrowseField = headerPanel.getProjectBrowseField();		
		projectBrowseField.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if ("file".equals(evt.getPropertyName())) {
					setProjectFolder((File)evt.getNewValue());
				}
			}
		});
		
		consoleOutput = new ConsoleOutputPanel();
		buildCommandPanel = new BuildCommandPanel(consoleOutput);
		projectProperties = new ProjectPropertyPanel(consoleOutput, getBackground());
		
		JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
		rightPanel.setOpaque(false);
		rightPanel.add(buildCommandPanel, BorderLayout.NORTH);
		rightPanel.add(consoleOutput, BorderLayout.CENTER);
		
		JPanel mainPanel = new JPanel(new GridLayout(-1, 2, 10, 10));
		mainPanel.setOpaque(false);
		mainPanel.add(projectProperties);
		mainPanel.add(rightPanel);
		
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setPreferredSize(new Dimension(750, 550));
		setLayout(new BorderLayout(5, 5));
		add(headerPanel, BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);
	}
	
	//Functions
	
	public static void main(String args[]) {
		AwtUtil.setDefaultLAF();
		
		final BuildGUI buildGui = new BuildGUI();
		if (args.length >= 2) {
			buildGui.createBuild(new File(args[0]), new File(args[1]));
		} else {
			if (new File("build-res").exists()) {
				buildGui.setProjectFolder(new File("").getAbsoluteFile());
			}
			
			try {
				buildGui.loadSettings();
			} catch (IOException e) {
				System.err.println(e);
			}
		}
				
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("NVList Build Config");
				//frame.setResizable(false);
				frame.setMinimumSize(new Dimension(700, 350));
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.add(buildGui, BorderLayout.CENTER);
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
				frame.addWindowListener(new WindowAdapter() {
					public void windowClosed(WindowEvent event) {
						try {
							buildGui.saveSettings();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				
				AwtUtil.setFrameIcon(frame, getImageRes("icon.png"));			
				buildGui.createBuild(buildGui.engineBrowseField.getFile(), buildGui.projectBrowseField.getFile());
			}
		});
	}
	
	public boolean askCreateProject(File projectFolder) {
		int r = JOptionPane.showConfirmDialog(this, "Project folder (" + projectFolder
				+ ") doesn't exist or is not a valid project folder.\nCreate a new project in that location?",
				"Confirm Create Project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		return r == JOptionPane.OK_OPTION;
	}

	private CreateProjectResult tryCreateProject(File engineFolder, File projectFolder) throws IOException {
		if (engineFolder == null) {
			//engineFolder = (build != null ? build.getEngineFolder() : projectFolder);
		}
		if (projectFolder == null) {
			projectFolder = (build != null ? build.getProjectFolder() : engineFolder);
		}

		if (engineFolder == null || !engineFolder.exists()) {
			return CreateProjectResult.UNABLE;
		} else if (projectFolder == null) {
			return CreateProjectResult.UNABLE;			
		} else if (new File(projectFolder, "res").exists()) {
			return CreateProjectResult.EXISTS;
		}
		
		if (!askCreateProject(projectFolder)) {
			return CreateProjectResult.REFUSED;
		}
		
		final File src = engineFolder, dst = projectFolder;
		final long srcSize = FileUtil.getSize(new File(src, "res")) + FileUtil.getSize(new File(src, "build-res"));
		final ProgressDialog dialog = new ProgressDialog();
		dialog.setMessage(String.format("Copying %s, please wait...",
				StringUtil.formatMemoryAmount(srcSize)));
		dialog.setProgress(25);
		
		SwingWorker<File, ?> worker = new SwingWorker<File, Void>() {
			protected File doInBackground() throws Exception {
				Build.createEmptyProject(src, dst);
				
				return dst;
			}
			protected void done() {
				dialog.dispose();
				super.done();
			}
		};
		
		dialog.setTask(worker);
		worker.execute();
		dialog.setVisible(true);
		
		try {
			if (worker.get().exists()) {
				return CreateProjectResult.CREATED;
			}
		} catch (InterruptedException e) {
			//Return an error
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException)e.getCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			}
		}
		return CreateProjectResult.ERROR;
	}
	
	protected void createBuild(File engineFolder, File projectFolder) {
		if (engineFolder == null) {
			engineFolder = (build != null ? build.getEngineFolder() : projectFolder);
		}
		if (projectFolder == null) {
			File engineBuildJAR = new File(engineFolder, "Build.jar");
			if (engineBuildJAR.exists()) {
				projectFolder = (build != null ? build.getProjectFolder() : engineFolder);
			} else {
				projectFolder = null;
			}
		}
		
		CreateProjectResult cpr = CreateProjectResult.ERROR;
		try {
			if (getParent() == null || engineFolder == null || projectFolder == null) {
				return;
			}
			
			cpr = tryCreateProject(engineFolder, projectFolder);
			if (cpr != CreateProjectResult.EXISTS && cpr != CreateProjectResult.CREATED) {
				return;
			}
			
			build = new Build(engineFolder, projectFolder);
			projectProperties.setPropertyDefinitions(build.getBuildDefs(),
					build.getGameDefs(), build.getPrefsDefaultDefs(),
					build.getInstallerConfigDefs());
			projectProperties.setBuild(build);
			projectProperties.update();
			buildCommandPanel.setBuild(build);
		} catch (RuntimeException re) {
			re.printStackTrace();
			AwtUtil.showError(re.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			AwtUtil.showError(e.getMessage());
		} catch (LinkageError e) {
			e.printStackTrace();
			AwtUtil.showError(e.getMessage());
		} finally {
			engineBrowseField.setFile(engineFolder);
			projectBrowseField.setFile(projectFolder);			
		}		
		
		//Trigger an automatic rebuild after creating a new project
		if (cpr == CreateProjectResult.CREATED) {
			buildCommandPanel.rebuild();
		}
	}
		
	protected void loadSettings() throws IOException {
		ini.read(new File("build.ini"));
		if (ini.containsKey("engineFolder")) {
			setEngineFolder(new File(ini.getString("engineFolder", "")));
		}
		if (ini.containsKey("projectFolder")) {
			setProjectFolder(new File(ini.getString("projectFolder", "")));
		}
	}
	protected void saveSettings() throws IOException {
		if (build != null) {
			ini.put("engineFolder", build.getEngineFolder().toString());
			ini.put("projectFolder", build.getProjectFolder().toString());
		}
		ini.write(new File("build.ini"));
	}
			
	//Getters
	protected static BufferedImage getImageRes(String filename) {
		try {
			return ImageIO.read(BuildGUI.class.getResource("res/" + filename));
		} catch (IOException e) {
			return new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		}		
	}
	
	//Setters
	public void setEngineFolder(File folder) {
		if (folder == null) return;
		
		System.out.println("Engine folder: \"" + folder + "\"");
		if (build == null || !build.getEngineFolder().equals(folder)) {
			createBuild(folder, projectBrowseField.getFile());
		}
	}
	public void setProjectFolder(File folder) {
		if (folder == null) return;
		
		System.out.println("Project folder: \"" + folder + "\"");
		if (build == null || !build.getProjectFolder().equals(folder)) {
			createBuild(engineBrowseField.getFile(), folder);
		}
	}
		
}
