package bluej.extensions.event;

/**
 * This interface allows you to listen for application events.
 *
 * @version $Id: ApplicationEventListener.java 1891 2003-04-25 09:32:30Z damiano $
 */
public interface ApplicationEventListener
{
    /**
     * This method will be called when an event occurs.
     * Note that this method is called from a Swing-like dispatcher and therefore you must
     * return as quickly as possible. 
     * If a long operation must be performed you should start a Thread.
     */
    public void blueJReady (ApplicationEvent event);
}
