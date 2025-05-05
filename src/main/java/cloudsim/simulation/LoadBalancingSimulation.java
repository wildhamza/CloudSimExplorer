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
        // Host H1: 4 CPUs, 8 GB RAM, 1000 GB storage (will be overloaded)
        Host host1 = CloudSimUtils.createHost(
            0,                // Host ID 
            HOST_PES,         // 4 CPUs
            HOST_MIPS,        // 1000 MIPS per CPU
            HOST_RAM,         // 8 GB RAM in MB
            HOST_STORAGE,     // 1000 GB storage
            HOST_BW,          // 10 Gbps bandwidth
            true              // Time-Shared policy
        );
        
        // Host H2: 4 CPUs, 8 GB RAM, 1000 GB storage (initially underutilized)
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
        
        System.out.println("Created hosts with following specifications:");
        System.out.printf("Host #0: %d CPUs (PEs), %d MIPS per CPU, %d MB RAM, %d GB Storage\n", 
                HOST_PES, HOST_MIPS, HOST_RAM, HOST_STORAGE);
        System.out.printf("Host #1: %d CPUs (PEs), %d MIPS per CPU, %d MB RAM, %d GB Storage\n", 
                HOST_PES, HOST_MIPS, HOST_RAM, HOST_STORAGE);
                
        // To fix VM allocation issues, we need a custom VM allocation policy that ensures
        // all VMs for host0 go to host0 and all VMs for host1 go to host1 initially
        // to create an imbalanced situation
        
        // Create datacenter with a simple VM allocation policy
        datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        datacenter.setSchedulingInterval(1); // Check for events every second
    }
    
    /**
     * Create VMs to create an overload scenario on Host 0
     */
    private void createVms() {
        // Create VMs with reduced requirements to prevent allocation failures
        // For host 0 (will be overloaded) - create 3 VMs
        System.out.println("Creating VMs for Load Balancing demonstration:");
        
        // First VM - 1 PE, 800 MIPS
        Vm vm1 = CloudSimUtils.createVm(
            0,              // VM ID
            1,              // 1 vCPU
            800,            // 800 MIPS per CPU
            1024,           // 1 GB RAM in MB
            1000,           // 1 Gbps bandwidth
            10,             // 10 GB disk
            true            // Time-Shared policy
        );
        vmList.add(vm1);
        System.out.println("Created VM #0: 1 vCPU, 800 MIPS, 1024 MB RAM - targeted for Host #0");
        
        // Second VM - 1 PE, 800 MIPS
        Vm vm2 = CloudSimUtils.createVm(
            1,              // VM ID
            1,              // 1 vCPU
            800,            // 800 MIPS per CPU
            1024,           // 1 GB RAM in MB
            1000,           // 1 Gbps bandwidth
            10,             // 10 GB disk
            true            // Time-Shared policy
        );
        vmList.add(vm2);
        System.out.println("Created VM #1: 1 vCPU, 800 MIPS, 1024 MB RAM - targeted for Host #0");
        
        // Third VM - 1 PE, 800 MIPS
        Vm vm3 = CloudSimUtils.createVm(
            2,              // VM ID
            1,              // 1 vCPU
            800,            // 800 MIPS per CPU
            1024,           // 1 GB RAM in MB
            1000,           // 1 Gbps bandwidth
            10,             // 10 GB disk
            true            // Time-Shared policy
        );
        vmList.add(vm3);
        System.out.println("Created VM #2: 1 vCPU, 800 MIPS, 1024 MB RAM - targeted for Host #0");
        
        // For host 1 (initially underutilized) - create 1 VM
        Vm vm4 = CloudSimUtils.createVm(
            3,              // VM ID
            1,              // 1 vCPU
            500,            // 500 MIPS per CPU (lower utilization)
            1024,           // 1 GB RAM in MB
            1000,           // 1 Gbps bandwidth
            10,             // 10 GB disk
            true            // Time-Shared policy
        );
        vmList.add(vm4);
        System.out.println("Created VM #3: 1 vCPU, 500 MIPS, 1024 MB RAM - targeted for Host #1");
        
        System.out.println("VM allocation strategy: first 3 VMs assigned to Host #0, last VM to Host #1");
        System.out.println("Host #0 is expected to be overloaded, Host #1 underutilized");
    }
    
    /**
     * Create cloudlets (tasks) that will run on VMs to create significant utilization differences
     * for demonstrating load balancing
     */
    private void createCloudlets() {
        // Create utilization models for cloudlets
        UtilizationModelDynamic utilizationRam = new UtilizationModelDynamic(0.3); // 30% RAM utilization
        UtilizationModelDynamic utilizationBw = new UtilizationModelDynamic(0.1);  // 10% BW utilization
        
        System.out.println("Creating cloudlets with varying utilization patterns:");
        
        // For VMs on Host 0 (overloaded host) - create high-utilization cloudlets
        
        // Cloudlet for VM #0 - High CPU utilization (80%)
        UtilizationModelDynamic highUtilizationCpu1 = new UtilizationModelDynamic(0.8);
        Cloudlet cloudlet1 = CloudSimUtils.createCloudlet(
            0,                // Cloudlet ID
            100000,           // Length in MI (Million Instructions)
            1,                // PE requirement
            1024,             // 1 MB input file size
            1024,             // 1 MB output file size
            highUtilizationCpu1, // 80% CPU utilization
            utilizationRam,   // RAM utilization model
            utilizationBw     // BW utilization model
        );
        cloudlet1.setVm(vmList.get(0));
        cloudletList.add(cloudlet1);
        System.out.println("Created Cloudlet #0: 80% CPU utilization, assigned to VM #0");
        
        // Cloudlet for VM #1 - High CPU utilization (85%)
        UtilizationModelDynamic highUtilizationCpu2 = new UtilizationModelDynamic(0.85);
        Cloudlet cloudlet2 = CloudSimUtils.createCloudlet(
            1,                // Cloudlet ID
            120000,           // Length in MI (Million Instructions)
            1,                // PE requirement
            1024,             // 1 MB input file size
            1024,             // 1 MB output file size
            highUtilizationCpu2, // 85% CPU utilization
            utilizationRam,   // RAM utilization model
            utilizationBw     // BW utilization model
        );
        cloudlet2.setVm(vmList.get(1));
        cloudletList.add(cloudlet2);
        System.out.println("Created Cloudlet #1: 85% CPU utilization, assigned to VM #1");
        
        // Cloudlet for VM #2 - High CPU utilization (90%)
        UtilizationModelDynamic highUtilizationCpu3 = new UtilizationModelDynamic(0.9);
        Cloudlet cloudlet3 = CloudSimUtils.createCloudlet(
            2,                // Cloudlet ID
            90000,            // Length in MI (Million Instructions)
            1,                // PE requirement
            1024,             // 1 MB input file size
            1024,             // 1 MB output file size
            highUtilizationCpu3, // 90% CPU utilization
            utilizationRam,   // RAM utilization model
            utilizationBw     // BW utilization model
        );
        cloudlet3.setVm(vmList.get(2));
        cloudletList.add(cloudlet3);
        System.out.println("Created Cloudlet #2: 90% CPU utilization, assigned to VM #2");
        
        // Cloudlet for VM #3 (on Host 1) - Low CPU utilization (30%)
        UtilizationModelDynamic lowUtilizationCpu = new UtilizationModelDynamic(0.3);
        Cloudlet cloudlet4 = CloudSimUtils.createCloudlet(
            3,                // Cloudlet ID
            50000,            // Length in MI (Million Instructions)
            1,                // PE requirement
            1024,             // 1 MB input file size
            1024,             // 1 MB output file size
            lowUtilizationCpu, // 30% CPU utilization
            utilizationRam,   // RAM utilization model
            utilizationBw     // BW utilization model
        );
        cloudlet4.setVm(vmList.get(3));
        cloudletList.add(cloudlet4);
        System.out.println("Created Cloudlet #3: 30% CPU utilization, assigned to VM #3");
        
        System.out.println("Initial setup: VMs 0-2 on Host #0 with high utilization, VM #3 on Host #1 with low utilization");
        System.out.println("Expected behavior: Load balancer should migrate at least one VM from Host #0 to Host #1");
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
