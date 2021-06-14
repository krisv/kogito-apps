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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.io.impl.ByteArrayResource;
import org.jbpm.compiler.canonical.ActionNodeVisitor;
import org.jbpm.compiler.canonical.VariableDeclarations;
import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.core.validation.ProcessValidationError;
import org.jbpm.process.instance.LightProcessRuntime;
import org.jbpm.process.instance.impl.Action;
import org.jbpm.ruleflow.core.validation.RuleFlowProcessValidator;
import org.jbpm.workflow.core.impl.DroolsConsequenceAction;
import org.jbpm.workflow.core.node.ActionNode;
import org.jbpm.workflow.core.node.CompositeContextNode;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.NodeContainer;
import org.kie.api.definition.process.Process;
import org.kie.api.definition.process.WorkflowProcess;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.runtime.process.ProcessContext;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.kogito.Addons;
import org.kie.kogito.event.DataEvent;
import org.kie.kogito.internal.process.runtime.KogitoNodeInstance;
import org.kie.kogito.jitexecutor.bpmn.requests.CompleteWork;
import org.kie.kogito.jitexecutor.bpmn.requests.Interaction;
import org.kie.kogito.jitexecutor.bpmn.responses.BPMNResultWithAudit;
import org.kie.kogito.process.bpmn2.BpmnProcess;
import org.kie.kogito.services.event.ProcessInstanceDataEvent;
import org.kie.kogito.services.event.impl.ProcessInstanceEventBatch;
import org.mvel2.ErrorDetail;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

public class BPMNEvaluator {

    private final LightProcessRuntime bpmnRuntime;
    private String processId;
    private List<String> errorMessages = new ArrayList<String>();

    public static BPMNEvaluator fromXML(String modelXML) {
        return new BPMNEvaluator(modelXML);
    }

    private BPMNEvaluator(String modelXML) {
        BpmnProcess process = BpmnProcess.from(new ByteArrayResource(modelXML.getBytes())).get(0);
        this.processId = process.process().getId();
        for (ProcessValidationError e : RuleFlowProcessValidator.getInstance().validateProcess(process.process())) {
            errorMessages.add(e.toString());
        }
        Process p = compileProcess(process.process());
        this.bpmnRuntime = LightProcessRuntime.ofProcess(p);
    }

    public List<String> validate() {
        return errorMessages;
    }

    // public Map<String, Object> startProcess(Map<String, Object> parameters, List<Interaction> interactions) {
    //     WorkflowProcessInstance processInstance = (WorkflowProcessInstance) bpmnRuntime.startProcess(processId, parameters);
    //     triggerInteractions(processInstance, interactions);
    //     return getOutputVariables(processInstance);
    // }

    public BPMNResultWithAudit startProcessWithAudit(Map<String, Object> parameters, List<Interaction> interactions) {
        if (!errorMessages.isEmpty()) {
            return new BPMNResultWithAudit(errorMessages);
        }
        AuditProcessEventListener processListener = new AuditProcessEventListener();
        bpmnRuntime.addEventListener(processListener);
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance) bpmnRuntime.startProcess(processId, parameters);
        triggerInteractions(processInstance, interactions);
        String errorMessage = ((WorkflowProcessInstanceImpl) processInstance).getErrorMessage();
        if (errorMessage != null) {
            errorMessages.add(errorMessage);
        }
        bpmnRuntime.removeEventListener(processListener);
        Collection<DataEvent<?>> auditEvents = processListener.getAudit();
        ProcessInstanceDataEvent auditEvent = null;
        if (auditEvents.size() == 1) {
            DataEvent<?> event = auditEvents.iterator().next();
            if (event instanceof ProcessInstanceDataEvent) {
                auditEvent = (ProcessInstanceDataEvent) event;
            } else {
                throw new IllegalArgumentException("Unexpected data event " + auditEvent);
            }
        }
        if (!errorMessages.isEmpty()) {
            return new BPMNResultWithAudit(null, auditEvent, errorMessages);
        }
        if (auditEvents.size() != 1) {
            throw new IllegalArgumentException("Unexpected number of data events " + auditEvents.size());
        }
        return new BPMNResultWithAudit(getOutputVariables(processInstance), (ProcessInstanceDataEvent) auditEvent);
    }

    private void triggerInteractions(WorkflowProcessInstance processInstance, List<Interaction> interactions) {
        if (interactions != null) {
            for (Interaction interaction : interactions) {
                triggerInteraction(processInstance, interaction);
            }
        }
    }

    private void triggerInteraction(WorkflowProcessInstance processInstance, Interaction interaction) {
        if (interaction instanceof CompleteWork) {
            CompleteWork completeWork = (CompleteWork) interaction;
            String workItemId = findWorkItem(processInstance, completeWork.getNodeName());
            bpmnRuntime.getKogitoWorkItemManager().completeWorkItem(workItemId, completeWork.getData());
        } else {
            throw new IllegalArgumentException("Unknown interaction " + interaction);
        }
    }

    private String findWorkItem(WorkflowProcessInstance processInstance, String nodeName) {
        if (nodeName == null || nodeName.trim().length() == 0) {
            throw new IllegalArgumentException("NodeName cannot be null or empty " + nodeName);
        }
        Collection<KogitoNodeInstance> nodeInstances = processInstance.getKogitoNodeInstances(n -> nodeName.equals(n.getNodeName()), true);
        if (nodeInstances.size() == 0) {
            throw new IllegalArgumentException("Could not find node instance for node name " + nodeName);
        }
        if (nodeInstances.size() > 1) {
            throw new IllegalArgumentException("Found multiple node instance for node name " + nodeName + ": " + nodeInstances.size());
        }
        KogitoNodeInstance nodeInstance = nodeInstances.iterator().next();
        if (nodeInstance instanceof WorkItemNodeInstance) {
            return ((WorkItemNodeInstance) nodeInstance).getWorkItemId();
        } else {
            throw new IllegalArgumentException("Found unexpected node instance (not a WorkItemNodeInstance): " + nodeInstance);
        }
    }

    private Map<String, Object> getOutputVariables(ProcessInstance processInstance) {
        VariableDeclarations varDecl = VariableDeclarations.ofOutput(
                (VariableScope) ((org.jbpm.process.core.Process) processInstance.getProcess()).getDefaultContext(VariableScope.VARIABLE_SCOPE));
        Map<String, Object> results = new HashMap<String, Object>();
        Map<String, Object> vars = ((WorkflowProcessInstance) processInstance).getVariables();
        for (String varName : varDecl.getTypes().keySet()) {
            results.put(varName, vars.get(varName));
        }
        return results;
    }

    private Process compileProcess(Process process) {
        // ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process);
        // System.out.println(metaData.getGeneratedClassModel());
        VariableScope variableScope = (VariableScope) ((org.jbpm.process.core.Process) process).getDefaultContext(VariableScope.VARIABLE_SCOPE);
        compileNodes(((WorkflowProcess) process).getNodes(), variableScope);
        return process;
    }

    private void compileNodes(Node[] nodes, VariableScope variableScope) {
        for (Node node : nodes) {
            if (node instanceof ActionNode) {
                ActionNode actionNode = (ActionNode) node;
                if (actionNode.getAction() instanceof DroolsConsequenceAction) {
                    DroolsConsequenceAction action = (DroolsConsequenceAction) actionNode.getAction();
                    String dialect = action.getDialect();
                    if ("java".equals(dialect) || "mvel".equals(dialect)) {
                        String consequence = action.getConsequence();
                        final StringBuilder expression = new StringBuilder();
                        List<Variable> variables = variableScope.getVariables();
                        variables.stream()
                                .filter(v -> consequence.contains(v.getName()))
                                .map(ActionNodeVisitor::makeAssignment)
                                .forEach(x -> expression.append(x));
                        expression.append(consequence);
                        Serializable s = compileExpression(expression.toString());
                        action.setMetaData("Action", (Action) kcontext -> {
                            Map<String, Object> vars = new HashMap<String, Object>();
                            vars.put("kcontext", kcontext);
                            MVEL.executeExpression(s, vars);
                        });
                    } else {
                        throw new IllegalArgumentException("Unsupported dialect found: " + dialect);
                    }
                }
            } else if (node instanceof NodeContainer) {
                NodeContainer nodeContainer = (NodeContainer) node;
                VariableScope scope = variableScope;
                if (node instanceof CompositeContextNode) {
                    if (((CompositeContextNode) node).getDefaultContext(VariableScope.VARIABLE_SCOPE) != null
                            && !((VariableScope) ((CompositeContextNode) node).getDefaultContext(VariableScope.VARIABLE_SCOPE)).getVariables().isEmpty()) {
                        scope = (VariableScope) ((CompositeContextNode) node).getDefaultContext(VariableScope.VARIABLE_SCOPE);
                    }
                }
                compileNodes(nodeContainer.getNodes(), scope);
            }
        }
    }

    private Serializable compileExpression(String expression) {
        ParserContext context = new ParserContext();
        context.addInput("kcontext", ProcessContext.class);
        Serializable result = MVEL.compileExpression(expression, context);
        for (ErrorDetail detail : context.getErrorList()) {
            this.errorMessages.add(detail.toString());
        }
        return result;
    }

    public class AuditProcessEventListener implements ProcessEventListener {

        private ProcessInstanceEventBatch batch = new ProcessInstanceEventBatch("BPMNJITEvaluator", Addons.EMTPY);

        @Override
        public void beforeProcessStarted(ProcessStartedEvent event) {
            batch.append(event);
        }

        @Override
        public void afterProcessStarted(ProcessStartedEvent event) {
            batch.append(event);
        }

        @Override
        public void beforeProcessCompleted(ProcessCompletedEvent event) {
            batch.append(event);
        }

        @Override
        public void afterProcessCompleted(ProcessCompletedEvent event) {
            batch.append(event);
        }

        @Override
        public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
            batch.append(event);
        }

        @Override
        public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
            batch.append(event);
        }

        @Override
        public void beforeNodeLeft(ProcessNodeLeftEvent event) {
            batch.append(event);
        }

        @Override
        public void afterNodeLeft(ProcessNodeLeftEvent event) {
            batch.append(event);
        }

        @Override
        public void beforeVariableChanged(ProcessVariableChangedEvent event) {
            batch.append(event);
        }

        @Override
        public void afterVariableChanged(ProcessVariableChangedEvent event) {
            batch.append(event);
        }

        public Collection<DataEvent<?>> getAudit() {
            return batch.events();
        }

    }

}
