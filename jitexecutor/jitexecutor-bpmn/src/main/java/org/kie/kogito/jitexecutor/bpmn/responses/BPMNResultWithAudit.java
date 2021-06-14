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

package org.kie.kogito.jitexecutor.bpmn.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kie.kogito.services.event.ProcessInstanceDataEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BPMNResultWithAudit {

    @JsonProperty("bpmnResult")
    public Map<String, Object> bpmnResult;
    @JsonProperty("errorMessages")
    private List<String> errorMessages;
    @JsonProperty("audit")
    public ProcessInstanceDataEvent audit;

    public BPMNResultWithAudit() {
    }

    public BPMNResultWithAudit(Map<String, Object> bpmnResult, ProcessInstanceDataEvent audit, List<String> errorMessages) {
        this.bpmnResult = bpmnResult;
        this.audit = audit;
        this.errorMessages = errorMessages;
    }

    public BPMNResultWithAudit(Map<String, Object> bpmnResult, ProcessInstanceDataEvent audit) {
        this(bpmnResult, audit, new ArrayList<String>());
    }

    public BPMNResultWithAudit(List<String> validationMessages) {
        this.errorMessages = validationMessages;
    }

    public Map<String, Object> getResult() {
        return bpmnResult;
    }

    public void setResult(Map<String, Object> bpmnResult) {
        this.bpmnResult = bpmnResult;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public ProcessInstanceDataEvent getAudit() {
        return audit;
    }

    public void setAudit(ProcessInstanceDataEvent audit) {
        this.audit = audit;
    }
}
