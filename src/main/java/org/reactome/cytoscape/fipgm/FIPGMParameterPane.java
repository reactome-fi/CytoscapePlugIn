/*
 * Created on Sep 10, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.reactome.cytoscape.pgm.FactorGraphRegistry;
import org.reactome.cytoscape.service.PGMAlgorithmPanel;
import org.reactome.factorgraph.InferenceType;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.fi.pgm.FIPGMConstructor.PGMType;
import org.reactome.pathway.factorgraph.PathwayPGMConfiguration;
/**
 * Customized JPanel for users to set up parameters for the FI PGM.
 * @author gwu
 *
 */
public class FIPGMParameterPane extends JPanel {
    private PGMAlgorithmPanel lbpPane;
    private JComboBox<PGMType> typeBox;
    
    /**
     * Default constructor.
     */
    public FIPGMParameterPane() {
        init();
    }
    
    /**
     * Make sure all changed parameters can be used in the wrapped inference algorithm.
     * @return
     */
    public boolean commit() {
        return lbpPane.commitValues();
    }
    
    /**
     * Get the configured LoopyBeliefPropgation object.
     * @return
     */
    public LoopyBeliefPropagation getLBP() {
        Inferencer alg = lbpPane.getAlgorithm();
        return (LoopyBeliefPropagation) alg; // This should be LBP based on the configuration in the class.
    }
    
    /**
     * Get the selected PGMType.
     * @return
     */
    public PGMType getPGMType() {
        return (PGMType) typeBox.getSelectedItem();
    }
    
    private void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel modelPane = createModelPane();
        add(modelPane);
        // The following two weird statments are used to initialize LBP.
        // This should be changed in the future
        // Note: FactorGraphRegistry is in package org.reactome.pathway.factorgraph.
        FactorGraphRegistry.getRegistry(); 
        // Call this method for initialization 
        PathwayPGMConfiguration config = PathwayPGMConfiguration.getConfig();
        LoopyBeliefPropagation lbp = config.getLBP();
        lbp.setInferenceType(InferenceType.MAX_PRODUCT); // Use this as default
        lbpPane = new PGMAlgorithmPanel("Inference Parameters",
                                                          lbp);
        add(lbpPane);
    }
    
    private JPanel createModelPane() {
        JPanel modelPane = new JPanel();
        modelPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                             "Graphic Model Type"));
        modelPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        JLabel label = new JLabel("Choose a model: ");
        modelPane.add(label, constraints);
        
        // For the time being, we will just support two models
        PGMType[] types = new PGMType[2];
        types[0] = PGMType.PairwiseMRF;
        types[1] = PGMType.NearestNeighborGibbs;
        typeBox = new JComboBox<PGMType>(types);
        modelPane.add(typeBox, constraints);
        return modelPane;
    }
    
}
