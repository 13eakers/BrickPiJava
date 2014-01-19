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

/**
 * Representation of a Touch Sensor.
 */
public class UltraSonicSensor extends Sensor {

    /**
     * Returns an instance of this sensor.
     */
    public UltraSonicSensor() {

    }

    @Override
    public int decodeValues(byte[] message, int startLocation) {
        value = BrickPi.decodeInt(8, message, startLocation);
        return startLocation + 8;
    }

    @Override
    public byte getSensorType() {
        return TYPE_SENSOR_ULTRASONIC_CONT;
    }

}
