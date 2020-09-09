package org.reactome.cytoscape.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReactomeFIVizPropsReader extends AbstractConfigDirPropsReader {
    private final Logger logger = LoggerFactory.getLogger(ReactomeFIVizPropsReader.class.getName());
    
    public ReactomeFIVizPropsReader(String name, String fileName) {
        super(name, fileName, CyProperty.SavePolicy.CONFIG_DIR);
        loadDefault(fileName);
    }
    
    /**
     * It seems that there is an issue to load the default properties from the jar file in the supclass. 
     * Re-load the default properties using this method.
     * @param fileName
     */
    private void loadDefault(String propFileName) {
        InputStream is = null;
        try {
            // Hard-coded the file name to the resources folder. This should work
            // for ReactomeFIViz only.
            String fileName = "/resources/" + propFileName;
            is = getClass().getClassLoader().getResourceAsStream(fileName);
            if (is != null ) {
                Properties prop = getProperties();
                Properties defaultProp = new Properties();
                defaultProp.load(is);
                for (Object key : defaultProp.keySet()) {
                    if (prop.containsKey(key))
                        continue;
                    prop.put(key, defaultProp.get(key));
                }
            }
            else
                logger.warn("couldn't find resource '" + propFileName + "' in jar.");
        }
        catch(IOException e) {
            logger.error(e.getMessage(), e);
        }
        finally {
            if (is != null) {
                try { is.close(); } catch (IOException ioe) {}
            }
        }
    }

}
