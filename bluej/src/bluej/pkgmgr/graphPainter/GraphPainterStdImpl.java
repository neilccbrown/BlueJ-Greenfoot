package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Graphics2D;
import java.util.Iterator;

import bluej.Config;
import bluej.graph.*;
import bluej.pkgmgr.dependency.*;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.Package;

/**
 * Paints a Graph using TargetPainters
 * 
 * @author fisker
 * @version $Id: GraphPainterStdImpl.java 2929 2004-08-23 11:51:06Z polle $
 */
public class GraphPainterStdImpl
    implements GraphPainter
{
    public static final Color[] shadowColours = { new Color(242, 242, 242), 
                                                  new Color(211, 211, 211),
                                                  new Color(189, 189, 189),
                                                  new Color(83, 83, 83)
                                                };
    static final int TEXT_HEIGHT = Integer.parseInt(Config.getPropString("bluej.target.fontsize")) + 4;
    static final int TEXT_BORDER = 4;
    static final float alpha = (float) 0.5;
    static AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);

    private final ClassTargetPainter classTargetPainter = new ClassTargetPainter();
    private final ReadmeTargetPainter readmePainter = new ReadmeTargetPainter();
    private final PackageTargetPainter packageTargetPainter = new PackageTargetPainter();
    private final ExtendsDependencyPainter extendsDependencyPainter = new ExtendsDependencyPainter();
    private final ImplementsDependencyPainter implementsDependencyPainter = new ImplementsDependencyPainter();
    private final UsesDependencyPainter usesDependencyPainter = new UsesDependencyPainter();
    private static final GraphPainterStdImpl singleton = new GraphPainterStdImpl();

    private GraphEditor graphEditor;

    private GraphPainterStdImpl()
    {} // prevent instantiation

    /**
     * Paint 'graph' on 'g'
     */
    public void paint(Graphics2D g, GraphEditor graphEditor)
    {
        this.graphEditor = graphEditor;
        Graph graph = graphEditor.getGraph();
        paintEdges(g, graph);
        paintVertices(g, graph);
        paintGhosts(g, graph);
        paintIntermediateDependency(g, graph);
    }

    /**
     * Paint the egdes in 'graph' on 'g'
     * 
     * @param g
     * @param graph
     */
    private void paintEdges(Graphics2D g, Graph graph)
    {
        Edge edge;
        //Paint the edges
        for (Iterator it = graph.getEdges(); it.hasNext();) {
            edge = (Edge) it.next();
            paintEdge(g, edge);
        }
    }

    /**
     * Paint the vertices in 'graph' on 'g'. If one of the targets to be painted
     * is in the process of drawing a dependency to another class, assign that
     * class to 'dependency'
     * 
     * @param g
     * @param graph
     * @param dependentTarget
     * @return the class from which a dependency is being drawn. Null if none.
     */
    private void paintVertices(Graphics2D g, Graph graph)
    {
        for (Iterator it = graph.getVertices(); it.hasNext();) {
            Vertex vertex = (Vertex) it.next();
            paintVertex(g, vertex);
        }
    }

    /**
     * Paint the ghosts (transparent versions) of the vertices in 'graph' that
     * are being dragged in the diagram.
     * 
     * @param g
     * @param graph
     */
    private void paintGhosts(Graphics2D g, Graph graph)
    {
        for (Iterator it = graph.getVertices(); it.hasNext();) {
            Object vertex = it.next();
            if (vertex instanceof Moveable) {
                Moveable moveable = (Moveable) vertex;
                if (moveable.isDragging()) {
                    paintGhostVertex(g, moveable);
                }
            }
        }
    }

    /**
     * Paint 'edge' on 'g'
     */
    private void paintEdge(Graphics2D g, Edge edge)
    {
        if (!(edge instanceof Dependency)) {
            throw new IllegalArgumentException("Not a dependency");
        }
        Dependency dependency = (Dependency) edge;
        getDependencyPainter(dependency).paint(g, dependency, isPermanentFocusOwner());
    }

    /**
     * Return the appropriate painter for a given dependency.
     * 
     * @param edge  The dependency we want to paint
     * @return  A painter that can paint the given dependency.
     */
    public DependencyPainter getDependencyPainter(Edge edge)
    {
        if (edge instanceof ImplementsDependency) {
            return implementsDependencyPainter;
        }
        else if (edge instanceof ExtendsDependency) {
            return extendsDependencyPainter;
        }
        else if (edge instanceof UsesDependency) {
            return usesDependencyPainter;
        }
        else {
            //assert false;
            return null;
        }
    }

    /**
     * Paint 'vertex' on 'g' using the appropiate painter.
     * 
     * @param g
     * @param vertex
     */
    private void paintVertex(Graphics2D g, Vertex vertex)
    {
        if (vertex instanceof ClassTarget) {
            classTargetPainter.paint(g, (ClassTarget) vertex, isPermanentFocusOwner());
        }
        else if (vertex instanceof ReadmeTarget) {
            readmePainter.paint(g, (ReadmeTarget) vertex, isPermanentFocusOwner());
        }
        else if (vertex instanceof PackageTarget) {
            packageTargetPainter.paint(g, (PackageTarget) vertex, isPermanentFocusOwner());
        }
        else {
            //asserts false;
        }
    }

    /**
     * Paint a ghostet (transparent) version of 'vertex' on 'g'
     * 
     * @param g
     * @param vertex
     */
    private void paintGhostVertex(Graphics2D g, Moveable vertex)
    {
        if (vertex instanceof ClassTarget) {
            classTargetPainter.paintGhost(g, (ClassTarget) vertex, isPermanentFocusOwner());
        }
        else if (vertex instanceof PackageTarget) {
            packageTargetPainter.paintGhost(g, (PackageTarget) vertex, isPermanentFocusOwner());
        }
        else {
            //asserts false;
        }
    }

    /**
     * Paint an arrow representing the intermediate dependency 'd', using the
     * appropiate painter, on 'g'
     * 
     * @param g
     * @param d
     */
    private void paintIntermediateDependency(Graphics2D g, Graph graph)
    {
        RubberBand rb = graphEditor.getRubberBand();
        if (rb != null) {
            if (((Package)graph).getState() == Package.S_CHOOSE_EXT_TO) {
                extendsDependencyPainter.paintIntermediateDependency(g, rb);
            }
            else if (((Package)graph).getState() == Package.S_CHOOSE_USES_TO) {
                usesDependencyPainter.paintIntermedateDependency(g, rb);
            }
        }
    }

    /**
     * Tell whether the graph editor has the permanent key focus - 
     * this is NOT the temporary which hasFocus() and isFocusOwner() uses.
     */
    private boolean isPermanentFocusOwner()
    {
        Component permanentFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        return (permanentFocusOwner == graphEditor);
    }

    /**
     * Get reference to the singleton GraphPainterStdImpl
     * 
     * @return GraphPainterStdImpl
     */
    public static GraphPainter getInstance()
    {
        return singleton;
    }

}