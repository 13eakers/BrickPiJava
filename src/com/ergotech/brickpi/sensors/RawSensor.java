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
public class RawSensor extends Sensor {

    /**
     * Returns an instance of this sensor.
     */
    public RawSensor() {

    }

    @Override
    public int decodeValues(byte[] message, int startLocation) {
        value = BrickPi.decodeInt(10, message, startLocation);
        return startLocation + 10;
    }

    @Override
    public byte getSensorType() {
        return TYPE_SENSOR_RAW;
    }

}
