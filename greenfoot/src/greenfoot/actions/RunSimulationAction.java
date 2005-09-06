package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * @author Poul Henriksen
 * @version $Id: RunSimulationAction.java 3551 2005-09-06 09:31:41Z polle $
 */
public class RunSimulationAction extends AbstractAction
{

    private Simulation simulation;

    public RunSimulationAction(String name, Icon icon, Simulation simulation)
    {
        super(name, icon);
        this.simulation = simulation;
    }

    public void actionPerformed(ActionEvent e)
    {
        //	We don't want to block!
        new Thread() {
            public void run()
            {
                if (simulation.isAlive()) {
                    simulation.setPaused(false);
                }
                else {
                    simulation.start();
                }
            }
        }.start();

    }

}