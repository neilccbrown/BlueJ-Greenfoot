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

import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.editor.Editor;
import bluej.editor.stride.FrameCatalogue;
import bluej.extensions2.SourceType;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.Package;
import bluej.stride.generic.Frame;
import bluej.utility.javafx.AbstractOperation;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class KotlinTarget extends CompilableTarget
{
    private final String baseName;

    /**
     * 
     * @param pkg The Package this target belongs to
     * @param identifierName The file name without the .kt extension
     */
    public KotlinTarget(Package pkg, String identifierName)
    {
        super(pkg, identifierName, "Kotlin");
        this.baseName = identifierName;
    }

    @Override
    protected File getSourceFile()
    {
        if (null == getPackage())
        {
            return null;
        }
        else
        {
            return new File(getPackage().getPath(), baseName + ".kt");
        }
    }

    /**
     * Compilation of the class represented by this target has begun.
     *
     * @param compilationSequence   compilation sequence identifier which can be used to associate
     *                              related compilation events.
     */
    public void markCompiling(int compilationSequence)
    {
        // The results of compilation will be invalid if the editor contents have not been saved:
        compilationInvalid = (editor != null) ? editor.isModified() : false;

        if (getState() == State.HAS_ERROR)
        {
            setState(State.NEEDS_COMPILE);
        }

        if (editor != null)
        {
            if (editor.compileStarted(compilationSequence))
            {
                setState(State.HAS_ERROR);
            }
        }
    }

    /**
     * Re-initialize the breakpoints which have been set in this
     * class.
     */
    public void reInitBreakpoints()
    {
        if (editor != null && isCompiled())
        {
            editor.reInitBreakpoints();
        }
    }

    @Override
    public void reload()
    {
        if (editor != null) {
            editor.reloadFile();
        }
        else {
            analyseSource();
        }
    }

    @Override
    public boolean hasSourceCode()
    {
        // Kotlin targets only exist if there is source code, otherwise they are a class target:
        return true;
    }

    @Override
    public List<ClassInfo> analyseSource()
    {
        // TODO Implement this (see ClassTarget for what it should do)
        return List.of();
    }

    @Override
    public Editor getEditor()
    {
        // TODO Implement this (see ClassTarget for what it should do)
        return null;
    }

    @Override
    public void remove()
    {
        // TODO Implement this (see ClassTarget for what it should do)
    }

    @Override
    public List<? extends AbstractOperation<Target>> getContextOperations()
    {
        // TODO Implement this (see ClassTarget for what it should do)
        return List.of();
    }

    public void showingInterface(boolean showing)
    {
        // TODO Implement this if Kotlin supports showing the interface (Javadoc) view.
    }

    @Override
    public void generateDoc()
    {
        // TODO Implement this if Kotlin supports showing the interface (Javadoc) view.
    }

    // These are all Blackbox related recording methods that we don't need to implement in Kotlin:
    @Override public void recordJavaEdit(String javaSource, boolean includeOneLineEdits) { }
    @Override public void recordStrideEdit(String javaSource, String strideSource, StrideEditReason reason) { }
    @Override public void recordOpen() { }
    @Override public void recordSelected() { }
    @Override public void recordClose() { }
    @Override public void recordShowErrorIndicators(Collection<Integer> identifiers) { }
    @Override public void recordShowErrorMessage(int identifier, String message, List<String> quickFixes) { }
    @Override public void recordEarlyErrors(List<DiagnosticWithShown> diagnostics, int compilationIdentifier) { }
    @Override public void recordLateErrors(List<DiagnosticWithShown> diagnostics, int compilationIdentifier) { }
    @Override public void recordFix(int errorIdentifier, int fixIndex) { }
    @Override public void recordCodeCompletionStarted(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, int codeCompletionId) { }
    @Override public void recordCodeCompletionEnded(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, String replacement, int codeCompletionId) { }
    @Override public void recordUnknownCommandKey(String enclosingFrameXpath, int cursorIndex, char key) { }
    @Override public void recordShowHideFrameCatalogue(String enclosingFrameXpath, int cursorIndex, boolean show, FrameCatalogue.ShowReason reason) { }
    @Override public void recordViewModeChange(String enclosingFrameXpath, int cursorIndex, Frame.View oldView, Frame.View newView, Frame.ViewChangeReason reason) { }
}
