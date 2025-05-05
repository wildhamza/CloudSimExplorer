package cloudsim.policies;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyBestFit;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import cloudsim.utils.CloudSimUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class compares different VM allocation policies
 */
public class VmAllocationPolicyComparison {
    
    @SuppressWarnings("unused")
    private final CloudSim simulation;
    private List<Host> hostList;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private Map<String, Double> completionTimes;
    
    /**
     * Constructor
     */
    public VmAllocationPolicyComparison() {
        this.simulation = new CloudSim();
        this.hostList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
        this.completionTimes = new HashMap<>();
    }
    
    /**
     * Compare different VM allocation policies
     */
    public void compareAllocationPolicies() {
        // Create hosts and other resources
        createResources();
        
        // Compare different policies
        System.out.println("=========== VM ALLOCATION POLICY COMPARISON ===========");
        
        comparePolicy("Simple Allocation Policy", new VmAllocationPolicySimple());
        comparePolicy("First Fit Allocation Policy", new VmAllocationPolicyFirstFit());
        comparePolicy("Best Fit Allocation Policy", new VmAllocationPolicyBestFit());
        
        // Print comparison summary
        printComparisonSummary();
    }
    
    /**
     * Create simulation resources (hosts, VMs, cloudlets)
     */
    @SuppressWarnings("unused")
    private void createResources() {
        // Create 4 hosts with varying capacities
        for (int i = 0; i < 4; i++) {
            // Create hosts with different configs to test allocation policies
            Host host = CloudSimUtils.createHost(
                i,                 // Host ID 
                4,                 // 4 CPUs
                1000 + (i * 200),  // Varying MIPS per CPU
                8 * 1024,          // 8 GB RAM in MB
                1000,              // 1000 GB storage
                10000,             // 10 Gbps bandwidth
                true               // Time-Shared policy
            );
            
            hostList.add(host);
        }
        
        // Create 10 VMs with varying requirements
        for (int i = 0; i < 10; i++) {
            Vm vm = CloudSimUtils.createVm(
                i,                  // VM ID
                (i % 4) + 1,        // 1-4 vCPUs
                800,                // 800 MIPS per CPU
                1024 + (i * 256),   // Varying RAM
                1000,               // 1 Gbps bandwidth
                (i % 5) * 20 + 10,  // Varying disk size
                true                // Time-Shared policy
            );
            
            vmList.add(vm);
        }
        
        // Create cloudlets
        for (int i = 0; i < 20; i++) {
            // Distribute cloudlets across VMs
            int vmId = i % vmList.size();
            
            // Create cloudlet with varying lengths
            Cloudlet cloudlet = CloudSimUtils.createCloudlet(
                i,                               // Cloudlet ID
                50000 + (i * 10000),             // Varying length
                (i % 2) + 1,                     // 1-2 PEs required
                1024,                            // 1 MB input file size
                1024,                            // 1 MB output file size
                new org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic(0.5), // CPU
                new org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic(0.2), // RAM
                new org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic(0.1)  // BW
            );
            
            cloudletList.add(cloudlet);
        }
    }
    
    /**
     * Compare a specific VM allocation policy
     * 
     * @param policyName Name of the policy
     * @param policy     VM allocation policy to test
     */
    private void comparePolicy(String policyName, VmAllocationPolicy policy) {
        System.out.println("\n----- Testing " + policyName + " -----");
        
        // Create new CloudSim instance for this policy
        CloudSim testSimulation = new CloudSim();
        
        // Create datacenter with the specified policy
        Datacenter datacenter = new DatacenterSimple(testSimulation, hostList, policy);
        datacenter.setSchedulingInterval(1);
        
        // Create broker
        DatacenterBroker broker = CloudSimUtils.createBroker(testSimulation);
        
        // Reset VM and Cloudlet assignments
        List<Vm> policyVmList = new ArrayList<>();
        for (Vm originalVm : vmList) {
            Vm newVm = CloudSimUtils.createVm(
                (int)originalVm.getId(),
                (int)originalVm.getNumberOfPes(),
                (long)originalVm.getMips(),
                (int)originalVm.getRam().getCapacity(),
                (int)originalVm.getBw().getCapacity(),
                (int)(originalVm.getStorage().getCapacity() / (1024 * 1024 * 1024)), // Convert bytes to GB
                originalVm.getCloudletScheduler() instanceof org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared
            );
            policyVmList.add(newVm);
        }
        
        List<Cloudlet> policyCloudletList = new ArrayList<>();
        for (Cloudlet originalCloudlet : cloudletList) {
            Cloudlet newCloudlet = CloudSimUtils.createCloudlet(
                (int)originalCloudlet.getId(),
                originalCloudlet.getLength(),
                (int)originalCloudlet.getNumberOfPes(),
                (int)originalCloudlet.getFileSize(),
                (int)originalCloudlet.getOutputSize(),
                new org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic(0.5), // Use new instances instead of casting
                new org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic(0.2),
                new org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic(0.1)
            );
            policyCloudletList.add(newCloudlet);
        }
        
        // Submit VMs and cloudlets to broker
        broker.submitVmList(policyVmList);
        broker.submitCloudletList(policyCloudletList);
        
        // Start simulation
        long startTime = System.currentTimeMillis();
        testSimulation.start();
        long simulationTime = System.currentTimeMillis() - startTime;
        
        // Calculate metrics
        double totalCompletionTime = 0;
        double maxCompletionTime = 0;
        
        // Check if any cloudlets were finished
        if (!broker.getCloudletFinishedList().isEmpty()) {
            for (Cloudlet cloudlet : broker.getCloudletFinishedList()) {
                double completionTime = cloudlet.getFinishTime() - cloudlet.getExecStartTime();
                totalCompletionTime += completionTime;
                maxCompletionTime = Math.max(maxCompletionTime, completionTime);
            }
        }
        
        double averageCompletionTime = broker.getCloudletFinishedList().isEmpty() ? 0 :
                                       totalCompletionTime / broker.getCloudletFinishedList().size();
        
        // Print results for this policy
        System.out.println("VM allocation results:");
        System.out.println("  Total simulation time: " + simulationTime + " ms");
        System.out.printf("  Average cloudlet completion time: %.2f seconds\n", averageCompletionTime);
        System.out.printf("  Makespan (total execution time): %.2f seconds\n", maxCompletionTime);
        
        // Print VM allocation distribution
        Map<Integer, Integer> hostAllocationCount = new HashMap<>();
        for (Vm vm : broker.getVmCreatedList()) {
            int hostId = (int)vm.getHost().getId();
            hostAllocationCount.put(hostId, hostAllocationCount.getOrDefault(hostId, 0) + 1);
        }
        
        System.out.println("  VM distribution across hosts:");
        for (Host host : hostList) {
            int count = hostAllocationCount.getOrDefault(host.getId(), 0);
            System.out.printf("    Host #%d: %d VMs\n", host.getId(), count);
        }
        
        // Store the average completion time for comparison
        completionTimes.put(policyName, averageCompletionTime);
    }
    
    /**
     * Print summary comparing all policies
     */
    private void printComparisonSummary() {
        System.out.println("\n=========== POLICY COMPARISON SUMMARY ===========");
        System.out.println("Average Cloudlet Completion Times:");
        
        // Find best policy (lowest average completion time)
        String bestPolicy = null;
        double bestTime = Double.MAX_VALUE;
        
        for (Map.Entry<String, Double> entry : completionTimes.entrySet()) {
            System.out.printf("  %s: %.2f seconds\n", entry.getKey(), entry.getValue());
            
            if (entry.getValue() < bestTime) {
                bestTime = entry.getValue();
                bestPolicy = entry.getKey();
            }
        }
        
        System.out.println("\nBest performing policy: " + bestPolicy);
        
        // Provide recommendations
        System.out.println("\nRecommendations:");
        System.out.println("- Simple Allocation Policy: Good for simple environments with homogeneous workloads.");
        System.out.println("- First Fit Allocation Policy: Efficient for environments where quick allocation is needed.");
        System.out.println("- Best Fit Allocation Policy: Better resource utilization in heterogeneous environments.");
        System.out.println("\nFor load balancing purposes, the policy should be selected based on:");
        System.out.println("1. The heterogeneity of VM resource requirements");
        System.out.println("2. The variability of the workload");
        System.out.println("3. The need for energy efficiency vs. performance");
    }
}
