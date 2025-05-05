package cloudsim.simulation;

import cloudsim.loadbalancing.LoadBalancer;
import cloudsim.monitor.UtilizationMonitor;
import cloudsim.utils.CloudSimUtils;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Load balancing simulation for Task 2
 */
public class LoadBalancingSimulation {
    
    private final CloudSim simulation;
    private Datacenter datacenter;
    private DatacenterBroker broker;
    private List<Host> hostList;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private LoadBalancer loadBalancer;
    private UtilizationMonitor utilizationMonitor;
    
    // Maps to store utilization data before and after load balancing
    private Map<Host, Double> utilizationBeforeLoadBalancing;
    private Map<Host, Double> utilizationAfterLoadBalancing;
    
    // Constants for the simulation
    private static final int HOST_PES = 4;
    private static final int HOST_MIPS = 1000;
    private static final int HOST_RAM = 8 * 1024; // 8 GB in MB
    private static final int HOST_STORAGE = 1000; // 1 TB in GB
    private static final int HOST_BW = 10000; // 10 Gbps
    
    private static final double CPU_OVERLOAD_THRESHOLD = 0.8; // 80%
    private static final double CPU_UNDERUTILIZED_THRESHOLD = 0.5; // 50%
    
    public LoadBalancingSimulation() {
        this.simulation = new CloudSim();
        this.hostList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
        this.utilizationBeforeLoadBalancing = new HashMap<>();
        this.utilizationAfterLoadBalancing = new HashMap<>();
    }
    
    /**
     * Run the simulation
     */
    public void run() {
        // Create datacenter with two hosts
        createDatacenter();
        
        // Create broker
        broker = CloudSimUtils.createBroker(simulation);
        
        // Create VMs
        createVms();
        
        // Create cloudlets
        createCloudlets();
        
        // Submit VMs and cloudlets to the broker
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        
        // Create utilization monitor and load balancer
        utilizationMonitor = new UtilizationMonitor(datacenter, CPU_OVERLOAD_THRESHOLD, CPU_UNDERUTILIZED_THRESHOLD);
        loadBalancer = new LoadBalancer(datacenter, utilizationMonitor, broker);
        
        // Run simulation
        System.out.println("Starting simulation...");
        
        // First phase: Run simulation without load balancing for a bit
        simulation.addOnClockTickListener(evt -> {
            if (evt.getTime() == 10) { // Check at 10 seconds
                // Record utilization before load balancing
                utilizationBeforeLoadBalancing = utilizationMonitor.getHostUtilization();
                
                // Print current state
                printCurrentState("BEFORE LOAD BALANCING");
            }
        });
        
        // Second phase: Apply load balancing
        simulation.addOnClockTickListener(evt -> {
            if (evt.getTime() == 20) { // Apply load balancing at 20 seconds
                System.out.println("\n====== APPLYING LOAD BALANCING ======");
                loadBalancer.executeLoadBalancing();
            }
        });
        
        // Third phase: Check results after load balancing
        simulation.addOnClockTickListener(evt -> {
            if (evt.getTime() == 30) { // Check results at 30 seconds
                // Record utilization after load balancing
                utilizationAfterLoadBalancing = utilizationMonitor.getHostUtilization();
                
                // Print current state
                printCurrentState("AFTER LOAD BALANCING");
                
                // Print comparison
                printLoadBalancingComparison();
            }
        });
        
        // Start simulation
        simulation.start();
        
        // Print final results
        printResults();
    }
    
    /**
     * Create datacenter with two hosts as per Task 2 requirements
     */
    private void createDatacenter() {
        // Create hosts
        // Host H1: 4 CPUs, 8 GB RAM, 1000 GB storage (Overloaded)
        Host host1 = CloudSimUtils.createHost(
            0,                // Host ID 
            HOST_PES,         // 4 CPUs
            HOST_MIPS,        // 1000 MIPS per CPU
            HOST_RAM,         // 8 GB RAM in MB
            HOST_STORAGE,     // 1000 GB storage
            HOST_BW,          // 10 Gbps bandwidth
            true              // Time-Shared policy
        );
        
        // Host H2: 4 CPUs, 8 GB RAM, 1000 GB storage (Underutilized)
        Host host2 = CloudSimUtils.createHost(
            1,                // Host ID 
            HOST_PES,         // 4 CPUs
            HOST_MIPS,        // 1000 MIPS per CPU
            HOST_RAM,         // 8 GB RAM in MB
            HOST_STORAGE,     // 1000 GB storage
            HOST_BW,          // 10 Gbps bandwidth
            true              // Time-Shared policy
        );
        
        hostList.add(host1);
        hostList.add(host2);
        
        // Create datacenter
        datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        datacenter.setSchedulingInterval(1); // Check for events every second
    }
    
    /**
     * Create VMs to create an overload scenario on Host 1
     */
    private void createVms() {
        // Create VMs for Host 1 (enough to create overload)
        for (int i = 0; i < 6; i++) {
            Vm vm = CloudSimUtils.createVm(
                i,              // VM ID
                1,              // 1 vCPU
                800,            // 800 MIPS per CPU
                1024,           // 1 GB RAM in MB
                1000,           // 1 Gbps bandwidth
                10,             // 10 GB disk
                true            // Time-Shared policy
            );
            vmList.add(vm);
        }
        
        // Create only one VM for Host 2 (to keep it underutilized)
        Vm vm7 = CloudSimUtils.createVm(
            6,              // VM ID
            1,              // 1 vCPU
            500,            // 500 MIPS per CPU
            1024,           // 1 GB RAM in MB
            1000,           // 1 Gbps bandwidth
            10,             // 10 GB disk
            true            // Time-Shared policy
        );
        vmList.add(vm7);
    }
    
    /**
     * Create cloudlets (tasks) that will run on VMs
     */
    private void createCloudlets() {
        // Create utilization models for cloudlets
        UtilizationModelDynamic utilizationCpu = new UtilizationModelDynamic(0.8); // 80% CPU utilization
        UtilizationModelDynamic utilizationRam = new UtilizationModelDynamic(0.3); // 30% RAM utilization
        UtilizationModelDynamic utilizationBw = new UtilizationModelDynamic(0.1);  // 10% BW utilization
        
        // Create cloudlets for VMs on Host 1
        for (int i = 0; i < 6; i++) {
            Cloudlet cloudlet = CloudSimUtils.createCloudlet(
                i,                // Cloudlet ID
                100000,           // 100K MI (Million Instructions)
                1,                // Requires 1 PE
                1024,             // 1 MB input file size
                1024,             // 1 MB output file size
                utilizationCpu,   // CPU utilization model
                utilizationRam,   // RAM utilization model
                utilizationBw     // BW utilization model
            );
            
            // Assign cloudlet to specific VM
            cloudlet.setVm(vmList.get(i));
            cloudletList.add(cloudlet);
        }
        
        // Create one cloudlet for VM on Host 2
        Cloudlet cloudlet7 = CloudSimUtils.createCloudlet(
            6,                  // Cloudlet ID
            50000,              // 50K MI (Million Instructions)
            1,                  // Requires 1 PE
            1024,               // 1 MB input file size
            1024,               // 1 MB output file size
            new UtilizationModelDynamic(0.4),   // 40% CPU utilization
            utilizationRam,     // RAM utilization model
            utilizationBw       // BW utilization model
        );
        
        // Assign cloudlet to VM 7
        cloudlet7.setVm(vmList.get(6));
        cloudletList.add(cloudlet7);
    }
    
    /**
     * Print the current state of the hosts and VMs
     */
    private void printCurrentState(String phase) {
        System.out.println("\n====== " + phase + " ======");
        
        // Print host utilization
        System.out.println("--- Host Utilization ---");
        for (Host host : hostList) {
            double cpuUtilization = utilizationMonitor.getHostCpuUtilization(host);
            System.out.printf("Host #%d: CPU Utilization = %.2f%%\n", 
                    host.getId(), cpuUtilization * 100);
            
            // Print VMs on this host
            System.out.println("  VMs running on this host:");
            for (Vm vm : host.getVmList()) {
                System.out.printf("    VM #%d (CPU Util: %.2f%%)\n", 
                        vm.getId(), vm.getCpuPercentUtilization() * 100);
            }
        }
    }
    
    /**
     * Print comparison of utilization before and after load balancing
     */
    private void printLoadBalancingComparison() {
        System.out.println("\n====== LOAD BALANCING COMPARISON ======");
        System.out.println("--- Host Utilization Comparison ---");
        System.out.println("                  Before         After       Difference");
        
        for (Host host : hostList) {
            double before = utilizationBeforeLoadBalancing.getOrDefault(host, 0.0);
            double after = utilizationAfterLoadBalancing.getOrDefault(host, 0.0);
            double difference = after - before;
            
            System.out.printf("Host #%-10d %.2f%%         %.2f%%       %s%.2f%%\n", 
                    host.getId(), 
                    before * 100, 
                    after * 100,
                    difference >= 0 ? "+" : "",
                    difference * 100);
        }
    }
    
    /**
     * Print final results of the simulation
     */
    private void printResults() {
        System.out.println("\n====== FINAL SIMULATION RESULTS ======");
        
        // Print cloudlet execution details
        CloudSimUtils.printCloudletResults(broker);
        
        // Print load balancing summary
        System.out.println("\n--- Load Balancing Summary ---");
        
        // Calculate total datacenter utilization before and after load balancing
        double totalUtilizationBefore = 0;
        double totalUtilizationAfter = 0;
        
        for (Host host : hostList) {
            totalUtilizationBefore += utilizationBeforeLoadBalancing.getOrDefault(host, 0.0);
            totalUtilizationAfter += utilizationAfterLoadBalancing.getOrDefault(host, 0.0);
        }
        
        double avgUtilizationBefore = totalUtilizationBefore / hostList.size();
        double avgUtilizationAfter = totalUtilizationAfter / hostList.size();
        
        System.out.printf("Average Host Utilization: Before = %.2f%%, After = %.2f%%\n", 
                avgUtilizationBefore * 100, avgUtilizationAfter * 100);
        
        // Calculate standard deviation to measure load distribution
        double varianceBefore = 0;
        double varianceAfter = 0;
        
        for (Host host : hostList) {
            double utilBefore = utilizationBeforeLoadBalancing.getOrDefault(host, 0.0);
            double utilAfter = utilizationAfterLoadBalancing.getOrDefault(host, 0.0);
            
            varianceBefore += Math.pow(utilBefore - avgUtilizationBefore, 2);
            varianceAfter += Math.pow(utilAfter - avgUtilizationAfter, 2);
        }
        
        double stdDevBefore = Math.sqrt(varianceBefore / hostList.size());
        double stdDevAfter = Math.sqrt(varianceAfter / hostList.size());
        
        System.out.printf("Standard Deviation (measure of imbalance): Before = %.4f, After = %.4f\n", 
                stdDevBefore, stdDevAfter);
        
        double improvementPercentage = ((stdDevBefore - stdDevAfter) / stdDevBefore) * 100;
        System.out.printf("Load Distribution Improvement: %.2f%%\n", improvementPercentage);
    }
}
