/*
 * Copyright 2025-present Open Networking Foundation
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
package org.test2025.app;

import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
import org.onosproject.net.flow.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.*;
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

    ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowruleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private MyProcessor processor = new MyProcessor();

    @Activate
    protected void activate() {

        log.info("Started application TEST 2025");
        packetService.addProcessor(processor, 10);

        appId = coreService.registerApplication("org.test2025.app");

        requestIPV4();
    }

    @Deactivate
    protected void deactivate() {

        log.info("Stopped application TEST 2025");
        packetService.removeProcessor(processor);
    }

    private class MyProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt.getEtherType() == Ethernet.TYPE_LLDP) {
                //log.info("[---PACKLOGGED---]: from {} ETH_TYPE: LLDP", context.inPacket().receivedFrom());
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
                //log.info("[---PACKLOGGED---]: from {} ETH_TYPE: ARP", context.inPacket().receivedFrom());
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {

                //Retrieving the source host
                HostId srcHostId = HostId.hostId(ethPkt.getSourceMAC());
                Host srcHost = hostService.getHost(srcHostId);

                if (srcHost == null) {
                    log.error("This SOURCE host is not known");
                }

                //Retrieving the source host
                HostId dstHostId = HostId.hostId(ethPkt.getDestinationMAC());
                Host dstHost = hostService.getHost(dstHostId);

                if (dstHost == null) {
                    log.error("This DESTINATION host is not known");
                }

                log.info("[---PACKLOGGED---]: from {} ETH_TYPE: IPv4", context.inPacket().receivedFrom());

                //If we are at the first hop
                DeviceId pckInDeviceId = pkt.receivedFrom().deviceId();
                DeviceId srchostDeviceId = srcHost.location().deviceId();
                if (srchostDeviceId.equals(pckInDeviceId)) {

                    log.warn("[---PACKLOGGED---]: NEW PACKET from host {} source MAC {} destination MAC {}",
                            srcHostId,
                            srcHost.mac(),
                            dstHost.mac());

                    /*Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), srcHost.location().deviceId(), dstHost.location().deviceId());
                    if (paths == null) {
                        log.error("[---PACKLOGGED---]: there are NOT paths");
                    } else {
                        log.warn("[---PACKLOGGED---]: there are {} candidated paths {}", paths.size(), paths);
                    }*/

                    TrafficSelector selector = DefaultTrafficSelector.builder()
                            .matchInPort(pkt.receivedFrom().port())
                            .matchEthSrc(srcHost.mac())
                            .matchEthDst(dstHost.mac())
                            .build();

                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .pushVlan()
                            .setVlanId(VlanId.vlanId("100"))
                            .setOutput(PortNumber.portNumber("3")) //This is only valid for ping h21 -> h41
                            .build();

                    FlowRule flowRule = DefaultFlowRule.builder()
                            .forDevice(srcHost.location().deviceId())
                            .withSelector(selector)
                            .withTreatment(treatment)
                            .withPriority(20)
                            .fromApp(appId)
                            .forTable(0)
                            .makePermanent()
                            .build();

                    flowruleService.applyFlowRules(flowRule);

                    return;
                }

                //If we are at the last hop
                DeviceId dsthostDeviceId = dstHost.location().deviceId();
                if (dsthostDeviceId.equals(pckInDeviceId)) {
                    log.warn("[---PACKLOGGED---]: LAST HOP to host {} is device {}",
                            dstHostId,
                            pckInDeviceId);

                    TrafficSelector selector = DefaultTrafficSelector.builder()
                            .matchInPort(pkt.receivedFrom().port())
                            .matchVlanId(VlanId.vlanId("100"))
                            .matchEthSrc(srcHost.mac())
                            .matchEthDst(dstHost.mac())
                            .build();

                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .popVlan()
                            .setOutput(PortNumber.portNumber("1"))
                            .build();

                    FlowRule flowRule = DefaultFlowRule.builder()
                            .forDevice(dstHost.location().deviceId())
                            .withSelector(selector)
                            .withTreatment(treatment)
                            .withPriority(20)
                            .fromApp(appId)
                            .forTable(0)
                            .makePermanent()
                            .build();

                    flowruleService.applyFlowRules(flowRule);

                    return;
                }
            }
        }
    }

    private void requestIPV4() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

}
