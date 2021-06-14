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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.util.IoUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.kogito.jitexecutor.bpmn.requests.CompleteWork;
import org.kie.kogito.jitexecutor.bpmn.requests.Interaction;
import org.kie.kogito.jitexecutor.bpmn.responses.BPMNResultWithAudit;

public class JITBPMNServiceImplTest {

    private static JITBPMNService jitbpmnService;

    @BeforeAll
    public static void setup() {
        jitbpmnService = new JITBPMNServiceImpl();
    }

    @Test
    public void testModelEvaluationWithAudit() throws IOException {
        String model = new String(IoUtils.readBytesFromInputStream(JITBPMNServiceImplTest.class.getResourceAsStream("/script.bpmn")));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "one");
        parameters.put("y", "two");
        BPMNResultWithAudit bpmnResult = jitbpmnService.startProcessWithAudit(model, parameters);
        Assertions.assertEquals(0, bpmnResult.getErrorMessages().size());
        Assertions.assertEquals(1, bpmnResult.bpmnResult.size());
        Assertions.assertEquals("oneASDF", bpmnResult.bpmnResult.get("z"));
    }

    @Test
    public void testModelEvaluationWithAuditAndInteractions() throws IOException {
        String userTaskModel = new String(IoUtils.readBytesFromInputStream(JITBPMNServiceImplTest.class.getResourceAsStream("/usertask.bpmn")));
        Map<String, Object> parameters = new HashMap<>();
        BPMNResultWithAudit bpmnResult = jitbpmnService.startProcessWithAudit(userTaskModel, parameters);
        Assertions.assertEquals(1, bpmnResult.bpmnResult.size());
        Assertions.assertNull(bpmnResult.bpmnResult.get("y"));

        List<Interaction> interactions = new ArrayList<Interaction>();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("y", "output");
        interactions.add(new CompleteWork("MyTask", data));
        bpmnResult = jitbpmnService.startProcessWithAudit(userTaskModel, parameters, interactions);
        Assertions.assertEquals(0, bpmnResult.getErrorMessages().size());
        Assertions.assertEquals(1, bpmnResult.bpmnResult.size());
        Assertions.assertEquals("output", bpmnResult.bpmnResult.get("y"));
    }

    @Test
    public void testValidate() throws IOException {
        String model = new String(IoUtils.readBytesFromInputStream(JITBPMNServiceImplTest.class.getResourceAsStream("/script_error.bpmn")));
        Map<String, Object> parameters = new HashMap<>();
        BPMNResultWithAudit bpmnResult = jitbpmnService.startProcessWithAudit(model, parameters);
        Assertions.assertEquals(1, bpmnResult.getErrorMessages().size());

        String model2 = new String(IoUtils.readBytesFromInputStream(JITBPMNServiceImplTest.class.getResourceAsStream("/script_incomplete.bpmn")));
        List<String> errorMessages = jitbpmnService.validateProcess(model2);
        Assertions.assertEquals(2, errorMessages.size());
    }

}
