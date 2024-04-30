/*
 * Copyright 2021-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.ConnectPointCompleter;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;

/**
 * Sample Apache Karaf CLI command
 */
@Service
@Command(scope = "onos", name = "pcklog-device-host-list",
         description = "This command lists the host connected to a specific device")
public class AppCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "device", description = "specific device",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String deviceString = null;

    @Override
    protected void doExecute() {

        HostService hostService = get(HostService.class);

        DeviceId device = DeviceId.deviceId(deviceString);

        print("Listing the hosts connected to device %s", deviceString);

        for (Host host : hostService.getConnectedHosts(device)) {
            print("Host id: %s with ip address %s", host.id(), host.ipAddresses());
        }
    }

}
