/*
 * Created on Mar 3, 2014
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.Color;

import javax.swing.JMenuItem;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This visual style is used for factor graph.
 * @author gwu
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FactorGraphVisualStyle extends FIVisualStyleImpl {
    
    /**
     * Default constructor.
     */
    public FactorGraphVisualStyle() {
    }

    @Override
    protected VisualStyle createVisualStyle(CyNetworkView view) {
        VisualStyle fiVisualStyle = super.createVisualStyle(view);
        // Add some modification to the FI visual style
        // Use different node types for factor graphs
        ServiceReference referenceD = getVisualMappingFunctionFactorServiceReference("(mapping.type=discrete)");
        if (referenceD == null)
            return fiVisualStyle;
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        VisualMappingFunctionFactory visMapFuncFactoryD = (VisualMappingFunctionFactory) context.getService(referenceD);
        DiscreteMapping nodeTypeShapeFunction = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction("nodeType",
                                                                                                                 String.class,
                                                                                                                 BasicVisualLexicon.NODE_SHAPE);
        nodeTypeShapeFunction.putMapValue("factor",
                                          NodeShapeVisualProperty.RECTANGLE);
        nodeTypeShapeFunction.putMapValue("variable",
                                          NodeShapeVisualProperty.ELLIPSE);
        fiVisualStyle.addVisualMappingFunction(nodeTypeShapeFunction);
        
        DiscreteMapping nodeTypeColorFunction = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction("nodeType",
                                                                                                                 String.class, BasicVisualLexicon.NODE_FILL_COLOR);
        nodeTypeColorFunction.putMapValue("factor",
                                          Color.WHITE);
        nodeTypeColorFunction.putMapValue("variable",
                                          Color.LIGHT_GRAY);
        fiVisualStyle.addVisualMappingFunction(nodeTypeColorFunction);
        
        DiscreteMapping<String, Double> nodeTypeSizeFunction = (DiscreteMapping<String, Double>) visMapFuncFactoryD.createVisualMappingFunction("nodeType",
                                                                                                                                                  String.class,
                                                                                                                                                  BasicVisualLexicon.NODE_SIZE);
        nodeTypeSizeFunction.putMapValue("factor", 25.0d);
        nodeTypeSizeFunction.putMapValue("variable", 50.0d);
        fiVisualStyle.addVisualMappingFunction(nodeTypeSizeFunction);
        
        context.ungetService(referenceD);
        return fiVisualStyle;
    }

    @Override
    protected JMenuItem getLayoutMenu() {
        return getLayoutMenu("Circular");
    }
    
    
}
