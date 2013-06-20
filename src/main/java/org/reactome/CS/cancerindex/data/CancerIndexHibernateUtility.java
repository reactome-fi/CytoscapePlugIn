/*
 * Created on Jul 14, 2010
 *
 */
package org.reactome.CS.cancerindex.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.reactome.r3.util.FileUtility;
import org.reactome.r3.util.HibernateUtil;

public class CancerIndexHibernateUtility {
    
    public CancerIndexHibernateUtility() {
    }
    
    public SessionFactory initSession() throws Exception {
        String configFileName = "resources/cancerindex.hibernate.cfg.xml";
        File configFile = new File(configFileName);
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory(configFile);
        return sessionFactory;
    }
    
    @Test
    public void testConfig() throws Exception {
        initSession();
    }
    
    @Test
    public void generateJavaConstantsFromDTD() throws IOException {
        String srcFileName = "datasets/NCI_CancerIndex_allphases_disease/CancerIndex_disease_XML.dtd";
        FileUtility fu = new FileUtility();
        fu.setInput(srcFileName);
        String line = null;
        Set<String> names = new HashSet<String>();
        while ((line = fu.readLine()) != null) {
            if (line.startsWith("<!ELEMENT")) {
                int index = line.indexOf(" ");
                int index1 = line.indexOf(" ", index + 1);
                String name = line.substring(index + 1, index1);
                names.add(name);
            }
        }
        List<String> nameList = new ArrayList<String>(names);
        Collections.sort(nameList);
        fu.close();
        String target = "src/org/reactome/cancerindex/model/CancerIndexConstants.java";
        fu.setOutput(target);
        fu.printLine("package org.reactome.CS.cancerindex.model;");
        fu.printLine("");
        fu.printLine("public class CancerIndexConstants {");
        for (String name : nameList) {
            fu.printLine("    public static final String " + name + " = \"" + name + "\"; ");
        }
        fu.printLine("}");
        fu.close();
    }
}
