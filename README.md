BrickPiJava
===========

Java implementation of Raspberry PI/Brick Pi interface


You can clone a complete Netbeans Project including the required libraries.  It uses pi4j.  The jar in the project has been modified to support he 500000 baud rate.

Most sensors are missing.  Except for I2C, color_full and ultrasonic other sensors should work as a RawSensor.

Motor is implemented but not yet tested.

The interfaces/APIs are subject to change at this point.  

You can integrate Netbeans so that it will automatically copy the code to the PI following the instructions here:

https://blogs.oracle.com/speakjava/entry/integrating_netbeans_for_raspberry_pi
 
 

