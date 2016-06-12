/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016  Michael Kolling and John Rosenberg 
 
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
package bluej.utility.javafx;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.Point;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.ComponentListener;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Window;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An FX dialog containing only a SwingNode.  This is intended to be a drop-in
 * replacement for EscapeDialog which displays the content as a SwingNode in
 * an FX window rather than a JDialog.
 * 
 * This is a transition class, used to convert some of the version control
 * dialogs to being FX windows until we have time to go back and convert
 * them properly to FX.
 */
public class SwingNodeDialog
{
    @OnThread(Tag.Any)
    private SwingNode swingNode;
    @OnThread(Tag.Swing)
    private boolean modal = false;
    @OnThread(Tag.FXPlatform)
    private Dialog<Object> dialog;
    
    @OnThread(Tag.Swing)
    protected SwingNodeDialog()
    {
        // Provide a content pane by default:
        swingNode = new SwingNode();
        swingNode.setContent(new JPanel());
        
        Platform.runLater(() -> {            
            dialog = new Dialog<>();
            // We must return non-null result to allow the dialog
            // to be closed even though we have no FX buttons
            dialog.setResultConverter(bt -> new Object());
            dialog.setResult(new Object());
            dialog.getDialogPane().setContent(swingNode);
            dialog.initModality(Modality.NONE);
        });
    }

    @OnThread(Tag.Swing)
    protected SwingNodeDialog(FXPlatformSupplier<Window> owner)
    {
        this();
        Platform.runLater(() -> dialog.initOwner(owner.get()));
    }
    
    @OnThread(Tag.Swing)
    protected void setModal(boolean makeModal)
    {
        this.modal = makeModal;
        Platform.runLater(() -> dialog.initModality(makeModal ? Modality.APPLICATION_MODAL : Modality.NONE));
    }
    
    @OnThread(Tag.Swing)
    public void setVisible(boolean show)
    {
        // Can't use dialog::show because may not be assigned yet
        if (show)
        {
            if (modal)
            {
                // We need to wait for the FX dialog, but in the mean time
                // keep the Swing thread responsive:
                SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
                Platform.runLater(() -> {
                    dialog.setOnHidden(e -> {
                        SwingUtilities.invokeLater(() -> loop.exit());
                    });
                    dialog.show();
                });
                loop.enter();
            }
            else
                // Fire and forget:
                Platform.runLater(() -> dialog.show());
        }
        else
            Platform.runLater(() -> dialog.hide());
    }

    @OnThread(Tag.Swing)
    protected void setTitle(String title)
    {
        Platform.runLater(() -> dialog.setTitle(title));
    }

    @OnThread(Tag.Swing)
    public void pack()
    {
        // Not applicable
        Dimension preferredSize = swingNode.getContent().getPreferredSize();
        swingNode.getContent().setPreferredSize(preferredSize);
        swingNode.getContent().validate();
        Platform.runLater(() -> {
            dialog.getDialogPane().setPrefWidth(preferredSize.getWidth());
            dialog.getDialogPane().setPrefHeight(preferredSize.getHeight());
        });
    }
    
    @OnThread(Tag.Swing)
    protected JComponent getContentPane()
    {
        return swingNode.getContent();
    }

    @OnThread(Tag.Swing)
    protected void setContentPane(JComponent content)
    {
        swingNode.setContent(content);
    }

    @OnThread(Tag.Swing)
    public void dispose()
    {
        setVisible(false);
    }

    @OnThread(Tag.FXPlatform)
    public Window asWindow()
    {
        Scene scene = dialog.getDialogPane().getScene();
        if (scene == null)
            return null;
        else
            return scene.getWindow();
    }
    
    @OnThread(Tag.Swing)
    protected void setDefaultButton(JButton button)
    {
        
    }
    
    @OnThread(Tag.Swing)
    public void setLocation(Point p)
    {
        Platform.runLater(() -> {
            if (asWindow() == null)
                return;
            asWindow().setX(p.x);
            asWindow().setY(p.y);
        });
    }
    
    @OnThread(Tag.Swing)
    public Point getLocation()
    {
        return new Point(0, 0); // Can't easily access this from Swing thread
    }

    @OnThread(Tag.Swing)
    public void addComponentListener(ComponentListener l)
    {
        // Will get replaced anyway
    }

    @OnThread(Tag.Swing)
    public FocusTraversalPolicy getFocusTraversalPolicy()
    {
        return swingNode.getContent().getFocusTraversalPolicy();
    }

    @OnThread(Tag.Swing)
    public void setFocusTraversalPolicy(FocusTraversalPolicy policy)
    {
        swingNode.getContent().setFocusTraversalPolicy(policy);
    }
    
    @OnThread(Tag.Swing)
    public void setLocationRelativeTo(JComponent comp)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public void setLocationRelativeTo(Window window)
    {

    }
    
}
