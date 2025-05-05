package cloudsim.simulation;

import cloudsim.utils.CloudSimUtils;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Special implementation of Cloud Simulation for Space-Shared policy
 * using direct host, VM, and cloudlet creation to avoid allocation issues
 */
public class SpaceSharedCloudSimulation {
    
    private final CloudSim simulation;
    private Datacenter datacenter;
    private DatacenterBroker broker;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    
    public SpaceSharedCloudSimulation() {
        this.simulation = new CloudSim();
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
    }
    
    /**
     * Run the simulation with Space-Shared policy
     */
    public void run() {
        createDatacenter();
        createBroker();
        createVms();
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
     * Create datacenter with Space-Shared host configuration
     */
    private void createDatacenter() {
        List<Host> hostList = new ArrayList<>();
        
        // Create a host with Space-Shared VM scheduler
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            peList.add(new PeSimple(1500, new PeProvisionerSimple()));
        }
        
        // Create host with specifications adjusted for Space-Shared
        long ram = 16 * 1024; // 16 GB RAM in MB
        long storage = 2000L * 1024 * 1024 * 1024; // 2000 GB in bytes
        long bw = 20000; // 20 Gbps
        
        Host host = new HostSimple(ram, bw, storage, peList);
        host.setId(0);
        host.setVmScheduler(new VmSchedulerSpaceShared());
        host.setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple());
        
        System.out.println("Created Host with Space-Shared scheduler: " + host);
        System.out.println("- RAM: " + ram + " MB");
        System.out.println("- Storage: " + storage + " bytes");
        System.out.println("- Bandwidth: " + bw + " Mbps");
        System.out.println("- Number of PEs: " + peList.size());
        System.out.println("- MIPS per PE: " + ((PeSimple)peList.get(0)).getCapacity());
        
        hostList.add(host);
        
        // Create datacenter with VM allocation policy compatible with Space-Shared
        datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        datacenter.setSchedulingInterval(1);
    }
    
    /**
     * Create broker to manage VMs and cloudlets
     */
    private void createBroker() {
        broker = new DatacenterBrokerSimple(simulation);
    }
    
    /**
     * Create VMs with Space-Shared cloudlet scheduler
     */
    private void createVms() {
        // Create two VMs with Space-Shared cloudlet scheduler
        // VM #1: 2 PEs
        Vm vm1 = new VmSimple(0, 1500, 2);
        vm1.setRam(4 * 1024) // 4 GB RAM
           .setBw(1000)      // 1 Gbps
           .setSize(100L * 1024 * 1024 * 1024) // 100 GB storage
           .setCloudletScheduler(new CloudletSchedulerSpaceShared());
        
        // VM #2: 1 PE
        Vm vm2 = new VmSimple(1, 1500, 1);
        vm2.setRam(2 * 1024) // 2 GB RAM
           .setBw(1000)      // 1 Gbps
           .setSize(50L * 1024 * 1024 * 1024) // 50 GB storage
           .setCloudletScheduler(new CloudletSchedulerSpaceShared());
        
        System.out.println("Created VMs with Space-Shared scheduler:");
        System.out.println("VM #0: " + vm1.getNumberOfPes() + " PEs, " + vm1.getRam().getCapacity() + " MB RAM");
        System.out.println("VM #1: " + vm2.getNumberOfPes() + " PEs, " + vm2.getRam().getCapacity() + " MB RAM");
        
        vmList.add(vm1);
        vmList.add(vm2);
    }
    
    /**
     * Create cloudlets compatible with Space-Shared policy
     */
    private void createCloudlets() {
        UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.5);
        
        // Create cloudlets with PE requirements compatible with our VMs
        // In Space-Shared, cloudlets will be executed in sequence if using the same resources
        
        // VM #0 has 2 PEs
        
        // Cloudlet #1: Requires 1 PE, runs on VM #0
        // Will start immediately as it only needs 1 PE
        Cloudlet cloudlet1 = new CloudletSimple(0, 50000, 1);
        cloudlet1.setFileSize(1024)
                 .setOutputSize(1024)
                 .setUtilizationModel(utilizationModel);
        cloudlet1.setVm(vmList.get(0)); // Explicitly assign to VM #0
        
        // Cloudlet #2: Requires 2 PEs, runs on VM #0
        // Will start after cloudlet1 completes (demonstrates Space-Shared scheduling)
        Cloudlet cloudlet2 = new CloudletSimple(1, 100000, 2);
        cloudlet2.setFileSize(1024)
                 .setOutputSize(1024)
                 .setUtilizationModel(utilizationModel);
        cloudlet2.setVm(vmList.get(0)); // Explicitly assign to VM #0
        
        // VM #1 has 1 PE
        
        // Cloudlet #3: Requires 1 PE, runs on VM #1
        // Will start immediately on VM #1 (demonstrates parallel execution across VMs)
        Cloudlet cloudlet3 = new CloudletSimple(2, 75000, 1);
        cloudlet3.setFileSize(1024)
                 .setOutputSize(1024)
                 .setUtilizationModel(utilizationModel);
        cloudlet3.setVm(vmList.get(1)); // Explicitly assign to VM #1
        
        System.out.println("Created Cloudlets for Space-Shared:");
        System.out.println("Cloudlet #0: " + cloudlet1.getNumberOfPes() + " PEs, " + cloudlet1.getLength() + " MI (assigned to VM #0)");
        System.out.println("Cloudlet #1: " + cloudlet2.getNumberOfPes() + " PEs, " + cloudlet2.getLength() + " MI (assigned to VM #0)");
        System.out.println("Cloudlet #2: " + cloudlet3.getNumberOfPes() + " PEs, " + cloudlet3.getLength() + " MI (assigned to VM #1)");
        System.out.println("With Space-Shared, cloudlets on the same VM wait for resources to be available.");
        System.out.println("Expected execution order: cloudlet 0 and 2 start immediately, cloudlet 1 waits for cloudlet 0.");
        
        cloudletList.add(cloudlet1);
        cloudletList.add(cloudlet2);
        cloudletList.add(cloudlet3);
    }
    
    /**
     * Print simulation results
     */
    private void printResults() {
        System.out.println("======== SIMULATION RESULTS: SPACE-SHARED POLICY ========");
        
        // Print VM allocation details
        System.out.println("--- VM Allocation ---");
        for (Vm vm : broker.getVmCreatedList()) {
            System.out.printf("VM #%d (CPU: %d, RAM: %d MB) => Host #%d\n", 
                    vm.getId(), vm.getNumberOfPes(), vm.getRam().getCapacity(), vm.getHost().getId());
        }
        
        // Print cloudlet results
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        System.out.println("\n========== CLOUDLET EXECUTION RESULTS ==========");
        System.out.println(String.format("%-10s %-10s %-15s %-15s %-15s",
                "Cloudlet", "VM", "Start Time", "Finish Time", "Execution Time"));
        
        for (Cloudlet cloudlet : finishedCloudlets) {
            System.out.println(String.format("%-10d %-10d %-15.2f %-15.2f %-15.2f",
                    cloudlet.getId(), 
                    cloudlet.getVm().getId(),
                    cloudlet.getExecStartTime(),
                    cloudlet.getFinishTime(),
                    cloudlet.getActualCpuTime()));
        }
        
        // Print summary
        System.out.println("\nTotal number of cloudlets: " + finishedCloudlets.size());
        
        // Calculate average execution time
        double totalExecutionTime = 0;
        for (Cloudlet cloudlet : finishedCloudlets) {
            totalExecutionTime += cloudlet.getActualCpuTime();
        }
        double avgExecutionTime = totalExecutionTime / finishedCloudlets.size();
        System.out.printf("Average execution time: %.2f seconds\n", avgExecutionTime);
        
        // Analyze the Space-Shared execution
        System.out.println("\n--- Space-Shared Execution Analysis ---");
        Cloudlet cl0 = finishedCloudlets.stream().filter(c -> c.getId() == 0).findFirst().orElse(null);
        Cloudlet cl1 = finishedCloudlets.stream().filter(c -> c.getId() == 1).findFirst().orElse(null);
        if (cl0 != null && cl1 != null && cl0.getVm().getId() == cl1.getVm().getId()) {
            System.out.println("Cloudlet 0 and 1 were assigned to the same VM (VM #" + cl0.getVm().getId() + "):");
            System.out.printf("- Cloudlet 0: Start=%.2f, Finish=%.2f\n", cl0.getExecStartTime(), cl0.getFinishTime());
            System.out.printf("- Cloudlet 1: Start=%.2f, Finish=%.2f\n", cl1.getExecStartTime(), cl1.getFinishTime());
            
            // Check if execution was sequential (Space-Shared behavior)
            if (Math.abs(cl1.getExecStartTime() - cl0.getFinishTime()) < 1.0) {
                System.out.println("✓ Space-Shared behavior confirmed: Cloudlet 1 started execution after Cloudlet 0 finished");
                System.out.println("  This demonstrates the non-preemptive, resource-exclusive nature of Space-Shared scheduling.");
            } else {
                System.out.println("Note: Cloudlet 1 did not start immediately after Cloudlet 0 finished.");
            }
        }
        
        // Print Space-Shared specific analysis
        System.out.println("\n--- Policy Analysis: Space-Shared ---");
        System.out.println("Space-Shared Policy Key Characteristics:");
        System.out.println("- Cloudlets are queued if resources are not available");
        System.out.println("- Once started, a cloudlet has exclusive access to allocated PEs");
        System.out.println("- Better for batch processing and CPU-intensive tasks");
        System.out.println("- Lower average completion time for high-priority tasks");
        System.out.println("- No interruption of running tasks (non-preemptive)");
        
        // Compare with Time-Shared
        System.out.println("\n--- Comparison with Time-Shared Policy ---");
        System.out.println("While Time-Shared allows all cloudlets to start immediately by sharing resources,");
        System.out.println("Space-Shared prioritizes resource dedication which can reduce overall execution time");
        System.out.println("for certain types of workloads when cloudlets need exclusive access to resources.");
        System.out.println("================================================");
    }
}
