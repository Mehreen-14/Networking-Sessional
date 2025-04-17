//this file will be in a folder in scratch
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <fstream>
#include "ns3/core-module.h"
#include "ns3/network-module.h"
#include "ns3/internet-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/point-to-point-layout-module.h"
#include "ns3/applications-module.h"
#include "ns3/stats-module.h"
#include "ns3/callback.h"
#include "ns3/flow-monitor-module.h"
#include "ns3/csma-module.h"
#include <sys/stat.h>


#include <unistd.h> // Include this header for getcwd function
#include <cerrno>

//#include "tutorial-app.h"



using namespace ns3;
using namespace std;

NS_LOG_COMPONENT_DEFINE ("CongestionControl");


class TutorialApp : public Application
{
public:
  TutorialApp ();
  virtual ~TutorialApp ();

  /**
   * Register this type.
   * \return The TypeId.
   */
  static TypeId GetTypeId (void);
  void Setup (Ptr<Socket> socket, Address address, uint32_t packetSize, DataRate dataRate, uint32_t simultime);

private:
  virtual void StartApplication (void);
  virtual void StopApplication (void);

  void ScheduleTx (void);
  void SendPacket (void);

  Ptr<Socket>     m_socket;
  Address         m_peer;
  uint32_t        m_packetSize;
  DataRate        m_dataRate;
  EventId         m_sendEvent;
  bool            m_running;
  uint32_t        m_packetsSent;
  uint32_t        m_simultime;
};

TutorialApp::TutorialApp ()
  : m_socket (0),
    m_peer (),
    m_packetSize (0),
    m_dataRate (0),
    m_sendEvent (),
    m_running (false),
    m_packetsSent (0),
    m_simultime (0)
{
}

TutorialApp::~TutorialApp ()
{
  m_socket = 0;
}

/* static */
TypeId TutorialApp::GetTypeId (void)
{
  static TypeId tid = TypeId ("TutorialApp")
    .SetParent<Application> ()
    .SetGroupName ("Tutorial")
    .AddConstructor<TutorialApp> ()
    ;
  return tid;
}

void
TutorialApp::Setup (Ptr<Socket> socket, Address address, uint32_t packetSize, DataRate dataRate, uint32_t simultime)
{
  m_socket = socket;
  m_peer = address;
  m_packetSize = packetSize;
  m_dataRate = dataRate;
  m_simultime = simultime;
  }

void
TutorialApp::StartApplication (void)
{
  m_running = true;
  m_packetsSent = 0;
    if (InetSocketAddress::IsMatchingType (m_peer))
    {
      m_socket->Bind ();
    }
  else
    {
      m_socket->Bind6 ();
    }
  m_socket->Connect (m_peer);
  SendPacket ();
}

void
TutorialApp::StopApplication (void)
{
  m_running = false;

  if (m_sendEvent.IsRunning ())
    {
      Simulator::Cancel (m_sendEvent);
    }

  if (m_socket)
    {
      m_socket->Close ();
    }
}

void
TutorialApp::SendPacket (void)
{
  Ptr<Packet> packet = Create<Packet> (m_packetSize);
  m_socket->Send (packet);

  if(Simulator::Now().GetSeconds() < m_simultime) ScheduleTx();
}

void
TutorialApp::ScheduleTx (void)
{
  if (m_running)
    {
      Time tNext (Seconds (m_packetSize * 8 / static_cast<double> (m_dataRate.GetBitRate ())));
      m_sendEvent = Simulator::Schedule (tNext, &TutorialApp::SendPacket, this);
    }
}

static void
CwndChange (Ptr<OutputStreamWrapper> stream, uint32_t oldCwnd, uint32_t newCwnd)
{
  *stream->GetStream () << Simulator::Now ().GetSeconds () << " " << newCwnd << std::endl;
}


int main(int argc, char *argv[]){


    char currentDir[FILENAME_MAX];
    if (getcwd(currentDir, sizeof(currentDir)) != NULL) {
        std::cout << "Current working directory: " << currentDir << std::endl;
    } else {
        perror("getcwd() error");
    }

    uint32_t payloadSize = 1024;
    std::string tcp1 = "ns3::TcpNewReno"; // TcpNewReno
    std::string tcp2 = "ns3::TcpHighSpeed"; //Variance
    
    int LeafNodes = 2;
    int numFlows = 2;
    std::string sender_drate = "1Gbps";
    std::string sender_delay = "1ms";
    int sim_sec = 30;
    int bttlnkRate = 50;
    int bttlnkDelay = 100;
    int ploss_exp = 6;
    int option = 1;
    std::string Myfolder = "1905078_HighSpeed";
    


    CommandLine cmd (__FILE__);
    cmd.AddValue ("bttlnkRate","Max Packets allowed in the device queue", bttlnkRate);
    cmd.AddValue ("plossRate", "Packet loss rate", ploss_exp);
    cmd.AddValue ("option","1 for bttlnck, 2 for packet loss rate", option);
    cmd.AddValue ("tcp2","Tcp Variant 2",tcp2);
    cmd.AddValue ("folder","Create Folder",Myfolder);
    cmd.Parse (argc,argv);

    if(tcp2 == "ns3::TcpHighSpeed"){
      Myfolder = "1905078_HighSpeed";
    
    }

    else if(tcp2 == "ns3::TcpAdaptiveReno")
    {
       Myfolder = "1905078_AdaptiveReno";
    
    }

    else if(tcp2 == "ns3::TcpWestwoodPlus"){
       Myfolder = "1905078_WestWoodPlus";
    
    }

    if (mkdir(Myfolder.c_str(), 0777) == 0) {
        std::cout << "Folder created successfully" << std::endl;
    } else {
        std::cout << Myfolder.c_str() << std::endl;
        std::cerr << "Failed to create folder. Error code: " << errno << std::endl;
   }




    LogComponentEnable("PacketSink",LOG_LEVEL_INFO);
    double packet_loss_rate = (1.0 / std::pow(10, ploss_exp));
    std::string bottleNeckDataRate = std::to_string(bttlnkRate) + "Mbps";
    std::string bottleNeckDelay = std::to_string(bttlnkDelay) + "ms";

    Config::SetDefault ("ns3::TcpSocket::SegmentSize", UintegerValue (payloadSize)); 

    PointToPointHelper bottleNeckLink;
    bottleNeckLink.SetDeviceAttribute  ("DataRate", StringValue (bottleNeckDataRate));
    bottleNeckLink.SetChannelAttribute ("Delay", StringValue (bottleNeckDelay));
             
    PointToPointHelper pointToPointLeaf;
    pointToPointLeaf.SetDeviceAttribute  ("DataRate", StringValue (sender_drate));
    pointToPointLeaf.SetChannelAttribute ("Delay", StringValue (sender_delay));

    float bandwidth_delay_product = (bttlnkDelay * bttlnkRate)/payloadSize;
    pointToPointLeaf.SetQueue ("ns3::DropTailQueue", "MaxSize", StringValue (std::to_string (bandwidth_delay_product) + "p"));

    PointToPointDumbbellHelper dumbbell (LeafNodes, pointToPointLeaf,
                                  LeafNodes, pointToPointLeaf,
                                  bottleNeckLink);

    //for packet loss forcefully
    Ptr<RateErrorModel> em = CreateObject<RateErrorModel> ();
    em->SetAttribute ("ErrorRate", DoubleValue (packet_loss_rate));
    dumbbell.m_routerDevices.Get(1)->SetAttribute ("ReceiveErrorModel", PointerValue (em)); 

     // tcp1
    Config::SetDefault ("ns3::TcpL4Protocol::SocketType", StringValue (tcp1));
    InternetStackHelper stack1;

    stack1.Install (dumbbell.GetLeft (0)); // left leaf
    stack1.Install (dumbbell.GetRight (0)); // right leaf
    
    stack1.Install (dumbbell.GetLeft ());
    stack1.Install (dumbbell.GetRight ());

    // tcp2
    Config::SetDefault ("ns3::TcpL4Protocol::SocketType", StringValue (tcp2));
    InternetStackHelper stack2;


    stack2.Install (dumbbell.GetLeft (1)); // left leaf
    stack2.Install (dumbbell.GetRight (1)); // right leaf
     


    stack2.Install (dumbbell.GetLeft ());
    stack2.Install (dumbbell.GetRight ());

     // ASSIGN IP Addresses
    dumbbell.AssignIpv4Addresses (Ipv4AddressHelper ("10.1.1.0", "255.255.255.0"), // left nodes
                          Ipv4AddressHelper ("10.2.1.0", "255.255.255.0"),  // right nodes
                          Ipv4AddressHelper ("10.3.1.0", "255.255.255.0")); // routers 
    Ipv4GlobalRoutingHelper::PopulateRoutingTables (); // populate routing table

    // install flow monitor
    FlowMonitorHelper flow_monitor;
    flow_monitor.SetMonitorAttribute("MaxPerHopDelay", TimeValue(Seconds(sim_sec)));
    Ptr<FlowMonitor> monitor = flow_monitor.InstallAll ();

    uint16_t sp = 8080;


    for(int i=0;i<numFlows; i++){
      Address sinkAddress (InetSocketAddress (dumbbell.GetRightIpv4Address (i), sp));
      PacketSinkHelper packetSinkHelper ("ns3::TcpSocketFactory", InetSocketAddress (Ipv4Address::GetAny(), sp));
      ApplicationContainer sinkApps = packetSinkHelper.Install (dumbbell.GetRight (i));
      sinkApps.Start (Seconds (0));
      sinkApps.Stop (Seconds (sim_sec));

      Ptr<Socket> ns3TcpSocket = Socket::CreateSocket (dumbbell.GetLeft (i), TcpSocketFactory::GetTypeId ());
      Ptr<TutorialApp> app = CreateObject<TutorialApp> ();
      app->Setup (ns3TcpSocket, sinkAddress, payloadSize, DataRate (sender_drate), sim_sec);
      dumbbell.GetLeft (i)->AddApplication (app);
      app->SetStartTime (Seconds (1));
      app->SetStopTime (Seconds (sim_sec));

      std::ostringstream oss;
      oss << Myfolder << "/tcp_" << i+1 <<  ".txt";
      AsciiTraceHelper asciiTraceHelper;
      Ptr<OutputStreamWrapper> stream = asciiTraceHelper.CreateFileStream (oss.str());
      ns3TcpSocket->TraceConnectWithoutContext ("CongestionWindow", MakeBoundCallback (&CwndChange, stream));
     }


     Simulator::Stop (Seconds (sim_sec));
     Simulator::Run ();

     std::string file = Myfolder + "/datafiles_" + std::to_string(option) + ".txt";


    
    // flow monitor
    int j = 0;
    float Throughput_arr[] = {0, 0};

    std::ofstream MyFile(file, std::ios_base::app);

    Ptr<Ipv4FlowClassifier> classifier = DynamicCast<Ipv4FlowClassifier> (flow_monitor.GetClassifier ());
    FlowMonitor::FlowStatsContainer stats = monitor->GetFlowStats ();

    for (auto it = stats.begin (); it != stats.end (); ++it) {
      Ipv4FlowClassifier::FiveTuple t = classifier->FindFlow (it->first);

      if(j%2 == 0) {
         Throughput_arr[0] += it->second.rxBytes; 
      }
      if(j%2 == 1) {
         Throughput_arr[1] += it->second.rxBytes; 
      }

      j = j + 1;  
    }

    Throughput_arr[0] =(Throughput_arr[0] * 8) / ((sim_sec)*1000) ; 
    Throughput_arr[1] =(Throughput_arr[1] * 8) / ((sim_sec)*1000) ;  //kbps
    
    MyFile << bottleNeckDataRate << " " << (packet_loss_rate) << " " << Throughput_arr[0] << " " << Throughput_arr[1] << " " << std::endl;
    
    Simulator::Destroy ();

    cout << "DONE" << endl;

    return 0;


}
