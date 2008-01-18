package bluej.groupwork.ui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;
import bluej.utility.EscapeDialog;

/**
 * A dialog for teamwork settings.
 *
 * @author fisker
 * @author bquig
 * @version $Id: TeamSettingsDialog.java 5459 2008-01-18 05:24:16Z davmac $
 */
public class TeamSettingsDialog extends EscapeDialog
{
    private String title = Config.getString(
            "team.settings.title");
    public static final int OK = 0;
    public static final int CANCEL = 1;
    private TeamSettingsController teamSettingsController;
    private TeamSettingsPanel teamSettingsPanel;
    private int event;
    
    private JButton okButton;

    /**
     * Create a team settings dialog with a reference to the team settings
     * controller that it manipulates.
     */
    public TeamSettingsDialog(TeamSettingsController controller)
    {
        teamSettingsController = controller;
        event = CANCEL;
        if(teamSettingsController.hasProject())
            title += " - " + teamSettingsController.getProject().getProjectName();
        setTitle(title);
       
        setModal(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BlueJTheme.dialogBorder);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel buttonPanel = makeButtonPanel();
        teamSettingsPanel = new TeamSettingsPanel(teamSettingsController, this);
        setFocusTraversalPolicy(teamSettingsPanel.getTraversalPolicy(
                getFocusTraversalPolicy()));
        mainPanel.add(teamSettingsPanel);
        mainPanel.add(buttonPanel);
        setContentPane(mainPanel);
        pack();
    }

    /**
     * Set up the panel containing the ok and cancel buttons, with associated
     * actions.
     */
    private JPanel makeButtonPanel()
    {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        {
            buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

            okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(new ActionListener() {
                    /**
                     * Write the data from the teamSettingsPanel to the project's team.defs file
                     * If checkbox in teamSettingsPanel is checked, the data is also stored in
                     * bluej.properties
                     */
                    public void actionPerformed(ActionEvent e)
                    {
                        TeamSettings settings = teamSettingsPanel.getSettings();
                        
                        String userKey = "bluej.teamsettings.user";
                        String userValue = settings.getUserName();
                        teamSettingsController.setPropString(userKey, userValue);

                        // passwords are handled differently for security reasons,
                        // we don't at present store them on disk
                        String passValue = settings.getPassword();
                        teamSettingsController.setPasswordString(passValue);

                        String keyBase = "bluej.teamsettings."
                                + teamSettingsPanel.getSelectedProvider().getProviderName().toLowerCase()
                                + ".";
                        String serverKey = keyBase + "server";
                        String serverValue = settings.getServer();
                        teamSettingsController.setPropString(serverKey,
                            serverValue);

                        String prefixKey = keyBase + "repositoryPrefix";
                        String prefixValue = settings.getPrefix();
                        teamSettingsController.setPropString(prefixKey,
                            prefixValue);

                        String protocolKey = keyBase + "protocol";
                        String protocolValue = settings.getProtocol();
                        teamSettingsController.setPropString(protocolKey,
                            protocolValue);

                        String groupKey = "bluej.teamsettings.groupname";
                        String groupValue = settings.getGroup();
                        teamSettingsController.setPropString(groupKey,
                            groupValue);

                        String useAsDefaultKey = "bluej.teamsettings.useAsDefault";
                        Config.putPropString(useAsDefaultKey,
                            Boolean.toString(
                                teamSettingsPanel.getUseAsDefault()));

                        if (teamSettingsPanel.getUseAsDefault()) {
                            Config.putPropString(userKey, userValue);
                            Config.putPropString(serverKey, serverValue);
                            Config.putPropString(prefixKey, prefixValue);
                            Config.putPropString(groupKey, groupValue);
                            Config.putPropString(protocolKey, protocolValue);
                        }

                        if ((teamSettingsController != null) &&
                                teamSettingsController.hasProject()) {
                            teamSettingsController.writeToProject();
                        }

                        event = OK;
                        setVisible(false);
                    }
                });

            getRootPane().setDefaultButton(okButton);

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        event = CANCEL;
                        setVisible(false);
                    }
                });

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
        }

        return buttonPanel;
    }

    /**
     * Disable the fields used to specify the repository:
     * group, prefix, server and protocol. Called when the team settings
     * dialog is connected to a project already.
     */
    public void disableRepositorySettings()
    {
        teamSettingsPanel.disableRepositorySettings();
    }

    /**
     * Display the dialog and wait for a response. Returns the user
     * response as OK or CANCEL.
     */
    public int doTeamSettings()
    {
        setVisible(true);

        return event;
    }
    
    /**
     * Enabled or disable to "Ok" button of the dialog.
     */
    public void setOkButtonEnabled(boolean enabled)
    {
        okButton.setEnabled(enabled);
    }
    
    /**
     * Get the settings specified by the user
     */
    public TeamSettings getSettings()
    {
        return teamSettingsPanel.getSettings();
    }
}
