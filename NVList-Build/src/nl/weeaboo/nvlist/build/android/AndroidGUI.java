package nl.weeaboo.nvlist.build.android;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import nl.weeaboo.awt.AwtUtil;
import nl.weeaboo.awt.DirectoryChooser;
import nl.weeaboo.nvlist.build.Build;
import nl.weeaboo.nvlist.build.ConsoleOutputPanel;
import nl.weeaboo.nvlist.build.LogoPanel;
import nl.weeaboo.nvlist.build.TranslucentPanel;

@SuppressWarnings("serial")
public class AndroidGUI extends LogoPanel {

	private enum AntMode {
		UPDATE, CREATE;
	}
	
	private final Build build;
	private final AndroidPropertyPanel androidProperties;
	private final ConsoleOutputPanel consoleOutput;
	private final JButton updateButton, createButton;
	
	private boolean busy;
	
	public AndroidGUI(Build b) {
		super("header.png");
		
		build = b;

		consoleOutput = new ConsoleOutputPanel();
		
		updateButton = new JButton("Update Existing Android Project...");
		updateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File updateFolder = new File(build.getProjectFolder(), "android-project");
				DirectoryChooser dc = new DirectoryChooser(true);
				dc.setSelectedDirectory(updateFolder);
				if (dc.showDialog(AndroidGUI.this, "Select Android project to update...")) {
					updateFolder = dc.getSelectedDirectory();
					if (!updateFolder.exists()) {
						AwtUtil.showError("Selected folder doesn't exist or can't be read: " + updateFolder);
						return;
					}
					createAndroidProject(AntMode.UPDATE, updateFolder, updateFolder);
				}
			}
		});
		
		createButton = new JButton("Create New Android Project");
		createButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createAndroidProject(AntMode.CREATE, null, null);
			}
		});
		
		JPanel buildPanel = new JPanel();
		buildPanel.setLayout(new GridLayout(-1, 1, 5, 5));
		buildPanel.setOpaque(false);
		buildPanel.add(updateButton);
		buildPanel.add(createButton);
		
		JPanel commandPanel = new TranslucentPanel();
		commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.X_AXIS));
		commandPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		commandPanel.add(Box.createHorizontalGlue());
		commandPanel.add(buildPanel);
		commandPanel.add(Box.createHorizontalGlue());
		
		JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
		rightPanel.setOpaque(false);
		rightPanel.add(consoleOutput, BorderLayout.CENTER);
		rightPanel.add(commandPanel, BorderLayout.SOUTH);
		
		androidProperties = new AndroidPropertyPanel(getBackground());
		androidProperties.setBuild(b);
		androidProperties.setPropertyDefinitions(b.getAndroidDefs());
		
		JPanel mainPanel = new JPanel(new GridLayout(-1, 2, 10, 10));
		mainPanel.setOpaque(false);
		mainPanel.add(androidProperties);
		mainPanel.add(rightPanel);
		
		setPreferredSize(new Dimension(650, 450));
		add(Box.createRigidArea(new Dimension(315, 95)), BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);
	}
	
	//Functions	
	public static void createFrame(AndroidGUI agui) {
		JFrame frame = new JFrame("Android Build Config");
		//frame.setResizable(false);
		frame.setMinimumSize(new Dimension(600, 350));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(agui, BorderLayout.CENTER);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		AwtUtil.setFrameIcon(frame, getImageRes("icon.png"));
	}
			
	private void createAndroidProject(AntMode mode, File templateFolder, File outputFolder) {
		StringBuilder sb = new StringBuilder("make-android-project");
		if (templateFolder != null) {
			sb.append(" -Ddist-android-template.dir=\"" + templateFolder + "\"");
		}
		if (outputFolder != null) {
			sb.append(" -Ddist-android-output.dir=\"" + outputFolder + "\"");
		}
		ant(sb.toString());
	}
	
	public void ant(String args, final Runnable... postBuildCallbacks) {
		setBusy(true);
		try {
			System.out.println("ANT: " + args);

			consoleOutput.process(build.ant(args), new Runnable() {
				public void run() {
					setBusy(false);
					
					if (postBuildCallbacks != null) {
						for (Runnable r : postBuildCallbacks) {
							r.run();
						}
					}
				}
			});
		} catch (IOException e) {
			AwtUtil.showError("Error starting ant with args: " + args + " :: " + e);
			setBusy(false);
		}
	}	
	
	//Getters
	
	//Setters
	public void setBusy(boolean b) {
		if (busy != b) {
			busy = b;
			
			updateButton.setEnabled(!busy);
			createButton.setEnabled(!busy);
		}
	}
		
}
