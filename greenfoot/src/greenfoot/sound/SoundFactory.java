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

import greenfoot.util.GreenfootUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Class responsible for creating Sounds and loading them.
 * 
 * @author Poul Henriksen
 */
public class SoundFactory 
{

    /** singleton */
    private static SoundFactory instance;    

    private SoundCollection soundCollection;
    
    /**
     * Only use clips when the size of the clip is below this value.
     */
    private static final int maxClipSize = 1;//500 * 1000;

    private SoundFactory()
    {
       soundCollection = new SoundCollection();
    }

    public synchronized static SoundFactory getInstance()
    {
        if (instance == null) {
            instance = new SoundFactory();
        }
        return instance;
    }
    
    public SoundCollection getSoundCollection() {
        return soundCollection;
    }

   
    /**
     * Creates the sound from file.
     * 
     * @param file Name of a file or an url
     * @throws LineUnavailableException if a matching line is not available due to resource restrictions
     * @throws FileNotFoundException if the file cannot be found.
     * @throws IOException if an I/O exception occurs
     * @throws SecurityException if a matching line is not available due to security restrictions
     * @throws UnsupportedAudioFileException if the URL does not point to valid audio file data
     * @throws IllegalArgumentException if the system does not support at least one line matching the specified Line.Info object through any installed mixer
     */
    public Sound createSound(final String file) throws IOException, UnsupportedAudioFileException, LineUnavailableException 
    {
      
            // First, determine the size of the sound, if possible
            URL url = GreenfootUtil.getURL(file, "sounds");
            int size = url.openConnection().getContentLength();

            if (size == -1 || size > maxClipSize) {
                // If we can not get the size, or if it is a big file we stream it
                // in a thread.

                System.out.println("Creating stream: " + file);
                final Sound soundStream = new SoundStream(url, soundCollection);
                return soundStream;
            }
            else {
                System.out.println("Creating clip: " + file);
                // The sound is small enough to be loaded into memory as a clip.
                SoundClip sound = new SoundClip(file, url, soundCollection);
                return sound;
            }
        
    }

}
