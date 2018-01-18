package org.reactome.cytoscape.mechismo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureIO;
import org.biojava.nbio.structure.gui.BiojavaJmol;
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
public class InteractionMutationView extends BiojavaJmol {
    private final Logger logger = LoggerFactory.getLogger(InteractionMutationView.class);
    private MutationPane mutationPane;
    
    public InteractionMutationView() {
        super();
        init();
    }
    
    public void setInteraction(Interaction interaction) {
        // The following is for test
        String pdb = null;
        Set<Mutation> mutations = interaction.getMutations();
        for (Mutation mutation : mutations) {
            ResidueStructure structure = mutation.getResidue().getStructure();
            pdb = structure.getPdb();
            break;
        }
        if (pdb == null) {
            JOptionPane.showMessageDialog(getFrame(),
                    "Cannot find any PDB identifier associated with this interaction!",
                    "No PDB",
                    JOptionPane.ERROR_MESSAGE);
        }
        else {
            mutationPane.setInteraction(interaction);
            displayStructure(pdb);
            highlightVariants();
            // The title should be set after display structure. Otherwise,
            // PDB id will be used as title.
            String title = "Mutation Profile for " + interaction.getName();
            title = title.replace("\t", " "); // Since interaction using tab.
            getFrame().setTitle(title);
            setMutationPaneTitle(interaction);
        }
    }
    
    private void setMutationPaneTitle(Interaction interaction) {
        StringBuilder builder = new StringBuilder();
        String name = interaction.getName().replaceAll("\t", " ");
        builder.append("Mutations in ").append(name).append(": ");
        mutationPane.titleLabel.setText(builder.toString());
    }
    
    private void displayStructure(String pdb) {
        try {
            Structure struc = StructureIO.getStructure(pdb);
            setStructure(struc);
            // send some commands to Jmol
            // The following lines are needed for a nice preview
            evalString("select * ; color chain;");            
            evalString("select *; spacefill off; wireframe off; cartoon on;  ");
            evalString("select ligands; cartoon off; wireframe 0.3; spacefill 0.5; color cpk;");
            // To show selection. To show this, have to make sure selection checkbox is checked
            evalString("set display selected;");
        } 
        catch (Exception e){
            logger.error("displayStructure: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(getFrame(),
                                         "Error in showing structure: " + e.getMessage(),
                                         "Error in Structure Display",
                                         JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void highlightVariants() {
        // Get a list of residues to highlight
        String residueInChain = mutationPane.getResidueInChains();
//        System.out.println(residueInChain);
        // After that, don't select any more
        evalString("select " + residueInChain + "; color white; spacefill; zoom out; select none; ");
    }
    
    /**
     * Need some customization here.
     */
    private void init() {
        JFrame frame = getFrame();
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar); // Don't want to have any menu bar
        WindowListener[] windowListeners = frame.getWindowListeners();
        if (windowListeners != null && windowListeners.length > 0) 
            Arrays.asList(windowListeners).forEach(l -> frame.removeWindowListener(l));
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
            }
        });
        frame.setLocationRelativeTo(PlugInObjectManager.getManager().getCytoscapeDesktop());
        frame.setSize(700, 800);
        
        Component jmolPane = getJMolContainer(frame);
        if (jmolPane == null)
            throw new IllegalStateException("Cannot find jMol pane!");
        checkShowSelectionBox(jmolPane);
        
        Container container = frame.getContentPane();
        container.remove(jmolPane);
        
        mutationPane = new MutationPane();
        mutationPane.mutationTable.getSelectionModel().addListSelectionListener(e -> handleTableSelection());
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jmolPane, mutationPane);
        jsp.setDividerLocation(550); // Assign a more space for the structure part
        
        container.add(jsp, BorderLayout.CENTER);
    }
    
    private void handleTableSelection() {
        String residues = mutationPane.getSelectedResidueInChains();
        evalString("select none; select " + residues + "; ");
    }
    
    private void checkShowSelectionBox(Component jmolPane) {
        if (!(jmolPane instanceof Box))
            return;
        Box box = (Box) jmolPane;
        Set<Container> current = new HashSet<>();
        Set<Container> next = new HashSet<>();
        current.add(box);
        while (current.size() > 0) {
            for (Container container : current) {
                Component[] comps = container.getComponents();
                if (comps == null || comps.length == 0)
                    continue;
                for (int i = 0; i < comps.length; i++) {
                    if (comps[i] instanceof JCheckBox) {
                        JCheckBox checkBox = (JCheckBox) comps[i];
                        String label = checkBox.getText();
                        if (label.equals("Show Selection")) {
                            checkBox.setSelected(true);
                            break;
                        }
                    }
                    else if (comps[i] instanceof Container)
                        next.add((Container)comps[i]);
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
    }
    
    private Component getJMolContainer(JFrame frame) {
        Component[] comps = frame.getContentPane().getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof Box)
                return comps[i];
        }
        return null;
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
        
        public String getResidueInChains() {
            MutationTableModel model = (MutationTableModel) mutationTable.getModel();
            Set<String> residues = model.getResiduesInChains();
            return String.join(",", residues);
        }
        
        public String getSelectedResidueInChains() {
            MutationTableModel model = (MutationTableModel) mutationTable.getModel();
            int[] selectedRows = mutationTable.getSelectedRows();
            if (selectedRows == null || selectedRows.length == 0)
                return null;
            List<Integer> rowList = new ArrayList<>();
            for (int i = 0; i < selectedRows.length; i++) {
                int rowInModel = mutationTable.convertRowIndexToModel(selectedRows[i]);
                rowList.add(rowInModel);
            }
            Set<String> residues = model.getResiduesInChains(rowList);
            return String.join(",", residues);
        }
        
    }
    
    private class MutationTableModel extends AbstractTableModel {
        private String[] headers = new String[] {
                "Cancer",
                "Sample",
                "Mutation",
                "PDB",
                "ResidueInPDB"
        };
        private List<List<String>> data;
        
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
                samples.forEach(sample -> {
                    List<String> row = new ArrayList<>();
                    row.add(sample.getCancerType().getAbbreviation());
                    row.add(sample.getName());
                    row.add(variant);
                    row.add(structure.getPdb());
                    row.add(residueStructue);
                    set.add(String.join("\t", row));
                });
            });
            set.forEach(row -> {
                String[] tokens = row.split("\t");
                data.add(Arrays.asList(tokens));
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
        
        public Set<String> getResiduesInChains() {
            List<Integer> rows = IntStream.range(0, getRowCount()).mapToObj(i -> new Integer(i)).collect(Collectors.toList());
            return getResiduesInChains(rows);
        }
        
        public Set<String> getResiduesInChains(List<Integer> rows) {
            Set<String> rtn = new HashSet<>();
            Pattern pattern = Pattern.compile("\\d+");
            rows.forEach(row -> {
                String residueInPDB = (String) getValueAt(row, getColumnCount() - 1);
                // Some parsing
                String[] tokens = residueInPDB.split(":");
                Matcher matcher = pattern.matcher(tokens[1]);
                if (matcher.find()) {
                    String location = matcher.group(0);
                    rtn.add(location + ":" + tokens[0]);
                }
                else // Which should not be possible
                    logger.error("Cannot find PDB residue location for " + residueInPDB);
            });
            return rtn;
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
            List<String> row = data.get(rowIndex);
            return row.get(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
        
        
    }

}
