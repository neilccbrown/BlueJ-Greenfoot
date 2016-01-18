/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2013,2014,2015,2016  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.gui;

import bluej.extensions.SourceType;
import greenfoot.Actor;
import greenfoot.World;
import greenfoot.actions.AboutGreenfootAction;
import greenfoot.actions.CloseProjectAction;
import greenfoot.actions.ConvertToJavaSelectedClassAction;
import greenfoot.actions.ExportProjectAction;
import greenfoot.actions.ImportClassAction;
import greenfoot.actions.NewClassAction;
import greenfoot.actions.NewWizardScenarioAction;
import greenfoot.actions.OpenProjectAction;
import greenfoot.actions.OpenRecentProjectAction;
import greenfoot.actions.PasteImageAction;
import greenfoot.actions.PauseSimulationAction;
import greenfoot.actions.PreferencesAction;
import greenfoot.actions.QuitAction;
import greenfoot.actions.RemoveSelectedClassAction;
import greenfoot.actions.ResetWorldAction;
import greenfoot.actions.RunOnceSimulationAction;
import greenfoot.actions.RunSimulationAction;
import greenfoot.actions.SaveAsAction;
import greenfoot.actions.SaveProjectAction;
import greenfoot.actions.SaveWorldAction;
import greenfoot.actions.SetPlayerAction;
import greenfoot.actions.ShowApiDocAction;
import greenfoot.actions.ShowCopyrightAction;
import greenfoot.actions.ShowReadMeAction;
import greenfoot.actions.ShowWebsiteAction;
import greenfoot.actions.ToggleAction;
import greenfoot.actions.ToggleDebuggerAction;
import greenfoot.actions.ToggleSoundAction;
import greenfoot.core.ClassStateManager;
import greenfoot.core.GClass;
import greenfoot.core.GCoreClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.CompileListener;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.Selectable;
import greenfoot.gui.classbrowser.SelectionListener;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.platforms.ide.SimulationDelegateIDE;
import greenfoot.platforms.ide.WorldHandlerDelegateIDE;
import greenfoot.sound.SoundFactory;
import greenfoot.util.AskHandler;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Menu;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
import javax.swing.Timer;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.BlueJTheme;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.prefmgr.PrefMgr;
import bluej.utility.CenterLayout;
import bluej.utility.DBox;
import bluej.utility.Debug;
import bluej.utility.Utility;

import com.apple.eawt.Application;

import java.awt.Font;

/**
 * The main frame for a Greenfoot project (one per project)
 * 
 * @author Poul Henriksen
 * @author mik
 */
public class GreenfootFrame extends JFrame
    implements WindowListener, CompileListener, WorldListener, SelectionListener
{
    private static final String shareIconFile = "export-publish-small.png";
    private static final int WORLD_MARGIN = 40;

    private static final int accelModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private static final int shiftAccelModifier = accelModifier | InputEvent.SHIFT_MASK;

    private RBlueJ rBlueJ;
    private GProject project;
    private GreenfootInspectorManager inspectorManager = new GreenfootInspectorManager();
        
    private WorldCanvas worldCanvas;
    private WorldHandler worldHandler;
    private WorldHandlerDelegateIDE worldHandlerDelegate;
    private Dimension worldDimensions;
    private ClassBrowser classBrowser;
    private ControlPanel controlPanel;
    private JScrollPane classScrollPane;
    /** The panel that needs to be revalidated when the world size changes */
    private JComponent centrePanel;

    private JMenu recentProjectsMenu;
    private JPanel messagePanel;
    private JLabel messageLabel;
    private JLabel messageLabel2;
    private JButton tooLongRestartButton;
    private CardLayout card;
    private DBox worldBox;
    private ExecutionTwirler executionTwirler;
    
    private NewClassAction newClassAction;
    private ImportClassAction importClassAction;
    private PasteImageAction pasteImageAction;
    private SaveProjectAction saveProjectAction;
    private SaveAsAction saveAsAction;
    private ShowReadMeAction showReadMeAction;
    private ExportProjectAction exportProjectAction;
    private ExportProjectAction shareAction;
    private CloseProjectAction closeProjectAction;
    private RemoveSelectedClassAction removeSelectedClassAction;
    private ConvertToJavaSelectedClassAction convertToJavaSelectedClassAction;
    private SaveWorldAction saveWorldAction;
    private SetPlayerAction setPlayerAction;
    
    private ToggleDebuggerAction toggleDebuggerAction;
    private ToggleSoundAction toggleSoundAction;
    
    // "Watchdog": make sure user code (world initialisation etc) doesn't stall for too long
    private Timer timer;
    private final static int WORLD_INITIALISING_TIMEOUT = 4000;
    private long startedInitialisingAt;
    
    /**
     * Specifies whether a compilation operation is in progress
     */
    private boolean isCompiling = false;
    
    /**
     * Specifies whether the project has been closed and only the empty frame is showing.
     * (Behind the scenes, the project is actually still open).
     */
    private boolean isClosedProject = true;
    
    /**
     * Indicates whether the VM was restarted (which we assume is via the debugger). If this
     * is the case, we should not try to instantiate a world until requested by the user.
     */
    private boolean wasRestarted = false;
    
    /**
     * Returns whether the project is closed or not
     * @return isClosedProject  True if the project is closed and false if not
     */
    public boolean isClosedProject() 
    {
        return isClosedProject;
    }
    
    public WorldHandlerDelegateIDE getWorldHandlerDelegate()
    {
        return worldHandlerDelegate;
    }

    /**
     * Indicate whether we want to resize. 
     * 
     * @see #setResizeWhenPossible(boolean)
     * @see #needsResize()
     */
    private boolean resizeWhenPossible = false;
    
    private static GreenfootFrame instance;
    private AskPanel askPanel;
    private AskHandler askHandler;
    
    public static GreenfootFrame getGreenfootFrame(final RBlueJ blueJ, ClassStateManager classStateManager)
    {
        instance = new GreenfootFrame(blueJ, classStateManager);                        
        return instance;
    }
    
    /**
     * Creates a new top level frame with all the GUI components.
     * @param classStateManager 
     */
    private GreenfootFrame(RBlueJ blueJ, ClassStateManager classStateManager)
        throws HeadlessException
    {
        super("Greenfoot");

        this.rBlueJ = blueJ;
        
        LocationTracker.instance(); //force initialisation
        Image icon = BlueJTheme.getApplicationIcon("greenfoot");
        if (icon != null) {
            setIconImage(icon);
        }

        makeFrame(classStateManager);
        addWindowListener(this);
        
        restoreFrameState();

        prepareMacOSApp();
    }
    
    /**
     * Restore the current main window size from the project properties.
     */
    private void restoreFrameState()
    {
        if (project == null) {
            // We don't have a project yet: just use default size
            setBounds(40, 40, 850, 600);
            setResizeWhenPossible(true);
            return;
        }
        
        ProjectProperties projectProperties = project.getProjectProperties();

        try {            
            int x = projectProperties.getInt("mainWindow.x");
            int y = projectProperties.getInt("mainWindow.y");

            int width = projectProperties.getInt("mainWindow.width");
            int height = projectProperties.getInt("mainWindow.height");
            
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            
            if (x > (d.width - 50)) {
                x = d.width - 50;
            }

            if (y > (d.height - 50)) {
                y = d.height - 50;
            }

            setBounds(x, y, width, height);
            setResizeWhenPossible(false);
        } 
        catch (NumberFormatException ecx) {
            // doesn't matter - just use some default size
            setBounds(40, 40, 850, 600);
            setResizeWhenPossible(true);
        }
    }

    
    /**
     * Prepare MacOS specific behaviour (About menu, Preferences menu, Quit menu)
     */
    private Application prepareMacOSApp()
    {
        if (Config.isMacOS()) {
            Application macApp = Application.getApplication();
            macApp.setPreferencesHandler(e -> PreferencesAction.getInstance().actionPerformed(null));
            macApp.setAboutHandler(e -> AboutGreenfootAction.getInstance(GreenfootFrame.this).actionPerformed(null));                    
            macApp.setQuitHandler((e, response) -> exit()); // response.confirmQuit() does not need to be called, since System.exit(0) is called explicitly
            return macApp;
        }
        return null;
    }
    
    /**
     * Open a given project into this frame.
     */
    public void openProject(final GProject project)
    {
        if (isClosedProject) {
            this.project = project;
            worldHandlerDelegate.attachProject(project);
            project.addCompileListener(this);
            setTitle("Greenfoot: " + project.getName());
            enableProjectActions();

            worldCanvas.setVisible(false);
            // Class browser
            constructClassBrowser(project);
            restoreFrameState();

            try {
                ProjectProperties props = project.getProjectProperties();
                int initialSpeed = props.getInt("simulation.speed");
                Simulation.getInstance().setSpeed(initialSpeed);
            }
            catch (NumberFormatException nfe) {
                // If there is no speed info in the properties we don't care...
            }
            
            if (needsResize()) {
                pack();
            }
            // set our project to be this possibly new project
            toggleDebuggerAction.setProject(project);
            toggleSoundAction.setProject(project);
            isClosedProject = false;
            
            Simulation.getInstance().setPaused(true);
            wasRestarted = project.isVmRestarted();

            if (! wasRestarted) {  
                worldHandlerDelegate.instantiateNewWorld();
            }
        }
        updateBackgroundMessage();
    }

    private void constructClassBrowser(final GProject project)
    {
        buildClassBrowser();
        populateClassBrowser(classBrowser, project);
        classBrowser.setVisible(true);
        classBrowser.setActions(newClassAction, importClassAction);
        classScrollPane.setViewportView(classBrowser);
    }
    
    /**
     * Calling this will make the current frame an empty frame.
     */
    public void closeProject()
    {
        setTitle("Greenfoot: ");
        project.removeCompileListener(this);
        project.closeEditors();
        worldCanvas.setVisible(false);
        classBrowser.setVisible(false);
        project = null;
        enableProjectActions();
        repaint();
        isClosedProject = true;
        
        //TODO is next line needed?
        updateBackgroundMessage();
    }

    /**
     * Get the class browser currently embedded in this frame.
     */
    public ClassBrowser getClassBrowser()
    {
        return classBrowser;
    }
    
    /**
     * Get the project showing in this frame. If this frame is empty,
     * will return null.
     */
    public GProject getProject()
    {
        return project;
    }
    
    /**
     * Create the GUI components for this project in the top level frame.
     * This includes opening the project and displaying the project classes.
     * @param classStateManager 
     */
    private void makeFrame(ClassStateManager classStateManager)
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Some first-time initializations
        worldCanvas = new WorldCanvas(null);
        worldCanvas.setWorldSize(200, 100);
        worldCanvas.setVisible(false);
        
        worldHandlerDelegate = new WorldHandlerDelegateIDE(this, inspectorManager, classStateManager);
        WorldHandler.initialise(worldCanvas, worldHandlerDelegate);
        worldHandler = WorldHandler.getInstance();
        worldHandler.addWorldListener(this);
        Simulation.initialize(new SimulationDelegateIDE());
        Simulation sim = Simulation.getInstance();
        sim.attachWorldHandler(worldHandler);
       
        // Build the class browser before building the menu, because
        // some menu actions work on the class browser.
        buildClassBrowser();
        setupActions();
        setJMenuBar(buildMenu(classStateManager));
        setGlassPane(DragGlassPane.getInstance());

        // build the centre panel. this includes the world and the controls
        
        centrePanel = new JPanel(new BorderLayout(4, 4)) {
            @Override
            public boolean isValidateRoot()
            {
                return true;
            }
        };

        sim.addSimulationListener(new SimulationListener() {
            @Override
            public void simulationChanged(SimulationEvent e)
            {
                // If the simulation starts, try to transfer keyboard
                // focus to the world canvas to allow control of Actors
                // via the keyboard
                if (e.getType() == SimulationEvent.STARTED) {
                    worldCanvas.requestFocusInWindow();
                }
            }
        });

        sim.addSimulationListener(SoundFactory.getInstance().getSoundCollection());
        
        worldCanvas.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        JPanel canvasPanel = new JPanel(new CenterLayout());
        canvasPanel.setBorder(BorderFactory.createEtchedBorder());        
        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (e.isPopupTrigger()) {
                    worldHandlerDelegate.showWorldPopupMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (e.isPopupTrigger()) {
                    worldHandlerDelegate.showWorldPopupMenu(e);
                }
            }
        });
        
        messagePanel = new JPanel(new CenterLayout());
        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
        messageLabel = new JLabel("");
        final Font msgFont = messageLabel.getFont().deriveFont(Font.BOLD + Font.ITALIC, 16.0f);
        final Color msgColor = new Color(150,150,150);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setForeground(msgColor);
        messageLabel.setFont(msgFont);
        subPanel.add(messageLabel);
        messageLabel2 = new JLabel("");
        messageLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel2.setForeground(msgColor);
        messageLabel2.setFont(msgFont);
        subPanel.add(messageLabel2);
        subPanel.add(Box.createVerticalStrut(15));
        tooLongRestartButton = new JButton(Config.getString("centrePanel.restartButton.label"));
        tooLongRestartButton.setVisible(false);
        tooLongRestartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        tooLongRestartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try {
                    project.getRProject().restartVM();
                }
                catch (RemoteException e1) {
                    Debug.reportError(e1);
                }
                catch (ProjectNotOpenException e1) {
                    Debug.reportError(e1);
                }                
            }
        });
        subPanel.add(tooLongRestartButton);
        messagePanel.add(subPanel);
        
        JScrollPane worldScrollPane = new JScrollPane(worldCanvas);
        //Stop the world scroll pane scrolling when arrow keys are pressed - stops it interfering with the scenario.
        String[] scrollLabels = new String[]{"unitScrollLeft", "unitScrollRight", "unitScrollUp", "unitScrollDown"};
        for(String scrollLabel : scrollLabels) {
            worldScrollPane.getActionMap().put(scrollLabel, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //Do nothing
                }
            });
        }
        
        JPanel worldAndAskPanel = new JPanel();
        worldAndAskPanel.setLayout(new OverlayLayout(worldAndAskPanel));
        askPanel = new AskPanel();
        askPanel.getComponent().setAlignmentX(0.5f);
        askPanel.getComponent().setAlignmentY(1.0f);
        worldAndAskPanel.add(askPanel.getComponent());
        askHandler = new AskHandler(askPanel, worldCanvas);
        worldScrollPane.setAlignmentX(0.5f);
        worldScrollPane.setAlignmentY(1.0f);
        worldAndAskPanel.add(worldScrollPane);
        

        card=new CardLayout();
        worldBox = new DBox(DBox.Y_AXIS, 0.5f); // scroll pane
        worldBox.setLayout(card);
        worldBox.add(worldAndAskPanel, "worldPanel"); // worldPanel is an ID that refers to the World Panel
        worldBox.add(messagePanel, "messagePanel"); // messagePanel is an ID that refers to the Message Panel

        canvasPanel.add(worldBox);
        // Set the scroll bar increments so that using the scroll wheel works well:
        setScrollIncrements(worldScrollPane);
        worldScrollPane.setOpaque(false);
        // Why are these not opaque? Maybe they have to be on some platforms? looks fine on Mac OS X Leopard.
        worldScrollPane.getViewport().setOpaque(false);
        worldScrollPane.setBorder(null);
        
        centrePanel.add(canvasPanel, BorderLayout.CENTER);
        
        // Execution control
        
        JPanel executionControl = new JPanel();
        executionTwirler = new ExecutionTwirler();
        executionControl.add(executionTwirler);
        Dimension d = executionControl.getPreferredSize();
        d.width = 100;
        executionControl.setPreferredSize(d);
        executionControl.setMaximumSize(d);
        
        // the control panel
        
        controlPanel = new ControlPanel(sim, executionControl);
        controlPanel.setBorder(BorderFactory.createEtchedBorder());

        centrePanel.add(controlPanel, BorderLayout.SOUTH);

        
        // EAST side: project info button and class browser
        
        JPanel eastPanel = new JPanel(new BorderLayout(12, 12));

        JButton shareButton = GreenfootUtil.createButton(shareAction); 
        shareButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource(shareIconFile)));
        shareButton.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        eastPanel.add(shareButton, BorderLayout.NORTH);       
        
        // the class browser 
        
        classScrollPane = new JScrollPane(classBrowser) {
            @Override
            public Dimension getPreferredSize()
            {
                Dimension size = super.getPreferredSize();
                // Always leave room for the vertical scroll bar to appear
                // This stops a horizontal scroll bar getting added when the vertical one appears
                size.width += getVerticalScrollBar().getWidth();
                return size;
            }
            
        };
        setScrollIncrements(classScrollPane);
        classScrollPane.setOpaque(false);
        classScrollPane.getViewport().setOpaque(false);
        classScrollPane.setBorder(BorderFactory.createEtchedBorder());
        eastPanel.add(classScrollPane, BorderLayout.CENTER);
        
        // arrange the major components in the content pane
        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(12, 12));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        contentPane.add(centrePanel, BorderLayout.CENTER);
        contentPane.add(eastPanel, BorderLayout.EAST);

        contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Config.GREENFOOT_SET_PLAYER_NAME_SHORTCUT, "setPlayerAction");
        contentPane.getActionMap().put("setPlayerAction", setPlayerAction);
        
        updateBackgroundMessage();
        pack();
    }

    /**
     * Sets the scroll increments on a scroll pane to be something sensible
     * and useable.
     */
    private static void setScrollIncrements(JScrollPane scrollPane)
    {
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(30);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(30);
    }

    /**
     * Pack the components in this frame.
     * As part of this, try to make sure that the frame does not get too big.
     * If necessary, make it smaller to fit on screen.
     * 
     * <p>Call on event thread only.
     */
    @Override
    public void pack()
    {
        super.pack();
        
        int width = getSize().width;
        int height = getSize().height;
        boolean change = false;
        
        if (width > getMaximumSize().width) {
            width = getMaximumSize().width;
            change = true;
        }
        if (height > getMaximumSize().height) {
            height = getMaximumSize().height;
            change = true;
        }
        if (change) {
            setSize(width, height);
        }
    }
    
    /**
     * Return the preferred size for the frame. The preferred size adds a bit of
     * spacing to the default size to get a margin around the world display.
     */
    @Override
    public  Dimension getPreferredSize()
    {
        Dimension dim = super.getPreferredSize();
        dim.setSize(dim.width + WORLD_MARGIN, dim.height + WORLD_MARGIN);
        return dim;
    }

    /**
     * Build a new (empty) class browser.
     */
    private void buildClassBrowser()
    {
        classBrowser = new ClassBrowser(project, this);
        classBrowser.getSelectionManager().addSelectionChangeListener(this);
        DragGlassPane.getInstance().setSelectionManager(classBrowser.getSelectionManager());
    }

    /**
     * Read the classes from a given project and display them in the class browser.
     */
    private void populateClassBrowser(ClassBrowser classBrowser, GProject project)
    {
        if (project != null) {
            try {
                GPackage pkg = project.getDefaultPackage();

                GClass[] classes = pkg.getClasses(false);
                //add the system classes
                classBrowser.quickAddClass(new ClassView(classBrowser,
                        new GCoreClass(World.class, project), worldHandlerDelegate));
                classBrowser.quickAddClass(new ClassView(classBrowser,
                        new GCoreClass(Actor.class, project), worldHandlerDelegate));

                for (int i = 0; i < classes.length; i++) {
                    GClass gClass = classes[i];
                    classBrowser.quickAddClass(new ClassView(classBrowser, gClass, worldHandlerDelegate));
                }

                classBrowser.updateLayout();
            }
            catch (Exception exc) {
                //Debug.reportError("Could not open classes in scenario", exc);
                exc.printStackTrace();
            }
        }
    }

    private void setupActions()
    {
        newClassAction = new NewClassAction(this, worldHandlerDelegate);
        saveProjectAction = new SaveProjectAction(this);
        saveAsAction = new SaveAsAction(this, rBlueJ);
        showReadMeAction = new ShowReadMeAction(this);
        saveWorldAction = worldHandlerDelegate.getSaveWorldAction();
        setPlayerAction = new SetPlayerAction(this);
        exportProjectAction = new ExportProjectAction(this, false);
        shareAction = new ExportProjectAction(this, true);
        importClassAction = new ImportClassAction(this, worldHandlerDelegate);
        pasteImageAction = new PasteImageAction(this);
        closeProjectAction = new CloseProjectAction(this);
        removeSelectedClassAction = new RemoveSelectedClassAction(this);
        convertToJavaSelectedClassAction = new ConvertToJavaSelectedClassAction(this);
        removeSelectedClassAction.setEnabled(false);
    }
    
    /**
     * Build the menu bar.
     * @param classStateManager
     */
    private JMenuBar buildMenu(ClassStateManager classStateManager)
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu scenarioMenu = addMenu(Config.getString("menu.scenario"), menuBar, 's');

        addMenuItem(new NewWizardScenarioAction(SourceType.Stride), scenarioMenu, KeyEvent.VK_F, false, KeyEvent.VK_F);
        addMenuItem(new NewWizardScenarioAction(SourceType.Java), scenarioMenu, KeyEvent.VK_J, false, KeyEvent.VK_J);
        addMenuItem(OpenProjectAction.getInstance(), scenarioMenu, KeyEvent.VK_O, false, KeyEvent.VK_O);
        
        recentProjectsMenu = new JMenu(Config.getString("menu.openRecent"));
        scenarioMenu.add(recentProjectsMenu);
        updateRecentProjects(classStateManager);
        
        addMenuItem(closeProjectAction, scenarioMenu, KeyEvent.VK_W, false, KeyEvent.VK_C);
        addMenuItem(saveProjectAction, scenarioMenu, KeyEvent.VK_S, false, KeyEvent.VK_S);
        addMenuItem(saveAsAction, scenarioMenu, -1, false, -1);
        scenarioMenu.addSeparator();
        addMenuItem(showReadMeAction, scenarioMenu, -1, false, -1);
        addMenuItem(exportProjectAction, scenarioMenu, KeyEvent.VK_E, false, KeyEvent.VK_E);

        if(! Config.isMacOS()) {
            scenarioMenu.addSeparator();
            addMenuItem(QuitAction.getInstance(), scenarioMenu, KeyEvent.VK_Q, false, KeyEvent.VK_Q);
        }
        
        JMenu editMenu = addMenu(Config.getString("menu.edit"), menuBar, 'e');
        
        addMenuItem(newClassAction, editMenu, KeyEvent.VK_N, false, KeyEvent.VK_N);
        addMenuItem(importClassAction, editMenu, KeyEvent.VK_I, false, KeyEvent.VK_I);
        addMenuItem(removeSelectedClassAction, editMenu, KeyEvent.VK_D, false, KeyEvent.VK_R);
        addMenuItem(convertToJavaSelectedClassAction, editMenu, KeyEvent.VK_Y, false, KeyEvent.VK_Y);
        editMenu.addSeparator();
        addMenuItem(pasteImageAction, editMenu, KeyEvent.VK_V, false, KeyEvent.VK_V);
                
        if (!Config.usingMacScreenMenubar()) { // no "Preferences" here for
            // Mac
            editMenu.addSeparator();
            addMenuItem(PreferencesAction.getInstance(), editMenu, KeyEvent.VK_COMMA, false, KeyEvent.VK_COMMA);
        }
        
        JMenu ctrlMenu = addMenu(Config.getString("menu.controls"), menuBar, 'c');
        
        addMenuItem(RunOnceSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_A, false, KeyEvent.VK_A);
        addMenuItem(RunSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_R, false, KeyEvent.VK_R);
        addMenuItem(PauseSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_R, true, KeyEvent.VK_P);
        addMenuItem(ResetWorldAction.getInstance(), ctrlMenu, KeyEvent.VK_T, false, KeyEvent.VK_T);

        RunOnceSimulationAction.getInstance().attachListener(worldHandlerDelegate);
        RunSimulationAction.getInstance().attachListener(worldHandlerDelegate);

        ctrlMenu.addSeparator();
        toggleDebuggerAction = new ToggleDebuggerAction(Config.getString("menu.debugger"), project);
        createCheckboxMenuItem(toggleDebuggerAction, false, ctrlMenu, KeyEvent.VK_B, false, KeyEvent.VK_B);
        
        toggleSoundAction = new ToggleSoundAction(Config.getString("menu.soundRecorder"), project);
        createCheckboxMenuItem(toggleSoundAction, false, ctrlMenu, KeyEvent.VK_U, false, KeyEvent.VK_U);
        addMenuItem(saveWorldAction, ctrlMenu, -1, false, KeyEvent.VK_W);
        
        JMenu helpMenu = addMenu(Config.getString("menu.help"), menuBar, 'h');
        
        if(! Config.isMacOS()) {
            addMenuItem(AboutGreenfootAction.getInstance(this), helpMenu, -1, false, KeyEvent.VK_A);
        }
        addMenuItem(ShowCopyrightAction.getInstance(this), helpMenu, -1, false, -1);
        helpMenu.addSeparator();
        addMenuItem(new ShowApiDocAction(Config.getString("menu.help.classDoc")), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.javadoc"), Config.getPropString("greenfoot.url.javaStdLib")), helpMenu, -1, false, -1);
        helpMenu.addSeparator();
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.tutorial"), Config.getPropString("greenfoot.url.tutorial")), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.website"), Config.getPropString("greenfoot.url.greenfoot")), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.moreScenarios"), Config.getPropString("greenfoot.url.scenarios")), helpMenu, -1, false, -1);
        helpMenu.addSeparator();
        addMenuItem(new ShowWebsiteAction(Config.getPropString("greenfoot.gameserver.name"), Config.getPropString("greenfoot.gameserver.address")), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.discuss"), Config.getPropString("greenfoot.url.discuss")), helpMenu, -1, false, -1);
        
        return menuBar;
    }

    /** 
     * Add a menu to a menu bar.
     */
    private static JMenu addMenu(String name, JMenuBar menubar, char mnemonic)
    {
        JMenu menu = new JMenu(name);
        if(!Config.isMacOS()) {
            menu.setMnemonic(mnemonic);
        }
        menubar.add(menu);
        return menu;
    }

    /** 
     * Add a menu item to a menu.
     */
    private static void addMenuItem(Action action, JMenu menu, int accelKey, boolean shift, int mnemonicKey)
    {
        if(accelKey != -1) {
            if(shift) {
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelKey, shiftAccelModifier));
            }
            else {
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelKey, accelModifier));
            }
        }
        if(!Config.isMacOS() && mnemonicKey != -1) {
            action.putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonicKey));
        }
        menu.add(action);
    }

    /**
     * Adds a new checkbox menu item to the {@link JMenu} provided. 
     * Uses the {@link ToggleAction} action to setup the changing 
     * selected state of the action, often determined by events elsewhere 
     * in the BlueJ/Greenfoot code.
     * 
     * @param action        To be added to the {@link Menu}.
     * @param selected      Default state of the action if its {@link ButtonModel} is null.
     * @param menu          That the {@link ToggleAction} will be added to.
     * @param accelKey      Quick keyboard shortcut for this action.
     * @param shift         Used to determine if the accelKey needs shift pressed to happen
     * @param mnemonicKey   Quick keyboard shortcut via the menu for this action.
     */
    private static void createCheckboxMenuItem(ToggleAction action, boolean selected, JMenu menu, int accelKey, boolean shift, int mnemonicKey)
    {
        if(accelKey != -1) {
            if(shift) {
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelKey, shiftAccelModifier));
            }
            else {
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelKey, accelModifier));
            }
        }
        if(!Config.isMacOS() && mnemonicKey != -1) {
            action.putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonicKey));
        }
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
        ButtonModel bm = action.getToggleModel();
        if (bm == null) {
            item.setSelected(selected);
        }
        else {
            item.setModel(bm);
        }
        menu.add(item);
    }
    
    /**
     * Update the 'Open Recent' menu, trying to include
     * the current project as the first item where possible.
     * @param classStateManager
     */
    private void updateRecentProjects(ClassStateManager classStateManager)
    {
        JMenuItem item = null;
        
        // can only add in the current project if there is a current project
        if (classStateManager != null && classStateManager.getProject() != null) {
            String currentName = classStateManager.getProject().getDir().getPath();
            item = new JMenuItem(currentName);
            item.addActionListener(OpenRecentProjectAction.getInstance());
            recentProjectsMenu.add(item);
            recentProjectsMenu.addSeparator();
        }
        
        List<?> projects = PrefMgr.getRecentProjects();
        for (Iterator<?> it = projects.iterator(); it.hasNext();) {
            item = new JMenuItem((String)it.next());
            item.addActionListener(OpenRecentProjectAction.getInstance());
            recentProjectsMenu.add(item);
        }
    }

    /**
     * Enable/disable the project specific actions, depending on whether a
     * project is currently open.
     */
    private void enableProjectActions() 
    {
        boolean state = (project != null);
    
        closeProjectAction.setEnabled(state);
        saveProjectAction.setEnabled(state);
        saveAsAction.setEnabled(state);
        newClassAction.setEnabled(state);
        importClassAction.setEnabled(state);
        pasteImageAction.setEnabled(state);
        showReadMeAction.setEnabled(state);
        saveWorldAction.setEnabled(state);
        exportProjectAction.setEnabled(state);
        shareAction.setEnabled(state);
        
        // Disable simulation buttons
        if (state == false) {
            WorldHandler.getInstance().discardWorld();
            removeSelectedClassAction.setEnabled(false);
        }

        //Initially convert to Java has to be false
        convertToJavaSelectedClassAction.setEnabled(false);
    }

    /**
     * Quit Greenfoot.
     */
    private void exit()
    {
        super.dispose();
        GreenfootMain.closeAll();
    }

    /**
     * This frame should never be disposed (at least not until the program is
     * closed). BlueJ disposes all windows when compiling, so dispose is
     * overridden to avoid it for this frame. Be aware of this when it should
     * really shut down!
     * 
     * @see java.awt.Window#dispose()
     * @see #exit()
     */
    @Override
    public void dispose()
    {
        // I will not close :-)
    }

    /**
     * Returns the maximum size, which is the size of the screen.
     */
    @Override
    public Dimension getMaximumSize()
    {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
    
    /**
     * Returns true if we need to resize the frame. Based on whether the world
     * has changed size or we have specifically asked for a resize by setting resizeWhenPossible.
     * 
     * @see #setResizeWhenPossible(boolean)
     * @return true, if we need a resize.
     */
    private boolean needsResize()
    {
        // Note that worldDimensions is actually the dimensions of the canvas for the _last_ world.
        // The dimensions of the canvas for the current world is held in 'dim'.

        Dimension dim = worldCanvas.getPreferredSize();
        if (resizeWhenPossible) {
            return true;
        }
        else if (worldDimensions == null) {
            // If the worldDimensions are null here, it means that we set the
            // size specifically when we created the frame.
            return false;
        }
        else if ( worldDimensions.width < dim.width || worldDimensions.height < dim.height ) {
            return true;
        }
        return false;
    }
    
    /**
     * Resizes the frame to its preferred size by running pack().
     * <p>
     * Should be run on the event thread.
     */
    private void resize()
    {
        setResizeWhenPossible(false);
        pack();
    }

    /**
     * Indicate whether we want to resize the next time we get new information
     * about the size, and hence might want to do a resize of the entire frame.
     */
    public void setResizeWhenPossible(boolean b)
    {
        worldDimensions = null;
        this.resizeWhenPossible = b;
    }
    
    public void updateBackgroundMessage()
    {
        String message = "";
        String message2 = "";
        if(worldCanvas.isVisible()) {
            card.show(worldBox, "worldPanel");
        }
        else if(!isCompiling){
            if (project == null) {
                message = Config.getString("centrePanel.message.openScenario");
            }
            else {
                boolean noWorldClassFound = true;
                boolean noCompiledWorldClassFound = true;
                GClass[] projectClasses = project.getDefaultPackage().getClasses(false);
                for (GClass projectClass : projectClasses) {
                    if (projectClass.isWorldSubclass()) {
                        noWorldClassFound = false;
                        if (projectClass.isCompiled()) {
                            noCompiledWorldClassFound = false;
                        }
                    }
                }

                tooLongRestartButton.setVisible(false);
                if (noWorldClassFound) {
                    message = Config.getString("centrePanel.message.createWorldClass");
                }
                else if (noCompiledWorldClassFound) {
                    message = Config.getString("centrePanel.message.compile");
                }
                else if (worldHandlerDelegate.initialisationError()) {
                    message = Config.getString("centrePanel.message.error1");
                    message2 = Config.getString("centrePanel.message.error2");
                }
                else if (initialisingForTooLong()) {
                    if (this.isFocused()) {
                        message = Config.getString("centrePanel.message.initialisingTooLong1");
                        message2 = Config.getString("centrePanel.message.initialisingTooLong2");
                        tooLongRestartButton.setVisible(true);
                    }
                    else {
                        message = Config.getString("centrePanel.message.notFocused");
                    }
                }
                else if (worldHandlerDelegate.initialising()) {
                    message = Config.getString("centrePanel.message.initialising");
                }
                else if (wasRestarted) {
                    message = Config.getString("centrePanel.message.afterRestarting1");
                    message2 = Config.getString("centrePanel.message.afterRestarting2");
                }
                else if (worldHandlerDelegate.isMissingConstructor()) {
                    message = Config.getString("centrePanel.message.missingWorldConstructor1");
                    message2 = Config.getString("centrePanel.message.missingWorldConstructor2");
                    worldHandlerDelegate.setMissingConstructor(false);
                }
            }
            card.show(worldBox, "messagePanel");
        }
        messageLabel.setText(message);
        messageLabel2.setText(message2);
    }
    
    /**
     * Reset/restart the watchdog timer. Once it times out, the "execution twirler" will be
     * enabled and (potentially) a message will be displayed in the world background.
     */
    public void resetTimer()
    {
        startedInitialisingAt = System.currentTimeMillis();
        if (timer == null) {
            timer = new Timer(WORLD_INITIALISING_TIMEOUT, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    updateBackgroundMessage();
                    executionTwirler.setEnabled(true);
                }
            });
            timer.setRepeats(false);
        }
        
        timer.restart();
    }
    
    /**
     * Stop the watchdog timer.
     */
    public void stopTimer()
    {
        timer.stop();
        executionTwirler.setEnabled(false);
    }

    /**
     * Returns true if the world is currently initialising, and has gone behind its timeout
     */
    public boolean initialisingForTooLong()
    {
        return worldHandlerDelegate.initialising()
                && System.currentTimeMillis() > startedInitialisingAt + WORLD_INITIALISING_TIMEOUT;
    }
    
    // ----------- WindowListener interface -----------
    
    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e)
    {
        GreenfootMain.closeProject(this, true);
    }

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {
        if (!isClosedProject())
        {
            Arrays.stream(project.getDefaultPackage().getClasses(false)).forEach(GClass::cancelFreshState);
            if (!wasRestarted && !WorldHandler.getInstance().hasWorld()) {
                WorldHandler.getInstance().instantiateNewWorld();
            }
        }
    }

    @Override
    public void windowDeactivated(WindowEvent e) {}

    // ----------- CompileListener interface -----------
    
    @Override
    public void compileStarted(RCompileEvent event)
    {
        EventQueue.invokeLater(() -> {
            Image snapshot = getWorldGreyedSnapShot();
            WorldHandler.getInstance().discardWorld();
            if (snapshot != null) {
                worldCanvas.setOverrideImage(snapshot);
            }
            this.isCompiling = true;
        });
    }

    private Image getWorldGreyedSnapShot()
    {
        BufferedImage screenShot = WorldHandler.getInstance().getSnapShot();
        if (screenShot != null) {
            GreenfootUtil.convertToGreyImage(screenShot);
            // With stripes           
            Utility.stripeRect(screenShot.getGraphics(), 0, 0, screenShot.getWidth(), screenShot.getHeight(), 40, 1, Color.GRAY);
        }
        return screenShot;
    }

    @Override
    public void compileError(RCompileEvent event) { }

    @Override
    public void compileWarning(RCompileEvent event) { }

    @Override
    public void compileSucceeded(RCompileEvent event)
    {
        EventQueue.invokeLater(() -> {
            if (! wasRestarted && GreenfootFrame.this.isActive()) {
                WorldHandler.getInstance().instantiateNewWorld();
            }
            classBrowser.repaint();
            isCompiling = false;
            updateBackgroundMessage();
        });
    }

    @Override
    public void compileFailed(RCompileEvent event)
    {
        EventQueue.invokeLater(() -> {
            isCompiling = false;
            updateBackgroundMessage();
        });
    }
    
    // ----------- end of WindowListener interface -----------
    
    // ----------- WorldListener interface -------------
    
    @Override
    public void worldCreated(WorldEvent e)
    {
        stopTimer();
        
        // Having instantiated a world successfully, we can assume it's safe to
        // try to do so again:
        wasRestarted = false;
        
        World newWorld = e.getWorld();
        if (needsResize() && newWorld != null) {
            // ensure we don't lose fullscreen on resize
            final int state = getExtendedState();
            if ( state != MAXIMIZED_BOTH ) {
                resize();
            }
        }
        worldCanvas.setVisible(true);
        centrePanel.revalidate();
        worldDimensions = worldCanvas.getPreferredSize();
        
        updateBackgroundMessage();
    }
    
    @Override
    public void worldRemoved(WorldEvent e)
    {
        inspectorManager.removeAllInspectors();
        updateBackgroundMessage();
    }

    // ------------- end of WorldListener interface ------------
    
    // ------------- SelectionListener interface ---------------
    
    @Override
    public void selectionChange(Selectable source)
    {
        convertToJavaSelectedClassAction.setEnabled(false);
        if (source instanceof ClassView) {
            ClassView classView = (ClassView)source;
            if(classView.getRealClass() == null) {
                removeSelectedClassAction.setEnabled(true);
            }
            else if(! (classView.getRealClass().getName().equals("greenfoot.Actor")) &&
                    ! (classView.getRealClass().getName().equals("greenfoot.World")))  {
                removeSelectedClassAction.setEnabled(true);
                convertToJavaSelectedClassAction.setEnabled(classView.getGClass().getSourceType() == SourceType.Stride);
            }
            else {
                removeSelectedClassAction.setEnabled(false);
            }
        }
        else {
            removeSelectedClassAction.setEnabled(false);
        }
        updateBackgroundMessage();
    }
    
    // ------------- end of SelectionListener interface --------

    /**
     * Get a reference to the inspector manager for the project shown in this frame.
     */
    public GreenfootInspectorManager getInspectorManager()
    {
        return inspectorManager;
    }
    
    /**
     * Asks the user to input a String.  Should be called from the EDT.  Returns a Callable
     * that you should call from a non-EDT thread, which will wait for the answer and then
     * return it.
     */
    public Callable<String> ask(String prompt)
    {
        return askHandler.ask(prompt, worldDimensions.width);
    }
    
    public void stopWaitingForAnswer()
    {
        askHandler.stopWaitingForAnswer();
    }
}
