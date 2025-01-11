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


import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
import org.onosproject.net.flow.*;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.packet.*;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

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
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    private InternalHostListener hostListener = new InternalHostListener();

    private ApplicationId appId;

    ReactivePacketProcessor processor = new ReactivePacketProcessor();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(("org.hybrid.app"));

        packetService.addProcessor(processor, PacketProcessor.director(2));
        hostService.addListener(hostListener);

        installIpv4FlowRules();

        log.info("[WNET 2024] app has started with id {}", appId);
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;

        hostService.removeListener(hostListener);
        hostListener = null;

        log.info("[WNET 2024] app has stopped");
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {

            if (event.type() == HostEvent.Type.HOST_ADDED) {
                log.info("A new host has been HOST_ADDED {}", event.subject().id());

                //Search for other hosts and establish host2host intents
                establishIntents(event.subject().id());
            }

            if (event.type() == HostEvent.Type.HOST_REMOVED) {
                log.info("An host has been REMOVED {}", event.subject().id());
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

                //
                if ((srcHost == null) || ((dstHost == null))) {
                    log.error("[WNET 2024] hosts not known");
                    return;
                }

                log.info("[WNET 2024] packet received IPV4 from {}", context.inPacket().receivedFrom());
                log.info("--- [WNET 2024] SRC host {} switch {}", srcHostId, srcHostLocation);
                log.info("--- [WNET 2024] DST host {} switch {}", dstHostId, dstHostLocation);

                if (currentDeviceId.equals(srcHostLocation)) {
                    log.info("[WNET 2024] packet received IPV4 from {} FISRT HOP !!!", context.inPacket().receivedFrom());

                    HostToHostIntent intent = HostToHostIntent.builder()
                            .appId(appId)
                            .one(srcHost.id())
                            .two(dstHost.id())
                            .build();

                    intentService.submit(intent);

                    return;

                    /*
                    //Source and destination are connected on the same device
                    if (srcHostLocation == dstHostLocation) {
                        log.warn("[WNET 2024] hosts connected on same device {}", srcHostLocation);

                        //Install a flow rule in the first switch forward direction
                        TrafficSelector selector = DefaultTrafficSelector.builder()
                                .matchInPort(srcHost.location().port())
                                .matchEthSrc(ethPck.getSourceMAC())
                                .matchEthDst(ethPck.getDestinationMAC())
                                .build();

                        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                .setOutput(dstHost.location().port())
                                .build();

                        FlowRule firstRule = DefaultFlowRule.builder()
                                .withSelector(selector)
                                .withTreatment(treatment)
                                .forDevice(currentDeviceId)
                                .fromApp(appId)
                                .withPriority(20)
                                .forTable(0)
                                .withIdleTimeout(10)
                                .build();

                        flowRuleService.applyFlowRules(firstRule);
                        log.warn("[WNET 2024] installed forward rule");

                        IPv4 ipv4Packet = (IPv4) ethPck.getPayload();
                        Ip4Prefix matchIp4SrcPrefix =
                                Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),
                                        Ip4Prefix.MAX_MASK_LENGTH);
                        Ip4Prefix matchIp4DstPrefix =
                                Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),
                                        Ip4Prefix.MAX_MASK_LENGTH);

                        //Install a flow rule in the first switch backward direction
                        TrafficSelector selectorBack = DefaultTrafficSelector.builder()
                                .matchInPort(dstHost.location().port())
                                .matchEthSrc(ethPck.getDestinationMAC())
                                .matchEthDst(ethPck.getSourceMAC())
                                .matchEthType(Ethernet.TYPE_IPV4)
                                .matchIPSrc(matchIp4DstPrefix)
                                .matchIPDst(matchIp4SrcPrefix)
                                .build();

                        TrafficTreatment treatmentBack = DefaultTrafficTreatment.builder()
                                .setOutput(srcHost.location().port())
                                .build();

                        FlowRule backwardRule = DefaultFlowRule.builder()
                                .withSelector(selectorBack)
                                .withTreatment(treatmentBack)
                                .forDevice(currentDeviceId)
                                .fromApp(appId)
                                .withPriority(20)
                                .forTable(0)
                                .withIdleTimeout(10)
                                .build();

                        flowRuleService.applyFlowRules(backwardRule);
                        log.warn("[WNET 2024] installed backward rule");

                        packetOut(context, dstHost.location().port());

                        return;
                    }

                    log.warn("[WNET 2024] hosts are connected on remote devices {}-{}",
                            srcHostLocation,
                            dstHostLocation);

                    //Log warning the path from source to destination
                    Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), srcHostLocation, dstHostLocation);

                    if (paths.isEmpty()) {
                        log.error("There is not a path between the two hosts");

                        return;
                    }

                    Path path = paths.iterator().next();

                    log.warn("[WNET 2024] path is {}", path);

                    for (int i=0; i < path.links().size(); i++) {
                        //This is the first link
                        if (i == 0) {
                            TrafficSelector selector = DefaultTrafficSelector.builder()
                                    .matchInPort(srcHost.location().port())
                                    .matchEthSrc(srcHost.mac())
                                    .matchEthDst(dstHost.mac())
                                    .build();

                            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                    .setOutput(path.links().get(0).src().port())
                                    .build();

                            FlowRule firstRule = DefaultFlowRule.builder()
                                    .withSelector(selector)
                                    .withTreatment(treatment)
                                    .forDevice(path.links().get(0).src().deviceId())
                                    .fromApp(appId)
                                    .withPriority(20)
                                    .forTable(0)
                                    .withIdleTimeout(10)
                                    .build();

                            log.info("--- [WNET 2024] flow rule sent to first node {}", currentDeviceId);
                            flowRuleService.applyFlowRules(firstRule);
                        }

                        //This is an intermediate link and the last one
                        if ((i > 0) && (i < path.links().size())) {
                            TrafficSelector selector = DefaultTrafficSelector.builder()
                                    .matchInPort(path.links().get(i-1).dst().port())
                                    .matchEthSrc(srcHost.mac())
                                    .matchEthDst(dstHost.mac())
                                    .build();

                            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                    .setOutput(path.links().get(i).src().port())
                                    .build();

                            FlowRule intermediateRule = DefaultFlowRule.builder()
                                    .withSelector(selector)
                                    .withTreatment(treatment)
                                    .forDevice(path.links().get(i).src().deviceId())
                                    .fromApp(appId)
                                    .withPriority(20)
                                    .forTable(0)
                                    .withIdleTimeout(10)
                                    .build();

                            log.info("--- [WNET 2024] flow rule sent to first node {}", path.links().get(i).src().deviceId());
                            flowRuleService.applyFlowRules(intermediateRule);
                        }

                        //This is the last link
                        if (i == path.links().size() - 1) {
                            TrafficSelector selector = DefaultTrafficSelector.builder()
                                    .matchInPort(path.links().get(i).dst().port())
                                    .matchEthSrc(srcHost.mac())
                                    .matchEthDst(dstHost.mac())
                                    .build();

                            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                    .setOutput(dstHost.location().port())
                                    .build();

                            FlowRule lastRule = DefaultFlowRule.builder()
                                    .withSelector(selector)
                                    .withTreatment(treatment)
                                    .forDevice(path.links().get(i).dst().deviceId())
                                    .fromApp(appId)
                                    .withPriority(20)
                                    .forTable(0)
                                    .withIdleTimeout(10)
                                    .build();

                            log.info("--- [WNET 2024] flow rule sent to first node {}", path.links().get(i).dst().deviceId());
                            flowRuleService.applyFlowRules(lastRule);
                        }
                    }

                    log.info("Sending OF_PckOut to device {} toward port {}",
                            context.inPacket().receivedFrom(),
                            path.links().get(0).src().port());

                    packetOut(context, path.links().get(0).src().port());

                    return;

                     */
                }

                log.error("[WNET 2024] packet received IPV4 from {} INTERMEDIATE HOP !!!", context.inPacket().receivedFrom());
                return;
            }

            return;
        }
    }

    private void installIpv4FlowRules() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .build();

        packetService.requestPackets(selector, PacketPriority.REACTIVE, appId);
    }

    private void removeIpv4FlowRules(){
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

}
