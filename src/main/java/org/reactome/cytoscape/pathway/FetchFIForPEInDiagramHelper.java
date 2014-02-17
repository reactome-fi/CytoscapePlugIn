/*
 * Created on Feb 10, 2014
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
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
            frame.getGlassPane().setVisible(false);
            showResults(list);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(pathwayEditor,
                                          "Error in fetching FIs for a selected object in diagram:\n" + e.getMessage(),
                                          "Error in Fetching FIs",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addFIsToPathwayDiagram(List<String[]> fis) {
        
    }
    
    private void showResults(List<Element> elements) {
        JFrame desktop = PlugInObjectManager.getManager().getCytoscapeDesktop();
        JDialog dialog = GKApplicationUtilities.createDialog(desktop,
                                                             "Fetch FIs Result");
        FIResultsPane resultPane = new FIResultsPane();
        resultPane.setFIs(elements);
        dialog.getContentPane().add(resultPane, BorderLayout.CENTER);
        // Don't want to use a modal dialog so that the user may check
        // PEs displayed in the dialog.
        //dialog.setModal(true);
        int width = 580;
        int height = 350;
        dialog.setSize(width, height);
        // Want to place this dialog at the center of desktop
        int x = (desktop.getWidth() - width) / 2;
        int y = (desktop.getHeight() - height) / 2;
        Point p = new Point(x, y);
        SwingUtilities.convertPointToScreen(p, desktop);
        dialog.setLocation(p);
        dialog.setVisible(true);
        if (!resultPane.isOkClicked)
            return;
        List<String[]> selectedFIs = resultPane.getSelectedFIs();
        if (selectedFIs == null || selectedFIs.size() == 0)
            return; // Nothing to be added
        addFIsToPathwayDiagram(selectedFIs);
    }
    
    /**
     * A customized JPanel is used to show fetched FIs. There may be many FIs can be fetched for a
     * selected PhysicalEntity, especially, for a Complex.
     */
    private class FIResultsPane extends JPanel {
        private JTable fiTable;
        private JTextArea titleText;
        private JButton addFIBtn;
        private boolean isOkClicked = false;
        
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
            
            // Served as a control panel
            JPanel controlPane = new JPanel();
            controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            addFIBtn = new JButton();
            addFIBtn.setText("Add Selected FIs to Diagram");
            JButton closeBtn = new JButton();
            closeBtn.setText("Close");
            controlPane.add(addFIBtn);
            controlPane.add(closeBtn);
            
            addFIBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, FIResultsPane.this);
                    dialog.dispose();
                    isOkClicked = true;
                }
            });
            closeBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, FIResultsPane.this);
                    dialog.dispose();
                    isOkClicked = false;
                }
            });
            add(controlPane, BorderLayout.SOUTH);
            
            installListeners();
        }
        
        private void installListeners() {
            // Enable selection synchronization
            addFIBtn.setEnabled(false);
            fiTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (fiTable.getSelectedRowCount() > 0)
                        addFIBtn.setEnabled(true);
                    else
                        addFIBtn.setEnabled(false);
                }
            });
            fiTable.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) 
                        doTablePopup(e);
                    else if (e.getClickCount() == 2)
                        queryFISource();
                }
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger())
                        doTablePopup(e);
                }
            });
        }
        
        private void doTablePopup(MouseEvent e) {
            // Work for one selection only
            if (fiTable.getSelectedRowCount() != 1)
                return;
            JPopupMenu popup = new JPopupMenu();
            JMenuItem goToReactome = new JMenuItem("Query FI Source");
            goToReactome.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    queryFISource();
                }
            });
            popup.add(goToReactome);
            popup.show(fiTable,
                       e.getX(),
                       e.getY());
        }
        
        private void queryFISource() {
            if (fiTable.getSelectedRowCount() != 1)
                return;
            System.out.println("Query FI Source: " + "");
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
                    "been excluded. Use Control-Click (Windows) or Command-Click (Mac OS) for "
                    + "multiple selections.)");
        }
        
        /**
         * Get the selected FIs listed in the table.
         * @return
         */
        public List<String[]> getSelectedFIs() {
            List<String[]> rtn = new ArrayList<String[]>();
            if (fiTable.getSelectedRows() != null) {
                for (int row : fiTable.getSelectedRows()) {
                    String[] fi = new String[] {
                            fiTable.getValueAt(row, 0) + "",
                            fiTable.getValueAt(row, 1) + "",
                            fiTable.getValueAt(row, 2) + ""
                    };
                    rtn.add(fi);
                }
            }
            return rtn;
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
