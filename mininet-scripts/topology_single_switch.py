from mininet.node import CPULimitedHost
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.log import setLogLevel, info
from mininet.node import RemoteController, OVSSwitch
from mininet.cli import CLI

class SimplePktSwitch(Topo):
    """Simple topology example."""

    def __init__(self, **opts):
        """Create custom topo."""

        # Initialize topology
        # It uses the constructor for the Topo cloass
        super(SimplePktSwitch, self).__init__(**opts)

        # Adding switches
	s0 = self.addSwitch('s0', dpid="1000000000000000", protocols='OpenFlow13', datapath='user')

def run():
    c = RemoteController('c', '127.0.0.1')
    net = Mininet(topo=SimplePktSwitch(), host=CPULimitedHost, controller=None, switch=OVSSwitch)
    net.addController(c)
    net.start()

    CLI(net)
    net.stop()

# if the script is run directly (sudo custom/optical.py):
if __name__ == '__main__':
    setLogLevel('info')
    run()
