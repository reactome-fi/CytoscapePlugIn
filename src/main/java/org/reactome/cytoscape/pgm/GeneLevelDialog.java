/*
 * Created on Feb 13, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.graphEditor.SelectionMediator;
import org.reactome.cytoscape.service.GeneLevelSelectionHandler;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This is a subclass for showing some gene level information in a dialog.
 * @author gwu
 *
 */
public abstract class GeneLevelDialog extends JDialog {
    // For diagram selection
    protected GeneLevelSelectionHandler selectionHandler;
    
    /**
     * Default constructor.
     */
    public GeneLevelDialog() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        selectionHandler = createSelectionHandler();
        final SelectionMediator mediator = PlugInObjectManager.getManager().getObservationVarSelectionMediator();
        mediator.addSelectable(selectionHandler);
        addWindowFocusListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                mediator.getSelectables().remove(selectionHandler);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                mediator.getSelectables().remove(selectionHandler);
            }
            
        });
        init();
    }
    
    protected abstract GeneLevelSelectionHandler createSelectionHandler();
    
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
        selectionHandler.setGeneLevelTable(table);
        PlugInObjectManager.getManager().getObservationVarSelectionMediator().fireSelectionEvent(selectionHandler);
    }
}
