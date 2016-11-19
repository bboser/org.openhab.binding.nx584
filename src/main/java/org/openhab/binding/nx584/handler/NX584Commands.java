/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nx584.handler;

import org.openhab.binding.nx584.internal.panel.NX584;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bernhard Boser - Initial contribution
 */
public class NX584Commands {

    private Logger logger = LoggerFactory.getLogger(NX584Commands.class);
    private final NX584 nx584;

    public NX584Commands(NX584 nx584) {
        this.nx584 = nx584;
    }

    /**
     * Request zone name message (0x03)
     *
     * @param zone Zone number. 1 for zone 1.
     */
    public void getZoneName(int zone) {
        if (zone < 1) {
            return;
        }
        nx584.sendCommand((byte) 0x23, (byte) (zone - 1));
    }

    /**
     * Request zone status message (0x04)
     *
     * @param zone Zone number. 1 for zone 1.
     */
    public void getZoneStatus(int zone) {
        if (zone < 1) {
            return;
        }
        nx584.sendCommand((byte) 0x24, (byte) (zone - 1));
    }

    /**
     * Request partition status message (0x06)
     *
     * @param partition Partition number. 1 for partition 1.
     */
    public void getPartitionStatus(int partition) {
        if (partition < 1) {
            return;
        }
        nx584.sendCommand((byte) 0x26, (byte) (partition - 1));
    }

    public void getPartionSnapshot() {
        nx584.sendCommand((byte) 0x27);
    }

    public void getSystemStatus() {
        logger.debug("getSystemStatus");
        nx584.sendCommand((byte) 0x28);
    }

    /**
     * Request user information without pin
     *
     * @param user User number, >=1
     */
    public void getUserInformation(int user) {
        if (user < 1) {
            return;
        }
        nx584.sendCommand((byte) 0x33, (byte) user);
    }

    public void disarmPanel() {
        nx584.sendCommand((byte) 0x3d, (byte) 0x01, (byte) 0xff);
    }

    public void armPanelAwayMode() {
        nx584.sendCommand((byte) 0x3d, (byte) 0x02, (byte) 0xff);
    }

    public void armPanelStayMode() {
        nx584.sendCommand((byte) 0x3d, (byte) 0x03, (byte) 0xff);
    }

    public void initiateAutoArm() {
        nx584.sendCommand((byte) 0x3d, (byte) 0x05, (byte) 0xff);
    }

    public void audibleAlarmOff() {
        nx584.sendCommand((byte) 0x3d, (byte) 0x00, (byte) 0xff);
    }

}
