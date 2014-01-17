/*
 *  Copyright ErgoTech Systems, Inc 2014
 *
 * This file is made available online through a Creative Commons Attribution-ShareAlike 3.0  license.
 * (http://creativecommons.org/licenses/by-sa/3.0/)
 *
 *  This is a library of functions to test the BrickPI.
 *  These should go into the Test Packages, but I'm using this integration:
 *  https://blogs.oracle.com/speakjava/entry/integrating_netbeans_for_raspberry_pi
 *  to copy the dist folder to the pi.  The dist folder does not include the tests
 *  so I'll just put them here for convenience.
 *  
 *  These are simple tests and I'm skipping using JUnit for the same reason as above.
 */
package com.ergotech.brickpi;

import com.ergotech.brickpi.motion.Motor;
import com.ergotech.brickpi.sensors.TouchSensor;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jim
 */
public class BrickPiTests {

    public static void main(String[] args) {
        BrickPi brickPi = BrickPi.getBrickPi();
        try {
            brickPi.setTimeout(20000);
        } catch (IOException ex) {
            Logger.getLogger(BrickPiTests.class.getName()).log(Level.SEVERE, null, ex);
        }
        // add touch sensors to all the ports.
        brickPi.setSensor(new TouchSensor(), 0);
        brickPi.setSensor(new TouchSensor(), 1);
        brickPi.setSensor(new TouchSensor(), 2);
        brickPi.setSensor(new TouchSensor(), 3);
        try {
            // configure the sensors
            brickPi.setupSensors();
        } catch (IOException ex) {
            Logger.getLogger(BrickPiTests.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Update Values");
        try {
            // get the updated values.
            Thread.sleep(200); // wait for the values to be read....
        } catch (InterruptedException ex) {
            Logger.getLogger(BrickPiTests.class.getName()).log(Level.SEVERE, null, ex);
        }
        // here're the values
        System.out.println("Sensors: " + brickPi.getSensor(0).getValue() + " " + brickPi.getSensor(1).getValue() + " " + brickPi.getSensor(2).getValue() + " " + brickPi.getSensor(3).getValue());

        Motor motor = new Motor();
        motor.setCommandedOutput(0);
        motor.setEnabled(true);
        motor.resetEncoder();
        brickPi.setMotor(motor, 0);
        motor.setCommandedOutput(25);
        for (int counter = 0; counter < 50; counter++) {
            try {
                System.out.println("Forward Motors: Speed " + brickPi.getMotor(0).getCurrentSpeed() + " encoder " + brickPi.getMotor(0).getCurrentEncoderValue()  + " Time " + System.currentTimeMillis() %10000);
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        motor.setCommandedOutput(-25);
        for (int counter = 0; counter < 50; counter++) {
            try {
                Thread.sleep(200);
                System.out.println("Reverse Motors: Speed " + brickPi.getMotor(0).getCurrentSpeed() + " encoder " + brickPi.getMotor(0).getCurrentEncoderValue() );
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        motor.setCommandedOutput(0);
        motor.setEnabled(false);
        try {
            // get the updated values.
            Thread.sleep(200); // wait for the values to be read....
        } catch (InterruptedException ex) {
            Logger.getLogger(BrickPiTests.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
