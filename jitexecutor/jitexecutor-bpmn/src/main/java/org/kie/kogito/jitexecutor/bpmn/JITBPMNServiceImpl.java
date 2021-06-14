/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.jitexecutor.bpmn;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.kie.kogito.jitexecutor.bpmn.requests.Interaction;
import org.kie.kogito.jitexecutor.bpmn.responses.BPMNResultWithAudit;

@ApplicationScoped
public class JITBPMNServiceImpl implements JITBPMNService {

    public JITBPMNServiceImpl() {
    }

    @Override
    public List<String> validateProcess(String modelXML) {
        BPMNEvaluator bpmnEvaluator = BPMNEvaluator.fromXML(modelXML);
        return bpmnEvaluator.validate();
    }

    // @Override
    // public Map<String, Object> startProcess(String modelXML, Map<String, Object> parameters) {
    //     return startProcess(modelXML, parameters, null);
    // }

    @Override
    public BPMNResultWithAudit startProcessWithAudit(String modelXML, Map<String, Object> parameters) {
        return startProcessWithAudit(modelXML, parameters, null);
    }

    // @Override
    // public Map<String, Object> startProcess(String modelXML, Map<String, Object> parameters, List<Interaction> interactions) {
    //     BPMNEvaluator bpmnEvaluator = BPMNEvaluator.fromXML(modelXML);
    //     Map<String, Object> result = bpmnEvaluator.startProcess(parameters, interactions);
    //     return result;
    // }

    @Override
    public BPMNResultWithAudit startProcessWithAudit(String modelXML, Map<String, Object> parameters, List<Interaction> interactions) {
        BPMNEvaluator bpmnEvaluator = BPMNEvaluator.fromXML(modelXML);
        BPMNResultWithAudit bpmnResult = bpmnEvaluator.startProcessWithAudit(parameters, interactions);
        return bpmnResult;
    }
}
