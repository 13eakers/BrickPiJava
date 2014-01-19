BrickPiJava
===========

Java implementation of Raspberry PI/Brick Pi interface


You can clone a complete Netbeans Project including the required libraries.  It uses pi4j.  The jar in the project has been modified to support he 500000 baud rate.

Some sensors are still missing, the Light, Ultrasonic and Touch are there and (minimally) tested.  The Color Sensors and RCX Light should all follow the Light Sensor pattern, but I don't have any to test. Color Full and I2C sensors are a little different - again, I don't have those sensors.

Motors are implemented.  You can get the instantaneous speed of the motor, as calculated from the encoder values, by means of the "getCurrentSpeed" method of your Motor class.  There are problem problems with this, such as if the encoder over/underflows, when changing direction, etc. - create an issue with a good description (and preferably a use-case)  The speed also seems to be double the actual speed.  No real idea why that is, unless the ticks per rev are 1440 and not 720.	

Take a look at the BrickPiTests.java for usage examples.  
Conceptually, it's pretty simple.  You create instances of one of the Sensor classes and/or Motors.  Associate them with the correct port number on the BrickPi instance.  You need to run "setupSensors", after that, it should all just work. 

The interfaces/APIs may change, but should be pretty stable.
You can integrate Netbeans so that it will automatically copy the code to the PI following the instructions here:

https://blogs.oracle.com/speakjava/entry/integrating_netbeans_for_raspberry_pi
 
 

