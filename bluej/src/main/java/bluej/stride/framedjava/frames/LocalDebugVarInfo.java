/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg 

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
package bluej.stride.framedjava.frames;

import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.VarDisplayInfo;
import bluej.pkgmgr.Package;
import bluej.utility.javafx.FXPlatformSupplier;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Debug information for a local variable in the current stack frame.
 */
public class LocalDebugVarInfo implements DebugVarInfo
{
    private final Package pkg;
    private final Window editorFrame;
    private final VarDisplayInfo info;

    @OnThread(Tag.FXPlatform)
    public LocalDebugVarInfo(Package pkg, Window editorFrame, VarDisplayInfo info)
    {
        this.pkg = pkg;
        this.editorFrame = editorFrame;
        this.info = info;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public Node getDisplay(DebugVarInfo prev)
    {
        FXPlatformSupplier<DebuggerObject> fetch = info.getFetchObject();
        if (fetch != null)
        {
            // Non-null reference: show an inspector icon, like for fields
            Image img = Config.getImageAsFXImage("image.eval.object");
            ImageView view = new ImageView(img);
            view.setOnMouseClicked(e -> {
                pkg.getProject().getInspectorInstance(fetch.get(), null, pkg, null, editorFrame, null);
                // Stop event being passed through to canvas:
                e.consume();
            });
            return view;
        }
        Label l = new Label(info.getValue());
        if (prev != null && !getInternalValueString().equals(prev.getInternalValueString()))
        {
            l.setStyle("-fx-font-weight: bold;-fx-underline:true;");
        }
        return l;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public String getInternalValueString()
    {
        return info.getValue();
    }

    @Override
    @OnThread(Tag.Any)
    public boolean isLocal()
    {
        return true;
    }
}
