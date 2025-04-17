#include "ns3/applications-module.h"
#include "ns3/core-module.h"
#include "ns3/internet-module.h"
#include "ns3/network-module.h"
#include "ns3/wifi-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/mobility-module.h"
#include "ns3/command-line.h"
#include <fstream>
using namespace ns3;

int tot_Txpckts=0;
int tot_Rxpckts=0;
int tot_received_Bytes=0;
Ptr<OutputStreamWrapper> stream1;
Ptr<OutputStreamWrapper>stream2;
Ptr<OutputStreamWrapper>stream3;
Ptr<OutputStreamWrapper>stream4;
Ptr<OutputStreamWrapper>stream5;

NS_LOG_COMPONENT_DEFINE("1905078_1");

void

Tx_callback(Ptr<const Packet> packet)

{

    tot_Txpckts++;
    //NS_LOG_UNCOND( "total Tx Packets \t" << tot_Txpckts);
    //std::cout<<"total Tx packets "<<tot_Txpckts<<std::endl;
}

void

Rx_callback(Ptr<const Packet> packet, const Address& address)

{

    tot_received_Bytes += packet->GetSize();
    //NS_LOG_UNCOND( "total received bytes \t" << tot_received_Bytes);

    //std::cout<<"total received bytes "<<tot_received_Bytes<<std::endl;

    tot_Rxpckts++;
    //NS_LOG_UNCOND( "total Rx Packets \t" << tot_Rxpckts);
    //std::cout<<"total Rx packets  "<<tot_Rxpckts<<std::endl;

}

double CalculateThroughput() {
    Time now = Simulator::Now();
    double cur = (tot_received_Bytes) * 8.0 / 1e6 / 0.5; // Convert to Mbit/s
    //NS_LOG_UNCOND(now.GetSeconds() << "s: \t" << cur << " Mbit/s");
    tot_received_Bytes = 0; // Reset received bytes for the next calculation
    return cur; // Return the calculated throughput
}

double
PacketDeliveryRatio(){
    Time now = Simulator::Now(); /* Return the simulator's virtual time. */
    if(tot_Txpckts != 0){
        double r = (double) tot_Rxpckts / (double) tot_Txpckts;
        //NS_LOG_UNCOND(now.GetSeconds() << "s: \t" << r << " Mbit/s");
        return r;
    }
    return 0;
}

//verify mobile topology
void PositionChangeCallback(Vector oldPosition, Vector newPosition) {
    //NS_LOG_UNCOND("Node position changed from " << oldPosition << " to " << newPosition);
}

int
main(int argc, char* argv[])
{
     bool verbose = true;
    uint32_t numNodes = 20; // Number of sender-receiver pairs
    uint32_t numFlows = 40;
    uint32_t coverage = 1;
    
    uint32_t packetsPerSecond = 400;
    double simulationTime = 10;
    double radius = 10.0; 
    double velocity = 5.0;
    int option=1;

   
    CommandLine cmd(__FILE__);
    
    cmd.AddValue("numNodes", "Number of \"extra\" nodes/devices", numNodes);
    //cmd.AddValue("verbose", "Tell echo applications to log if true", verbose);
    cmd.AddValue("numFlows","Number of Flows",numFlows);
    cmd.AddValue("packetsPerSecond","Packet per second",packetsPerSecond);
    cmd.AddValue("coverage","Coverage Area",coverage);
    cmd.AddValue("velocity","Velocity",velocity);
    cmd.AddValue("option","Options",option);
    cmd.Parse(argc, argv);

    //NS_LOG_INFO("Value of numNodes: " << numNodes);


    double txpDistance = ((double)coverage)*250.0;
    std::string dataRate = (std::to_string(packetsPerSecond*1000*8/1e6))+"Mbps";  //  data rate = 400*1000*8/1e6
    

     AsciiTraceHelper asciiTraceHelper;
    if(option==1){
    stream1  = asciiTraceHelper.CreateFileStream("1905078_1_nodes.dat");
    }
    else if(option==2){
    stream2  = asciiTraceHelper.CreateFileStream("1905078_1_flows.dat");
    }
    else if(option==3){
    stream3  = asciiTraceHelper.CreateFileStream("1905078_1_packets.dat");
    }
    else if(option==4){
    stream4  = asciiTraceHelper.CreateFileStream("1905078_1_coveragearea.dat");
    }
    else if(option==5){
        stream4  = asciiTraceHelper.CreateFileStream("1905078_1_speed.dat");
    
    }
    if (verbose)
    {
        LogComponentEnable("UdpEchoClientApplication", LOG_LEVEL_INFO);
        LogComponentEnable("UdpEchoServerApplication", LOG_LEVEL_INFO);
    }

    NS_LOG_INFO(numNodes);

    // Create nodes
    NodeContainer p2pNodes;
    p2pNodes.Create (2);

    NodeContainer senderNodes;
    //senderNodes.Add(p2pNodes.Get(0));
    senderNodes.Create(numNodes/2);

    NodeContainer receiverNodes;
    //receiverNodes.Add(p2pNodes.Get(1));
    receiverNodes.Create(numNodes/2);

    PointToPointHelper pointToPoint;
    pointToPoint.SetDeviceAttribute("DataRate", StringValue("1Mbps"));
    pointToPoint.SetChannelAttribute("Delay", StringValue("10ms"));
    NetDeviceContainer p2pDevices;
    p2pDevices = pointToPoint.Install (p2pNodes);

    // Set up WiFi
    WifiHelper wifi;
    //wifi.SetStandard(ns3::WIFI_STANDARD_80211p); // Use 5 GHz band
    // wifi.SetRemoteStationManager("ns3::ConstantRateWifiManager",
    //                              "DataMode",
    //                              StringValue("HtMcs7"),
    //                              "ControlMode",
    //                              StringValue("HtMcs0"));


    // Set up propagation loss model
    YansWifiChannelHelper channel = YansWifiChannelHelper::Default();
    channel.SetPropagationDelay("ns3::ConstantSpeedPropagationDelayModel");
    channel.AddPropagationLoss("ns3::RangePropagationLossModel",
                                   "MaxRange",
                                   DoubleValue(txpDistance));//set Tx_range
    
    YansWifiPhyHelper phy1;
    YansWifiPhyHelper phy2;
    phy1.SetChannel(channel.Create()); 
    phy2.SetChannel(channel.Create());
    
    // Install WiFi on sender and receiver nodes
    NetDeviceContainer senderDevices;
    NetDeviceContainer receiverDevices;

    WifiMacHelper mac1;
    WifiMacHelper mac2;
    Ssid ssid1 = Ssid("ns-3-ssid1");
    Ssid ssid2 = Ssid("ns-3-ssid2");
    mac1.SetType("ns3::StaWifiMac", "Ssid", SsidValue(ssid1), "ActiveProbing", BooleanValue(false));
    mac2.SetType("ns3::StaWifiMac", "Ssid", SsidValue(ssid2), "ActiveProbing", BooleanValue(false));

    senderDevices = wifi.Install(phy1, mac1, senderNodes);
    receiverDevices = wifi.Install(phy2, mac2, receiverNodes);


    NetDeviceContainer p2pDevices1;
    mac1.SetType("ns3::ApWifiMac", "Ssid", SsidValue(ssid1));
    p2pDevices1 = wifi.Install(phy1, mac1, p2pNodes.Get(0));

    NetDeviceContainer p2pDevices2;
    mac2.SetType("ns3::ApWifiMac", "Ssid", SsidValue(ssid2));
    p2pDevices2 = wifi.Install(phy2, mac2, p2pNodes.Get(1));



    MobilityHelper mobility;

    mobility.SetPositionAllocator("ns3::GridPositionAllocator",
                                  "MinX",
                                  DoubleValue(3.0),
                                  "MinY",
                                  DoubleValue(3.0),
                                  "DeltaX",
                                  DoubleValue(3.0),
                                  "DeltaY",
                                  DoubleValue(3.0),
                                  "GridWidth",
                                  UintegerValue(3),
                                  "LayoutType",
                                  StringValue("RowFirst"));
    //mobility.SetPositionAllocator("ns3::RandomDiscPositionAllocator", "Rho", ns3::StringValue("ns3::UniformRandomVariable[Min=0.0|Max=" + std::to_string(radius) + "]"));
    mobility.SetMobilityModel("ns3::ConstantVelocityMobilityModel");
    mobility.Install(senderNodes);
   
    for (uint32_t i = 0; i < senderNodes.GetN(); ++i) {
        Ptr<ConstantVelocityMobilityModel> cvModel = senderNodes.Get(i)->GetObject<ConstantVelocityMobilityModel>();
        cvModel->SetVelocity(Vector(velocity, 0, 0)); // Moving along the X-axis
        // Set up callback to detect position changes
        //std::cout<<"position changes with "<<velocity<<std::endl;
        cvModel->TraceConnectWithoutContext("PositionChange", MakeCallback(&PositionChangeCallback));
        //std::cout<<"position changes with "<<std::endl;
  
    }


    mobility.SetMobilityModel("ns3::ConstantPositionMobilityModel");
     mobility.Install(p2pNodes.Get(0));
   

    mobility.SetPositionAllocator("ns3::GridPositionAllocator",
                                  "MinX",
                                  DoubleValue(3.0),
                                  "MinY",
                                  DoubleValue(3.0),
                                  "DeltaX",
                                  DoubleValue(3.0),
                                  "DeltaY",
                                  DoubleValue(3.0),
                                  "GridWidth",
                                  UintegerValue(3),
                                  "LayoutType",
                                  StringValue("RowFirst"));
    mobility.SetMobilityModel("ns3::ConstantVelocityMobilityModel");
    mobility.Install(receiverNodes);

    for (uint32_t i = 0; i < receiverNodes.GetN(); ++i) {
        Ptr<ConstantVelocityMobilityModel> cvModel1 = receiverNodes.Get(i)->GetObject<ConstantVelocityMobilityModel>();
        cvModel1->SetVelocity(Vector(velocity, 0, 0)); // Moving along the X-axis
        // Set up callback to detect position changes
        cvModel1->TraceConnectWithoutContext("PositionChange", MakeCallback(&PositionChangeCallback));
  
    }

    mobility.SetMobilityModel("ns3::ConstantPositionMobilityModel");
    mobility.Install(p2pNodes.Get(1));
    

    // Set up IPv4 addresses for sender and receiver nodes
    InternetStackHelper stack;
    
    stack.Install(senderNodes);
    stack.Install(p2pNodes.Get(0));
    stack.Install(receiverNodes);
    stack.Install(p2pNodes.Get(1));

    Ipv4AddressHelper address;
    address.SetBase("10.1.1.0", "255.255.255.0"); // Starting address for senders
    Ipv4InterfaceContainer senderInterfaces = address.Assign(senderDevices);
    address.Assign(p2pDevices1);

    address.SetBase("10.1.2.0", "255.255.255.0"); // Starting address for receivers
    Ipv4InterfaceContainer receiverInterfaces = address.Assign(receiverDevices);
    address.Assign(p2pDevices2);

    address.SetBase("10.1.3.0", "255.255.255.0"); // Starting address for receivers
    Ipv4InterfaceContainer p2pInterfaces = address.Assign(p2pDevices);


    
    // Create OnOff applications for each sender
    Ipv4GlobalRoutingHelper::PopulateRoutingTables();

    // Install applications (OnOffApplication and PacketSink) for each sender-receiver pair
    uint16_t port = 9;

    
    
    for (u_int32_t i = 0; i < (numNodes/2); i++)
    {
        for(int j=0;j<(numFlows/(numNodes/2));j++){
            uint16_t port = 9 + j; // Each flow uses a different port
        //uint32_t senderIndex = i % numPairs;
        //uint32_t receiverIndex = i % numPairs;

            OnOffHelper onoff("ns3::TcpSocketFactory",
                          InetSocketAddress(receiverInterfaces.GetAddress(i), port));
            onoff.SetAttribute("OnTime", StringValue("ns3::ConstantRandomVariable[Constant=1]"));
            onoff.SetAttribute("OffTime", StringValue("ns3::ConstantRandomVariable[Constant=0]"));
            onoff.SetAttribute("DataRate", DataRateValue(DataRate(dataRate))); //set data Rate
            //std::cout << "Installed on off for " << i << std::endl;
            // NS_LOG_INFO("Installing on off");
            ApplicationContainer apps = onoff.Install(senderNodes.Get(i));
            apps.Start(Seconds(2.0));
            apps.Stop(Seconds(10.0));
            Ptr<OnOffApplication> onOffApp = StaticCast<OnOffApplication>(apps.Get(0));

            onOffApp->TraceConnectWithoutContext("Tx", MakeCallback(&Tx_callback));
        }
        
        
    }


    

    ApplicationContainer sinkApp;
    PacketSinkHelper sink("ns3::TcpSocketFactory", InetSocketAddress(Ipv4Address::GetAny(), port));
    for (u_int32_t i = 0; i < (numNodes/2); i++)
    {
        // std::cout<<i<<" : ";
        // NS_LOG_INFO("Installing packet sink");
        //std::cout << "Installed packetsink for " << i << std::endl;
        sinkApp = sink.Install(receiverNodes.Get(i));
        sinkApp.Start(Seconds(2.0));
        
    }
    Ptr<PacketSink> Sink = StaticCast<PacketSink>(sinkApp.Get(0));

    Sink->TraceConnectWithoutContext("Rx", MakeCallback(&Rx_callback));
    //std::cout<<"Finished "<<std::endl;
    Simulator::Stop(Seconds(simulationTime+1));

    // Run the simulation
    Simulator::Run();
    
    //CalculateThroughput();
    double averageThroughput = CalculateThroughput();
    double packetdelratio = PacketDeliveryRatio();


     if(option==1){
        NS_LOG_UNCOND(numNodes<<"\t"<<averageThroughput<<"\t"<<packetdelratio);
    }
    else if(option==2){
        NS_LOG_UNCOND(numFlows<<"\t"<<averageThroughput<<"\t"<<packetdelratio);
    }
    else if(option==3){
        NS_LOG_UNCOND(packetsPerSecond<<"\t"<<averageThroughput<<"\t"<<packetdelratio);
    }
    else if(option==4){
        NS_LOG_UNCOND(coverage<<"\t"<<averageThroughput<<"\t"<<packetdelratio);
    }

    else if(option==5){
        NS_LOG_UNCOND(velocity<<"\t"<<averageThroughput<<"\t"<<packetdelratio);
   
    }

    Simulator::Destroy();
    

    return 0;
}
