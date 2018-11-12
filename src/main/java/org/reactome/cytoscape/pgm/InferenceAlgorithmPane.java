/*
 * Created on Jan 26, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.reactome.cytoscape.service.PGMAlgorithmPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.GibbsSampling;
import org.reactome.factorgraph.InferenceType;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.pathway.factorgraph.PathwayPGMConfiguration;

/**
 * A customized JPanel for setting up parameters for inference algorithms.
 * @author gwu
 *
 */
public class InferenceAlgorithmPane extends JPanel {
    private PGMAlgorithmPanel lbpPane;
    private PGMAlgorithmPanel gibbsPane;
    private JComboBox<Inferencer> algBox;
    // For what algorithm should be used
    private JRadioButton defaultAlgBtn;
    private JRadioButton selectedAlgBtn;
    
    /**
     * Default constructor.
     */
    public InferenceAlgorithmPane() {
        init();
    }
    
    /**
     * Get the selected algorithm. The client to this method should check if isOkClicked() returns
     * true. If isOkClicked() returns false, null will be returned to avoid an un-validated 
     * PGMInferenceAlgorithm object.
     * @return
     */
    public List<Inferencer> getSelectedAlgorithms() {
        List<Inferencer> rtn = new ArrayList<Inferencer>();
        if (selectedAlgBtn.isSelected()) {
            Inferencer selected = (Inferencer) algBox.getSelectedItem();
            if (selected instanceof LoopyBeliefPropagation)
                rtn.add(lbpPane.getAlgorithm());
            else if (selected instanceof GibbsSampling)
                rtn.add(gibbsPane.getAlgorithm());
        }
        else {
            rtn.add(lbpPane.getAlgorithm());
            rtn.add(gibbsPane.getAlgorithm());
        }
        return rtn;
    }
    
    private void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        lbpPane = new PGMAlgorithmPanel("Loopy Belief Propagation (LBP)",
                                        getLBP());
        add(lbpPane);
        gibbsPane = new PGMAlgorithmPanel("Gibbs Sampling (Gibbs)",
                                          getGibbs());
        add(gibbsPane);
        add(createAlgorithmSelectionPane());
    }
    
    private GibbsSampling getGibbs() {
        List<Inferencer> algorithms = FactorGraphRegistry.getRegistry().getLoadedAlgorithms();
        GibbsSampling gibbs = null;
        if (algorithms != null) {
            for (Inferencer alg : algorithms) {
                if (alg instanceof GibbsSampling) {
                    gibbs = (GibbsSampling) alg;
                    break;
                }
            }
        }
        if (gibbs == null)
            gibbs = PlugInObjectManager.getManager().getPathwayPGMConfig().getGibbsSampling();
        return gibbs;
    }
    
    private LoopyBeliefPropagation getLBP() {
        List<Inferencer> algorithms = FactorGraphRegistry.getRegistry().getLoadedAlgorithms();
        LoopyBeliefPropagation lbp = null;
        if (algorithms != null) {
            for (Inferencer alg : algorithms) {
                if (alg instanceof LoopyBeliefPropagation) {
                    lbp = (LoopyBeliefPropagation) alg;
                    break;
                }
            }
        }
        if (lbp == null) {
            lbp = PlugInObjectManager.getManager().getPathwayPGMConfig().getLBP();
            // Default to use SUM_PRODUCT
            lbp.setInferenceType(InferenceType.MAX_PRODUCT);
        }
        return lbp;
    }
    
    private JPanel createAlgorithmSelectionPane() {
        JPanel selectionPane = new JPanel();
        selectionPane.setBorder(BorderFactory.createEtchedBorder());
        selectionPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        defaultAlgBtn = new JRadioButton("Use LBP as default. If LBP cannot converge, switch to Gibbs automatically.");
        selectionPane.add(defaultAlgBtn, constraints);
        // Set up the selected algorithm
        constraints.gridy = 1;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        selectedAlgBtn = new JRadioButton("Use the selected algorithm:");
        selectionPane.add(selectedAlgBtn, constraints);
        setupAlgBox();
        constraints.gridx = 1;
        selectionPane.add(algBox, constraints);
        // Make sure only one button can be selected.
        ButtonGroup group = new ButtonGroup();
        group.add(defaultAlgBtn);
        group.add(selectedAlgBtn);
        defaultAlgBtn.setSelected(true); // This should be the default choice
        return selectionPane;
    }
    
    private void setupAlgBox() {
        algBox = new JComboBox<Inferencer>();
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                // Just want to display a simple name for inferrers
                return super.getListCellRendererComponent(list, 
                                                          value.getClass().getSimpleName(),
                                                          index,
                                                          isSelected,
                                                          cellHasFocus);
            }
        };
        algBox.setRenderer(renderer);
        algBox.setEditable(false);
        // There are only two algorithms are supported
        PathwayPGMConfiguration config = PlugInObjectManager.getManager().getPathwayPGMConfig();
        LoopyBeliefPropagation lbp = config.getLBP();
        lbp.setInferenceType(InferenceType.MAX_PRODUCT);
        algBox.addItem(lbp);
        algBox.addItem(config.getGibbsSampling());
        // Choose the first one as the default
        algBox.setSelectedIndex(0); // The first should be LBP
    }
    
    public boolean commitValues() {
        // The following statement return true only when both panels cannot commit
        return (lbpPane.commitValues() && gibbsPane.commitValues());
    }
    
    public void reset() {
        // The first should be LBP
        Inferencer lbp = algBox.getItemAt(0);
        lbpPane.copyProperties(lbp);
        // The second should be Gibbs
        Inferencer gibbs = algBox.getItemAt(1);
        gibbsPane.copyProperties(gibbs);
    }
    
}
