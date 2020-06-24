package org.reactome.cytoscape.service;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This class provides layout/VisMap setup for a given network view. Without it,
 * all a user sees is what looks to be a single node (this is a common quirk in
 * older Cytoscape plugin (e.g. 2.7) releases.
 * 
 * @author Eric T. Dawson
 * @date July 2013
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FIVisualStyleImpl implements FIVisualStyle {
    protected String styleName = "FI Network";

    public FIVisualStyleImpl() {
    }

    @Override
    public void setVisualStyle(CyNetworkView view) {
        setVisualStyle(view, true);
    }
    
    /**
     * 
     * @param view
     * @param createStyle true to create a new VisualStyle from scratch. Otherwise, use an existing one.
     */
    @Override
    public void setVisualStyle(CyNetworkView view, boolean createStyle) {
        // Find an existing one
        VisualStyle style = null;
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(VisualMappingManager.class.getName());
        VisualMappingManager visMapManager = (VisualMappingManager) context.getService(reference);
        Iterator it = visMapManager.getAllVisualStyles().iterator();
        while (it.hasNext()) {
            VisualStyle current = (VisualStyle) it.next();
            if (current.getTitle().equalsIgnoreCase(styleName)) {
                if (createStyle)
                    visMapManager.removeVisualStyle(current);
                else
                    style = current;
                break;
            }
        }
        if (style == null) {
            style = createVisualStyle(view);
            if (style != null)
                visMapManager.addVisualStyle(style);
        }
        if (style == null)
            return; // Cannot do anything
        // Apply the visual style and update the network view.
        visMapManager.setVisualStyle(style, view);
        view.updateView();
        visMapManager = null;
        context.ungetService(reference);
    }
    
    protected ServiceReference getVisualMappingFunctionFactorServiceReference(String filter) {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        try {
            ServiceReference[] references = context.getServiceReferences(VisualMappingFunctionFactory.class.getName(),
                                                                         filter);
            if (references == null)
                return null;
            return references[0];
        }
        catch(InvalidSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected VisualStyle createVisualStyle(CyNetworkView view) {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(VisualStyleFactory.class.getName());
        VisualStyleFactory visStyleFactory = (VisualStyleFactory) context.getService(reference);
        // Create a fresh visual style
        VisualStyle fiVisualStyle = visStyleFactory.createVisualStyle(styleName);
        visStyleFactory = null;
        context.ungetService(reference);
        
        // We want to use all three types of visual mapping
        ServiceReference referenceC = getVisualMappingFunctionFactorServiceReference("(mapping.type=continuous)");
        ServiceReference referenceD = getVisualMappingFunctionFactorServiceReference("(mapping.type=discrete)");
        ServiceReference referenceP = getVisualMappingFunctionFactorServiceReference("(mapping.type=passthrough)");
        if (referenceC == null || referenceD == null || referenceP == null) {
            return null;
        }
        VisualMappingFunctionFactory visMapFuncFactoryC = (VisualMappingFunctionFactory) context.getService(referenceC);
        VisualMappingFunctionFactory visMapFuncFactoryD = (VisualMappingFunctionFactory) context.getService(referenceD);
        VisualMappingFunctionFactory visMapFuncFactoryP = (VisualMappingFunctionFactory) context.getService(referenceP);
        // Handle node styles
        setDefaultNodeStyle(fiVisualStyle,
                            visMapFuncFactoryP);
        setNodeColors(fiVisualStyle, visMapFuncFactoryD);
        setNodeSizes(view,
                     fiVisualStyle, 
                     visMapFuncFactoryC);
        setLinkerNodeStyle(fiVisualStyle, visMapFuncFactoryD);
        handleNodeHighlight(fiVisualStyle, 
                         visMapFuncFactoryD,
                         visMapFuncFactoryC);
        displayNodeType(fiVisualStyle,
                        visMapFuncFactoryD);
        // Handle edge styles
        setDefaultEdgeStyle(fiVisualStyle);
        setEdgeStyleOnAnnotations(fiVisualStyle, 
                                  visMapFuncFactoryD,
                                  visMapFuncFactoryC);
        setEdgeStyleOnEdgeType(fiVisualStyle, 
                               visMapFuncFactoryD);

        context.ungetService(referenceC);
        context.ungetService(referenceD);
        context.ungetService(referenceP);
        return fiVisualStyle;
    }
    
    protected void displayNodeType(VisualStyle style,
                                   VisualMappingFunctionFactory visMapFuncFactoryD) {
        // Set the node color based on module
        DiscreteMapping colorToModuleFunction = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction(
                "module", Integer.class, BasicVisualLexicon.NODE_FILL_COLOR);
        String moduleColors = PlugInObjectManager.getManager().getProperties().getProperty(
                "moduleColors");
        String[] tokens = moduleColors.split(";");
        for (int i = 0; i < tokens.length; i++)
        {
            String[] text = tokens[i].split(",");
            Color moduleColor = new Color(Integer.parseInt(text[0]),
                    Integer.parseInt(text[1]), Integer.parseInt(text[2]));
            colorToModuleFunction.putMapValue(i, moduleColor);
        }
        style.addVisualMappingFunction(colorToModuleFunction);
        
        // Set the node shape based on type
        // Currently two types are supported: Gene and Drug
        DiscreteMapping nodeTypeShape = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction("nodeType", 
                                                                                                         String.class,
                                                                                                         BasicVisualLexicon.NODE_SHAPE);
        nodeTypeShape.putMapValue("Gene", 
                                  NodeShapeVisualProperty.ELLIPSE);
        nodeTypeShape.putMapValue("Drug",
                                  NodeShapeVisualProperty.DIAMOND);
        style.addVisualMappingFunction(nodeTypeShape);
    }
                                 

    protected void setNodeSizes(CyNetworkView view, 
                                VisualStyle fiVisualStyle,
                                VisualMappingFunctionFactory visMapFuncFactoryC) {
        // Set the node size based on sample number
        int[] sampleNumberRange = getSampleNumberRange(view);
        if (sampleNumberRange != null)
        {
            ContinuousMapping sampleNumberToSizeFunction = (ContinuousMapping) visMapFuncFactoryC.createVisualMappingFunction(
                    "sampleNumber", Integer.class, BasicVisualLexicon.NODE_SIZE);
            Integer lowerSampleNumberBound = sampleNumberRange[0];
            BoundaryRangeValues<Double> lowerBoundary = new BoundaryRangeValues<Double>(
                    30.0, 30.0, 30.0);
            Integer upperSampleNumberBound = sampleNumberRange[1];
            BoundaryRangeValues<Double> upperBoundary = new BoundaryRangeValues<Double>(
                    100.0, 100.0, 100.0);
            sampleNumberToSizeFunction.addPoint(lowerSampleNumberBound,
                    lowerBoundary);
            sampleNumberToSizeFunction.addPoint(upperSampleNumberBound,
                    upperBoundary);
            fiVisualStyle.addVisualMappingFunction(sampleNumberToSizeFunction);
        }
    }
    
    protected void setEdgeStyleOnAnnotations(VisualStyle fiVisualStyle, 
                                             VisualMappingFunctionFactory visMapFuncFactoryD,
                                             VisualMappingFunctionFactory visMapFuncFactoryC) {
        // Set the edge target arrow shape based on FI Direction
        DiscreteMapping arrowMapping = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction(
                "FI Direction", String.class,
                BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);
        arrowMapping.putMapValue("->", ArrowShapeVisualProperty.ARROW);
        arrowMapping.putMapValue("|->", ArrowShapeVisualProperty.ARROW);
        arrowMapping.putMapValue("<->", ArrowShapeVisualProperty.ARROW);

        arrowMapping.putMapValue("-|", ArrowShapeVisualProperty.T);
        arrowMapping.putMapValue("|-|", ArrowShapeVisualProperty.T);
        arrowMapping.putMapValue("<-|", ArrowShapeVisualProperty.T);

        fiVisualStyle.addVisualMappingFunction(arrowMapping);

        // Set the edge source arrow shape based on FI Direction
        arrowMapping = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction(
                "FI Direction", String.class,
                BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE);
        arrowMapping.putMapValue("<-", ArrowShapeVisualProperty.ARROW);
        arrowMapping.putMapValue("<-|", ArrowShapeVisualProperty.ARROW);
        arrowMapping.putMapValue("<->", ArrowShapeVisualProperty.ARROW);

        arrowMapping.putMapValue("|-", ArrowShapeVisualProperty.T);
        arrowMapping.putMapValue("|-|", ArrowShapeVisualProperty.T);
        arrowMapping.putMapValue("|->", ArrowShapeVisualProperty.T);

        fiVisualStyle.addVisualMappingFunction(arrowMapping);
        // Use dashed lines for predicted interactions.
        DiscreteMapping edgeLineMapping = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction(
                "FI Annotation", String.class,
                BasicVisualLexicon.EDGE_LINE_TYPE);
        edgeLineMapping.putMapValue("predicted",
                LineTypeVisualProperty.LONG_DASH);

        fiVisualStyle.addVisualMappingFunction(edgeLineMapping);
    }

    private void setEdgeStyleOnEdgeType(VisualStyle fiVisualStyle, VisualMappingFunctionFactory visMapFuncFactoryD) {
        // Use dashed lines for predicted interactions.
        DiscreteMapping edgeStrokeColor = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction(
                "EDGE_TYPE", String.class,
                BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
        edgeStrokeColor.putMapValue("FI",
                Color.BLACK);
        edgeStrokeColor.putMapValue("Drug/Target",
                                    Color.BLUE);

        fiVisualStyle.addVisualMappingFunction(edgeStrokeColor);
    }

    protected void setDefaultEdgeStyle(VisualStyle fiVisualStyle) {
        // Set default edge color and width
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_UNSELECTED_PAINT,
                                      Color.BLUE);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 1.5d);
    }

    private void setLinkerNodeStyle(VisualStyle fiVisualStyle, VisualMappingFunctionFactory visMapFuncFactoryD) {
        // Change the node shape from the default (ellipse)
        // to diamond if the gene is a linker.
        // As of January 14, 2017, gene linkers are displayed with 50% transaprancey and RED font to use shapes for node types.
        DiscreteMapping linkerGeneTransparency = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction("isLinker", 
                                                                                                                   Boolean.class,
                                                                                                                   BasicVisualLexicon.NODE_TRANSPARENCY);
        linkerGeneTransparency.putMapValue(true, 50.0f);
        fiVisualStyle.addVisualMappingFunction(linkerGeneTransparency);
        
        DiscreteMapping linkerGeneFontColor = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction("isLinker", 
                                                                                                               Boolean.class,
                                                                                                               BasicVisualLexicon.NODE_LABEL_COLOR);
        linkerGeneFontColor.putMapValue(true, Color.RED);
        fiVisualStyle.addVisualMappingFunction(linkerGeneFontColor);
    }

    private void setNodeColors(VisualStyle fiVisualStyle, VisualMappingFunctionFactory visMapFuncFactoryD) {
        // Set the node color based on module
        DiscreteMapping colorToModuleFunction = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction(
                "module", Integer.class, BasicVisualLexicon.NODE_FILL_COLOR);
        String moduleColors = PlugInObjectManager.getManager().getProperties().getProperty(
                "moduleColors");
        String[] tokens = moduleColors.split(";");
        for (int i = 0; i < tokens.length; i++)
        {
            String[] text = tokens[i].split(",");
            Color moduleColor = new Color(Integer.parseInt(text[0]),
                    Integer.parseInt(text[1]), Integer.parseInt(text[2]));
            colorToModuleFunction.putMapValue(i, moduleColor);
        }
        fiVisualStyle.addVisualMappingFunction(colorToModuleFunction);
    }

    protected void setDefaultNodeStyle(VisualStyle fiVisualStyle, VisualMappingFunctionFactory visMapFuncFactoryP) {
        // Set the default node shape and color
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE,
                NodeShapeVisualProperty.ELLIPSE);
        Color color = new Color(0, 204, 0);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT,
                color);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, color);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_TRANSPARENCY, 100);
        // Give the nodes a label based on their name
        String nodeLabelAttrName = "nodeLabel";
        PassthroughMapping labelFunction = (PassthroughMapping) visMapFuncFactoryP.createVisualMappingFunction(
                nodeLabelAttrName, String.class, BasicVisualLexicon.NODE_LABEL);
        String toolTipAttrName = "nodeToolTip";
        PassthroughMapping toolTipFunction = (PassthroughMapping) visMapFuncFactoryP.createVisualMappingFunction(
                toolTipAttrName, String.class, BasicVisualLexicon.NODE_TOOLTIP);
        fiVisualStyle.addVisualMappingFunction(labelFunction);
        fiVisualStyle.addVisualMappingFunction(toolTipFunction);
    }

    protected void handleNodeHighlight(VisualStyle fiVisualStyle,
                                       VisualMappingFunctionFactory visMapFuncFactoryD,
                                       VisualMappingFunctionFactory visMapFuncFactoryC) {
        // Also a color for hit genes
        DiscreteMapping hitGeneColorFunction = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction("isHitGene",
                                                                                                                 Boolean.class,
                                                                                                                 BasicVisualLexicon.NODE_BORDER_PAINT);
        hitGeneColorFunction.putMapValue(true,
                                         NODE_HIGHLIGHT_COLOR);
        fiVisualStyle.addVisualMappingFunction(hitGeneColorFunction);
        // Make it more obvious since we cannot use a filling color, which has been used by clusters
        DiscreteMapping hitGeneBorderFunction = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction("isHitGene",
                                                                                                                      Boolean.class,
                                                                                                                      BasicVisualLexicon.NODE_BORDER_WIDTH);
        hitGeneBorderFunction.putMapValue(true, 5);
        fiVisualStyle.addVisualMappingFunction(hitGeneBorderFunction);
    }

    private int[] getSampleNumberRange(CyNetworkView view) {
        Map<Long, Object> idToSampleNumber = new TableHelper().getNodeTableValuesBySUID(view.getModel(), 
                                                                                        "sampleNumber", 
                                                                                        Integer.class);
        if (idToSampleNumber == null || idToSampleNumber.isEmpty())
            return null;
        Set<Object> set = new HashSet<Object>(idToSampleNumber.values());
        List<Integer> list = new ArrayList<Integer>();
        for (Object obj : set) {
            list.add((Integer) obj);
        }
        Collections.sort(list);
        Integer min = list.get(0);
        Integer max = list.get(list.size() - 1);
        return new int[]{min, max};
    }

    protected JMenuItem getLayoutMenu(String layout) {
        JMenu yFilesMenu = null;
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        for (Component item : desktopApp.getJMenu("Layout").getMenuComponents()) {
            if (item instanceof JMenu && ((JMenu) item).getText().equals("yFiles Layouts")) {
                yFilesMenu = (JMenu) item;
                break;
            }
        }
        if (yFilesMenu != null) {
            JMenuItem organicMenuItem = null;
            for (Component item : yFilesMenu.getMenuComponents()) {
                if (item instanceof JMenuItem && ((JMenuItem) item).getText().equals(layout)) {
                    organicMenuItem = (JMenuItem) item;
                    break;
                }
            }
            return organicMenuItem;
        }
        return null;
    }
    
    protected JMenuItem getLayoutMenu() {
        return getLayoutMenu("Organic");
    }
    
    /**
     * This new implementation is for Cytoscape 3.6.0.
     * @return
     */
    private JMenuItem getPreferredLayout() {
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        for (Component item : desktopApp.getJMenu("Layout").getMenuComponents()) {
            if (item instanceof JMenuItem && ((JMenuItem) item).getText().equals("Apply Preferred Layout")) {
                return (JMenuItem) item;
            }
        }
        return null;
    }

    @Override
    public void doLayout() {
        // Set the desired layout (yFiles Organic)
        // This method manually clicks the menu item to trigger
        // the new layout, as yFiles layouts are not available
        // for programmatic use.
//        JMenuItem yFilesOrganicMenuItem = getLayoutMenu();
//        if (yFilesOrganicMenuItem != null) {
//            yFilesOrganicMenuItem.doClick();
//        }
        JMenuItem layout = getPreferredLayout();
        if (layout != null)
            layout.doClick();
    }

}
