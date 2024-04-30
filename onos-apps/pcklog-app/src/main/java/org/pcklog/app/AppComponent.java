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
package org.pcklog.app;

import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
import org.onosproject.net.flow.*;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.pcklog.app");

        packetService.addProcessor(processor, PacketProcessor.director(2));

        installIpv4FlowRule();

        log.info("Packet Logger application has been started with appId {}", appId);
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;

        removeIpv4FlowRule();

        log.info("Packet Logger application has been stopped with appId {}", appId);
    }

    /**
     * Request packet in via packet service.
     */
    private void installIpv4FlowRule() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void removeIpv4FlowRule() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
                return;
            }
            
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt.getEtherType() == Ethernet.TYPE_LLDP) {
                log.info("[---PACKLOGGED---]: from {} ETH_TYPE: LLDP", context.inPacket().receivedFrom());
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
                log.info("[---PACKLOGGED---]: from {} ETH_TYPE: ARP", context.inPacket().receivedFrom());
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {

                //----
                //Identify the destination host
                HostId id = HostId.hostId(ethPkt.getDestinationMAC());
                Host dst = hostService.getHost(id);

                //Identify the source host
                HostId idSrc = HostId.hostId(ethPkt.getSourceMAC());
                Host src = hostService.getHost(idSrc);

                if (dst == null) {
                    log.error("PCKLOG destination host is not known MAC {}", ethPkt.getDestinationMAC());
                    return;
                }

                if (src == null) {
                    log.error("PCKLOG source host is not known MAC {}", ethPkt.getSourceMAC());
                    return;
                }

                DeviceId currentDeviceId = pkt.receivedFrom().deviceId();
                DeviceId hostLocation = src.location().deviceId();
                if (hostLocation.equals(currentDeviceId)) {
                    log.info("PCKLOG NEW PCK source host {} to destination {} from device {}",
                            ethPkt.getSourceMAC(),
                            ethPkt.getDestinationMAC(),
                            currentDeviceId);
                }



                log.info("[---PACKLOGGED---]: from {} ETH_TYPE: IPv4", context.inPacket().receivedFrom());

                /*---

                // Are we on an edge switch that our destination is on? If so, install rule and packet out
                if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
                    if (!context.inPacket().receivedFrom().port().equals(dst.location().port())) {

                        log.warn("PROFWD packet received on the destination switch, rule installed");

                        installRule(context,
                                pkt.receivedFrom().deviceId(),
                                pkt.receivedFrom().port(),
                                dst.location().port());

                        packetOut(context,dst.location().port());
                    }
                    return;
                }

                log.info("PROFWD received packet from device {} directed to device {}",
                        pkt.receivedFrom().deviceId(),
                        dst.location().deviceId());

                // Otherwise, get a set of paths that lead from here to the
                // destination edge switch.
                Set<Path> paths =
                        topologyService.getPaths(topologyService.currentTopology(),
                                pkt.receivedFrom().deviceId(),
                                dst.location().deviceId());

                if (paths.isEmpty()) {
                    // If there are no paths, flood and bail.
                    log.error("There is not a path from source to destination device.");
                    return;
                }

                // Otherwise, pick a path that does not lead back to where we
                // came from; if no such path, flood and bail.
                Path path = pickForwardPathIfPossible(paths, pkt.receivedFrom().port());
                if (path == null) {
                    log.error("Don't know where to go from here {} for {} -> {}",
                            pkt.receivedFrom(), ethPkt.getSourceMAC(), ethPkt.getDestinationMAC());
                    return;
                }

                //Get the first path in the set
                Path path = paths.iterator().next();

                log.info("PROFWD path:");
                for (Link link : path.links()) {
                    log.info("--- PROFWD link {}/{}->{}/{}",
                            link.src().deviceId(),
                            link.src().port(),
                            link.dst().deviceId(),
                            link.dst().port());
                }

                //Install a single flow rule to each device in the path.
                for (int i=0; i<path.links().size(); i++) {
                    if (i == 0) {
                        log.info("PROFWD {} installing rule on device {} in {} out {}",
                                i,
                                pkt.receivedFrom().deviceId(),
                                pkt.receivedFrom().port(),
                                path.links().get(0).src().port());

                        installRule(context,
                                pkt.receivedFrom().deviceId(),
                                pkt.receivedFrom().port(),
                                path.links().get(0).src().port());
                    } else {
                        log.info("PROFWD {} installing rule on device {} in {} out {}",
                                i,
                                path.links().get(i).src().deviceId(),
                                path.links().get(i-1).dst().port(),
                                path.links().get(i).src().port());

                        installRule(context,
                                path.links().get(i).src().deviceId(),
                                path.links().get(i-1).dst().port(),
                                path.links().get(i).src().port());

                        if (i == path.links().size() - 1) {
                            log.info("PROFWD {} installing rule on device {} in {} out {}",
                                    i,
                                    dst.location().deviceId(),
                                    path.links().get(i).dst().port(),
                                    dst.location().port());

                            installRule(context,
                                    dst.location().deviceId(),
                                    path.links().get(i).dst().port(),
                                    dst.location().port());
                        }
                    }
                }

                //Send packet out message
                log.info("PROFWD sending packet out to device {}", context.inPacket().receivedFrom().deviceId());
                packetOut(context, path.links().get(0).src().port());
                ---*/

            }
        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    // Selects a path from the given set that does not lead back to the
    // specified port if possible.
    private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
        for (Path path : paths) {
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Install a flow rule on deviceId using MAC src and dst taked from the context.
     *
     * @param context packet received within a packetin
     * @param deviceId device where to install the rule
     * @param inPort input port included in the selector
     * @param outPort output port included in the treatment
     */
    private void installRule(PacketContext context, DeviceId deviceId,
                             PortNumber inPort, PortNumber outPort) {
        //
        // We don't support (yet) buffer IDs in the Flow Service so
        // packet out first.
        //
        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchInPort(inPort)
                .matchEthSrc(inPkt.getSourceMAC())
                .matchEthDst(inPkt.getDestinationMAC());

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(outPort)
                .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(10)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent()
                .add();

        flowObjectiveService.forward(deviceId, forwardingObjective);
    }
}
