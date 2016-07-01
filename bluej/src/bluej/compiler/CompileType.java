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
package bluej.compiler;

/**
 * An enum keeping track of the compile type, in order to decide
 * whether to retain or discard the class files generated by 
 * the compilation.  We automatically compile when the user edits the code, for
 * error checking, but we discard the class files in this case.
 * We only keep the class files if the user explicitly invoked compilation,
 * or if it was an internal compile (e.g. for method invocation) where we
 * want the result.
 */
public enum CompileType
{
    EXPLICIT_USER_COMPILE, // User-invoked compile, should keep resulting classes
    ERROR_CHECK_ONLY, // Auto-compilation to check for errors, discard class files
    INTERNAL_COMPILE, // Internal compile, keep resulting classes
    EXTENSION, // Extension triggered it, keep resulting classes
    INDIRECT_USER_COMPILE; // Compile of user class, caused by indirect user action (e.g. new class), keep resulting classes
    
    public boolean keepClasses()
    {
        return this != ERROR_CHECK_ONLY;
    }
}