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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Sample Apache Karaf CLI command
 */
@Service
@Command(scope = "onos", name = "mesh-list-hosts",
         description = "Sample Apache Karaf CLI command")
public class AppCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        HostService hostService = get(HostService.class);

        //Traslates the Set of hosts in a List of hosts
        List<Host> hosts = newArrayList(hostService.getHosts());

        print("-------------------------------");
        print("--- LIST OF CONNECTED HOSTS ---");
        print("-------------------------------");

        for(int i=0; i < hosts.size(); i++) {

            print("Host ID: %s - IP addresses: %s",
                    hosts.get(i).id(),
                    hosts.get(i).ipAddresses());
        }
    }
}
