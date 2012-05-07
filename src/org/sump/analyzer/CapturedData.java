/*
 *  Copyright (C) 2006 Michael Poppitz
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
package org.sump.analyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * CapturedData encapsulates the data obtained by the analyzer during a single run.
 * It also provides a method for saving the data to a file.
 * <p>
 * Data files will start with a header containing meta data marked by lines starting with ";".
 * The actual readout values will follow after the header. A value is a
 * logic level transition of one channel. The associated timestamp since
 * sample start (start has timestamp 0) is stored, too after a @ character.
 * This is called compressed format. The handling of the data within the 
 * class is the same.
 * A value is 32bits long. The value is encoded in hex and
 * each value is followed by a new line.
 * <p>
 * In the java code each transition is represented by an integer together with a
 * timestamp represented by a long value.
 * 
 * @version 0.7
 * @author Michael "Mr. Sump" Poppitz
 *
 */
public class CapturedData extends Object {
	/** indicates that rate or trigger position are not available */
	public final static int NOT_AVAILABLE = -1;

	/**
	 * Constructs CapturedData based on the given compressed sampling data.
	 * 
	 * @param values 32bit values as read from device
	 * @param timestamps timstamps in number of samples since sample start
	 * @param triggerPosition position of trigger as index of values array
	 * @param rate sampling rate (may be set to <code>NOT_AVAILABLE</code>)
	 * @param channels number of used channels
	 * @param enabledChannels bit mask identifying used channels
	 * @param absLen absolute number of samples
	 */
	public CapturedData(int[] values, long[] timestamps, long triggerPosition, int rate, int channels, int enabledChannels, long absLen) {
		this.values = values;
		this.timestamps = timestamps;
		this.triggerPosition = triggerPosition;
		this.rate = rate;
		this.channels = channels;
		this.enabledChannels = enabledChannels;
		this.cursorPositionA = 0;
		this.cursorPositionB = 0;
		this.absoluteLength = absLen;
	}

	/**
	 * Constructs CapturedData based on the given absolute sampling data.
	 * 
	 * @param values 32bit values as read from device
	 * @param triggerPosition position of trigger as index of values array
	 * @param rate sampling rate (may be set to <code>NOT_AVAILABLE</code>)
	 * @param channels number of used channels
	 * @param enabledChannels bit mask identifying used channels
	 */
	public CapturedData(int[] values, long triggerPosition, int rate, int channels, int enabledChannels) {
		this.triggerPosition = triggerPosition;
		this.rate = rate;
		this.channels = channels;
		this.enabledChannels = enabledChannels;
		this.cursorPositionA = 0;
		this.cursorPositionB = 0;
		// calculate transitions
		int tmp = values[0];
		int count = 1; // first value is the initial value at time 0
		for(int i=0;i<values.length;i++) {
			if(tmp != values[i]) count++;
			tmp = values[i];
		}
		this.timestamps = new long[count];
		this.values = new int[count];
		this.timestamps[0] = 0;
		this.values[0] = values[0];
		tmp = values[0];
		count = 1;
		for(int i=0;i<values.length;i++) {
			if(tmp != values[i]) {
				// store only transitions
				this.timestamps[count] = i;
				this.values[count] = values[i];
				count++;
			}
			tmp = values[i];
		}
		this.absoluteLength = values.length;
	}

	/**
	 * Constructs CapturedData based on the data read from the given file.
	 * 
	 * @param file			file to read captured data from
	 * @throws IOException when reading from file failes
	 */
	public CapturedData(File file) throws IOException {
		int size = 0, r = -1, channels = 32, enabledChannels = -1;
		long t = -1, a = 0, b = 0;
		boolean cursors = false;
		boolean compressed = false;
		long absLen = 0;
		String line;
		BufferedReader br = new BufferedReader(new FileReader(file));
		do {
			line = br.readLine();
			if (line == null)
				throw new IOException("File appears to be corrupted.");
			else if (line.startsWith(";Size: "))
				size = Integer.parseInt(line.substring(7));
			else if (line.startsWith(";Rate: "))
				r = Integer.parseInt(line.substring(7));
			else if (line.startsWith(";Channels: "))
				channels = Integer.parseInt(line.substring(11));
			else if (line.startsWith(";TriggerPosition: "))
				t = Long.parseLong(line.substring(18));
			else if (line.startsWith(";EnabledChannels: "))
				enabledChannels = Integer.parseInt(line.substring(18));
			else if (line.startsWith(";CursorA: "))
				a = Long.parseLong(line.substring(10));
			else if (line.startsWith(";CursorB: "))
				b = Long.parseLong(line.substring(10));
			else if (line.startsWith(";CursorEnabled: "))
				cursors = Boolean.parseBoolean(line.substring(16));
			else if (line.startsWith(";Compressed: "))
				compressed = Boolean.parseBoolean(line.substring(13));
			else if (line.startsWith(";AbsoluteLength: "))
				absLen = Long.parseLong(line.substring(17));
		} while (line.startsWith(";"));

		if(compressed) {
			// new compressed file format
			this.absoluteLength = absLen;
			this.values = new int[size];
			this.timestamps = new long[size];
			try {
				for (int i = 0; i < this.values.length && line != null; i++) {
					this.values[i] = 					
						Integer.parseInt(line.substring(0, 4), 16) << 16
						| Integer.parseInt(line.substring(4, 8), 16);
					this.timestamps[i] = Long.parseLong(line.substring(9));
					line = br.readLine();
				}
			} catch (NumberFormatException E) {
				throw new IOException("Invalid data encountered.");
			}
		} else {
			// old sample based file format
			if (size <= 0 || size > 1024 * 256)
				throw new IOException("Invalid size encountered.");
			
			this.absoluteLength = size;
			int[] tmpValues = new int[size];
			try {
				// read all values
				for (int i = 0; i < tmpValues.length && line != null; i++) {
					// TODO: modify to work with all channel counts up to 32
					if (channels > 16) {
						tmpValues[i] =
							Integer.parseInt(line.substring(0, 4), 16) << 16
							| Integer.parseInt(line.substring(4, 8), 16);
					} else {
						tmpValues[i] = Integer.parseInt(line.substring(0, 4), 16);					
					}
					line = br.readLine();
				}
			} catch (NumberFormatException E) {
				throw new IOException("Invalid data encountered.");
			}
			
			int count = 0;
			int tmp = tmpValues[0];

			// calculate transitions
			for(int i=0;i<tmpValues.length;i++) {
				if(tmp != tmpValues[i]) count++;
				tmp = tmpValues[i];
			}
			count++;
			
			// compress
			this.values = new int[count];
			this.timestamps = new long[count];
			this.timestamps[0] = 0;
			this.values[0] = tmpValues[0];
			tmp = tmpValues[0];
			count = 1;
			for(int i=0;i<tmpValues.length;i++) {
				if(tmp != tmpValues[i]) {
					// store only transitions
					this.timestamps[count] = i;
					this.values[count] = tmpValues[i];
					count++;
				}
				tmp = tmpValues[i];
			}
		}

		this.triggerPosition = t;
		this.rate = r;
		this.channels = channels;
		this.enabledChannels = enabledChannels;
		this.cursorPositionA = a;
		this.cursorPositionB = b;
		this.cursorEnabled = cursors;

		br.close();
	}
	
	/**
	 * Writes device data to given file.
	 * 
	 * @param file			file to write to
	 * @throws IOException when writing to file failes
	 */
	public void writeToFile(File file) throws IOException  {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write(";Size: " + values.length);
			bw.newLine();
			bw.write(";Rate: " + rate);
			bw.newLine();
			bw.write(";Channels: " + channels);
			bw.newLine();
			bw.write(";EnabledChannels: " + enabledChannels);
			bw.newLine();
			if (triggerPosition >= 0) {
				bw.write(";TriggerPosition: " + triggerPosition);
				bw.newLine();
			}
			bw.write(";CursorEnabled: " + cursorEnabled);
			bw.newLine();
			bw.write(";CursorA: " + cursorPositionA);
			bw.newLine();
			bw.write(";CursorB: " + cursorPositionB);
			bw.newLine();
			bw.write(";Compressed: true");
			bw.newLine();
			bw.write(";AbsoluteLength: " + absoluteLength);
			bw.newLine();
			
			for (int i = 0; i < values.length; i++) {
				String hexVal = Integer.toHexString(values[i]);
				bw.write("00000000".substring(hexVal.length()) + hexVal);
				bw.write("@" + timestamps[i]);
				bw.newLine();
			}
			bw.close();
		} catch (Exception E) {
			E.printStackTrace(System.out);
		}
	}

	/**
	 * Returns wether or not the object contains timing data
	 * @return <code>true</code> when timing data is available
	 */
	public boolean hasTimingData() {
		return (rate != NOT_AVAILABLE);
	}

	/**
	 * Returns wether or not the object contains trigger data
	 * @return <code>true</code> when trigger data is available
	 */
	public boolean hasTriggerData() {
		return (triggerPosition != NOT_AVAILABLE);
	}
	
	/**
	 * Set cursor position of cursor A
	 * @param pos sample position
	 */
	public void setCursorPositionA(long pos) {
		if(pos >= this.absoluteLength) pos = this.absoluteLength - 1;
		if(pos < 0) pos = 0;
		this.cursorPositionA = pos;
	}
	/**
	 * Get position if cursor A
	 * @return sample position
	 */
	public long getCursorPositionA() {
		return this.cursorPositionA;
	}
	/**
	 * Set cursor position of cursor B
	 * @param pos sample position
	 */
	public void setCursorPositionB(long pos) {
		if(pos >= this.absoluteLength) pos = this.absoluteLength - 1;
		if(pos < 0) pos = 0;
		this.cursorPositionB = pos;
	}
	/**
	 * Get position if cursor B
	 * @return sample position
	 */
	public long getCursorPositionB() {
		return this.cursorPositionB;
	}

	/**
	 * calculate index number from absolute time
	 * @param abs absolute time value
	 * @return sample number before selected absolute time
	 */
	public int getSampleIndex(long abs) {
		int i;
		for(i=1;i<timestamps.length;i++) {
			if(abs < timestamps[i]) break;
		}
		return i-1;
	}
	
	/**
	 * return the data value at a specified absolute time offset
	 * @param abs absolute time value
	 * @return data value
	 */
	public int getDataAt(long abs) {
		return values[getSampleIndex(abs)];
	}

	/** captured values */
	public final int[] values;
	/** timestamp values in samples count from start */
	public final long[] timestamps;
	/** position of trigger as index of values */
	public final long triggerPosition;
	/** sampling rate in Hz */
	public final int rate;
	/** number of channels (1-32) */
	public final int channels;
	/** bit map of enabled channels */
	public final int enabledChannels;
	/** absolute sample length */
	public final long absoluteLength;

	/* position of cursor A */
	private long cursorPositionA;
	/* position of cursor B */
	private long cursorPositionB;
	/** cursors enabled status */
	public boolean cursorEnabled;
}
