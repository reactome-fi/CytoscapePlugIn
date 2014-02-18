/*
 * Created on Feb 17, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jiggle.*;

/**
 * A simple layout by fixing the center of a network.
 * @author gwu
 *
 */
public class JiggleLayout {
    
    /**
     * Default constructor.
     */
    public JiggleLayout() {
    }
    
    /**
     * The actual method that does jiggle layout.
     * @param center
     * @param partners
     * @param the coordinates for center and partners keyed by their names.
     */
    public Map<String, double[]> jiggleLayout(String center,
                                              List<String> partners) {
        Graph g = new Graph();
        //Initialize the center of the graph.
        Vertex centerVertex = g.insertVertex();
        initializeJiggleVertex(centerVertex);
        List<Vertex> vertices = new ArrayList<Vertex>();
        for (int i1 = 0; i1 < partners.size(); i1++) {
            Vertex v = g.insertVertex();
            initializeJiggleVertex(v);
            g.insertEdge(v, centerVertex);
            vertices.add(v);
        }
        int d = g.getDimensions();
        double k = 25;
        SpringLaw springLaw = new QuadraticSpringLaw(g, k);
        // Use strong repulsion
        VertexVertexRepulsionLaw vvRepulsionLaw = new HybridVertexVertexRepulsionLaw(g, 3 * k);
        //vvRepulsionLaw.setBarnesHutTheta(0.9d);
        VertexEdgeRepulsionLaw veRepulsionLaw = new InverseSquareVertexEdgeRepulsionLaw(g, k);
        ForceModel fm = new ForceModel(g);
        fm.addForceLaw (springLaw);
        fm.addForceLaw (vvRepulsionLaw);
        // Using a force repulsion law hurts performance unfortunately.
        fm.addForceLaw(veRepulsionLaw);
        double acc = 0.5d;
        double rt = 0.2d;
        FirstOrderOptimizationProcedure opt = new ConjugateGradients(g, fm, acc, rt);
        opt.setConstrained(true);
        // Do a layout for 100 iterations
        for (int i = 0; i < 40; i++)
            opt.improveGraph();
        Map<String, double[]> nodeToCoords = new HashMap<String, double[]>();
        for (int i = 0; i < partners.size(); i++) {
            String partner = partners.get(i);
            Vertex v = vertices.get(i);
            nodeToCoords.put(partner,
                                v.getCoords());
        }
        // Want to add the center coordinates too as a reference
        nodeToCoords.put(center, centerVertex.getCoords());
        return nodeToCoords;
    }
    
    private void initializeJiggleVertex(Vertex v) {
        // Assign a random position
        double[] pos = v.getCoords();
        pos[0] = 500 * Math.random();
        pos[1] = 500 * Math.random();
        // Assign a fixed size
        double[] size = v.getSize();
        size[0] = (int) 50;
        size[1] = (int) 50;
    }
    
}
