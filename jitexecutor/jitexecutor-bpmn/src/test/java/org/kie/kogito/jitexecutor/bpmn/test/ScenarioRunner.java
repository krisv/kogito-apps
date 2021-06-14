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
import java.util.regex.Pattern;

import org.hamcrest.Matcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class ScenarioRunner {

    private String model;
    private Report report;

    public ScenarioRunner(String model) {
        this.model = model;
        report = new Report(model);
    }

    public String[] validate() {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(model)
                .when().post("/jitbpmn/validate");
        response.then()
                .statusCode(200);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(response.getBody().asString(), String[].class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not process response " + response.getBody().asString(), e);
        }
    }

    public void testScenario(Scenario scenario) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(scenario.getPayload())
                .when().post("/jitbpmn");
        response.then()
                .statusCode(200)
                .body(scenario.getStatusMatcher(), scenario.getMatchers().toArray(new Matcher<?>[0]));
        try {
            List<String> nodeInstances = new ArrayList<String>();
            java.util.regex.Matcher m = Pattern.compile("\"nodeDefinitionId\":\"[^\"]+\"")
                    .matcher(response.getBody().asString());
            while (m.find()) {
                String s = m.group();
                nodeInstances.add(s.substring(20, s.length() - 1));
            }
            report.addScenario(nodeInstances);
        } catch (Throwable t) {
            throw new IllegalArgumentException("Cound not parse response " + response.getBody().asString(), t);
        }
    }

    public Report getReport() {
        return report;
    }

}
