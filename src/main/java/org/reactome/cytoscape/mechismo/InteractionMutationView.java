package org.reactome.cytoscape.mechismo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureIO;
import org.biojava.nbio.structure.align.gui.jmol.JmolPanel;
import org.biojava.nbio.structure.align.gui.jmol.RasmolCommandListener;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.mechismo.model.Interaction;
import org.reactome.mechismo.model.Mutation;
import org.reactome.mechismo.model.Residue;
import org.reactome.mechismo.model.ResidueStructure;
import org.reactome.mechismo.model.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this class to display mutations involved in a functional interaction. The view
 * is based on StructureAlignmentJMol so that protein 3D structures can be displayed.
 * @author wug
 *
 */
@SuppressWarnings("serial")
public class InteractionMutationView extends JDialog {
    private final Logger logger = LoggerFactory.getLogger(InteractionMutationView.class);
    private MutationPane mutationPane;
    private JTabbedPane structureTabs;
    // Keep this map for quick access
    private Map<String, StructurePane> pdbToStructurePane;
    
    public InteractionMutationView() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    public void setInteraction(Interaction interaction) {
        Set<Mutation> mutations = interaction.getMutations();
        if (mutations == null || mutations.size() == 0) {
            JOptionPane.showMessageDialog(getOwner(),
                    "Cannot find any mutation related to this interaction.", 
                    "No Mutation", 
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Multiple structures may be cited in one interaction
        Set<String> pdbIds = new HashSet<>();
        for (Mutation mutation : mutations) {
            ResidueStructure structure = mutation.getResidue().getStructure();
            if (structure.getPdb() != null)
                pdbIds.add(structure.getPdb());
        }
        if (pdbIds.size() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Cannot find any PDB identifier associated with this interaction.",
                    "No PDB",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        displayStructures(pdbIds);
        mutationPane.setInteraction(interaction);
        highlightVariants();
        // The title should be set after display structure. Otherwise,
        // PDB id will be used as title.
        String title = "Mutation Profile for " + interaction.getName().replace("\t", " ");
        setTitle(title);
        setMutationPaneTitle(interaction);
        
        setModal(false);
        setVisible(true);
    }
    
    private void displayStructures(Set<String> pdbIds) {
        // Need to sort it first
        pdbIds.stream().sorted().forEach(pdbId -> {
            StructurePane pane = new StructurePane();
            try {
                pane.displayStructure(pdbId);
                structureTabs.add(pdbId, pane);
                pdbToStructurePane.put(pdbId, pane);
            } 
            catch (Exception e) {
                logger.error(e.getMessage(), e);
                JOptionPane.showMessageDialog(InteractionMutationView.this,
                                              "Cannot show structure for " + pdbId + ": " + e.getMessage(),
                                              "Error in Structure Display",
                                              JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private void setMutationPaneTitle(Interaction interaction) {
        StringBuilder builder = new StringBuilder();
        String name = interaction.getName().replaceAll("\t", " ");
        builder.append("Mutations in ").append(name).append(": ");
        mutationPane.titleLabel.setText(builder.toString());
    }
    
    private void highlightVariants() {
        // Get a list of residues to highlight
        Map<String, String> pdbToResidueInChain = mutationPane.getResidueInChains();
        pdbToResidueInChain.forEach((pdb, residues) -> {
            StructurePane structurePane = pdbToStructurePane.get(pdb);
            if (structurePane == null)
                return;
            structurePane.highlightVariants(residues);
            structurePane.saveState(); // To be reset if needed
        });
    }
    
    /**
     * Need some customization here.
     */
    private void init() {
        setLocationRelativeTo(PlugInObjectManager.getManager().getCytoscapeDesktop());
        setSize(700, 800);
        
        structureTabs = new JTabbedPane();
        
        mutationPane = new MutationPane();
        mutationPane.mutationTable.getSelectionModel().addListSelectionListener(e -> handleTableSelection());
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, structureTabs, mutationPane);
        jsp.setDividerLocation(550); // Assign a more space for the structure part
        
        add(jsp, BorderLayout.CENTER);
        
        JPanel controlPane = new JPanel();
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        controlPane.add(closeBtn);
        add(controlPane, BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
            
        });
        
        pdbToStructurePane = new HashMap<>();
    }
    
    private void handleTableSelection() {
        Map<String, String> pdbToResidues = mutationPane.getSelectedResidueInChains();
        pdbToResidues.forEach((pdb, residues) -> {
            StructurePane structurePane = pdbToStructurePane.get(pdb);
            if (structurePane != null) {
                structurePane.selectResidues(residues);
                // This is a little bit random if more than one structure
                // is selected
                structureTabs.setSelectedComponent(structurePane);
            }
        });
    }
    
    /**
     * A list of mutations
     * @author wug
     *
     */
    private class MutationPane extends JPanel {
        private JLabel titleLabel;
        private JTable mutationTable;
        private JTextField filterTF;
        
        public MutationPane() {
            init();
        }
        
        private void init() {
            setBorder(BorderFactory.createEtchedBorder());
            setLayout(new BorderLayout());
            titleLabel = new JLabel("Mutations:");
            titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            Font font = titleLabel.getFont();
            titleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize() - 1.0f));
            add(titleLabel, BorderLayout.NORTH);
            mutationTable = new JTable();
            MutationTableModel model = new MutationTableModel();
            mutationTable.setModel(model);
            mutationTable.setAutoCreateRowSorter(true);
            add(new JScrollPane(mutationTable), BorderLayout.CENTER);
            // Add a filter pane
            JPanel filterPane = new JPanel();
            filterPane.setBorder(BorderFactory.createEtchedBorder());
            filterPane.setLayout(new FlowLayout(FlowLayout.LEFT));
            JLabel filterLabel = new JLabel("Filter rows:");
            filterPane.add(filterLabel);
            filterTF = new JTextField();
            filterTF.setColumns(20);
            filterTF.addActionListener(event -> filterTable());
            filterPane.add(filterTF);
            add(filterPane, BorderLayout.SOUTH);
        }
        
        @SuppressWarnings({"rawtypes", "unchecked"})
        private void filterTable() {
            String text = filterTF.getText().trim();
            TableRowSorter sorter = (TableRowSorter) mutationPane.mutationTable.getRowSorter();
            sorter.setRowFilter(RowFilter.regexFilter(text));
        }
        
        public void setInteraction(Interaction interaction) {
            MutationTableModel model = (MutationTableModel) mutationTable.getModel();
            model.setInteraction(interaction);
        }
        
        public Map<String, String> getResidueInChains() {
            MutationTableModel model = (MutationTableModel) mutationTable.getModel();
            Map<String, Set<String>> pdbToResidues = model.getResiduesInChains();
            return getResidueInChains(pdbToResidues);
        }

        private Map<String, String> getResidueInChains(Map<String, Set<String>> pdbToResidues) {
            Map<String, String> pdbToText = new HashMap<>();
            pdbToResidues.forEach((pdb, residues) -> pdbToText.put(pdb, String.join(",", residues)));
            return pdbToText;
        }
        
        public Map<String, String> getSelectedResidueInChains() {
            MutationTableModel model = (MutationTableModel) mutationTable.getModel();
            int[] selectedRows = mutationTable.getSelectedRows();
            if (selectedRows == null || selectedRows.length == 0)
                return null;
            List<Integer> rowList = new ArrayList<>();
            for (int i = 0; i < selectedRows.length; i++) {
                int rowInModel = mutationTable.convertRowIndexToModel(selectedRows[i]);
                rowList.add(rowInModel);
            }
            return getResidueInChains(model.getResiduesInChains(rowList));
        }
        
    }
    
    /**
     * The following customized JPanel is modified from BiojavaJmol class so that
     * we can create multiple copies of structure view.
     * @author wug
     *
     */
    private class StructurePane extends JPanel {
        private JmolPanel jmolPanel;
        private JComboBox<String> colorBox;
        
        public StructurePane() {
            init();
        }
        
        // The following code was copied directly from BiojavaJMol class with a little bit
        // of modification.
        private void init() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            jmolPanel = new JmolPanel();

            jmolPanel.setPreferredSize(new Dimension(500,500));
            add(jmolPanel);

            JTextField field = new JTextField();

            field.setMaximumSize(new Dimension(Short.MAX_VALUE,30));
            field.setText("Enter RASMOL like command...");
            RasmolCommandListener listener = new RasmolCommandListener(jmolPanel,field) ;

            field.addActionListener(listener);
            field.addMouseListener(listener);
            field.addKeyListener(listener);
            add(field);

            Box hBox1 = Box.createHorizontalBox();
            hBox1.setMaximumSize(new Dimension(Short.MAX_VALUE,30));

            String[] styles = new String[] { "Cartoon", "Backbone", "CPK", "Ball and Stick", "Ligands","Ligands and Pocket"};
            JComboBox<String> style = new JComboBox<>(styles);

            hBox1.add(new JLabel("Style"));
            hBox1.add(style);
            add(hBox1);

            style.addActionListener(jmolPanel);

            String[] colorModes = new String[] { "Secondary Structure", "By Chain", "Rainbow", "By Element", "By Amino Acid", "Hydrophobicity" };
            colorBox = new JComboBox<>(colorModes);
            colorBox.addActionListener(jmolPanel);
            hBox1.add(Box.createGlue());
            hBox1.add(new JLabel("Color"));
            hBox1.add(colorBox);
            
            // Check boxes
            Box hBox2 = Box.createHorizontalBox();
            hBox2.setMaximumSize(new Dimension(Short.MAX_VALUE,30));

            // Make sure select none is called
            JButton resetDisplay = new JButton("Reset Display");

            resetDisplay.addActionListener(e -> {
                jmolPanel.executeCmd("restore STATE state_1; select none; ");
            });

            hBox2.add(resetDisplay); hBox2.add(Box.createGlue());

            JCheckBox toggleSelection = new JCheckBox("Show Selection");
            toggleSelection.addItemListener(e -> {
                boolean showSelection = (e.getStateChange() == ItemEvent.SELECTED);
                if (showSelection){
                    jmolPanel.executeCmd("set display selected");
                } else {
                    jmolPanel.executeCmd("set display off");
                }
            });
            // Default should be selected
            toggleSelection.setSelected(true);
            hBox2.add(toggleSelection);
            hBox2.add(Box.createGlue());
            add(hBox2);
        }
        
        public void displayStructure(String pdbId) throws Exception {
            Structure struc = StructureIO.getStructure(pdbId);
            String pdb = struc.toPDB();
            jmolPanel.openStringInline(pdb);
            // send some commands to Jmol
            // The following lines are needed for a nice preview
            evalString("select * ; color chain;");            
            evalString("select *; spacefill off; wireframe off; cartoon on;  ");
            evalString("select ligands; cartoon off; wireframe 0.3; spacefill 0.5; color cpk;");
            colorBox.setSelectedIndex(1); // Color by chain is chosen
            // To show selection. To show this, have to make sure selection checkbox is checked
            evalString("set display selected;");
        }
        
        public void highlightVariants(String residuesInChain) {
            // After that, don't select any more
            evalString("select " + residuesInChain + "; color white; spacefill; zoom out; select none; ");
        }
        
        public void saveState() {
            jmolPanel.evalString("save STATE state_1"); // Save this state to reset it
        }
        
        public void selectResidues(String residues) {
            evalString("select none; select " + residues + "; ");
        }
        
        private void evalString(String string) {
            jmolPanel.evalString(string);
        }
        
    }
    
    private class MutationTableModel extends AbstractTableModel {
        private String[] headers = new String[] {
                "Cancer",
                "Sample",
                "Mutation",
                "MechismoScore",
                "PDB",
                "ResidueInPDB"
        };
        private List<List<Object>> data;
        
        public MutationTableModel() {
            data = new ArrayList<>();
        }
        
        public void setInteraction(Interaction interaction) {
            data.clear();
            Set<Mutation> mutations = interaction.getMutations();
            if (mutations == null || mutations.size() == 0)
                return;
            // We may see duplication rows if no normalizatio is done.
            // Duplication may be caused by one residue interacts with multiple
            // other residues and mutation involves frameshift. 
            Set<String> set = new HashSet<>();
            mutations.forEach(mutation -> {
                // If a mutation has many samples, we will expand them
                Set<Sample> samples = mutation.getSamples();
                ResidueStructure structure = mutation.getResidue().getStructure();
                String variant = getVariant(mutation);
                String residueStructue = getResidueStructure(structure);
                Double mechismoScore = mutation.getResidue().getMechismoScore();
                samples.forEach(sample -> {
                    List<String> row = new ArrayList<>();
                    row.add(sample.getCancerType().getAbbreviation());
                    row.add(sample.getName());
                    row.add(variant);
                    row.add(mechismoScore + "");
                    row.add(structure.getPdb());
                    row.add(residueStructue);
                    set.add(String.join("\t", row));
                });
            });
            set.forEach(row -> {
                String[] tokens = row.split("\t");
                // the third is a Double
                List<Object> rowData = new ArrayList<>();
                for (int i = 0; i < tokens.length; i++) {
                    if (i == 3) {
                        if (tokens[i].equals("null"))
                            rowData.add(null);
                        else
                            rowData.add(new Double(tokens[i]));
                    }
                    else
                        rowData.add(tokens[i]);
                }
                data.add(rowData);
            });
            fireTableStructureChanged();
        }
        
        private String getVariant(Mutation mutation) {
            StringBuilder builder = new StringBuilder();
            Residue residue = mutation.getResidue();
            builder.append(residue.getProtein().getName());
            builder.append(":");
            builder.append(residue.getResidue());
            builder.append(residue.getPosition());
            builder.append(mutation.getVariant());
            return builder.toString();
        }
        
        private String getResidueStructure(ResidueStructure structure) {
            StringBuilder builder = new StringBuilder();
            // Get the chain first
            builder.append(structure.getPdbChain());
            builder.append(":");
            builder.append(structure.getPdbResidue());
            builder.append(structure.getPdbPosition());
            return builder.toString();
        }
        
        public Map<String, Set<String>> getResiduesInChains() {
            List<Integer> rows = IntStream.range(0, getRowCount()).mapToObj(i -> new Integer(i)).collect(Collectors.toList());
            return getResiduesInChains(rows);
        }
        
        public Map<String, Set<String>> getResiduesInChains(List<Integer> rows) {
            Map<String, Set<String>> pdbToResidues = new HashMap<>();
            Pattern pattern = Pattern.compile("\\d+");
            rows.forEach(row -> {
                String residueInPDB = (String) getValueAt(row, getColumnCount() - 1);
                // Some parsing
                String[] tokens = residueInPDB.split(":");
                Matcher matcher = pattern.matcher(tokens[1]);
                if (matcher.find()) {
                    String location = matcher.group(0);
                    String text = location + ":" + tokens[0];
                    String pdb = (String) getValueAt(row, getColumnCount() - 2);
                    pdbToResidues.compute(pdb, (key, set) -> {
                        if (set == null)
                            set = new HashSet<>();
                        set.add(text);
                        return set;
                    });
                }
                else // Which should not be possible
                    logger.error("Cannot find PDB residue location for " + residueInPDB);
            });
            return pdbToResidues;
        }
        
        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            List<Object> row = data.get(rowIndex);
            return row.get(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3)
                return Double.class;
            return String.class;
        }
        
        
    }

}
