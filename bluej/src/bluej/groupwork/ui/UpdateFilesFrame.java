package bluej.groupwork.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.UpdateFilter;
import bluej.groupwork.actions.UpdateAction;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Project;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.SwingWorker;


/**
 * A Swing based user interface for showing files to be updated
 * @author Bruce Quig
 * @author Davin McCall
 * @version $Id: UpdateFilesFrame.java 5811 2008-07-23 16:45:17Z polle $
 */
public class UpdateFilesFrame extends EscapeDialog
{
    private JList updateFiles;
    private JPanel topPanel;
    private JPanel bottomPanel;
    private JButton updateButton;
    private JCheckBox includeLayoutCheckbox;
    private ActivityIndicator progressBar;
    private UpdateAction updateAction;
    private UpdateWorker updateWorker;

    private Project project;
    
    private Repository repository;
    private DefaultListModel updateListModel;
    
    private Set changedLayoutFiles; // set of TeamStatusInfo
    private Set forcedLayoutFiles; // set of File
    private boolean includeLayout = true;
    
    private static String noFilesToUpdate = Config.getString("team.noupdatefiles"); 

    public UpdateFilesFrame(Project proj)
    {
        project = proj;
        changedLayoutFiles = new HashSet();
        forcedLayoutFiles = new HashSet();
        createUI();
        DialogManager.centreDialog(this);
    }
    
    public void setVisible(boolean show)
    {
        super.setVisible(show);
        if (show) {
            // we want to set update action disabled until we know that
            // there's something to update
            updateAction.setEnabled(false);
            includeLayoutCheckbox.setSelected(false);
            includeLayoutCheckbox.setEnabled(false);
            changedLayoutFiles.clear();
            forcedLayoutFiles.clear();
            updateListModel.removeAllElements();
            
            repository = project.getRepository();
            
            if (repository != null) {
                project.saveAllEditors();
                project.saveAllGraphLayout();
                startProgress();
                updateWorker = new UpdateWorker();
                updateWorker.start();
            }
            else {
                super.setVisible(false);
            }
        }
    }
    
    /**
     * Create the user-interface for the error display dialog.
     */
    protected void createUI()
    {
        setTitle(Config.getString("team.update.title"));
        updateListModel = new DefaultListModel();
        
        //setIconImage(BlueJTheme.getIconImage());
        setLocation(Config.getLocation("bluej.updatedisplay"));

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.updatedisplay", getLocation());
                }
            });

        topPanel = new JPanel();

        JScrollPane updateFileScrollPane = new JScrollPane();

        {
            topPanel.setLayout(new BorderLayout());

            JLabel updateFilesLabel = new JLabel(Config.getString(
                        "team.update.files"));
            updateFilesLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            topPanel.add(updateFilesLabel, BorderLayout.NORTH);

            updateFiles = new JList(updateListModel);
            updateFiles.setCellRenderer(new FileRenderer(project));
            updateFiles.setEnabled(false);
            updateFileScrollPane.setViewportView(updateFiles);
            
            topPanel.add(updateFileScrollPane, BorderLayout.CENTER);
        }

        bottomPanel = new JPanel();

        {
            bottomPanel.setLayout(new BorderLayout());

            updateAction = new UpdateAction(this);
            updateButton = BlueJTheme.getOkButton();
            updateButton.setAction(updateAction);
            updateButton.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e)
                {
                   includeLayoutCheckbox.setEnabled(false);
                } 
            });
            getRootPane().setDefaultButton(updateButton);

            JButton closeButton = BlueJTheme.getCancelButton();
            closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        updateAction.cancel();
                        updateWorker.abort();
                        setVisible(false);
                    }
                });
           
            DBox buttonPanel = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
            buttonPanel.setBorder(BlueJTheme.generalBorder);
            
            progressBar = new ActivityIndicator();
            progressBar.setRunning(false);
            
            DBox checkBoxPanel = new DBox(DBoxLayout.Y_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
            includeLayoutCheckbox = new JCheckBox(Config.getString("team.update.includelayout"));
            includeLayoutCheckbox.setEnabled(false);
            includeLayoutCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    JCheckBox layoutCheck = (JCheckBox)e.getSource();
                    includeLayout = layoutCheck.isSelected();
                    resetForcedFiles();
                    if (includeLayout) {
                        addModifiedLayouts();
                        if(!updateButton.isEnabled()) {
                            updateAction.setEnabled(true);
                        }
                    }
                    // unselected
                    else {
                        removeModifiedLayouts();
                        if(isUpdateListEmpty()) {
                            updateAction.setEnabled(false);
                        }
                    }
                }
            });

            checkBoxPanel.add(includeLayoutCheckbox);
            checkBoxPanel.add(buttonPanel);
            
            buttonPanel.add(progressBar);
            buttonPanel.add(updateButton);
            buttonPanel.add(closeButton);
            bottomPanel.add(checkBoxPanel, BorderLayout.SOUTH);
        }

        DBox mainPanel = new DBox(DBox.Y_AXIS, 0.5f);
        mainPanel.setBorder(BlueJTheme.dialogBorder);
        mainPanel.add(topPanel);
        mainPanel.add(bottomPanel);
        getContentPane().add(mainPanel);
        
        pack();
    }

    public void reset()
    {
        updateListModel.clear();
    }
    
    private void removeModifiedLayouts()
    {
        // remove modified layouts from list of files shown for commit
        for(Iterator it = changedLayoutFiles.iterator(); it.hasNext();) {
            updateListModel.removeElement(it.next());
        }
        if(updateListModel.isEmpty()) {
            updateListModel.addElement(noFilesToUpdate);
        }
    }
    
    private boolean isUpdateListEmpty()
    {
        return updateListModel.isEmpty() || updateListModel.contains(noFilesToUpdate);
    }
    
    /**
     * Add the modified layouts to the displayed list of files to be updated.
     */
    private void addModifiedLayouts()
    {
        if(updateListModel.contains(noFilesToUpdate)) {
            updateListModel.removeElement(noFilesToUpdate);
        }
        // add diagram layout files to list of files to be committed
        for(Iterator it = changedLayoutFiles.iterator(); it.hasNext(); ) {
            updateListModel.addElement(it.next());
        }
    }
    
    /**
     * Get a set (of File) containing the layout files which need to be updated.
     */
    public Set getChangedLayoutFiles()
    {
        Set files = new HashSet();
        for(Iterator it = changedLayoutFiles.iterator(); it.hasNext(); ) {
            TeamStatusInfo info = (TeamStatusInfo)it.next();
            files.add(info.getFile());
        }
        return files;
    }
    
    public boolean includeLayout()
    {
        return includeLayoutCheckbox != null && includeLayoutCheckbox.isSelected();
    }
    
    /**
     * Start the activity indicator.
     */
    public void startProgress()
    {
        progressBar.setRunning(true);
    }

    /**
     * Stop the activity indicator. Call from any thread.
     */
    public void stopProgress()
    {
        progressBar.setRunning(false);
    }
    
    public Project getProject()
    {
        return project;
    }
    
    /**
     * The layout has changed. Enable the "include layout" checkbox, etc.
     */
    private void setLayoutChanged()
    {
        includeLayoutCheckbox.setEnabled(true);
        includeLayoutCheckbox.setSelected(includeLayout);
    }
    
    /**
     * Re-set the forced files in the update action. This needs to be
     * done when the "include layout" option is toggled.
     */
    private void resetForcedFiles()
    {
        Set forcedFiles = new HashSet(forcedLayoutFiles);
        if (includeLayout) {
            for (Iterator i = changedLayoutFiles.iterator(); i.hasNext(); ) {
                TeamStatusInfo info = (TeamStatusInfo) i.next();
                forcedFiles.add(info.getFile());
            }
        }
        updateAction.setFilesToForceUpdate(forcedFiles);
    }

    /**
    * Inner class to do the actual cvs status check to populate commit dialog
    * to ensure that the UI is not blocked during remote call
    */
    class UpdateWorker extends SwingWorker implements StatusListener
    {
        List response;
        TeamworkCommand command;
        TeamworkCommandResult result;
        private boolean aborted;

        public UpdateWorker()
        {
            super();
            response = new ArrayList();
            FileFilter filter = project.getTeamSettingsController().getFileFilter(true);
            command = repository.getStatus(this, filter, true);
        }
        
        /* (non-Javadoc)
         * @see bluej.groupwork.StatusListener#gotStatus(bluej.groupwork.TeamStatusInfo)
         */
        public void gotStatus(TeamStatusInfo info)
        {
            response.add(info);
        }
        
        public Object construct()
        {
            result = command.getResult();
            return response;
        }
        
        public void abort()
        {
            command.cancel();
            aborted = true;
        }

        public void finished()
        {
            stopProgress();
            if (! aborted) {
                if (result.isError()) {
                    TeamUtils.handleServerResponse(result, UpdateFilesFrame.this);
                    setVisible(false);
                }
                else {
                    Set filesToUpdate = new HashSet();
                    Set conflicts = new HashSet();
                    Set modifiedLayoutFiles = new HashSet();

                    List info = response;
                    getUpdateFileSet(info, filesToUpdate, conflicts, modifiedLayoutFiles);

                    if (conflicts.size() != 0) {
                        String filesList = "";
                        Iterator i = conflicts.iterator();
                        for (int j = 0; j < 10 && i.hasNext(); j++) {
                            File conflictFile = (File) i.next();
                            filesList += "    " + conflictFile.getName() + "\n";
                        }

                        // If there are more than 10 conflicts, we won't list them
                        // all in the dialog
                        if (i.hasNext()) {
                            filesList += "    (and more - check status)";
                        }

                        DialogManager.showMessageWithText(UpdateFilesFrame.this, "team-unresolved-conflicts", filesList);
                        UpdateFilesFrame.this.setVisible(false);
                        return;
                    }

                    // Build the actual set of files to update. If there are new or removed
                    // directories, don't include files within.
                    Set updateFiles = new HashSet();
                    for (Iterator i = filesToUpdate.iterator(); i.hasNext(); ) {
                        File file = (File) i.next();
                        if (! filesToUpdate.contains(file.getParentFile())) {
                            updateFiles.add(file);
                        }
                    }
                    for (Iterator i = forcedLayoutFiles.iterator(); i.hasNext(); ) {
                        File file = (File) i.next();
                        if (filesToUpdate.contains(file.getParentFile())) {
                            i.remove();
                        }
                    }
                    
                    updateAction.setStatusCommand(command);
                    updateAction.setFilesToUpdate(updateFiles);
                    resetForcedFiles();

                    if (includeLayout && ! changedLayoutFiles.isEmpty()) {
                        addModifiedLayouts();
                    }

                    if(updateListModel.isEmpty()) {
                        updateListModel.addElement(noFilesToUpdate);
                    }
                    else {
                        updateAction.setEnabled(true);
                    }
                }
            }
        }
        
        /**
         * Go through the status list, and figure out which files to update, and
         * which to force update.
         * 
         * @param info  The list of files with status (List of TeamStatusInfo)
         * @param filesToUpdate  The set to store the files to update in
         * @param modifiedLayoutFiles  The set to store the files to be force updated in
         * @param conflicts      The set to store unresolved conflicts in
         *                       (any files in this set prevent update from occurring)
         */
        private void getUpdateFileSet(List info, Set filesToUpdate, Set conflicts, Set modifiedLayoutFiles)
        {
            UpdateFilter filter = new UpdateFilter();

            for (Iterator it = info.iterator(); it.hasNext();) {
                TeamStatusInfo statusInfo = (TeamStatusInfo) it.next();
                int status = statusInfo.getStatus();
                if(filter.accept(statusInfo)) {
                    if (!BlueJPackageFile.isPackageFileName(statusInfo.getFile().getName())) { 
                        updateListModel.addElement(statusInfo);
                        filesToUpdate.add(statusInfo.getFile());
                    }
                    else {
                        if (filter.updateAlways(statusInfo)) {
                            // The package file is new or removed. There is no
                            // option not to include it in the update.
                            updateListModel.addElement(statusInfo);
                            forcedLayoutFiles.add(statusInfo.getFile());
                        }
                        else {
                            // add file to list of files that may be added to commit
                            modifiedLayoutFiles.add(statusInfo.getFile());
                            // keep track of StatusInfo objects representing changed diagrams
                            changedLayoutFiles.add(statusInfo);
                        }
                    }
                }
                else {
                    boolean conflict;
                    conflict = status == TeamStatusInfo.STATUS_UNRESOLVED;
                    conflict |= status == TeamStatusInfo.STATUS_CONFLICT_ADD;
                    conflict |= status == TeamStatusInfo.STATUS_CONFLICT_LMRD;
                    if (conflict) {
                        if(!BlueJPackageFile.isPackageFileName(statusInfo.getFile().getName())) {
                            conflicts.add(statusInfo.getFile());
                        }
                        else {
                            // bluej.pkg will be force-updated
                            modifiedLayoutFiles.add(statusInfo.getFile());
                            changedLayoutFiles.add(statusInfo);
                        }
                    }
                }
            }
            
            if (! changedLayoutFiles.isEmpty()) {
                setLayoutChanged();
            }
        }
    }
}
