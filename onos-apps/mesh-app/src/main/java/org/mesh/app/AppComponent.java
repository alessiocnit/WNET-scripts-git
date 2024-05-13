/*
 * Copyright 2019-present Open Networking Foundation
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
package org.mesh.app;

import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.topology.TopologyEvent;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    private InternalHostListener hostListener = new InternalHostListener();

    private ApplicationId appId;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.mesh.app");

        hostService.addListener(hostListener);

        log.info("The application {} has been started", appId);
    }

    @Deactivate
    protected void deactivate() {
        hostService.removeListener(hostListener);
        hostListener = null;

        log.info("The application {} has been stopped", appId);
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {

            if (event.type() == HostEvent.Type.HOST_ADDED) {
                log.info("A new host has been HOST_ADDED {}", event.subject().id());

                //Search for other hosts and establish host2host intents
                establishIntents(event.subject().id());
            }

            if (event.type() == HostEvent.Type.HOST_UPDATED) {
                log.info("A new host has been HOST_UPDATED {}", event.subject().id());
            }
        }
    }

    protected void establishIntents(HostId hostId) {
        List<Host> hosts = newArrayList(hostService.getHosts());

        for (int i=0; i < hostService.getHostCount(); i++) {

            if (!hosts.get(i).id().equals(hostId)) {
                //Submit an intent
                HostToHostIntent intent;
                intent = HostToHostIntent.builder()
                        .one(hostId)
                        .two(hosts.get(i).id())
                        .priority(500)
                        .appId(appId)
                        .build();

                log.info("Intent established between {} and {}",
                        hostId,
                        hosts.get(i).id());

                intentService.submit(intent);
            }
        }
    }
}
