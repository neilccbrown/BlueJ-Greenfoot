package greenfoot.guifx.classes;

import bluej.Config;
import bluej.extensions.SourceType;
import bluej.utility.Debug;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.guifx.GreenfootStage;
import greenfoot.util.GreenfootUtil;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A dialog showing the possible importable classes in the "common" directory in the Greenfoot
 * installation.  The user can select a class and see the documentation.
 */
public class ImportClassDialog extends Dialog<File>
{
    private final ClassDisplaySelectionManager classDisplaySelectionManager = new ClassDisplaySelectionManager();
    // Maps fully-qualified class names to originating files:
    private final Map<String, File> filesForQualifiedClasses = new HashMap<>();

    /**
     * Create a dialog, with the given parent GreenfootStage
     */
    public ImportClassDialog(GreenfootStage greenfootStage)
    {
        initModality(Modality.APPLICATION_MODAL);
        initOwner(greenfootStage);
        ClassGroup classGroup = new ClassGroup(greenfootStage);
        List<ImportableClassInfo> foundClasses = findImportableClasses(new File(Config.getGreenfootLibDir(), "common"));
        Collections.sort(foundClasses, Comparator.comparing(c -> c.getDisplayName()));
        classGroup.getLiveClasses().addAll(foundClasses);
        classGroup.updateAfterAdd();
        if (!foundClasses.isEmpty())
        {
            classDisplaySelectionManager.select(foundClasses.get(0).getDisplay(greenfootStage));
        }
        for (ImportableClassInfo foundClass : foundClasses)
        {
            filesForQualifiedClasses.put(foundClass.getQualifiedName(), foundClass.file);
        }
        
        WebView docView = new WebView();
        classDisplaySelectionManager.addSelectionListener(selection -> {
            File file = selection == null ? null : filesForQualifiedClasses.get(selection.getQualifiedName());
            // Hide doc view unless we successfully load:
            docView.setVisible(false);
            if (file != null)
            {
                File htmlFile = new File(GreenfootUtil.removeExtension(file.getAbsolutePath()) + ".html");
                if (htmlFile.exists())
                {
                    try
                    {
                        docView.getEngine().load(htmlFile.toURI().toURL().toExternalForm());
                        docView.setVisible(true);
                    }
                    catch (MalformedURLException e)
                    {
                        Debug.reportError(e);
                    }
                }
            }
        });
        
        getDialogPane().setContent(new BorderPane(docView, null, null, null, classGroup));
        BorderPane.setMargin(classGroup, new Insets(0, 8, 0, 0));
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        JavaFXUtil.addStyleClass(getDialogPane(), "import-class-dialog");
        setResizable(true);
        setWidth(700);
        setHeight(550);
        Config.addGreenfootStylesheets(getDialogPane().getScene());
        
        setResultConverter(bt -> {
            if (bt == ButtonType.OK)
            {
                ClassDisplay selected = classDisplaySelectionManager.getSelected();
                if (selected != null)
                {
                    return filesForQualifiedClasses.get(selected.getQualifiedName());
                }
            }
            return null;
        });
    }

    /**
     * Find all the importable classes in the given directory and all its subdirectories.
     * @param dir The directory to search (must be non null)
     * @return The list of all classes found.
     */
    private List<ImportableClassInfo> findImportableClasses(File dir)
    {
        List<ImportableClassInfo> foundClasses = new ArrayList<>();
        // List all files before all directories:
        File[] files = dir.listFiles(new ImportableClassesFileFilter());
        if (files != null)
        {
            for (File file : files)
            {
                foundClasses.add(new ImportableClassInfo(file));
            }
        }

        // List all directories:
        File[] folders = dir.listFiles(new ImportableFoldersFileFilter());
        if (folders != null)
        {
            for (File folder : folders)
            {
                // Recurse to process sub-directories:
                foundClasses.addAll(findImportableClasses(folder));
            }
        }
        return foundClasses;
    }

    /**
     * Filter to find importable items (Java, Stride, Class)
     */
    private static class ImportableClassesFileFilter implements FileFilter
    {
        @Override
        public boolean accept(File pathname)
        {
            return pathname.getAbsolutePath().endsWith(".class")
                    || pathname.getAbsolutePath().endsWith("." + SourceType.Java.toString().toLowerCase())
                    || pathname.getAbsolutePath().endsWith("." + SourceType.Stride.toString().toLowerCase());
        }
    }

    /**
     * Filter to find folders
     */
    private static class ImportableFoldersFileFilter implements FileFilter
    {
        @Override
        public boolean accept(File pathname)
        {
            return pathname.isDirectory();
        }
    }

    /**
     * Looks for an image that might be associated with the given class.
     *
     * So given /foo/Crab.java, /foo/Crab.stride or /foo/Crab.class, 
     * it looks (case insensitive) for /foo/crab.png, /foo/Crab.jpg, etc
     * 
     * @param classFile The original file, the extension of which will be ignored.
     * @return The image file found (arbitrary pick if multiple), or null if none found.
     */
    private static File findImage(File classFile)
    {
        String[] extensions = ImageIO.getReaderFileSuffixes();

        File directory = classFile.getAbsoluteFile().getParentFile();
        String stemName = GreenfootUtil.removeExtension(classFile.getAbsoluteFile().getName());

        File[] allFiles = directory.listFiles();

        if (allFiles == null)
        {
            return null;
        }

        for (File f : allFiles)
        {
            for (String ext : extensions)
            {
                if (f.getName().equalsIgnoreCase(stemName + "." + ext))
                {
                    return f;
                }
            }
        }

        return null;
    }


    /**
     * A ClassInfo used for display.  Overrides parent to remove any context menus, and
     * stores the file associated with the class.
     */
    private class ImportableClassInfo extends ClassInfo
    {
        private final File file;
        
        public ImportableClassInfo(File file)
        {
            super(
                GreenfootUtil.removeExtension(file.getName()),
                GreenfootUtil.removeExtension(file.getName()), 
                JavaFXUtil.loadImage(findImage(file)),
                Collections.emptyList(),
                classDisplaySelectionManager);
            this.file = file;
        }

        @Override
        protected void setupClassDisplay(GreenfootStage greenfootStage, ClassDisplay display)
        {
            // No context menus
            display.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                {
                    setResult(file);
                    close();
                }
            });
        }
    }

}