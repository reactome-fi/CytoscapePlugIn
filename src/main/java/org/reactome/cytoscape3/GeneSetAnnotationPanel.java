/*
 * Created on Jun 23, 2010
 *
 */
package org.reactome.cytoscape3;

import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import keggapi.KEGGLocator;
import keggapi.KEGGPortType;

import org.gk.util.StringUtils;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This customized JPanel is used to show annotations for modules.
 * @author wgm
 *
 */
public class GeneSetAnnotationPanel extends NetworkModulePanel {
    protected List<ModuleGeneSetAnnotation> annotations;
    protected JComboBox fdrFilter;

    public GeneSetAnnotationPanel() {
        init();
    }
    public GeneSetAnnotationPanel(String title){
        super(title);
        init();
    }
        
    private void init() {
        // Remove closeBtn 
        controlToolBar.remove(closeGlue);
        controlToolBar.remove(closeBtn);
        // Add some filters
        Font font = hideOtherNodesBox.getFont();
        JLabel filterLabel = new JLabel("Apply Filters: ");
        filterLabel.setFont(font);
        controlToolBar.addSeparator();
        controlToolBar.add(filterLabel);
        Double fdrValues[] = new Double[] {
                0.001d, 0.005d, 0.01d, 0.05d, 0.25d, 0.50d, 1.00d
        };
        fdrFilter = new JComboBox(fdrValues);
        fdrFilter.setFont(font);
        fdrFilter.setSelectedItem(1.0d);
        JLabel fdrLabel = new JLabel(" FDR");
        fdrLabel.setFont(font);
        controlToolBar.add(fdrLabel);
        controlToolBar.add(fdrFilter);
        // Add close button back
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        // Add listener to two spinners
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetAnnotations();
            }
        };
        fdrFilter.addActionListener(listener);
    }
    
    protected String getSelectedPathway() {
        return (String) contentTable.getValueAt(contentTable.getSelectedRow(), 0);
    }
    
    protected void doContentTablePopup(MouseEvent e) {
        JPopupMenu popupMenu = createExportAnnotationPopup();
        // This will work for pathway only
        String title = getTitle();
        if (!title.startsWith("Pathway")) {
            popupMenu.show(contentTable,
                           e.getX(),
                           e.getY());
            return;
        }
        // Only for one selected pathway
        int selectedCount = contentTable.getSelectedRowCount();
        if (selectedCount != 1)
            return;
        Point point = e.getPoint();
        // Make sure the mouse is at the correct row
        if (contentTable.rowAtPoint(point) != contentTable.getSelectedRow())
            return;
//        // Have to make sure the mouse is in the first column
//        int pointedCol = contentTable.columnAtPoint(point);
//        if ((isForModule && pointedCol != 1) ||
//            (!isForModule && pointedCol != 0))
//            return;
        // Get the selected pathway
        String pathway = getSelectedPathway();
        JMenuItem showDetailMenu = new JMenuItem("Show Pathway Detail");
        final String selectedPathway = pathway;
        showDetailMenu.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                showPathwayDetail(selectedPathway);
            }
        });
        popupMenu.addSeparator();
        popupMenu.add(showDetailMenu);
//        // Diagram diagrams for Reactome pathways only
//        if (pathway.endsWith("(R)")) {
            JMenuItem showPathwayDiagramMenu = new JMenuItem("Show Pathway Diagram");
            showPathwayDiagramMenu.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showPathwayDiagram(selectedPathway);
                }
            });
            popupMenu.add(showPathwayDiagramMenu);
//        }
        popupMenu.show(contentTable,
                       e.getX(),
                       e.getY());
    }

    private void showPathwayDiagram(String pathway) {
        try {
            // Highlite
            String nodes = (String) contentTable.getValueAt(contentTable.getSelectedRow(),
                                                            contentTable.getColumnCount() - 1);
            if (pathway.endsWith("(K)")) {
                showPathwayDiagramForKEGG(pathway,
                                          nodes);
            }
            else {
                CyPathwayDiagramHelper helper = CyPathwayDiagramHelper.getHelper();
                helper.showPathwayDiagram(pathway);
                helper.highlightNodes(nodes);
            }
        }
        catch(Exception e) {
            PlugInUtilities.showErrorMessage("Error in Showing Pathway", "Cannot show pathway diagram for " + pathway);
            System.err.println("GeneSetAnnotationPanel.showPathwayDiagram(): " + e);
            e.printStackTrace();
        }
    }
    
    private void showPathwayDiagramForKEGG(final String pathway,
                                           final String nodes) throws Exception {
        Thread t = new Thread(){
            public void run(){
                KEGGHelper keggHelper = new KEGGHelper();
                keggHelper.openKeggUrl(pathway, nodes);  
            }
        };
        t.start();
        
    }
    
    private void showPathwayDetail(String pathway) {
        RESTFulFIService service = new RESTFulFIService();
        try {
            if (pathway.endsWith("(K)")) {
                String name = pathway.substring(0, pathway.length() - 3); // Remove the tag (K).
                String id = service.queryKEGGPathwayId(name);
                String url = "http://www.genome.jp/kegg/pathway/hsa/hsa" + id + ".html";
                PlugInUtilities.openURL(url);
            }
            else {
                Long dbId = service.queryPathwayId(pathway);
                String dataSourceURL = PlugInScopeObjectManager.getManager().getDataSourceURL();
                PlugInUtilities.openURL(dataSourceURL + dbId);
            }
        }
        catch(IOException e) {
            JOptionPane.showMessageDialog(PlugInScopeObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot find the DB_ID for the selected pathway: " + pathway,
                                          "Error in Querying Pathway DB_ID",
                                          JOptionPane.ERROR_MESSAGE);
            System.err.println("GeneSetAnnotationPanel.showPathwayDetail(): " + e);
            e.printStackTrace();
        }
    }
    
    public void setAnnotations(List<ModuleGeneSetAnnotation> annotations) {
        this.annotations = annotations;
        resetAnnotations();
    }

    protected void resetAnnotations() {
        AnnotationTableModel model = (AnnotationTableModel) contentTable.getModel();
        model.setContent(annotations,
                         (Double) fdrFilter.getSelectedItem());
    }
    
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new AnnotationTableModel();
    }

    protected class AnnotationTableModel extends NetworkModuleTableModel {

        protected String[] geneSetHeaders = new String[] {
                "GeneSet",
                "RatioOfProteinInGeneSet",
                "NumberOfProteinInGeneSet",
                "ProteinFromNetwork",
                "P-value",
                "FDR",
                "Nodes"
        };
        
        public AnnotationTableModel() {
            columnHeaders = geneSetHeaders;
        }
        
        public void setContent(List<ModuleGeneSetAnnotation> annotations,
                               Double fdrCutoff) {
            tableData.clear();
            for (ModuleGeneSetAnnotation moduleAnnot : annotations) {
                // This should be sorted already
                List<GeneSetAnnotation> annots = moduleAnnot.getAnnotations();
                for (GeneSetAnnotation annot : annots) {
                    if (fdrCutoff != null &&
                        !annot.getFdr().startsWith("<") &&
                        (new Double(annot.getFdr()) > fdrCutoff)) // Filter based on FDR
                        continue; 
                    String[] row = new String[columnHeaders.length];
                    int start = 0;
                    row[start ++] = annot.getTopic();
                    row[start ++] = String.format("%.4f", annot.getRatioOfTopic());
                    row[start ++] = annot.getNumberInTopic() + "";
                    row[start ++] = annot.getHitNumber() + "";
                    row[start ++] = String.format("%.4f", annot.getPValue());
                    row[start ++] = annot.getFdr() + "";
                    row[start] = StringUtils.join(",", annot.getHitIds());
                    tableData.add(row);
                }
            }
            fireTableDataChanged();
        }

    }
    
}
