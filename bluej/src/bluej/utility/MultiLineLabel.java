package bluej.utility;

import java.awt.*;
import javax.swing.*;

/**
 ** @version $Id: MultiLineLabel.java 2724 2004-07-02 18:51:36Z mik $
 ** @author Justin Tan
 ** A multi-line Label-like AWT component.
 **/
public class MultiLineLabel extends JPanel
{
    protected int fontAttributes = Font.PLAIN;
    protected float alignment;
    protected Color col = null;
	protected int spacing = 0;
    
    /**
     ** Constructor - make a multiline label
     **/
    public MultiLineLabel(String text, float alignment)
    {
        this.alignment = alignment;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if(text != null)
            setText(text);
    }

    /**
     ** Constructor, defaults to centered text
     **/
    public MultiLineLabel(String text)
    {
        this(text, LEFT_ALIGNMENT);
    }

    /**
     * Constructor, empty with the given alignment
     */
    public MultiLineLabel(float alignment)
    {
        this(null, alignment);
    }

    /**
     * Constructor, empty with the given alignment and line spacing
     */
    public MultiLineLabel(float alignment, int spacing)
    {
        this(null, alignment);
        this.spacing = spacing;
    }

    /**
     ** Constructor - make an empty multiline label
     **/
    public MultiLineLabel()
    {
        this(null, LEFT_ALIGNMENT);
    }
	
    public void setText(String text)
    {
        // clear the existing lines from the panel
        removeAll();
        addText(text);
    }
	
    public void addText(String text)
    {
        addText(text, 12);
    }
    
    public void addText(String text, int size)
    {
        if(spacing > 0)
            add(Box.createVerticalStrut(spacing));

        String strs[] = Utility.splitLines(text);
        JLabel l;
        Font font = new Font("SansSerif", fontAttributes, size);

        for (int i = 0; strs != null && i < strs.length; i++) {
            l = new JLabel(strs[i]);
            l.setFont(font);
            l.setAlignmentX(alignment);

            if (col != null)
                l.setForeground(col);

            add(l);
        }   
    }
    
    public void addText(String text, boolean bold, boolean italic)
    {
        int oldAttributes = fontAttributes;
        setBold(bold);
        setItalic(italic);
        addText(text);
        fontAttributes = oldAttributes;
    }

    public void setForeground(Color col)
    {
        this.col = col;    
        Component[] components = this.getComponents();
        for (int i = 0; i < components.length; i++) {
			Component component = components[i];
			component.setForeground(col);
		}
    }
    	
    public void setItalic(boolean italic)
    {
        if(italic)
           fontAttributes |= Font.ITALIC;
        else
            fontAttributes &= ~Font.ITALIC;
    }
	
    public void setBold(boolean bold)
    {
        if(bold)
            fontAttributes |= Font.BOLD;
        else
            fontAttributes &= ~Font.BOLD;
    }
}
