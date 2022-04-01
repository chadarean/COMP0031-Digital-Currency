package src.TODA;

import java.util.concurrent.TimeUnit;

import src.TODA.Relay;

public class RelaySchedulerTest {

    public static void testRelayScheduler0() {
        Relay r = new Relay(10, TimeUnit.SECONDS);

        try {
            TimeUnit.SECONDS.sleep(15);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        int cycles = r.getNoOfCyclesIssued();
        System.out.println(cycles);
        if(cycles < 2) {
            throw new RuntimeException("Less cycles issued than expected!");
        } else if(cycles > 2) {
            throw new RuntimeException("More cycles issued than expected!");
        }
    }

    public static void testRelayScheduler1() {
        Relay r = new Relay(10, TimeUnit.SECONDS);
        
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int cycles = r.getNoOfCyclesIssued();
        if(cycles < 1) {
            throw new RuntimeException("Less cycles issued than expected!");
        } else if(cycles > 1) {
            throw new RuntimeException("More cycles issued than expected!");
        }
    }

    public static void main(String[] args) {
        testRelayScheduler0();
        testRelayScheduler1();
        System.out.println("Test passed!");
    }
}
