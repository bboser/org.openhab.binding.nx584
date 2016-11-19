/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nx584.handler;

import java.math.BigDecimal;
import java.util.Date;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.nx584.internal.panel.NX584;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.NRSerialPort;

/**
 * The {@link NX584Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Bernhard Boser - Initial contribution
 */
public class NX584Handler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(NX584Handler.class);
    private NX584 nx584;
    private NX584Commands nx584Commands;
    private int zones = 24;

    public NX584Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(" + channelUID + ", '" + command.toString() + "')");
        String id = channelUID.getId();
        if ("panel".equals(id)) {
            switch (command.toString()) {
                case "setClock":
                    nx584.setClock(new Date());
                    break;
                case "query":
                    nx584Commands.getSystemStatus();
                    break;
                case "queryZones":
                    for (int zone = 1; zone <= zones; zone++) {
                        nx584Commands.getZoneStatus(zone);
                    }
                    break;
                case "queryZoneNames":
                    for (int zone = 1; zone <= zones; zone++) {
                        nx584Commands.getZoneName(zone);
                    }
                    break;
                case "queryPartitions":
                    for (int partition = 1; partition <= 8; partition++) {
                        nx584Commands.getPartitionStatus(partition);
                    }
                    break;
                case "arm":
                case "armAwayMode":
                    nx584Commands.armPanelAwayMode();
                    break;
                case "armStayMode":
                    nx584Commands.armPanelStayMode();
                    break;
                case "disarm":
                    nx584Commands.disarmPanel();
                    break;
                case "channels":
                    // list all channels
                    logger.debug("list all " + getThing().getChannels().size() + " channels:");
                    for (Channel c : getThing().getChannels()) {
                        logger.debug(String.format("Channel Type %s UID %s", c.getChannelTypeUID(), c.getUID()));
                    }
                    break;
                default:
                    logger.warn("NX584: unrecognized command '" + command + "' (ignored)");
            }
        } else {
            logger.warn("NX584: unrecognized request " + id + " (ignored)");
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        // get configuration parameters
        String port;
        int baud = 1;

        Configuration config = getThing().getConfiguration();

        port = (String) config.get("port");
        if (port == null) {
            port = "not-specified";
        }
        try {
            baud = ((BigDecimal) config.get("baudrate")).intValue();
        } catch (Throwable t) {
        }
        try {
            zones = ((BigDecimal) config.get("zones")).intValue();
        } catch (Throwable t) {
        }

        // create & start panel interface
        try {
            logger.info(
                    "starting nx584 interface at port " + port + " with baudrate " + baud + " and " + zones + " zones");
            nx584 = new NX584(port, baud);
            nx584.connect();
            nx584.addSecurityPanelListener(new NX584Events(this));
            nx584Commands = new NX584Commands(nx584);

            // query panel status
            for (int zone = 1; zone <= zones; zone++) {
                nx584Commands.getZoneStatus(zone);
            }
            for (int partition = 1; partition <= 8; partition++) {
                nx584Commands.getPartitionStatus(partition);
            }
            nx584Commands.getSystemStatus();
        } catch (Throwable t) {
            StringBuilder b = new StringBuilder();
            for (String s : NRSerialPort.getAvailableSerialPorts()) {
                b.append(s).append(' ');
            }
            logger.error(String.format("cannot connect to panel at port %s, available ports: %s", port, b.toString()),
                    t);
        }

        // list all channels
        logger.debug("list all " + getThing().getChannels().size() + " channels:");
        for (Channel c : getThing().getChannels()) {
            logger.debug(String.format("Channel Type %s UID %s", c.getChannelTypeUID(), c.getUID()));
        }

    }

    @Override
    public void dispose() {
        logger.info("dispose nx584 handler, releasing serial port");
        nx584.disconnect();
    }

    @Override
    // Make public for access by NX584Event. Why in the world is this protected?
    public void updateState(String channelID, State state) {
        super.updateState(channelID, state);
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        super.channelLinked(channelUID);
        logger.debug("channel linked: " + channelUID);
    }

}
