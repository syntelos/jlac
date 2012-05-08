/*
 *  Copyright (C) 2006 Michael Poppitz
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
package org.sump.analyzer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputListener;

import org.sump.util.Properties;

/**
 * This component displays a diagram which is obtained from a {@link CapturedData} object.
 * The settings for the diagram are obtained from the embedded {@link DiagramSettings} and {@link DiagramLabels} objects.
 * Look there for an overview of ways to display data.
 * <p>
 * Component size changes with the size of the diagram.
 * Therefore it should only be used from within a JScrollPane.
 *
 * @version 0.8
 * @author Michael "Mr. Sump" Poppitz
 * @author John Pritchard
 */
public class Diagram 
    extends JComponent 
    implements MouseInputListener, Configurable, ActionListener
{

    private static final long serialVersionUID = 1L;
	
    private CapturedData capturedData;
    private DiagramSettings settings;
    private DiagramLabels labels;
    private long unitFactor;
    private String unitName;
	
    private int offsetX;
    private int offsetY;
    private int mouseX;
    private int mouseY;
    private int mouseDragX;
    private StatusChangeListener statusChangeListener;
	
    private double scale;
    private double maxScale;
    private int timeDivider;
    private int currentPage;
    private int maxPages;
    private int pageLen;
	
    private Color signal;
    private Color trigger;
    private Color grid;
    private Color text;
    private Color time;
    private Color groupBackground;
    private Color background;
    private Color label;
    private Color cursorA;
    private Color cursorB;
	
    private int draggedCursor;
    private Cursor cursorDefault;
    private Cursor cursorDrag;
    private Vector<DiagramCursorChangeListener> curListners;
	
    private Point contextMenuPosition;
    private JPopupMenu contextMenu;
	
    private Dimension size;


    /*
     * TODO: Optimization: drawEdge is called many times with data containing many signal transitions.
     * - Optimize 1: draw a rectangle instead of the edges when zoomed to fit
     * - Optimize 2: enable double buffering could increase performance
     */
	

	
    /**
     * Create a new empty diagram to be placed in a container.
     */
    public Diagram() {
        super();
		
        this.size = new Dimension(25, 1);
		
        this.signal = new Color(0,0,196);
        this.trigger = new Color(196,255,196);
        this.grid = new Color(196,196,196);
        this.text = new Color(0,0,0);
        this.time = new Color(0,0,0);
        this.groupBackground = new Color(242,242,242);
        this.background = new Color(255,255,255);
        this.label = new Color(255,196,196);
        this.cursorA = new Color(190,120,0);
        this.cursorB = new Color(190,120,0);
		
        this.offsetX = 25;
        this.offsetY = 18;
		
        zoomDefault();
        setBackground(background);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.contextMenuPosition = new Point();
        this.contextMenu = new JPopupMenu();
        JMenuItem gotoA = new JMenuItem("Set Cursor A");
        gotoA.addActionListener(this);
        this.contextMenu.add(gotoA);
        JMenuItem gotoB = new JMenuItem("Set Cursor B");
        gotoB.addActionListener(this);
        this.contextMenu.add(gotoB);

        this.settings = new DiagramSettings();
        this.capturedData = null;
		
        this.labels = new DiagramLabels();
		
        this.cursorDefault = this.getCursor();
        this.cursorDrag = new Cursor(Cursor.MOVE_CURSOR);
        this.draggedCursor = 0;
        this.curListners = new Vector<DiagramCursorChangeListener>();
		
        this.maxScale = 10.0;
        this.timeDivider = 1;
        this.maxPages = 1;
        this.currentPage = 0;
        this.pageLen = 0;
    }
	
    public void addCursorChangeListener(DiagramCursorChangeListener listener) {
        this.curListners.add(listener);
    }
	
    /**
     * Resizes the diagram as required by available data and scaling factor.
     *
     */
    private void resize() {
        if (capturedData == null)
            return;

        int height = 20;
        for (int group = 0; group < capturedData.channels / 8 && group < 4; group++)
            if (((capturedData.enabledChannels >> (8 * group)) & 0xff) != 0) {
                if ((settings.groupSettings[group] & DiagramSettings.DISPLAY_CHANNELS) > 0)
                    height += 20 * 8;
                if ((settings.groupSettings[group] & DiagramSettings.DISPLAY_SCOPE) > 0)
                    height += 133;
                if ((settings.groupSettings[group] & DiagramSettings.DISPLAY_BYTE) > 0)
                    height += 20;
            }
		
        long width = (long)(scale * capturedData.absoluteLength);
        width /= maxPages;
        width += 25;
		
        //		System.out.println("set width=" + width);
		
        Rectangle rect = getBounds();
        rect.setSize((int)width, height);
        setBounds(rect);
        size.width = (int)width;
        size.height = height;
		
        /**
         * !!! fk - Update call removed !!!
         * It makes problems on a openSuSE 10.2 system with NVIDIA 3D activated with 
         * Java 1.5 release 11. It causes an X-Server session crash (blackout).
         * Regardless of this the update is done (may be implicitely called by the 
         * setBounds method ?)
         * 
         * now repaint() is used instead.
         */
        //update(this.getGraphics());
        this.repaint();
    }
	
    /**
     * Sets the captured data object to use for drawing the diagram.
     * 
     * @param capturedData		captured data to base diagram on
     */
    public void setCapturedData(CapturedData capturedData) {
        this.capturedData = capturedData;
		
        // reset zoom, etc.
        scale = maxScale;
        zoomDefault();
        timeDivider = 1;
        maxPages = 1;
        currentPage = 0;
        pageLen = 0;
		
        // show data
        calculateUnits();
        resize();
    }

    private void calculateUnits() {
        if (capturedData != null && capturedData.hasTimingData()) {
            double step = (1000.0 / scale) / capturedData.rate;
			
            unitFactor = 1;
            unitName = "s";
            if (step < 0.000001) { unitFactor = 1000000000; unitName = "ns"; } 
            else if (step < 0.001) { unitFactor = 1000000; unitName = "\u00b5s"; } 
            else if (step < 1) { unitFactor = 1000; unitName = "ms"; } 
			
            //			System.out.println("scale=" + scale);
            //			System.out.println("step=" + step);
            //			System.out.println("rate=" + capturedData.rate);
            //			System.out.println("unitFactor=" + unitFactor);
            //			System.out.println("unitName=" + unitName);
        } else {
            unitFactor = 1;
            unitName = "";
        }
    }
	
    /**
     * Returns the captured data object currently displayed in the diagram.
     * 
     * @return diagram's current captured data
     */
    public CapturedData getCapturedData() {
        return (capturedData);
    }

    /**
     * Returns wheter or not the diagram has any data.
     * 
     * @return <code>true</code> if captured data exists, <code>false</code> otherwise
     */
    public boolean hasCapturedData() {
        return (capturedData != null);
    }
	
    /**
     * Zooms in by factor 2 and resizes the component accordingly.
     *
     */
    public void zoomIn() {
        if (scale < maxScale) {
            scale = scale * 2;
            if(scale > maxScale) scale = maxScale;
            calculatePages();
            calculateUnits();
            resize();
        }
    }
	
    /**
     * Zooms out by factor 2 and resizes the component accordingly.
     *
     */
    public void zoomOut() {
        scale = scale / 2;
        calculatePages();
        calculateUnits();
        resize();
    }
	
    /**
     * Reverts back to the standard zoom level.
     *
     */
    public void zoomDefault() {
        scale = maxScale;
        calculatePages();
        calculateUnits();
        resize();
    }

    /**
     * Zooms to fitting the view on Display.
     *
     */
    public void zoomFit(int width) {
        // reverse the scaling
        width -= 25;
        if(width < 1) width = 1;

        // avoid null pointer exception when no data available
        if(capturedData == null) return;

        if(capturedData.absoluteLength > 0)
            scale = (double)width / (double)capturedData.absoluteLength;
        else
            scale = maxScale;
		
        calculatePages();
        calculateUnits();
        resize();
    }
	
    /**
     * calculate number and size of pages for various zoom levels.
     */
    private void calculatePages() {
        if(capturedData != null) {
            double maxAvailableWidth = Integer.MAX_VALUE - 100;
            double currentScaledSize = (long)(scale * capturedData.absoluteLength);
            if(currentScaledSize > maxAvailableWidth) {
                maxPages = (int)Math.ceil(currentScaledSize / maxAvailableWidth);
                pageLen = (int)(scale * capturedData.absoluteLength / maxPages);
            } else {
                maxPages = 1;
                pageLen = (int)(scale * capturedData.absoluteLength);
            }
        } else {
            maxPages = 1;
            pageLen = 0;
        }
        //		System.out.println("maxPages=" + maxPages + " pageLen=" + pageLen);
    }
	
    /**
     * calulate the position within a window (pane) based on current page and zoom settings
     * @param width window width
     * @param pos sample position
     * @return current position within window
     */
    public int getTargetPosition(int width, long pos) {
        pos -= getPageOffset();
        if(pos < 0) pos = 0;
        return (int)((double)pos * (double)width * (double)scale / (double)pageLen);
    }

    /**
     * @param page set the current page
     */
    public void setCurrentPage(int page) {
        currentPage = page;
        resize();
    }
	
    /**
     * get the page number based on a sample position
     * @param pos sample position
     * @return page number
     */
    public int getPage(long pos) {
        if(capturedData != null) {
            return (int)(pos * maxPages / capturedData.absoluteLength);
        } else {
            return 0;
        }
    }

    /**
     * @return the maxPages
     */
    public int getMaxPages() {
        return maxPages;
    }

    /**
     * @return the sample offset of the current page
     */
    public long getPageOffset() {
        return (currentPage * capturedData.absoluteLength / maxPages);
    }

    /**
     * Display the diagram settings dialog.
     * Will block until the dialog is closed again.
     *
     */
    public void showSettingsDialog(Frame frame) {
        if (settings.showDialog(frame) == DiagramSettings.OK)
            resize();
    }

    /**
     * Display the diagram labels dialog.
     * Will block until the dialog is closed again.
     *
     */
    public void showLabelsDialog(Frame frame) {
        if (labels.showDialog(frame) == DiagramLabels.OK)
            resize();
    }

    /**
     * Gets the dimensions of the full diagram.
     * Used to inform the container (preferrably a JScrollPane) about the size.
     */
    @Override
    public Dimension getPreferredSize() {
        return (size);
    }
	
    /**
     * Gets the dimensions of the full diagram.
     * Used to inform the container (preferrably a JScrollPane) about the size.
     */
    @Override
    public Dimension getMinimumSize() {
        return (size);
    }

    /**
     * Enable/Disable diagram cursors
     */
    public void setCursorMode(boolean enabled) {
        if(this.hasCapturedData()) capturedData.cursorEnabled = enabled;
        updateStatus(false);
        resize();
    }
    /**
     * get current cursor mode
     */
    public boolean getCursorMode() {
        if(this.hasCapturedData())
            return capturedData.cursorEnabled;
        else
            return false;
    }

    private void drawEdge(Graphics g, int x, int y, boolean falling, boolean rising) {
        if (scale <= 1) {
            g.drawLine(x, y, x, y + 14);
        } else {
            int edgeX = x;
            if (scale >= 5)
                edgeX += (int)(scale * 0.4);
	
            if (rising) {
                g.drawLine(x, y + 14, edgeX, y);
                g.drawLine(edgeX, y, x + (int)scale, y);
            }	
            if (falling) {
                g.drawLine(x, y, edgeX, y + 14);
                g.drawLine(edgeX, y + 14, x + (int)scale, y + 14);
            }	
        }
    }
    /**
     * Draws a channel.
     * @param g graphics context to draw on
     * @param x x offset
     * @param y y offset
     * @param data array containing the sampled data
     * @param n number of channel to display
     * @param from index of first sample to display
     * @param to index of last sample to display
     */
    private void drawChannel(Graphics g, int x, int y, int[] data, long[] time, int n, long from, long to) {
        int dataIndex = 0;

        //		boolean printit = true;
		
        from /= timeDivider;
        to /= timeDivider;
		
        do {
            if((time[dataIndex] / timeDivider) > from) 	break;
            dataIndex++;
        } while(dataIndex < time.length);
        if(dataIndex > 0) dataIndex--;
		
        for (long current = from; current < to;) {
            //			if(printit) {
            //				System.out.println("X=" + x + " current=" + current + " getPageOffset=" + getPageOffset());
            //			}
            int currentX = (int)((current - getPageOffset()) * scale * timeDivider);
            int currentV = (data[dataIndex] >> n) & 0x01;
            int nextV = currentV;
            long next = current;
	
            // here is a transition
            dataIndex++;
            if(dataIndex < data.length) {
                nextV = (data[dataIndex] >> n) & 0x01;
                next = time[dataIndex] / timeDivider;
            } else {
                next = to;
            }
            if(next >= to) next = to + 1;
			
            if(currentX < 0) currentX = 0;
            currentX += x;
			
            int currentEndX = currentX + (int)(scale * (next - current - 1) * timeDivider);
			
            //			if(printit) {
            //				System.out.println("currentX=" + currentX + " currentEndX=" + currentEndX + " next=" + next + " current=" + current + " poffs=" + getPageOffset());
            //				printit = false;
            //			}
			
            // draw straight line up to the point of change and a edge if not at end
            if (currentV == nextV) {
                g.drawLine(currentX, y + 14 * (1 - currentV), currentEndX + (int)(scale * timeDivider), y + 14 * (1 - currentV));
            } else {
                g.drawLine(currentX, y + 14 * (1 - currentV), currentEndX, y + 14 * (1 - currentV));
                if (currentV > nextV)
                    drawEdge(g, currentEndX, y, true, false);
                else if (currentV < nextV)
                    drawEdge(g, currentEndX, y, false, true);
            }
            current = next;
        }
    }
	
    private void drawGridLine(Graphics g, Rectangle clipArea, int y) {
        g.setColor(grid);
        g.drawLine(clipArea.x, y, clipArea.x + clipArea.width, y);
    }
	
    /**
     * Draws a byte bar.
     * @param g graphics context to draw on
     * @param x x offset
     * @param y y offset
     * @param data array containing the sampled data
     * @param n number of group to display (0-3 for 32 channels)
     * @param from index of first sample to display
     * @param to index of last sample to display
     */
    private int drawGroupByte(Graphics g, int x, int y, int[] data, long[] time, Rectangle clipArea, int n, long from, long to) {
		
        int dataIndex = 0;
        // find the time index one before "from" 
		
        from /= timeDivider;
        to /= timeDivider;
		
        do {
            if((time[dataIndex] / timeDivider) > from) 	break;
            dataIndex++;
        } while(dataIndex < time.length);
        if(dataIndex > 0) dataIndex--;

        // draw background
        g.setColor(groupBackground);
        g.fillRect(clipArea.x, y, clipArea.width, 19);
        g.setColor(text);
        g.drawString("B" + n, 5, y + 14);
        // draw bottom grid line
        drawGridLine(g, clipArea, y + 19);
		
        g.setColor(signal);
		
        int yOfs = y + 2;
        int h = 14;

        for (long current = from; current < to;) {
            int currentX = (int)((current - getPageOffset()) * scale * timeDivider);
            int currentXSpace = (int)(x + (current - 1) * scale * timeDivider);
            int currentV = (data[dataIndex] >> (8 * n)) & 0xff;
            int nextV = currentV;
            long next = current;

			
            // here is a transition
            dataIndex++;
            if(dataIndex < data.length) {
                nextV = (data[dataIndex] >> n) & 0x01;
                next = time[dataIndex] / timeDivider;
            } else {
                next = to;
            }
            if(next >= to) next = to + 1;

            if(currentX < 0) currentX = 0;
            currentX += x;

            int currentEndX = currentX + (int)(scale * (next - current - 1) * timeDivider);
			
            // draw straight lines up to the point of change and a edge if not at end
            if (currentV == nextV) {
                g.drawLine(currentX, yOfs + h, currentEndX + (int)scale * timeDivider, yOfs + h);
                g.drawLine(currentX, yOfs, currentEndX + (int)scale * timeDivider, yOfs);
            } else {
                g.drawLine(currentX, yOfs + h, currentEndX, yOfs + h);
                g.drawLine(currentX, yOfs, currentEndX, yOfs);
                drawEdge(g, currentEndX, yOfs, true, true);
            }
			
            // if steady long enough, add hex value
            if (currentEndX - currentXSpace > 15) {
                if (currentV >= 0x10)
                    g.drawString(Integer.toString(currentV, 16), (currentXSpace + currentEndX) / 2 - 2, y + 14);
                else
                    g.drawString("0" + Integer.toString(currentV, 16), (currentXSpace + currentEndX) / 2 - 2, y + 14);

            }
			
            current = next;
        }
        return (20);
    }
	
    private int drawGroupAnalyzer(Graphics g, int xofs, int yofs, int data[], long[] time, Rectangle clipArea, int n, long from, long to, String labels[]) {
        // draw channel separators
        for (int bit = 0; bit < 8; bit++) {
            g.setColor(grid);
            g.drawLine(clipArea.x, 20 * bit + yofs + 19, clipArea.x + clipArea.width, 20 * bit + yofs + 19);
            g.setColor(text);
            g.drawString("" + (bit + n * 8), 5, 20 * bit + yofs + 14);
            g.setColor(label);
            if(labels[bit + n * 8] != null)
                {
                    if(clipArea.x < xofs)
                        g.drawString(labels[bit + n * 8], xofs, 20 * bit + yofs + 14);
                    else
                        g.drawString(labels[bit + n * 8], clipArea.x, 20 * bit + yofs + 14);
                }
        }
		
        // draw actual data
        g.setColor(signal);
        for (int bit = 0; bit < 8; bit++)
            drawChannel(g, xofs, yofs + 20 * bit + 2, data, time, 8 * n + bit, from, to);

        return (20 * 8);
    }

    private int drawGroupScope(Graphics g, int x, int y, int data[], long[] time, Rectangle clipArea, int n, long from, long to) {
        int dataIndex = 0;
        // find the time index one before "from" 
		
        from /= timeDivider;
        to /= timeDivider;
		
        do {
            if((time[dataIndex] / timeDivider) > from) 	break;
            dataIndex++;
        } while(dataIndex < time.length);
        if(dataIndex > 0) dataIndex--;

        // draw label
        g.setColor(text);
        g.drawString("S" + n, 5, y + 70);
		
        //		System.out.println("Scope: from=" + from + " to=" + to);

        // draw actual data
        g.setColor(signal);
        int last = (255 - ((data[dataIndex] >> (n * 8)) & 0xff)) / 2;
        int val = (255 - ((data[dataIndex] >> (n * 8)) & 0xff)) / 2;
        int oldPosTmp = calcTmpPos(from);
        oldPosTmp += x;
        int posTmp;
        for (long pos = from; pos < to; ) {
            long oldPos = pos;
            pos = time[dataIndex] / timeDivider;
            if(pos > oldPos) {
                val = (255 - ((data[dataIndex] >> (n * 8)) & 0xff)) / 2;
					
                oldPosTmp = calcTmpPos(oldPos);
                oldPosTmp += x;
				
                posTmp = calcTmpPos(pos);
                posTmp += x;

                //				System.out.println("Scope: oldPos=" + oldPos + " pos=" + pos + " val=" + val + " idx=" + dataIndex + " posTmp=" + posTmp + " oldPosTmp=" + oldPosTmp + " pageOffs=" + getPageOffset());
					
                g.drawLine(oldPosTmp, y + 2 + last, posTmp, y + 2 + val);

                last = val;
            }
            dataIndex++;
            if(dataIndex >= time.length) break;
        }
        posTmp = calcTmpPos(to);
        posTmp += x;

        //		System.out.println("Scope: val=" + val + " posTmp=" + posTmp + " oldPosTmp=" + oldPosTmp);
		
        g.drawLine(oldPosTmp, y + 2 + last, posTmp, y + 2 + val);
		
        // draw bottom grid line
        drawGridLine(g, clipArea, y + 132);

        return (133);
    }

    private int calcTmpPos(long pos) {
        long lval = (long)((pos * timeDivider - getPageOffset()) * scale);
        if(lval >= Integer.MAX_VALUE) lval = Integer.MAX_VALUE - 100;
        if(lval < 0) lval = 0;
        return (int)lval;
    }
	
    /**
     * Paints the diagram to the extend necessary.
     */
    @Override
    public void paintComponent(Graphics g) {
        if (capturedData == null)
            return;
		
        boolean hasTiming = capturedData.hasTimingData();
        boolean hasTrigger = capturedData.hasTriggerData();
        int channels = capturedData.channels;
        int enabled = capturedData.enabledChannels;
        long triggerPosition = capturedData.triggerPosition;
        if (!hasTrigger)
            triggerPosition = 0;
        int rate = capturedData.rate;
        if (!hasTiming)	// value of rate is only valid if timing data exists
            rate = 1;
		
        int xofs = offsetX;
        int yofs = offsetY + 2;

        // obtain portion of graphics that needs to be drawn
        Rectangle clipArea = g.getClipBounds();

        //		System.out.println("clip: x=" + clipArea.x + " y=" + clipArea.y + " w=" + clipArea.width + " h=" + clipArea.height);
		
        // find index of first row that needs drawing
        long firstRow = xToIndex(clipArea.x);
			
        // find index of last row that needs drawing
        long lastRow = xToIndex(clipArea.x + clipArea.width) + 1;

        // calculate time divider for samplecount > 2^31-1
        long visibleSamples = lastRow - firstRow;
        timeDivider = 1;
        while((visibleSamples / timeDivider) >= Integer.MAX_VALUE) timeDivider++;
	
        //		System.out.println("first=" + firstRow + " last=" + lastRow + " visible=" + visibleSamples + " divider=" + timeDivider + " scale=" + scale + " pages=" + maxPages + " pageLen=" + pageLen);
		
        // paint portion of background that needs drawing
        g.setColor(background);
        g.fillRect(clipArea.x, clipArea.y, clipArea.width, clipArea.height);

        // draw trigger if existing and visible
        if (hasTrigger && triggerPosition >= firstRow && triggerPosition <= lastRow) {
            g.setColor(trigger);
            g.fillRect(xofs + (int)((triggerPosition - getPageOffset()) * scale) - 1, 0, (int)(scale) + 2, yofs + 36 * 20);		
        }
		
        // draw time line
        int rowInc = (int)(10 / scale);
        int timeLineShift = (int)(triggerPosition % rowInc);
        g.setColor(time);
        for (long row = ( firstRow / rowInc) * rowInc + timeLineShift; row < lastRow; row += rowInc) {
            int pos = (int)(scale * (row - getPageOffset()));
            if(pos < 0) pos = 0;
            pos += xofs;
            if (((row - triggerPosition) / rowInc) % 20 == 0) {
                g.drawLine(pos, 1, pos, 15);
                if (hasTiming) {
                    NumberFormat nf = NumberFormat.getInstance();
                    nf.setMaximumFractionDigits(15);
                    nf.setMinimumFractionDigits(1);
                    g.drawString(nf.format((double)(row - triggerPosition) / (double)rate) + " sec", pos + 5, 10);
                } else
                    g.drawString(Long.toString(row - triggerPosition), pos + 5, 10);
            } else {
                g.drawLine(pos, 12, pos, 15);
            }
        }

        // draw groups
        int bofs = yofs;
        drawGridLine(g, clipArea, bofs++);
        for (int block = 0; block < channels / 8; block++)
            if (((enabled >> (8 * block)) & 0xff) != 0) {
                if (block < 4 && (settings.groupSettings[block] & DiagramSettings.DISPLAY_CHANNELS) > 0)
                    bofs += drawGroupAnalyzer(g, xofs, bofs, capturedData.values, capturedData.timestamps, clipArea, block, firstRow, lastRow, labels.diagramLabels);
                if (block < 4 && (settings.groupSettings[block] & DiagramSettings.DISPLAY_SCOPE) > 0)
                    bofs += drawGroupScope(g, xofs, bofs, capturedData.values, capturedData.timestamps, clipArea, block, firstRow, lastRow);
                if (block < 4 && (settings.groupSettings[block] & DiagramSettings.DISPLAY_BYTE) > 0)
                    bofs += drawGroupByte(g, xofs, bofs, capturedData.values, capturedData.timestamps, clipArea, block, firstRow, lastRow);
            }
		
        // draw cursors if enabled
        if(capturedData.cursorEnabled) {
            // draw cursor B first (lower priority)
            if (capturedData.getCursorPositionB() >= firstRow && capturedData.getCursorPositionB() <= lastRow) {
                g.setColor(background);
                g.fillRect(xofs + (int)((capturedData.getCursorPositionB() - getPageOffset()) * scale), 0, 8, 12);
                g.setColor(cursorB);
                g.drawRect(xofs + (int)((capturedData.getCursorPositionB() - getPageOffset()) * scale), 0, 8, 12);
                g.drawLine(xofs + (int)((capturedData.getCursorPositionB() - getPageOffset()) * scale), 
                           0, 
                           xofs + (int)((capturedData.getCursorPositionB() - getPageOffset()) * scale), 
                           yofs + 36 * 20);
                g.drawString("B",xofs + (int)((capturedData.getCursorPositionB() - getPageOffset()) * scale) + 1, 11);
            }
            // draw cursor A last (higher priority)
            if (capturedData.getCursorPositionA() >= firstRow && capturedData.getCursorPositionA() <= lastRow) {
                g.setColor(background);
                g.fillRect(xofs + (int)((capturedData.getCursorPositionA() - getPageOffset()) * scale), 0, 8, 12);
                g.setColor(cursorA);
                g.drawRect(xofs + (int)((capturedData.getCursorPositionA() - getPageOffset()) * scale), 0, 8, 12);
                g.drawLine(xofs + (int)((capturedData.getCursorPositionA() - getPageOffset()) * scale), 
                           0, 
                           xofs + (int)((capturedData.getCursorPositionA() - getPageOffset()) * scale), 
                           yofs + 36 * 20);
                g.drawString("A",xofs + (int)((capturedData.getCursorPositionA() - getPageOffset()) * scale), 11);
            }
        }
    }
	
    /**
     * Convert x position to sample index.
     * @param x horizontal position in pixels
     * @return sample index
     */
    private long xToIndex(int x) {
        long index = (long)((x - offsetX) / scale);
        index += getPageOffset();
        if (index < 0)
            index = 0;
        if (index >= capturedData.absoluteLength)
            index = capturedData.absoluteLength - 1;
        return (index);
    }
	
    /**
     * Convert sample count to time string.
     * @param count sample count (or index)
     * @return string containing time information
     */
    private String indexToTime(long count) {
        double time = (((double)count * (double)unitFactor) / (double)capturedData.rate); 
        return (String.format("%.3f", time) + unitName);
    }

    /**
     * Update status information.
     * Notifies {@link StatusChangeListener}.
     * @param dragging <code>true</code> indicates that dragging information should be added 
     */
    private void updateStatus(boolean dragging) {
        if (capturedData == null || statusChangeListener == null)
            return;

        StringBuffer sb = new StringBuffer(" ");
		
        int row = (mouseY - offsetY) / 20;
        if (row <= capturedData.channels + (capturedData.channels / 9)) {
            if (row % 9 == 8)
                sb.append("Byte " + (row / 9));
            else
                sb.append("Channel " + (row - (row / 9)));
            sb.append(" | ");
        }

        if(capturedData.cursorEnabled) {
            // print cursor data to status line
            if(!capturedData.hasTimingData()) {
                sb.append("Sample@A=" + (capturedData.getCursorPositionA() - capturedData.triggerPosition));
                sb.append(" | Sample@B=" + (capturedData.getCursorPositionB() - capturedData.triggerPosition));
                sb.append(" | Distance(A,B)=" + (capturedData.getCursorPositionB() - capturedData.getCursorPositionA()));
            } else {
                float frequency = 0;
                if(capturedData.getCursorPositionA() == capturedData.getCursorPositionB()) {
                    // no difference between cursors --> infinite frequency
                } else {
                    frequency = Math.abs((float)capturedData.rate / (float)(capturedData.getCursorPositionA() - capturedData.getCursorPositionB()));
                }
                String unit;
                int div;
                if (frequency >= 1000000) { unit = "MHz"; div = 1000000; }
                else if (frequency >= 1000) { unit = "kHz"; div = 1000; }
                else { unit = "Hz"; div = 1; } 
                sb.append("Time@A=" + indexToTime(capturedData.getCursorPositionA() - capturedData.triggerPosition));
                sb.append(" | Time@B=" + indexToTime(capturedData.getCursorPositionB() - capturedData.triggerPosition));
                sb.append(" (Duration " + indexToTime(Math.abs(capturedData.getCursorPositionA() - capturedData.getCursorPositionB())) + ", ");
                if(frequency != 0)
                    sb.append("Frequency " + (frequency / (float)div) + unit + ")");
                else
                    sb.append("Frequency undefined)");
            }
        } else {
            // print origin status when no cursors used
            if (dragging && xToIndex(mouseDragX) != xToIndex(mouseX)) {
                long index = xToIndex(mouseDragX);
				
                if (!capturedData.hasTimingData()) {
                    sb.append("Sample " + (index - capturedData.triggerPosition));
                    sb.append(" (Distance " + (index - xToIndex(mouseX)) + ")");
                } else {
                    float frequency = Math.abs((float)capturedData.rate / (float)(index - xToIndex(mouseX)));
                    String unit;
                    int div;
                    if (frequency >= 1000000) { unit = "MHz"; div = 1000000; }
                    else if (frequency >= 1000) { unit = "kHz"; div = 1000; }
                    else { unit = "Hz"; div = 1; } 
                    sb.append("Time " + indexToTime(index - capturedData.triggerPosition));
                    sb.append(" (Duration " + indexToTime(index - xToIndex(mouseX)) + ", ");
                    sb.append("Frequency " + (frequency / (float)div) + unit + ")");
                }
            } else {
                if (!capturedData.hasTimingData())
                    sb.append("Sample " + (xToIndex(mouseX) - capturedData.triggerPosition));
                else
                    sb.append("Time " + indexToTime(xToIndex(mouseX) - capturedData.triggerPosition));
            }
        }
        statusChangeListener.statusChanged(sb.toString());
    }
	
    private void updateCursors(boolean dragged) {
        if (capturedData == null)
            return;
		
        long index;
		
        if(capturedData.cursorEnabled) {
            if(dragged) {
                // drag cursor only when mouse is near by
                switch(draggedCursor) {
                case 1:
                    // cursor A is dragged
                    index = xToIndex(mouseDragX);
                    capturedData.setCursorPositionA(index);
                    // notify cursor change listeners
                    if(index > 0 && index < (capturedData.absoluteLength - 1)) {
                        for(int i=0;i<curListners.size();i++) {
                            curListners.get(i).onCursorChanged(mouseDragX);
                        }
                    }
                    break;
                case 2:
                    // cursor B is dragged
                    index = xToIndex(mouseDragX);
                    capturedData.setCursorPositionB(index);
                    // notify cursor change listeners
                    if(index > 0 && index < (capturedData.absoluteLength - 1)) {
                        for(int i=0;i<curListners.size();i++) {
                            curListners.get(i).onCursorChanged(mouseDragX);
                        }
                    }
                    break;
                default:
                    break;
                }
                this.repaint();
            } else {
                // not dragged, just check if the cursor is near by a trigger
                if(Math.abs(xToIndex(mouseX) - (capturedData.getCursorPositionA())) < (5 / scale)) {
                    this.setCursor(cursorDrag);
                    this.draggedCursor = 1;
                } else if(Math.abs(xToIndex(mouseX) - (capturedData.getCursorPositionB())) < (5 / scale)) {
                    this.setCursor(cursorDrag);
                    this.draggedCursor = 2;
                } else {
                    this.setCursor(cursorDefault);
                    this.draggedCursor = 0;
                }
            }
        }
    }
	
    /**
     * Handles mouse dragged events and produces status change "events" accordingly.
     */
    public void mouseDragged(MouseEvent event) {
        mouseDragX = event.getX();
        updateCursors(true);
        updateStatus(true);
    }

    /**
     * Handles mouse moved events and produces status change "events" accordingly.
     */
    public void mouseMoved(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
        updateCursors(false);
        updateStatus(false);
    }
	
    /**
     * Handles mouse button events for context menu
     */
    public void mousePressed(MouseEvent event) {
        contextMenuPosition.x = event.getX();
        contextMenuPosition.y = event.getY();
        if((event.getButton() == MouseEvent.BUTTON3) && (this.capturedData != null) && (this.getCursorMode())) {
            contextMenu.show(event.getComponent(), contextMenuPosition.x, contextMenuPosition.y);
        }
    }
	
    public void mouseReleased(MouseEvent event) {
    }
	
    public void mouseClicked(MouseEvent event) {
    }
	
    public void mouseEntered(MouseEvent event) {
    }

    public void mouseExited(MouseEvent event) {
    }
	
    /**
     * Handle context menu mouse clicks
     */
    public void actionPerformed(ActionEvent event) {
        if(event.getActionCommand().equals("Set Cursor A")) {
            capturedData.setCursorPositionA(xToIndex(contextMenuPosition.x));
        } else if(event.getActionCommand().equals("Set Cursor B")) {
            capturedData.setCursorPositionB(xToIndex(contextMenuPosition.x));
        }
        this.repaint();
    }
	
    /**
     * Adds a status change listener for this diagram,
     * Simple implementation that will only call the last added listener on status change.
     */
    public void addStatusChangeListener(StatusChangeListener listener) {
        statusChangeListener = listener;
    }

    public void readProperties(Properties properties) {
        settings.readProperties(properties);
        labels.readProperties(properties);
        resize();
    }

    public void writeProperties(Properties properties) {
        settings.writeProperties(properties);
        labels.writeProperties(properties);
    }
}
