package bluej.extensions;

import bluej.compiler.JobQueue;
import bluej.pkgmgr.ClassTarget;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;

import java.util.Map;
import java.util.HashMap;
import bluej.pkgmgr.Package;
import bluej.views.*;

/**
 * This should behave as much as possible like the Java reflection API.
 * It will not be the same thing but every effort is made to avoid differences.
 * The reasoning behind it is that is is no good to create a new standard when there 
 * is already one that can be used.
 * 
 * @version $Id: BClass.java 1686 2003-03-10 13:02:09Z damiano $
 */
public class BClass
{
    private Package  bluej_pkg;
    private final ClassTarget classTarget;
    private final Class loadedClass;
    private final View  bluej_view;


    /**
     * For use only by the bluej.extensions package
     */
    BClass (Package i_bluej_pkg, ClassTarget classTarget)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = classTarget;
        this.loadedClass = bluej_pkg.loadClass (classTarget.getQualifiedName());
        bluej_view = View.getView (loadedClass);
    }

    BClass (Package i_bluej_pkg, Class systemClass)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = null;
        this.loadedClass = systemClass;
        bluej_view = View.getView (loadedClass);
    }


    private BClass (Package i_bluej_pkg, View view)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = null;
        this.loadedClass = bluej_pkg.loadClass (view.getQualifiedName());
        bluej_view = view;
    }

    /**
     * For extensions use only.
     */    
    BClass (Package i_bluej_pkg, String className)
    {
        bluej_pkg = i_bluej_pkg;
        this.classTarget = null;
        this.loadedClass = bluej_pkg.loadClass (transJavaToClass(className));
        bluej_view = View.getView (loadedClass);
    }


    /**
     * Gets the Java Class being hidden by this BClass, this means showing the real Object...
     * This is the core point where you can get more information about the class. Es:
     * What is the real class being hidden
     * Is it an array
     * What is the typ of the array element
     * 
     * NOTE: This is for INFORMATION ONLY, if you want to interact with BlueJ you MUST
     * use the methods proviced in BClass
     */
    public Class getJavaClass ()
      {
      return loadedClass;
      }

    /**
     * Gets the owning Package of this class
     * @return the originator
     */
    public BPackage getPackage()
    {
        return new BPackage (bluej_pkg);
    }
    
      
    /**
     * Checks to see if a class has been compiled. Ignored and returns <code>true</code> if it
     * is a virtual class.
     * @return <code>true</code> if the class has been compiled and has not been modified since.
     */
    public boolean isCompiled()
    {
        if (classTarget == null) return true;

        return classTarget.isCompiled();
    }
    
    /**
     * Compile this class, and any dependants. Performed synchronously.  Ignored and returns 
     * <code>true</code> if it is a virtual class.
     * @return <code>true</code> if the compilation was successful. 
     */
    public boolean compile()
    {
        if (classTarget == null) return true;
        if (isCompiled()) return true;
        classTarget.compile (null);
        JobQueue.getJobQueue().waitForEmptyQueue();
        return isCompiled();
    }

    /**
     * Gets the superclass to this one.
     * This must remain here othervise there would be no way to make a superclass of a BClass
     * 
     * @return the immediate superclass to this one, or <code>null</code> if it has none
     */
    public BClass getSuper()
    {
        View sup = bluej_view.getSuper();

        if ( sup == null ) return null;
        
        return new BClass (bluej_pkg, sup);
    }
    
    /**
     * As From reflection: gets all constructors of this class
     * NOTE: If the class is NOT compiled it WILL return a zero len constructors array
     * 
     * @return an array of constructors, zero len array if none or invalid
     */
    public BConstructor[] getConstructors()
        {
        if ( ! isCompiled() ) return new BConstructor[0];

        ConstructorView[] constructorViews = bluej_view.getConstructors();
        BConstructor[] result = new BConstructor [constructorViews.length];
        for (int index=0; index<constructorViews.length; index++) 
            result[index] = new BConstructor (bluej_pkg, constructorViews[index]);

        return result;
        }
     
    /**
     * Gets a constructor for this class complying with the given criteria
     * @param signature the signature of the required constructor
     * @return the requested constructor of this class, or <code>null</code> if
     * the class has not been compiled or the constructor cannot be found.
     */
    public BConstructor getConstructor (Class[] signature)
        {
        if (!isCompiled()) return null;
        
        ConstructorView[] constructorViews = bluej_view.getConstructors();
        for (int index=0; index<constructorViews.length; index++) 
            {
            BConstructor aConstr = new BConstructor (bluej_pkg, constructorViews[index]);
            if (aConstr.matches (signature)) return aConstr;
            }
        return null;
        }


    /**
     * Gets the declared method of this class
     * @return the methods of this class
     */
    public BMethod[] getDeclaredMethods()
    {
        if (!isCompiled()) return new BMethod[0];
        
        MethodView[] methodView = bluej_view.getDeclaredMethods();
        BMethod[] methods = new BMethod [methodView.length];

        for (int index=0; index<methods.length; index++)
            methods[index] = new BMethod (bluej_pkg, methodView[index] );

        return methods;
    }

    /**
     * Gets the declared method of this class with the given signature
     * @return the methods of this class
     */
    public BMethod getDeclaredMethod(String methodName, Class[] params )
        {
        if (!isCompiled()) return null;
        
        MethodView[] methodView = bluej_view.getDeclaredMethods();
        BMethod[] methods = new BMethod [methodView.length];

        for (int index=0; index<methods.length; index++)
            {
            BMethod aResul = new BMethod (bluej_pkg, methodView[index]);
            if ( aResul.matches(methodName, params) ) return aResul;
            }

        return null;
        }


    /**
     * Returns all BFields of this Class
     */
    public BField[] getFields()
        {
        if (!isCompiled()) return null;

        FieldView[] fieldView = bluej_view.getAllFields();
        BField[] bFields = new BField [fieldView.length];
        for ( int index=0; index<fieldView.length; index++)
            bFields[index] = new BField (fieldView[index]);
            
        return bFields;
        }


    /**
     * Returns a specific Field of this Class
     */
    public BField getField(String fieldName)
        {
        if (!isCompiled()) return null;
        if ( fieldName == null ) return null;
        
        FieldView[] fieldView = bluej_view.getAllFields();
        for ( int index=0; index<fieldView.length; index++)
            {
            BField result = new BField (fieldView[index]);
            if ( result.matches(fieldName) ) return result;
            }
            
        return null;
        }


     // ====================== UTILITY AREA ====================================
     private static Map primiMap;

     static
        {
        // This will be executed once when this class is loaded
        primiMap = new HashMap();
        primiMap.put ("boolean", "Z");
        primiMap.put ("byte", "B");
        primiMap.put ("short", "S");
        primiMap.put ("char", "C");
        primiMap.put ("int", "I");
        primiMap.put ("long", "J");
        primiMap.put ("float", "F");
        primiMap.put ("double", "D");
        }

    /**
     * Needed to convert java style class names to classloaded class names.
     */
    private String transJavaToClass ( String javaStyle )
        {
        String className = javaStyle;

        int arrayCount = 0;
        while (className.endsWith ("[]")) 
          {
          // Counts how may arrays are in this class name
          arrayCount++;
          className = className.substring (0, className.length()-2);
          }

        // No array around, nothing to do.  
        if (arrayCount <= 0) return className;
        
        String replace = (String)BClass.primiMap.get(className);

        // If I can substitute the name I will do it
        if (replace != null)  className = replace;
        else                  className = "L"+className+";";
            
        while (arrayCount-- > 0) className = "["+className;
          
        return className;
        }

    }   