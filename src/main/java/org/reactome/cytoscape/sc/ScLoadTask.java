package org.reactome.cytoscape.sc;

import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CELL_NUMBER_NAME;
import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CLUSTER_NAME;
import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.CONNECTIVITY_NAME;
import static org.reactome.cytoscape.sc.SCNetworkVisualStyle.EDGE_IS_DIRECTED;
import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellClusterNetwork;
import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellNetwork;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.gk.util.ProgressPane;
import org.jmol.script.ScriptManager;
import org.reactome.cytoscape.service.FIAnalysisTask;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.r3.util.InteractionUtilities;

import com.fasterxml.jackson.core.io.JsonEOFException;

public class ScLoadTask extends ScAnalysisTask {
    private String file;

    public ScLoadTask() {
    }
    
    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    protected void _doAnalysis(JSONServerCaller serverCaller, 
                               ProgressPane progPane, 
                               JFrame parentFrame) throws Exception {
        if (file == null)
            throw new IllegalStateException("The analyzed data file is not specified.");
        String message = serverCaller.openAnalyzedData(file);
        if (!checkMessage(parentFrame, message))
            return;
        parseIsForVelocity(message);
        // Looks everything is fine. Let's build the network
        progPane.setText("Building cluster network...");
        buildClusterNetwork(serverCaller);
        progPane.setText("Building cell network...");
        buildCellNetwork(serverCaller);
    }
    
    private void parseIsForVelocity(String message) {
        // Check if the returned text has velocity related information
        this.isForRNAVelocity = message.contains("velocity_graph"); // This may not be reliable
        ScNetworkManager.getManager().setForRNAVelocity(this.isForRNAVelocity);
    }
}
