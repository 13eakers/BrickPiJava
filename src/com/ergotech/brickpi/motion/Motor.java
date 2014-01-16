/*
 *  Copyright ErgoTech Systems, Inc 2014
 *
 * This file is made available online through a Creative Commons Attribution-ShareAlike 3.0  license.
 * (http://creativecommons.org/licenses/by-sa/3.0/)
 *
 *  This is a library of functions for the RPi to communicate with the BrickPi.
 */
package com.ergotech.brickpi.motion;

import com.ergotech.brickpi.BrickPi;
import static com.ergotech.brickpi.BrickPi.decodeInt;
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
    protected int commandedSpeed;

    /**
     * The number of ticks per revolution of the motor.
     */
    protected int ticksPerRevolution;

    /**
     * The current value as read from the encoder. This value may be
     * Integer.MAX_VALUE if the encoder has not been read, does not exist or is
     * otherwise invalid.
     *
     */
    protected int currentEncoderValue;

    /**
     * The current speed as read from the encoder. This value may be
     * Integer.MAX_VALUE if the encoder has not been read, does not exist or is
     * otherwise invalid.
     *
     */
    protected double currentSpeed;

    /**
     * Last reading time. The last time the encoder value was update. Used to
     * calculate the speed.
     */
    protected long lastReadingTime;

    /**
     * The current commanded direction of the motor.
     */
    protected Direction direction;

    /**
     * Whether commanded enabled.
     */
    protected boolean enabled;

    /**
     * Create the motor.
     */
    public Motor() {
        currentSpeed = Double.MAX_VALUE;
        currentEncoderValue = Integer.MAX_VALUE;
        ticksPerRevolution = 720;
    }

    /**
     * The number of ticks per revolution of the motor. This is used to
     * calculate the motor speed in RPM.
     *
     * @return The number of ticks per revolution of the motor
     */
    public int getTicksPerRevolution() {
        return ticksPerRevolution;
    }

    /**
     * Sets the number of ticks per revolution of the motor
     *
     * @param ticksPerRevolution The number of ticks per revolution of the motor
     */
    public void setTicksPerRevolution(int ticksPerRevolution) {
        this.ticksPerRevolution = ticksPerRevolution;
    }

    /**
     * returns the last raw encoder value read from the brick pi.
     *
     * @return the current raw encoder value.
     */
    public int getCurrentEncoderValue() {
        return currentEncoderValue;
    }

    /**
     * The calculated current speed. This will return -1 if the speed is
     * unknown. Counter clockwise speeds will be negative.
     *
     * @return the current calculated speed or -1 if the speed is unknown.
     */
    public double getCurrentSpeed() {
        // internally we keep a positive speed.
        double tmp = currentSpeed;
        if (currentSpeed == Double.MAX_VALUE) {
            tmp = -1;
        } else {
            if (getDirection() == Direction.COUNTER_CLOCKWISE) {
                tmp = -tmp;
            }
        }
        return tmp;
    }

    public long getLastReadingTime() {
        return lastReadingTime;
    }

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
        int tmpSpeed = commandedSpeed;
        for (int counter = 0; counter < 8; counter++) {
            message.set(startLocation++, (tmpSpeed & 0x1) == 1);
            tmpSpeed >>= 1;
        }
        return startLocation; // nothing to encode.
    }

    /**
     * Decode the encoder data associated with the motor from the incoming
     * message. This will set the currentSpeed variable.
     *
     * @param wordLength
     * @param message the BitSet representing the outgoing message.
     * @param startLocation the starting bit location in the message at which to
     * begin decoding
     */
    public void decodeValues(int wordLength, byte[] message, int startLocation) {
        long currentTime = System.currentTimeMillis();
        int tmpEncoderValue = decodeInt(wordLength, message, startLocation);
        if (isEnabled()) { // don't calculate the speed if we're not enabled...
            if (currentEncoderValue != Integer.MAX_VALUE) {
                double readingDifference = currentEncoderValue - tmpEncoderValue;
                long timeDifference = currentTime - lastReadingTime;
                double immediateSpeed = Math.abs(readingDifference / timeDifference / ticksPerRevolution * 1000);
//            // could run a little low-pass filtering here, but it needs to be corrected
//            // for direction changes, etc.
//            if (currentSpeed == Double.MAX_VALUE) {
//                currentSpeed = immediateSpeed;
//            } else {
//                currentSpeed = Math.abs((currentSpeed * 4 + immediateSpeed) / 5);
//            }
                currentSpeed = immediateSpeed;
            }
        }
        lastReadingTime = currentTime;
        currentEncoderValue = tmpEncoderValue;
    }

    /**
     * Returns the commanded speed
     *
     * @return the commanded speed
     */
    public int getCommandedSpeed() {
        int tmp = commandedSpeed;
        if (getDirection() == Direction.COUNTER_CLOCKWISE) {
            tmp = -tmp;
        }
        return tmp;

    }

    /**
     * Set the commanded speed. For convenience the speed is set as an int
     * although the max is still 255.
     *
     * @param commandedSpeed
     */
    public void setCommandedSpeed(int commandedSpeed) {
        // internally we'll keep a positive speed.
        if (commandedSpeed < 0) {
            this.commandedSpeed = -commandedSpeed;
            setDirection(Direction.COUNTER_CLOCKWISE);
        } else {
            this.commandedSpeed = commandedSpeed;
            setDirection(Direction.CLOCKWISE);
        }
        // wake up the update thread so that the values are immediately send to the brick pi
        synchronized (BrickPi.getBrickPi()) {
            BrickPi.getBrickPi().notify();
        }

    }

    /**
     * Returns the direction.
     *
     * @return
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Set the direction.
     *
     * @param direction
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
        // wake up the update thread so that the values are immediately send to the brick pi
        synchronized (BrickPi.getBrickPi()) {
            BrickPi.getBrickPi().notify();
        }
    }

    /**
     * Returns the state of the enabled flag.
     *
     * @return true if the motor is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the motor
     *
     * @param enabled set to true to enable the motor.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        // wake up the update thread so that the values are immediately send to the brick pi
        synchronized (BrickPi.getBrickPi()) {
            BrickPi.getBrickPi().notify();
        }
    }

}
