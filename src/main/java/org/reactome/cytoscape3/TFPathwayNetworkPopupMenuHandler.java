package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.ServiceProperties;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.bn.BooleanNetworkMainPane;
import org.reactome.cytoscape.bn.NetworkBNMainPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

public class TFPathwayNetworkPopupMenuHandler extends FINetworkPopupMenuHandler {

	@Override
	protected void installMenus() {
		BundleContext context = PlugInObjectManager.getManager().getBundleContext();

		Properties props = new Properties();
		props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
		addPopupMenu(context, 
				new FuzzyLogicSimulationMenu(), 
				CyNetworkViewContextMenuFactory.class, 
				props);
	}

	private void performFuzzyLogicSimulation(CyNetworkView view) {
		// Step 1: Convert the displayed network into a Boolean network
		NetworkBNMainPane mainPane = (NetworkBNMainPane) PlugInUtilities.getCytoPanelComponent(NetworkBNMainPane.class,
				CytoPanelName.EAST,
				BooleanNetworkMainPane.TITLE);
		mainPane.setNetworkView(view);
		mainPane.createNewSimulation();
	}

	private class FuzzyLogicSimulationMenu implements CyNetworkViewContextMenuFactory {

		@Override
		public CyMenuItem createMenuItem(final CyNetworkView view) {
			JMenuItem menuItem = new JMenuItem("Run Logic Model Analysis");
			menuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Thread t = new Thread() {
						public void run() {
							performFuzzyLogicSimulation(view);
						}
					};
					t.start();
				}
			});
			return new CyMenuItem(menuItem, 2.0f);
		}
	}

}
