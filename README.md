# NX584E / Caddx / Networx Alarm Binding

This repo is **unmaintained** and kept up only for reference.

This is an OpenHAB 2 binding for the NX584E security panel with RS-232 interface. I use this with a raspberry pi 3 and connect to the panel via a USB to RS-232 cable. 

Note that the NX584X has a male RS-232 connector. Get an appropriate USB-to-serial cable (I tested with a "DTECH 10 ft USB to RS232 DB9 Female Serial Port Adapter Cable with FTDI Chipset" but have found others to work, too) or a null-modem.

To install, either build from source or simply copy target/org.openhab.binding.nx584-2.0.0-SNAPSHOT.jar to the addons folder of your OpenHAB 2 installation.

If you get an error in the log about gnu.io not available, install it as follows from the karaf console:

```
$ ssh openhab@localhost -p 8101
> feature:install openhab-transport-serial
> logout
$ sudo service openhab2 restart
```

## Thing Declaration

```
// RS-232 port name. E.g. /dev/ttyUSB0 or COM1.
// On Linux (rpi), it may be necessary to add the openhab user to the 
// dialout group to give access to the port.
// sudo adduser openhab dialout
Thing nx584:security:creston  [ port="/dev/ttyUSB0", baudrate=38400, zones=20 ]
```

The "creston" part of the thing declaration is arbitrary. Replace with whatever you like, but use the same pattern in the items definitions.

## Item Declarations

```
// Panel

String securityPanel        "Security Panel"                        { channel="nx584:security:creston:panel"}
Switch securityPanelACFail  "Security Panel AC Fail"                { channel="nx584:security:creston:panel#ac_fail"}
Switch securityPanelLowBattery "Security Panel Low Battery"         { channel="nx584:security:creston:panel#low_battery"}

// Zones

Contact entryUpstairs       "Upstairs Entry"            <door>      { channel="nx584:security:creston:zone2#status" }
Contact windowLiving        "Living Room"               <window>    { channel="nx584:security:creston:zone16#status" }
Contact smokeDen            "Smoke Den"                 <smoke>     { channel="nx584:security:creston:zone19#status" }
Contact supervisorBell      "Bell Supervision"          <switch>    { channel="nx584:security:creston:zone5#status" }
Contact supervisorMotion    "Motion down Supervision"   <switch>    { channel="nx584:security:creston:zone9#status" }

Switch entryBypassed        "Upstairs Entry Bypassed"               { channel="nx584:security:creston:zone2#bypassed" }

// Partition 1

Switch partitionReady       "Ready"                                 { channel="nx584:security:creston:partition1#ready" }

```

The securityPanel item is used to send the following messages to the panel (from a rules file):

```
sendCommand(securityPanel, "arm")
sendCommand(securityPanel, "armAwayMode")
sendCommand(securityPanel, "armStayMode")
sendCommand(securityPanel, "disarm")
sendCommand(securityPanel, "setClock")
```

## Sample Rules

```
rule "arm"
when 
	Item nx584arm changed from OFF to ON
then
	sendCommand(securityPanel, "arm")
end

rule "disarm"
when 
	Item nx584arm changed from ON to OFF
then
	sendCommand(securityPanel, "disarm")
end

rule "clock"
when 
	Item nx584clock changed from OFF to ON
then
	sendCommand(securityPanel, "setClock")
end
```
