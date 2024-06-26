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

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Sample Apache Karaf CLI command
 */
@Service
@Command(scope = "onos", name = "pcklog-complete-host-list",
        description = "This command lists all known hosts")
public class AnotherCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {

        HostService hostService = get(HostService.class);

        print("---------------------------");
        print("--- LIST OF KNOWN HOSTS ---");
        print("---------------------------");

        //List hosts = newArrayList(hostService.getHosts());

        for (Host host : hostService.getHosts()) {
            print("Host id: %s with ip address %s", host.id(), host.ipAddresses());
        }
    }

}
