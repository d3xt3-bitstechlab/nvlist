package nl.weeaboo.nvlist.build.android;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import nl.weeaboo.awt.AwtUtil;
import nl.weeaboo.awt.FileBrowseField;
import nl.weeaboo.awt.ProgressDialog;
import nl.weeaboo.nvlist.build.Build;
import nl.weeaboo.nvlist.build.LogoPanel;

@SuppressWarnings("serial")
public class AndroidGUI extends LogoPanel {

	private final Build build;
	private final AndroidPropertyPanel androidProperties;
	private final FileBrowseField templateBrowse;
	private final FileBrowseField outputBrowse;
	private final JButton createButton;
	
	public AndroidGUI(Build b) {
		super("header.png");
		
		build = b;								

		templateBrowse = new FileBrowseField(true, true);
		templateBrowse.setFile(new File(b.getProjectFolder(), "build-res/android-project-template"));
		
		outputBrowse = new FileBrowseField(true, true);
		outputBrowse.setFile(new File(b.getProjectFolder(), "android-project"));
		
		createButton = new JButton("Create Project");
		createButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					createProject();
				} catch (FileNotFoundException fnfe) {
					AwtUtil.showError(fnfe.getMessage());
				} catch (IOException ioe) {
					ioe.printStackTrace();
					AwtUtil.showError(ioe.getMessage());
				}
			}
		});
		
		JPanel createButtonPanel = new JPanel(new BorderLayout(5, 5));
		createButtonPanel.add(createButton, BorderLayout.EAST);
		
		JPanel outputPanel = new JPanel(new GridLayout(-1, 1, 5, 5));
		outputPanel.add(templateBrowse);
		outputPanel.add(outputBrowse);
		outputPanel.add(createButtonPanel);
		
		JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
		rightPanel.add(outputPanel, BorderLayout.SOUTH);
		
		androidProperties = new AndroidPropertyPanel(getBackground());
		androidProperties.setBuild(b);
		androidProperties.setPropertyDefinitions(b.getAndroidDefs());
		
		JPanel mainPanel = new JPanel(new GridLayout(-1, 2, 10, 10));
		mainPanel.setOpaque(false);
		mainPanel.add(androidProperties);
		mainPanel.add(rightPanel);
		
		setPreferredSize(new Dimension(600, 350));
		add(Box.createRigidArea(new Dimension(315, 95)), BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);
	}
	
	//Functions	
	public static void createFrame(AndroidGUI agui) {
		JFrame frame = new JFrame("NVList Build Config");
		//frame.setResizable(false);
		frame.setMinimumSize(new Dimension(600, 350));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(agui, BorderLayout.CENTER);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		AwtUtil.setFrameIcon(frame, getImageRes("icon.png"));
	}
			
	protected boolean createProject() throws IOException {		
		final File gameF = build.getProjectFolder();
		final File templateF = templateBrowse.getFile();
		if (!templateF.exists()) {
			throw new FileNotFoundException("Project template not found: " + templateF);
		}
		final File dstF = outputBrowse.getFile();

		createButton.setEnabled(false);
		
		final ProgressDialog dialog = new ProgressDialog();
		dialog.setMessage(String.format("Creating Android project..."));
		dialog.setProgress(25);
		
		SwingWorker<File, ?> worker = new SwingWorker<File, Void>() {
			protected File doInBackground() throws Exception {
				AndroidConfig config = AndroidConfig.fromFile(new File(build.getProjectFolder(), Build.PATH_ANDROID_INI));
				AndroidProjectCompiler apc = new AndroidProjectCompiler(gameF, templateF, dstF, config);
				apc.compile();
				return dstF;
			}
			protected void done() {
				dialog.dispose();
				createButton.setEnabled(true);
				super.done();
			}
		};
		
		dialog.setTask(worker);
		worker.execute();
		dialog.setVisible(true);
		
		try {
			File file = worker.get();
			if (file.exists()) {
				return true;
			}
		} catch (InterruptedException e) {
			//Return an error
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException)e.getCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			} else {
				throw new IOException(e.getCause());
			}
		}
		return false;
	}
	
	//Getters
	
	//Setters
		
}
