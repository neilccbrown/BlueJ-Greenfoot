package greenfoot.gui;

import greenfoot.GreenfootWorld;
import greenfoot.actions.AboutGreenfootAction;
import greenfoot.actions.CloseProjectAction;
import greenfoot.actions.CompileAllAction;
import greenfoot.actions.CompileClassAction;
import greenfoot.actions.EditClassAction;
import greenfoot.actions.NewProjectAction;
import greenfoot.actions.OpenProjectAction;
import greenfoot.actions.SaveProjectAction;
import greenfoot.actions.ShowCopyrightAction;
import greenfoot.actions.ShowWebsiteAction;
import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.core.Greenfoot;
import greenfoot.core.LocationTracker;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.CompileListener;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.Config;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import greenfoot.actions.NYIAction;
import greenfoot.actions.RunOnceSimulationAction;
import greenfoot.actions.RunSimulationAction;

/**
 * The main frame of the greenfoot application
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootFrame.java 3890 2006-03-27 16:04:42Z mik $
 */
public class GreenfootFrame extends JFrame
    implements WindowListener, CompileListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private static final int accelModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private static final int shiftAccelModifier = accelModifier | KeyEvent.SHIFT_MASK;

    private ClassBrowser classBrowser;
    private ControlPanel controlPanel;
    
    private Thread projectOpenThread;

    /**
     * Creates a new frame with all the basic components (menus...)
     *  
     */
    public GreenfootFrame(RBlueJ blueJ, final GProject project)
        throws HeadlessException, ProjectNotOpenException, RemoteException
    {
        super("greenfoot: " + project.getName());
        try {
            //HACK to avoid error in class diagram (getPreferredSize stuff) on
            // windows, we use cross platform look and feel
            if (Config.isWinOS()) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (UnsupportedLookAndFeelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LocationTracker.instance(); //force initialisation
        setSize(400, 300);
        URL iconFile = this.getClass().getClassLoader().getResource("greenfoot-icon.gif");
        ImageIcon icon = new ImageIcon(iconFile);
        setIconImage(icon.getImage());
        buildUI();
        addWindowListener(this);
        Greenfoot.getInstance().addCompileListener(this);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        prepareMacOSApp();
        
        projectOpenThread = new Thread() {
            public void run() {
                openProject(project);
                GreenfootFrame.this.setCursor(Cursor.getDefaultCursor());
            }
        };
        projectOpenThread.start();
    }
    
    public void waitForProjectOpen() {
        try {
            projectOpenThread.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepare MacOS specific behaviour (About menu, Preferences menu, Quit
     * menu)
     */
    private Application prepareMacOSApp()
    {
        Application macApp = new Application();
        macApp.setEnabledPreferencesMenu(true);
        macApp.addApplicationListener(new ApplicationAdapter() {
            public void handleAbout(ApplicationEvent e)
            {
                AboutGreenfootAction.getInstance(GreenfootFrame.this).actionPerformed(null);
                e.setHandled(true);
            }

            public void handlePreferences(ApplicationEvent e)
            {
               // PreferencesAction.getInstance().actionPerformed(getMostRecent());
               // e.setHandled(true);
            }

            public void handleQuit(ApplicationEvent e)
            {
                exit();
                e.setHandled(true);
            }
        });

        return macApp;
    }
    
    private void buildUI()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        setJMenuBar(buildMenu());
        setGlassPane(DragGlassPane.getInstance());
    }

    private void openProject(GProject project)
    {
        classBrowser = buildClassBrowser();
        classBrowser.setBackground(Color.WHITE);
        JScrollPane classScrollPane = new JScrollPane(classBrowser);
        buildWorld(project);
        WorldHandler worldHandler = WorldHandler.instance();
        Simulation.initialize(worldHandler);
        Simulation sim = Simulation.getInstance();
        WorldCanvas worldCanvas = worldHandler.getWorldCanvas();
        JScrollPane worldScrollPane = new JScrollPane(worldCanvas);
        worldScrollPane.getViewport().setOpaque(true);
        worldCanvas.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        JPanel worldPanel = new JPanel(new BorderLayout(4, 4));

        controlPanel = new ControlPanel(sim);
        controlPanel.setBorder(BorderFactory.createEtchedBorder());        
        worldHandler.addWorldListener(controlPanel);

        //todo this should be moved to WorldCanvas becuase it changes when new
        // worlds are created
        worldPanel.add(worldHandler.getWorldTitle(), BorderLayout.NORTH);
        worldPanel.add(worldScrollPane, BorderLayout.CENTER);
        worldPanel.add(controlPanel, BorderLayout.SOUTH);

        JPanel rightPane = new JPanel(new BorderLayout());
        rightPane.add(classScrollPane, BorderLayout.CENTER);
        Box buttonPanel = new Box(BoxLayout.Y_AXIS);
        buttonPanel.setAlignmentX(Box.CENTER_ALIGNMENT);

        JButton button = new JButton(CompileAllAction.getInstance());
        Dimension pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
        buttonPanel.add(button);

        buttonPanel.add(Box.createVerticalStrut(5));

        //TODO create proper implementation.
        Action newClassAction = new AbstractAction("New Class...") {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                GreenfootFrame.this.showNYIMessage();
            }

        };
        button = new JButton(newClassAction);
        pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
        
        buttonPanel.add(button);

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPane.add(buttonPanel, BorderLayout.SOUTH);

//        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
//        splitPane.setLeftComponent(worldPanel);
//        splitPane.setRightComponent(rightPane);
//        splitPane.setResizeWeight(.9);
//        splitPane.resetToPreferredSizes();
//
//        getContentPane().add(splitPane, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout(6,6));
        getContentPane().add(worldPanel, BorderLayout.CENTER);
        getContentPane().add(rightPane, BorderLayout.EAST);
        Border emptyBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8);
        ((JPanel)getContentPane()).setBorder(emptyBorder);

        instantiateNewWorld(classBrowser);
        pack();

        worldHandler.setSelectionManager(classBrowser.getSelectionManager());

    }

    public void showNYIMessage()
    {
        JOptionPane.showMessageDialog(this, "Not Yet Implemented - sorry.");
    } 

    public void pack()
    {
//        splitPane.resetToPreferredSizes();
        super.pack();
        super.pack();   // this seems a bug: if not called twice, it gets the size wrong...
        
        int width = getSize().width;
        int height = getSize().height;
        boolean needChange = false;
        
        if (width > getMaximumSize().width) {
            width = getMaximumSize().width;
            needChange = true;
        }
        if (height > getMaximumSize().height) {
            height = getMaximumSize().height;
            needChange = true;
        }
        if (needChange) {
            setSize(width, height);
        }
    }

    /**
     * Tries to instantiate a new world
     * 
     * @param classBrowser
     */
    private GreenfootWorld instantiateNewWorld(ClassBrowser classBrowser)
    {
        //init a random world
        Iterator worldClasses = classBrowser.getWorldClasses();

        while (worldClasses.hasNext()) {
            ClassView classView = (ClassView) worldClasses.next();
            if (!classView.getClassName().equals("GreenfootWorld")) {
                classView.reloadClass();
                Object o = classView.createInstance();
                if (o instanceof GreenfootWorld) {
                    GreenfootWorld world = (GreenfootWorld) o;
                    if (world != null) {
                        WorldHandler.instance().setWorld(world);
                    }
                    return world;
                }
            }
        }

        return null;
    }


    private void buildWorld(GProject project)
    {
        GreenfootWorld world = null;
        WorldCanvas worldCanvas = new WorldCanvas(world);
        WorldHandler.initialise(project, worldCanvas, world);
    }

    /**
     * Build the class browser.
     */
    private ClassBrowser buildClassBrowser()
    {
        ClassBrowser classBrowser = new ClassBrowser();
        classBrowser.addCompileClassAction(CompileClassAction.getInstance());
        classBrowser.addEditClassAction(EditClassAction.getInstance());

        try {
            //pkg = Greenfoot.getInstance().getCurrentPackage();
            GProject project = Greenfoot.getInstance().getProject();
            //	TODO when project is empty (a new project) the systemclasses get
            // loaded twice
            GPackage pkg = project.getDefaultPackage();

            GClass[] classes = pkg.getClasses();
            //add the system classes
            GPackage sysPkg = project.getGreenfootPackage();
            if (sysPkg == null) {
                sysPkg = project.newPackage("greenfoot");
            }

            GClass[] gClasses = sysPkg.getClasses();
            for (int i = 0; i < gClasses.length; i++) {
                GClass gClass = gClasses[i];
                classBrowser.quickAddClass(gClass);
            }

            for (int i = 0; i < classes.length; i++) {
                GClass gClass = classes[i];
                classBrowser.quickAddClass(gClass);
            }
            
            classBrowser.updateLayout();
            
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        catch (PackageAlreadyExistsException e) {
            e.printStackTrace();
        }

        Border insideBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        classBrowser.setBorder(insideBorder);
        return classBrowser;
    }

    private JMenuBar buildMenu()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu projectMenu = addMenu("Project", menuBar, 'p');
        
        addMenuItem(NewProjectAction.getInstance(), projectMenu, KeyEvent.VK_N, false, KeyEvent.VK_N);
        addMenuItem(OpenProjectAction.getInstance(), projectMenu, KeyEvent.VK_O, false, KeyEvent.VK_O);
//        addMenuItem(new NYIAction("Open Recent...", this), projectMenu, -1, false, -1);
        addMenuItem(CloseProjectAction.getInstance(), projectMenu, KeyEvent.VK_W, false, KeyEvent.VK_C);
        addMenuItem(SaveProjectAction.getInstance(), projectMenu, KeyEvent.VK_S, false, KeyEvent.VK_S);
        addMenuItem(new NYIAction("Save As...", this), projectMenu, KeyEvent.VK_S, true, -1);
        projectMenu.addSeparator();
        addMenuItem(new NYIAction("Page Setup...", this), projectMenu, KeyEvent.VK_P, true, -1);
        addMenuItem(new NYIAction("Print...", this), projectMenu, KeyEvent.VK_P, false, KeyEvent.VK_P);
        
        JMenu ctrlMenu = addMenu("Controls", menuBar, 'c');
        
        addMenuItem(RunOnceSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_A, false, KeyEvent.VK_A);
        addMenuItem(RunSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_R, false, KeyEvent.VK_R);
        addMenuItem(new NYIAction("Increase Speed", this), ctrlMenu, KeyEvent.VK_PLUS, false, KeyEvent.VK_PLUS);
        addMenuItem(new NYIAction("Decrease Speed", this), ctrlMenu, KeyEvent.VK_MINUS, false, KeyEvent.VK_MINUS);
        ctrlMenu.addSeparator();
        addMenuItem(CompileAllAction.getInstance(), ctrlMenu, KeyEvent.VK_K, false, -1);
        
        JMenu helpMenu = addMenu("Help", menuBar, 'h');
        
        addMenuItem(AboutGreenfootAction.getInstance(this), helpMenu, -1, false, KeyEvent.VK_A);
        addMenuItem(ShowCopyrightAction.getInstance(this), helpMenu, -1, false, -1);
        helpMenu.addSeparator();
        addMenuItem(new ShowWebsiteAction("Greenfoot Web Site", "http://www.greenfoot.org"), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction("Greenfoot Tutorial", "http://www.greenfoot.org/doc/tutorial.html"), helpMenu, -1, false, -1);
        
        return menuBar;
    }

    /** 
     * Add a menu to a menu bar.
     */
    private JMenu addMenu(String name, JMenuBar menubar, char mnemonic)
    {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        menubar.add(menu);
        return menu;
    }

    /** 
     * Add a menu item to a menu.
     */
    private void addMenuItem(Action action, JMenu menu, int accelKey, boolean shift, int mnemonicKey)
    {
        if(accelKey != -1) {
            if(shift)
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelKey, shiftAccelModifier));
            else
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelKey, accelModifier));
        }
        if(mnemonicKey != -1)
            action.putValue(Action.MNEMONIC_KEY, new Integer(mnemonicKey));
        menu.add(action);
    }

    /**
     * 
     * This frame should never be disposed (at least not until the program is
     * closed). BlueJ disposes all windows when compiling, so dispose is
     * overridden to avoid it for this frame. Be aware of this when it should
     * really shut down!
     * 
     * @see java.awt.Window#dispose()
     * @see #exit()
     */
    public void dispose()
    {
    //I will not close :-)
    }

    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
        logger.info("WindowClosing");
        exit();
    }

    private void exit()
    {
        super.dispose();
        Greenfoot.getInstance().closeThisInstance();
    }

    public void windowClosed(WindowEvent e)
    {
        logger.info("WindowClosed");
    }

    public void windowIconified(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowActivated(WindowEvent e)
    {}

    public void windowDeactivated(WindowEvent e)
    {}

    public void compileStarted(RCompileEvent event)
    {        
        WorldHandler.instance().reset();
    }

    public void compileError(RCompileEvent event)
    {

    }

    public void compileWarning(RCompileEvent event)
    {

    }

    public void compileSucceeded(RCompileEvent event)
    {
        instantiateNewWorld(classBrowser);
        classBrowser.rebuild();
        pack();
    }

    public void compileFailed(RCompileEvent event)
    {}

    
    /**
     * Returns the maximum size, which is the size of the screen.
     */
    public Dimension getMaximumSize()
    {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
}