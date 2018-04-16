/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2014,2015,2016,2018  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.Simulation.SimulationRunnable;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.DragListener;
import greenfoot.gui.DropTarget;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.input.InputManager;
import greenfoot.gui.input.KeyboardManager;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.gui.input.mouse.MousePollingManager;
import greenfoot.gui.input.mouse.WorldLocator;
import greenfoot.platforms.WorldHandlerDelegate;
import greenfoot.util.GraphicsUtilities;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import bluej.debugmgr.objectbench.ObjectBenchInterface;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The worldhandler handles the connection between the World and the
 * WorldCanvas.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.Simulation)
public class WorldHandler
    implements TriggeredKeyListener, DropTarget, DragListener, SimulationListener
{
    /** A flag to check whether a world has been set. Can be tested/cleared by callers. */
    private boolean worldIsSet;

    private World initialisingWorld;
    // Note: this field is used by name in GreenfootDebugHandler, so don't rename/remove without altering that code.
    private volatile World world;
    private WorldCanvas worldCanvas;

    // where did the the drag/drop operation begin? In pixels
    private int dragBeginX;
    private int dragBeginY;

    private KeyboardManager keyboardManager;
    @OnThread(Tag.Any)
    private static WorldHandler instance;
    private EventListenerList listenerList = new EventListenerList();
    private WorldHandlerDelegate handlerDelegate;
    private MousePollingManager mousePollingManager;
    private InputManager inputManager;

    // Offset from the middle of the actor when initiating a drag on an actor.
    private int dragOffsetX;
    private int dragOffsetY;
    // The actor being dragged
    private Actor dragActor;
    private boolean dragActorMoved;
    private int dragId;
    private Cursor defaultCursor;
    
    /** Lock used for world manipulation */
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /** Timeout used for readers attempting to acquire lock */
    public static final int READ_LOCK_TIMEOUT = 500;
    
    /** Condition used to wait for repaint */
    private Object repaintLock = new Object();
    private boolean isRepaintPending = false;
    
    public static synchronized void initialise(WorldCanvas worldCanvas, WorldHandlerDelegate helper)
    {
        instance = new WorldHandler(worldCanvas, helper);
    }
    
    /**
     * Initialiser for unit testing.
     */
    public static synchronized void initialise()
    {
        instance = new WorldHandler();
    }
    
    /**
     * Return the singleton instance.
     */
    @OnThread(Tag.Any)
    public synchronized static WorldHandler getInstance()
    {
        return instance;
    }

    /**
     * Constructor used for unit testing.
     */
    private WorldHandler() 
    {
        instance = this;
        mousePollingManager = new MousePollingManager(null);
        handlerDelegate = new WorldHandlerDelegate() {

            @Override
            public void discardWorld(World world)
            {                
            }

            @Override
            public InputManager getInputManager()
            {
                return null;
            }

            @Override
            public void instantiateNewWorld(String className, Runnable runIfError)
            {
            }

            @Override
            public void setWorld(World oldWorld, World newWorld)
            {
            }

            @Override
            public void setWorldHandler(WorldHandler handler)
            {
            }
            
            @Override
            public void objectAddedToWorld(Actor actor)
            {
            }

            @Override
            public String ask(String prompt, WorldCanvas worldCanvas)
            {
                return "";
            }
        };
    }
        
    /**
     * Creates a new worldHandler and sets up the connection between worldCanvas
     * and world.
     * 
     * @param handlerDelegate
     */
    private WorldHandler(final WorldCanvas worldCanvas, WorldHandlerDelegate handlerDelegate)
    {
        instance = this;
        this.handlerDelegate = handlerDelegate;
        this.handlerDelegate.setWorldHandler(this);

        this.worldCanvas = worldCanvas;
        
        mousePollingManager = new MousePollingManager(null);

        worldCanvas.setDropTargetListener(this);

        LocationTracker.instance().setSourceComponent(worldCanvas);
        keyboardManager = new KeyboardManager();
        worldCanvas.addFocusListener(keyboardManager);

        inputManager = handlerDelegate.getInputManager();
        addWorldListener(inputManager);
        inputManager.setRunningListeners(getKeyboardManager(), mousePollingManager, mousePollingManager);
        worldCanvas.addMouseListener(inputManager);
        worldCanvas.addMouseMotionListener(inputManager);
        worldCanvas.addKeyListener(inputManager);
        inputManager.init();

        defaultCursor = worldCanvas.getCursor();
    }

    /**
     * Get the keyboard manager.
     */
    public KeyboardManager getKeyboardManager()
    {
        return keyboardManager;
    }

    /**
     * Get the mouse manager.
     */
    public MousePollingManager getMouseManager()
    {
        return mousePollingManager;
    }

    /**
     * Drag operation starting.
     */
    public void startDrag(Actor actor, Point p, int dragId)
    {
        dragActor = actor;
        dragActorMoved = false;
        dragBeginX = ActorVisitor.getX(actor) * world.getCellSize() + world.getCellSize() / 2;
        dragBeginY = ActorVisitor.getY(actor) * world.getCellSize() + world.getCellSize() / 2;
        dragOffsetX = dragBeginX - p.x;
        dragOffsetY = dragBeginY - p.y;
        this.dragId = dragId;
        drag(actor, p);
    }
    
    public boolean isDragging()
    {
        return dragActor != null;
    }

    /*
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e)
    {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (dragActor != null && dragActorMoved) {

            }
            dragActor = null;
            worldCanvas.setCursor(defaultCursor);
        }
    }

    /**
     * Returns an object at the given pixel location. If multiple objects exist
     * at the one location, this method returns the top-most one according to
     * paint order.
     * 
     * @param x
     *            The x-coordinate
     * @param y
     *            The y-coordinate
     */
    public Actor getObject(int x, int y)
    {
        return getObject(this.world, x, y);
    }

    /**
     * Like getObject but returns all actors at that position,
     * sorted by paint order (painted first means earlier in the list)
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    public List<Actor> getObjects(int x, int y)
    {
        if (world == null)
            return Collections.emptyList();

        int timeout = READ_LOCK_TIMEOUT;
        try {
            if (lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {

                List<Actor> objectsThere = new ArrayList<>(WorldVisitor.getObjectsAtPixel(world, x, y));
                
                Collections.sort(objectsThere, Comparator.comparingInt(ActorVisitor::getLastPaintSeqNum).reversed());

                lock.readLock().unlock();

                return objectsThere;
            }
        }
        catch (InterruptedException ie) {}

        return Collections.emptyList();
    }
    
    /**
     * Returns an object from the given world at the given pixel location. If multiple objects
     * exist at the one location, this method returns the top-most one according to
     * paint order.
     * 
     * @param x
     *            The x-coordinate
     * @param y
     *            The y-coordinate
     */
    private static Actor getObject(World world, int x, int y)
    {
        if (world == null) {
            return null;
        }
        
        int timeout = READ_LOCK_TIMEOUT;
        try {
            if (lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {

                Collection<?> objectsThere = WorldVisitor.getObjectsAtPixel(world, x, y);
                if (objectsThere.isEmpty()) {
                    lock.readLock().unlock();
                    return null;
                }

                Iterator<?> iter = objectsThere.iterator();
                Actor topmostActor = (Actor) iter.next();
                int seq = ActorVisitor.getLastPaintSeqNum(topmostActor);

                while (iter.hasNext()) {
                    Actor actor = (Actor) iter.next();
                    int actorSeq = ActorVisitor.getLastPaintSeqNum(actor);
                    if (actorSeq > seq) {
                        topmostActor = actor;
                        seq = actorSeq;
                    }
                }
                
                lock.readLock().unlock();
                return topmostActor;
            }
        }
        catch (InterruptedException ie) {}

        return null;
    }

    /*
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e)
    {
        worldCanvas.requestFocusInWindow();
    }

    /*
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e)
    {
        if (dragActor != null) {
            dragActorMoved = false;
            Simulation.getInstance().runLater(new SimulationRunnable() {
                private Actor dragActor = WorldHandler.this.dragActor;
                private int dragBeginX = WorldHandler.this.dragBeginX;
                private int dragBeginY = WorldHandler.this.dragBeginY;
                @Override
                public void run()
                {
                    ActorVisitor.setLocationInPixels(dragActor, dragBeginX, dragBeginY);
                    repaint();
                }
            });
        }
    }

    /**
     * Request a repaints of world
     */
    public void repaint()
    {
        worldCanvas.repaint();
    }
    
    /**
     * Request a repaint of the world, and wait (with a timeout) until the repaint actually occurs.
     */
    public void repaintAndWait()
    {
        worldCanvas.repaint();

        boolean isWorldLocked = lock.isWriteLockedByCurrentThread();
        
        synchronized (repaintLock) {
            // If the world lock is held, as it should be unless this method is called from
            // a user-created thread, we should unlock it to allow the repaint to occur.
            if (isWorldLocked) {
                lock.writeLock().unlock();
            }
            
            // When the repaint actually happens, repainted() will be called, which
            // sets isRepaintPending false and signals repaintLock.
            isRepaintPending = true;
            try {
                do {
                    repaintLock.wait(100);
                } while (isRepaintPending);
            }
            catch (InterruptedException ie) {
                throw new ActInterruptedException();
            }
            finally {
                isRepaintPending = false; // in case our wait interrupted/timed out
                if (isWorldLocked) {
                    lock.writeLock().lock();
                }
            }
        }
    }

    /**
     * The world has been painted.
     */
    public void repainted()
    {
        synchronized (repaintLock) {
            if (isRepaintPending) {
                isRepaintPending = false;
                repaintLock.notify();
            }
        }
        Simulation.getInstance().worldRepainted();
    }

    @Override
    @OnThread(Tag.Swing)
    public void keyTyped(KeyEvent e) {}

    @Override
    @OnThread(Tag.Swing)
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (dragActor != null) {
                dragActorMoved = false;
                Simulation.getInstance().runLater(new SimulationRunnable() {
                    private Actor dragActor = WorldHandler.this.dragActor;
                    private int dragBeginX = WorldHandler.this.dragBeginX;
                    private int dragBeginY = WorldHandler.this.dragBeginY;
                    @Override
                    public void run()
                    {
                        ActorVisitor.setLocationInPixels(dragActor, dragBeginX, dragBeginY);
                        repaint();
                    }
                });
                dragActor = null;
                worldCanvas.setCursor(defaultCursor);
            }
        }
    }

    @Override
    @OnThread(Tag.Swing)
    public void keyReleased(KeyEvent e)
    {
        //TODO: is this really necessary?
        worldCanvas.requestFocus();
    }

    /**
     * Get the world lock, used to control access to the world.
     */
    public ReentrantReadWriteLock getWorldLock()
    {
        return lock;
    }
    
    /**
     * Instantiate a new world and do any initialisation needed to activate that
     * world.
     * 
     * @param className The fully qualified name of the world class to instantiate
     *                  if a specific class is wanted.  If null, use the most recently
     *                  instantiated world class.
     */
    public void instantiateNewWorld(String className)
    {
        handlerDelegate.instantiateNewWorld(className, () -> worldInstantiationError());
    }

    /**
     * Notify that construction of a new world has started.  Note that this method
     * has a special breakpoint set by GreenfootDebugHandler, so do not remove/rename
     * without also editing that code.
     */
    public void setInitialisingWorld(World world)
    {
        this.initialisingWorld = world;
        handlerDelegate.initialisingWorld();
    }

    /** 
     * Removes the current world. This can be called from any thread.
     */
    public synchronized void discardWorld()
    {
        if (world == null)
        {
            return;
        }
        
        handlerDelegate.discardWorld(world); 
        final World discardedWorld = world;
        world = null;

        Simulation.getInstance().runLater(() -> {
            worldCanvas.setWorld(null);
            fireWorldRemovedEvent(discardedWorld);
        });
    }

    /**
     * Check whether a world has been set (via {@link #setWorld()}) since the "world is set" flag was last cleared.
     */
    public synchronized boolean checkWorldSet()
    {
        return worldIsSet;
    }

    /**
     * Clear the "world is set" flag.
     */
    public synchronized void clearWorldSet()
    {
        worldIsSet = false;
    }

    /**
     * Sets a new world.
     * 
     * @param world  The new world. Must not be null.
     * @param byUserCode Was this world set by a call to Greenfoot.setWorld (which thus would
     *                   have come from the user's code)?  If false, it means it was set by our own
     *                   internal code, e.g. initialisation during standalone, or GUI interactions
     *                   in the IDE.
     */
    public synchronized void setWorld(final World world, boolean byUserCode)
    {
        worldIsSet = true;
        
        handlerDelegate.setWorld(this.world, world);
        mousePollingManager.setWorldLocator(new WorldLocator() {
            @Override
            public Actor getTopMostActorAt(MouseEvent e)
            {
                Point p = new Point(e.getX(), e.getY());
                return getObject(world, p.x, p.y);
            }

            @Override
            public int getTranslatedX(MouseEvent e)
            {
                Point p = new Point(e.getX(), e.getY());
                return WorldVisitor.toCellFloor(world, p.x);
            }

            @Override
            public int getTranslatedY(MouseEvent e)
            {
                Point p = new Point(e.getX(), e.getY());
                return WorldVisitor.toCellFloor(world, p.y);
            }
        });
        this.world = world;
        
        Simulation.getInstance().runLater(() -> {
            if(worldCanvas != null) {
                worldCanvas.setWorld(world);
            }
            fireWorldCreatedEvent(world);
        });

        worldChanged(byUserCode);
    }

    /**
     * This is a special method which will have a breakpoint set by the GreenfootDebugHandler
     * class.  Do not remove or rename without also changing that class.
     * 
     * @param byUserCode Was this world set by a call to Greenfoot.setWorld (which thus would
     *                   have come from the user's code)?  If false, it means it was set by our own
     *                   internal code, e.g. initialisation during standalone, or GUI interactions
     *                   in the IDE.  This param is marked unused but actually
     *                   GreenfootDebugHandler will inspect it via JDI
     */
    private void worldChanged(boolean byUserCode)
    {
    }

    /**
     * This is a special method which will have a breakpoint set by the GreenfootDebugHandler
     * class.  Do not remove or rename without also changing that class.
     * It is called where there is an error instantiated the world class
     * (as a result of a user interactive creation, not user code)
     */
    private void worldInstantiationError()
    {
    }

    /**
     * Return the currently active world.
     */
    public synchronized World getWorld()
    {
        if (world == null) {
            return initialisingWorld;
        }
        else {
            return world;
        }
    }
    
    /**
     * Checks if there is a world set.
     * 
     * This is not the same as checking if getWorld() is null, because getWorld()
     * can return a world being initialised.  This method checks if a world has
     * actually been set.
     */
    public synchronized boolean hasWorld()
    {
        return world != null;
    }

    /**
     * Handle drop of actors. Handles QuickAdd
     * 
     * When existing actors are dragged around in the world, that uses drag -- drop is *not* called for those
     */
    public boolean drop(Object o, Point p)
    {
        final World world = this.world;
        
        int maxHeight = WorldVisitor.getHeightInPixels(world);
        int maxWidth = WorldVisitor.getWidthInPixels(world);
        final int x = (int) p.getX();
        final int y = (int) p.getY();

        if (x >= maxWidth || y >= maxHeight || x < 0 || y < 0) {
            return false;
        }
        else if (o instanceof Actor && ActorVisitor.getWorld((Actor) o) == null) {
            // object received from the inspector via the Get button.
            Actor actor = (Actor) o;
            addActorAtPixel(actor, x, y);
            return true;
        }
        else if (o instanceof Actor) {
            final Actor actor = (Actor) o;
            if (ActorVisitor.getWorld(actor) == null) {
                // Under some strange circumstances the world can be null here.
                // This can happen in the GridWorld scenario because it
                // overrides World.addObject().
                return false;
            }
            Simulation.getInstance().runLater(() -> ActorVisitor.setLocationInPixels(actor, x, y));
            dragActorMoved = true;
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Handle drag on actors that are already in the world.
     * 
     * <p>This is called on the Swing event dispatch thread.
     */
    public boolean drag(Object o, Point p)
    {
        World world = this.world;
        if (o instanceof Actor && world != null) {
            int x = WorldVisitor.toCellFloor(world, (int) p.getX() + dragOffsetX);
            int y = WorldVisitor.toCellFloor(world, (int) p.getY() + dragOffsetY);
            final Actor actor = (Actor) o;
            try {
                int oldX = ActorVisitor.getX(actor);
                int oldY = ActorVisitor.getY(actor);

                if (oldX != x || oldY != y) {
                    if (x < WorldVisitor.getWidthInCells(world) && y < WorldVisitor.getHeightInCells(world)
                            && x >= 0 && y >= 0) {
                        WriteLock writeLock = lock.writeLock();
                        // The only reason we would fail to obtain the lock is if a repaint
                        // is happening at this very instant. That shouldn't be too much of
                        // a problem; it will mean a slight glitch in the drag, probably not
                        // noticeable.
                        if (writeLock.tryLock()) {
                            ActorVisitor.setLocationInPixels(actor,
                                    (int) p.getX() + dragOffsetX,
                                    (int) p.getY() + dragOffsetY);
                            writeLock.unlock();
                            dragActorMoved = true;
                            repaint();
                        }
                    }
                    else {
                        WriteLock writeLock = lock.writeLock();
                        if (writeLock.tryLock()) {
                            ActorVisitor.setLocationInPixels(actor, dragBeginX, dragBeginY);
                            x = WorldVisitor.toCellFloor(getWorld(), dragBeginX);
                            y = WorldVisitor.toCellFloor(getWorld(), dragBeginY);
                            writeLock.unlock();
                            
                            dragActorMoved = false; // Pinged back to where it was

                            repaint();
                        }
                        return false;
                    }
                }
            }
            catch (IndexOutOfBoundsException e) {}
            catch (IllegalStateException e) {
                // If World.addObject() has been overridden the actor might not
                // have been added to the world and we will get this exception
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Adds the object where the mouse event occurred.
     * 
     * @return true if successful, or false if the mouse event was outside the world bounds.
     */
    public synchronized boolean addObjectAtEvent(Actor actor, MouseEvent e)
    {
        Component source = (Component) e.getSource();
        if (source != worldCanvas) {
            e = SwingUtilities.convertMouseEvent(source, e, worldCanvas);
        }
        int xPixel = e.getX();
        int yPixel = e.getY();
        return addActorAtPixel(actor, xPixel, yPixel);
    }

    /**
     * Add an actor at the given pixel co-ordinates. The co-ordinates are translated
     * into cell co-ordinates, and the actor is added at those cell co-ordinates, if they
     * are within the world.
     * 
     * @return  true if the Actor was added into the world; false if the co-ordinates were
     *          outside the world.
     */
    public boolean addActorAtPixel(final Actor actor, int xPixel, int yPixel)
    {
        final World world = this.world;
        final int x = WorldVisitor.toCellFloor(world, xPixel);
        final int y = WorldVisitor.toCellFloor(world, yPixel);
        if (x < WorldVisitor.getWidthInCells(world) && y < WorldVisitor.getHeightInCells(world)
                && x >= 0 && y >= 0) {
            Simulation.getInstance().runLater(() -> {
                world.addObject(actor, x, y);
                // Make sure we repaint after user adds something to the world,
                // otherwise will look like lag:
                Simulation.getInstance().paintRemote(true);
            });
            return true;
        }
        else {
            return false;
        }
    }

    public void dragEnded(Object o)
    {
        if (o instanceof Actor && world != null) {
            final Actor actor = (Actor) o;
            Simulation.getInstance().runLater(() -> world.removeObject(actor));
        }
    }

    public void dragFinished(Object o)
    {
    }

    @OnThread(Tag.Simulation)
    protected void fireWorldCreatedEvent(World newWorld)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        WorldEvent worldEvent = new WorldEvent(newWorld);
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == WorldListener.class) {
                ((WorldListener) listeners[i + 1]).worldCreated(worldEvent);
            }
        }
    }

    @OnThread(Tag.Simulation)
    public void fireWorldRemovedEvent(World discardedWorld)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        WorldEvent worldEvent = new WorldEvent(discardedWorld);
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == WorldListener.class) {
                ((WorldListener) listeners[i + 1]).worldRemoved(worldEvent);
            }
        }
    }

    /**
     * Add a worldListener to listen for when a worlds are created and removed.
     * Events will be delivered on the GUI event thread.
     * 
     * @param l
     *            Listener to add
     */
    public void addWorldListener(WorldListener l)
    {
        listenerList.add(WorldListener.class, l);
    }

    /**
     * Removes a worldListener.
     * 
     * @param l
     *            Listener to remove
     */
    public void removeWorldListener(WorldListener l)
    {
        listenerList.remove(WorldListener.class, l);
    }

    /**
     * Used to indicate the start of a simulation round. For use in the
     * collision checker. Called from the simulation thread.
     * 
     * @see greenfoot.collision.CollisionChecker#startSequence()
     */
    private void startSequence()
    {
        // Guard against world getting nulled concurrently:
        World world = this.world;
        if (world != null) {
            WorldVisitor.startSequence(world);
            mousePollingManager.newActStarted();
        }
    }

    public WorldCanvas getWorldCanvas()
    {
        return worldCanvas;
    }

    public EventListenerList getListenerList()
    {
        return listenerList;
    }

    /**
     * Completes the current drag if it is the given drag ID
     */
    public void finishDrag(int dragId)
    {
        // if the operation was cancelled, add the object back into the
        // world at its original position
        if (this.dragId == dragId)
        {
            if (dragActorMoved)
            {
                // This makes sure that a single (final) setLocation
                // call is received by the actor when dragging ends.
                // This matters if the actor has overridden setLocation
                Simulation.getInstance().runLater(new SimulationRunnable() {
                    private Actor dragActor = WorldHandler.this.dragActor;
                    @Override
                    public void run()
                    {
                        int ax = ActorVisitor.getX(dragActor);
                        int ay = ActorVisitor.getY(dragActor);
                        // First we set the position to be the pre-drag position.
                        // This means that if the user overrides setLocation and
                        // chooses not to call the inherited setLocation, the position
                        // will be as if the drag never happened:
                        ActorVisitor.setLocationInPixels(dragActor, dragBeginX, dragBeginY);
                        dragActor.setLocation(ax, ay);
                    }
                });
            }
            dragActor = null;
        }
    }

    @OnThread(Tag.Simulation)
    public void simulationChanged(SimulationEvent e)
    {
        if (e.getType() == SimulationEvent.NEW_ACT_ROUND) {
            startSequence();
        }
        else {
            inputManager.simulationChanged(e);
        }
    }

    /**
     * Get the object bench if it exists. Otherwise return null.
     */
    public ObjectBenchInterface getObjectBench()
    {
        if(handlerDelegate instanceof ObjectBenchInterface) {
            return (ObjectBenchInterface) handlerDelegate;
        }
        else {
            return null;
        }
    }
    
    public InputManager getInputManager()
    {
        return inputManager;
    }

    /**
     * Get a snapshot of the currently instantiated world or null if no world is
     * instantiated.
     * 
     * Must be called on the EDT.
     */
    @OnThread(Tag.Swing)
    public BufferedImage getSnapShot()
    {
        if (world == null) {
            return null;
        }

        WorldCanvas canvas = getWorldCanvas();
        BufferedImage img = GraphicsUtilities.createCompatibleImage(WorldVisitor.getWidthInPixels(world), WorldVisitor
                .getHeightInPixels(world));
        Graphics2D g = img.createGraphics();
        g.setColor(canvas.getBackground());
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        canvas.paintBackground(g);

        int timeout = READ_LOCK_TIMEOUT;
        // We need to sync when calling the paintObjects
        try {
            if (lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                try {
                    canvas.paintObjects(g);
                }
                finally {
                    lock.readLock().unlock();
                }
            }
        }
        catch (InterruptedException e) {
        }
        return img;
    }

    @Override
    public void listeningEnded()
    {
    }

    @Override
    public void listeningStarted(Object obj)
    {
    }
    
    /**
     * This is a hook called by the World whenever an actor gets added to it. When running in the IDE,
     * this allows names to be assigned to the actors for interaction recording purposes.
     */
    public void objectAddedToWorld(Actor object)
    {
        handlerDelegate.objectAddedToWorld(object);
    }

    /**
     * Ask a question, with a given prompt, to the user (i.e. implement Greenfoot.ask()).
     */
    public String ask(String prompt)
    {
        boolean held = lock.isWriteLockedByCurrentThread();
        if (held)
        {
            lock.writeLock().unlock();
        }
        
        String answer = handlerDelegate.ask(prompt, worldCanvas);
        
        if (held)
        {
            lock.writeLock().lock();
        }
        
        return answer;
    }

    public void continueDragging(int dragId, int x, int y)
    {
        if (dragId == this.dragId)
        {
            drag(dragActor, new Point(x, y));
            // We're gonna need another paint after this:
            Simulation.getInstance().runLater(() -> {
                Simulation.getInstance().paintRemote(true);
            });
        }
    }
}
