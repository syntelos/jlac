/*
 *  Copyright (C) 2007 Frank Kunz
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
package org.sump.analyzer.tools;
  
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import org.sump.analyzer.CapturedData;
import org.sump.analyzer.Configurable;

import org.sump.util.Properties;

/**
 * UART Protocol analyzer
 *
 * @author Frank Kunz
 * @author John Pritchard
 */
public class UARTProtocolAnalysis extends Base implements Tool, Configurable {

    private static GridBagConstraints createConstraints(int x, int y, int w, int h, double wx, double wy) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = x; gbc.gridy = y;
        gbc.gridwidth = w; gbc.gridheight = h;
        gbc.weightx = wx; gbc.weighty = wy;
        return (gbc);
    }

    /**
     * Class for UART dataset
     * @author Frank Kunz
     */
    private class UARTProtocolAnalysisDataSet implements Comparable<UARTProtocolAnalysisDataSet> {
        /*
         * data
         */
        public UARTProtocolAnalysisDataSet (long time, int data, int type) {
            this.time = time;
            this.data = data;
            this.type = type;
            this.event = null;
        }
		
        /*
         * type specific event
         */
        public UARTProtocolAnalysisDataSet (long time, String ev, int type) {
            this.time = time;
            this.data = 0;
            this.type = type;
            this.event = new String(ev);
        }
		
        /*
         * generic event
         */
        public UARTProtocolAnalysisDataSet (long time, String ev) {
            this.time = time;
            this.data = 0;
            this.type = UART_TYPE_EVENT;
            this.event = new String(ev);
        }

        /*
         * for result sort algo
         * (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(UARTProtocolAnalysisDataSet cmp) {
            return (int)(this.time - cmp.time);
        }
		
        public long time;
        public int data;
        public int type;
        public String event;
        public static final int UART_TYPE_EVENT   = 0;
        public static final int UART_TYPE_RXEVENT = 1;
        public static final int UART_TYPE_TXEVENT = 2;
        public static final int UART_TYPE_RXDATA  = 3;
        public static final int UART_TYPE_TXDATA  = 4;
    }
	
    /**
     * The Dialog Class
     * @author Frank Kunz
     *
     * The dialog class draws the basic dialog with a grid layout. The dialog
     * consists of three main parts. A settings panel, a table panel
     * and three buttons.
     */
    private class UARTProtocolAnalysisDialog extends JDialog implements ActionListener, Runnable {
        public UARTProtocolAnalysisDialog(Frame frame, String name) {
            super(frame, name, true);
            Container pane = getContentPane();
            pane.setLayout(new GridBagLayout());
            getRootPane().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            decodedData = new Vector<UARTProtocolAnalysisDataSet>();
            startOfDecode = -1;
            decodedSymbols = 0;
            bitLength = 0;
            detectedErrors = 0;
			
            /*
             * add protocol settings elements
             */
            JPanel panSettings = new JPanel();
            panSettings.setLayout(new GridLayout(12,2,5,5));
            panSettings.setBorder(BorderFactory.createCompoundBorder(
                                                                     BorderFactory.createTitledBorder("Settings"),
                                                                     BorderFactory.createEmptyBorder(5, 5, 5, 5)));
			
            String channels[] = new String[33];
            for (int i = 0; i < 32; i++)
                channels[i] = new String("Channel " + i);
            channels[channels.length-1] = new String("unused");

            panSettings.add(new JLabel("RxD"));
            rxd = new JComboBox(channels);
            panSettings.add(rxd);
			
            panSettings.add(new JLabel("TxD"));
            txd = new JComboBox(channels);
            panSettings.add(txd);
			
            panSettings.add(new JLabel("CTS"));
            cts = new JComboBox(channels);
            cts.setSelectedItem("unused");
            panSettings.add(cts);

            panSettings.add(new JLabel("RTS"));
            rts = new JComboBox(channels);
            rts.setSelectedItem("unused");
            panSettings.add(rts);

            panSettings.add(new JLabel("DTR"));
            dtr = new JComboBox(channels);
            dtr.setSelectedItem("unused");
            panSettings.add(dtr);

            panSettings.add(new JLabel("DSR"));
            dsr = new JComboBox(channels);
            dsr.setSelectedItem("unused");
            panSettings.add(dsr);

            panSettings.add(new JLabel("DCD"));
            dcd = new JComboBox(channels);
            dcd.setSelectedItem("unused");
            panSettings.add(dcd);

            panSettings.add(new JLabel("RI"));
            ri = new JComboBox(channels);
            ri.setSelectedItem("unused");
            panSettings.add(ri);

            panSettings.add(new JLabel("Parity"));
            parityarray = new String[3];
            parityarray[0] = new String("none");
            parityarray[1] = new String("odd");
            parityarray[2] = new String("even");
            parity = new JComboBox(parityarray);
            panSettings.add(parity);
			
            panSettings.add(new JLabel("Bits"));
            bitarray = new String[4];
            for (int i = 0; i < bitarray.length; i++)
                bitarray[i] = new String("" + (i+5));
            bits = new JComboBox(bitarray);
            bits.setSelectedItem("8");
            panSettings.add(bits);

            panSettings.add(new JLabel("Stopbit"));
            stoparray = new String[3];
            stoparray[0] = new String("1");
            stoparray[1] = new String("1.5");
            stoparray[2] = new String("2");
            stop = new JComboBox(stoparray);
            panSettings.add(stop);

            inv = new JCheckBox();
            panSettings.add(new JLabel("Invert"));
            panSettings.add(inv);
			
            pane.add(panSettings, createConstraints(0, 0, 1, 1, 0, 0));
			
            /*
             * add an empty output view
             */
            JPanel panTable = new JPanel();
            panTable.setLayout(new GridLayout(1, 1, 5, 5));
            panTable.setBorder(BorderFactory.createCompoundBorder(
                                                                  BorderFactory.createTitledBorder("Results"),
                                                                  BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            outText = new JEditorPane("text/html", toHtmlPage(true));
            outText.setMargin(new Insets(5,5,5,5));
            panTable.add(new JScrollPane(outText));
            add(panTable, createConstraints(1, 0, 3, 3, 1.0, 1.0));
			
            /*
             * add progress bar
             */
            JPanel panProgress = new JPanel();
            panProgress.setLayout(new BorderLayout());
            panProgress.setBorder(BorderFactory.createCompoundBorder(
                                                                     BorderFactory.createTitledBorder("Progress"),
                                                                     BorderFactory.createEmptyBorder(5, 5, 5, 5)
                                                                     ));
            progress = new JProgressBar(0, 100);
            progress.setMinimum(0);
            progress.setValue(0);
            progress.setMaximum(100);
            panProgress.add(progress, BorderLayout.CENTER);
            add(panProgress, createConstraints(0, 3, 3, 1, 1.0, 0));

            /*
             * add buttons
             */
            btnConvert = new JButton("Analyze");
            btnConvert.addActionListener(this);
            add(btnConvert, createConstraints(0, 4, 1, 1, 1.0, 0));
            btnExport = new JButton("Export");
            btnExport.addActionListener(this);
            add(btnExport, createConstraints(1, 4, 1, 1, 1.0, 0));
            btnCancel = new JButton("Close");
            btnCancel.addActionListener(this);
            add(btnCancel, createConstraints(2, 4, 1, 1, 1.0, 0));
			
            fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter((FileFilter) new CSVFilter());
            fileChooser.addChoosableFileFilter((FileFilter) new HTMLFilter());

            setSize(1000, 550);
            setResizable(false);
            runFlag = false;
            thrWorker = null;
        }

        /**
         * shows the dialog and sets the data to use
         * @param data data to use for analysis
         */
        public void showDialog(CapturedData data) {
            analysisData = data;
            setVisible(true);
            setLocationRelativeTo(null);
        }
		
        /**
         * set the controls of the dialog enabled/disabled
         * @param enable status of the controls
         */
        private void setControlsEnabled(boolean enable) {
            rxd.setEnabled(enable);
            txd.setEnabled(enable);
            cts.setEnabled(enable);
            rts.setEnabled(enable);
            dtr.setEnabled(enable);
            dsr.setEnabled(enable);
            dcd.setEnabled(enable);
            ri.setEnabled(enable);
            parity.setEnabled(enable);
            bits.setEnabled(enable);
            stop.setEnabled(enable);
            inv.setEnabled(enable);
            btnExport.setEnabled(enable);
            btnCancel.setEnabled(enable);
        }
		
        /**
         * Dialog Action handler
         */
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Analyze")) {
                runFlag = true;
                thrWorker = new Thread(this);
                thrWorker.start();
            } else if (e.getActionCommand().equals("Close")) {
                setVisible(false);
            } else if (e.getActionCommand().equals("Export")) {
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if(fileChooser.getFileFilter().getDescription().equals("Website (*.html)")) {
                        storeToHtmlFile(file);
                    } else {
                        storeToCsvFile(file);
                    }
                }
            } else if (e.getActionCommand().equals("Abort")) {
                runFlag = false;
            }
        }

        /**
         * This is the UART protocol decoder core
         *
         * The decoder scans for a decode start event like CS high to
         * low edge or the trigger of the captured data. After this the
         * decoder starts to decode the data by the selected mode, number
         * of bits and bit order. The decoded data are put to a JTable
         * object directly.
         */
        private void decode() {
            // process the captured data and write to output
            int i,a;
			
            // clear old data
            decodedData.clear();
			
            /*
             * Build bitmasks based on the RxD, TxD, CTS and RTS
             * pins.
             */
			
            int rxdmask = 0;
            if(!((String)rxd.getSelectedItem()).equals("unused"))
                rxdmask= (1 << rxd.getSelectedIndex());
			
            int txdmask = 0;
            if(!((String)txd.getSelectedItem()).equals("unused"))
                txdmask = (1 << txd.getSelectedIndex());
			
            int ctsmask = 0;
            if(!((String)cts.getSelectedItem()).equals("unused"))
                ctsmask = (1 << cts.getSelectedIndex());
			
            int rtsmask = 0;
            if(!((String)rts.getSelectedItem()).equals("unused"))
                rtsmask = (1 << rts.getSelectedIndex());
			
            int dcdmask = 0;
            if(!((String)dcd.getSelectedItem()).equals("unused"))
                dcdmask = (1 << dcd.getSelectedIndex());
			
            int rimask = 0;
            if(!((String)ri.getSelectedItem()).equals("unused"))
                rimask  = (1 << ri.getSelectedIndex());
			
            int dsrmask = 0;
            if(!((String)dsr.getSelectedItem()).equals("unused"))
                dsrmask = (1 << dsr.getSelectedIndex());
			
            int dtrmask = 0;
            if(!((String)dtr.getSelectedItem()).equals("unused"))
                dtrmask = (1 << dtr.getSelectedIndex());
			
            System.out.println("rxdmask = 0x" + Integer.toHexString(rxdmask));
            System.out.println("txdmask = 0x" + Integer.toHexString(txdmask));
            System.out.println("ctsmask = 0x" + Integer.toHexString(ctsmask));
            System.out.println("rtsmask = 0x" + Integer.toHexString(rtsmask));
            System.out.println("dcdmask = 0x" + Integer.toHexString(dcdmask));
            System.out.println("rimask  = 0x" + Integer.toHexString(rimask));
            System.out.println("dsrmask = 0x" + Integer.toHexString(dsrmask));
            System.out.println("dtrmask = 0x" + Integer.toHexString(dtrmask));
			
            /*
             * Start decode from trigger or if no trigger is available from the
             * first falling edge.
             * The decoder works with two independant decoder runs. First for 
             * RxD and then for TxD, after this CTS, RTS, etc. is detected if enabled.
             * After decoding all the decoded data are unsortet before the data is
             * displayed it must be sortet by time.
             */
			
            /*
             * set the start of decode to the trigger if avail or
             * find first state change on the selected lines
             */
            if(analysisData.cursorEnabled) {
                startOfDecode = analysisData.getCursorPositionA();
                endOfDecode = analysisData.getCursorPositionB();
            } else {
                if(analysisData.hasTriggerData()) {
                    startOfDecode = analysisData.triggerPosition;
                    // the trigger may be too late, a workaround is to go back some samples here
                    startOfDecode -= 10;
                    if(startOfDecode < 0) startOfDecode = 0;
                } else {
                    int mask = rxdmask | rimask | ctsmask | txdmask | dcdmask | rimask | dsrmask | dtrmask;
                    a = analysisData.values[0] & mask;
                    for(i=0;i<analysisData.values.length;i++) {
                        if(a != (analysisData.values[i] & mask)) {
                            startOfDecode = analysisData.timestamps[i];
                            break;
                        }
                    }
                }
                endOfDecode = analysisData.absoluteLength;
            }
            decodedSymbols = 0;
            detectedErrors = 0;
			
            // decode RxD
            if(rxdmask != 0) {
                BaudRateAnalyzer baudrate = new BaudRateAnalyzer(analysisData.values, analysisData.timestamps, rxdmask);
                System.out.println(baudrate.toString());
                bitLength = baudrate.getBest();
                if(bitLength == 0) {
                    System.out.println("No data for decode");
                } else {
                    System.out.println("Samplerate=" + analysisData.rate + " Bitlength=" + bitLength + " Baudrate=" + analysisData.rate / bitLength);
                    decodedSymbols += decodeData(bitLength, rxdmask, UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA);
                }
            }
            // decode TxD
            if(txdmask != 0) {
                BaudRateAnalyzer baudrate = new BaudRateAnalyzer(analysisData.values, analysisData.timestamps, txdmask);
                System.out.println(baudrate.toString());
                bitLength = baudrate.getBest();
                if(bitLength == 0) {
                    System.out.println("No data for decode");
                } else {
                    System.out.println("Samplerate=" + analysisData.rate + " Bitlength=" + bitLength + " Baudrate=" + analysisData.rate / bitLength);
                    decodedSymbols += decodeData(bitLength, txdmask, UARTProtocolAnalysisDataSet.UART_TYPE_TXDATA);
                }
            }
            // decode control lines
            decodeControl(ctsmask, "CTS");
            decodeControl(rtsmask, "RTS");
            decodeControl(dcdmask, "DCD");
            decodeControl(rimask,  "RI");
            decodeControl(dsrmask, "DSR");
            decodeControl(dtrmask, "DTR");
			
            // sort the results by time
            Collections.sort(decodedData);
			
            outText.setText(toHtmlPage(false));
            outText.setEditable(false);
        }
		
        /**
         * decode a control line
         * @param mask bitmask for the control line
         * @param name name string of the control line
         */
        private void decodeControl(int mask, String name) {
            if(mask == 0) return;
            System.out.println("Decode " + name);
            long i;
            int a;
            a = analysisData.getDataAt(0) & mask;
            progress.setValue(0);
            for(i=startOfDecode;i<endOfDecode;i++) {
                if(a < (analysisData.getDataAt(i) & mask)) {
                    // rising edge
                    decodedData.add(new UARTProtocolAnalysisDataSet(i,name + "_HIGH"));
                }
                if(a > (analysisData.getDataAt(i) & mask)) {
                    // falling edge
                    decodedData.add(new UARTProtocolAnalysisDataSet(i,name + "_LOW"));
                }
                a = analysisData.getDataAt(i) & mask;
				
                // update progress
                progress.setValue((int)(i * 100 / (endOfDecode - startOfDecode)));

                // abort here
                if(!runFlag) {
                    break;
                }
            }
            progress.setValue(100);
        }
		
        /**
         * calculate the time offset
         * @param time absolute sample number
         * @return time relative to data
         */
        private long calculateTime(long time) {
            if(analysisData.hasTriggerData()) {
                return time - analysisData.triggerPosition;
            } else {
                return time;
            }
        }

        /**
         * decode a UART data line
         * @param baud baudrate (counted samples per bit)
         * @param mask bitmask for the dataline
         * @param type type of the data (rx or tx)
         */
        private int decodeData(int baud, int mask, int type) {
            if(mask == 0) return(0);
            long a = 0;
            int b = 0;
            long c = 0;
            long i = 0;
            int value = 0;
            int bitCount;
            int stopCount;
            int parityCount;
            int count = 0;
			
            bitCount = Integer.parseInt((String)bits.getSelectedItem());
            if(((String)parity.getSelectedItem()).equals("none")) {
                parityCount = 0;
            } else {
                parityCount = 1;
            }
            if(((String)stop.getSelectedItem()).equals("1")) {
                stopCount = 1;
            } else {
                stopCount = 2;
            }
			
            if(startOfDecode > 0) a = startOfDecode;
			
            while((endOfDecode - a) > ((bitCount + stopCount + parityCount) * baud)) {

                /*
                 * find first falling edge this 
                 * is the start of the startbit.
                 * If the inverted checkbox is set find the first rising edge.
                 */
                b = analysisData.getDataAt(a) & mask;
                for(i=a;i<endOfDecode;i++) {
                    if(inv.isSelected()) {
                        if(b < (analysisData.getDataAt(i) & mask)) {
                            c = i;
                            break;
                        }
                    } else {
                        if(b > (analysisData.getDataAt(i) & mask)) {
                            c = i;
                            break;
                        }
                    }
                    b = analysisData.getDataAt(i) & mask;

                    // update progress
                    progress.setValue((int)(i * 100 / (endOfDecode - startOfDecode)));

                    // abort here
                    if(!runFlag) {
                        System.out.println("Abort: count=" + count + " pos=" + i);
                        i = endOfDecode;
                        break;
                    }
                }
                if(i >= endOfDecode) {
                    System.out.println("End decode");
                    break;
                }
	
				
                /*
                 * Sampling is done in the middle of each bit
                 * the start bit must be low
                 * If the inverted checkbox is set the startbit must be high
                 */
                a = c + baud / 2;
                if(inv.isSelected()) {
                    if((analysisData.getDataAt(a) & mask) == 0) {
                        // this is not a start bit !
                        if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                            decodedData.add(new UARTProtocolAnalysisDataSet(calculateTime(a),"START_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                        else
                            decodedData.add(new UARTProtocolAnalysisDataSet(calculateTime(a),"START_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                        detectedErrors++;
                    }
                } else {
                    if((analysisData.getDataAt(a) & mask) != 0) {
                        // this is not a start bit !
                        if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                            decodedData.add(new UARTProtocolAnalysisDataSet(calculateTime(a),"START_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                        else
                            decodedData.add(new UARTProtocolAnalysisDataSet(calculateTime(a),"START_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                        detectedErrors++;
                    }
                }
				
                /*
                 * sample the databits in the middle of the bit position
                 */
				
                value = 0;
                for(i=0;i<bitCount;i++) {
                    a += baud;
                    if(inv.isSelected()) {
                        if((analysisData.getDataAt(a) & mask) == 0) {
                            value |= (1 << i);
                        }
                    } else {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            value |= (1 << i);
                        }
                    }
                }
                decodedData.add(new UARTProtocolAnalysisDataSet(a,value,type));
                count++;
				
                /*
                 * sample parity bit if available
                 */
                String parityText = (String)parity.getSelectedItem();
                if(parityText.equals("odd")) {
                    a += baud;
                    if((Integer.bitCount(value) & 1) == 0) {
                        if(inv.isSelected()) {
                            // odd parity, bitcount is even --> parity bit must be 0 (inverted)
                            if((analysisData.getDataAt(a) & mask) != 0) {
                                // parity error
                                if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                                else
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                                detectedErrors++;
                            }
                        } else {
                            // odd parity, bitcount is even --> parity bit must be 1
                            if((analysisData.getDataAt(a) & mask) == 0) {
                                // parity error
                                if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                                else
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                                detectedErrors++;
                            }
                        }
                    } else {
                        if(inv.isSelected()) {
                            // odd parity, bitcount is odd --> parity bit must be 1 (Inverted)
                            if((analysisData.getDataAt(a) & mask) == 0) {
                                // parity error
                                if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                                else
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                                detectedErrors++;
                            }
                        } else {
                            // odd parity, bitcount is odd --> parity bit must be 0
                            if((analysisData.getDataAt(a) & mask) != 0) {
                                // parity error
                                if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                                else
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                                detectedErrors++;
                            }
                        }
                    }
                }
                if(parityText.equals("even")) {
                    a += baud;
                    if((Integer.bitCount(value) & 1) == 0) {
                        if(inv.isSelected()) {
                            // even parity, bitcount is even --> parity bit must be 1 (inverted)
                            if((analysisData.getDataAt(a) & mask) == 0) {
                                // parity error
                                if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                                else
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                                detectedErrors++;
                            }
                        } else {
                            // even parity, bitcount is even --> parity bit must be 0
                            if((analysisData.getDataAt(a) & mask) != 0) {
                                // parity error
                                if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                                else
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                                detectedErrors++;
                            }
                        }
                    } else {
                        if(inv.isSelected()) {
                            // even parity, bitcount is odd --> parity bit must be 0 (inverted)
                            if((analysisData.getDataAt(a) & mask) != 0) {
                                // parity error
                                if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                                else
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                                detectedErrors++;
                            }
                        } else {
                            // even parity, bitcount is odd --> parity bit must be 1
                            if((analysisData.getDataAt(a) & mask) == 0) {
                                // parity error
                                if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                                else
                                    decodedData.add(new UARTProtocolAnalysisDataSet(a,"PARITY_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                                detectedErrors++;
                            }
                        }
                    }
                }
				
                /*
                 * sample stopbit(s)
                 */
                String stopText = (String)stop.getSelectedItem();
                a += baud;
                if(stopText.equals("1")) {
                    if(inv.isSelected()) {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    } else {
                        if((analysisData.getDataAt(a) & mask) == 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    }
                } else if(stopText.equals("1.5")) {
                    if(inv.isSelected()) {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    } else {
                        if((analysisData.getDataAt(a) & mask) == 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    }
                    a += (baud / 4);
                    if(inv.isSelected()) {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    } else {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    }
                } else {
                    if(inv.isSelected()) {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    } else {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    }
                    a += baud;
                    if(inv.isSelected()) {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    } else {
                        if((analysisData.getDataAt(a) & mask) != 0) {
                            // framing error
                            if(type == UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA)
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT));
                            else
                                decodedData.add(new UARTProtocolAnalysisDataSet(a,"FRAME_ERR",UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT));
                            detectedErrors++;
                        }
                    }
                }
            }
            progress.setValue(100);
            return(count);
        }
		
        /**
         * exports the data to a CSV file
         * @param file File object
         */
        private void storeToCsvFile(File file) {
            if(decodedData.size() > 0) {
                UARTProtocolAnalysisDataSet dSet;
                System.out.println("writing decoded data to " + file.getPath());
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));

                    bw.write("\"" + 
                             "index" + 
                             "\",\"" +
                             "time" +
                             "\",\"" +
                             "RxD data or event" +
                             "\",\"" +
                             "TxD data or event" +
                             "\"");
                    bw.newLine();

                    for(int i = 0; i < decodedData.size(); i++) {
                        dSet = decodedData.get(i);
                        switch(dSet.type) {
                        case UARTProtocolAnalysisDataSet.UART_TYPE_EVENT:
                            bw.write("\"" + 
                                     i + 
                                     "\",\"" +
                                     indexToTime(dSet.time) +
                                     "\",\"" +
                                     dSet.event +
                                     "\",\"" +
                                     dSet.event +
                                     "\"");
                            break;
                        case UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT:
                            bw.write("\"" + 
                                     i + 
                                     "\",\"" +
                                     indexToTime(dSet.time) +
                                     "\",\"" +
                                     dSet.event +
                                     "\",\"" +
                                     "\"");
                            break;
                        case UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT:
                            bw.write("\"" + 
                                     i + 
                                     "\",\"" +
                                     indexToTime(dSet.time) +
                                     "\",\"" +
                                     "\",\"" +
                                     dSet.event +
                                     "\"");
                            break;
                        case UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA:
                            bw.write("\"" + 
                                     i + 
                                     "\",\"" +
                                     indexToTime(dSet.time) +
                                     "\",\"" +
                                     dSet.data +
                                     "\",\"" +
                                     "\"");
                            break;
                        case UARTProtocolAnalysisDataSet.UART_TYPE_TXDATA:
                            bw.write("\"" + 
                                     i + 
                                     "\",\"" +
                                     indexToTime(dSet.time) +
                                     "\",\"" +
                                     "\",\"" +
                                     dSet.data +
                                     "\"");
                            break;
                        default:
                            break;
                        }
                        bw.newLine();
                    }
                    bw.close();
                } catch (Exception E) {
                    E.printStackTrace(System.out);
                }
            }
        }
		
        /**
         * stores the data to a HTML file
         * @param file file object
         */
        private void storeToHtmlFile(File file) {
            if(decodedData.size() > 0) {
                System.out.println("writing decoded data to " + file.getPath());
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
					
                    // write the complete displayed html page to file
                    bw.write(outText.getText());
					
                    bw.close();
                } catch (Exception E) {
                    E.printStackTrace(System.out);
                }
            }
        }
		
        /**
         * Convert sample count to time string.
         * @param count sample count (or index)
         * @return string containing time information
         */
        private String indexToTime(long count) {
            count -= startOfDecode;
            if(count < 0) count = 0;
            if(analysisData.hasTimingData()) {
                float time = (float)(count * (1.0 / analysisData.rate));
                if(time < 1.0e-6) 			{return(Math.rint(time*1.0e9*100)/100 + "ns");}
                else if(time < 1.0e-3) 		{return(Math.rint(time*1.0e6*100)/100 + "µs");}
                else if(time < 1.0) 		{return(Math.rint(time*1.0e3*100)/100 + "ms");}
                else 						{return(Math.rint(time*100)/100 + "s");}
            } else {
                return("" + count);
            }
        }

        /**
         * generate a HTML page
         * @param empty if this is true an empty output is generated
         * @return String with HTML data
         */
        private String toHtmlPage(boolean empty) {
            Date now = new Date();
            DateFormat df=DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
            int bitCount = Integer.parseInt((String)bits.getSelectedItem());
            int bitAdder = 0;
			
            if(bitCount % 4 != 0) {
                bitAdder = 1;
            }
			
            // generate html page header
            String header =
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">" +
                "<html>" +
                "  <head>" +
                "    <title></title>" +
                "    <meta content=\"\">" +
                "    <style>" +
                "			th { text-align:left;font-style:italic;font-weight:bold;font-size:medium;font-family:sans-serif;background-color:#C0C0FF; }" +
                "		</style>" +
                "  </head>" +
                "	<body>" +
                "		<H2>UART Analysis Results</H2>" +
                "		<hr>" +
                "			<div style=\"text-align:right;font-size:x-small;\">" +
                df.format(now) +
                "           </div>" +
                "		<br>";

            // generate the statistics table
            String stats = new String(); 
            if(!empty) {
                if(bitLength == 0) {
                    stats = stats.concat("<p style=\"color:red;\">Baudrate calculation failed !</p><br><br>");
                } else {
                    stats = stats.concat(
                                         "<table style=\"width:100%;\">" +
                                         "<TR><TD style=\"width:30%;\">Decoded Symbols</TD><TD>" + decodedSymbols + "</TD></TR>" +
                                         "<TR><TD style=\"width:30%;\">Detected Bus Errors</TD><TD>" + detectedErrors + "</TD></TR>" +
                                         "<TR><TD style=\"width:30%;\">Baudrate</TD><TD>" + analysisData.rate / bitLength + "</TD></TR>" +
                                         "</table>" +
                                         "<br>" +
                                         "<br>");
                    if(bitLength < 15) {
                        stats = stats.concat("<p style=\"color:red;\">The baudrate may be wrong, use a higher samplerate to avoid this !</p><br><br>");
                    }
                }
            }

            // generate the data table
            String data =
                "<table style=\"font-family:monospace;width:100%;\">" +
                "<tr><th style=\"width:15%;\">Index</th><th style=\"width:15%;\">Time</th><th style=\"width:10%;\">RxD Hex</th><th style=\"width:10%;\">RxD Bin</th><th style=\"width:8%;\">RxD Dec</th><th style=\"width:7%;\">RxD ASCII</th><th style=\"width:10%;\">TxD Hex</th><th style=\"width:10%;\">TxD Bin</th><th style=\"width:8%;\">TxD Dec</th><th style=\"width:7%;\">TxD ASCII</th></tr>";
            if(empty) {
            } else {
                UARTProtocolAnalysisDataSet ds;
                for (int i = 0; i < decodedData.size(); i++) {
                    ds = decodedData.get(i);
                    switch(ds.type) {
                    case UARTProtocolAnalysisDataSet.UART_TYPE_EVENT:
                        data = data.concat( 
                                           "<tr style=\"background-color:#E0E0E0;\"><td>" +
                                           i +
                                           "</td><td>" +
                                           indexToTime(ds.time) +
                                           "</td><td>" +
                                           ds.event +
                                           "</td><td></td><td></td><td></td><td>" +
                                           ds.event +
                                           "</td><td></td><td></td><td></td></tr>");
                        break;
                    case UARTProtocolAnalysisDataSet.UART_TYPE_RXEVENT:
                        data = data.concat( 
                                           "<tr style=\"background-color:#E0E0E0;\"><td>" +
                                           i +
                                           "</td><td>" +
                                           indexToTime(ds.time) +
                                           "</td><td>" +
                                           ds.event +
                                           "</td><td></td><td></td><td></td><td>" +
                                           "</td><td></td><td></td><td></td></tr>");
                        break;
                    case UARTProtocolAnalysisDataSet.UART_TYPE_TXEVENT:
                        data = data.concat( 
                                           "<tr style=\"background-color:#E0E0E0;\"><td>" +
                                           i +
                                           "</td><td>" +
                                           indexToTime(ds.time) +
                                           "</td><td>" +
                                           "</td><td></td><td></td><td></td><td>" +
                                           ds.event +
                                           "</td><td></td><td></td><td></td></tr>");
                        break;
                    case UARTProtocolAnalysisDataSet.UART_TYPE_RXDATA:
                        data = data.concat(
                                           "<tr style=\"background-color:#FFFFFF;\"><td>" +
                                           i +
                                           "</td><td>" +
                                           indexToTime(ds.time) +
                                           "</td><td>" +
                                           "0x" + integerToHexString(ds.data, bitCount / 4 + bitAdder) +
                                           "</td><td>" +
                                           "0b" + integerToBinString(ds.data, bitCount) +
                                           "</td><td>" +
                                           ds.data +
                                           "</td><td>");
						
                        if((ds.data >= 32) && (bitCount == 8))
                            data += (char)ds.data;
                        data = data.concat("</td><td>" +
                                           "</td><td>" +
                                           "</td><td>" +
                                           "</td><td>");
                        data = data.concat("</td></tr>");

                        break;
                    case UARTProtocolAnalysisDataSet.UART_TYPE_TXDATA:
                        data = data.concat(
                                           "<tr style=\"background-color:#FFFFFF;\"><td>" +
                                           i +
                                           "</td><td>" +
                                           indexToTime(ds.time) +
                                           "</td><td>" +
                                           "</td><td>" +
                                           "</td><td>" +
                                           "</td><td>");

                        data = data.concat("</td><td>" +
                                           "0x" + integerToHexString(ds.data, bitCount / 4 + bitAdder) +
                                           "</td><td>" +
                                           "0b" + integerToBinString(ds.data, bitCount) +
                                           "</td><td>" +
                                           ds.data +
                                           "</td><td>");
						
                        if((ds.data >= 32) && (bitCount == 8))
                            data += (char)ds.data;
                        data = data.concat("</td></tr>");

                        break;
                    default:
                        break;								
                    }
					
                }
            }
            data = data.concat("</table");

            // generate the footer table
            String footer =
                "	</body>" +
                "</html>";

            return(header + stats + data + footer);
        }

        public void readProperties(Properties properties) {
            selectByIndex(rxd, properties.getProperty("tools.UARTProtocolAnalysis.rxd"));
            selectByIndex(txd, properties.getProperty("tools.UARTProtocolAnalysis.txd"));
            selectByIndex(cts, properties.getProperty("tools.UARTProtocolAnalysis.cts"));
            selectByIndex(rts, properties.getProperty("tools.UARTProtocolAnalysis.rts"));			
            selectByIndex(dtr, properties.getProperty("tools.UARTProtocolAnalysis.dtr"));			
            selectByIndex(dsr, properties.getProperty("tools.UARTProtocolAnalysis.dsr"));			
            selectByIndex(dcd, properties.getProperty("tools.UARTProtocolAnalysis.dcd"));			
            selectByIndex(ri, properties.getProperty("tools.UARTProtocolAnalysis.ri"));			
            selectByValue(parity, parityarray, properties.getProperty("tools.UARTProtocolAnalysis.parity"));
            selectByValue(bits, bitarray, properties.getProperty("tools.UARTProtocolAnalysis.bits"));
            selectByValue(stop, stoparray, properties.getProperty("tools.UARTProtocolAnalysis.stop"));
            inv.setSelected(Boolean.parseBoolean(properties.getProperty("tools.UARTProtocolAnalysis.inverted")));
        }

        public void writeProperties(Properties properties) {
            properties.setProperty("tools.UARTProtocolAnalysis.rxd", Integer.toString(rxd.getSelectedIndex()));
            properties.setProperty("tools.UARTProtocolAnalysis.txd", Integer.toString(txd.getSelectedIndex()));
            properties.setProperty("tools.UARTProtocolAnalysis.cts", Integer.toString(cts.getSelectedIndex()));
            properties.setProperty("tools.UARTProtocolAnalysis.rts", Integer.toString(rts.getSelectedIndex()));
            properties.setProperty("tools.UARTProtocolAnalysis.dtr", Integer.toString(dtr.getSelectedIndex()));
            properties.setProperty("tools.UARTProtocolAnalysis.dsr", Integer.toString(dsr.getSelectedIndex()));
            properties.setProperty("tools.UARTProtocolAnalysis.dcd", Integer.toString(dcd.getSelectedIndex()));
            properties.setProperty("tools.UARTProtocolAnalysis.ri", Integer.toString(ri.getSelectedIndex()));
            properties.setProperty("tools.UARTProtocolAnalysis.parity", (String)parity.getSelectedItem());
            properties.setProperty("tools.UARTProtocolAnalysis.bits", (String)bits.getSelectedItem());
            properties.setProperty("tools.UARTProtocolAnalysis.stop", (String)stop.getSelectedItem());
            properties.setProperty("tools.UARTProtocolAnalysis.inverted", "" + inv.isSelected());
        }
		
        /**
         * converts an integer to a hex string with leading zeros
         * @param val integer value for conversion
         * @param fieldWidth number of charakters in field
         * @return a nice string
         */
        private String integerToHexString(int val, int fieldWidth) {
            // first build a mask to cut off the signed extension
            int mask = (int)Math.pow(16.0, (double)fieldWidth);
            mask--;
            String str = Integer.toHexString(val & mask);
            int numberOfLeadingZeros = fieldWidth - str.length();
            if(numberOfLeadingZeros < 0) numberOfLeadingZeros = 0;
            if(numberOfLeadingZeros > fieldWidth) numberOfLeadingZeros = fieldWidth;
            char zeros[] = new char[numberOfLeadingZeros];
            for(int i = 0; i < zeros.length; i++)
                zeros[i] = '0';
            String ldz = new String(zeros);
            return(new String(ldz + str));
        }

        /**
         * converts an integer to a bin string with leading zeros
         * @param val integer value for conversion
         * @param fieldWidth number of charakters in field
         * @return a nice string
         */
        private String integerToBinString(int val, int fieldWidth) {
            // first build a mask to cut off the signed extension
            int mask = (int)Math.pow(2.0, (double)(fieldWidth));
            mask--;
            String str = Integer.toBinaryString(val & mask);
            int numberOfLeadingZeros = fieldWidth - str.length();
            if(numberOfLeadingZeros < 0) numberOfLeadingZeros = 0;
            if(numberOfLeadingZeros > fieldWidth) numberOfLeadingZeros = fieldWidth;
            char zeros[] = new char[numberOfLeadingZeros];
            for(int i = 0; i < zeros.length; i++)
                zeros[i] = '0';
            String ldz = new String(zeros);
            return(new String(ldz + str));
        }

        /**
         * runs the conversion when started
         */
        public void run() {
            setControlsEnabled(false);
            btnConvert.setText("Abort");
            decode();
            setControlsEnabled(true);
            btnConvert.setText("Analyze");
        }

        private String[] parityarray;
        private String[] bitarray;
        private String[] stoparray;

        private JComboBox rxd;
        private JComboBox txd;
		
        private JComboBox cts;
        private JComboBox rts;
        private JComboBox dtr;
        private JComboBox dsr;
        private JComboBox dcd;
        private JComboBox ri;
		
        private JComboBox parity;
        private JComboBox bits;
        private JComboBox stop;
        private JCheckBox inv;
		
        private JButton btnConvert;
        private JButton btnExport;
        private JButton btnCancel;
		
        private JProgressBar progress;
        private boolean runFlag;
		
        private Thread thrWorker;
		
        private CapturedData analysisData;
        private JEditorPane outText;
        private Vector<UARTProtocolAnalysisDataSet> decodedData;
        private JFileChooser fileChooser;
        private long startOfDecode;
        private long endOfDecode;
        private int decodedSymbols;
        private int bitLength;
        private int detectedErrors;
		
        private static final long serialVersionUID = 1L;
    }
	
    /**
     * Inner class defining a File Filter for CSV files. 
     * 
     */
    private class CSVFilter extends FileFilter {
        public boolean accept(File f) {
            return (f.isDirectory() || f.getName().toLowerCase().endsWith(".csv"));
        }
        public String getDescription() {
            return ("Character sepatated Values (*.csv)");
        }
    }

    /**
     * Inner class defining a File Filter for HTML files. 
     * 
     */
    private class HTMLFilter extends FileFilter {
        public boolean accept(File f) {
            return (f.isDirectory() || f.getName().toLowerCase().endsWith(".html"));
        }
        public String getDescription() {
            return ("Website (*.html)");
        }
    }

    /**
     * Inner class for statistical baudrate analysis
     *
     */
    private class BaudRateAnalyzer {
        /*
         * create a histogram that allows to evaluate each
         * detected bitlength. The bitlength with the highest
         * occurrence is used for baudrate calculation.
         */
        public BaudRateAnalyzer(int[] data, long[] time, int mask) {
            int a,b,c;
            int[] valuePair;
            long last = 0;
            b = data[0] & mask;
            a = 0;
            statData = new LinkedList<int[]>();
            for(int i=0;i<data.length;i++) {
                if(b != (data[i] & mask)) {
                    a = (int)(time[i] - last);
                    c = findValue(a);
                    if(c < 0) {
                        valuePair = new int[2];
                        valuePair[0] = a; // bitlength
                        valuePair[1] = 1; // count
                        statData.add(valuePair);
                    } else {
                        statData.get(c)[1]++;
                    }
                    last = time[i];
                }
                b = data[i] & mask;
            }
        }
		
        private int findValue(int val) {
            for(int i=0;i<statData.size();i++) {
                if(statData.get(i)[0] == val)
                    return i;
            }
            return -1;
        }
		
        public int getMax() {
            int max = 0;
            for(int i=0;i<statData.size();i++) {
                if(statData.get(i)[0] > max) max = statData.get(i)[0];
            }
            return max;
        }
        public int getMin() {
            int min = Integer.MAX_VALUE;
            for(int i=0;i<statData.size();i++) {
                if(statData.get(i)[0] < min) min = statData.get(i)[0];
            }
            return min;
        }
        public int getBest() {
            int rank = 0;
            int index = 0;
            for(int i=0;i<statData.size();i++) {
                if(statData.get(i)[1] > rank) {
                    rank = statData.get(i)[1];
                    index = i;
                }
            }
            if(statData.size() == 0)
                return 0;
            return statData.get(index)[0];
        }
        public String toString() {
            return new String("BaudRateAnalyzer:min=" + getMin() + ":max=" + getMax() + ":best=" + getBest());
        }
		
        /*
         * Store as linked list with 2 int sized arrays as elements.
         * Each array element stores at index 0 the bitlength and at
         * index 1 the number of occurrences.
         */
        private LinkedList<int[]> statData;
    }


    private UARTProtocolAnalysisDialog spad;

	
    public UARTProtocolAnalysis () {
        super();
    }
	
    public void init(Frame frame) {
        spad = new UARTProtocolAnalysisDialog(frame, getName());
    }
	
    /**
     * Returns the tools visible name.
     * @return the tools visible name
     */
    public String getName() {
        return ("UART Protocol Analysis...");
    }

    /**
     * Convert captured data from timing data to state data using the given channel as clock.
     * @param data - captured data to work on
     * @return always <code>null</code>
     */
    public CapturedData process(CapturedData data) {
        spad.showDialog(data);
        return null;
    }
	
    /**
     * Reads dialog settings from given properties.
     * @param properties Properties containing dialog settings
     */
    public void readProperties(Properties properties) {
        spad.readProperties(properties);
    }

    /**
     * Writes dialog settings to given properties.
     * @param properties Properties where the settings are written to
     */
    public void writeProperties(Properties properties) {
        spad.writeProperties(properties);
    }
}
