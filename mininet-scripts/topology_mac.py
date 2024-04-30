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

        # Add hosts
	h01 = self.addHost('h01',ip='10.10.0.1',mac='00:00:00:00:00:01')
       	h11 = self.addHost('h11',ip='10.10.1.1',mac='00:00:00:00:00:11')
        h21 = self.addHost('h21',ip='10.10.2.1',mac='00:00:00:00:00:21')
        h31 = self.addHost('h31',ip='10.10.3.1',mac='00:00:00:00:00:31')
        h41 = self.addHost('h41',ip='10.10.4.1',mac='00:00:00:00:00:41')
	h51 = self.addHost('h51',ip='10.10.5.1',mac='00:00:00:00:00:51')
	h61 = self.addHost('h61',ip='10.10.6.1',mac='00:00:00:00:00:61')
	h71 = self.addHost('h71',ip='10.10.7.1',mac='00:00:00:00:00:71')
	h81 = self.addHost('h81',ip='10.10.8.1',mac='00:00:00:00:00:81')
	h91 = self.addHost('h91',ip='10.10.9.1',mac='00:00:00:00:00:91')
	
	h02 = self.addHost('h02',ip='10.10.0.2',mac='00:00:00:00:00:02')
	h12 = self.addHost('h12',ip='10.10.1.2',mac='00:00:00:00:00:12')
        h22 = self.addHost('h22',ip='10.10.2.2',mac='00:00:00:00:00:22')
        h32 = self.addHost('h32',ip='10.10.3.2',mac='00:00:00:00:00:32')
        h42 = self.addHost('h42',ip='10.10.4.2',mac='00:00:00:00:00:42')
        h52 = self.addHost('h52',ip='10.10.5.2',mac='00:00:00:00:00:52')
        h62 = self.addHost('h62',ip='10.10.6.2',mac='00:00:00:00:00:62')
        h72 = self.addHost('h72',ip='10.10.7.2',mac='00:00:00:00:00:72')
        h82 = self.addHost('h82',ip='10.10.8.2',mac='00:00:00:00:00:82')
        h92 = self.addHost('h92',ip='10.10.9.2',mac='00:00:00:00:00:92')


        # Add switches
	s0 = self.addSwitch('s0', dpid="1000000000000000", protocols='OpenFlow13', datapath='user')
        s1 = self.addSwitch('s1', dpid="1000000000000001", protocols='OpenFlow13', datapath='user')
        s2 = self.addSwitch('s2', dpid="1000000000000002", protocols='OpenFlow13', datapath='user')
        s3 = self.addSwitch('s3', dpid="1000000000000003", protocols='OpenFlow13', datapath='user')
        s4 = self.addSwitch('s4', dpid="1000000000000004", protocols='OpenFlow13', datapath='user')
	s5 = self.addSwitch('s5', dpid="1000000000000005", protocols='OpenFlow13', datapath='user')
	s6 = self.addSwitch('s6', dpid="1000000000000006", protocols='OpenFlow13', datapath='user')
	s7 = self.addSwitch('s7', dpid="1000000000000007", protocols='OpenFlow13', datapath='user')
	s8 = self.addSwitch('s8', dpid="1000000000000008", protocols='OpenFlow13', datapath='user')
	s9 = self.addSwitch('s9', dpid="1000000000000009", protocols='OpenFlow13', datapath='user')
 
        # Add host to device links
        self.addLink(h01, s0)
	self.addLink(h11, s1)
        self.addLink(h21, s2)
        self.addLink(h31, s3)
        self.addLink(h41, s4)
	self.addLink(h51, s5)
	self.addLink(h61, s6)
	self.addLink(h71, s7)
	self.addLink(h81, s8)
	self.addLink(h91, s9)

	self.addLink(h02, s0)
        self.addLink(h12, s1)
        self.addLink(h22, s2)
        self.addLink(h32, s3)
        self.addLink(h42, s4)
        self.addLink(h52, s5)
        self.addLink(h62, s6)
        self.addLink(h72, s7)
        self.addLink(h82, s8)
        self.addLink(h92, s9)

	# Add device to device links (infrastructure links)
        self.addLink(s1, s2)
        self.addLink(s2, s3)
        self.addLink(s1, s4)
	self.addLink(s4, s5)
	self.addLink(s4, s6)
	self.addLink(s3, s5)
	self.addLink(s5, s7)
	self.addLink(s6, s7)
	self.addLink(s5, s8)
	self.addLink(s7, s0)
	self.addLink(s8, s9)
	self.addLink(s9, s0)

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
