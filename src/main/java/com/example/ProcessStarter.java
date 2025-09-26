package com.example;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import java.util.Map;
public class ProcessStarter {

    private final KieContainer kieContainer;
    // Name of the ksession declared in kmodule.xml (or default session)
    private final String ksessionName;

    public ProcessStarter(String ksessionName) {
        this.ksessionName = ksessionName;
        KieServices ks = KieServices.Factory.get();
        this.kieContainer = ks.getKieClasspathContainer();
    }

    /**
     * Start a process by id with parameters.
     * Returns process instance id.
     */
    public long startProcess(String processId, Map<String, Object> params) {
        KieSession ksession = null;
        try {
           
            if (ksessionName == null || ksessionName.isEmpty()) {
                ksession = kieContainer.newKieSession();
            } else {
                ksession = kieContainer.newKieSession(ksessionName);
            }
            // start process
            org.kie.api.runtime.process.ProcessInstance pi = ksession.startProcess(processId, params);
            return pi.getId();
        } finally {
            if (ksession != null) {
                try {
                    ksession.dispose();
                } catch (Exception e) {
                    
                }
            }
        }
    }
}

