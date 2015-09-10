/*
 * Created on Sep 9, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.common.DataType;
import org.reactome.factorgraph.common.EmpiricalFactorHandler;

/**
 * A customized MutationEmpiricalFactorHandler mainly fetches the MA scores via the
 * URL to create an EmpiricalDistribution.
 * @author gwu
 *
 */
public class CyMutationEmpiricalFactorHandler extends EmpiricalFactorHandler {
    
    /**
     * Default constructor.
     */
    public CyMutationEmpiricalFactorHandler() {
    }

    /**
     * Override this method to fetch the MA scores via URL.
     */
    @Override
    public EmpiricalDistribution getDistribution(DataType dataType) {
        EmpiricalDistribution distribution = super.getDistribution(DataType.Mutation);
        if (distribution.isLoaded())
            return distribution;
        // Want to fetch a pre-generated MA scores from the server.
        String hostURL = PlugInObjectManager.getManager().getHostURL();
        String fileUrl = hostURL + "Cytoscape/MA_AllScores_10_MA_hg19.txt.zip";
        try {
            URL url = new URL(fileUrl);
            InputStream is = url.openStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ZipInputStream zis = new ZipInputStream(bis);
            ZipEntry entry = zis.getNextEntry(); // Have to call this method to get the zipped file
            BufferedReader br = new BufferedReader(new InputStreamReader(zis));
            List<Double> scores = new ArrayList<Double>();
            String line = null;
            while ((line = br.readLine()) != null)
                scores.add(new Double(line));
            br.close();
            is.close();
            double[] scoreArray = new double[scores.size()];
            for (int i = 0; i < scores.size(); i++)
                scoreArray[i] = scores.get(i);
            distribution.load(scoreArray);
            return distribution;
        }
        catch(Exception e) { // Convert thrown Exceptions into an unchecked exception for super classes.
            throw new IllegalStateException("Exception during loading MA scores: " + e);
        }
    }
    
}
