package org.reactome.cytoscape.service;

import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;

public class ReactomeFIVizPropsReader extends AbstractConfigDirPropsReader {
    
    public ReactomeFIVizPropsReader(String name, String fileName) {
        super(name, fileName, CyProperty.SavePolicy.CONFIG_DIR);
    }

}
