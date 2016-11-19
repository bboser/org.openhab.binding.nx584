/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nx584.handler;

import java.io.UnsupportedEncodingException;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.openhab.binding.nx584.internal.panel.NX584;
import org.openhab.binding.nx584.internal.panel.SecurityPanelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bernhard Boser - Initial contribution
 */
public class NX584Events implements SecurityPanelListener {

    private Logger logger = LoggerFactory.getLogger(NX584Events.class);
    private final NX584Handler handler;

    public NX584Events(NX584Handler handler) {
        this.handler = handler;
    }

    @Override
    /** Interpret messages received from the security panel. */
    public void nx584message(int type, byte msg[]) {
        switch (type) {
            case 0x03:
                zoneNameMessage(msg);
                break;
            case 0x04:
                zoneStatusMessage(msg);
                break;
            case 0x05:
                // buggy?
                break;
            case 0x06:
                partitionStatusMessage(msg);
                break;
            case 0x07:
                partitionSnapshotMessage(msg);
                break;
            case 0x08:
                systemStatusMessage(msg);
                break;
            case 0x0a:
                logEventMessage(msg);
                break;
            case 0x1c:
                logger.warn("panel reports: nx584 command / request failed");
                break;
            case 0x1d:
                logger.debug("positive acknowledge from panel");
                break;
            case 0x1e:
                logger.warn("negative acknowledge from panel");
                break;
            case 0x1f:
                logger.warn("message rejected by panel");
                break;
            default:
                logger.debug(String.format("ignored message type 0x%02x received from panel", type));
                break;
        }
    }

    private void zoneNameMessage(byte data[]) {
        if (data[1] < 0 || data[1] > 64) {
            logger.error("zoneNameMessage Zone number out of range: " + data[1]);
        }
        if (data.length != 18) {
            logger.error("zoneNameMessage Length " + data.length + " is not 18 bytes!");
        }
        int zone = data[1] + 1;
        final byte b[] = new byte[16];
        System.arraycopy(data, 1, b, 0, 16);
        String name = "?";
        try {
            name = new String(b, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
        }
        handler.updateState("zone" + zone + "#name", new StringType(name));
        logger.debug(String.format("Zone %d name = '%s'", zone, name));
    }

    private void zoneStatusMessage(byte data[]) {
        int zone = data[1] + 1;
        handler.updateState("zone" + zone + "#status", openClosed(data[6], 0x01));
        handler.updateState("zone" + zone + "#tampered", onOff(data[6], 0x02));
        handler.updateState("zone" + zone + "#trouble", onOff(data[6], 0x04));
        handler.updateState("zone" + zone + "#bypassed", onOff(data[6], 0x08));
        handler.updateState("zone" + zone + "#force_armed", onOff(data[6], 0x10));
        logger.debug(String.format(
                "ZoneStatus: updated zone %2d, pm=0x%02x ztf1=0x%02x ztf2=0x%02x ztf3=0x%02x zcf1=0x%02x zcf2=0x%02x",
                zone, data[2], data[3], data[4], data[5], data[6], data[7]));
    }

    private void partitionStatusMessage(byte data[]) {
        int partition = data[1] + 1;
        logger.debug("received status for partition " + partition + ": " + NX584.bytes2string(data));
        handler.updateState("partition" + partition + "#armed", onOff(data[2], 0x40));
        handler.updateState("partition" + partition + "#ready", onOff(data[7], 0x08));
        handler.updateState("partition" + partition + "#exit1", onOff(data[4], 0x40));
        handler.updateState("partition" + partition + "#exit2", onOff(data[4], 0x80));
    } // partitionStatusMessage

    private void partitionSnapshotMessage(byte data[]) {
        logger.debug("received Partion Snapshot: " + NX584.bytes2string(data));
        for (int i = 1; i < 9; i++) {
            byte b = data[i];
            handler.updateState("partition" + i + "#valid", onOff(b, 0x01));
            handler.updateState("partition" + i + "#ready", onOff(b, 0x02));
            handler.updateState("partition" + i + "#armed", onOff(b, 0x04));
            handler.updateState("partition" + i + "#stay_mode", onOff(b, 0x08));
        }
    } // partitionSnapshotMessage

    private void systemStatusMessage(byte data[]) {
        logger.debug("received System Status Message: " + NX584.bytes2string(data));
        handler.updateState("panel#line_seizure", offOn(data[2], 0x01));
        handler.updateState("panel#off_hook", offOn(data[2], 0x02));
        handler.updateState("panel#ground_fault", offOn(data[3], 0x01));
        handler.updateState("panel#phone_fault", offOn(data[3], 0x02));
        handler.updateState("panel#low_battery", offOn(data[3], 0x40));
        handler.updateState("panel#ac_fail", offOn(data[6], 0x02));
    }

    private final String eventType[] = { "Alarm", // 0
            "Alarm restore", // 1
            "Bypass", // 2
            "Bypass restore", // 3
            "Tamper", // 4
            "Tamper restore", // 5
            "Trouble", // 6
            "Trouble restore", // 7
            "TX low battery", // 8
            "TX low battery restore", // 9
            "Zone lost", // 10
            "Zone lost restore", // 11
            "Start of cross time", // 12
            "13", // 13
            "14", // 14
            "15", // 15
            "16", // 16
            "Special expansion event", // 17
            "Duress", // 18
            "Manual fire", // 19
            "Auxiliary 2 panic", // 20
            "21", // 21
            "Panic", // 22
            "Keypad tamper", // 23
            "Control box tamper", // 24
            "Control box tamper restore", // 25
            "AC fail", // 26
            "AC fail restore", // 27
            "Low battery", // 28
            "Low battery restore", // 29
            "Over-current", // 30
            "Over-current restore", // 31
            "Siren tamper", // 32
            "Siren tamper restore", // 33
            "Telephone fault", // 34
            "Telephone fault restore", // 35
            "Expander trouble", // 36
            "Expander trouble restore", // 37
            "Fail to communicate", // 38
            "Log full", // 39
            "Opening", // 40
            "Closing", // 41
            "Exit error", // 42
            "Recent closing", // 43
            "Auto-test", // 44
            "Start program", // 45
            "End program", // 46
            "Start download", // 47
            "End download", // 48
            "Cancel", // 49
            "Ground fault", // 50
            "Ground fault restore", // 51
            "Manual test", // 52
            "Closed with zones bypassed", // 53
            "Start of listen in", // 54
            "Technician on site", // 55
            "Technician left", // 56
            "Control power up", // 57
            "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75",
            "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93",
            "94", "95", "96", "97", "98", "99", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109",
            "110", "111", "112", "113", "114", "115", "116", "117", "118", "119", "First to open", // 120
            "Last to close", // 121
            "PIN entered with bit 7 set", // 122
            "Begin walk test", // 123
            "End walk test", // 124
            "Re-exit", // 125
            "Output trip", // 126
            "Data lost" // 127
    };

    private void logEventMessage(byte data[]) {
        int type = data[3];
        if ((type & 0x80) != 0) {
            String msg = eventType[type & 0x7f];
            handler.updateState("panel#log", new StringType(msg));
        }
    } // logEventMessage

    private OpenClosedType openClosed(byte info, int mask) {
        return (info & mask) == 0 ? OpenClosedType.CLOSED : OpenClosedType.OPEN;
    }

    private OnOffType onOff(byte info, int mask) {
        return (info & mask) == 0 ? OnOffType.OFF : OnOffType.ON;
    }

    private OnOffType offOn(byte info, int mask) {
        return (info & mask) == 0 ? OnOffType.ON : OnOffType.OFF;
    }

}
