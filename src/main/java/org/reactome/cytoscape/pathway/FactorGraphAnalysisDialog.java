/*
 * Created on Jan 26, 2015
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.pgm.FactorGraphRegistry;
import org.reactome.cytoscape.pgm.InferenceAlgorithmPane;
import org.reactome.cytoscape.pgm.ObservationDataLoadPanel;
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;

/**
 * A customized JDialog for asking the user to enter parameters needed for factor graph based
 * pathway analysis.
 * @author gwu
 *
 */
public class FactorGraphAnalysisDialog extends FIActionDialog {
    private ObservationDataLoadPanel dataPane;
    private DialogControlPane controlPane;
    // A reset button for inference
    private JButton resetBtn;
    private InferenceAlgorithmPane algPane;
    
    /**
     * Default constructor.
     */
    public FactorGraphAnalysisDialog() {
        init();
    }
    
    public ObservationDataLoadPanel getDataLoadPane() {
        return dataPane;
    }
    
    public InferenceAlgorithmPane getAlgorithmPane() {
        return this.algPane;
    }
    
    private void init() {
        setTitle("Run Graphical Model Analysis");
        final JTabbedPane tabbedPane = new JTabbedPane();
        dataPane = new ObservationDataLoadPanel() {
            
            @Override
            protected void createFileChooserGui(JTextField fileTF,
                                                JLabel fileChooseLabel,
                                                JButton browseButton, JPanel loadPanel,
                                                GridBagConstraints constraints) {
                FactorGraphAnalysisDialog.this.createFileChooserGui(fileTF,
                                                                    fileChooseLabel,
                                                                    browseButton,
                                                                    loadPanel,
                                                                    constraints);
            }
        };
        tabbedPane.add("Specify Observation Data", dataPane);
        
        algPane = new InferenceAlgorithmPane();
        tabbedPane.add("Set up Inference Algorithms", algPane);
        
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        controlPane = new DialogControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        okBtn = controlPane.getOKBtn();
        // If the data has been loaded before, we should enable the OKbtn
        // Default should be disabled.
        okBtn.setEnabled(FactorGraphRegistry.getRegistry().isDataLoaded()); 
        okBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                doOKAction();
            }
        });
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        resetBtn = new JButton("Reset");
        resetBtn.setPreferredSize(okBtn.getPreferredSize());
        resetBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                algPane.reset();
            }
        });
        controlPane.add(resetBtn);
        resetBtn.setEnabled(false);
        tabbedPane.getModel().addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                resetBtn.setEnabled(tabbedPane.getSelectedComponent() == algPane);
            }
        });
    }

    @Override
    protected JPanel createInnerPanel(FIVersionSelectionPanel versionPanel,
                                      Font font) {
        return null;
    }

    @Override
    protected String getTabTitle() {
        return null;
    }
    
    @Override
    protected void doOKAction() {
        if (!dataPane.validateValues() || !algPane.commitValues())
            return;
        super.doOKAction();
    }
}
