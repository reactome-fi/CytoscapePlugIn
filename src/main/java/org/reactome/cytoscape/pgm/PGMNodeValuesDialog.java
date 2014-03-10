/*
 * Created on Mar 10, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.reactome.pgm.PGMNode;

/**
 * A customized JDialog to show values saved in a PGMNode.
 * @author gwu
 *
 */
public abstract class PGMNodeValuesDialog extends JDialog {
    
    protected JTextArea textLabel;
    
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
    
    protected JTextArea createTextLabel(JPanel labelPane) {
        labelPane.setLayout(new BorderLayout());
        labelPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JTextArea textLabel = new JTextArea();
        textLabel.setBackground(labelPane.getBackground());
        textLabel.setWrapStyleWord(true);
        textLabel.setLineWrap(true);
        textLabel.setEditable(false);
        Font font = textLabel.getFont();
        font = font.deriveFont(Font.BOLD);
        textLabel.setFont(font);
        labelPane.add(textLabel, BorderLayout.CENTER);
        return textLabel;
    }
    
    protected abstract JComponent createContentPane();
    
    /**
     * The client should call this method to set the values for display.
     * @param factor
     */
    public abstract void setPGMNode(PGMNode node);
    
}
