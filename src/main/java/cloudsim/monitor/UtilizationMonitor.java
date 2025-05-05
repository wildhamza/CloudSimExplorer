package cloudsim.monitor;

import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.Map;

/**
 * Monitors CPU utilization of hosts in a datacenter
 */
public class UtilizationMonitor {
    
    private final Datacenter datacenter;
    private final double overloadThreshold;
    private final double underutilizedThreshold;
    
    /**
     * Constructor for UtilizationMonitor
     * 
     * @param datacenter            The datacenter to monitor
     * @param overloadThreshold     CPU utilization threshold to consider a host overloaded (e.g., 0.8 for 80%)
     * @param underutilizedThreshold CPU utilization threshold to consider a host underutilized (e.g., 0.5 for 50%)
     */
    public UtilizationMonitor(Datacenter datacenter, double overloadThreshold, double underutilizedThreshold) {
        this.datacenter = datacenter;
        this.overloadThreshold = overloadThreshold;
        this.underutilizedThreshold = underutilizedThreshold;
    }
    
    /**
     * Get the current CPU utilization of a host
     * 
     * @param host The host to check
     * @return CPU utilization as a value between 0.0 and 1.0
     */
    public double getHostCpuUtilization(Host host) {
        if (host.getVmList().isEmpty()) {
            return 0.0;
        }
        
        double totalCpuUtilization = 0.0;
        
        for (Vm vm : host.getVmList()) {
            // Get VM's CPU utilization and factor in the number of PEs it's using
            double vmUtilization = vm.getCpuPercentUtilization() * vm.getNumberOfPes() / host.getNumberOfPes();
            totalCpuUtilization += vmUtilization;
        }
        
        return Math.min(1.0, totalCpuUtilization); // Cap at 100%
    }
    
    /**
     * Check if a host is overloaded (CPU utilization above threshold)
     * 
     * @param host The host to check
     * @return true if the host is overloaded, false otherwise
     */
    public boolean isHostOverloaded(Host host) {
        return getHostCpuUtilization(host) >= overloadThreshold;
    }
    
    /**
     * Check if a host is underutilized (CPU utilization below threshold)
     * 
     * @param host The host to check
     * @return true if the host is underutilized, false otherwise
     */
    public boolean isHostUnderutilized(Host host) {
        return getHostCpuUtilization(host) < underutilizedThreshold;
    }
    
    /**
     * Get the current utilization for all hosts in the datacenter
     * 
     * @return Map of hosts to their CPU utilization
     */
    public Map<Host, Double> getHostUtilization() {
        Map<Host, Double> utilizationMap = new HashMap<>();
        
        for (Host host : datacenter.getHostList()) {
            utilizationMap.put(host, getHostCpuUtilization(host));
        }
        
        return utilizationMap;
    }
    
    /**
     * Get the overload threshold used by this monitor
     * 
     * @return The CPU utilization threshold for considering a host overloaded
     */
    public double getOverloadThreshold() {
        return overloadThreshold;
    }
    
    /**
     * Get the underutilized threshold used by this monitor
     * 
     * @return The CPU utilization threshold for considering a host underutilized
     */
    public double getUnderutilizedThreshold() {
        return underutilizedThreshold;
    }
    
    /**
     * Print the current utilization of all hosts in the datacenter
     */
    public void printDatacenterUtilization() {
        System.out.println("====== DATACENTER UTILIZATION REPORT ======");
        
        for (Host host : datacenter.getHostList()) {
            double cpuUtil = getHostCpuUtilization(host);
            int vmCount = host.getVmList().size();
            
            System.out.printf("Host #%d: CPU Utilization = %.2f%% (%d VMs)\n", 
                    host.getId(), cpuUtil * 100, vmCount);
            
            // Print utilization contribution from each VM
            if (!host.getVmList().isEmpty()) {
                System.out.println("  VM contributions:");
                for (Vm vm : host.getVmList()) {
                    double vmContribution = vm.getCpuPercentUtilization() * vm.getNumberOfPes() / host.getNumberOfPes();
                    System.out.printf("    VM #%d: %.2f%% (using %d PEs)\n", 
                            vm.getId(), vmContribution * 100, vm.getNumberOfPes());
                }
            }
        }
        
        // Calculate average utilization
        double totalUtilization = 0.0;
        for (Host host : datacenter.getHostList()) {
            totalUtilization += getHostCpuUtilization(host);
        }
        double avgUtilization = totalUtilization / datacenter.getHostList().size();
        
        System.out.printf("\nAverage Host Utilization: %.2f%%\n", avgUtilization * 100);
        System.out.println("===========================================");
    }
}
