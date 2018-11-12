/*
 * Created on Sep 15, 2015
 *
 */
package org.reactome.cytoscape.util;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.gk.util.DialogControlPane;

/**
 * @author gwu
 *
 */
public class MessageDialog extends JDialog {
    
    private JTextArea ta;
    
    public MessageDialog(Window parent) {
        super(parent);
        init();
    }
    
    private void init() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        Border outer = BorderFactory.createEtchedBorder();
        Border in = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        contentPane.setBorder(BorderFactory.createCompoundBorder(outer, in));
        ta = new JTextArea();
        ta.setLineWrap(true);
        ta.setEditable(false);
        ta.setWrapStyleWord(true);
        contentPane.add(new JScrollPane(ta), BorderLayout.CENTER);
        
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getCancelBtn().setVisible(false);
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageDialog.this.dispose();
            }
        });
        getContentPane().add(contentPane, BorderLayout.CENTER);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
    }
    
    public void setText(String text) {
        ta.setText(text);
    }
    
}
