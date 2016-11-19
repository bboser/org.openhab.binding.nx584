/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nx584;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link NX584Binding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Bernhard Boser - Initial contribution
 */
public class NX584BindingConstants {

    public static final String BINDING_ID = "nx584";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_SECURITY = new ThingTypeUID(BINDING_ID, "security");

}
