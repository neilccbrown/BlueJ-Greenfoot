package bluej.extensions;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.Target;
import bluej.debugger.ObjectWrapper;



import java.util.List; 
import java.util.ListIterator;
import java.awt.Frame;



/**
 * A wrapper for a single package of a BlueJ project.
 * This represents an open package, and functions relating to that package.
 *
 * @version $Id: BPackage.java 1968 2003-05-21 09:59:49Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
 
public class BPackage
{
    private Identifier packageId;

    /**
     * Constructor for a BPackage.
     */
    BPackage (Identifier aPackageId)
    {
        packageId=aPackageId;
    }


    /**
     * Returns the package's project.
     */
    public BProject getProject() throws ProjectNotOpenException
    {
        Project bluejProject = packageId.getBluejProject();

        return new BProject (new Identifier(bluejProject));
    }



    /**
     * Returns the name of the package. 
     * Returns an empty string if no package name has been set.
     */
    public String getName() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package bluejPkg = packageId.getBluejPackage();

        return bluejPkg.getQualifiedName();
        }
    
    /**
     * Returns the package frame.
     * This can be used (e.g.) as the "parent" frame for positioning modal dialogues.
     */
    public Frame getFrame() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        return packageId.getPackageFrame();
        }

    
    /**
     * Returns the class with the given name in this package.
     * Returns null if the class name does not exist.
     * 
     * @param name the simple name of the required class.
     */
    public BClass getBClass (String name)   
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Project bluejPrj = packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        Target aTarget = bluejPkg.getTarget (name);

        if ( aTarget == null ) return null;
        if ( !(aTarget instanceof ClassTarget)) return null;

        ClassTarget classTarget = (ClassTarget)aTarget;
        
        return new BClass (new Identifier (bluejPrj,bluejPkg, classTarget.getQualifiedName()));
    }
    
    /**
     * Returns an array containing all the classes in this package.
     * If there are no classes an empty array will be returned.
     */
    public BClass[] getBClasses() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Project bluejPrj = packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        String pkgBasename = bluejPkg.getBaseName();
        if ( pkgBasename.length() > 1 ) pkgBasename = pkgBasename+".";
        
        List names = bluejPkg.getAllClassnames();
        
        BClass[] classes = new BClass [names.size()];
        for (ListIterator iter=names.listIterator(); iter.hasNext();) {
            int index=iter.nextIndex();
            String className = pkgBasename+(String)iter.next();
            classes [index] = new BClass (new Identifier (bluejPrj,bluejPkg,className));
        }
        return classes;
    }
    
    /**
     * Returns a wrapper for the object with the given name on BlueJ's object bench.
     * @param name the name of the object as shown on the object bench
     * @return the object, or null if no such object exists.
     */
    public BObject getObject (String instanceName) 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        // The usual check to avoid silly stack trace
        if ( instanceName == null ) return null;

        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame pmf = packageId.getPackageFrame();
        
        ObjectWrapper[] objects = pmf.getObjectBench().getWrappers();
        for (int index=0; index<objects.length; index++) 
            {
            ObjectWrapper wrapper = objects[index];
            if (instanceName.equals(wrapper.getName())) return new BObject (wrapper);
            }
        return null;
    }    

    /**
     * Returns an array of all the Objects on the object bench.
     * The array will be empty if no objects are on the bench.
     */
    public BObject[] getObjects() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame pmf = packageId.getPackageFrame();
   
        ObjectWrapper[] objectWrappers = pmf.getObjectBench().getWrappers();
        BObject[] objects = new BObject [objectWrappers.length];
        for (int index=0; index<objectWrappers.length; index++) {
            ObjectWrapper wrapper = (ObjectWrapper)objectWrappers[index];
            objects[index] = new BObject (wrapper);
        }
        return objects;
    }
    

    /**
     * Compile this package.
     * If forceAll is true it will compile all files otherwise it will compile
     * just the ones that are modified.
     * @param forceAll if <code>true</code> compile all files.
     */
    public void compile (boolean forceAll) 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package bluejPkg = packageId.getBluejPackage();

        if (forceAll) bluejPkg.rebuild(); 
        else bluejPkg.compile();
        }
    
    /**
     * Reloads the entire package.
     * This is used (e.g.) when a new <code>.java</code> file has been added to the package.
     */
    public void reload() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package bluejPkg = packageId.getBluejPackage();

        bluejPkg.reload();
        }

    /**
     * Returns a string representation of the Object
     */
    public String toString () 
      {
      try 
        {
        Package bluejPkg = packageId.getBluejPackage();
        return "BPackage: "+bluejPkg.getQualifiedName();
        }
      catch ( ExtensionException exc )
        {
        return "BPackage: INVALID";  
        }
      }

}
