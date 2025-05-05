package cloudsim.report;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Generates reports for CloudSim simulations
 */
public class SimulationReport {
    
    /**
     * Generate a comprehensive report for a simulation
     * 
     * @param broker            Datacenter broker with finished cloudlets
     * @param datacenter        Datacenter used in the simulation
     * @param startUtilization  Host utilization before load balancing
     * @param endUtilization    Host utilization after load balancing
     * @param outputFilePath    Path to write the report to
     */
    public static void generateSimulationReport(DatacenterBroker broker, Datacenter datacenter,
                                               Map<Host, Double> startUtilization, 
                                               Map<Host, Double> endUtilization,
                                               String outputFilePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            // Report header
            writer.println("=====================================================");
            writer.println("           CLOUDSIM SIMULATION REPORT               ");
            writer.println("=====================================================");
            writer.println();
            
            // Datacenter and host information
            writer.println("1. DATACENTER CONFIGURATION");
            writer.println("---------------------------");
            writer.println("Number of hosts: " + datacenter.getHostList().size());
            
            for (Host host : datacenter.getHostList()) {
                writer.printf("Host #%d: %d CPUs, %.2f MIPS/CPU, %.2f GB RAM, %.2f GB Storage\n",
                        host.getId(),
                        host.getNumberOfPes(),
                        host.getTotalMipsCapacity() / host.getNumberOfPes(),
                        host.getRam().getCapacity() / 1024.0, // Convert MB to GB
                        host.getStorage().getCapacity() / (1024.0 * 1024 * 1024) // Convert bytes to GB
                );
            }
            writer.println();
            
            // VM information
            writer.println("2. VIRTUAL MACHINE ALLOCATION");
            writer.println("-----------------------------");
            writer.println("Number of VMs: " + broker.getVmCreatedList().size());
            
            for (Vm vm : broker.getVmCreatedList()) {
                writer.printf("VM #%d: %d vCPUs, %.2f MIPS/CPU, %.2f GB RAM, Hosted on Host #%d\n",
                        vm.getId(),
                        vm.getNumberOfPes(),
                        vm.getMips(),
                        vm.getRam().getCapacity() / 1024.0, // Convert MB to GB
                        vm.getHost().getId()
                );
            }
            writer.println();
            
            // Cloudlet execution information
            writer.println("3. CLOUDLET EXECUTION RESULTS");
            writer.println("-----------------------------");
            writer.println("Number of cloudlets: " + broker.getCloudletFinishedList().size());
            
            writer.printf("%-10s %-10s %-15s %-15s %-15s %-15s %-15s\n",
                    "Cloudlet", "VM", "Start Time", "Finish Time", "Execution Time", "CPU Cost", "Total Cost");
            
            for (Cloudlet cloudlet : broker.getCloudletFinishedList()) {
                writer.printf("%-10d %-10d %-15.2f %-15.2f %-15.2f %-15.4f %-15.4f\n",
                        cloudlet.getId(), 
                        cloudlet.getVm().getId(),
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime(),
                        cloudlet.getActualCpuTime(),
                        0.0, // Cost per second removed in newer CloudSim
                        0.0); // Total cost removed in newer CloudSim
            }
            
            // Calculate statistics
            double totalExecutionTime = 0;
            double maxExecutionTime = 0;
            
            for (Cloudlet cloudlet : broker.getCloudletFinishedList()) {
                double executionTime = cloudlet.getActualCpuTime();
                totalExecutionTime += executionTime;
                maxExecutionTime = Math.max(maxExecutionTime, executionTime);
            }
            
            double avgExecutionTime = broker.getCloudletFinishedList().isEmpty() ? 0 : 
                                     totalExecutionTime / broker.getCloudletFinishedList().size();
            
            writer.println("\nExecution Statistics:");
            writer.printf("Average execution time: %.2f seconds\n", avgExecutionTime);
            writer.printf("Total execution time (makespan): %.2f seconds\n", maxExecutionTime);
            writer.println("Cost calculations not available in this CloudSim version.");
            writer.println();
            
            // Load balancing information
            writer.println("4. LOAD BALANCING RESULTS");
            writer.println("--------------------------");
            writer.println("Host Utilization Before vs After Load Balancing:");
            writer.println("                  Before         After       Difference");
            
            double totalBeforeUtilization = 0;
            double totalAfterUtilization = 0;
            double varianceBefore = 0;
            double varianceAfter = 0;
            
            for (Host host : datacenter.getHostList()) {
                double before = startUtilization.getOrDefault(host, 0.0);
                double after = endUtilization.getOrDefault(host, 0.0);
                double difference = after - before;
                
                writer.printf("Host #%-10d %.2f%%         %.2f%%       %s%.2f%%\n", 
                        host.getId(), 
                        before * 100, 
                        after * 100,
                        difference >= 0 ? "+" : "",
                        difference * 100);
                
                totalBeforeUtilization += before;
                totalAfterUtilization += after;
            }
            
            double avgBeforeUtilization = totalBeforeUtilization / datacenter.getHostList().size();
            double avgAfterUtilization = totalAfterUtilization / datacenter.getHostList().size();
            
            for (Host host : datacenter.getHostList()) {
                double before = startUtilization.getOrDefault(host, 0.0);
                double after = endUtilization.getOrDefault(host, 0.0);
                
                varianceBefore += Math.pow(before - avgBeforeUtilization, 2);
                varianceAfter += Math.pow(after - avgAfterUtilization, 2);
            }
            
            double stdDevBefore = Math.sqrt(varianceBefore / datacenter.getHostList().size());
            double stdDevAfter = Math.sqrt(varianceAfter / datacenter.getHostList().size());
            
            writer.println("\nLoad Distribution Statistics:");
            writer.printf("Average Host Utilization: Before = %.2f%%, After = %.2f%%\n", 
                    avgBeforeUtilization * 100, avgAfterUtilization * 100);
            
            writer.printf("Standard Deviation (measure of imbalance): Before = %.4f, After = %.4f\n", 
                    stdDevBefore, stdDevAfter);
            
            double improvementPercentage = ((stdDevBefore - stdDevAfter) / stdDevBefore) * 100;
            writer.printf("Load Distribution Improvement: %.2f%%\n", improvementPercentage);
            writer.println();
            
            // Conclusion
            writer.println("5. CONCLUSION");
            writer.println("-------------");
            
            if (stdDevAfter < stdDevBefore) {
                writer.println("The load balancing algorithm successfully improved resource utilization balance across hosts.");
                writer.printf("The imbalance was reduced by %.2f%%, demonstrating the effectiveness of the migration strategy.\n", 
                        improvementPercentage);
            } else {
                writer.println("The load balancing algorithm did not improve resource utilization balance in this scenario.");
                writer.println("Further tuning of migration thresholds or policies may be required.");
            }
            
            if (avgAfterUtilization > avgBeforeUtilization) {
                writer.printf("Average resource utilization increased from %.2f%% to %.2f%%, improving overall resource efficiency.\n",
                        avgBeforeUtilization * 100, avgAfterUtilization * 100);
            } else if (avgAfterUtilization < avgBeforeUtilization) {
                writer.printf("Average resource utilization decreased from %.2f%% to %.2f%%, which may indicate over-provisioning.\n",
                        avgBeforeUtilization * 100, avgAfterUtilization * 100);
            } else {
                writer.println("Average resource utilization remained the same, but distribution across hosts improved.");
            }
            
            writer.println("\nRecommendations:");
            if (stdDevAfter > 0.1) { // Still some imbalance
                writer.println("- Further optimization of VM allocation and migration policies is recommended");
                writer.println("- Consider more aggressive migration thresholds for heavily loaded hosts");
                writer.println("- Implement predictive load balancing to anticipate utilization spikes");
            } else {
                writer.println("- The current load balancing strategy is effective for the tested workload");
                writer.println("- Consider experimenting with different VM allocation policies for initial placement");
                writer.println("- Monitor VM migration overhead in a production environment");
            }
            
            writer.println("\n=====================================================");
            writer.println("                  END OF REPORT                     ");
            writer.println("=====================================================");
            
            System.out.println("Simulation report generated successfully at: " + outputFilePath);
            
        } catch (IOException e) {
            System.err.println("Error generating simulation report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generate a simplified report focusing on load balancing comparison
     * 
     * @param beforeUtilization Host utilization before load balancing
     * @param afterUtilization  Host utilization after load balancing
     * @param outputFilePath    Path to write the report to
     */
    public static void generateLoadBalancingReport(Map<Host, Double> beforeUtilization, 
                                                  Map<Host, Double> afterUtilization,
                                                  String outputFilePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            // Report header
            writer.println("=====================================================");
            writer.println("               LOAD BALANCING REPORT                ");
            writer.println("=====================================================");
            writer.println();
            
            // Host utilization comparison
            writer.println("HOST UTILIZATION COMPARISON");
            writer.println("---------------------------");
            writer.println("                  Before         After       Difference");
            
            // Calculate statistics for each host
            double totalBeforeUtilization = 0;
            double totalAfterUtilization = 0;
            int hostCount = beforeUtilization.size();
            
            for (Map.Entry<Host, Double> entry : beforeUtilization.entrySet()) {
                Host host = entry.getKey();
                double before = entry.getValue();
                double after = afterUtilization.getOrDefault(host, 0.0);
                double difference = after - before;
                
                writer.printf("Host #%-10d %.2f%%         %.2f%%       %s%.2f%%\n", 
                        host.getId(), 
                        before * 100, 
                        after * 100,
                        difference >= 0 ? "+" : "",
                        difference * 100);
                
                totalBeforeUtilization += before;
                totalAfterUtilization += after;
            }
            
            // Calculate averages
            double avgBeforeUtilization = totalBeforeUtilization / hostCount;
            double avgAfterUtilization = totalAfterUtilization / hostCount;
            
            // Calculate standard deviations (to measure imbalance)
            double varianceBefore = 0;
            double varianceAfter = 0;
            
            for (Map.Entry<Host, Double> entry : beforeUtilization.entrySet()) {
                Host host = entry.getKey();
                double before = entry.getValue();
                double after = afterUtilization.getOrDefault(host, 0.0);
                
                varianceBefore += Math.pow(before - avgBeforeUtilization, 2);
                varianceAfter += Math.pow(after - avgAfterUtilization, 2);
            }
            
            double stdDevBefore = Math.sqrt(varianceBefore / hostCount);
            double stdDevAfter = Math.sqrt(varianceAfter / hostCount);
            
            // Write summary statistics
            writer.println("\nSUMMARY STATISTICS");
            writer.println("------------------");
            writer.printf("Average Host Utilization: Before = %.2f%%, After = %.2f%%\n", 
                    avgBeforeUtilization * 100, avgAfterUtilization * 100);
            
            writer.printf("Standard Deviation (imbalance): Before = %.4f, After = %.4f\n", 
                    stdDevBefore, stdDevAfter);
            
            double improvementPercentage = 0;
            if (stdDevBefore > 0) {
                improvementPercentage = ((stdDevBefore - stdDevAfter) / stdDevBefore) * 100;
            }
            
            writer.printf("Load Distribution Improvement: %.2f%%\n", improvementPercentage);
            writer.println();
            
            // Write analysis and conclusion
            writer.println("ANALYSIS AND CONCLUSION");
            writer.println("-----------------------");
            
            if (stdDevAfter < stdDevBefore) {
                writer.printf("The load balancing mechanism has successfully improved resource distribution across hosts by %.2f%%.\n", 
                        improvementPercentage);
                writer.println("This indicates effective VM migration strategy that helps prevent hotspots and resource contention.");
            } else {
                writer.println("The load balancing mechanism did not improve resource distribution in this scenario.");
                writer.println("This may be due to inappropriate migration thresholds or lack of migration opportunities.");
            }
            
            if (avgAfterUtilization > avgBeforeUtilization) {
                writer.printf("Average resource utilization increased from %.2f%% to %.2f%%, improving overall efficiency.\n",
                        avgBeforeUtilization * 100, avgAfterUtilization * 100);
            } else if (avgAfterUtilization < avgBeforeUtilization) {
                writer.printf("Average resource utilization decreased from %.2f%% to %.2f%%, indicating potential over-provisioning.\n",
                        avgBeforeUtilization * 100, avgAfterUtilization * 100);
            } else {
                writer.println("Average resource utilization remained constant, but distribution across hosts has changed.");
            }
            
            // Write recommendations
            writer.println("\nRECOMMENDATIONS");
            writer.println("---------------");
            
            // Thresholds recommendations
            writer.println("1. Threshold Optimization:");
            if (stdDevAfter > 0.1) {
                writer.println("   - Consider adjusting overload threshold to trigger migrations earlier");
                writer.println("   - The current thresholds may be too conservative for the workload pattern");
            } else {
                writer.println("   - Current thresholds appear appropriate for the workload");
                writer.println("   - Consider slight adjustments to fine-tune performance");
            }
            
            // VM selection strategy recommendations
            writer.println("\n2. VM Selection Strategy:");
            writer.println("   - Consider prioritizing migration of VMs with higher resource demands");
            writer.println("   - Evaluate VM migration cost vs. benefit more precisely");
            
            // Host selection strategy recommendations
            writer.println("\n3. Host Selection Strategy:");
            writer.println("   - Implement more sophisticated target host selection algorithms");
            writer.println("   - Consider power efficiency in addition to load balancing");
            
            writer.println("\n=====================================================");
            writer.println("                  END OF REPORT                     ");
            writer.println("=====================================================");
            
            System.out.println("Load balancing report generated successfully at: " + outputFilePath);
            
        } catch (IOException e) {
            System.err.println("Error generating load balancing report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
