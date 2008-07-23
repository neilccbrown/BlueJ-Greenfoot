package bluej.pkgmgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reference to the BlueJ package file(s). This includes references to the old
 * file called bluej.pkg as well as the current file named package.bluej.
 * 
 * There are (or will be) three versions of BlueJ that handles these package
 * files differently:
 * 
 * <ul>
 * <li><i>Old BlueJ:</i> support only the .pkg extension. This is all versions
 * before BlueJ 2.3.0.</li>
 * <li><i>Transition BlueJ:</i> support both .pkg and .bluej extension. If .pkg
 * exists, it will load from this file and it will also write to it. If .pkg
 * doesn't exist it is NOT created. It will always attempt to save to .bluej.
 * The first transition version is BlueJ 2.3.0.</li>
 * <li><i>New BlueJ:</i> supports mostly the .bluej extension. If .pkg exists it
 * will load from it. When saving, if it manages to save to .bluej, it will
 * delete .pkg. If it can't save to .bluej it will attempt to save to .pkg if it
 * exists.</li>
 * <ul>
 * 
 * One implication of this is that a project that has been opened with a New
 * version of BlueJ can not be opened with an Old version of BlueJ. The
 * alternative would be to keep the .pkg around forever, which is not what we
 * want.
 * 
 * @author Poul Henriksen
 */
public class BlueJPackageFile
    implements PackageFile
{
    private static final String pkgfileName = "package.bluej";
    private static final String oldPkgfileName = "bluej.pkg";

    private File dir;
    private File pkgFile;
    private File oldPkgFile;

    public BlueJPackageFile(File dir)
    {
        this.dir = dir;
        this.pkgFile = new File(dir, pkgfileName);
        this.oldPkgFile = new File(dir, oldPkgfileName);
    }

    public String toString()
    {
        return "BlueJ package file in: " + dir.toString();
    }

    /**
     * Whether a BlueJ package file exists in this directory.
     */
    public static boolean exists(File dir)
    {
        if (dir == null)
            return false;

        // don't try to test Windows root directories (you'll get in
        // trouble with disks that are not in drives...).

        if (dir.getPath().endsWith(":\\"))
            return false;

        if (!dir.isDirectory())
            return false;

        File packageFile = new File(dir, pkgfileName);
        if (packageFile.exists()) {
            return true;
        }

        File oldPackageFile = new File(dir, oldPkgfileName);
        return oldPackageFile.exists();
    }

    /**
     * Will first try to load from the old package file (.pkg) if that fails, it
     * will try to load from the new one (package.bluej)
     */
    public void load(Properties p)
        throws IOException
    {
        FileInputStream input = null;
        try {
            // First, try to load from the old package file since, if it exists,
            // will be the most up-to-date.
            if (oldPkgFile.canRead()) {
                input = new FileInputStream(oldPkgFile);
            }
            else if (pkgFile.canRead()) {
                input = new FileInputStream(pkgFile);
            }
            else {
                throw new IOException("Can't read from package file(s): " + this);
            }
            p.load(input);
        }
        finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Save the given properties to the file.
     * <p>
     * 
     * Store properties to both package files. It always try to store to the
     * pkgFile, and if the oldPkgFile exists it will also try to store to that.
     * It should fail if the oldPkgFile exists but can't be written, because
     * this is the first one to be loaded if both exists and it would then
     * result in inconsistent properties. If it manages to store to the
     * oldPkgFile it doesn't matter if it fails to store to the new one, since
     * whenever the old one is present, that will be loaded first in all
     * versions of BlueJ
     * 
     * @throws IOException if something goes wrong while trying to write the
     *             properties.
     */
    public void save(Properties props)
        throws IOException
    {

        boolean oldPkgSaved = false;
        if (oldPkgFile.exists()) {
            if (!oldPkgFile.canWrite()) {
                throw new IOException("BlueJ package file not writable: " + oldPkgFile);
            }
            saveToFile(props, oldPkgFile);
            oldPkgSaved = true;
        }

        pkgFile.createNewFile();
        if (!pkgFile.canWrite()) {
            if (!oldPkgSaved) {
                // Not OK, the oldPkgFile didn't exist.
                throw new IOException("BlueJ package file not writable: " + pkgFile);
            }
            else {
                // It is OK, we successfully saved to the oldPkgFile anyway, so
                // just return now.
                return;
            }
        }
        else {
            saveToFile(props, pkgFile);
        }
    }

    private void saveToFile(Properties props, File file)
        throws IOException
    {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            String header = "BlueJ package file";
            props.store(output, header);
        }
        catch (IOException e) {
            throw new IOException("Error when storing properties to BlueJ package file: " + file);
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Check if the given name matches the name of a BlueJ package file (either
     * bluej.pkg or package.bluej).
     */
    public static boolean isPackageFileName(String name)
    {
        return name.equals(pkgfileName) || name.equals(oldPkgfileName);
    }

    /**
     * Creates the two package files if they don't already exist. If only
     * package.bluej exists it will not create bluej.pkg.
     * 
     * @param dir The directory to create package files in.
     * @throws IOException If the package file(s) could not be created.
     * 
     */
    public static boolean create(File dir)
        throws IOException
    {
        File pkgFile = new File(dir, pkgfileName);
        File oldPkgFile = new File(dir, oldPkgfileName);

        boolean created = false;
        if (pkgFile.exists() && !oldPkgFile.exists()) {
            return false;
        }

        if (!pkgFile.exists()) {
            pkgFile.createNewFile();
            created = true;
        }
        if (!oldPkgFile.exists()) {
            oldPkgFile.createNewFile();
            if (!created) {
                created = true;
            }
        }
        return created;
    }
}
