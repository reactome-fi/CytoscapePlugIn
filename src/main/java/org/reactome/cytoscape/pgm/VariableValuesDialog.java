/*
 * Created on Mar 10, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
    private JLabel textLabel;
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
        
        textLabel = new JLabel("");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5d;
        constraints.gridwidth = 2;
//        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;
        contentPane.add(textLabel, constraints);
        
        constraints.gridwidth = 1;
        state0Value = addStateLabels(contentPane, constraints, 0);
        state1Value = addStateLabels(contentPane, constraints, 1);
        state2Value = addStateLabels(contentPane, constraints, 2);
        
        return contentPane;
    }  
    
    @Override
    public void setPGMNode(PGMNode variable) {
        if (!(variable instanceof PGMVariable))
            return;
        List<Double> values = variable.getValues();
        if (values == null || values.size() == 0) {
            textLabel.setText("<html><center><b><u>Unknown marginal probabilities for variable \"" + variable.getLabel() + "\".</u></b></center></html>");
            return;
        }
        textLabel.setText("<html><center><b><u>Marginal Probabilities for Variable \"" + variable.getLabel() + "\"</u></b></center></html>");
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
