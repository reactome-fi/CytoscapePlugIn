/*
 * Created on Feb 10, 2014
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.ProgressPane;
import org.gk.util.StringUtils;
import org.jdom.Element;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This customized task is used to fetch FIs for a selected object in a displayed
 * pathway diagram.
 * @author gwu
 *
 */
public class FetchFIForPEInDiagramHelper {
    private Long peDbID;
    private CyZoomablePathwayEditor pathwayEditor;
    // Keep this map in order to map from DB_ID to _displayNames
    private Map<Long, String> dbIdToName;
    
    /**
     * The sole constructor needs two parameters.
     * @param peDbId the DB_ID of the selected PhysicalEntity
     * @param diagramDbId the DB_ID of the displayed PathwayDiagram
     */
    public FetchFIForPEInDiagramHelper(Long peDbId,
                                       CyZoomablePathwayEditor pathwayEditor) {
        this.peDbID = peDbId;
        this.pathwayEditor = pathwayEditor;
        dbIdToName = new HashMap<Long, String>();
        @SuppressWarnings("unchecked")
        List<Renderable> objects = pathwayEditor.getPathwayEditor().getDisplayedObjects();
        if (objects != null) {
            for (Renderable r : objects) {
                if (r instanceof HyperEdge || r.getReactomeId() == null)
                    continue;
                dbIdToName.put(r.getReactomeId(),
                               r.getDisplayName());
            }
        }
    }
    
    /**
     * The actual method to fetch FIs.
     * @param taskMonitor
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void fetchFIs() {
        RenderablePathway pathway = (RenderablePathway) pathwayEditor.getPathwayEditor().getRenderable();
        if (pathway == null) {
            // This should not be possible
            JOptionPane.showMessageDialog(pathwayEditor,
                                          "No pathway diagram has been loaded!",
                                          "Empty Diagram",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        Long pdId = pathway.getReactomeDiagramId();
        if (pdId == null) {
            // This may be a problem with some old pathway diagram
            JOptionPane.showMessageDialog(pathwayEditor,
                                          "Cannot find the DB_ID for the displayed pathway diagram!",
                                          "Null Pathway Diagram ID",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            RESTFulFIService service = new RESTFulFIService();
            ProgressPane progressPane = new ProgressPane();
            JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
            frame.setGlassPane(progressPane);
            progressPane.setMinimum(0);
            progressPane.setMaximum(100);
            progressPane.setTitle("Fetching FIs");
            progressPane.setVisible(true);
            progressPane.setText("Querying the server...");
            Element element = service.queryFIsForPEInDiagram(peDbID, pdId);
            // Parse the returned results
            List<Element> list = element.getChildren("geneInDiagramToGeneToPEIds");
            if (list == null || list.size() == 0) {
                progressPane.setValue(100);
                progressPane.setText("No extra FIs can be fetched for the selected object. (Note: FIs that can be\n" + 
                                              "extracted in the displayed diagram are not included for \"Fetching\".)");
                frame.getGlassPane().setVisible(false);
            }
            progressPane.setValue(100);
            progressPane.setText("Finish query.");
            showResults(list);
            frame.getGlassPane().setVisible(false);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(pathwayEditor,
                                          "Error in fetching FIs for a selected object in diagram:\n" + e.getMessage(),
                                          "Error in Fetching FIs",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showResults(List<Element> elements) {
        JFrame desktop = PlugInObjectManager.getManager().getCytoscapeDesktop();
        JDialog dialog = GKApplicationUtilities.createDialog(desktop,
                                                             "Fetch FIs Result");
        FIResultsPane resultPane = new FIResultsPane();
        resultPane.setFIs(elements);
        dialog.getContentPane().add(resultPane, BorderLayout.CENTER);
        dialog.setModal(true);
        dialog.setSize(580, 350);
        GKApplicationUtilities.center(dialog);
        dialog.setVisible(true);
    }
    
    /**
     * A customized JPanel is used to show fetched FIs. There may be many FIs can be fetched for a
     * selected PhysicalEntity, especially, for a Complex.
     */
    private class FIResultsPane extends JPanel {
        private JTable fiTable;
        private JTextArea titleText;
        
        public FIResultsPane() {
            init();
        }
        
        private void init() {
            Border border = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                               BorderFactory.createEmptyBorder(2, 2, 2, 2));
            setBorder(border);
            setLayout(new BorderLayout());
            
            // Some kind of indication
            titleText = new JTextArea();
            Font font = titleText.getFont();
            titleText.setFont(font.deriveFont(Font.BOLD));
            titleText.setEditable(false);
            titleText.setWrapStyleWord(true);
            titleText.setLineWrap(true);
            titleText.setBackground(getBackground());
            titleText.setText("Some text:");
            add(titleText, BorderLayout.NORTH);
            
            fiTable = new JTable();
            FITableModel tableModel = new FITableModel();
            fiTable.setModel(tableModel);
            TableRowSorter<FITableModel> sorter = new TableRowSorter<FetchFIForPEInDiagramHelper.FITableModel>(tableModel);
            fiTable.setRowSorter(sorter);
            add(new JScrollPane(fiTable), BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, FIResultsPane.this);
                    dialog.dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, FIResultsPane.this);
                    dialog.dispose();
                }
            });
            add(controlPane, BorderLayout.SOUTH);
        }
        
        public void setFIs(List<Element> fiList) {
            FITableModel model = (FITableModel) fiTable.getModel();
            model.setContent(fiList);
            int totalFIs = model.getRowCount();
            String name = dbIdToName.get(peDbID);
            if (name == null)
                name = peDbID + "";
            titleText.setText(totalFIs + " FI" + 
                    (totalFIs == 1 ? " has " : "s have") +
                    " been fetched for the selected \"" + name + "\". " + 
                    "(Note: FIs that can be extracted from the displayed diagram have " + 
                    "been excluded.)");
        }
        
    }
    
    private class FITableModel extends AbstractTableModel {
        private String[] tableHeaders = new String[]{"Gene in Selected Object",
                                                     "FI Partner",
                                                     "Objects Containing FI Patner"};
        private String[][] content;
        
        public FITableModel() {
        }
        
        public void setContent(List<Element> elements) {
            Map<String, Map<String, String>> geneToPartnerToPEs = new HashMap<String, Map<String, String>>();
            for (Element elm : elements) {
                String gene = elm.getChildText("gene");
                Map<String, String> partnerToPeIds = geneToPartnerToPEs.get(gene);
                if (partnerToPeIds == null) {
                    partnerToPeIds = new HashMap<String, String>();
                    geneToPartnerToPEs.put(gene, partnerToPeIds);
                }
                Element partnerGeneElm = elm.getChild("partnerGene");
                String partner = partnerGeneElm.getChildText("gene");
                @SuppressWarnings("unchecked")
                List<Element> peDbIdsElms = partnerGeneElm.getChildren("peDbIds");
                List<String> peNames = new ArrayList<String>();
                if (peDbIdsElms != null && peDbIdsElms.size() > 0) {
                    for (Element peDbIdsElm : peDbIdsElms) {
                        String text = peDbIdsElm.getText();
                        String name = dbIdToName.get(new Long(text));
                        if (name != null)
                            peNames.add(name);
                    }
                }
                if (peNames.size() > 1)
                    Collections.sort(peNames);
                partnerToPeIds.put(partner, 
                                   StringUtils.join(", ", peNames));
            }
            // Convert the map into an array for display
            // Need to find how many total rows
            int row = 0;
            for (String gene : geneToPartnerToPEs.keySet()) {
                row += geneToPartnerToPEs.get(gene).size();
            }
            content = new String[row][tableHeaders.length];
            List<String> geneList = new ArrayList<String>(geneToPartnerToPEs.keySet());
            Collections.sort(geneList);
            int index = 0;
            for (String gene : geneList) {
                Map<String, String> partnerToPEIds = geneToPartnerToPEs.get(gene);
                List<String> partnerList = new ArrayList<String>(partnerToPEIds.keySet());
                Collections.sort(partnerList);
                for (String partner : partnerList) {
                    content[index][0] = gene;
                    content[index][1] = partner;
                    content[index][2] = partnerToPEIds.get(partner);
                    index ++;
                }
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            if (content != null)
                return content.length;
            return 0;
        }

        @Override
        public int getColumnCount() {
            return tableHeaders.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (content == null)
                return "";
            if (rowIndex >= 0 && rowIndex < content.length)
                return content[rowIndex][columnIndex];
            return null;
        }

        @Override
        public String getColumnName(int column) {
            return tableHeaders[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
        
    }
}
