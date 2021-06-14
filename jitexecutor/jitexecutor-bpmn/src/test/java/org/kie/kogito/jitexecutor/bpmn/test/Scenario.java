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

package org.kie.kogito.jitexecutor.bpmn.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.kie.kogito.jitexecutor.bpmn.requests.CompleteWork;
import org.kie.kogito.jitexecutor.bpmn.requests.JITBPMNPayload;

import static org.hamcrest.CoreMatchers.containsString;

public class Scenario {

    private String model;
    private Map<String, Object> data;
    private List<CompleteWork> interactions;
    private List<Matcher<String>> checks = new ArrayList<Matcher<String>>();
    private long status = 1;

    public Scenario(String model) {
        this.model = model;
    }

    public Scenario startProcess(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public Scenario completeWork(String nodeName, Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Need to call startProcess before completing work");
        }
        if (interactions == null) {
            interactions = new ArrayList<CompleteWork>();
        }
        interactions.add(new CompleteWork(nodeName, data));
        addNodeCompletedCheck(nodeName);
        return this;
    }

    public Scenario checkNodeCompleted(String nodeName) {
        addNodeCompletedCheck(nodeName);
        return this;
    }

    public Scenario processActive() {
        this.status = 1;
        return this;
    }

    public Scenario processCompleted() {
        this.status = 2;
        return this;
    }

    public Scenario processAborted() {
        this.status = 3;
        return this;
    }

    public Scenario processError() {
        this.status = 5;
        return this;
    }

    public Scenario checkOutput(String variableName, String value) {
        checks.add(containsString("\"" + variableName + "\":\"" + value + "\""));
        return this;
    }

    private void addNodeCompletedCheck(String nodeName) {
        checks.add(containsString("\"nodeName\":\"" + nodeName + "\""));
    }

    public JITBPMNPayload getPayload() {
        return new JITBPMNPayload(model, data, interactions);
    }

    public List<Matcher<String>> getMatchers() {
        return checks;
    }

    public Matcher<String> getStatusMatcher() {
        return containsString("\"state\":" + status);
    }
}
