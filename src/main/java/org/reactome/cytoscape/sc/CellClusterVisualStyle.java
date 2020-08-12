package org.reactome.cytoscape.sc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.reactome.cytoscape.service.TableHelper;

/**
 * This style is used for showing the paga cell cluster network.
 * @author wug
 *
 */
public class CellClusterVisualStyle extends SCNetworkVisualStyle {
    public static final double MIN_CLUSTER_DIST = 100.0d; // Two maximum node sizes
    private final double MIN_NODE_SIZE = 10.0d;
    private final double MAX_NODE_SIZE = 100.0d;
    private final double MIN_EDGE_WIDTH = MIN_NODE_SIZE / 10.0d;
    private final double MAX_EDGE_WIDTH = MAX_NODE_SIZE / 10.0d;
    
    public CellClusterVisualStyle() {
        styleName = "Cell Cluster Style";
        needNodeLabel = true;
    }
    
    @Override
    protected void setEdgeStyleOnAnnotations(CyNetworkView view,
                                             VisualStyle fiVisualStyle,
                                             VisualMappingFunctionFactory visMapFuncFactoryD,
                                             VisualMappingFunctionFactory visMapFuncFactoryC) {
        setEdgeWeights(view, fiVisualStyle, visMapFuncFactoryC, MIN_EDGE_WIDTH, MAX_EDGE_WIDTH);
        // Check if we need to use direction
        // Set the edge target arrow shape based on FI Direction
        DiscreteMapping<Boolean, ArrowShape> arrowMapping = (DiscreteMapping<Boolean, ArrowShape>) visMapFuncFactoryD.createVisualMappingFunction(SCNetworkVisualStyle.EDGE_IS_DIRECTED, 
                                                                                                        Boolean.class,
                                                                                                        BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);
        arrowMapping.putMapValue(Boolean.TRUE, ArrowShapeVisualProperty.ARROW);
        arrowMapping.putMapValue(Boolean.FALSE, ArrowShapeVisualProperty.NONE);
        fiVisualStyle.addVisualMappingFunction(arrowMapping);
    }
    
    @Override
    protected void setNodeSizes(CyNetworkView view, 
                                VisualStyle fiVisualStyle,
                                VisualMappingFunctionFactory visMapFuncFactoryC) {
        int[] cellsRange = getCellsRange(view);
        if (cellsRange == null)
            return;
        // Set the node size based on sample number
        ContinuousMapping<Integer, Double> nodeSizeFunc = (ContinuousMapping<Integer, Double>) visMapFuncFactoryC.createVisualMappingFunction("cells", 
                                                                                                                                              Integer.class, 
                                                                                                                                            BasicVisualLexicon.NODE_SIZE);
        BoundaryRangeValues<Double> lowerBoundary = new BoundaryRangeValues<Double>(MIN_NODE_SIZE, MIN_NODE_SIZE, MIN_NODE_SIZE);
        BoundaryRangeValues<Double> upperBoundary = new BoundaryRangeValues<Double>(MAX_NODE_SIZE, MAX_NODE_SIZE, MAX_NODE_SIZE);
        nodeSizeFunc.addPoint(cellsRange[0],
                              lowerBoundary);
        nodeSizeFunc.addPoint(cellsRange[1],
                              upperBoundary);
        fiVisualStyle.addVisualMappingFunction(nodeSizeFunc);
    }
    
    private int[] getCellsRange(CyNetworkView view) {
        Map<Long, Object> idToCells = new TableHelper().getNodeTableValuesBySUID(view.getModel(), 
                                                                                        CELL_NUMBER_NAME, 
                                                                                        Integer.class);
        if (idToCells == null || idToCells.isEmpty())
            return null;
        Set<Object> set = new HashSet<Object>(idToCells.values());
        List<Integer> list = new ArrayList<Integer>();
        for (Object obj : set) {
            list.add((Integer) obj);
        }
        Collections.sort(list);
        Integer min = list.get(0);
        Integer max = list.get(list.size() - 1);
        return new int[]{min, max};
    }

}
