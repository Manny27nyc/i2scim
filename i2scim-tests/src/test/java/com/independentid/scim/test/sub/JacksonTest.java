/*
 * Copyright 2021.  Independent Identity Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.independentid.scim.test.sub;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.independentid.scim.schema.Schema;
import com.independentid.scim.serializer.JsonUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ScimSubComponentTestProfile.class)
public class JacksonTest {

    @Test
    public void aTest() {
       ObjectNode node = getNode();
       System.out.println("Basic Node:\n"+node.toPrettyString());
       String nstring = node.toString();
       System.out.println("Compare string:\n"+nstring);

       node.set("objtest",getObj(1));

       System.out.println("Nested obj:\n"+node.toPrettyString());

        node = getNode();
        node.setAll(getObj(2));

        System.out.println("Setall:\n"+node.toPrettyString());

        ArrayNode anode = node.putArray("testArray");
        for (int i=1; i<10; i++) {
            anode.add(getObj(i));
        }

        System.out.println("Array:\n"+node.toPrettyString());
    }

    public ObjectNode getNode() {
        ObjectNode node = JsonUtil.getMapper().createObjectNode();

        node.putArray("schemas").add(Schema.SCHEMA_ID);
        node.put("id","bleh");
        return  node;
    }

    public ObjectNode getObj(int val) {
        ObjectNode node = JsonUtil.getMapper().createObjectNode();
        node.put("attribute",val);
        return node;
    }
}
