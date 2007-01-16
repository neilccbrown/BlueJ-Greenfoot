package greenfoot.util;

import greenfoot.World;
import greenfoot.core.GreenfootMain;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.gui.ControlPanel;
import greenfoot.gui.WorldCanvas;

import java.awt.BorderLayout;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JFrame;

import bluej.runtime.ExecServer;

/**
 * This class can view and run a greenfoot scenario. It is not possible to
 * interact with the objects in any way.
 * 
 * @author Poul Henriksen
 * 
 */
public class GreenfootScenarioViewer
{

    private Simulation sim;
    private WorldCanvas canvas;
    private ProjectProperties properties;

    private ControlPanel controls;

    /**
     * Start the scenario. <p>
     * 
     * BlueJ and the scenario MUST be on the classpath.
     * 
     * @param args Two arguments should be passed to this method. The
     * first one should be the World to be instantiated and the second argument
     * should be a method that populates the world with actors. If no arguments
     * are supplied it will use AntWorld and scenario2 as arguments.
     * 
     */
    public static void main(String[] args)
    {
        String worldClassName = "AntWorld";
        String worldInitMethod = "scenario2";  
        if(args.length == 2) {
            worldClassName = args[0];
            worldInitMethod = args[1];
        }
        
        GreenfootScenarioViewer gs = new GreenfootScenarioViewer();
        gs.init(worldClassName, worldInitMethod);
        gs.buildGUI();        
    }

    private void buildGUI()
    {
        JFrame frame = new JFrame();
        frame.getContentPane().add(canvas, BorderLayout.CENTER);

       
        frame.getContentPane().add(controls, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

    private void init(String worldClassName, String worldInitMethod)
    {
        try {            
            File projectDir = GreenfootUtil.getDirectoryContaining(worldClassName + ".class");
            properties = new ProjectProperties(projectDir);
            GreenfootMain.initialize(properties);
            Class worldClass = Class.forName(worldClassName);
            ExecServer.setClassLoader(worldClass.getClassLoader());
            Constructor worldConstructor = worldClass.getConstructor(new Class[]{});
            World world = (World) worldConstructor.newInstance(new Object[]{});

            canvas = new WorldCanvas(world);

            WorldHandler.initialise(canvas);
            WorldHandler worldHandler = WorldHandler.getInstance();
            Simulation.initialize(worldHandler);
            sim = Simulation.getInstance();
            controls = new ControlPanel(sim);
            worldHandler.setWorld(world);
            
            Method initMethod = worldClass.getMethod(worldInitMethod, new Class[]{});
            initMethod.invoke(world, new Object[]{});

        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            e.getCause().printStackTrace();
        }
    }
}
