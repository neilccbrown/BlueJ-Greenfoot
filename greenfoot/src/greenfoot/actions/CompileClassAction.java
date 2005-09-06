package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileClassAction.java 3551 2005-09-06 09:31:41Z polle $
 */
public class CompileClassAction extends ClassAction
{

    /**
     * Compiles the currently selected class. If no class is selected it does
     * nothing.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        Simulation.getInstance().setPaused(true);
        try {
            if (selectedClass != null) {
                selectedClass.compile(false);
            }
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
        catch (ProjectNotOpenException e1) {
            e1.printStackTrace();
        }
        catch (PackageNotFoundException e1) {
            e1.printStackTrace();
        }
        catch (CompilationNotStartedException e1) {
            e1.printStackTrace();
        }
    }

    public CompileClassAction(String name)
    {
        super(name);
    }

}