/*
 *  Copyright ErgoTech Systems, Inc 2014
 *
 * This file is made available online through a Creative Commons Attribution-ShareAlike 3.0  license.
 * (http://creativecommons.org/licenses/by-sa/3.0/)
 *
 *  This is a library of functions for the RPi to communicate with the BrickPi.
 */
package com.ergotech.brickpi.motion;

import java.util.BitSet;

/**
 *
 * The class representing a motor.
 */
public class Motor {

    protected enum Direction {

        CLOCKWISE, COUNTER_CLOCKWISE
    };

    /**
     * The current commanded speed of the motor.
     */
    protected byte speed;

    /**
     * The current commanded direction of the motor.
     */
    protected Direction direction;

    /**
     * Whether commanded enabled.
     */
    protected boolean enabled;

//    /**
//     * Encode the data associated with the motor encoder offsets to the outgoing message.
//     * Encoder offsets are not yet supports so this encodes a single bit to the bitset
//     * 
//     * I'm not totally sure that the encoder values should be associated with the motors.
//     *
//     * @param message the BitSet representing the outgoing message.
//     * @param startLocation the starting bit location in the message at which to
//     * begin encoding
//     * @return the ending location. That is the startLocation for the next
//     * encoding.
//     */
//    public int encodeEncoderOffsetToValueRequest(BitSet message, int startLocation) {
//        return startLocation; // next bit..
//    }
    /**
     * Encode the data associated with the motor to the outgoing message. The
     * default method does nothing.
     *
     * @param message the BitSet representing the outgoing message.
     * @param startLocation the starting bit location in the message at which to
     * begin encoding
     * @return the ending location. That is the startLocation for the next
     * encoding.
     */
    public int encodeToValueRequest(BitSet message, int startLocation) {
        // here, I think, is how this bit-encoding works.
        // the value is encoded LSb first into the array
        // so, given the 4-bit value 3
        // the array would be 1100
        // that's sort of the same as saying that the value is bit-reversed
        // could be wrong here, writing the documentation before running it...
        // first the motor-enable
        message.set(startLocation++, enabled);
        // direction
        if (direction == Direction.CLOCKWISE) {  // could be reversed 
            message.set(startLocation++);
        } else {
            message.clear(startLocation++);
        }
        // speed
        byte tmpSpeed = speed;
        for (int counter = 0; counter < 8; counter++) {
            message.set(startLocation++, tmpSpeed & 0x1);
            tmpSpeed >>= 1;
        }
        return startLocation; // nothing to encode.
    }

    /**
     * Decode the data associated with the motor from the incoming message.
     *
     * @param message the BitSet representing the outgoing message.
     * @param startLocation the starting bit location in the message at which to
     * begin decoding
     * @return the ending location. That is the startLocation for the next
     * encoding.
     */
    public int decodeValues(BitSet message, int startLocation) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
