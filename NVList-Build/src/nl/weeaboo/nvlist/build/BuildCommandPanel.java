package nl.weeaboo.nvlist.build;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

import nl.weeaboo.awt.AwtUtil;
import nl.weeaboo.awt.MessageBox;
import nl.weeaboo.nvlist.build.android.AndroidGUI;
import nl.weeaboo.settings.INIFile;

@SuppressWarnings("serial")
public class BuildCommandPanel extends JPanel {

	private Build build;

	private final RunPanel runPanel;
	private final ConsoleOutputPanel outputPanel;
	private final Action optimizerAction, buildAppletAction, buildInstallerAction, buildInstallerCDAction, androidAction;
	private final JButton rebuildButton, editButton, backupButton;
	private final JButton moreButton;

	public BuildCommandPanel(ConsoleOutputPanel output) {
		outputPanel = output;
		runPanel = new RunPanel(this, outputPanel);

		optimizerAction = new AbstractAction("Resource Optimizer...") {
			public void actionPerformed(ActionEvent event) {
				ClassLoader cl = build.getClassLoader();
				try {
					Class<?> clazz = cl.loadClass("nl.weeaboo.game.optimizer.OptimizerGUI");
					Constructor<?> constr = clazz.getConstructor(File.class, String.class);
					Object optObj = constr.newInstance(build.getProjectFolder(), build.getGameId());
					clazz.getDeclaredMethod("createFrame", clazz).invoke(null, optObj);
				} catch (Exception e) {
					e.printStackTrace();
					AwtUtil.showError(e);
				}
			}
		};

		buildAppletAction = new AbstractAction("Create Applet") {
			public void actionPerformed(ActionEvent e) {
				if (preReleaseCheck()) {
					ant("clean dist-applet");
				}
			}
		};

		buildInstallerAction = new AbstractAction("Create Release") {
			public void actionPerformed(ActionEvent e) {
				if (preReleaseCheck()) {
					ant("clean dist make-installer-zip make-installer make-installer-mac");
				}
			}
		};

		buildInstallerCDAction = new AbstractAction("Create CD Release") {
			public void actionPerformed(ActionEvent e) {
				if (preReleaseCheck()) {
					ant("clean dist make-installer-cd");
				}
			}
		};
		
		androidAction = new AbstractAction("Create Android Project...") {
			public void actionPerformed(ActionEvent event) {
				AndroidGUI agui = new AndroidGUI(build);
				AndroidGUI.createFrame(agui);
			}
		};

		rebuildButton = new JButton("Rebuild");
		rebuildButton.setOpaque(false);
		rebuildButton.setEnabled(false);
		rebuildButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rebuild();
			}
		});

		editButton = new JButton("Edit");
		editButton.setOpaque(false);
		editButton.setEnabled(false);
		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File scriptF = new File(build.getProjectFolder(), "res/script");
				try {
					Desktop.getDesktop().open(scriptF);
				} catch (IOException ioe) {
					try {
						Runtime.getRuntime().exec("open " + scriptF);
					} catch (IOException ioe2) {
						JOptionPane.showMessageDialog(BuildCommandPanel.this, "Error opening script folder: "
								+ scriptF, "Unable to perform action", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		backupButton = new JButton("Backup");
		backupButton.setOpaque(false);
		backupButton.setEnabled(false);
		backupButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ant("backup");
			}
		});

		JPanel buttonPanel = new JPanel(new GridLayout(-1, 3, 10, 10));
		buttonPanel.setOpaque(false);
		buttonPanel.add(rebuildButton);
		buttonPanel.add(editButton);
		buttonPanel.add(backupButton);

		moreButton = new JButton("...");
		moreButton.setPreferredSize(new Dimension(30, 22));
		moreButton.setOpaque(false);
		moreButton.setEnabled(false);
		moreButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JPopupMenu popup = new JPopupMenu();
				popup.add(optimizerAction);
				popup.add(buildAppletAction);
				popup.add(buildInstallerAction);
				popup.add(buildInstallerCDAction);
				popup.add(androidAction);
				popup.show(moreButton, 0, 0);
			}
		});

		JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
		mainPanel.setOpaque(false);
		mainPanel.add(buttonPanel, BorderLayout.CENTER);
		mainPanel.add(moreButton, BorderLayout.EAST);
		mainPanel.add(runPanel, BorderLayout.SOUTH);

		JLabel titleLabel = new JLabel("Build Commands");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setBorder(new EmptyBorder(5, 0, 5, 0));

		setOpaque(false);
		setLayout(new BorderLayout());
		add(titleLabel, BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);
	}
    
	// Functions
	public void rebuild(final Runnable... postBuildCallbacks) {
		ant(build.getRebuildTarget(), postBuildCallbacks);		
	}
	
	public void ant(String target, final Runnable... postBuildCallbacks) {
		try {
			outputPanel.process(build.ant(target), new Runnable() {
				public void run() {
					runPanel.update();
					
					if (postBuildCallbacks != null) {
						for (Runnable r : postBuildCallbacks) {
							r.run();
						}
					}
				}
			});
		} catch (IOException e) {
			AwtUtil.showError("Error starting " + target + " command: " + e);
		}
	}

	protected boolean preReleaseCheck() {
		INIFile ini = build.getProperties();

		if (ini.getBoolean("debug", false)) {
			MessageBox mb = new MessageBox("Confirm action", "Debug mode is still on. "
					+ "Are you sure you want to build a release with it turned on?");
			mb.addButton("Build a debug release", "");
			mb.addButton("Cancel", "");
			if (mb.showMessage(this) != 0) {
				return false;
			}
		}
		
		if (ini.getBoolean("vn.enableProofreaderTools", false)) {
			MessageBox mb = new MessageBox("Confirm action", "Proofreader tools are currently turned on. "
					+ "Are you sure you want to build a release with proofreader tools enabled?");
			mb.addButton("I want proofreader tools enabled", "");
			mb.addButton("Cancel", "");
			if (mb.showMessage(this) != 0) {
				return false;
			}
		}

		return true;
	}

	// Getters

	// Setters
	public void setBuild(Build b) {
		if (build != b) {
			build = b;

			runPanel.setBuild(b);
		}

		rebuildButton.setEnabled(build != null);
		editButton.setEnabled(build != null);
		backupButton.setEnabled(build != null);
		moreButton.setEnabled(build != null);
	}

}
