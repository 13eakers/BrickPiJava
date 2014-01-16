/*
 *  Copyright ErgoTech Systems, Inc 2014
 *
 * This file is made available online through a Creative Commons Attribution-ShareAlike 3.0  license.
 * (http://creativecommons.org/licenses/by-sa/3.0/)
 *
 *  This is a library of functions for the RPi to communicate with the BrickPi.
 */
package com.ergotech.brickpi.sensors;

import com.ergotech.brickpi.BrickPi;
import java.util.BitSet;

/**
 * Representation of a Touch Sensor.
 */
public class TouchSensor extends Sensor {

    /**
     * The current value of the sensor.
     */
    protected boolean set;

    /**
     * Returns an instance of this sensor.
     */
    public TouchSensor() {

    }

    @Override
    public int decodeValues(byte[] message, int startLocation) {
        set = (BrickPi.decodeInt(1, message, startLocation++) != 0);
        return startLocation;
    }

    @Override
    public byte getSensorType() {
        return TYPE_SENSOR_TOUCH;
    }

    /**
     * Returns the last value read from the sensor, or false if a value has not
     * been read.
     *
     * @return the last value read from the sensor.
     */
    public boolean isSet() {
        return set;
    }
    
     /**
     * Returns the 1 or 0 for consistency with the sensor interface.
     */
    public int getValue() {
        return set ? 1 : 0;
    }


}
