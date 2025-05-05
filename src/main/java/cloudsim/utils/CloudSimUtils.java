package cloudsim.utils;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyBestFit;
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
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class with common methods for CloudSim simulations
 */
public class CloudSimUtils {

    /**
     * Creates a host with specified configurations
     *
     * @param id           Host ID
     * @param numCpus      Number of CPUs
     * @param mipsPerCpu   MIPS per CPU
     * @param ramInMB      RAM in MB
     * @param storageInGB  Storage in GB
     * @param bwInMbps     Bandwidth in Mbps
     * @param timeShared   Whether to use time-shared VM scheduler
     * @return             Created host
     */
    public static Host createHost(int id, int numCpus, long mipsPerCpu, int ramInMB, 
                                 int storageInGB, int bwInMbps, boolean timeShared) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < numCpus; i++) {
            peList.add(new PeSimple(mipsPerCpu, new PeProvisionerSimple()));
        }
        
        Host host;
        // Convert GB to Bytes for storage
        System.out.println("Host Storage (GB): " + storageInGB);
        long storageInBytes = (long)storageInGB * 1024L * 1024L * 1024L;
        System.out.println("Host Storage (Bytes): " + storageInBytes);
        
        System.out.println("Host RAM (MB): " + ramInMB);
        System.out.println("Host Bandwidth (Mbps): " + bwInMbps);
        System.out.println("Host CPUs: " + numCpus + " with MIPS: " + mipsPerCpu);
        
        if (timeShared) {
            host = new HostSimple(ramInMB, bwInMbps, storageInBytes, peList);
            host.setVmScheduler(new VmSchedulerTimeShared());
        } else {
            host = new HostSimple(ramInMB, bwInMbps, storageInBytes, peList);
            host.setVmScheduler(new VmSchedulerSpaceShared());
        }
        
        host.setId(id);
        // Initialize proper resource provisioners
        host.setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple());
        
        System.out.println("Created Host: " + host);
        System.out.println("- RAM Capacity: " + host.getRam().getCapacity() + "MB");
        System.out.println("- RAM Allocation Policy: " + host.getRamProvisioner().getClass().getSimpleName());
        System.out.println("- BW Capacity: " + host.getBw().getCapacity() + "Mbps");
        System.out.println("- BW Allocation Policy: " + host.getBwProvisioner().getClass().getSimpleName());
        System.out.println("- Storage Capacity: " + host.getStorage().getCapacity() + "bytes");
        System.out.println("- Number of PEs: " + host.getNumberOfPes());
        System.out.println("- VM Scheduler: " + host.getVmScheduler().getClass().getSimpleName());
        
        return host;
    }

    /**
     * Creates a Virtual Machine with specified configurations
     *
     * @param id          VM ID
     * @param numCpus     Number of CPUs
     * @param mipsPerCpu  MIPS per CPU
     * @param ramInMB     RAM in MB
     * @param bwInMbps    Bandwidth in Mbps
     * @param sizeInGB    Storage size in GB
     * @param timeShared  Whether to use time-shared cloudlet scheduler
     * @return            Created VM
     */
    public static Vm createVm(int id, int numCpus, long mipsPerCpu, int ramInMB, 
                            int bwInMbps, int sizeInGB, boolean timeShared) {
        // Convert GB to Bytes for storage
        System.out.println("Creating VM " + id + " with specifications:");
        System.out.println("- Number of CPUs: " + numCpus);
        System.out.println("- MIPS per CPU: " + mipsPerCpu);
        System.out.println("- Total MIPS: " + (mipsPerCpu * numCpus));
        System.out.println("- RAM: " + ramInMB + " MB");
        System.out.println("- Bandwidth: " + bwInMbps + " Mbps");
        System.out.println("- Storage: " + sizeInGB + " GB");
        
        long sizeInBytes = (long)sizeInGB * 1024L * 1024L * 1024L;
        
        // Create VM with proper initialization
        Vm vm = new VmSimple(id, mipsPerCpu * numCpus, numCpus);
        vm.setRam(ramInMB).setBw(bwInMbps).setSize(sizeInBytes);
        
        if (timeShared) {
            vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
            System.out.println("- Using CloudletSchedulerTimeShared");
        } else {
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            System.out.println("- Using CloudletSchedulerSpaceShared");
        }
        
        return vm;
    }

    /**
     * Creates a cloudlet (task) with specified configurations
     *
     * @param id             Cloudlet ID
     * @param length         Length of cloudlet in Million Instructions (MI)
     * @param pesNumber      Number of PEs required
     * @param fileSize       Input file size in Bytes
     * @param outputSize     Output file size in Bytes
     * @param utilizationCpu CPU utilization model
     * @param utilizationRam RAM utilization model 
     * @param utilizationBw  Bandwidth utilization model
     * @return               Created cloudlet
     */
    public static Cloudlet createCloudlet(int id, long length, int pesNumber, 
                                        int fileSize, int outputSize,
                                        UtilizationModelDynamic utilizationCpu,
                                        UtilizationModelDynamic utilizationRam,
                                        UtilizationModelDynamic utilizationBw) {
        Cloudlet cloudlet = new CloudletSimple(id, length, pesNumber);
        cloudlet.setFileSize(fileSize)
                .setOutputSize(outputSize)
                .setUtilizationModelCpu(utilizationCpu)
                .setUtilizationModelRam(utilizationRam)
                .setUtilizationModelBw(utilizationBw);
        
        return cloudlet;
    }

    /**
     * Creates a data center with specified host
     *
     * @param simulation CloudSim simulation instance
     * @param hostList   List of hosts
     * @return           Created datacenter
     */
    public static Datacenter createDatacenter(CloudSim simulation, List<Host> hostList) {
        System.out.println("Creating datacenter with " + hostList.size() + " hosts");
        for (Host host : hostList) {
            System.out.println("Host " + host.getId() + ": RAM=" + host.getRam().getCapacity() + "MB, "
                + "Bandwidth=" + host.getBw().getCapacity() + "Mbps, "
                + "Storage=" + host.getStorage().getCapacity() + "bytes, "
                + "Num PEs=" + host.getNumberOfPes());
        }
        
        // Check if we're using TimeShared or SpaceShared VM scheduler
        boolean usingTimeShared = true;
        if (!hostList.isEmpty()) {
            usingTimeShared = hostList.get(0).getVmScheduler().getClass().getSimpleName().contains("TimeShared");
        }
        
        // Select appropriate allocation policy based on scheduler type
        VmAllocationPolicy allocationPolicy;
        if (usingTimeShared) {
            allocationPolicy = new VmAllocationPolicyBestFit();
            System.out.println("Using VmAllocationPolicyBestFit for Time-Shared scheduling");
        } else {
            // For Space-Shared, use a simpler allocation policy that's more compatible
            allocationPolicy = new VmAllocationPolicySimple();
            System.out.println("Using VmAllocationPolicySimple for Space-Shared scheduling");
        }
        
        // Configure datacenter with proper characteristics
        DatacenterSimple datacenter = new DatacenterSimple(simulation, hostList, allocationPolicy);
        
        // Configure datacenter characteristics
        datacenter.setSchedulingInterval(1); // Check events every second
        
        return datacenter;
    }

    /**
     * Creates a datacenter broker
     *
     * @param simulation CloudSim simulation instance
     * @return           Created broker
     */
    public static DatacenterBroker createBroker(CloudSim simulation) {
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);
        return broker;
    }

    /**
     * Prints the cloudlet execution results
     *
     * @param broker Datacenter broker containing finished cloudlets
     */
    public static void printCloudletResults(DatacenterBroker broker) {
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        
        // Header
        System.out.println("========== CLOUDLET EXECUTION RESULTS ==========");
        System.out.println(String.format("%-10s %-10s %-15s %-15s %-15s %-15s %-15s",
                "Cloudlet", "VM", "Start Time", "Finish Time", "Execution Time", "CPU Cost", "Total Cost"));
        
        // Print each cloudlet's details
        for (Cloudlet cloudlet : finishedCloudlets) {
            System.out.println(String.format("%-10d %-10d %-15.2f %-15.2f %-15.2f %-15.4f %-15.4f",
                    cloudlet.getId(), 
                    cloudlet.getVm().getId(),
                    cloudlet.getExecStartTime(),
                    cloudlet.getFinishTime(),
                    cloudlet.getActualCpuTime(),
                    0.0, // Cost per second removed in newer CloudSim
                    0.0)); // Total cost removed in newer CloudSim
        }
        
        // Summary
        System.out.println("\nTotal number of cloudlets: " + finishedCloudlets.size());
        
        // Calculate average execution time
        double totalExecutionTime = 0;
        for (Cloudlet cloudlet : finishedCloudlets) {
            totalExecutionTime += cloudlet.getActualCpuTime();
        }
        double avgExecutionTime = totalExecutionTime / finishedCloudlets.size();
        System.out.printf("Average execution time: %.2f seconds\n", avgExecutionTime);
        
        // Cost calculation removed in newer CloudSim
        System.out.println("Cost calculations not available in this CloudSim version.");
        
        System.out.println("================================================");
    }
}
