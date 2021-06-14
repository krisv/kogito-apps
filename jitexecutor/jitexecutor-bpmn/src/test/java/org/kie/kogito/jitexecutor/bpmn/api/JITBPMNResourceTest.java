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

package org.kie.kogito.jitexecutor.bpmn.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.drools.core.util.IoUtils;
import org.junit.jupiter.api.Test;
import org.kie.kogito.jitexecutor.bpmn.test.Scenario;
import org.kie.kogito.jitexecutor.bpmn.test.ScenarioRunner;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class JITBPMNResourceTest {

    @Test
    public void testjitEndpointScript() throws IOException {
        String model = new String(IoUtils.readBytesFromInputStream(JITBPMNResourceTest.class.getResourceAsStream("/script.bpmn")));
        ScenarioRunner runner = new ScenarioRunner(model);
        Scenario scenario = new Scenario(model);
        scenario
                .startProcess(Map.of("x", "one", "y", "two"))
                .checkNodeCompleted("Start")
                .checkNodeCompleted("Script")
                .checkNodeCompleted("End")
                .processCompleted()
                .checkOutput("z", "oneASDF");
        runner.testScenario(scenario);
        // System.out.println(runner.getReport());
        // System.out.println(runner.getReport().getNodeDetails());
    }

    @Test
    public void testjitEndpointUserTask() throws IOException {
        String model = new String(IoUtils.readBytesFromInputStream(JITBPMNResourceTest.class.getResourceAsStream("/usertask.bpmn")));
        ScenarioRunner runner = new ScenarioRunner(model);
        Scenario scenario = new Scenario(model);
        scenario
                .startProcess(new HashMap<String, Object>())
                .checkNodeCompleted("Start")
                .completeWork("MyTask", Map.of("y", "output"))
                .checkNodeCompleted("MyTask")
                .checkNodeCompleted("End")
                .processCompleted()
                .checkOutput("y", "output");
        runner.testScenario(scenario);
        // System.out.println(runner.getReport());
        // System.out.println(runner.getReport().getNodeDetails());
    }

    @Test
    public void testjitEndpointError() throws IOException {
        String model = new String(IoUtils.readBytesFromInputStream(JITBPMNResourceTest.class.getResourceAsStream("/script_error.bpmn")));
        ScenarioRunner runner = new ScenarioRunner(model);
        Scenario scenario = new Scenario(model);
        scenario
                .startProcess(new HashMap<String, Object>())
                .checkNodeCompleted("Start")
                .processError();
        runner.testScenario(scenario);
    }

    @Test
    public void testjitEndpointValidate() throws IOException {
        String model = new String(IoUtils.readBytesFromInputStream(JITBPMNResourceTest.class.getResourceAsStream("/script_incomplete.bpmn")));
        ScenarioRunner runner = new ScenarioRunner(model);
        String[] errors = runner.validate();
        assertEquals(2, errors.length);

        String model2 = new String(IoUtils.readBytesFromInputStream(JITBPMNResourceTest.class.getResourceAsStream("/usertask.bpmn")));
        runner = new ScenarioRunner(model2);
        errors = runner.validate();
        assertEquals(0, errors.length);

    }

}
