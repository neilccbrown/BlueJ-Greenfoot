package bluej.utility;

import bluej.Config;

import java.awt.*;
import java.io.*;
import java.util.Random;
import java.util.Vector;
import java.awt.event.*;
import javax.swing.*;

/**
 ** Some generally useful utility methods available to all of bluej.
 ** $Id: Utility.java 267 1999-11-10 02:53:02Z mik $
 ** @author Michael Cahill
 ** @author Justin Tan
 ** @author Michael Kolling
 **/
public class Utility
{
    static final String browserCmd1 = Config.getPropString("browserCmd1");
    static final String browserCmd2 = Config.getPropString("browserCmd2");
    static final String winBrowserCmd1 = Config.getPropString("winBrowserCmd1");
    static final String winBrowserCmd2 = Config.getPropString("winBrowserCmd2");

    private static Random random = new Random();
    public static int getRandom(int min, int max)
    {
	int r = random.nextInt();
	r = (r > 0) ? r : -r;

	return min + (r % (max - min));
    }

    /**
     ** Draw a thick line - one of the (many) useful things missing from the AWT
     **/
    public static void drawThickLine(Graphics g, int x1, int y1, int x2, int y2, int thickness)
    {
	// Note: there is some subtlety here to do with rounding
	double perp = Math.atan2(-(y1 - y2), x1 - x2) + Math.PI / 2;
	int[] xPoints = new int[4];
	int[] yPoints = new int[4];
	int ldx = (int)(thickness * Math.cos(perp)) / 2;
	int ldy = (int)(thickness * Math.sin(perp)) / 2;
	int rdx = (int)(thickness * Math.cos(perp));
	int rdy = (int)(thickness * Math.sin(perp));
		
	xPoints[0] = x1 - ldx;
	yPoints[0] = y1 + ldy;
	xPoints[1] = x2 - ldx;
	yPoints[1] = y2 + ldy;
	xPoints[2] = xPoints[1] + rdx;
	yPoints[2] = yPoints[1] - rdy;
	xPoints[3] = xPoints[0] + rdx;
	yPoints[3] = yPoints[0] - rdy;

	g.fillPolygon(xPoints, yPoints, 4);
    }

    /**
     ** Draw a thick rectangle - another of the things missing from the AWT
     **/
    public static void drawThickRect(Graphics g, int x, int y, int width, int height, int thickness)
    {
	for(int i = 0; i < thickness; i++)
	    g.drawRect(x + i, y + i, width - 2*i, height - 2*i);
    }

    /**
     ** Draw stripes over a rectangle - yet another thing missing from the AWT
     **/
    public static void stripeRect(Graphics g, int x, int y, int width, int height, int separation, int thickness)
    {
	for(int offset = 0; offset < width + height; offset += separation)
	    for(int i = 0; i < thickness; i++, offset++) {
		int x1, y1, x2, y2;
				
		if(offset < height)
		    { x1 = x; y1 = y + offset; }
		else
		    { x1 = x + offset - height; y1 = y + height; }
				
		if(offset < width)
		    { x2 = x + offset; y2 = y; }
		else
		    { x2 = x + width; y2 = y + offset - width; }
				
		g.drawLine(x1, y1, x2, y2);
	    }
    }
	
    public static void drawCentredText(Graphics g, String str, int x, int y, int width, int height)
    {
	FontMetrics fm = g.getFontMetrics();

	Shape oldClip = g.getClip();
	g.clipRect(x, y, width, height);
	g.drawString(str, x + (width - fm.stringWidth(str)) / 2,
		     y + (height + fm.getAscent()) / 2);
	g.setClip(oldClip);
    }

    public static void drawRightText(Graphics g, String str, int x, int y, int width, int height)
    {
	FontMetrics fm = g.getFontMetrics();

	Shape oldClip = g.getClip();
	g.clipRect(x, y, width, height);
	g.drawString(str, x + width - fm.stringWidth(str),
		     y + (height + fm.getAscent()) / 2);
	g.setClip(oldClip);
    }

    protected static void swap(Object[] arr, int i, int j)
    {
	Object tmp = arr[i];
	arr[i] = arr[j];
	arr[j] = tmp;
    }

    public static void quicksort(Comparer c, Object[] arr, int left, int right)
    {
	int i, last;

	if(left >= right)	// we're done here
	    return;

	swap(arr, left, (left + right) / 2);
	last = left;
	for(i = left + 1; i <= right; i++)
	    {
		if(c.cmp(arr[i], arr[left]) < 0)
		    swap(arr, ++last, i);
	    }
	swap(arr, left, last);

	quicksort(c, arr, left, last - 1);
	quicksort(c, arr, last + 1, right);
    }

    /**
     ** Splits "string" by "Delimiter"
     ** @param str - the string to be split
     ** @param delimiter - the field delimiter within str
     ** @returns	an array of Strings
     **/
    public static String[] split(String str, String delimiter)
    {
	Vector strings = new Vector();
	int	start = 0;
	int len = str.length();
	int dlen = delimiter.length();
	int offset = str.lastIndexOf(delimiter);		// First of all, find the         
	// Last occurance of the Delimiter
	// Stop empty delimiters
	if (dlen < 1)
	    return null;
	else if(offset < 0)	 // one element
	    {
		String[] result = { str };
		return result;
	    }
			
	//
	// Append the delimiter onto the end if it doesn't already exit
	//
	if (len > offset + dlen) {
	    str += delimiter;
	    len += dlen;
	}
	
	do {
	    // Get the new Offset
	    offset = str.indexOf(delimiter,start);
	    strings.addElement(str.substring(start,offset));
			
	    // Get the new Start position
	    start = offset + dlen;
	} while ((start < len) && (offset != -1));

	// Convert the Vector into an Array of Strings
	String result[] = new String[strings.size()];
	strings.copyInto(result);		
	return result;
    }
	
    /**
     ** Splits "string" into lines (stripping end-of-line characters)
     ** @param str - the string to be split
     ** @returns	an array of Strings
     **/
    public static String[] splitLines(String str)
    {
	return (str == null ? null : split(str, "\n"));
    }

    /**
     * Check whether a string is a valid Java identifier
     */
    public static boolean isIdentifier(String str)
    {
	if (str.length() == 0)
	    return false;
	if (! Character.isJavaIdentifierStart(str.charAt(0)))
	    return false;
	for (int i=1; i < str.length(); i++)
	    if (! Character.isJavaIdentifierPart(str.charAt(i)))
		return false;

	return true;
    }

    /**
     * return a string in which all the '\' characters of the
     * original string are quoted ('\\').
     */
    public static String quoteSloshes(String src)
    {
	StringBuffer buf = new StringBuffer();
		
	for(int i = 0; i < src.length(); i++)
	    {
		if(src.charAt(i) == '\\')
		    buf.append('\\');
		buf.append(src.charAt(i));
	    }
		
	return buf.toString();
    }

    /**
     * Strips package prefix's from full class name.
     * 
     * @return the stripped class name.
     */
    public static String stripPackagePrefix(String fullClassName)
    {
	if(fullClassName != null) {
	    int index = fullClassName.lastIndexOf(".");
	    if(index >= 0)
		return fullClassName.substring(++index);
	}

	return fullClassName;
    }

    /**
     *  return true if the file is a BlueJ package directory
     */
    public static boolean isJBPackage(File f, String pkgFileName) {
	if (f == null)
	    return false;

	if(!f.isDirectory())
	    return false;

	// don't try to test Windows root directories (you'll get in
	// trouble with disks that are not in drives...).

	if(f.getPath().endsWith(":\\"))
	    return false;

	File packageFile = new File(f, pkgFileName);
	return (packageFile.exists());
    }

    /**
     *  return true if the file is a BlueJ package directory
     */
    public static boolean openWebBrowser(String url) {

	String cmd;
	String osname = System.getProperty("os.name");

	try {
	    // try first command, eg "netscape -remote"

	    if(osname != null && osname.startsWith("Windows"))
		cmd = mergeStrings(winBrowserCmd1, url);
	    else
		cmd = mergeStrings(browserCmd1, url);

	    //   Debug.message(cmd);
	    Process p = Runtime.getRuntime().exec(cmd);

	    try {
		// wait for exit code. 0 indicates success, otherwise
		// we try second command
		int exitCode = p.waitFor();

		if(osname != null && osname.startsWith("Windows"))
		    cmd = winBrowserCmd2;
		else
		    cmd = browserCmd2;

		if(exitCode != 0 && cmd != null && cmd.length() > 0) {
		    cmd = mergeStrings(cmd, url);
		    //Debug.message(cmd);
		    p = Runtime.getRuntime().exec(cmd);
		}
	    }
	    catch(InterruptedException e) {
		Debug.reportError("cannot start web browser: " + cmd);
		Debug.reportError("caught exc " + e);
		return false;
	    }
	}
	catch(IOException e) {
	    Debug.reportError("could not start web browser");
	    Debug.reportError("caught exc " + e);
	    return false;
	}
	return true;
    }

    /**
     * merge s2 into s1 at position of first '$'
     */
    public static String mergeStrings (String s1, String s2)
    {
	int pos = s1.indexOf('$');
	if(pos == -1)
	    return s1;
	else
	    return s1.substring(0,pos) + s2 + s1.substring(pos+1);
    }

    /**
     * merge strings in s2 into s1 at positions of '$'
     */
    public static String mergeStrings (String s1, String s2[]) { 
	    for (int current = 0; current < s2.length; current++) {
		    s1 = mergeStrings(s1, s2[current]);
	    }
	    
	    return s1;
    }

    /**
     * typeName - a utility function to fix up Java class names. Class names
     *  as returned by the Class.getName() functions are okay for non-array
     *  classes (we don't need to do anything for them), but are in a funny
     *  format for arrays. "String[]", for example, is shown as
     *  "[Ljava.lang.String;". See the Class.getName() documentation for 
     *  details. Here, we transform the array names into standard Java syntax.
     */
    public static String typeName(String className)
    {
	if( !(className.charAt(0) == '['))
	    return className;

	String name = "";
	while (className.startsWith("[")) {
	    className = className.substring(1);
	    name = name + "[]";
	}
	switch (className.charAt(0)) {
	    case 'L' : name = className.substring(1, className.length()-1)
			      + name;
	               break;
	    case 'B' : name = "byte" + name;
	               break;
	    case 'C' : name = "char" + name;
	               break;
	    case 'D' : name = "double" + name;
	               break;
	    case 'F' : name = "float" + name;
	               break;
	    case 'I' : name = "int" + name;
	               break;
	    case 'J' : name = "long" + name;
	               break;
	    case 'S' : name = "short" + name;
	               break;
	    case 'Z' : name = "boolean" + name;
	               break;
	}
	return name;
    }
}	
