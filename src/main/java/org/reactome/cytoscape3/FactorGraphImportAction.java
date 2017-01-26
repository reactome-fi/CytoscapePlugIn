/*
 * Created on Jan 25, 2017
 *
 */
package org.reactome.cytoscape3;

import java.io.File;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.cytoscape.util.swing.FileUtil;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.reactome.cytoscape.pathway.DiagramAndFactorGraphSwitcher;
import org.reactome.cytoscape.service.FICytoscapeAction;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;

/**
 * @author gwu
 *
 */
public class FactorGraphImportAction extends FICytoscapeAction {
    
    /**
     * @param title
     */
    public FactorGraphImportAction() {
        super("Import Factor Graph");
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(8.5f);
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.FICytoscapeAction#doAction()
     */
    @Override
    protected void doAction() {
        // Get a file
        File file = PlugInUtilities.getAnalysisFile("Import Factor Graph",
                                               FileUtil.LOAD);
        if (file == null) {
            return;
        }
        
        SAXBuilder builder = new SAXBuilder();
        try {
            Document jdomDoc = builder.build(file);
            // Convert it into org.w3.dom.Document to be used in JAXB
            org.w3c.dom.Document document = new DOMOutputter().output(jdomDoc);
            org.w3c.dom.Node docRoot = document.getDocumentElement();
            JAXBContext jc = JAXBContext.newInstance(FactorGraph.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            FactorGraph fg = (FactorGraph) unmarshaller.unmarshal(docRoot);
            DiagramAndFactorGraphSwitcher switcher = new DiagramAndFactorGraphSwitcher();
            switcher.convertFactorGraphToNetwork(fg, file.getName(), null);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot open a factor graph: " + e.getMessage(),
                                          "Error in Opening Factor Graph",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
}
