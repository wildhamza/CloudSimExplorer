package cloudsim;

import cloudsim.simulation.BasicCloudSimulation;
import cloudsim.simulation.LoadBalancingSimulation;
import org.cloudsimplus.util.Log;

/**
 * Main class to run the CloudSim simulations
 */
public class Main {
    public static void main(String[] args) {
        // Configure logging to reduce verbosity
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN");
        
        System.out.println("======== TASK 1: BASIC CLOUD SIMULATION ========");
        // Run basic cloud simulation with Time-Shared policy
        System.out.println("\n=== Running simulation with Time-Shared policy ===");
        BasicCloudSimulation timeSharedSimulation = new BasicCloudSimulation(true);
        timeSharedSimulation.run();
        
        // Run basic cloud simulation with Space-Shared policy
        System.out.println("\n=== Running simulation with Space-Shared policy ===");
        BasicCloudSimulation spaceSharedSimulation = new BasicCloudSimulation(false);
        spaceSharedSimulation.run();
        
        System.out.println("\n======== TASK 2: LOAD BALANCING SIMULATION ========");
        LoadBalancingSimulation loadBalancingSimulation = new LoadBalancingSimulation();
        loadBalancingSimulation.run();
    }
}
