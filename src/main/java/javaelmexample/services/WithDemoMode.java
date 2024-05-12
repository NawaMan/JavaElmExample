package javaelmexample.services;

/**
 * Service that support Demo mode which is the ability to save its value and reset it.
 */
public interface WithDemoMode {
    
    /** Take the current snapshot. */
    public void takeSnapshot();
    
    /** Reset to use the snapshot. */
    public void resetToSnapshot();
    
}
