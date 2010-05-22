/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.inspector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.testmgr.record.GetInvokerRecord;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.ObjectInspectInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

/**
 * 
 * A window that displays the fields in an object or class. This class is
 * subclassed for objects, classes and method results separately
 * (ObjectInspector, ClassInspector, ResultInspector).
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @author Bruce Quig
 */
public abstract class Inspector extends JFrame
    implements ListSelectionListener
{
    // === static variables ===


    protected final static String showClassLabel = Config.getString("debugger.inspector.showClass");
    protected final static String inspectLabel = Config.getString("debugger.inspector.inspect");
    protected final static String getLabel = Config.getString("debugger.inspector.get");
    protected final static String close = Config.getString("close");
 
    // === instance variables ===

    protected FieldList fieldList = null;
    private Color fieldListBackgroundColor;

    protected JButton inspectButton;
    protected JButton getButton;
    protected AssertPanel assertPanel;

    protected DebuggerObject selectedField; // the object currently selected in
    // the list
    protected String selectedFieldName; // the name of the field of the
    // currently selected object
    protected String selectedFieldType;
    protected InvokerRecord selectedInvokerRecord; // an InvokerRecord for the
    // selected
    // object (if possible, else null)

    protected Package pkg;
    protected InspectorManager inspectorManager;
    protected InvokerRecord ir;
    protected Point initialClick;

    //The width of the list of fields
    private static final int MIN_LIST_WIDTH = 150;
    private static final int MAX_LIST_WIDTH = 300;

  
    /**
     * Constructor.
     * 
     * @param pkg
     *            the package this inspector belongs to (or null)
     * @param ir
     *            the InvokerRecord for this inspector (or null)
     */
    protected Inspector(InspectorManager inspectorManager, Package pkg, InvokerRecord ir, Color valueFieldColor)
    {
        super(AWTUtilitiesWrapper.getBestGC());
        
        if(inspectorManager == null) {
            throw new NullPointerException("An inspector must have an InspectorManager.");
        }
        setIconImage(BlueJTheme.getIconImage());

        if (pkg == null && ir != null) {
            // Get button cannot be enabled when pkg==null
            ir = null;
        }
        this.inspectorManager = inspectorManager;
        this.pkg = pkg;
        this.ir = ir;

        // We want to be able to veto a close
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                doClose(true);
            }
        });

        fieldListBackgroundColor = valueFieldColor;
        initFieldList();
    }

    /**
     * Initializes the list of fields. This creates the component that shows the
     * fields.
     * @param valueFieldColor 
     */
    private void initFieldList()
    {
        fieldList = new FieldList(MAX_LIST_WIDTH, fieldListBackgroundColor);
        fieldList.setBackground(this.getBackground());
        fieldList.setOpaque(true);
        fieldList.setSelectionBackground(Config.getSelectionColour());
        fieldList.getSelectionModel().addListSelectionListener(this);
        // add mouse listener to monitor for double clicks to inspect list
        // objects. assumption is made that valueChanged will have selected
        // object on first click
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                // monitor for double clicks
                if (e.getClickCount() == 2) {
                    doInspect();
                }
            }
        };
        fieldList.addMouseListener(mouseListener);
        
        //to make it possible to close dialogs with the keyboard (ENTER or ESCAPE), we
        // grab the key event from the fieldlist. 
        fieldList.addKeyListener(new KeyListener() {            
            public void keyPressed(KeyEvent e)
            {                
            }

            public void keyReleased(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    doClose(true);
                }    
            }

            public void keyTyped(KeyEvent e)
            {
            }
        });        
    }

    protected boolean isGetEnabled()
    {
        return ir != null;
    }

    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        if (visible)
            fieldList.requestFocus(); // doesn't seem to work
        // requestFocus seems to work only of the
        // component is already visible
    }

    /**
     * De-iconify the window (if necessary) and bring it to the front.
     */
    public void bringToFront()
    {
        setState(Frame.NORMAL); // de-iconify
        toFront(); // window to front
    }

    // --- abstract interface to be implemented by subclasses ---

    /**
     * Returns the list of data.
     */
    abstract protected Object[] getListData();

    /**
     * An element in the field list was selected.
     */
    abstract protected void listElementSelected(int slot);

    /**
     * Show the inspector for the class of an object.
     */
    abstract protected void showClass();

    /**
     * Remove this inspector.
     */
    abstract protected void remove();

    /**
     * Return the preferred number of rows that should be shown in the list
     * 
     * @return The number of rows
     */
    abstract protected int getPreferredRows();

    // --- end of abstract methods ---

    /**
     * Requests an update of the field values shown in this viewer to show current object
     * values.
     * 
     */
    public void update()
    {
        final Object[] listData = getListData();
        
        fieldList.setData(listData);
        fieldList.setTableHeader(null);
        
        // Ensures that an element (if any exist) is always selected
        if (fieldList.getSelectedRow() == -1 && listData.length > 0) {
            fieldList.setRowSelectionInterval(0, 0);
        }
                
        // if (assertPanel != null) {
        //    assertPanel.updateWithResultData((String) listData[0]);
        // }
        
        int slot = fieldList.getSelectedRow();
        
        // occurs if valueChanged picked up a clearSelection event from
        // the list
        if (slot != -1) {
            listElementSelected(slot);
        }
        
        repaint();
    }

    /**
     * Call this method when you want the inspector to resize to its preferred
     * size as calculated from the elements in the inspector.
     * 
     */
    public void updateLayout()
    {
        final Object[] listData = getListData();
        double height = fieldList.getPreferredSize().getHeight();
        int rows = listData.length;
        int scrollBarWidth = 0;
        if (rows > getPreferredRows()) {
            height = fieldList.getRowHeight() * getPreferredRows();
            scrollBarWidth = 32; // add some space for a scrollbar
        }
        
        int width = (int) fieldList.getPreferredSize().getWidth();
        if (width < MIN_LIST_WIDTH) {
            width = MIN_LIST_WIDTH;
        }
        if(width > MAX_LIST_WIDTH) {
            width = MAX_LIST_WIDTH;
        }
        
        
        fieldList.setPreferredScrollableViewportSize(new Dimension(width+scrollBarWidth, (int) height));
        pack();
        repaint();
    }

    // ----- ListSelectionListener interface -----

    /**
     * The value of the list selection has changed. Update the selected object.
     * 
     * @param e
     *            The event object describing the event
     */
    public void valueChanged(ListSelectionEvent e)
    {
        // ignore mouse down, dragging, etc.
        if (e.getValueIsAdjusting()) {
            return;
        }

        int slot = fieldList.getSelectedRow();

        // occurs if valueChanged picked up a clearSelection event from
        // the list
        if (slot == -1) {
            return;
        }

        listElementSelected(slot);
    }

    // ----- end of ListSelectionListener interface -----

    /**
     * Store the object currently selected in the list.
     * 
     * @param object
     *            The new CurrentObj value
     * @param name
     *            The name of the selected field
     * @param type
     *            The type of the selected field
     */
    protected void setCurrentObj(DebuggerObject object, String name, String type)
    {
        selectedField = object;
        selectedFieldName = name;
        selectedFieldType = type;
    }

    /**
     * Enable or disable the Inspect and Get buttons.
     * 
     * @param inspect
     *            The new ButtonsEnabled value
     * @param get
     *            The new ButtonsEnabled value
     */
    protected void setButtonsEnabled(boolean inspect, boolean get)
    {
        inspectButton.setEnabled(inspect);
        getButton.setEnabled(get && isGetEnabled());
    }

    /**
     * The "Inspect" button was pressed. Inspect the selected object.
     */
    protected void doInspect()
    {
        if (selectedField != null) {
            boolean isPublic = getButton.isEnabled();
            
            InvokerRecord newIr = new ObjectInspectInvokerRecord(selectedFieldName, ir);
            inspectorManager.getInspectorInstance(selectedField, selectedFieldName, pkg, isPublic ? newIr : null, this);
        }
    }

    /**
     * The "Get" button was pressed. Get the selected object on the object
     * bench.
     */
    protected void doGet()
    {
        if (selectedField != null) {
            GetInvokerRecord getIr = new GetInvokerRecord(selectedFieldType, selectedFieldName, ir);
            PackageEditor pkgEd = pkg.getEditor();
            pkgEd.recordInteraction(getIr);
            pkgEd.raisePutOnBenchEvent(this, selectedField, selectedField.getGenType(), getIr);
        }
    }

    /**
     * Close this inspector. The caller should remove it from the list of open
     * inspectors.
     * 
     * @param handleAssertions   Whether assertions should be attached to the
     *                           invoker record. If true, the user may be prompted
     *                           to fill in assertion data. 
     */
    public void doClose(boolean handleAssertions)
    {
        boolean closeOk = true;

        if (handleAssertions) {
            // handleAssertions may veto the close
            closeOk = handleAssertions();
        }

        if (closeOk) {
            setVisible(false);
            remove();
            dispose();
        }
    }

    protected boolean handleAssertions()
    {
        if (assertPanel != null && assertPanel.isAssertEnabled()) {
            
            if (! assertPanel.isAssertComplete()) {
                int choice = DialogManager.askQuestion(this, "empty-assertion-text");
                
                if (choice == 0) {
                    return false;
                }
            }
            
            ir.addAssertion(assertPanel.getAssertStatement());
        }
        return true;
    }

    protected JButton createCloseButton()
    {
        JButton button = new JButton(close);
        {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    doClose(true);
                }
            });
        }

        return button;

    }

    /**
     * Creates a panel with an inspect button and a get button
     * 
     * @return A panel with two buttons
     */
    protected JPanel createInspectAndGetButtons()
    {
        // Create panel with "inspect" and "get" buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setDoubleBuffered(false);
        buttonPanel.setLayout(new GridLayout(0, 1));
        inspectButton = new JButton(inspectLabel);
        inspectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                doInspect();
            }
        });
        inspectButton.setEnabled(false);
        buttonPanel.add(inspectButton);

        getButton = new JButton(getLabel);
        getButton.setEnabled(false);
        getButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                doGet();
            }
        });
        buttonPanel.add(getButton);

        JPanel buttonFramePanel = new JPanel();
        buttonFramePanel.setOpaque(false);
        buttonFramePanel.setDoubleBuffered(false);
        buttonFramePanel.setLayout(new BorderLayout(0, 0));
        buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
        return buttonFramePanel;
    }

    /**
     * Creates a ScrollPane for the fieldList
     */
    protected JScrollPane createFieldListScrollPane()
    {
        JScrollPane scrollPane = new JScrollPane(fieldList);
        scrollPane.setBorder(BorderFactory.createLineBorder(fieldListBackgroundColor, 10));
        scrollPane.getViewport().setBackground(fieldListBackgroundColor);
        return scrollPane;
    }
    
    // Allow movement of the window by dragging
    // Adapted from: http://www.stupidjavatricks.com/?p=4
    protected void installListenersForMoveDrag()
    {
        addMouseListener( new MouseAdapter()
        {
            public void mousePressed( MouseEvent e )
            {
                initialClick = e.getPoint();
                getComponentAt( initialClick );
            }

            public void mouseReleased(MouseEvent e)
            {
                initialClick = null;
            }
        });
     
        // Move window when mouse is dragged
        addMouseMotionListener( new MouseMotionAdapter()
        {
            public void mouseDragged( MouseEvent e )
            {
                if (initialClick == null) {
                    initialClick = e.getPoint();
                    return;
                }
                
                // get location of Window
                int thisX = getLocation().x;
                int thisY = getLocation().y;
                
                
     
                // Determine how much the mouse moved since the initial click
                int xMoved = ( thisX + e.getX() ) - ( thisX + initialClick.x );
                int yMoved = ( thisY + e.getY() ) - ( thisY + initialClick.y );
     
                // Move window to this position
                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                setLocation( X, Y );
            }
        });
    }
    
    /**
    * Taken from the source code at: http://java.sun.com/developer/technicalArticles/GUI/translucent_shaped_windows/
    *
    * @author Anthony Petrov
    */
   private static class AWTUtilitiesWrapper {

       private static Class<?> awtUtilitiesClass;
       private static Class<?> translucencyClass;
       private static Method mIsTranslucencySupported,  mIsTranslucencyCapable,  mSetWindowShape,  mSetWindowOpacity,  mSetWindowOpaque;
       public static Object PERPIXEL_TRANSPARENT,  TRANSLUCENT,  PERPIXEL_TRANSLUCENT;

       static void init() {
           try {
               awtUtilitiesClass = Class.forName("com.sun.awt.AWTUtilities");
               translucencyClass = Class.forName("com.sun.awt.AWTUtilities$Translucency");
               if (translucencyClass.isEnum()) {
                   Object[] kinds = translucencyClass.getEnumConstants();
                   if (kinds != null) {
                       PERPIXEL_TRANSPARENT = kinds[0];
                       TRANSLUCENT = kinds[1];
                       PERPIXEL_TRANSLUCENT = kinds[2];
                   }
               }
               mIsTranslucencySupported = awtUtilitiesClass.getMethod("isTranslucencySupported", translucencyClass);
               mIsTranslucencyCapable = awtUtilitiesClass.getMethod("isTranslucencyCapable", GraphicsConfiguration.class);
               mSetWindowShape = awtUtilitiesClass.getMethod("setWindowShape", Window.class, Shape.class);
               mSetWindowOpacity = awtUtilitiesClass.getMethod("setWindowOpacity", Window.class, float.class);
               mSetWindowOpaque = awtUtilitiesClass.getMethod("setWindowOpaque", Window.class, boolean.class);
           } catch (Exception ex) {
               Debug.reportError("Couldn't support AWTUtilities", ex);
           }
       }

       static {
           init();
       }
       
       private static boolean isSupported(Method method, Object kind) {
           if (awtUtilitiesClass == null ||
                   method == null)
           {
               return false;
           }
           try {
               Object ret = method.invoke(null, kind);
               if (ret instanceof Boolean) {
                   return ((Boolean)ret).booleanValue();
               }
           } catch (Exception ex) {
               Debug.reportError("Couldn't support AWTUtilities", ex);
           }
           return false;
       }
       
       public static boolean isTranslucencySupported(Object kind) {
           if (translucencyClass == null) {
               return false;
           }
           return isSupported(mIsTranslucencySupported, kind);
       }
       
       public static boolean isTranslucencyCapable(GraphicsConfiguration gc) {
           return isSupported(mIsTranslucencyCapable, gc);
       }
       
       private static void set(Method method, Window window, Object value) {
           if (awtUtilitiesClass == null ||
                   method == null)
           {
               return;
           }
           try {
               method.invoke(null, window, value);
           } catch (Exception ex) {
               Debug.reportError("Couldn't support AWTUtilities: ", ex);
           }
       }
       
       public static void setWindowShape(Window window, Shape shape) {
           set(mSetWindowShape, window, shape);
       }

       public static void setWindowOpacity(Window window, float opacity) {
           set(mSetWindowOpacity, window, Float.valueOf(opacity));
       }
       
       public static void setWindowOpaque(Window window, boolean opaque) {
           set(mSetWindowOpaque, window, Boolean.valueOf(opaque));
       }
       
       public static GraphicsConfiguration getBestGC() {
           GraphicsConfiguration translucencyCapableGC = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
           if (!AWTUtilitiesWrapper.isTranslucencyCapable(translucencyCapableGC)) {
               translucencyCapableGC = null;

               GraphicsEnvironment env =
                       GraphicsEnvironment.getLocalGraphicsEnvironment();
               GraphicsDevice[] devices = env.getScreenDevices();

               for (int i = 0; i < devices.length && translucencyCapableGC == null; i++) {
                   GraphicsConfiguration[] configs = devices[i].getConfigurations();
                   for (int j = 0; j < configs.length && translucencyCapableGC == null; j++) {
                       if (AWTUtilitiesWrapper.isTranslucencyCapable(configs[j])) {
                           translucencyCapableGC = configs[j];
                       }
                   }
               }
           }
           if (translucencyCapableGC == null) {
               Debug.message("No translucency capable GC");
           }
           return translucencyCapableGC;
       }
   }
   
   protected void setWindowOpaque(boolean b)
   {
       AWTUtilitiesWrapper.setWindowOpaque(this, b);
   }
}
