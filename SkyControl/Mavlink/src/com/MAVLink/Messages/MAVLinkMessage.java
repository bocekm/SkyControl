
package com.MAVLink.Messages;

import java.io.Serializable;

public abstract class MAVLinkMessage implements Serializable {
	private static final long serialVersionUID = -7754622750478538539L;
	// The MAVLink message classes have been changed to implement Serializable, 
	// this way is possible to pass a mavlink message trought the Service-Acctivity interface
	
	/**
     * ID of the SENDING system. Allows to differentiate different MAVs on the
     * same network.
     */
	public int sysid;
	/**
     * ID of the SENDING component. Allows to differentiate different components
     * of the same system, e.g. the IMU and the autopilot.
     */
	public int compid;
	/**
     * ID of the message - the id defines what the payload means and how it
     * should be correctly decoded.
     */
	public int msgid;
	public abstract MAVLinkPacket pack();
	public abstract void unpack(MAVLinkPayload payload);
}
	