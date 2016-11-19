package org.openhab.binding.nx584.internal.panel;

public interface SecurityPanelListener {

    /** Raw message received from 584 interface of NX-8e panel. */
    public void nx584message(int type, byte data[]);

}   
