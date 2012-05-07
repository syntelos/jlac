/*
 *  Copyright (C) 2008 Frank Kunz
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or (at
 *  your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 */
package org.sump.analyzer.devices;

import java.awt.Component;

import javax.swing.JFrame;

import org.sump.analyzer.CapturedData;
import org.sump.analyzer.Configurable;

/**
 * Interface for implementing device controllers. Each device controller must implement
 * at least this interface.
 * 
 * @author Frank Kunz
 *
 */
public interface DeviceController extends Configurable {
	/** dialog showing and waiting for user action */
	public final static int IDLE = 0;
	/** capture currently running */
	public final static int RUNNING = 1;
	/** capture / dialog aborted by user */
	public final static int ABORTED = 2;
	/** capture finished */
	public final static int DONE = 3;

	/**
	 * read the captured device data
	 * @param parent parent component that requests the data (null if none)
	 * @return the captured data or null if no data available
	 */
	public CapturedData getDeviceData(Component parent);
	/**
	 * update the status of the device controller GUI input fields
	 */
	public void updateFields();
	/**
	 * shows the device controllers GUI
	 * @param frame parent frame
	 * @return dialog status
	 * @throws Exception when the dialog can not be shown
	 */
	public int showCaptureDialog(JFrame frame) throws Exception;
	/**
	 * shows the device controllers GUI and starts the capture with the current settings
	 * @param frame parent frame
	 * @return dialog status
	 * @throws Exception when the dialog can not be shown
	 */
	public int showCaptureProgress(JFrame frame) throws Exception;
	/**
	 * get the device controller identification string
	 * @return name of the controller
	 */
	public String getControllerName();
}
