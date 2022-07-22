/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.marklogic.processor;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecuteScriptMarkLogicIT extends AbstractMarkLogicIT {

    @BeforeEach
    public void beforeEach() {
        super.setup();
    }

    @Test
    public void simpleJS() {
        TestRunner runner = super.getNewTestRunner(ExecuteScriptMarkLogic.class);
        runner.setValidateExpressionUsage(false);

        runner.setProperty(ExecuteScriptMarkLogic.EXECUTION_TYPE, ExecuteScriptMarkLogic.AV_JAVASCRIPT);
        runner.setProperty(ExecuteScriptMarkLogic.RESULTS_DESTINATION, ExecuteScriptMarkLogic.AV_CONTENT);
        runner.setProperty(ExecuteScriptMarkLogic.SCRIPT_BODY, "1 + 1");
        runner.setProperty(ExecuteScriptMarkLogic.SKIP_FIRST, "false");


        MockFlowFile mockFlowFile = new MockFlowFile(3);
        Map<String, String> attributes = new HashMap<>();
        mockFlowFile.putAttributes(attributes);

        runner.enqueue(mockFlowFile);
        runner.run(1);

        runner.assertQueueEmpty();
        assertEquals(0, runner.getFlowFilesForRelationship(ExecuteScriptMarkLogic.FAILURE).size());
        assertEquals(1, runner.getFlowFilesForRelationship(ExecuteScriptMarkLogic.RESULTS).size());

        List<MockFlowFile> results = runner.getFlowFilesForRelationship(ExecuteScriptMarkLogic.RESULTS);
        assertEquals(1, results.size());

        MockFlowFile result = results.get(0);
        String resultValue = new String(runner.getContentAsByteArray(result));
        assertEquals("2", resultValue, "The script is expected to return the value 2");
    }

    @Test
    public void jsWithError() {
        TestRunner runner = super.getNewTestRunner(ExecuteScriptMarkLogic.class);
        runner.setValidateExpressionUsage(false);

        runner.setProperty(ExecuteScriptMarkLogic.EXECUTION_TYPE, ExecuteScriptMarkLogic.AV_JAVASCRIPT);
        runner.setProperty(ExecuteScriptMarkLogic.RESULTS_DESTINATION, ExecuteScriptMarkLogic.AV_CONTENT);
        // The following script should cause a null access error. The processor should then route the flowfile
        // to the FAILURE relationship. 
        runner.setProperty(ExecuteScriptMarkLogic.SCRIPT_BODY, "const foo = {}; foo.bar.stuff");
        runner.setProperty(ExecuteScriptMarkLogic.SKIP_FIRST, "false");


        MockFlowFile mockFlowFile = new MockFlowFile(3);
        Map<String, String> attributes = new HashMap<>();
        mockFlowFile.putAttributes(attributes);

        runner.enqueue(mockFlowFile);
        runner.run(1);

        runner.assertQueueEmpty();
        assertEquals(1, runner.getFlowFilesForRelationship(ExecuteScriptMarkLogic.ERROR).size());
        assertEquals(0, runner.getFlowFilesForRelationship(ExecuteScriptMarkLogic.FAILURE).size());
        assertEquals(0, runner.getFlowFilesForRelationship(ExecuteScriptMarkLogic.RESULTS).size());
    }

}
