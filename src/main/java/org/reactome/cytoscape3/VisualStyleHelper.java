package org.reactome.cytoscape3;

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
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.TaskManager;

/**
 * This class provides layout/VisMap setup for a given network view. Without it,
 * all a user sees is what looks to be a single node (this is a common quirk in
 * older Cytoscape plugin (e.g. 2.7) releases.
 * 
 * @author Eric T. Dawson
 * @date July 2013
 */
public class VisualStyleHelper
{
    private final String FI_VISUAL_STYLE = "FI Network";
    private VisualMappingManager visMapManager;
    private VisualStyleFactory visStyleFactory;
    private VisualMappingFunctionFactory visMapFuncFactoryP;
    private VisualMappingFunctionFactory visMapFuncFactoryD;
    private CyLayoutAlgorithmManager layoutAlgManager;
    private TaskManager taskManager;
    private CySwingApplication desktopApp;
    private VisualMappingFunctionFactory visMapFuncFactoryC;

    public VisualStyleHelper(VisualMappingManager visMapManager,
            VisualStyleFactory visStyleFactory,
            VisualMappingFunctionFactory visMapFuncFactoryC,
            VisualMappingFunctionFactory visMapFuncFactoryD,
            VisualMappingFunctionFactory visMapFuncFactoryP,
            CyLayoutAlgorithmManager layoutAlgManager, TaskManager taskManager,
            CySwingApplication desktopApp)
    {
        this.visMapManager = visMapManager;
        this.visStyleFactory = visStyleFactory;
        this.visMapFuncFactoryC = visMapFuncFactoryC;
        this.visMapFuncFactoryD = visMapFuncFactoryD;
        this.visMapFuncFactoryP = visMapFuncFactoryP;
        this.layoutAlgManager = layoutAlgManager;
        this.taskManager = taskManager;
        this.desktopApp = desktopApp;
    }

    public void setVisualStyle(CyNetworkView view)
    {
        //VisualStyle style = visMapManager.getVisualStyle(view);
        Iterator it = visMapManager.getAllVisualStyles().iterator();
        while(it.hasNext()){
            VisualStyle current = (VisualStyle) it.next();
            if(current.getTitle().equalsIgnoreCase(FI_VISUAL_STYLE))
            {
                visMapManager.removeVisualStyle(current);
                break;
            }
        }
        createVisualStyle(view);
    }

    private void createVisualStyle(CyNetworkView view)
    {

        // Create a fresh visual style
        VisualStyle fiVisualStyle = visStyleFactory
                .createVisualStyle(FI_VISUAL_STYLE);
        CyTable tableForStyling = view.getModel().getDefaultNodeTable();

        // Set the default node shape and color
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE,
                NodeShapeVisualProperty.ELLIPSE);
        Color color = new Color(0, 204, 0);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT,
                color);
        fiVisualStyle
                .setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, color);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_TRANSPARENCY, 100);
        // Give the nodes a label based on their name
        String nodeLabelAttrName = "nodeLabel";
        PassthroughMapping labelFunction = (PassthroughMapping) this.visMapFuncFactoryP
                .createVisualMappingFunction(nodeLabelAttrName, String.class,
                        BasicVisualLexicon.NODE_LABEL);
        String toolTipAttrName = "nodeToolTip";
        PassthroughMapping toolTipFunction = (PassthroughMapping) this.visMapFuncFactoryP
                .createVisualMappingFunction(toolTipAttrName, String.class, BasicVisualLexicon.NODE_TOOLTIP);
        fiVisualStyle.addVisualMappingFunction(labelFunction);
        fiVisualStyle.addVisualMappingFunction(toolTipFunction);

        // Set the node color based on module
        DiscreteMapping colorToModuleFunction =
                            (DiscreteMapping) this.visMapFuncFactoryD.createVisualMappingFunction("module",
                                                            String.class, BasicVisualLexicon.NODE_FILL_COLOR);
        String moduleColors = PlugInScopeObjectManager.getManager().getProperties().getProperty("moduleColors");
        String [] tokens = moduleColors.split(";");
        for (int i = 0; i < tokens.length; i++)
        {
            String [] text = tokens[i].split(",");
            Color moduleColor = new Color(Integer.parseInt(text[0]),
                                          Integer.parseInt(text[1]),
                                          Integer.parseInt(text[2]));
            colorToModuleFunction.putMapValue(i, moduleColor);
        }
        fiVisualStyle.addVisualMappingFunction(colorToModuleFunction);
        
        // Change the node shape from the default (ellipse)
        // to diamond if the gene is a linker.
        DiscreteMapping linkerGeneShapeFunction = (DiscreteMapping) this.visMapFuncFactoryD.createVisualMappingFunction("isLinker", Boolean.class,
                        BasicVisualLexicon.NODE_SHAPE);
        boolean key = true;
        linkerGeneShapeFunction.putMapValue(key,
                NodeShapeVisualProperty.DIAMOND);
        fiVisualStyle.addVisualMappingFunction(linkerGeneShapeFunction);
        // Set default edge color and width
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, Color.BLUE);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 1.5d);
        
        //Set the edge target/source shape based
        //on the FI direction.
        
        //Use dashed lines for predicted interactions.
        
        //Set the node size based on sample number
        int [] sampleNumberRange = getSampleNumberRange(view);
        if (sampleNumberRange != null)
        {
            ContinuousMapping sampleNumberToSizeFunction = (ContinuousMapping) this.visMapFuncFactoryC.createVisualMappingFunction("sampleNumber", Integer.class, BasicVisualLexicon.NODE_SIZE);
            Integer lowerSampleNumberBound = sampleNumberRange[0];
            BoundaryRangeValues<Integer> lowerBoundary = new BoundaryRangeValues<Integer>(30,30,30);
            Integer upperSampleNumberBound = sampleNumberRange[1];
            BoundaryRangeValues<Integer> upperBoundary = new BoundaryRangeValues<Integer>(100,100,100);
            sampleNumberToSizeFunction.addPoint(lowerSampleNumberBound, lowerBoundary);
            sampleNumberToSizeFunction.addPoint(upperSampleNumberBound, upperBoundary);
            fiVisualStyle.addVisualMappingFunction(sampleNumberToSizeFunction);
        }
        // Apply the visual style and update the network view.
        visMapManager.setVisualStyle(fiVisualStyle, view);
        view.updateView();

        // Set the desired layout (yFiles Organic)
        // This method manually clicks the menu item to trigger
        // the new layout, as yFiles layouts are not available
        // for programmatic use.
        setLayout();
    }

    private int[] getSampleNumberRange(CyNetworkView view)
    {
        Map<Long, Object> idToSampleNumber = new CyTableManager().getNodeTableValues(view.getModel(), "sampleNumber", Integer.class);
        if (idToSampleNumber == null || idToSampleNumber.isEmpty())
            return null;
        Set<Object> set = new HashSet<Object>(idToSampleNumber.values());
        List<Integer> list = new ArrayList<Integer>();
        for (Object obj : set)
            list.add((Integer) obj);
        Collections.sort(list);
        Integer min = (Integer) list.get(0);
        Integer max = (Integer) list.get(list.size() - 1);
        return new int []{min, max};
    }

    private JMenuItem getyFilesOrganic()
    {
        JMenu yFilesMenu = null;
        for (Component item : desktopApp.getJMenu("Layout").getMenuComponents())
        {
            if (item instanceof JMenu
                    && ((JMenu) item).getText().equals("yFiles Layouts"))
            {
                yFilesMenu = (JMenu) item;
                break;
            }
        }
        JMenuItem organicMenuItem = null;
        for (Component item : yFilesMenu.getMenuComponents())
        {
            if (item instanceof JMenuItem
                    && ((JMenuItem) item).getText().equals("Organic"))
            {
                organicMenuItem = (JMenuItem) item;
                break;
            }
        }
        return organicMenuItem;
    }

    private void setLayout()
    {
        JMenuItem yFilesOrganicMenuItem = getyFilesOrganic();
        yFilesOrganicMenuItem.doClick();
    }
}
