/*
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

import java.awt.event.ActionEvent;

import java.util.Map;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;

/**
 * @see MainWindow
 * @version 0.8
 * @author John Pritchard
 */
public enum Label {

    Open("Open"),
    SaveAs("Save as"),
    OpenProject("Open Project"),
    SaveProjectAs("Save Project as"),
    Capture("Capture"),
    RepeatCapture("Repeat Capture"),
    Exit("Exit"),
    ZoomIn("Zoom In"),
    ZoomOut("Zoom Out"),
    DefaultZoom("Default Zoom"),
    ZoomFit("Zoom Fit"),
    GotoTrigger("Goto Trigger"),
    GotoA("Goto A"),
    GotoB("Goto B"),
    DiagramSettings("Diagram Settings"),
    Labels("Labels"),
    Cursors("Cursors"),
    About("About"),
    ComboBoxChanged("Combo box changed"),
    Controller("Controller"),
    UNKNOWN("UNKNOWN");


    private final static Map<String,Label> LabelMap = new HashMap();
    static {
        for (Label label: Label.values()){
            LabelMap.put(label.label,label);
        }
    }
    public final static Label For(String name){
        Label label = LabelMap.get(name);
        if (null != label)
            return label;
        else
            return Label.UNKNOWN;
    }
    public final static String For(ActionEvent evt){
        String label = evt.getActionCommand();
        if (null == Label.For(label)){
            Object source = evt.getSource();
            if (source instanceof AbstractButton){
                return ((ImageIcon)((AbstractButton)source).getIcon()).getDescription();
            }
        }
        return label;
    }


    public final String label;

    private Label(String label){
        this.label = label;
    }


}
