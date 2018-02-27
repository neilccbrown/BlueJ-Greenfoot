/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013,2014,2015,2016  Poul Henriksen and Michael Kolling
 
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
package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Properties;

import rmiextension.wrappers.event.RApplicationListener;
import rmiextension.wrappers.event.RClassListener;
import rmiextension.wrappers.event.RCompileListener;

/**
 * 
 * Interface for accessing BlueJ-functionality
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public interface RBlueJ
    extends java.rmi.Remote
{
    /**
     * Get a stream object that can be used to output to the debug log.
     */
    public RPrintStream getDebugPrinter()
        throws RemoteException;
    
    /**
     * Register a Compile event listener for the project identified by the given path.
     */
    public void addCompileListener(RCompileListener listener, File projectPath)
        throws RemoteException;

    /**
     * Register a remote class event listener
     */
    public void addClassListener(RClassListener listener)
        throws RemoteException;
    
    /**
     * Get a BlueJ property value
     */
    public String getBlueJPropertyString(String property, String def)
        throws RemoteException;

    /**
     * Get a BlueJ extensions property value
     * 
     * @param property  The property whose value to retrieve
     * @param def       The default value to return
     * @return   The property value
     */
    public String getExtensionPropertyString(String property, String def)
        throws RemoteException;


    /**
     * Get a list of all open projects.
     */
    public RProject[] getOpenProjects()
        throws RemoteException;

    /**
     * Get the Bluej "lib" dir.
     */
    public File getSystemLibDir()
        throws RemoteException;

    /**
     * Remove a compile listener.
     * @param listener  The listener to remove
     */
    public void removeCompileListener(RCompileListener listener)
        throws RemoteException;

    /**
     * De-register a remote class event listener.
     */
    public void removeClassListener(RClassListener listener)
        throws RemoteException;

    /**
     * Set an extension property value.
     * @param property  The property key
     * @param value     The value to set
     */
    public void setExtensionPropertyString(String property, String value)
        throws RemoteException;

    /**
     * Exits the entire application.
     * 
     * @throws RemoteException
     */
    public void exit()
        throws RemoteException;

    /**
     * Get the properties that were given on the command line and used 
     * to initialise bluej.Config.
     */
    public Properties getInitialCommandLineProperties()
        throws RemoteException;

    /**
     * Get the directory where user preferences are stored
     */
    public File getUserPrefDir()
        throws RemoteException;

    /**
     * Hide the splash screen, if still currently showing.
     */
    public void hideSplash()
        throws RemoteException;

    public long getBlueJProcessId()
        throws RemoteException;

    public void addApplicationListener(RApplicationListener listener)
        throws RemoteException;
}
