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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.sump.analyzer.devices.DeviceController;
import org.sump.analyzer.devices.FpgaDeviceController;
import org.sump.analyzer.tools.Tool;
import org.sump.util.ClassPath;

/**
 * Main frame and starter for Logic Analyzer Client.
 * <p>
 * This class only provides a simple end-user frontend and no functionality to be used by other code.
 * 
 * @version 0.8
 * @author Michael "Mr. Sump" Poppitz
 * @author Benjamin Vedder
 * @author John Pritchard
 */
public final class MainWindow
    extends JFrame
    implements Runnable, 
               ActionListener, 
               WindowListener, 
               StatusChangeListener, 
               DiagramCursorChangeListener, 
               MouseWheelListener
{

	
    /**
     * Inner class defining a File Filter for SLA files.
     * 
     * @author Michael "Mr. Sump" Poppitz
     *
     */
    private class SLAFilter extends FileFilter {
        public boolean accept(File f) {
            return (f.isDirectory() || f.getName().toLowerCase().endsWith(FILE_EXTENSION));
        }
        public String getDescription() {
            return ("Sump's Logic Analyzer Files (*" + FILE_EXTENSION + ")");
        }
		
        public static final String FILE_EXTENSION = ".sla";
    }

    /**
     * Inner class defining a File Filter for SLP files.
     * 
     * @author Michael "Mr. Sump" Poppitz
     *
     */
    private class SLPFilter extends FileFilter {
        public boolean accept(File f) {
            return (f.isDirectory() || f.getName().toLowerCase().endsWith(FILE_EXTENSION));
        }
        public String getDescription() {
            return ("Sump's Logic Analyzer Project Files (*" + FILE_EXTENSION + ")");
        }

        public static final String FILE_EXTENSION = ".slp";
    }

	
    private static final String APP_NAME = "Logic Analyzer Client";


    private JMenu toolMenu;
    private JMenu diagramMenu;
	
    private JFileChooser fileChooser;
    private JFileChooser projectChooser;

    private int currentController;

    private JScrollPane diagramPane;

    private JLabel status;

    private JCheckBoxMenuItem cursorsEnabledMenuItem;
    private JComboBox currentDisplayPage;
    private JLabel maxDisplayPage;


    private final Project project;
    private final ClassPath classpath;
    private final Diagram diagram;
    private final Tool[] tools;
    private final DeviceController[] controllers;

    /**
     * Default constructor.
     *
     */
    public MainWindow() {
        super(APP_NAME);

        this.setIconImage((new ImageIcon("org/sump/analyzer/icons/la.png")).getImage());
        this.addWindowListener(this);

        this.classpath = new ClassPath();
        this.project = new Project(this.classpath);
        this.controllers = this.classpath.controllers();
        this.tools = this.classpath.tools(this);

        this.diagram = this.classpath.getDiagram();
        this.diagram.addStatusChangeListener(this);
    }


    /**
     * Enables or disables functions that can only operate when captured data has been added to the diagram.
     * @param enable set <code>true</code> to enable these functions, <code>false</code> to disable them
     */
    private void enableDataDependingFunctions(boolean enable) {
        diagramMenu.setEnabled(enable);
        toolMenu.setEnabled(enable);
    }	
    /**
     *
     */	
    private void updatePageInfo() {
        maxDisplayPage.setText("" + diagram.getMaxPages());
        currentDisplayPage.removeAllItems();
        for(int i=0;i<diagram.getMaxPages();i++) {
            currentDisplayPage.addItem(i+1);
        }
    }
	
    /**
     * Handles all user interaction.
     */
    public void actionPerformed(ActionEvent event) {

        String label = Label.For(event);

        try {

            switch (Label.For(label)){
            case Open:

                if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if (file.isFile()) {
                        loadData(file);
                        this.setTitle(APP_NAME + " - " + file.getName());
                    }
                    cursorsEnabledMenuItem.setSelected(diagram.getCursorMode());
                    Container contentPane = this.getContentPane();
                    diagram.zoomFit((contentPane.getSize().width * 95) / 100);
                    updatePageInfo();
                }
                return;			
            case SaveAs:

                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if(!file.getName().endsWith(SLAFilter.FILE_EXTENSION)) {
                        file = new File(file.getAbsolutePath() + SLAFilter.FILE_EXTENSION);
                    }
                    boolean writefile = true;
                    if(file.exists() && (JOptionPane.showConfirmDialog(this,
                                                                       "The file " + file.getName() + " already exists. Overwrite it?",
                                                                       "Overwrite File",
                                                                       JOptionPane.YES_NO_OPTION,
                                                                       JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION)) {
                        writefile = false;
                    }
                    if (writefile) {
                        diagram.getCapturedData().writeToFile(file);
                        this.setTitle(APP_NAME + " - " + file.getName());
                    }
                }
                return;

            case OpenProject:

                if (projectChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File file = projectChooser.getSelectedFile();
                    if (file.isFile())
                        loadProject(file);
                }
                return;
		
            case SaveProjectAs:		

                if (projectChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File file = projectChooser.getSelectedFile();
                    if(!file.getName().endsWith(SLPFilter.FILE_EXTENSION)) {
                        file = new File(file.getAbsolutePath() + SLPFilter.FILE_EXTENSION);
                    }
                    boolean writefile = true;
                    if(file.exists() && (JOptionPane.showConfirmDialog(this,
                                                                       "The file " + file.getName() + " already exists. Overwrite it?",
                                                                       "Overwrite File",
                                                                       JOptionPane.YES_NO_OPTION,
                                                                       JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION)) {
                        writefile = false;
                    }
                    if (writefile) {
                        project.store(file);
                    }
                }
                return;

            case Capture:
			

                if (currentController < 0)
                    return;
                else if (controllers[currentController].showCaptureDialog(this) == DeviceController.DONE) {
                    diagram.setCapturedData(controllers[currentController].getDeviceData(this));
                    Container contentPane = this.getContentPane();
                    //diagram.zoomFit((contentPane.getSize().width * 95) / 100);
                    diagram.zoomFit(diagramPane.getViewport().getViewRect().width);
                    cursorsEnabledMenuItem.setSelected(diagram.getCursorMode());
                }
                return;

            case RepeatCapture:

                if (currentController < 0)
                    return;
                else if (controllers[currentController].showCaptureProgress(this) == DeviceController.DONE) {
                    diagram.setCapturedData(controllers[currentController].getDeviceData(this));
                    Container contentPane = this.getContentPane();
                    //diagram.zoomFit((contentPane.getSize().width * 95) / 100);
                    diagram.zoomFit(diagramPane.getViewport().getViewRect().width);
                    cursorsEnabledMenuItem.setSelected(diagram.getCursorMode());
                }
                return;

            case Exit:
                System.exit(0);
                return;
			
            case ZoomIn:
                {
                    JViewport vp = diagramPane.getViewport();
                    Point p1 = vp.getViewPosition();
                    int cdiff = vp.getViewRect().width / 2;
                    p1.x += (cdiff);
                    double x = (double)p1.x / (double)vp.getViewSize().width;
				
                    diagram.zoomIn();
                    updatePageInfo();
				
                    Point p2 = new Point((int)(x * (double)vp.getViewSize().width), 0);
                    if (p2.x > cdiff) {
                        p2.x -= cdiff;
                    }
				
                    vp.setViewPosition(p2);
                }
                return;
			
            case ZoomOut:
                {
                    JViewport vp = diagramPane.getViewport();
                    Point p1 = vp.getViewPosition();
                    int cdiff = vp.getViewRect().width / 2;
                    p1.x += (cdiff);
                    double x = (double)p1.x / (double)vp.getViewSize().width;
				
                    diagram.zoomOut();
				
                    if (vp.getViewSize().width < vp.getViewRect().width) {
                        diagram.zoomFit(vp.getViewRect().width);
                    }
				
                    updatePageInfo();
				
                    Point p2 = new Point((int)(x * (double)vp.getViewSize().width), 0);
                    if (p2.x > cdiff) {
                        p2.x -= cdiff;
                    }
				
                    vp.setViewPosition(p2);
                }
                return;

            case DefaultZoom:
                diagram.zoomDefault();
                updatePageInfo();
                return;

            case ZoomFit:
                Container contentPane = this.getContentPane();
                //diagram.zoomFit((contentPane.getSize().width * 95) / 100);
                diagram.zoomFit(diagramPane.getViewport().getViewRect().width);
                updatePageInfo();
                return;
				
            case GotoTrigger:

                // do this only with data and trigger available 
                if(diagram.hasCapturedData() && diagram.getCapturedData().hasTriggerData()) {
                    gotoPosition(diagram.getCapturedData().triggerPosition);
                }
                return;
				
            case GotoA:
                if(diagram.getCursorMode()) {
                    gotoPosition(diagram.getCapturedData().getCursorPositionA());
                }
                return;

            case GotoB:
                if(diagram.getCursorMode()) {
                    gotoPosition(diagram.getCapturedData().getCursorPositionB());
                }
                return;

            case DiagramSettings:
                diagram.showSettingsDialog(this);
                return;
				
            case Labels:
                diagram.showLabelsDialog(this);
                return;

            case Cursors:
                diagram.setCursorMode(((JCheckBoxMenuItem)event.getSource()).getState());
                return;

            case About:
                JOptionPane.showMessageDialog(null,
                                              "Sump's Logic Analyzer Client\n"
                                              + "\n"
                                              + "Copyright 2006 Michael Poppitz\n"
                                              + "Copyright 2012 John Pritchard\n"
                                              + "\n"
                                              + "This software is released under the GNU GPL.\n"
                                              + "\n"
                                              + "Version: 0.8\n"
                                              + "\n"
                                              + "Modified 2012-05-07\n"
                                              + "by John Pritchard (jdp@syntelos.org)\n"
                                              + "\n"
                                              + "For more information see:\n"
                                              + "http://www.sump.org/projects/analyzer/",
                                              "About", JOptionPane.INFORMATION_MESSAGE
                                              );
                return;
            case ComboBoxChanged:
                {
                    Object current = currentDisplayPage.getSelectedItem();
                    if(current != null) {
                        Integer page = (Integer)current;
                        page--;
                        diagramPane.getViewport().setViewPosition(new Point(0,0));
                        diagram.setCurrentPage(page.intValue());
                    }
                }
                return;
            case Controller:
                {
                    String[] possibleControllers = new String[controllers.length];
                    for(int i=0;i<controllers.length;i++)
                        possibleControllers[i] = new String(controllers[i].getControllerName());
                    Object selectedController = JOptionPane.showInputDialog(
                                                                            null,
                                                                            "Select a Device Controller", 
                                                                            "Device Controller",
                                                                            JOptionPane.INFORMATION_MESSAGE, 
                                                                            null,
                                                                            possibleControllers,
                                                                            possibleControllers[currentController]);
                    for(int i=0;i<controllers.length;i++) {
                        if(controllers[i].getControllerName().equals(selectedController)) {
                            currentController = i;
                        }
                    }
                }
                return;
            default:
                break;
            }


            /*
             * Check if a tool has been selected and if so, process
             * captured data by tool
             */
            for (int i = 0; i < tools.length; i++){
                if (label.equals(tools[i].getName())) {
                    CapturedData newData = tools[i].process(diagram.getCapturedData());
                    if (newData != null)
                        diagram.setCapturedData(newData);
                }
            }

            enableDataDependingFunctions(diagram.hasCapturedData());
				
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
    }
	
    /**
     * Handles mouse wheel events
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
		
        int notches = e.getWheelRotation();
        JViewport vp = diagramPane.getViewport();
		
        if (e.isControlDown()) {
			
            if (notches < 0) {
                /*
                 * Zoom in
                 */
				
                Point p1 = vp.getViewPosition();
                p1.x += e.getX();
                double x = (double)p1.x / (double)vp.getViewSize().width;
				
                diagram.zoomIn();
                updatePageInfo();
				
                Point p2 = new Point((int)(x * (double)vp.getViewSize().width), 0);
                if (p2.x > e.getX()) {
                    p2.x -= e.getX();
                }
				
                vp.setViewPosition(p2);
				
            } else {
                /*
                 * Zoom out
                 */
				
                Point p1 = vp.getViewPosition();
                p1.x += e.getX();
                double x = (double)p1.x / (double)vp.getViewSize().width;
				
                diagram.zoomOut();
                updatePageInfo();
				
                if (vp.getViewSize().width < vp.getViewRect().width) {
                    diagram.zoomFit(vp.getViewRect().width);
                }
				
                Point p2 = new Point((int)(x * (double)vp.getViewSize().width), 0);
                if (p2.x > e.getX()) {
                    p2.x -= e.getX();
                }
				
                vp.setViewPosition(p2);
				
            }
			
        } else {
			
            if (notches < 0) {
				
                Point p = vp.getViewPosition();
                if (p.x > 30) {
                    p.x -= 30;
                } else {
                    p.x = 0;
                }
				
                vp.setViewPosition(p);
				
            } else {
				
                Point p = vp.getViewPosition();
                if ((p.x + 30) < (vp.getViewSize().width - vp.getViewRect().width)) {
                    p.x += 30;
                } else {
                    p.x = (vp.getViewSize().width - vp.getViewRect().width);
                }
				
                vp.setViewPosition(p);
				
            }
			
        }
		
    }
	
    public void onCursorChanged(int mousePos) {
        Point currentViewPos = diagramPane.getViewport().getViewPosition();
        int vpMin = currentViewPos.x;
        int vpMax = diagramPane.getViewport().getSize().width + currentViewPos.x;

        /*
         * The step size to left/right depends on the distance of the current mouse
         * position to the right/left viewport border. So when the mouse cursor is
         * far outside the diagram scrolls faster.
         */
        int stepSizeLeft = Math.abs(mousePos - vpMin);  
        int stepSizeRight = Math.abs(mousePos - vpMax);  
		
        if(mousePos < vpMin) {
            currentViewPos.x -= stepSizeLeft;
            diagramPane.getViewport().setViewPosition(currentViewPos);
        }
        if(mousePos > vpMax) {
            currentViewPos.x += stepSizeRight;
            diagramPane.getViewport().setViewPosition(currentViewPos);
        }
    }
	
    /**
     * set Diagramm viewport position
     * @param samplePos sample position
     */
    private void gotoPosition(long samplePos) {
        Dimension dim = diagram.getPreferredSize();
        JViewport vp = diagramPane.getViewport();
		
        // do nothing if the zoom factor is nearly the viewport size
        if(dim.width < vp.getWidth() * 2) return;
		
        currentDisplayPage.setSelectedIndex(diagram.getPage(samplePos));
        int pos = diagram.getTargetPosition(dim.width, samplePos) - 20;
        if(pos < 0) pos = 0;
		
        if(samplePos == 0) {
            vp.setViewPosition(new Point(0,0));
        } else {
            vp.setViewPosition(new Point(pos,0));
        }
    }

    /** 
     * Handles status change requests.
     */
    public void statusChanged(String s) {
        status.setText(s);
    }
    public void windowOpened(WindowEvent evt){
    }
    public void windowClosing(WindowEvent evt) {

        System.exit(0);
    }
    public void windowClosed(WindowEvent evt){
    }
    public void windowIconified(WindowEvent evt){
    }
    public void windowDeiconified(WindowEvent evt){
    }
    public void windowActivated(WindowEvent evt){
    }
    public void windowDeactivated(WindowEvent evt){
    }
    /**
     * Load the given file as data.
     * @param file file to be loaded as data
     * @throws IOException when an IO error occurs
     */
    public void loadData(File file) throws IOException {

        diagram.setCapturedData(new CapturedData(file));
    }
	
    /**
     * Load the given file as project.
     * @param file file to be loaded as projects
     * @throws IOException when an IO error occurs
     */
    public void loadProject(File file) throws IOException {

        project.load(file);
    }
	
    /**
     * Starts GUI creation and displays it.
     * Must be called be Swing event dispatcher thread.
     */
    public void run() {

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        JMenuBar mb = new JMenuBar();
		
        /*
         * File menu
         */
        JMenu fileMenu = createMenu("File", (new Label[]{Label.Open, Label.SaveAs, Label.UNKNOWN, Label.Exit}));
        mb.add(fileMenu);
		
        /*
         * Project menu
         */
        JMenu projectMenu = createMenu("Project", (new Label[]{Label.OpenProject, Label.SaveProjectAs }));
        mb.add(projectMenu);

        /*		
         * Device menu
         */
        currentController = -1;

        if (this.controllers.length > 0) {

            currentController = 0;

            for (int i = 0; i < this.controllers.length; i++) {

                if (this.controllers[i] instanceof FpgaDeviceController){
                    currentController = i;
                }
            }
	
            /*
             * Device controller menu is only added when at least one controller is available
             */
            JMenu deviceMenu = null;
            if (controllers.length > 1) {
                /*
                 * When more than one controller then add a controller seclection to menue
                 */
                Label[] deviceEntries = {Label.Controller, Label.UNKNOWN, Label.Capture, Label.RepeatCapture};
                deviceMenu = createMenu("Device", deviceEntries);
            }
            else {				
                Label[] deviceEntries = {Label.Capture, Label.RepeatCapture};
                deviceMenu = createMenu("Device", deviceEntries);
            }
            mb.add(deviceMenu);
        }

        /*
         * Diagram menu
         */
        Label[] diagramEntries = {Label.ZoomIn, Label.ZoomOut, Label.DefaultZoom, Label.ZoomFit, Label.GotoTrigger, Label.GotoA, Label.GotoB, Label.UNKNOWN, Label.DiagramSettings, Label.Labels};
        diagramMenu = createMenu("Diagram", diagramEntries);
        cursorsEnabledMenuItem = new JCheckBoxMenuItem("Cursors");
        cursorsEnabledMenuItem.addActionListener(this);
        diagramMenu.add(cursorsEnabledMenuItem);
        mb.add(diagramMenu);

        /*
         * Tools menu
         */
        Label[] toolEntries = new Label[tools.length];
        for (int i = 0; i < tools.length; i++) {

            toolEntries[i] = Label.For(tools[i].getName());
        }
        toolMenu = createMenu("Tools", toolEntries);

        mb.add(toolMenu);
		

        /*
         * Help menu
         */
        JMenu helpMenu = createMenu("Help", (new Label[]{Label.About}));
        mb.add(helpMenu);

		
        this.setJMenuBar(mb);
		
        JToolBar tools = new JToolBar();
        tools.setRollover(true);
        tools.setFloatable(false);
		
        String[] fileToolsF = {"fileopen.png", "filesaveas.png"}; // , "fileclose.png"};
        Label[] fileToolsD = {Label.Open, Label.SaveAs}; // , Label.Close};
        createTools(tools, fileToolsF, fileToolsD);
        tools.addSeparator();

        String[] deviceToolsF = {"launch.png", "reload.png"};
        Label[] deviceToolsD = {Label.Capture, Label.RepeatCapture};
        createTools(tools, deviceToolsF, deviceToolsD);
        tools.addSeparator();

        String[] diagramToolsF = {"viewmag+.png", "viewmag-.png", "viewmag1.png", "viewmag_fit.png", "viewmag_cursor.png", "viewmag_a.png", "viewmag_b.png"};
        Label[] diagramToolsD = {Label.ZoomIn, Label.ZoomOut, Label.DefaultZoom, Label.ZoomFit, Label.GotoTrigger, Label.GotoA, Label.GotoB};
        createTools(tools, diagramToolsF, diagramToolsD);
        tools.addSeparator();

        tools.add(new JLabel("Page "));
        String[] pages = {
            "1"
        };
        currentDisplayPage = new JComboBox(pages);
        currentDisplayPage.addActionListener(this);

        currentDisplayPage.setMaximumSize(new Dimension(60,currentDisplayPage.getMaximumSize().height));
        tools.add(currentDisplayPage);
        tools.add(new JLabel(" of "));
        maxDisplayPage = new JLabel("1");
        tools.add(maxDisplayPage);
		
        contentPane.add(tools, BorderLayout.NORTH);
		
        status = new JLabel(" ");
        contentPane.add(status, BorderLayout.SOUTH);
		

        diagram.setPreferredSize(contentPane.getSize());
        diagramPane = new JScrollPane(this.diagram);
        diagramPane.setWheelScrollingEnabled(false);
		
        contentPane.add(diagramPane, BorderLayout.CENTER);

        enableDataDependingFunctions(false);

        this.setSize(1000, 700);
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter((FileFilter) new SLAFilter());

        projectChooser = new JFileChooser();
        projectChooser.addChoosableFileFilter((FileFilter) new SLPFilter());
		
        diagram.addCursorChangeListener(this);
        diagramPane.addMouseWheelListener(this);
    }

    /**
     * Creates a JMenu containing items as specified.
     * If an item name is empty, a separator will be added in its place.
     * 
     * @param name Menu name
     * @param entries array of menu item names.
     * @return created menu
     */
    private JMenu createMenu(String name, Label[] entries) {
        JMenu menu = new JMenu(name);
        if(entries != null) {
            for (int i = 0; i < entries.length; i++) {
                Label label = entries[i];
                if (Label.UNKNOWN == label)
                    menu.add(new JSeparator());
                else {
                    JMenuItem item = new JMenuItem(label.label);
                    item.addActionListener(this);
                    menu.add(item);
                }
            }
        }
        return menu;
    }
    /**
     * Creates tool icons and adds them the the given tool bar.
     * 
     * @param tools tool bar to add icons to
     * @param files array of icon file names
     * @param descriptions array of icon descriptions
     */
    private void createTools(JToolBar tools, String[] files, Label[] descriptions) {
		
        for (int i = 0; i < files.length; i++) {
            URL u = MainWindow.class.getResource("icons/" + files[i]);
            JButton b = new JButton(new ImageIcon(u, descriptions[i].label));
            b.setToolTipText(descriptions[i].label);
            b.setMargin(new Insets(0,0,0,0));
            b.addActionListener(this);
            tools.add(b);
        }
    }
}
