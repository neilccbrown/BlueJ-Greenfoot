/*
 This file is part of the BlueJ program. 
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.pkgmgr.target;

import bluej.Config;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.editor.Editor;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.Package;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CompilableTarget extends DependentTarget
{
    // Is the current node queued for compilation?
    private boolean queued;
    // Whether the current compilation is invalid due to edits since compilation began
    protected boolean compilationInvalid = false;
    //properties map to store values used in the editor from the props (if necessary)
    protected final Map<String, String> properties = new HashMap<String, String>();


    /**
     * Create a new target belonging to the specified package.  Constructor forwarded from DependentTarget.
     */
    public CompilableTarget(Package pkg, String identifierName, String accessibleTargetType)
    {
        super(pkg, identifierName, accessibleTargetType);
    }


    /**
     * Process a double click on this target. That is: open its editor.
     *
     * @param  openInNewWindow if this is true, the editor opens in a new window
     */
    @Override
    public void doubleClick(boolean openInNewWindow)
    {
        Editor editor = getEditor();
        if(editor == null)
        {
            getPackage().showError("error-open-source");
        }
        editor.setEditorVisible(true, openInNewWindow);
    }

    public boolean isQueued()
    {
        return queued;
    }

    public void setQueued(boolean queued)
    {
        this.queued = queued;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type)
    {
        if (Config.isGreenfoot() && type == CompileType.EXPLICIT_USER_COMPILE)
        {
            // We compile the package rather than just the class for explicit compiles in
            // Greenfoot, but mark this target as modified first so that we do also compile
            // this class even if we wouldn't otherwise (and can report the result to the
            // editor, which is expecting to receive it):
            markModified();
            getPackage().getProject().scheduleCompilation(immediate, reason, type, getPackage());
        }
        else
        {
            getPackage().getProject().scheduleCompilation(immediate, reason, type, this);
        }
    }

    public abstract List<ClassInfo> analyseSource();
    public abstract boolean hasSourceCode();

    /**
     * Gets the compiled attribute of the ClassTarget object
     * 
     * @return The compiled value
     */
    public boolean isCompiled()
    {
        return getState() == State.COMPILED;
    }
    public abstract void reload();

    public abstract void reInitBreakpoints();

    /**
     * Remove the step mark in this case
     * (the mark in the editor that shows where execution is)
     */
    public void removeStepMark()
    {
        if (editor != null) {
            editor.removeStepMark();
        }
    }
    public abstract void markCompiling(int compilationSequence);

    /**
     * Retrieves a property from the editor
     */
    @Override
    public String getProperty(String key)
    {
        return properties.get(key);
    }

    /**
     * Sets a property for the editor
     */
    @Override
    public void setProperty(String key, String value)
    {
        properties.put(key, value);
    }

    /**
     * Display a compilation diagnostic (error message), if possible and appropriate. The editor
     * decides if it is appropriate to display the error and may have a policy where eg it only
     * shows a limited number of errors.
     * 
     * @param diagnostic   the compiler-generated diagnostic
     * @param errorIndex   the index of the error in this batch (first error is 0)
     * @param compileType  the type of compilation leading to the error
     * @return    true if the diagnostic was displayed to the user
     */
    public boolean showDiagnostic(Diagnostic diagnostic, int errorIndex, CompileType compileType)
    {
        // If an edit has been made since the compilation started, we don't want to display the
        // error since it may no longer be present, and if it is it will be shown by a later
        // compilation anyway:
        if (compilationInvalid)
        {
            return false;
        }

        Editor ed = getEditor();
        if (ed == null)
        {
            return false;
        }

        setState(State.HAS_ERROR);
        return ed.displayDiagnostic(diagnostic, errorIndex, compileType);
    }
}
