package org.reactome.cytoscape.sc;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This customized JDialog is used to choose a cell or specific clusters for root inference.
 * @author wug
 *
 */
public class CellRootSelectionDialog extends JDialog {
    private boolean isOkCliked;
    private JTextField rootTF;
    private JTextField clusterTF;

    public CellRootSelectionDialog(JFrame parent) {
        super(parent);
        init();
    }
    
    public CellRootSelectionDialog() {
        this(PlugInObjectManager.getManager().getCytoscapeDesktop());
    }
    
    public String getRootCell() {
        String text = rootTF.getText().trim();
        if (text.length() == 0)
            return null;
        return text;
    }
    
    public List<String> getClusters() {
        String text = clusterTF.getText().trim();
        if (text.length() == 0)
            return null;
        return Stream.of(text.split(",")).map(t -> t.trim()).collect(Collectors.toList());
    }
    
    public boolean isOkClicked() {
        return this.isOkCliked;
    }
    
    private void init() {
        setTitle("Choose Cell Root");
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel("Provide the id of a cell as the root: ");
        rootTF = new JTextField();
        rootTF.setColumns(12);
        constraints.gridx = 0;
        constraints.gridy = 0;
        contentPane.add(label, constraints);
        constraints.gridx = 1;
        contentPane.add(rootTF, constraints);
        label = new JLabel("Or enter clusters to infer a root: ");
        clusterTF = new JTextField();
        clusterTF.setColumns(12);
        constraints.gridy ++;
        constraints.gridx = 0;
        contentPane.add(label, constraints);
        constraints.gridx = 1;
        contentPane.add(clusterTF, constraints);
        // Add a note
        JTextArea noteTF = new JTextArea();
        Font font = label.getFont();
        noteTF.setFont(font.deriveFont(Font.ITALIC, font.getSize() - 1));
        noteTF.setEditable(false);
        noteTF.setBackground(getBackground());
        noteTF.setWrapStyleWord(true);
        noteTF.setLineWrap(true);
        noteTF.setText("Note: You may enter more than one cluster by delimiting them with \",\". "
                + "To use all clusters, enter \"all\". If you enter information for both text boxes, "
                + "the information in the cell root box will be used.");
        constraints.gridy ++;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        contentPane.add(noteTF, constraints);
        getContentPane().add(contentPane, BorderLayout.CENTER);
        
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getOKBtn().addActionListener(e -> {
            isOkCliked = true;
            dispose();
        });
        controlPane.getCancelBtn().addActionListener(e -> dispose());
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        setSize(490, 235);
        setModal(true);
        setLocationRelativeTo(getOwner());
    }
    
    public static void main(String[] args) {
        CellRootSelectionDialog dialog = new CellRootSelectionDialog(null);
        dialog.setVisible(true);
    }
    
}
