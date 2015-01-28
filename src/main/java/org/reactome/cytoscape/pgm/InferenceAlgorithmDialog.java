/*
 * Created on Apr 19, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.reactome.factorgraph.Inferencer;

/**
 * This customized JDialog is used to set up an inference algorithm for factor graphs.
 * @author gwu
 *
 */
public class InferenceAlgorithmDialog extends JDialog {
    private boolean isOkClicked;
    private InferenceAlgorithmPane algPane;
    
    public InferenceAlgorithmDialog(JFrame parentFrame) {
        super(parentFrame);
        init();
    }
    
    public boolean isOkClicked() {
        return this.isOkClicked;
    }
    
    /**
     * Get the selected algorithm. The client to this method should check if isOkClicked() returns
     * true. If isOkClicked() returns false, null will be returned to avoid an un-validated 
     * PGMInferenceAlgorithm object.
     * @return
     */
    public List<Inferencer> getSelectedAlgorithms() {
        return algPane.getSelectedAlgorithms();
    }
    
    private void init() {
        setTitle("Inference Algorithm Configuration");
        algPane = new InferenceAlgorithmPane();
        JPanel controlPane = createControlPane();
        
        getContentPane().add(algPane, BorderLayout.CENTER);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
    }
    
    private JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!algPane.commitValues())
                    return;
                isOkClicked = true;
                dispose();
            }
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isOkClicked = false;
                dispose();
            }
        });
        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                algPane.reset();
            }
        });

        controlPane.add(okBtn);
        controlPane.add(cancelBtn);
        controlPane.add(resetBtn);
        
        // Set okBtn as the default
        okBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(okBtn);
        
        return controlPane;
    }
}
