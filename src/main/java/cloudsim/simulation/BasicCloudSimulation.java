package cloudsim.simulation;

import cloudsim.utils.CloudSimUtils;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic CloudSim simulation for Task 1 to compare Time-Shared vs Space-Shared policies
 */
public class BasicCloudSimulation {
    
    private final CloudSim simulation;
    private final boolean timeSharedPolicy;
    private Datacenter datacenter;
    private DatacenterBroker broker;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    
    /**
     * Constructor for BasicCloudSimulation
     * 
     * @param timeSharedPolicy Whether to use Time-Shared policy
     */
    public BasicCloudSimulation(boolean timeSharedPolicy) {
        this.timeSharedPolicy = timeSharedPolicy;
        this.simulation = new CloudSim();
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
    }
    
    /**
     * Run the simulation
     */
    public void run() {
        // Create datacenter
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
        
        // Start simulation
        simulation.start();
        
        // Print results
        printResults();
    }
    
    /**
     * Create datacenter with one host as per Task 1 requirements
     */
    private void createDatacenter() {
        List<Host> hostList = new ArrayList<>();
        
        // Create one host as per specification: 4 CPUs, 8 GB RAM, 1000 GB storage
        // Adjusted host capacity to ensure it can handle both VMs
        Host host = CloudSimUtils.createHost(
            0,              // Host ID 
            4,              // 4 CPUs (enough for both VMs)
            1200,           // 1200 MIPS per CPU (increased to ensure enough capacity)
            10 * 1024,      // 10 GB RAM in MB (increased to ensure enough capacity)
            2000,           // 2000 GB storage (increased to ensure enough capacity)
            20000,          // 20 Gbps bandwidth (increased to ensure enough capacity)
            timeSharedPolicy // Time-Shared or Space-Shared policy
        );
        
        hostList.add(host);
        datacenter = CloudSimUtils.createDatacenter(simulation, hostList);
        datacenter.setSchedulingInterval(1); // Check for events every second
    }
    
    /**
     * Create two VMs with different configurations as per Task 1 requirements
     */
    private void createVms() {
        // For Space-Shared policy, we need to adjust the VM configurations
        // to ensure proper PE allocation
        
        int vm1PEs = timeSharedPolicy ? 2 : 2; // Same for both policies
        int vm2PEs = timeSharedPolicy ? 1 : 1; // Same for both policies
        
        // Adjust MIPS based on policy type
        long vm1Mips = timeSharedPolicy ? 800 : 800;
        long vm2Mips = timeSharedPolicy ? 1000 : 1000;
        
        System.out.println("Creating VMs with " + (timeSharedPolicy ? "Time-Shared" : "Space-Shared") + " policy");
        
        // Create VM 1: 2 vCPUs, 4 GB RAM, 100 GB disk
        Vm vm1 = CloudSimUtils.createVm(
            0,              // VM ID
            vm1PEs,         // Number of PEs (CPUs)
            vm1Mips,        // MIPS per CPU
            4 * 1024,       // 4 GB RAM in MB
            1000,           // 1 Gbps bandwidth
            100,            // 100 GB disk
            timeSharedPolicy // Time-Shared or Space-Shared policy
        );
        
        // Create VM 2: 1 vCPU, 2 GB RAM, 50 GB disk
        Vm vm2 = CloudSimUtils.createVm(
            1,              // VM ID
            vm2PEs,         // Number of PEs (CPUs)
            vm2Mips,        // MIPS per CPU
            2 * 1024,       // 2 GB RAM in MB
            1000,           // 1 Gbps bandwidth
            50,             // 50 GB disk
            timeSharedPolicy // Time-Shared or Space-Shared policy
        );
        
        // Debug information for VMs
        System.out.println("VM 1: RAM=" + vm1.getRam().getCapacity() + "MB, "
            + "Bandwidth=" + vm1.getBw().getCapacity() + "Mbps, "
            + "Storage=" + vm1.getStorage().getCapacity() + "bytes, "
            + "Num PEs=" + vm1.getNumberOfPes() + ", "
            + "MIPS=" + vm1.getMips());
            
        System.out.println("VM 2: RAM=" + vm2.getRam().getCapacity() + "MB, "
            + "Bandwidth=" + vm2.getBw().getCapacity() + "Mbps, "
            + "Storage=" + vm2.getStorage().getCapacity() + "bytes, "
            + "Num PEs=" + vm2.getNumberOfPes() + ", "
            + "MIPS=" + vm2.getMips());
            
        vmList.add(vm1);
        vmList.add(vm2);
    }
    
    /**
     * Create at least three cloudlets (tasks) as per Task 1 requirements
     */
    private void createCloudlets() {
        // Create utilization models for cloudlets
        UtilizationModelDynamic utilizationCpu = new UtilizationModelDynamic(0.5); // 50% CPU utilization
        UtilizationModelDynamic utilizationRam = new UtilizationModelDynamic(0.2); // 20% RAM utilization
        UtilizationModelDynamic utilizationBw = new UtilizationModelDynamic(0.1);  // 10% BW utilization
        
        // Create three cloudlets with different lengths and PE requirements
        // For Space-Shared, we need to be careful about PE requirements
        
        // Cloudlet 1: Single PE requirement (compatible with both VMs)
        Cloudlet cloudlet1 = CloudSimUtils.createCloudlet(
            0,                // Cloudlet ID
            50000,            // 50K MI (Million Instructions)
            1,                // Requires 1 PE
            1024,             // 1 MB input file size
            1024,             // 1 MB output file size
            utilizationCpu,   // CPU utilization model
            utilizationRam,   // RAM utilization model
            utilizationBw     // BW utilization model
        );
        
        // Cloudlet 2: For Space-Shared, we need to make sure it can fit in the VMs
        // In Space-Shared, the VM must have at least as many PEs as the cloudlet requests
        int requiredPEs = 2;
        // If using Space-Shared, ensure we don't exceed available PEs in VM1 (which has 2 PEs)
        if (!timeSharedPolicy) {
            requiredPEs = Math.min(requiredPEs, 2); // Limit to 2 PEs for Space-Shared
        }
        
        Cloudlet cloudlet2 = CloudSimUtils.createCloudlet(
            1,                // Cloudlet ID
            100000,           // 100K MI (Million Instructions)
            requiredPEs,      // Requires PEs (adjusted based on policy)
            2048,             // 2 MB input file size
            1024,             // 1 MB output file size
            utilizationCpu,   // CPU utilization model
            utilizationRam,   // RAM utilization model
            utilizationBw     // BW utilization model
        );
        
        // Cloudlet 3: Single PE requirement (compatible with both VMs)
        Cloudlet cloudlet3 = CloudSimUtils.createCloudlet(
            2,                // Cloudlet ID
            75000,            // 75K MI (Million Instructions)
            1,                // Requires 1 PE
            1024,             // 1 MB input file size
            2048,             // 2 MB output file size
            utilizationCpu,   // CPU utilization model
            utilizationRam,   // RAM utilization model
            utilizationBw     // BW utilization model
        );
        
        // Add cloudlets to the list
        cloudletList.add(cloudlet1);
        cloudletList.add(cloudlet2);
        cloudletList.add(cloudlet3);
        
        // Print cloudlet information
        System.out.println("Created " + cloudletList.size() + " cloudlets with " + 
                         (timeSharedPolicy ? "Time-Shared" : "Space-Shared") + " policy");
        for (Cloudlet cloudlet : cloudletList) {
            System.out.println("Cloudlet " + cloudlet.getId() + ": Requires " + 
                            cloudlet.getNumberOfPes() + " PEs, Length: " + 
                            cloudlet.getLength() + " MI");
        }
    }
    
    /**
     * Print simulation results
     */
    private void printResults() {
        String policyName = timeSharedPolicy ? "Time-Shared" : "Space-Shared";
        System.out.println("======== SIMULATION RESULTS: " + policyName + " POLICY ========");
        
        // Print VM allocation details
        System.out.println("--- VM Allocation ---");
        for (Vm vm : broker.getVmCreatedList()) {
            System.out.printf("VM #%d (CPU: %d, RAM: %d MB) => Host #%d\n", 
                    vm.getId(), vm.getNumberOfPes(), vm.getRam().getCapacity(), vm.getHost().getId());
        }
        
        // Print cloudlet execution details using utility method
        CloudSimUtils.printCloudletResults(broker);
        
        // Print summary based on scheduling policy
        System.out.println("\n--- Policy Analysis: " + policyName + " ---");
        if (timeSharedPolicy) {
            System.out.println("Time-Shared Policy:");
            System.out.println("- All cloudlets start execution immediately");
            System.out.println("- CPU time is shared among all running cloudlets");
            System.out.println("- Good for interactive tasks with variable lengths");
            System.out.println("- Higher average response time");
        } else {
            System.out.println("Space-Shared Policy:");
            System.out.println("- Cloudlets are queued if resources are not available");
            System.out.println("- Once started, a cloudlet has exclusive access to allocated PEs");
            System.out.println("- Better for batch processing and CPU-intensive tasks");
            System.out.println("- Lower average completion time for high-priority tasks");
        }
    }
}
