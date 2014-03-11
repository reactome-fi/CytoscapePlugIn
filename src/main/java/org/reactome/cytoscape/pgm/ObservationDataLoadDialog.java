/*
 * Created on Mar 10, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;

/**
 * This customized JDialog is used to load observation data for a factor graph.
 * @author gwu
 *
 */
public class ObservationDataLoadDialog extends FIActionDialog {
    
    /**
     * Constructor.
     */
    public ObservationDataLoadDialog() {
    }
    
    
    
    @Override
    protected JPanel createInnerPanel(FIVersionSelectionPanel versionPanel,
                                      Font font) {
        // There is no need for a FI version
        versionPanel.setVisible(false);
        
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        
        JPanel dnaPane = new JPanel();
        Border titleBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                              "DNA Data",
                                                              TitledBorder.LEFT,
                                                              TitledBorder.CENTER,
                                                              font);
        dnaPane.setBorder(titleBorder);
        contentPane.add(dnaPane);
        
        JPanel geneExpressionPane = new JPanel();
        titleBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                       "Gene Expression",
                                                       TitledBorder.LEFT,
                                                       TitledBorder.CENTER,
                                                       font);
        geneExpressionPane.setBorder(titleBorder);
        contentPane.add(geneExpressionPane);

        return contentPane;
    }

    @Override
    protected String getTabTitle() {
        return "Load Observation Data";
    }

}
