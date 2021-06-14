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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.drools.core.io.impl.ByteArrayResource;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.NodeContainer;
import org.kie.api.definition.process.WorkflowProcess;
import org.kie.kogito.process.bpmn2.BpmnProcess;

public class Report {

    private String model;
    private String processName;
    private String processId;
    private Map<String, Node> nodes;

    private int scenarios = 0;
    private Map<String, Integer> nodeCounters = new HashMap<String, Integer>();

    public Report(String model) {
        this.model = model;
    }

    public void addScenario(List<String> nodeInstances) {
        scenarios++;
        for (String n : nodeInstances) {
            Integer counter = nodeCounters.get(n);
            if (counter == null) {
                counter = 0;
            }
            nodeCounters.put(n, counter + 1);
        }
    }

    public String toString() {
        return getSummary();
    }

    public String getSummary() {
        if (nodes == null) {
            analyzeNodes();
        }
        Long coverage = nodes.keySet().stream().filter(n -> nodeCounters.containsKey(n)).count() * 100 / nodes.size();
        return "Report for process: " + processName + " [" + processId + "]" + "\n" +
                "Number of scenarios run: " + scenarios + "\n" +
                "Number of node instances triggered " + nodeCounters.values().stream().mapToInt(i -> i).sum() + "\n" +
                "Coverage: " + coverage + "%";
    }

    public String getNodeDetails() {
        String s = "";
        for (Entry<String, Integer> entry : nodeCounters.entrySet()) {
            if (s.length() != 0) {
                s += "\n";
            }
            String nodeId = entry.getKey();
            Node node = nodes.get(nodeId);
            s += node.getName() + " [" + node.getMetaData().get("UniqueId") + "]: " + entry.getValue();
        }
        for (Entry<String, Node> entry : nodes.entrySet()) {
            if (s.length() != 0) {
                s += "\n";
            }
            if (!nodeCounters.containsKey(entry.getKey())) {
                s += entry.getValue().getName() + " [" + entry.getValue().getMetaData().get("UniqueId") + "]: 0";
            }
        }
        return s;
    }

    private void analyzeNodes() {
        nodes = new HashMap<String, Node>();
        BpmnProcess process = BpmnProcess.from(new ByteArrayResource(model.getBytes())).get(0);
        this.processName = process.process().getName();
        this.processId = process.process().getId();
        analyzeNodes(((WorkflowProcess) process.process()).getNodes());
    }

    private void analyzeNodes(Node[] nodes) {
        for (Node node : nodes) {
            this.nodes.put((String) node.getMetaData().get("UniqueId"), node);
            if (node instanceof NodeContainer) {
                NodeContainer nodeContainer = (NodeContainer) node;
                analyzeNodes(nodeContainer.getNodes());
            }
        }
    }

    public Map<String, Integer> getNodeCounters() {
        return nodeCounters;
    }

}
