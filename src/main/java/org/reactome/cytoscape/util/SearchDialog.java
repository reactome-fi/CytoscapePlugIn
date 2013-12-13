/*
 * Created on Dec 13, 2013
 *
 */
package org.reactome.cytoscape.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.gk.util.DialogControlPane;

/**
 * @author gwu
 *
 */
public class SearchDialog extends JDialog {
    
    private boolean isOkClicked;
    private JTextField textField;
    private JButton okBtn;
    private JButton cancelBtn;
    private JCheckBox wholeNameBox;
    private JLabel searchLabel;
    
    public SearchDialog(JFrame parent) {
        super(parent);
        init();
    }
    
    public boolean isOKClicked() {
        return this.isOkClicked;
    }
    
    /**
     * Search the text for the label (e.g. Search diseases)
     * @param text
     */
    public void setLabel(String text) {
        this.searchLabel.setText(text);
    }
    
    public String getSearchKey() {
        return textField.getText().trim();
    }
    
    public boolean isWholeNameNeeded() {
        return wholeNameBox.isSelected();
    }
    
    private void init() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        searchLabel = new JLabel("Search objects:");
        contentPane.add(searchLabel, constraints);
        textField = new JTextField();
        constraints.gridy = 1;
        textField.setPreferredSize(new Dimension(250, 25));
        contentPane.add(textField, constraints);
        wholeNameBox = new JCheckBox("Match whole name only");
        constraints.gridy = 2;
        contentPane.add(wholeNameBox, constraints);
        DialogControlPane controlBox = new DialogControlPane();
        okBtn = controlBox.getOKBtn();
        okBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(okBtn);
        cancelBtn = controlBox.getCancelBtn();
        getContentPane().add(contentPane, BorderLayout.CENTER);
        getContentPane().add(controlBox, BorderLayout.SOUTH);
        setSize(370, 220);
        setLocationRelativeTo(getOwner());
        installListeners();
    }
    
    private void installListeners() {
        okBtn.setEnabled(false);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent e) {
                validateOkBtn();
            }
            
            public void insertUpdate(DocumentEvent e) {
                validateOkBtn();
            }
            
            public void changedUpdate(DocumentEvent e) {
                validateOkBtn();
            }
        });
        
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isOkClicked = true;
                dispose();
            }
        });
        
        cancelBtn.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
    
    private void validateOkBtn() {
        String text = textField.getText().trim();
        okBtn.setEnabled(text.length() > 0);
    }
    
}
