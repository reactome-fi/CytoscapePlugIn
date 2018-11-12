package org.reactome.cytoscape.service;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.gk.util.DialogControlPane;

/**
 * Customized JDialog to paste a set of genes.
 * @author gwu
 *
 */
public abstract class GeneSetEnterDialog extends JDialog {
    private boolean isOKClicked = false;
    private DialogControlPane controlPane;
    private JTextArea geneTA;
    private JTextField fileTF;
    
    public GeneSetEnterDialog(JDialog owner, JTextField fileTF) {
        super(owner);
        this.fileTF = fileTF;
        init();
    }
    
    public boolean isOKClicked() {
        return isOKClicked;
    }

    public DialogControlPane getControlPane() {
        return controlPane;
    }

    public JTextArea getGeneTA() {
        return geneTA;
    }

    private void init() {
        setTitle("Gene Set Input");
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), 
                                                                 BorderFactory.createEtchedBorder()));
        contentPane.setLayout(new BorderLayout());
        JLabel label = new JLabel("Enter or paste genes below (one line per gene):");
        contentPane.add(label, BorderLayout.NORTH);
        geneTA = new JTextArea();
        contentPane.add(new JScrollPane(geneTA), BorderLayout.CENTER);
        controlPane = new DialogControlPane();
        contentPane.add(controlPane, BorderLayout.SOUTH);
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                isOKClicked = true;
            }
        });
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                isOKClicked = false;
            }
        });
        controlPane.getOKBtn().setEnabled(false);
        
        synchronizeGUIs();
        
        getContentPane().add(contentPane, BorderLayout.CENTER);
        
        setLocationRelativeTo(getOwner());
        setSize(360, 325);
    }
    
    /**
     * Make sure all GUIs are in a consistent states.
     */
    private void synchronizeGUIs() {
        geneTA.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                controlPane.getOKBtn().setEnabled(true);
                controlPane.getOKBtn().setEnabled(true);
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                controlPane.getOKBtn().setEnabled(true);
                controlPane.getOKBtn().setEnabled(true);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        
        geneTA.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doGeneTAPopup(e.getPoint());
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doGeneTAPopup(e.getPoint());
            }
            
        });
        
        // Related to the container GUIs. Adding these actions here makes
        // this dialog tied with this class only.
        fileTF.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (fileTF.getText().trim().length() > 0) {
                    resetEnterGeneGUIs();
                }
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (fileTF.getText().trim().length() > 0) {
                    resetEnterGeneGUIs();
                }
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        
    }
    
    protected abstract void resetEnterGeneGUIs();
    
    private void doGeneTAPopup(Point position) {
        JPopupMenu popup = new JPopupMenu();
        Action paste = new AbstractAction("Paste") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                geneTA.paste();
            }
        };
        popup.add(paste);
        Action selectAll = new AbstractAction("Select All") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                geneTA.selectAll();
            }
        };
        popup.add(selectAll);
        popup.show(geneTA, position.x, position.y);
    }
   
}