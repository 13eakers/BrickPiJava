/*
 *  Copyright ErgoTech Systems, Inc 2014
 *
 * This file is made available online through a Creative Commons Attribution-ShareAlike 3.0  license.
 * (http://creativecommons.org/licenses/by-sa/3.0/)
 *
 *  This is a library of functions for the RPi to communicate with the BrickPi.
 */
package com.ergotech.brickpi;

import com.ergotech.brickpi.motion.Motor;
import com.ergotech.brickpi.sensors.RawSensor;
import com.ergotech.brickpi.sensors.Sensor;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPortException;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides utility method for communication with the brick pi.
 */
public class BrickPi {

    /**
     * The current debug level.
     */
    public static int DEBUG_LEVEL = 0;

    /**
     * It would seem to be a desirable, and fairly likely feature that the brick
     * pis could be made stackable. In this case we will have multiple slaves on
     * the serial port. Currently this is not the case and we have only two, but
     * just to simplify future changes, I'll make this a constant.
     */
    public static final int SERIAL_TARGETS = 2;

    /**
     * Change the UART address.
     */
    public static final byte MSG_TYPE_CHANGE_ADDR = 1;
    /**
     * Change/set the sensor type.
     */
    public static final byte MSG_TYPE_SENSOR_TYPE = 2;
    /**
     * Set the motor speed and direction, and return the sesnors and encoders.
     */
    public static final byte MSG_TYPE_VALUES = 3;
    /**
     * Float motors immediately
     */
    public static final byte MSG_TYPE_E_STOP = 4;
    /**
     * Set the timeout
     */
    public static final byte MSG_TYPE_TIMEOUT_SETTINGS = 5;

    /**
     * The singleton instance of this class.
     */
    protected static BrickPi brickPi;

    /**
     * Serial port instance.
     */
    protected final Serial serial;

    /**
     * The addresses of the 2 brick pi atmel chips. At this point in development
     * I have not yet found a reason why these should be exposed to the user at
     * all. If I find a reason, I'll expose them (maybe a future brick pi design
     * will need it).
     */
    protected byte[] serialAddresses;

    /**
     * The array of sensors.
     */
    protected Sensor[] sensorType;

    /**
     * The array of motors.
     */
    protected Motor[] motors;

    /**
     * The thread that calls "updateValues" frequently. This thread is started
     * on "setupSensors".
     */
    protected Thread updateValuesThread;

    /**
     * How frequently to call updateValues. This is in milliseconds. A value of
     * zero (or less) stops the updates. Actually, the thread waits this amount
     * of time after the previous call before calling again, so the update rate
     * will be slightly slower by the amount of time it take to complete the
     * call. Additionally, the thread may be woken up, eg by configuring a motor
     * to ensure that the value is passed to the BrickPi.
     */
    protected volatile int updateDelay;

    /**
     * Return the brick pi singleton.
     *
     * @return the brick pi instance.
     */
    public static BrickPi getBrickPi() {
        if (brickPi == null) {
            try {
                // we'll try/catch the exception and log it here.
                // the "getBrickPi" could be called often and should not
                // fail (at least after initial debugging) and catch the 
                // exception externally might be irritating after a while...
                brickPi = new BrickPi();

            } catch (IOException ex) {
                Logger.getLogger(BrickPi.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        return brickPi;

    }

    /**
     * Create the brick pi instance. This will only occur on the "getBrickPi"
     * call, and only if it has not already been created.
     *
     * @throws java.io.IOException thrown if serial open throws a
     * SerialPortException
     */
    protected BrickPi() throws IOException {
        updateDelay = 0;
        try {
            serial = SerialFactory.createInstance();
            //System.out.println ("Port opening... "  + com.pi4j.wiringpi.Serial.serialOpen("/dev/ttyAMA0", 500000));
            System.out.println("Opening Serial Port");
            serial.open("/dev/ttyAMA0", 500000);
            System.out.println("port opened");
            // the listener thread in PI4J currently waits 100ms to see if data is available.
            // That's a little long for BrickPi applications.
        } catch (SerialPortException se) {
            // never let a runtime exception pass.  It can crash a whole application
            // since you won't necessarily see it until it fails...
            System.out.println(se.getMessage());
            se.printStackTrace();
            throw new IOException("Failed to open communications to BrickPi");
        }
        serialAddresses = new byte[SERIAL_TARGETS];
        serialAddresses[0] = 1;  // problem if SERIAL_TARGETS is not 2
        serialAddresses[1] = 2;
        sensorType = new Sensor[SERIAL_TARGETS * 2];
        motors = new Motor[SERIAL_TARGETS * 2];
        updateDelay = 100;
    }

    /**
     * Send a packet to the brick pi.
     *
     * @param destinationAddress
     * @param packet
     */
    protected void sendToBrickPi(byte destinationAddress, byte[] packet) {
        // clear the read buffer before writing...
        serial.flush();
        // the checksum is the sum of all the bytes in the entire packet EXCEPT the checksum
        int checksum = destinationAddress + packet.length;
        for (byte toAdd : packet) {
            checksum += (int) (toAdd & 0xFF);
        }
        byte[] toSend = new byte[packet.length + 3];
        System.arraycopy(packet, 0, toSend, 3, packet.length);
        toSend[0] = destinationAddress;
        toSend[1] = (byte) (checksum & 0xFF);  // checksum...
        toSend[2] = (byte) (packet.length & 0xFF);
        if (DEBUG_LEVEL > 0) {
            StringBuffer output = new StringBuffer();
            output.append("Sending");
            for (byte toAdd : toSend) {
                output.append(" ");
                output.append(Integer.toHexString(toAdd & 0xFF));
            }
            System.out.println(output.toString());
        }
        serial.write(toSend);
        //serial.write(packet);
    }

    /**
     * Read a packet from the brick pi.
     *
     * @param timeout total read timeout in ms
     * @return the packet read from the serial port/brickpi
     * @throws java.io.IOException thrown if there's a timeout reading the port.
     */
    protected byte[] readFromBrickPi(int timeout) throws IOException { // timeout in mS

        int delay = timeout / 5;  // we'll wait a maximum of timeout
        while (serial.availableBytes() < 2 && delay-- >= 0) { // we need at least the checksum and bytecount (2 bytes)
            try {
                Thread.sleep(5);  // 5ms

            } catch (InterruptedException ex) {
                Logger.getLogger(BrickPi.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (serial.availableBytes() < 1) {
            throw new IOException("Read timed out - Header");
        }

        // the first byte of the recieved packet in the checksum.
        // the second is the number of bytes in the packet.
        byte checksum = (byte) serial.read();
        byte packetSize = (byte) serial.read();  // the packet size does not include this two byte header.
        int inCheck = packetSize;  // the incoming checksum does not include the checksum...

        // so, we have packetSize bytes left to read.
        // delay should still be good.  If we had to wait above, it will be less than timeout/5
        // but the overall timeout in the method should still max out at timeout.
        while (serial.availableBytes() < packetSize && delay-- >= 0) { // we need at least the checksum and bytecount (2 bytes)
            try {
                Thread.sleep(5);  // 5ms

            } catch (InterruptedException ex) {
                Logger.getLogger(BrickPi.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (serial.availableBytes() < packetSize) {
            throw new IOException("Read timed out - Packet");
        }

        byte[] packet = new byte[packetSize];
        for (int counter = 0; counter < packetSize; counter++) {
            packet[counter] = (byte) (serial.read() & 0xFF);
            inCheck += (int) (packet[counter] & 0xFF);
        }
        if (DEBUG_LEVEL > 0) {
            StringBuffer input = new StringBuffer();
            input.append("Received ");
            input.append(Integer.toHexString(checksum & 0xFF));
            input.append(" ");
            input.append(Integer.toHexString(packetSize & 0xFF));
            for (byte received : packet) {
                input.append(" ");
                input.append(Integer.toHexString(received & 0xFF));
            }
            System.out.println(input.toString());
        }

        if ((inCheck & 0xFF) != (checksum & 0xFF)) {
            throw new IOException("Bad Checksum " + inCheck + " expected " + checksum);
        }
        // if we get to here, all is well.
        return packet;
    }

    /**
     * Sets the motor timeout. This is a watchdog. If the brickpi has not seen a
     * message from the pi in this amount of time the motors will gracefully
     * halt.
     *
     * @param timeout the timeout in microseconds (us).
     * @throws java.io.IOException thrown if the message transaction fails.
     */
    public void setTimeout(long timeout) throws IOException {
        byte[] packet = new byte[5];
        packet[0] = MSG_TYPE_TIMEOUT_SETTINGS;
        packet[1] = (byte) (timeout & 0xFF);
        packet[2] = (byte) ((timeout >> 8) & 0xFF);
        packet[3] = (byte) ((timeout >> 16) & 0xFF);
        packet[4] = (byte) ((timeout >> 24) & 0xFF);
        for (int counter = 0; counter < SERIAL_TARGETS; counter++) {
            serialTransactionWithRetry(counter, packet, 100);
        }
    }

    /**
     * Returns the current update delay.
     */
    public int getUpdateDelay() {
        return updateDelay;
    }

    /**
     * Sets the current update delay.
     */
    public void setUpdateDelay(int updateDelay) {
        this.updateDelay = updateDelay;
    }

    /**
     * Set the sensor at the particular port. There are current four sensor
     * ports.
     *
     * @param sensor the sensor to associate with the port. May be null to clear
     * the sensor configuration.
     * @param port the port. This, currently, should be 0-3. Values outside that
     * range will throw an IndexOutOfBoundsException.
     */
    public void setSensor(Sensor sensor, int port) {
        sensorType[port] = sensor;
    }

    /**
     * Returns the sensor attached to a particular port. This method will not
     * return null. If a sensor has not previously been attached to the port, a
     * RawSensor will be created, attached and returned.
     *
     * @param port the port associated with the requested sensor.
     * @return a valid Sensor object. If no sensor is current associated with
     * the port a RawSensor will be returned.
     */
    public Sensor getSensor(int port) {
        if (sensorType[port] == null) {
            sensorType[port] = new RawSensor();
        }
        return sensorType[port];
    }

    /**
     * Set the motor at the particular port. There are current four motor ports.
     *
     * @param motor the motor to associate with the port. May be null to clear
     * the motor configuration.
     * @param port the port. This, currently, should be 0-3. Values outside that
     * range will throw an IndexOutOfBoundsException.
     */
    public void setMotor(Motor motor, int port) {
        motors[port] = motor;
    }

    /**
     * Returns the motor attached to a particular port. This method may return
     * null.
     *
     * @param port the port associated with the requested motor.
     * @return a valid motor object or null
     */
    public Motor getMotor(int port) {
        return motors[port];
    }

    /**
     * Configure the sensors.
     *
     * @throws java.io.IOException thrown if no response is received from the
     * BrickPi
     */
    public void setupSensors() throws IOException {
        for (int counter = 0; counter < SERIAL_TARGETS; counter++) {
            int startingBitLocation = 0;
            byte[] packet;
            // we're going to use a BitSet to pack the bits.
            BitSet sensorData = new BitSet();
            for (int sensorCount = 0; sensorCount < 2; sensorCount++) {
                Sensor currentSensor = sensorType[counter * 2 + sensorCount];
                if (currentSensor != null) {
                    // request that each sensor encode itself into the packet.
                    currentSensor.encodeToSetup(sensorData, startingBitLocation);
                }
            }
            byte[] sensorBytes = sensorData.toByteArray();
            // create a packet of the correct size and fill in the header data.
            packet = new byte[sensorBytes.length + 3];
            System.arraycopy(sensorBytes, 0, packet, 3, sensorBytes.length);
            packet[0] = MSG_TYPE_SENSOR_TYPE;
            // fill in bytes 1 & 2 the sensor types. Counter is still the serial target
            // sensor count is 1 or 2 so the second or third byte in the message.
            for (int sensorCount = 0; sensorCount < 2; sensorCount++) {
                if (sensorType[counter * 2 + sensorCount] == null) {
                    packet[1 + sensorCount] = 0;
                } else {
                    packet[1 + sensorCount] = sensorType[counter * 2 + sensorCount].getSensorType();
                }
            }
            serialTransactionWithRetry(counter, packet, 2500);
            // should probably check the response here...
        }
        // set up the polling thread
        if (updateDelay > 0 && updateValuesThread == null) {
            Runnable update = new Runnable() {

                @Override
                public void run() {
                    try {
                        while (updateDelay > 0) {
                            try {
                                updateValues();
                                synchronized (BrickPi.this) {
                                    BrickPi.this.wait(updateDelay);
                                }
                            } catch (ThreadDeath td) {
                                throw td;  // don't know whether this is still required by Java - used to be.
                            } catch (Throwable any) {
                                Logger.getLogger(BrickPi.class.getName()).log(Level.SEVERE, null, any);
                            }
                        }  // end of while
                    } finally {
                        updateValuesThread = null;  // reset this so that the thread can be restarted in the future.
                    }
                }

            };
            updateValuesThread = new Thread(update, "Update Values Thread");
            // not sure about this.  If it's daemon then when the application exits
            // this thread will also exit.
            // If it's not deamon, then it can be used to keep the application running
            // which means that no other thread needs to do this.
            updateValuesThread.setDaemon(true);
            updateValuesThread.start();
        }

    }

    /**
     * Poll the BrickPi for new values.
     */
    public void updateValues() throws IOException {
        for (int counter = 0; counter < SERIAL_TARGETS; counter++) {
            int startingBitLocation = 0;
            byte[] packet;
            // we're going to use a BitSet to pack the bits.
            BitSet pollingData = new BitSet();
            // encoder offsets are not supported.  This code will need to be changed
            // when they are.
            // When there are no encoder offsets, the first two bits of the 
            // bitset need to be zeroed.
            pollingData.clear(0, 2);
            startingBitLocation += 2;  // account for these bits.
            for (int motorCount = 0; motorCount < 2; motorCount++) {
                Motor motor = motors[counter * 2 + motorCount];
                if (motor != null) {
                    // request that each motor encode itself into the packet.
                    startingBitLocation = motor.encodeToValueRequest(pollingData, startingBitLocation);
                } else {
                    // we have to encode 10 bits of zero.
                    pollingData.clear(startingBitLocation, startingBitLocation + 10);
                    startingBitLocation += 10;
                }
            }
            for (int sensorCount = 0; sensorCount < 2; sensorCount++) {
                Sensor currentSensor = sensorType[counter * 2 + sensorCount];
                if (currentSensor != null) {
                    // request that each sensor encode itself into the packet.
                    startingBitLocation = currentSensor.encodeToValueRequest(pollingData, startingBitLocation);
                }
            }
            byte[] pollingBytes = pollingData.toByteArray();
            if ((startingBitLocation % 8) == 0) {
                pollingBytes = Arrays.copyOf(pollingBytes, startingBitLocation / 8);
            } else {
                pollingBytes = Arrays.copyOf(pollingBytes, startingBitLocation / 8 + 1);
            }
            // create a packet of the correct size and fill in the header data.
            packet = new byte[pollingBytes.length + 1];
            System.arraycopy(pollingBytes, 0, packet, 1, pollingBytes.length);
            packet[0] = MSG_TYPE_VALUES;
            byte[] values = serialTransactionWithRetry(counter, packet, 50);
//            if (values != null) {
//                 
//                values = new byte[]{(byte) 0x3 , (byte) 0x74 , (byte) 0x10 , (byte) 0xc , (byte) 0x20 , (byte) 0xfd , (byte) 0xf7 , (byte) 0x1f};
//            Gives: BrickPi.Temp_BitsUsed[0]: 20 BrickPi.Temp_BitsUsed[1]: 3
//            Gives:  Temp_EncoderVal: 525060 [0]
//            Gives:  Temp_EncoderVal: 4 [1]
//            }
            if (values[0] == MSG_TYPE_VALUES) { // hard to think it would be anything else
                //BitSet incoming = BitSet.valueOf(values);
                startingBitLocation = 8; // the message type is still in there, so forget that
                // there are 5 bits associated with each of the encoders
                // these are then encode word length.
                int bitLength = 5;
                // I think that the motor decode method could/should be used to decode these values
                // problem is that it's encoder0 length, encoder1 length, encoder0 value, encoder1 value
                // rather than dealing with each encoder as a block.
                int encoderWordLength0 = decodeInt(bitLength, values, startingBitLocation);
                startingBitLocation += 5;  // skip encoder lengths
                int encoderWordLength1 = decodeInt(bitLength, values, startingBitLocation);
                startingBitLocation += 5;  // skip encoder lengths
                Motor motor = getMotor(counter * 2);
                if (motor != null) {
                    motor.decodeValues(encoderWordLength0, values, startingBitLocation);
                }
                //int encoderVal0 = decodeInt(encoderWordLength0, incoming, startingBitLocation);
                startingBitLocation += encoderWordLength0;
                motor = getMotor(counter * 2 + 1);
                if (motor != null) {
                    motor.decodeValues(encoderWordLength1, values, startingBitLocation);
                }
                try {
                    //int encoderVal1 = decodeInt(encoderWordLength1, incoming, startingBitLocation);
                    startingBitLocation += encoderWordLength1;
                    for (int sensorCount = 0; sensorCount < 2; sensorCount++) {
                        Sensor currentSensor = sensorType[counter * 2 + sensorCount];
                        if (currentSensor != null) {
                            // request that each sensor encode itself into the packet.
                            currentSensor.decodeValues(values, startingBitLocation);
                        } else {
                            startingBitLocation += 10;  // the default seems to be 10 bits....
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Decode an arbitrary number of bits from the bitset.
     *
     * @param bitLength the number of bits to decode
     * @param incoming the bitset to decode them from
     * @param startingBitLocation the starting bit location in the bitset
     * @return the decoded value
     */
    public static int decodeInt(int bitLength, byte[] incoming, int startingBitLocation) {
        int value = 0;
        while (bitLength-- > 0) {
            value <<= 1;
            int location = bitLength + startingBitLocation;
            boolean set = ((incoming[location / 8] & (1 << (location % 8))) != 0);
            if (set) {
                value |= 1;
            }
        }
        return value;
    }

    /**
     * Send the packet to the BrickPi. This will retry up to five times.  This method is 
     * synchronized to prevent multiple threads from attempting to use the serial
     * interface simultaneously.
     *
     * @param addressPointer the index into the serialAddresses array
     * @param packet the packet to send
     * @param timeout how long to wait for a reply
     * @return the received packet
     * @throws IOException throw if anything goes wrong.
     */
    protected synchronized byte[] serialTransactionWithRetry(int addressPointer, byte[] packet, int timeout) throws IOException {
        byte[] response;
        // this is ridiculous.  The serial interface should be, basically 100% reliable
        // it's inexcusable to have to add retry-hacks.
        // the "for" loop will exit on success and return
        // and if that doesn't happen then the method will throw.
        IOException lastioe = new IOException("Unknown");  // value should never be used.
        for (int retry = 0; retry < 5; retry++) {
            try {
                sendToBrickPi(serialAddresses[addressPointer], packet);
                response = readFromBrickPi(timeout);
                return response; // if we were successful break out here.
            } catch (IOException ioe) {
                lastioe = ioe;
            }
        }
        throw lastioe;
    }
}
