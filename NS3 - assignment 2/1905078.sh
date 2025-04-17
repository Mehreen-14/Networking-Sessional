#!/bin/bash
#run.sh file will be in ns-3.39 (not in scratch)

FILE="scratch/maliha/1905078.cc"



#destination folders
rm -r "1905078_HighSpeed"
mkdir -p "1905078_HighSpeed"

rm -r "1905078_AdaptiveReno"
mkdir -p "1905078_AdaptiveReno"

rm -r "1905078_WestWoodPlus"
mkdir -p "1905078_WestWoodPlus"

DIR1="1905078_HighSpeed"
DIR2="1905078_AdaptiveReno"
DIR3="1905078_WestWoodPlus"


mkdir -p "${DIR1}/Graphs"
mkdir -p "${DIR2}/Graphs"
mkdir -p "${DIR3}/Graphs"

mkdir -p "${DIR1}/Graphs/BottleNeck_Rate"
mkdir -p "${DIR2}/Graphs/BottleNeck_Rate"
mkdir -p "${DIR3}/Graphs/BottleNeck_Rate"

mkdir -p "${DIR1}/Graphs/PacketLoss_Rate"
mkdir -p "${DIR2}/Graphs/PacketLoss_Rate"
mkdir -p "${DIR3}/Graphs/PacketLoss_Rate"

F3="Graphs/BottleNeck_Rate"
F5="Graphs/PacketLoss_Rate"




# Arrays for different parameters
BOTTLENKDRATE=("25" "50" "100" "200" "300")
PCKTLOSSRATE=("2" "3" "4" "5" "6")
TCP=("ns3::TcpHighSpeed" "ns3::TcpAdaptiveReno" "ns3::TcpWestwoodPlus")

simulation() {
    ./ns3 run "$FILE" -- --bttlnkRate="$1" --plossRate="$2" --option="$3" --tcp2="$4" --folder="$5" >> "$6" 2>&1
     echo "Simulation completed."
}

# Function to create Gnuplot plots
gnuplot_func() {
    gnuplot << EOF
    set terminal png size 3000,1500
    set output "$3.png"
    set xlabel "$6"
    set ylabel "$7"
    plot "$1" using 1:2 with linespoints lc rgb "blue" title "tcp 1", \
         "$2" using 1:2 with linespoints lc rgb "red" title "tcp 2"
    exit
EOF
}

# Function to create Gnuplot plots
gnuplot_func2() {
    gnuplot << EOF
    set terminal png size 3000,1500
    set output "$2.png"
    set xlabel "$4"
    set ylabel "$5"
    plot "$1" using $6:$7 with linespoints lc rgb "blue" title "tcp 1", \
         "$1" using $6:$8 with linespoints lc rgb "red" title "tcp 2"
    exit
EOF
}



for t in "${TCP[@]}"; do
    plossRate=6
    option=1

    if [ "$t" == "ns3::TcpHighSpeed" ]; then
       folder="$DIR1"
    elif [ "$t" == "ns3::TcpAdaptiveReno" ]; then
       folder="$DIR2"
    elif [ "$t" == "ns3::TcpWestwoodPlus" ]; then
       folder="$DIR3"
    fi
    # Loop through each combination of parameters and run the simulation
    for drate in "${BOTTLENKDRATE[@]}"; do
       echo "Processing data_rate=$drate..."
       simulation "$drate" "$plossRate" "$option" "$t" "$folder" "${folder}/tcp_1.txt"
       gnuplot_func "${folder}/tcp_1.txt" "${folder}/tcp_2.txt" "${folder}/${F3}/1905078_Congestion_Window_VS_Time_d_Rate($drate)_Mbps" "Congestion Window VS Time (tcp 1)" "Congestion Window VS Time (tcp 2)" "times" "Congestion window"     
    done

    gnuplot_func2 "${folder}/datafiles_1.txt" "${folder}/${F3}/Throughput_Vs_BottleNeck_LinkCapacity" "Throughput_Vs_BottleNeck_Capacity" "BottleNeck_LinkCapacity (Mbps)" "Average Throughput (kbps)" 1 3 4


    bttlnkRate=50
    option=2

    # Loop through each combination of parameters and run the simulation
    for plrate in "${PCKTLOSSRATE[@]}"; do
       echo "Processing packet_loss_rate=$plrate..."
       simulation "$bttlnkRate" "$plrate" "$option" "$t" "$folder" "${folder}/tcp_1.txt"
       gnuplot_func "${folder}/tcp_1.txt" "${folder}/tcp_2.txt" "${folder}/${F5}/1905078_Congestion_Window_VS_Time_pl_Rate($plrate)" "Congestion Window VS Time (tcp 1)" "Congestion Window VS Time (tcp 2)" "times" "Congestion window" 
    done

    gnuplot_func2 "${folder}/datafiles_2.txt" "${folder}/${F5}/Throughput_Vs_PacketLoss_Rate" "Throughput_Vs_PacketLoss_Rate" "PacketLoss_Rate (Mbps)" "Average Throughput(Kbps)" 2 3 4
  
done





