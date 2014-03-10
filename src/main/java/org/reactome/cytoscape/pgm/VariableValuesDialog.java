/*
 * Created on Mar 10, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.reactome.pgm.PGMNode;
import org.reactome.pgm.PGMVariable;

/**
 * A customized JDialog for showing values (aka marginal properties) of a variable.
 * @author gwu
 *
 */
public class VariableValuesDialog extends PGMNodeValuesDialog {
    private JLabel state0Value;
    private JLabel state1Value;
    private JLabel state2Value;
    
    /**
     * @param owner
     */
    public VariableValuesDialog(Frame owner) {
        super(owner);
        setTitle("Variable Marginals");
    }
    
    @Override
    protected JComponent createContentPane() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        
        JPanel labelPane = new JPanel();
        textLabel = createTextLabel(labelPane);
        GridBagConstraints constraints0 = new GridBagConstraints();
        constraints0.gridx = 0;
        constraints0.gridy = 0;
        constraints0.weightx = 0.5d;
        constraints0.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(labelPane, constraints0);
        
        JPanel valuePane = new JPanel();
        valuePane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        state0Value = addStateLabels(valuePane, constraints, 0);
        state1Value = addStateLabels(valuePane, constraints, 1);
        state2Value = addStateLabels(valuePane, constraints, 2);
        constraints0.gridy = 1;
        contentPane.add(valuePane, constraints0);
        
        return contentPane;
    }  
    
    @Override
    public void setPGMNode(PGMNode variable) {
        if (!(variable instanceof PGMVariable))
            return;
        List<Double> values = variable.getValues();
        if (values == null || values.size() == 0) {
            textLabel.setText("Unknown marginal probabilities for variable \"" + variable.getLabel() + "\".");
            return;
        }
        textLabel.setText("Marginal Probabilities for variable \"" + variable.getLabel() + "\":");
        state0Value.setText(formatProbability(values.get(0)));
        if (values.size() > 1)
            state1Value.setText(formatProbability(values.get(1)));
        if (values.size() > 2)
            state2Value.setText(formatProbability(values.get(2)));  
    }
    
    private String formatProbability(double value) {
        return String.format("%1.2E", value);
    }
    
    private JLabel addStateLabels(JPanel contentPane,
                                  GridBagConstraints constraints,
                                  int state) {
        JLabel label = new JLabel("State " + state + ": ");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        constraints.gridy ++;
        constraints.gridx = 0;
        contentPane.add(label, constraints);
        JLabel valueLabel = new JLabel("");
        constraints.gridx = 1;
        contentPane.add(valueLabel, constraints);
        return valueLabel;
    }
    
}
