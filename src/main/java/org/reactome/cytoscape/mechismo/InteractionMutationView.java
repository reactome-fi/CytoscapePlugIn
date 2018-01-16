package org.reactome.cytoscape.mechismo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

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
            getFrame().setTitle(title);
        }
    }
    
    private void displayStructure(String pdb) {
        try {
            Structure struc = StructureIO.getStructure(pdb);
            setStructure(struc);
            // send some commands to Jmol
            evalString("select * ; color chain;");            
            evalString("select *; spacefill off; wireframe off; cartoon on;  ");
            evalString("select ligands; cartoon off; wireframe 0.3; spacefill 0.5; color cpk;");
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
        evalString("select " + residueInChain + "; color white; spacefill; zoom out");
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
        frame.setLocationRelativeTo(PlugInObjectManager.getManager().getCytoscapeDesktop());
        frame.setSize(700, 800);
        
        Component jmolPane = getJMolPane(frame);
        if (jmolPane == null)
            throw new IllegalStateException("Cannot find jMol pane!");
        
        Container container = frame.getContentPane();
        container.remove(jmolPane);
        
        mutationPane = new MutationPane();
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jmolPane, mutationPane);
        jsp.setDividerLocation(550); // Assign a more space for the structure part
        
        container.add(jsp, BorderLayout.CENTER);
    }
    
    private Component getJMolPane(JFrame frame) {
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
        
        public MutationPane() {
            init();
        }
        
        private void init() {
            setLayout(new BorderLayout());
            titleLabel = new JLabel("Mutations:");
            add(titleLabel, BorderLayout.NORTH);
            mutationTable = new JTable();
            MutationTableModel model = new MutationTableModel();
            mutationTable.setModel(model);
            mutationTable.setAutoCreateRowSorter(true);
            add(new JScrollPane(mutationTable), BorderLayout.CENTER);
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
            Set<String> rtn = new HashSet<>();
            Pattern pattern = Pattern.compile("\\d+");
            for (int i = 0; i < getRowCount(); i++) {
                String residueInPDB = (String) getValueAt(i, getColumnCount() - 1);
                // Some parsing
                String[] tokens = residueInPDB.split(":");
                Matcher matcher = pattern.matcher(tokens[1]);
                if (matcher.find()) {
                    String location = matcher.group(0);
                    rtn.add(location + ":" + tokens[0]);
                }
                else // Which should not be possible
                    logger.error("Cannot find PDB residue location for " + residueInPDB);
            }
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
