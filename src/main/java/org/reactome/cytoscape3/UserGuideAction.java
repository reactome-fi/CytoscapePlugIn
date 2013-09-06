package org.reactome.cytoscape3;

/**
 * This class manages the menu action performed
 * when the user clicks User Guide. Currently it
 * links to the online user guide, but all of the
 * pieces are in place to create an offline user
 * guide except for the actual user guide document
 * itself.
 * @author Eric T Dawson
 */
import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.reactome.cytoscape.util.PlugInUtilities;

public class UserGuideAction extends AbstractCyAction
{
    public UserGuideAction()
    {
        // Add the 'User Guide' item to the ReactomeFI menu
        super("User Guide");
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(3.0f);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Pop open a browser window pointing to the
        // online Reactome FI app user guide.
        String url = PlugInScopeObjectManager.getManager().getProperties()
                .getProperty("userGuideURL");
        if (url == null)
        {
            PlugInUtilities
                    .showErrorMessage("Error in Showing User Guide",
                            "The user guide URL is not available. No user guide can be shown.");
            return;
        }
        PlugInUtilities.openURL(url);
    }

}
