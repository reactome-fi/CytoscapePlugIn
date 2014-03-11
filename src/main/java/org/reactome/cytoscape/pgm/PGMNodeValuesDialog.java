/*
 * Created on Mar 10, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.reactome.pgm.PGMNode;

/**
 * A customized JDialog to show values saved in a PGMNode.
 * @author gwu
 *
 */
public abstract class PGMNodeValuesDialog extends JDialog {
    /**
     * @param owner
     */
    public PGMNodeValuesDialog(Frame owner) {
        super(owner);
        init();
    }
    
    protected void init() {
        JComponent contentPane = createContentPane();
        getContentPane().add(contentPane, BorderLayout.CENTER);
        
        JPanel controlPane = new JPanel();
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        controlPane.add(closeBtn);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
    }
    
    protected abstract JComponent createContentPane();
    
    /**
     * The client should call this method to set the values for display.
     * @param factor
     */
    public abstract void setPGMNode(PGMNode node);
    
}
