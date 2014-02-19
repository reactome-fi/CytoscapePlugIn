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

import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableInteraction;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableProtein;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.ProgressPane;
import org.gk.util.StringUtils;
import org.jdom.Element;
import org.reactome.cytoscape.service.FISourceQueryHelper;
import org.reactome.cytoscape.service.JiggleLayout;
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
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        try {
            RESTFulFIService service = new RESTFulFIService();
            ProgressPane progressPane = new ProgressPane();
            frame.setGlassPane(progressPane);
            progressPane.setMinimum(0);
            progressPane.setMaximum(100);
            progressPane.setIndeterminate(true);
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
            frame.getGlassPane().setVisible(false);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void addFIsToPathwayDiagram(List<SimpleFI> fis) {
        // Get the selected PE as the anchor for adding FIs
        PathwayEditor editor = pathwayEditor.getPathwayEditor();
        List<Renderable> selection = editor.getSelection();
        if (selection == null || selection.size() != 1)
            return;
        Renderable r = selection.get(0);
        if (!(r instanceof Node))
            return;
        Node node = (Node) r;
        // Two or more FIs can link two PEs, including newly added genes and 
        // existing pathway PEs. Use this map to merge them together into one 
        // single FI since multiple FIs cannot be displayed properly in the 
        // pathway diagram.
        // Some FIs may have been added previously. Newly added FIs should be
        // merged into them if possible.
        Map<String, RenderableInteraction> nodesToInteraction = new HashMap<String, RenderableInteraction>();
        getPreAddedFIs(node, nodesToInteraction);
        filterPreAddedFIs(fis, nodesToInteraction);
        List<Renderable> newNodes = new ArrayList<Renderable>();
        for (SimpleFI fi : fis) {
            Node partner = null;
            // Check if a FI should be added to the existing objects
            if (fi.existedPEs.length() == 0) {
                partner = getRenderableForGene(fi.partner2,
                                               newNodes);
                createInteraction(node, 
                                  partner, 
                                  fi, 
                                  editor,
                                  nodesToInteraction);
            }
            else {
                // Need to split the text first
                String[] names = fi.existedPEs.split(", ");
                for (String name : names) {
                    partner = getNodeForName(name);
                    if (partner == null)
                        continue;
                    createInteraction(node, 
                                      partner, 
                                      fi, 
                                      editor,
                                      nodesToInteraction);
                }
            }
        }
        // Do a layout
        layout(node, newNodes);
        editor.repaint(editor.getVisibleRect());
    }
    
    private void filterPreAddedFIs(List<SimpleFI> fis,
                                   Map<String, RenderableInteraction> nodesToFI) {
        List<SimpleFI> preFIs = new ArrayList<SimpleFI>();
        for (SimpleFI fi : fis) {
            String name = generateFIName(fi);
            for (RenderableInteraction rFI : nodesToFI.values()) {
                // This check should be reliable enough since the format used
                // to generate a name for a FI.
                if (rFI.getDisplayName().contains(name)) {
                    preFIs.add(fi);
                    break;
                }
            }
        }
        if (preFIs.size() == 0)
            return;
        fis.removeAll(preFIs);
        if (fis.size() == 0) {
            String message = (preFIs.size() == 1 ? "The selected FI has " : "The selected FIs have ");
            message = "been added before!";
            JOptionPane.showMessageDialog(pathwayEditor,
                                          message,
                                          "Adding FIs",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Generate a message
        StringBuilder builder = new StringBuilder();
        if (preFIs.size() == 1) 
            builder.append("This selected FI has been added, and will not be "
                    + "added again: " + generateFIName(preFIs.get(0)));
        else {
            builder.append("The following FIs have been added, and will not be " + 
                           "added again:");
            for (SimpleFI fi : preFIs) {
                builder.append("\n").append(generateFIName(fi));
            }
        }
        JOptionPane.showMessageDialog(pathwayEditor,
                                      builder.toString(),
                                      "Adding FIs",
                                      JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void getPreAddedFIs(Node node,
                                Map<String, RenderableInteraction> nodesToFI) {
        List<HyperEdge> edges = node.getConnectedReactions();
        for (HyperEdge edge : edges) {
            if (edge instanceof FIRenderableInteraction) {
                Node input = edge.getInputNode(0);
                Node output = edge.getOutputNode(0);
                String key = generateKeyForFINodes(input, output);
                nodesToFI.put(key, (RenderableInteraction)edge);
            }
        }
    }
    
    /**
     * Do a giggle layout around the center nodes for newly added FI partners.
     * @param node
     * @param newNodes
     */
    private void layout(Node node,
                        List<Renderable> newNodes) {
        if (newNodes.size() == 0)
            return;
        List<String> newNames = new ArrayList<String>();
        for (Renderable r : newNodes)
            newNames.add(r.getDisplayName());
        Map<String, double[]> nameToCoords = new JiggleLayout().jiggleLayout(node.getDisplayName(),
                                                                             newNames);
        double dx = node.getPosition().getX() - nameToCoords.get(node.getDisplayName())[0];
        double dy = node.getPosition().getY() - nameToCoords.get(node.getDisplayName())[1];
        for (Renderable r : newNodes) {
            double[] coords = nameToCoords.get(r.getDisplayName());
            int x = (int) (coords[0] + dx);
            int y = (int) (coords[1] + dy);
            // Should not be allowed outside the bounds
            if (x < 50) // These numbers 50 and 25 are rather arbitrary
                x = 50;
            if (y < 25)
                y = 25;
            r.setPosition(x, y);
            List<HyperEdge> interactions = ((Node)r).getConnectedReactions();
            for (HyperEdge edge : interactions)
                edge.layout();
        }
    }

    private void createInteraction(Node node, 
                                   Node partner, 
                                   SimpleFI fi,
                                   PathwayEditor editor,
                                   Map<String, RenderableInteraction> nodesToInteraction) {
        String key = generateKeyForFINodes(node, partner);
        FIRenderableInteraction interaction = (FIRenderableInteraction) nodesToInteraction.get(key);
        if (interaction != null) {
            // Add a new name
            String name = interaction.getDisplayName();
            name += ", " + generateFIName(fi);
            interaction.setDisplayName(name);
            // Merge directions together too.
            interaction.addDirections(fi.direction);
            return;
        }
        // Create an interaction
        interaction = new FIRenderableInteraction();
        interaction.addInput(node);
        interaction.addOutput(partner);
        interaction.setDirections(fi.direction);
        // Add a display name
        interaction.setDisplayName(generateFIName(fi));
        interaction.layout();
        editor.insertEdge(interaction, false);
        nodesToInteraction.put(key, interaction);
    }
    
    private String generateFIName(SimpleFI fi) {
        String partner1 = fi.partner1;
        String partner2 = fi.partner2;
        if (partner1.compareTo(partner2) < 0)
            return partner1 + " - " + partner2;
        else
            return partner2 + " - " + partner1;
    }

    private String generateKeyForFINodes(Node node, Node partner) {
        // Generate a key
        String name1 = node.getDisplayName();
        String name2 = partner.getDisplayName();
        String key = null;
        if (name1.compareTo(name2) < 0)
            key = name1 + " - " + name2;
        else
            key = name2 + " - " + name1;
        return key;
    }
    
    /**
     * Get a RenderableProtein for a gene specified by its name. If this gene has been
     * added in the diagram, the previously added RenderableProtein should be returned.
     * Otherwise, a new RenderableProtein should be created. For the time being, only
     * RenderableProtein will be created assuming that FIs involve proteins only.
     * @param gene
     * @return
     */
    private RenderableProtein getRenderableForGene(String gene,
                                                   List<Renderable> newNodes) {
        for (Object obj : pathwayEditor.getPathwayEditor().getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            if (r instanceof HyperEdge || r.getReactomeId() != null)
                continue;
            if (r instanceof RenderableProtein && r.getDisplayName().equals(gene))
                return (RenderableProtein) r;
        }
        // Need to add a new RenderableProtein
        RenderableProtein protein = new RenderableProtein();
        protein.setDisplayName(gene);
        pathwayEditor.getPathwayEditor().insertNode(protein);
        newNodes.add(protein);
        return protein;
    }
    
    private Node getNodeForName(String name) {
        for (Object obj : pathwayEditor.getPathwayEditor().getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            if (!(r instanceof Node) || r.getReactomeId() == null)
                continue;
            Node node = (Node) r;
            if (node.getDisplayName().equals(name))
                return node;
        }
        return null;
    }
    
    private void showResults(List<Element> elements) {
        JFrame desktop = PlugInObjectManager.getManager().getCytoscapeDesktop();
        JDialog dialog = GKApplicationUtilities.createDialog(desktop,
                                                             "Fetch FIs Result");
        FIResultsPane resultPane = new FIResultsPane();
        resultPane.setFIs(elements);
        if (resultPane.getTotalFIs() == 0) {
            JOptionPane.showMessageDialog(desktop,
                                          "No FI can be fetched for the selected object. (Note: FIs that can\n"
                                          + "be extracted from the displayed pathway diagram are excluded.)",
                                          "No FI",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
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
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!resultPane.isOkClicked)
            return;
        List<SimpleFI> selectedFIs = resultPane.getSelectedFIs();
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
            int selectedRow = fiTable.getSelectedRow();
            String partner1 = (String) fiTable.getValueAt(selectedRow, 0);
            String partner2 = (String) fiTable.getValueAt(selectedRow, 1);
            FISourceQueryHelper helper = new FISourceQueryHelper();
            helper.queryFISource(partner1,
                                 partner2,
                                 this);
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
         * Check how many FIs displayed in total.
         * @return
         */
        public int getTotalFIs() {
            return fiTable.getModel().getRowCount();     
        }
        
        /**
         * Get the selected FIs listed in the table.
         * @return
         */
        public List<SimpleFI> getSelectedFIs() {
            List<SimpleFI> rtn = new ArrayList<SimpleFI>();
            if (fiTable.getSelectedRows() != null) {
                for (int row : fiTable.getSelectedRows()) {
                    SimpleFI fi = new SimpleFI();
                    fi.partner1 = fiTable.getValueAt(row, 0) + "";
                    fi.partner2 = fiTable.getValueAt(row, 1) + "";
                    fi.direction = fiTable.getValueAt(row, 2) + "";
                    fi.existedPEs = fiTable.getValueAt(row, 3) + "";
                    rtn.add(fi);
                }
            }
            return rtn;
        }
        
    }
    
    /**
     * A simple class to encode a FI returned from the server.
     * @author gwu
     *
     */
    private class SimpleFI {
        private String partner1;
        private String partner2;
        private String direction;
        private String existedPEs;
        
        public SimpleFI() {
        }
    }
    
    private class FITableModel extends AbstractTableModel {
        private String[] tableHeaders = new String[]{"Gene in Selected Object",
                                                     "FI Partner",
                                                     "FI Direction",
                                                     "Objects Containing FI Patner"};
        private String[][] content;
        
        public FITableModel() {
        }
        
        public void setContent(List<Element> elements) {
            Map<String, Map<String, String>> geneToPartnerToPEs = new HashMap<String, Map<String, String>>();
            Map<String, Map<String, String>> geneToPartnerToDirection = new HashMap<String, Map<String, String>>();
            for (Element elm : elements) {
                String gene = elm.getChildText("gene");
                Map<String, String> partnerToPeIds = geneToPartnerToPEs.get(gene);
                if (partnerToPeIds == null) {
                    partnerToPeIds = new HashMap<String, String>();
                    geneToPartnerToPEs.put(gene, partnerToPeIds);
                }
                Map<String, String> partnerToDirection = geneToPartnerToDirection.get(gene);
                if (partnerToDirection == null) {
                    partnerToDirection = new HashMap<String, String>();
                    geneToPartnerToDirection.put(gene, partnerToDirection);
                }
                Element partnerGeneElm = elm.getChild("partnerGene");
                String partner = partnerGeneElm.getChildText("gene");
                String direction = elm.getChildText("direction");
                partnerToDirection.put(partner, direction);
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
                Map<String, String> partnerToDirection = geneToPartnerToDirection.get(gene);
                List<String> partnerList = new ArrayList<String>(partnerToPEIds.keySet());
                Collections.sort(partnerList);
                for (String partner : partnerList) {
                    content[index][0] = gene;
                    content[index][1] = partner;
                    content[index][2] = partnerToDirection.get(partner);
                    content[index][3] = partnerToPEIds.get(partner);
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
