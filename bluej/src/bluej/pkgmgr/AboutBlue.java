package bluej.pkgmgr;

import bluej.*;
import bluej.Config;
import bluej.utility.MultiLineLabel;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The BlueJ about box.
 *
 * @author  Michael Kolling
 * @version $Id: AboutBlue.java 2724 2004-07-02 18:51:36Z mik $
 */
class AboutBlue extends JDialog
{
    public AboutBlue(JFrame parent, String version)
    {
        super(parent, Config.getString("menu.help.about"), true);

        // Create About box text
        JPanel aboutPanel = new JPanel();
        aboutPanel.setBorder(BlueJTheme.dialogBorder);
        aboutPanel.setLayout(new BorderLayout(12,0));
        aboutPanel.setBackground(Color.white);

        // insert logo
        Icon icon = Config.getImageAsIcon("image.logo");
        JLabel logoLabel = new JLabel(icon);
        aboutPanel.add(logoLabel, BorderLayout.WEST);

        // Create Text Panel
        MultiLineLabel text = new MultiLineLabel(LEFT_ALIGNMENT, 6);
        text.setBackground(Color.white);
        text.addText(Config.getString("about.theTeam") + "\n ", false, true);
        text.addText("      Damiano Bolla, Kasper Fisker,\n");
        text.addText("      Poul Henriksen, Michael K\u00F6lling,\n");
        text.addText("      Davin McCall, Andrew Patterson,\n");
        text.addText("      Bruce Quig, John Rosenberg,\n");
        text.addText("      Ian Utting");

        aboutPanel.add(text, BorderLayout.CENTER);

        // footer text
        MultiLineLabel bottomtext = new MultiLineLabel(LEFT_ALIGNMENT);
        bottomtext.setBackground(Color.white);
        bottomtext.addText(" ");
        bottomtext.addText(Config.getString("about.bluej.version") + " "+ version +
                "  (" + Config.getString("about.java.version") + " " + System.getProperty("java.version") +
                ")", 14);
        bottomtext.addText(" ");
        bottomtext.addText(Config.getString("about.vm") + " " +
                System.getProperty("java.vm.name") + " " +
                System.getProperty("java.vm.version") +
                " (" + System.getProperty("java.vm.vendor") + ")");
        bottomtext.addText(Config.getString("about.runningOn") + " " + System.getProperty("os.name") +
                " " + System.getProperty("os.version") +
                " (" + System.getProperty("os.arch") + ")");
        bottomtext.addText(Config.getString("about.javahome") + " " + System.getProperty("java.home"));
        bottomtext.addText(" ");
        bottomtext.addText(Config.getString("about.moreInfo"));
        bottomtext.addText(" ");
        bottomtext.addText(Config.getString("about.logfile") + " " + Config.getUserConfigFile(Config.debugLogName));
        
        aboutPanel.add(bottomtext, BorderLayout.SOUTH);

        // Create Button Panel
        JPanel buttonPanel = new JPanel();
        //buttonPanel.setBackground(Color.white);
        buttonPanel.setLayout(new FlowLayout());
        JButton ok = BlueJTheme.getOkButton();
        buttonPanel.add(ok);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(aboutPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Close Action when OK is pressed
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
        	{
        	    setVisible(false);
        	    dispose();
        	}
        });

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event)
        	{
        	    Window win = (Window)event.getSource();
        	    win.setVisible(false);
        	    win.dispose();
        	}
        });

        setResizable(false);
        pack();
        DialogManager.centreDialog(this);
    }
}

