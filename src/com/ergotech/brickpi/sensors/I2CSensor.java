/*
 *  Copyright ErgoTech Systems, Inc 2014
 *
 * This file is made available online through a Creative Commons Attribution-ShareAlike 3.0  license.
 * (http://creativecommons.org/licenses/by-sa/3.0/)
 *
 *  This is a library of functions for the RPi to communicate with the BrickPi.
 */
package com.ergotech.brickpi.sensors;

import java.util.BitSet;

/**
 *
 * @author jim
 */
public class I2CSensor extends Sensor {

    @Override
    public byte getSensorType() {
        return TYPE_SENSOR_I2C;
    }

    @Override
    public int decodeValues(byte[] message, int startLocation) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
