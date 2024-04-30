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
package org.wnet.app;

import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
import org.onosproject.net.flow.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    private ApplicationId appId;

    ReactivePacketProcessor processor = new ReactivePacketProcessor();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(("org.wnet.app"));

        packetService.addProcessor(processor, PacketProcessor.director(2));

        log.info("[WNET 2024] app has started with id {}", appId);
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;

        log.info("[WNET 2024] app has stopped");
    }


    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            InboundPacket pck = context.inPacket();
            Ethernet ethPck = pck.parsed();

            if (ethPck.getEtherType() == Ethernet.TYPE_LLDP) {
                //log.info("[WNET 2024] packet received LLDP from {}", context.inPacket().receivedFrom());

                return;
            }

            if (ethPck.getEtherType() == Ethernet.TYPE_ARP) {
                log.info("[WNET 2024] packet received ARP from {}", context.inPacket().receivedFrom());

                return;
            }

            if (ethPck.getEtherType() == Ethernet.TYPE_IPV4) {

                DeviceId currentDeviceId = pck.receivedFrom().deviceId();

                //Finding out the SRC host
                HostId srcHostId = HostId.hostId(ethPck.getSourceMAC());
                Host srcHost = hostService.getHost(srcHostId);
                DeviceId srcHostLocation = srcHost.location().deviceId();

                //Finding out the DST host --- this may be unkown
                HostId dstHostId = HostId.hostId(ethPck.getDestinationMAC());
                Host dstHost = hostService.getHost(dstHostId);
                DeviceId dstHostLocation = dstHost.location().deviceId();

                log.info("[WNET 2024] packet received IPV4 from {}", context.inPacket().receivedFrom());

                if (currentDeviceId.equals(srcHostLocation)) {
                    log.info("[WNET 2024] packet received IPV4 from {} FISRT HOP !!!", context.inPacket().receivedFrom());

                    //Log warning the path from source to destination
                    Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), srcHostLocation, dstHostLocation);
                    log.warn("[WNET 2024] path is {}", paths.iterator().next());

                    //Install a flow rule in the first switch pushing a VLAN tag
                    TrafficSelector selector = DefaultTrafficSelector.builder()
                            .matchInPort(pck.receivedFrom().port())
                            .matchEthSrc(ethPck.getSourceMAC())
                            .matchEthDst(ethPck.getDestinationMAC())
                            .build();

                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .pushVlan()
                            .setVlanId(VlanId.vlanId("222"))
                            .setOutput(PortNumber.portNumber("3"))
                            .build();

                    FlowRule firstRule = DefaultFlowRule.builder()
                            .withSelector(selector)
                            .withTreatment(treatment)
                            .forDevice(currentDeviceId)
                            .fromApp(appId)
                            .withPriority(20)
                            .forTable(0)
                            .withIdleTimeout(60)
                            .build();

                    flowRuleService.applyFlowRules(firstRule);
                }

                if (currentDeviceId.equals(dstHostLocation)) {
                    log.info("[WNET 2024] packet received IPV4 from {} LAST HOP !!!", context.inPacket().receivedFrom());

                    TrafficSelector selector = DefaultTrafficSelector.builder()
                            .matchVlanId(VlanId.vlanId("222"))
                            .matchInPort(pck.receivedFrom().port())
                            .matchEthSrc(ethPck.getSourceMAC())
                            .matchEthDst(ethPck.getDestinationMAC())
                            .build();

                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .popVlan()
                            .setOutput(PortNumber.portNumber("1"))
                            .build();

                    FlowRule lastRule = DefaultFlowRule.builder()
                            .withSelector(selector)
                            .withTreatment(treatment)
                            .forDevice(currentDeviceId)
                            .fromApp(appId)
                            .withPriority(20)
                            .forTable(0)
                            .withIdleTimeout(60)
                            .build();

                    flowRuleService.applyFlowRules(lastRule);
                }

                return;
            }

            return;
        }
    }

}


