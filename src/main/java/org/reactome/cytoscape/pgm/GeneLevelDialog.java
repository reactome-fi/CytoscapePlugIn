/*
 * Created on Feb 13, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.graphEditor.PathwayEditor;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This is a subclass for showing some gene level information in a dialog.
 * @author gwu
 *
 */
public abstract class GeneLevelDialog extends JDialog {
    // For diagram selection
    private GeneToPathwayEntityHandler handler;
    
    /**
     * Default constructor.
     */
    public GeneLevelDialog() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        handler = new GeneToPathwayEntityHandler();
        init();
    }
    
    protected abstract void init();
    
    protected void addTableSelectionListener(final JTable table) {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    handleTableSelection(table);
            }
        });
    }
    
    protected void handleTableSelection(JTable table) {
        handler.handleTableSelection(table, 0);
    }
    
    public void enableDiagramSelection(PathwayEditor editor) {
        handler.enableDiagramSelection(editor);
    }
    
}
