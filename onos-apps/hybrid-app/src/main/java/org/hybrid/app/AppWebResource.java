/*
 * Copyright 2024-present Open Networking Foundation
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
package org.hybrid.app;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import java.util.Iterator;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Sample web resource.
 */
@Path("sample")
public class AppWebResource extends AbstractWebResource {

    /**
     * Get hello world greeting.
     *
     * @return 200 OK
     */
    @GET
    @Path("")
    public Response getGreeting() {
        HostService hostService = get(HostService.class);
        IntentService intentService = get(IntentService.class);

        Iterator iterIntents = intentService.getIntents().iterator();

        ArrayNode arrayNode = mapper().createArrayNode();

        while (iterIntents.hasNext()) {
            Intent intent = (Intent) iterIntents.next();

            ObjectNode node = mapper().createObjectNode();
            if (intent instanceof HostToHostIntent) {

                        node.put("id:", intent.id().toString());
                        node.put("src", ((HostToHostIntent) intent).one().toString());
                        node.put("dst", ((HostToHostIntent) intent).two().toString());
            }

            arrayNode.add(node);
        }

        ObjectNode root = mapper().createObjectNode().putPOJO("Intents", arrayNode);

        return ok(root).build();
    }

    /**
     * Get hello world greeting.
     *
     * @return 200 OK
     */
    @DELETE
    @Path("intents")
    public Response deleteIntent(@PathParam("intentId") String id) {
        IntentService intentService = get(IntentService.class);

        Iterator iterIntents = intentService.getIntents().iterator();

        while (iterIntents.hasNext()) {
            Intent intent = (Intent) iterIntents.next();

            if (intent.id().equals(id)) {

                intentService.withdraw(intent);
            }
        }


        return Response.accepted().build();
    }

}
