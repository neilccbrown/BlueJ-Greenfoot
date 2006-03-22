package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * Graphics for the end line part of an arrow. With a connector to the right.
 *  | |__
 * 
 * @author Poul Henriksen
 * @version $Id: ArrowConnectEnd.java 3857 2006-03-22 00:08:17Z mik $
 */
public class ArrowConnectEnd extends ArrowElement
{
    public void paintComponent(Graphics g)
    {
        Dimension size = getSize();
        g.drawLine(size.width / 2, 0, size.width / 2, size.height / 2);
        g.drawLine(size.width / 2, size.height / 2, size.width, size.height / 2);
    }
}