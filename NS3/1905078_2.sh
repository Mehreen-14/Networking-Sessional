#!/bin/bash


rm -r "scratch/1905078_2_nodes"
rm -r "scratch/1905078_2_flows"
rm -r "scratch/1905078_2_packets"
rm -r "scratch/1905078_2_coveragearea"
# Create a directory to store data files
mkdir -p scratch/1905078_2_nodes
mkdir -p scratch/1905078_2_flows
mkdir -p scratch/1905078_2_packets
mkdir -p scratch/1905078_2_coveragearea
mkdir -p scratch/1905078_2_speed

FILE="scratch/1905078_2"
F1="scratch/1905078_2_nodes"
F2="scratch/1905078_2_flows"
F3="scratch/1905078_2_packets"
F4="scratch/1905078_2_coveragearea"
F5="scratch/1905078_2_speed"

# Remove previous files
rm -f "${F1}.dat"
rm -f "${F2}.dat"
rm -f "${F3}.dat"
rm -f "${F4}.dat"
rm -f "${F5}.dat"

# Arrays for different parameters
NODES=("20" "40" "60" "80" "100")
FLOWS=("10" "20" "30" "40" "50")
PACKETS_PER_SECOND=("100" "200" "300" "400" "500")
COVERAGE_AREA=("1" "2" "4" "5")
SPEED=("5" "10" "15" "20" "25")

#initialize
numNodes=20
numFlows=10
packetsPerSecond=100
coverage=5
velocity=5.0
option=1

# Function to run simulation
simulation() {
    echo "Running simulation with: nodes=$1, flows=$2, pps=$3, coverage=$4,velocity=$5 option=$6"
    #./ns3 run "$FILE" --numNodes="$1" --numFlows="$2" --packetsPerSecond="$3" --coverage="$4" >> "$5" 2>&1
    ./ns3 run "$FILE" -- --numNodes="$1" --numFlows="$2" --packetsPerSecond="$3" --coverage="$4" --velocity="$5" --option="$6" >> "$7" 2>&1
     echo "Simulation completed."
}

# Function to create Gnuplot plots
gnuplot_func() {
    gnuplot << EOF
    set terminal png size 640,480
    set output "$2.png"
    set xlabel "$4"
    set ylabel "$5"
    plot "$1" using 1:$6 title "$3" with linespoints
    exit
EOF
}
# Loop through each combination of parameters and run the simulation
for node in "${NODES[@]}"; do
    echo "Processing nodes=$node..."
    flow=$(( "$node" / 2 ))
    simulation "$node" "$flow" "$packetsPerSecond" "$coverage" "$velocity" "$option" "${F1}.dat"
done

# Create Gnuplot plots
gnuplot_func "${F1}.dat" "$F1/1905078_2_Node_vs_Avg_throughput" "Nodes vs Average Throughput" "Nodes" "Average Throughput" 2
gnuplot_func "${F1}.dat" "$F1/1905078_2_Node_vs_Delivery_ratio" "Nodes vs Delivery ratio" "Nodes" "Delivery ratio" 3


numNodes=20
option=2
for flow in "${FLOWS[@]}"; do
    echo "Processing flows=$flow..."
    simulation "$numNodes" "$flow" "$packetsPerSecond" "$coverage" "$velocity" "$option" "${F2}.dat"
done

# Create Gnuplot plots
gnuplot_func "${F2}.dat" "$F2/1905078_2_Num of Flows_vs_Avg_throughput" "Number of Flows vs Average Throughput" "Number of Flows" "Average Throughput" 2
gnuplot_func "${F2}.dat" "$F2/1905078_2_Num of Flows_vs_Delivery_ratio" "Number of Flows vs Delivery ratio" "Number of Flows" "Delivery ratio" 3

numFlows=10
option=3
for pps in "${PACKETS_PER_SECOND[@]}"; do
    echo "Processing pps=$pps..."
    simulation "$numNodes" "$numFlows" "$pps" "$coverage" "$velocity" "$option" "${F3}.dat"
done

# Create Gnuplot plots
gnuplot_func "${F3}.dat" "$F3/1905078_2_PPS_vs_Avg_throughput" "PPS vs Average Throughput" "PPS" "Average Throughput" 2
gnuplot_func "${F3}.dat" "$F3/1905078_2_PPS_vs_Delivery_ratio" "PPS vs Delivery ratio" "PPS" "Delivery ratio" 3

packetsPerSecond=100
option=4
for cov in "${COVERAGE_AREA[@]}"; do
    echo "Processing coverage area=$cov..."
    simulation "$numNodes" "$numFlows" "$packetsPerSecond" "$cov" "$velocity" "$option" "${F4}.dat"
done

# Create Gnuplot plots
gnuplot_func "${F4}.dat" "$F4/1905078_2_Coverage_area_vs_Avg_throughput" "Coverage Area vs Average Throughput" "Coverage Area" "Average Throughput" 2
gnuplot_func "${F4}.dat" "$F4/1905078_2_Coverage_area_vs_Delivery_ratio" "COverage Area vs Delivery ratio" "Coverage Area" "Delivery ratio" 3

coverage=5
option=5
for vel in "${SPEED[@]}"; do
    echo "Processing velocity=$vel..."
    simulation "$numNodes" "$numFlows" "$packetsPerSecond" "$coverage" "$vel" "$option" "${F5}.dat"
done

# Create Gnuplot plots
gnuplot_func "${F5}.dat" "$F5/1905078_2_Num of Speed_vs_Avg_throughput" "Number of Speed vs Average Throughput" "Number of Speed" "Average Throughput" 2
gnuplot_func "${F5}.dat" "$F5/1905078_2_Num of Speed_vs_Delivery_ratio" "Number of Speed vs Delivery ratio" "Number of Speed" "Delivery ratio" 3
