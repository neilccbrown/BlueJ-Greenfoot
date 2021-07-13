/*
 This file is part of the BlueJ program. 
 Copyright (C) 2021  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2.editor;

import bluej.editor.stride.FrameEditor;
import bluej.extensions2.BClass;
import javafx.geometry.Rectangle2D;

/**
 * A class representing an editor for a Stride class.
 * 
 * @since Extension API 3.2 (BlueJ 5.0.2)
 */
public class StrideEditor
{
    private final BClass bClass;
    private final FrameEditor frameEditor;

    StrideEditor(BClass bClass, FrameEditor frameEditor)
    {
        this.bClass = bClass;
        this.frameEditor = frameEditor;
    }

    /**
     * Gets the class that this editor is editing.
     */
    public BClass getBClass()
    {
        return bClass;
    }

    /**
     * Gets the screen bounds of the window that this editor is contained in,
     * if and only if this is the selected tab in its editor window and the window
     * is showing and the window is not minimised.
     * Returns null otherwise.
     * @since Extension API 3.2 (BlueJ 5.0.2)
     */
    public Rectangle2D getScreenBounds()
    {
        return frameEditor.getScreenBoundsIfSelectedTab();
    }

}
