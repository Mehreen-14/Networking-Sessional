#include "tcp-adaptive-reno.h"
#include "ns3/log.h"
#include "ns3/simulator.h"
#include "rtt-estimator.h"
#include "tcp-socket-base.h"

NS_LOG_COMPONENT_DEFINE ("TcpAdaptiveReno");

namespace ns3 {

NS_OBJECT_ENSURE_REGISTERED (TcpAdaptiveReno);

TypeId
TcpAdaptiveReno::GetTypeId (void)
{
  //ns3::TcpAdaptiveReno
  static TypeId tid =
   TypeId("ns3::TcpAdaptiveReno")
    .SetParent<TcpNewReno>()
    .SetGroupName ("Internet")
    .AddConstructor<TcpAdaptiveReno>()
    .AddAttribute("FilterType", "Use this to choose no filter or Tustin's approximation filter",
                  EnumValue(TcpAdaptiveReno::TUSTIN), MakeEnumAccessor(&TcpAdaptiveReno::m_fType),
                  MakeEnumChecker(TcpAdaptiveReno::NONE, "None", TcpAdaptiveReno::TUSTIN, "Tustin"))
    .AddTraceSource("EstimatedBW", "The estimated bandwidth",
                    MakeTraceSourceAccessor(&TcpAdaptiveReno::m_currentBW),
                    "ns3::TracedValueCallback::Double")
  ;
  return tid;
}

TcpAdaptiveReno::TcpAdaptiveReno (void) :
  TcpWestwoodPlus(),
  m_minimumRtt (Time (0)),
  m_currRtt (Time (0)),
  m_jpcktLossRtt (Time (0)),
  m_conjRtt (Time (0)),
  m_prevConjRtt (Time(0)),
  m_incWnd (0),
  m_baseWnd (0),
  m_probeWnd (0)
{
  NS_LOG_FUNCTION (this);
}

TcpAdaptiveReno::TcpAdaptiveReno (const TcpAdaptiveReno& sock) :
  TcpWestwoodPlus (sock),
  m_minimumRtt (Time (0)),
  m_currRtt (Time (0)),
  m_jpcktLossRtt (Time (0)),
  m_conjRtt (Time (0)),
  m_prevConjRtt (Time(0)),
  m_incWnd (0),
  m_baseWnd (0),
  m_probeWnd (0)
{
  NS_LOG_FUNCTION (this);
}

TcpAdaptiveReno::~TcpAdaptiveReno (void)
{
}

/*
The function is called every time an ACK is received (only one time
also for cumulative ACKs) and contains timing information
*/
void
TcpAdaptiveReno::PktsAcked (Ptr<TcpSocketState> tcb, uint32_t packetsAcked,
                        const Time& rtt)
{
  NS_LOG_FUNCTION (this << tcb << packetsAcked << rtt);

  if (rtt.IsZero ())
    {
      NS_LOG_WARN ("RTT measured is zero!");
      return;
    }

  m_ackedSegments += packetsAcked;

  /*

      INITIALIZE AND SET VALUES FOR  m_minimumRtt, m_currRtt

  */

  // calculate min rtt here
  if(m_minimumRtt.IsZero()) { m_minimumRtt = rtt; }
  else if(rtt <= m_minimumRtt) { m_minimumRtt = rtt; }

  m_currRtt = rtt;
  
  TcpWestwoodPlus::EstimateBW (rtt, tcb);
}


double
TcpAdaptiveReno::EstimateCongestionLevel()
{
  
  float a = 0.85; // exponential smoothing factor
  if(m_prevConjRtt < m_minimumRtt) a = 0; // the initial value should take the full current Jth loss Rtt

  double prev = a*m_prevConjRtt.GetSeconds();
  double jpkt = (1-a)*m_jpcktLossRtt.GetSeconds();
  
  double conjRtt =  prev + jpkt; 
  m_conjRtt = Seconds(conjRtt); 
  
  double minimum = std::min((m_currRtt.GetSeconds() - m_minimumRtt.GetSeconds()) / (conjRtt - m_minimumRtt.GetSeconds()),1.0);

  return minimum;
  }


void 
TcpAdaptiveReno::EstimateIncWnd(Ptr<TcpSocketState> tcb)
{
  
  double congtn = EstimateCongestionLevel();
  int M = 1000; 

  double b = static_cast<double>(m_currentBW.Get().GetBitRate())/ 8.0 ;
  double MSS = static_cast<double> (tcb->m_segmentSize * tcb->m_segmentSize);
  double m_maxIncWnd = b / M * MSS ; 

  double alpha = 10; 
  double beta = 2 * m_maxIncWnd * ((1/alpha) - ((1/alpha + 1)/(std::exp(alpha))));
  double gamma = 1 - (2 * m_maxIncWnd * ((1/alpha) - ((1/alpha + 0.5)/(std::exp(alpha)))));

  m_incWnd = (int)((m_maxIncWnd / std::exp(alpha * congtn)) + (beta * congtn) + gamma);

  
}


void
TcpAdaptiveReno::CongestionAvoidance (Ptr<TcpSocketState> tcb, uint32_t segmentsAcked)
{
  
  NS_LOG_FUNCTION (this << tcb << segmentsAcked);

  if (segmentsAcked > 0)
    {
      EstimateIncWnd(tcb);
      double adder = static_cast<double> (tcb->m_segmentSize * tcb->m_segmentSize) / tcb->m_cWnd.Get ();
      adder = std::max (1.0, adder);
      m_baseWnd += static_cast<uint32_t> (adder);

      // change probe window
      m_probeWnd = std::max(
        (double) (m_probeWnd + m_incWnd / (int)tcb->m_cWnd.Get()), 
        (double) 0
      );
      
      tcb->m_cWnd = m_baseWnd + m_probeWnd;
      //NS_LOG_INFO ("In CongAvoid, updated to cwnd " << tcb->m_cWnd <<
       //            " ssthresh " << tcb->m_ssThresh);
    }

}
//packet drop hole GetSsThresh function call hobe
uint32_t
TcpAdaptiveReno::GetSsThresh (Ptr<const TcpSocketState> tcb, 
                          uint32_t bytesInFlight)
{
  
  m_prevConjRtt = m_conjRtt; // set the previous conjestion RTT as a loss event will be occured
  m_jpcktLossRtt = m_currRtt; //  the RTT of previous packet or jth loss event
  
  double congestion = EstimateCongestionLevel();
  uint32_t ssthresh = std::max (2*tcb->m_segmentSize,(uint32_t) (tcb->m_cWnd / (1.0+congestion)));

  // reset calculations
  m_baseWnd = ssthresh;
  m_probeWnd = 0;
  
  
  return ssthresh;

}

Ptr<TcpCongestionOps>
TcpAdaptiveReno::Fork ()
{
  return CreateObject<TcpAdaptiveReno> (*this);
}

} // namespace ns3