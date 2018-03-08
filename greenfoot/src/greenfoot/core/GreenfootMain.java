/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2013,2014,2015,2016,2017,2018  Poul Henriksen and Michael Kolling
 
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
package greenfoot.core;

import bluej.Boot;
import bluej.collect.DataSubmissionFailedDialog;
import greenfoot.event.CompileListener;
import greenfoot.event.CompileListenerForwarder;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.MessageDialog;
import greenfoot.importer.scratch.ScratchImport;
import greenfoot.platforms.ide.ActorDelegateIDE;
import greenfoot.util.FileChoosers;
import greenfoot.util.Version;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.rmi.ServerError;
import java.rmi.ServerException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import rmiextension.wrappers.event.RApplicationListenerImpl;
import rmiextension.wrappers.event.RCompileEvent;
import rmiextension.wrappers.event.RProjectListener;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.SourceType;
import bluej.pkgmgr.GreenfootProjectFile;
import bluej.pkgmgr.Project;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.Utility;
import bluej.views.View;

/**
 * The main class for greenfoot. This is a singelton (in the JVM). Since each
 * project is opened in its own JVM there can be several Greenfoot instances,
 * but each will be in its own JVM so it is effectively a singleton.
 * 
 * @author Poul Henriksen
 */
public class GreenfootMain extends Thread implements CompileListener, RProjectListener
{
    public static enum VersionInfo {
        /** The project API version matches the greenfoot API version */
        VERSION_OK,
        /** The project API version was different, and has been updated */
        VERSION_UPDATED,
        /** The project was not a greenfoot project, or the user chose to cancel the open */
        VERSION_BAD }
    
    public static class VersionCheckInfo
    {
        public final VersionInfo versionInfo;
        public final boolean removeAWTImports;

        public VersionCheckInfo(VersionInfo versionInfo, boolean removeAWTImports)
        {
            this.removeAWTImports = removeAWTImports;
            this.versionInfo = versionInfo;
        }
    }

    /** Version of the API for this Greenfoot release. */
    private static Version version = null;

    /** Greenfoot is a singleton - this is the instance. */
    private static GreenfootMain instance;

    /** The connection to BlueJ via RMI */
    private RBlueJ rBlueJ;

    /** The main frame of greenfoot. */
    private GreenfootFrame frame;

    /** The project this Greenfoot singelton refers to. */
    private GProject project;

    /** The package this Greenfoot singelton refers to. */
    private GPackage pkg;

    /** The path to the dummy startup project */
    private File startupProject;

    /**
     * Forwards compile events to all the compileListeners that has registered
     * to reccieve compile events.
     */
    private CompileListenerForwarder compileListenerForwarder;
    private List<CompileListener> compileListeners = new LinkedList<CompileListener>();

    /** The class state manager notifies GClass objects when their compilation state changes */
    private ClassStateManager classStateManager;

    /** Filter that matches class files */
    private static FilenameFilter classFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.toLowerCase().endsWith(".class");
        }
    };

    private ClassLoader currentLoader;

    // ----------- static methods ------------

    /**
     * Initializes the singleton. This can only be done once - subsequent calls
     * will have no effect.
     * 
     * @param rBlueJ   remote BlueJ instance
     * @param pkg      remote reference to the unnamed package of the project corresponding to this Greenfoot instance
     * @param shmFilePath The path to the shared-memory file to be mmap-ed for communication
     * @param wizard   whether to run the "new project wizard"
     * @param sourceType  default source type for the new project
     */
    public static void initialize(RBlueJ rBlueJ, RPackage pkg, String shmFilePath, boolean wizard, SourceType sourceType)
    {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        if (instance == null) {
            try {
                instance = new GreenfootMain(rBlueJ, pkg.getProject(), shmFilePath, wizard, sourceType);
            }
            catch (ProjectNotOpenException pnoe) {
                // can't happen
                Debug.reportError("Getting remote project", pnoe);
            }
            catch (RemoteException re) {
                // shouldn't happen
                Debug.reportError("Getting remote project", re);
            }
        }
    }

    /**
     * Gets the singleton.
     */
    public static GreenfootMain getInstance()
    {
        return instance;
    }

    // ----------- instance methods ------------

    /**
     * Contructor is private. This class is initialised via the 'initialize'
     * method (above).
     */
    private GreenfootMain(final RBlueJ rBlueJ, final RProject proj, String shmFilePath, boolean wizard, SourceType sourceType)
    {
        instance = this;
        this.rBlueJ = rBlueJ;
        currentLoader = ExecServer.getCurrentClassLoader();
        addCompileListener(this);
        try {
            // determine the path of the startup project
            File startupProj = rBlueJ.getSystemLibDir();
            startupProj = new File(startupProj, "greenfoot");
            startupProject = new File(startupProj, "startupProject");

            this.project = GProject.newGProject(proj);
            addCompileListener(project);
            this.pkg = project.getDefaultPackage();
            ActorDelegateIDE.setupAsActorDelegate(project);

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!isStartupProject()) {
                        try {
                            classStateManager = new ClassStateManager(project);
                        } catch (RemoteException exc) {
                            Debug.reportError("Error when opening scenario", exc);
                        }
                    }

                    // Initialise JavaFX:
                    new JFXPanel();
                    Platform.setImplicitExit(false);

                    frame = GreenfootFrame.getGreenfootFrame(rBlueJ, classStateManager, project, shmFilePath);

                    // Want to execute this after the simulation has been initialised:
                    ExecServer.setCustomRunOnThread(r -> Simulation.getInstance().runLater(r));

                    // Config is initialized in GreenfootLauncherDebugVM

                    if (!isStartupProject()) {
                        try {
                            // bringToFront is done automatically by BlueJ
                            // Utility.bringToFront(frame);

                            compileListenerForwarder = new CompileListenerForwarder(compileListeners);
                            GreenfootMain.this.rBlueJ.addCompileListener(compileListenerForwarder, pkg.getProject().getDir());

                            
                            rBlueJ.addClassListener(classStateManager);

                            proj.greenfootReady();
                        }
                        catch (RemoteException exc) {
                            Debug.reportError("Error when opening scenario", exc);
                        }
                    }
                    
                    try
                    {
                        rBlueJ.hideSplash();
                    }
                    catch (RemoteException e)
                    {
                        Debug.reportError(e);
                    }
                    frame.setVisible(true);
                    Utility.bringToFront(frame);

                    try
                    {
                        proj.startImportsScan();
                    }
                    catch (RemoteException | ProjectNotOpenException e)
                    {
                        Debug.reportError(e);
                    }
                    
                    EventQueue.invokeLater(() -> {
                        if (wizard) {
                            //new NewSubWorldAction(frame, true, sourceType).actionPerformed(null);
                        }
                    });

                    // We can do this late on, because although the submission failure may have already
                    // happened, the event is re-issued to new listeners.  And we don't want to accidentally
                    // show the dialog during load because we may interrupt important processes:
                    try
                    {
                        GreenfootMain.this.rBlueJ.addApplicationListener(new RApplicationListenerImpl() {
                            @Override
                            public void dataSubmissionFailed() throws RemoteException
                            {
                                if (Boot.isTrialRecording())
                                {
                                    Platform.runLater(() -> {
                                        new DataSubmissionFailedDialog().show();
                                    });
                                }
                            }
                        });
                    }
                    catch (RemoteException e)
                    {
                        Debug.reportError(e);
                        // Show the dialog anyway; probably best to restart:
                        Platform.runLater(() -> {
                            new DataSubmissionFailedDialog().show();
                        });
                    }

                }
            });
        }
        catch (Exception exc) {
            Debug.reportError("could not create greenfoot main", exc);
        }
    }

    /**
     * Check whether this instance of greenfoot is running the dummy
     * startup project.
     * @return true if this is the startup project
     */
    private boolean isStartupProject()
    {
        return project.getDir().equals(startupProject);
    }

    /**
     * Get the project for this greenfoot instance.
     * @return
     */
    public GProject getProject()
    {
        return project;
    }

    /*
     * @see rmiextension.wrappers.event.RProjectListener#projectClosing()
     */
    @Override
    public void projectClosing()
    {
        try {
            if (!isStartupProject()) {
                rBlueJ.removeCompileListener(compileListenerForwarder);
                rBlueJ.removeClassListener(classStateManager);
                storeFrameState();
            }
        }
        catch (RemoteException re) {
            Debug.reportError("Closing project", re);
        }
    }

    /**
     * Close all open Greenfoot project instances, i.e. exit the application.
     */
    public static void closeAll()
    {
        try {
            getInstance().rBlueJ.exit();
        }
        catch (RemoteException re) {
            Debug.reportError("Closing all projects", re);
        }
    }

    /**
     * Store the current main window size to the project properties.
     */
    private void storeFrameState()
    {
        ProjectProperties projectProperties = project.getProjectProperties();

        projectProperties.setInt("mainWindow.width", frame.getWidth());
        projectProperties.setInt("mainWindow.height", frame.getHeight());
        Point loc = frame.getLocation();
        projectProperties.setInt("mainWindow.x", loc.x);
        projectProperties.setInt("mainWindow.y", loc.y);

        projectProperties.save();
    }

    /**
     * Adds a listener for compile events
     * 
     * @param listener
     */
    private void addCompileListener(CompileListener listener)
    {
        synchronized (compileListeners) {
            compileListeners.add(0, listener);
        }
    }

    /**
     * Creates a new project
     */
    public RProject newProject(boolean wizard, SourceType sourceType)
    {
        File newFile = FileUtility.getDirName(frame,
                Config.getString("greenfoot.utilDelegate.newScenario"),
                Config.getString("pkgmgr.newPkg.buttonLabel"),
                false, true);
        if (newFile != null) {
            if (newFile.exists() && (!newFile.isDirectory() || newFile.list().length > 0)) {
                DialogManager.showError(frame, "project-already-exists");
                return null;
            }
            //RProject rproj = rBlueJ.newProject(newFile, wizard, sourceType);
        }
        return null;
    }

    /**
     * Get a reference to the greenfoot frame.
     */
    public GreenfootFrame getFrame()
    {
        return frame;
    }

    /**
     * Makes a project a greenfoot project. It cleans up the project directory
     * and makes sure everything that needs to be there is there.
     * 
     * @param deleteClassFiles whether the class files in the destination should
     *            be deleted. If true, they will be deleted and appear as
     *            needing a recompile in the Greenfoot class browser.
     */
    private static void prepareGreenfootProject(File greenfootLibDir, File projectDir,
                                                ProjectAPIVersionAccess p, boolean deleteClassFiles, String greenfootApiVersion)
    {
        if (isStartupProject(greenfootLibDir, projectDir)) {
            return;
        }
        File dst = projectDir;

        File greenfootDir = new File(dst, "greenfoot");
        
        // Since Greenfoot 1.5.2 we no longer require the greenfoot directory,
        // so we delete everything that we might have had in there previously,
        // and delete the dir if it is empty after that.
        deleteGreenfootDir(greenfootDir);        
        
        if(deleteClassFiles) {
            deleteAllClassFiles(dst);
        }
        
        // Since Greenfoot 1.3.0 we no longer use the bluej.pkg file, so if it
        // exists it should now be deleted.
        try {
            File pkgFile = new File(dst, "bluej.pkg");
            if (pkgFile.exists()) {
                pkgFile.delete();
            }   
            File pkhFile = new File(dst, "bluej.pkh");
            if (pkhFile.exists()) {
                pkhFile.delete();
            }
        }
        catch (SecurityException e) {
            // If we don't have permission to delete, just leave them there.
        }   
        
        try {
            File images = new File(dst, "images");
            images.mkdir();
            File sounds = new File(dst, "sounds");
            sounds.mkdir();
        }
        catch (SecurityException e) {
            Debug.reportError("SecurityException when trying to create images/sounds directories", e);
        }
        
        p.setAPIVersionAndSave(greenfootApiVersion);
    }

    private static void deleteGreenfootDir(File greenfootDir) 
    {
        if (greenfootDir.exists()) {
            try {
                File actorJava = new File(greenfootDir, "Actor.java");
                if (actorJava.exists()) {
                    actorJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                File worldJava = new File(greenfootDir, "World.java");
                if (worldJava.exists()) {
                    worldJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                File actorJava = new File(greenfootDir, "Actor.class");
                if (actorJava.exists()) {
                    actorJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                File worldJava = new File(greenfootDir, "World.class");
                if (worldJava.exists()) {
                    worldJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                File worldJava = new File(greenfootDir, "project.greenfoot");
                if (worldJava.exists()) {
                    worldJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                greenfootDir.delete();
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
        }
    }
    
    public static interface ProjectAPIVersionAccess
    {
        /**
         * Attempts to find the version number the greenfoot API that a greenfoot
         * project was created with. If it can not find a version number, it will
         * return Version.NO_VERSION. Thread-safe.
         *
         * @return API version
         */
        Version getAPIVersion();

        /**
         * Sets the API version and saves this to the project file.
         * @param version
         */
        void setAPIVersionAndSave(String version);
    }

    /**
     * Checks whether the API version this project was created with is
     * compatible with the current API version. If it is not, it will attempt to
     * update the project to the current version of the API and present the user
     * with a dialog with instructions on what to do if there are changes in API
     * version that requires manual modifications of the API.
     * <p>
     * If is considered safe to open this project with the current API version
     * the method will return true.
     * 
     * @param project The project in question.
     * @param parent Frame that should be used to place dialogs.
     * @return One of VERSION_OK, VERSION_UPDATED or VERSION_BAD
     */
    public static VersionCheckInfo updateApi(File projectDir, ProjectAPIVersionAccess projectVersionAccess, Frame parent, String greenfootApiVersion)
    {
        File greenfootLibDir = Config.getGreenfootLibDir();
        Version projectVersion = projectVersionAccess.getAPIVersion();

        Version apiVersion = GreenfootMain.getAPIVersion();

        if (projectVersion.isBad()) {
            String message = projectVersion.getBadMessage();
            JButton continueButton = new JButton(Config.getString("greenfoot.continue"));
            MessageDialog dialog = new MessageDialog(parent, message, Config.getString("project.version.mismatch"), 50,
                    new JButton[]{continueButton});
            dialog.displayModal();
            Debug.message("Bad version number in project: " + greenfootLibDir);
            GreenfootMain.prepareGreenfootProject(greenfootLibDir, projectDir,
                    projectVersionAccess, true, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, false);
        }
        else if (projectVersion.isOlderAndBreaking(apiVersion)) {
            String message = projectVersion.getChangesMessage(apiVersion);
            boolean removeAWTImports;
            if (projectVersion.crosses300Boundary(apiVersion))
            {
                message += "\n\n" + Config.getString("greenfoot.importfix.question"); //"Would you like to try to automatically update your code?";
                JButton yesButton = new JButton(Config.getString("greenfoot.importfix.yes"));
                JButton noButton = new JButton(Config.getString("greenfoot.importfix.no"));
                MessageDialog dialog = new MessageDialog(parent, message, Config.getString("project.version.mismatch"), 80,
                        Config.isMacOS() ? new JButton[]{noButton, yesButton} : new JButton[]{yesButton, noButton});
                removeAWTImports = dialog.displayModal() == yesButton;
            }
            else
            {
                JButton continueButton = new JButton(Config.getString("greenfoot.continue"));
                MessageDialog dialog = new MessageDialog(parent, message, Config.getString("project.version.mismatch"), 80,
                        new JButton[]{continueButton});
                dialog.displayModal();
                removeAWTImports = false;
            }
            GreenfootMain.prepareGreenfootProject(greenfootLibDir, projectDir,
                    projectVersionAccess, true, greenfootApiVersion);

            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, removeAWTImports);
        }
        else if (apiVersion.isOlderAndBreaking(projectVersion)) {
            String message = projectVersion.getNewerMessage();

            JButton cancelButton = new JButton(Config.getString("greenfoot.cancel"));
            JButton continueButton = new JButton(Config.getString("greenfoot.continue"));
            MessageDialog dialog = new MessageDialog(parent, message, Config.getString("project.version.mismatch"), 50,
                    new JButton[]{continueButton, cancelButton});
            JButton pressed = dialog.displayModal();

            if (pressed == cancelButton) {
                return new VersionCheckInfo(VersionInfo.VERSION_BAD, false);
            }
            prepareGreenfootProject(greenfootLibDir, projectDir, projectVersionAccess, true, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, false);
        }
        else if (projectVersion.isNonBreaking(apiVersion) ) {
            prepareGreenfootProject(greenfootLibDir, projectDir,
                    projectVersionAccess, true, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, false);
        }
        else if (projectVersion.isInternal(apiVersion)) {
            prepareGreenfootProject(greenfootLibDir, projectDir,
                    projectVersionAccess, false, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, false);
        }
        else {       
            prepareGreenfootProject(greenfootLibDir, projectDir,
                    projectVersionAccess, false, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_OK, false);            
        }
    }

    /**
     * Deletes all class files in the directory, including the greenfoot subdirectory,
     * only if they have a .java file related to them.
     */
    public static void deleteAllClassFiles(File dir)
    {
        String[] classFiles = dir.list(classFilter);
        if(classFiles == null) return;

        for (int i = 0; i < classFiles.length; i++) {
            String fileName = classFiles[i];
            int index = fileName.lastIndexOf('.');
            String javaFileName = fileName.substring(0, index) + "." + SourceType.Java.toString().toLowerCase();
            File file = new File(dir, fileName);
            File javaFile = new File(dir, javaFileName);
            if (javaFile.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Checks if the project is the default startup project that is used when no
     * other project is open. It is necessary to have this dummy project,
     * becuase we must have a project in order to launch the DebugVM.
     * 
     */
    public static boolean isStartupProject(File blueJLibDir, File projectDir)
    {
        File startupProject = new File(blueJLibDir, "startupProject");
        if (startupProject.equals(projectDir)) {
            return true;
        }

        return false;
    }

    /**
     * Gets the version number of the Greenfoot API for this Greenfoot release.
     */
    public static Version getAPIVersion()
    {
        if (version == null) {
            try {
                Class<?> bootCls = Class.forName("bluej.Boot");
                Field field = bootCls.getField("GREENFOOT_API_VERSION");
                String versionStr = (String) field.get(null);
                version = new Version(versionStr);
            }
            catch (ClassNotFoundException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
            catch (SecurityException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
            catch (NoSuchFieldException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
            catch (IllegalArgumentException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
            catch (IllegalAccessException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
        }

        return version;
    }

    /**
     * See if there is a new class loader in place. If so, we want to
     * clear all views (BlueJ views) which refer to classes loaded by the previous
     * loader.
     */
    private void checkClassLoader()
    {
        ClassLoader newLoader = ExecServer.getCurrentClassLoader();
        if (newLoader != currentLoader) {
            View.removeAll(currentLoader);
            currentLoader = newLoader;
        }
    }

    // ------------ CompileListener interface -------------

    @Override
    public void compileStarted(RCompileEvent event)
    {
        checkClassLoader();
    }

    @Override
    public void compileSucceeded(RCompileEvent event)
    {
        checkClassLoader();

    }

    @Override
    public void compileFailed(RCompileEvent event)
    {
        checkClassLoader();
    }

    @Override
    public void compileError(RCompileEvent event) {}

    @Override
    public void compileWarning(RCompileEvent event){}
}
