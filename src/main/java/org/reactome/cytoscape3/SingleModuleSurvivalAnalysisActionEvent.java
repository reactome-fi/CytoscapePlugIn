/*
 * Created on Apr 19, 2011
 *
 */
package org.reactome.cytoscape3;

import java.util.EventObject;

/**
 * @author wgm
 *
 */
public class SingleModuleSurvivalAnalysisActionEvent extends EventObject {
    private String module;
    
    public SingleModuleSurvivalAnalysisActionEvent(Object src) {
        super(src);
    }
    
    public void setModule(String module) {
        this.module = module;
    }
    
    public String getModule() {
        return this.module;
    }
}
