package org.reactome.cytoscape.sc;

import javax.swing.JFrame;

import org.gk.util.ProgressPane;
import org.reactome.cytoscape.sc.server.JSONServerCaller;

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
        progPane.setText("Loading the data...");
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
