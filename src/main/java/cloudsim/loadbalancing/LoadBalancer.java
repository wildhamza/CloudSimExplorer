package cloudsim.loadbalancing;

import cloudsim.monitor.UtilizationMonitor;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements load balancing logic to migrate VMs from overloaded hosts
 * to underutilized hosts
 */
public class LoadBalancer {
    
    private final Datacenter datacenter;
    private final UtilizationMonitor utilizationMonitor;
    private final DatacenterBroker broker;
    
    /**
     * Constructor for LoadBalancer
     * 
     * @param datacenter        The datacenter to apply load balancing to
     * @param utilizationMonitor Utilization monitor to track host utilization
     * @param broker            Datacenter broker to facilitate VM migrations
     */
    public LoadBalancer(Datacenter datacenter, UtilizationMonitor utilizationMonitor, DatacenterBroker broker) {
        this.datacenter = datacenter;
        this.utilizationMonitor = utilizationMonitor;
        this.broker = broker;
    }
    
    /**
     * Execute load balancing by migrating VMs from overloaded hosts to underutilized ones
     */
    public void executeLoadBalancing() {
        System.out.println("Executing load balancing...");
        
        // Get all hosts
        List<Host> hosts = datacenter.getHostList();
        
        // Identify overloaded and underutilized hosts
        List<Host> overloadedHosts = hosts.stream()
                .filter(utilizationMonitor::isHostOverloaded)
                .sorted(Comparator.comparingDouble(utilizationMonitor::getHostCpuUtilization).reversed())
                .collect(Collectors.toList());
        
        List<Host> underutilizedHosts = hosts.stream()
                .filter(utilizationMonitor::isHostUnderutilized)
                .sorted(Comparator.comparingDouble(utilizationMonitor::getHostCpuUtilization))
                .collect(Collectors.toList());
        
        System.out.printf("Found %d overloaded hosts and %d underutilized hosts\n", 
                overloadedHosts.size(), underutilizedHosts.size());
        
        // No overloaded hosts or no underutilized hosts? Nothing to do
        if (overloadedHosts.isEmpty() || underutilizedHosts.isEmpty()) {
            System.out.println("No load balancing necessary at this time.");
            return;
        }
        
        // For each overloaded host, try to migrate VMs
        for (Host overloadedHost : overloadedHosts) {
            System.out.printf("Processing overloaded Host #%d (Utilization: %.2f%%)\n", 
                    overloadedHost.getId(), utilizationMonitor.getHostCpuUtilization(overloadedHost) * 100);
            
            // Sort VMs by CPU utilization (descending) to migrate the most resource-intensive ones first
            List<Vm> vmsToMigrate = overloadedHost.getVmList().stream()
                    .sorted(Comparator.<Vm, Double>comparing(vm -> vm.getCpuPercentUtilization()).reversed())
                    .collect(Collectors.toList());
            
            for (Vm vm : vmsToMigrate) {
                // Try to find a suitable underutilized host for this VM
                for (Host targetHost : underutilizedHosts) {
                    // Check if migration would be beneficial and feasible
                    if (canMigrateVm(vm, overloadedHost, targetHost)) {
                        // Perform the migration
                        System.out.printf("Migrating VM #%d from Host #%d to Host #%d\n",
                                vm.getId(), overloadedHost.getId(), targetHost.getId());
                        
                        // In newer CloudSim versions, use a broker to migrate VMs
                        datacenter.getVmAllocationPolicy().deallocateHostForVm(vm);
                        datacenter.getVmAllocationPolicy().allocateHostForVm(vm, targetHost);
                        
                        // After migration, recalculate host utilization
                        double sourceUtilAfter = utilizationMonitor.getHostCpuUtilization(overloadedHost);
                        double targetUtilAfter = utilizationMonitor.getHostCpuUtilization(targetHost);
                        
                        System.out.printf("After migration - Source Host #%d: %.2f%%, Target Host #%d: %.2f%%\n",
                                overloadedHost.getId(), sourceUtilAfter * 100, 
                                targetHost.getId(), targetUtilAfter * 100);
                        
                        // If the source host is no longer overloaded, stop migrating VMs from it
                        if (!utilizationMonitor.isHostOverloaded(overloadedHost)) {
                            System.out.printf("Host #%d is no longer overloaded (%.2f%%). Stopping VM migrations from this host.\n",
                                    overloadedHost.getId(), sourceUtilAfter * 100);
                            break;
                        }
                        
                        // If the target host is no longer underutilized, remove it from the list
                        if (!utilizationMonitor.isHostUnderutilized(targetHost)) {
                            System.out.printf("Host #%d is no longer underutilized (%.2f%%). Removing from target hosts list.\n",
                                    targetHost.getId(), targetUtilAfter * 100);
                            underutilizedHosts.remove(targetHost);
                            break;
                        }
                    }
                }
                
                // If no more underutilized hosts, stop
                if (underutilizedHosts.isEmpty()) {
                    System.out.println("No more underutilized hosts available for migration. Ending load balancing.");
                    return;
                }
            }
        }
        
        System.out.println("Load balancing completed.");
    }
    
    /**
     * Check if a VM can be migrated from source host to target host
     * 
     * @param vm The VM to migrate
     * @param sourceHost The source host
     * @param targetHost The target host
     * @return true if migration is feasible and beneficial, false otherwise
     */
    private boolean canMigrateVm(Vm vm, Host sourceHost, Host targetHost) {
        // Check if the target host has enough resources (CPU, RAM, etc.)
        if (!hasEnoughResources(targetHost, vm)) {
            return false;
        }
        
        // Estimate utilization after migration
        double sourceUtilBefore = utilizationMonitor.getHostCpuUtilization(sourceHost);
        double targetUtilBefore = utilizationMonitor.getHostCpuUtilization(targetHost);
        
        // Estimate VM's contribution to CPU utilization (simple estimate)
        double vmCpuUtilization = vm.getCpuPercentUtilization() * vm.getNumberOfPes() / sourceHost.getNumberOfPes();
        
        // Estimate utilization after migration
        double sourceUtilAfter = sourceUtilBefore - vmCpuUtilization;
        double targetUtilAfter = targetUtilBefore + (vmCpuUtilization * sourceHost.getNumberOfPes() / targetHost.getNumberOfPes());
        
        // Don't migrate if it would make the target host overloaded
        if (targetUtilAfter >= utilizationMonitor.getOverloadThreshold()) {
            return false;
        }
        
        // Migration is beneficial if:
        // 1. It reduces source host utilization below the overload threshold, OR
        // 2. It significantly reduces the imbalance between hosts
        boolean reducesOverload = sourceUtilBefore >= utilizationMonitor.getOverloadThreshold() && 
                                  sourceUtilAfter < utilizationMonitor.getOverloadThreshold();
        
        boolean reducesImbalance = (sourceUtilBefore - targetUtilBefore) > 0.2 && // at least 20% imbalance initially
                                   Math.abs(sourceUtilAfter - targetUtilAfter) < Math.abs(sourceUtilBefore - targetUtilBefore);
        
        return reducesOverload || reducesImbalance;
    }
    
    /**
     * Check if a host has enough resources to accommodate a VM
     * 
     * @param host The host to check
     * @param vm The VM to be placed on the host
     * @return true if the host has enough resources, false otherwise
     */
    private boolean hasEnoughResources(Host host, Vm vm) {
        // Check if host has enough available CPUs - using available method in newer CloudSim
        boolean hasEnoughPes = host.getFreePesNumber() >= vm.getNumberOfPes();
        
        // Check RAM
        boolean hasEnoughRam = host.getRam().getAvailableResource() >= vm.getRam().getCapacity();
        
        // Check bandwidth
        boolean hasEnoughBw = host.getBw().getAvailableResource() >= vm.getBw().getCapacity();
        
        // Check storage
        boolean hasEnoughStorage = host.getStorage().getAvailableResource() >= vm.getStorage().getCapacity();
        
        return hasEnoughPes && hasEnoughRam && hasEnoughBw && hasEnoughStorage;
    }
}
