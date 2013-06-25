/*
 * Created on Jul 12, 2012
 *
 */
package org.reactome.CS.x.internal;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This customized JPanel is used to select a FI network version. Several FI network versions
 * may be provided in the plug-in so that results based on old network can be reproduced still.
 * @author gwu
 *
 */
public class FIVersionSelectionPanel extends JPanel {
    private List<JRadioButton> buttons;
    
    public FIVersionSelectionPanel() {
        init();
    }
    
    private void init() {
        // Use two panels
        setLayout(new BorderLayout());
        JPanel choicePane = new JPanel();
        choicePane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        add(choicePane, BorderLayout.CENTER);
        
        String versions = PlugInScopeObjectManager.getManager().getProperties().getProperty("FINetworkVersions");
        String[] tokens = versions.split(",");
        // Create JRadioButtons based on tokens
        ButtonGroup group = new ButtonGroup();
        int index = 0;
        buttons = new ArrayList<JRadioButton>();
        for (String token : tokens) {
            JRadioButton button = new JRadioButton(token);
            buttons.add(button);
            group.add(button);
            constraints.gridx = index;
            index++;
            choicePane.add(button, constraints);
        }
        
        // Add a note at the bottom
        JLabel noteLabel = new JLabel("* You may get different results by using different version of the FI network.");
        Font font = noteLabel.getFont();
        noteLabel.setFont(font.deriveFont(Font.ITALIC));
        add(noteLabel, BorderLayout.SOUTH);
        
        addChangeListener();
        // Choose the first version of FI network as the default: it should be 2009
        JRadioButton btn = buttons.get(0);
        btn.setSelected(true);
    }
    
    private void addChangeListener() {
        ChangeListener listner = new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                // Get the selected button
                for (JRadioButton btn : buttons) {
                    if (btn.isSelected()) {
                        PlugInScopeObjectManager.getManager().setFiNetworkVersion(btn.getText());
                        break;
                    }
                }
            }
        };
        for (JRadioButton btn : buttons)
            btn.addChangeListener(listner);
    }
    
    /**
     * Get a text lable for this customized JPanel.
     * @return
     */
    public String getTitle() {
        return "Selecting FI Network Version";
    }
    
}
