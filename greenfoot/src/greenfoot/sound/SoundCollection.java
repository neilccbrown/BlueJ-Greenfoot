/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.sound;

import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Contains a collection of sounds that are currently playing. This collections is used for stopping sounds.
 * 
 * @author Poul Henriksen
 */
public class SoundCollection implements SimulationListener, SoundPlaybackListener
{
    /** Sounds currently playing or paused by this SoundCollection. */
    private Set<Sound> playingSounds = new HashSet<Sound>();
    
    /** Sounds paused by the user code. */
    private Set<Sound> pausedSounds = new HashSet<Sound>();    
    
    private volatile boolean ignoreEvents = false;
    
    /**
     * Stop sounds when simulation is disabled (a new world is created).
     */
    public void simulationChanged(SimulationEvent e)
    {
        // TODO: RESET??? pressing reset only pauses the songs and it should stop them instead
        
        if (e.getType() == SimulationEvent.DISABLED) {
            stop();
        }
        else if (e.getType() == SimulationEvent.STOPPED) {
            pause();
        }
        else if (e.getType() == SimulationEvent.STARTED) {
            resume();
        }
    }
    

    /** 
     * Resumes all songs previously paused with a call to pause()
     */
    private void resume()
    {
        
        synchronized (this) {
            ignoreEvents = true;
        }
        for (Sound sound : playingSounds) {
            sound.resume();
        }
        synchronized (this) {
            ignoreEvents = false;
        }
    }


    /**
     * Pauses all sounds currently playing. 
     * 
     */
    private void pause()
    {
        synchronized (this) {
            ignoreEvents = true;
        }
        for (Sound sound : playingSounds) {
            sound.pause();
        }
        synchronized (this) {
            ignoreEvents = false;
        }
    }
    

    /**
     * Stops all sounds.
     * 
     */
    private void stop()
    {
        //System.out.println("Sounds alive: " + playingSounds.size() + " " + pausedSounds.size());
        
        synchronized (this) {
            ignoreEvents = true;
        }
        
        Iterator<Sound> iter = playingSounds.iterator();
        while (iter.hasNext() ) {
            Sound sound = iter.next();
            iter.remove();
            sound.stop();
        }
        
        iter = pausedSounds.iterator();
        while (iter.hasNext() ) {
            Sound sound = iter.next();
            iter.remove();
            sound.stop();
        }
        playingSounds.clear();
        pausedSounds.clear();

        synchronized (this) {
            ignoreEvents = false;
        }
    }

    // Listener callbacks

    public synchronized void playbackStarted(Sound sound)
    {
      //  System.out.println("playbackStarted: " + sound);
        if (!ignoreEvents) {
            playingSounds.add(sound);
            pausedSounds.remove(sound);
        }
    }

    public synchronized void playbackStopped(Sound sound)
    {        
      //  System.out.println("playbackStopped: " + sound);

        if (!ignoreEvents) {
            playingSounds.remove(sound);
            pausedSounds.remove(sound);
        }
    }

    public synchronized void playbackPaused(Sound sound)
    {
      //  System.out.println("playbackPaused: " + sound);

        if (!ignoreEvents) {
            pausedSounds.add(sound);
            playingSounds.remove(sound);
        }
    }
}
