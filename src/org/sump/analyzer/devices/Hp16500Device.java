/*
 *  Copyright (C) 2008 Frank Kunz
 *  Copyright (C) 2012 John Pritchard
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

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Device provides access to HP16500 logic analyzer.
 * It requires the rxtx package from http://www.rxtx.org/ to
 * access the serial port the analyzer is connected to.
 * 
 * @author Frank Kunz
 * @author John Pritchard
 */
public class Hp16500Device {
	
    /**
     * create a analyzer object
     */
    public Hp16500Device() {
        port = null;
        inputStream = null;
        outputStream = null;
        analyzerId = null;
        cardCage = null;
        progress = 0;
        running = false;
        debug = false;
    }
	
    /**
     * Gets a string array containing the names all available serial ports.
     * @return array containing serial port names
     */
    @SuppressWarnings("unchecked")
    static public String[] getPorts() {
        Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        LinkedList<String> portList = new LinkedList<String>();
        CommPortIdentifier portId = null;

        while (portIdentifiers.hasMoreElements()) {
            portId = portIdentifiers.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portList.addLast(portId.getName());
                //System.out.println(portId.getName());
            }
        }
			
        return ((String[])portList.toArray(new String[1]));
    }

    /**
     * open Analyzer port for communication
     * @param portName name of the com port
     * @param portRate baud rate
     * @param portParity parity setting
     * @param portStopbit number of stop bits
     * @throws PortInUseException when the port is not available
     */
    public void open(String portName, int portRate, int portParity, int portStopbit) throws PortInUseException {
        CommPortIdentifier portId = null;

        if(port != null)
            throw new PortInUseException();
		
        try {
            portId = CommPortIdentifier.getPortIdentifier(portName);
			
            port = (SerialPort) portId.open("Logic Analyzer Client", 1000);
				
            port.setSerialPortParams(
                                     portRate,
                                     SerialPort.DATABITS_8,
                                     portStopbit,
                                     portParity
                                     );
            port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            port.disableReceiveFraming();
            port.enableReceiveTimeout(100);
			
            outputStream = port.getOutputStream();
            inputStream = port.getInputStream();
        } catch(Exception E) {
            E.printStackTrace(System.out);
        }		
    }
	
    /**
     * write a command to the analyzer
     * @param cmd byte array with command
     */
    synchronized private void analyzerCommand(String cmd) {
        if((inputStream != null) && (outputStream != null)) {
            if(debug) System.out.println("CMD:" + cmd);
            try {
                byte[] c = cmd.getBytes();
                outputStream.write(c);
                if(c[c.length-1] != 10)
                    outputStream.write(10); // send terminator if not included in cmd
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                outputStream.flush();
            } catch(IOException e) {
                e.printStackTrace();
            }
			
        }
    }
	
    /**
     * read string data from analyzer
     * @return data string
     * @throws IOException when read fails
     */
    synchronized private String analyzerReadString() throws IOException {
        String retval = new String();
        int timeout = 50;
		
        running = true;
        progress = 0;
		
        if(inputStream != null) {
            int data;
            LinkedList<Byte> chars = new LinkedList<Byte>();
            while(timeout > 0) {
                if(inputStream.available() > 0) {
                    data = inputStream.read();
                    if((data < ' ') || (data == 10) || (data == 13))
                        break;
                    chars.add(new Byte((byte)data));
                    timeout = 50;
                } else {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                    timeout--;
                }
            }
			
            if(timeout == 0)
                throw new IOException("No response from analyzer");
			
            byte[] charData = new byte[chars.size()];
            for(int i=0;i<charData.length;i++) {
                charData[i] = chars.get(i);
            }
            retval = new String(charData);
        }
        if(debug) System.out.println("RET=\"" + retval + "\"");
		
        progress = 100;
        running = false;
		
        return retval;
    }
	
    /**
     * read block data from analyzer
     * @return raw data array
     * @throws IOException when read fails
     */
    synchronized private byte[] analyzerReadBlock() throws IOException {
        byte[] retval = new byte[0];
        int timeout = 50;
        int step = 0;
        int i=0,j=0,k=0;
        int readSize = 0;
        char[] numberOfBytes = new char[0];
		
        running = true;
		
        if(inputStream != null) {
            while(timeout > 0) {
                if(inputStream.available() > 0) {
                    timeout = 50;
                    switch(step) {
                    case 0:
                        i++;
                        // try to detect first block char #
                        if(inputStream.read() == '#')
                            step++;
                        break;
                    case 1:
                        if(debug) System.out.println("skipped " + i + " bytes before block data");
                        // read number of chars in size field
                        i = inputStream.read() - '0';
                        if(debug) System.out.println("read " + i + " chars for size");
                        step++;
                        numberOfBytes = new char[i];
                        i = 0;
                        progress = 0;
                        break;
                    case 2:
                        // read the size
                        numberOfBytes[i] = (char)inputStream.read();
                        i++;
                        if(i >= numberOfBytes.length) {
                            readSize = Integer.parseInt(new String(numberOfBytes));
                            step++;
                            if(debug) System.out.println("BLK=\"" + readSize + "\"");
                            retval = new byte[readSize];
                            i = readSize;
                            k = 0;
                        }
                        break;
                    case 3:
                        j = inputStream.read(retval, k, i);
                        i -= j;
                        k += j;
                        if(debug) System.out.println("read " + k + " of " + readSize + " bytes");
                        if(i == 0) {
                            step++;
                        }
                        // update progress
                        progress = (k * 100) / readSize;
                        break;
                    case 4:
                        if(debug) System.out.println("read complete");
                        inputStream.read();
                        progress = 100;
						
                        running = false;
						
                        return retval;
                        //break;
                    default:
                        break;
                    }
                } else {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                    timeout--;
                }
            }
        }
		
        running = false;
		
        return retval;
    }
	
    /**
     * read analyzer ID string
     */
    private void readAnalyzerId() {
        analyzerCommand("*IDN?");
        try {
            analyzerId = analyzerReadString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
    /**
     * return analyzer ID string
     * @return string with analyzer identification
     * @throws IOException when no analyzer is connected
     */
    public String getAnalyzerId() {
        if(analyzerId == null) {
            readAnalyzerId();
        }
        return analyzerId;
    }
	
    /**
     * reads the card cage of the analyzer
     */
    private void readCardCage() throws IOException {
        analyzerCommand(":CARD?");
        try {
            String response = analyzerReadString();
            StringTokenizer st = new StringTokenizer(response,",");
            int tokens = st.countTokens();
            // only first half of the tokens are from interest
            tokens /= 2;
            cardCage = new int[tokens];
            for(int i=0;i<tokens;i++) {
                cardCage[i] = Integer.parseInt(st.nextToken());
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
	
    /**
     * return the card cage content
     * @return string array with card cage
     */
    public int[] getCardCage() throws IOException {
        if(cardCage == null) {
            readCardCage();
        }
        return cardCage;
    }
	
    /**
     * get the card decription string from the card id
     * @param id card id
     * @return string with description
     */
    public String getCardString(int id) {
        for(int i=0;i<ANALYZER_OPTION_CODES.length;i++) {
            if(ANALYZER_OPTION_CODES[i] == id) {
                return(ANALYZER_OPTION_STRINGS[i]);
            }
        }
        return null;
    }
	
    /**
     * get the card type due to its description string 
     * @param card card string
     * @return card id
     */
    public int getCardId(String card) {
        String start;
        for(int i=0;i<ANALYZER_OPTION_STRINGS.length;i++) {
            start = ANALYZER_OPTION_STRINGS[i].substring(0, ANALYZER_OPTION_STRINGS[i].indexOf(' '));
            if(start.equals(card)) {
                return(ANALYZER_OPTION_CODES[i]);
            }
        }
        return -1;
    }
	
    /**
     * finds a card in card cage
     * @param card card id
     * @return cage slot number
     */
    public int getCardInCage(int card) {
        if(cardCage != null)
            for(int i=0;i<cardCage.length;i++) {
                if(cardCage[i] == card) return(i);
            }
        return(-1);
    }
	
    /**
     * read captured data from an analyzer card
     * @param card card number
     * @return raw data bytes
     */
    public byte[] getData(int card) throws IOException, IllegalArgumentException {
        card++;
        if((card < 1) || (card > cardCage.length)) {
            throw new IllegalArgumentException("card " + card + " is out of range");
        }
        String cmd = ":SEL " + card; 
        analyzerCommand(cmd);
        analyzerCommand(":SYST:DATA?");
        byte[] data = new byte[0];
        data = analyzerReadBlock();
        return data;
    }
	
    /**
     * get the progress of a block read operation in
     * @return progress in %
     */
    public int getPercentage() {
        return progress;
    }
	
    /**
     * check if device io is running
     * @return state of device io
     */
    public boolean isRunning() {
        return running;
    }
	
    /**
     * close the analyzer communication port
     */
    public void close() {
        try {
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        port.close();
        port = null;
        inputStream = null;
        outputStream = null;
        //cardCage = null;
        analyzerId = null;
    }

    private SerialPort port;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String analyzerId;
    private int[] cardCage;
    private int progress;
    private boolean running; 
	
    private static int[] ANALYZER_OPTION_CODES = {
        1,2,11,12,13,21,22,30,31,32,33,40,41,42,43
    };
	
    private static String[] ANALYZER_OPTION_STRINGS = {
        "HP16515A 1 GHz Timing Master Card", // 1
        "HP16516A 1 GHz Timing Expansion Card", // 2
        "HP16530A Oscilloscope Timebase Card", // 11
        "HP16531A Oscilloscope Acquisition Card", // 12
        "HP16532A Oscilloscope Card", // 13
        "HP16520A Pattern Generator Master Card", // 21
        "HP16521A Pattern Generator Expansion Card", // 22
        "HP16511B Logic Analyzer Card", // 30
        "HP16510A or B Pattern Generator Master Card", // 31
        "HP16550A Logic Analyzer Master Card", // 32
        "HP16550A Logic Analyzer Expansion Card", // 33
        "HP16540A Logic Analyzer Card", // 40
        "HP16541A Logic Analyzer Card", // 41
        "HP16542A Logic Analyzer Master Card", // 42
        "HP16543A Logic Analyzer Expansion Card" // 43
    };
	
    boolean debug;
}
