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

import java.io.File;

import javax.swing.SwingUtilities;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * Loader for the Logic Analyzer Client.
 * <p>
 * Processes command arguments and starts the UI. After the UI is closed it terminates the VM.
 * <p>
 * See description for {@link Loader#main(String[])} for details on supported arguments.
 * 
 * @version 0.8
 * @author Michael "Mr. Sump" Poppitz
 * @author Benjamin Vedder
 * @author John Pritchard
 */
public class Loader {

    /**
     * Starts up the logic analyzer client.  Project ("*.slp") and
     * data ("*.sla") files can be supplied as arguments.  The files
     * will then be loaded. If a file cannot be read, the client will
     * exit.
     * 
     * @param args arguments
     */
    public static void main(String[] args) {

        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (UnsupportedLookAndFeelException e) {
            // handle exception
        } catch (ClassNotFoundException e) {
            // handle exception
        } catch (InstantiationException e) {
            // handle exception
        } catch (IllegalAccessException e) {
            // handle exception
        }


        MainWindow w = new MainWindow();

        for (int argx = 0; argx < args.length; argx++) {
            String arg = args[argx];

            if (arg.startsWith("-")) {

                System.out.println("Usage: run [file.slp] [file.sla]");
                System.exit(1);
            }
            else {
                try {
                    final File f = new File(arg);

                    if (!f.isFile()) {

                        System.out.printf("Error, file not found '%s'%n",arg);

                        System.exit(1);
                    }
                    else {
                        final String fext = Fext(f);
                        if (fext.equals("slp")) {

                            w.loadProject(f);
                        }
                        else if (fext.equals("sla")) {

                            w.loadData(f);
                        }
                        else {
                            System.out.printf("Error, unrecognized file name extension '%s' in '%s'%n",fext,arg);
                            System.exit(1);
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
		
        try {
            SwingUtilities.invokeLater(w);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private final static String Fext(File f){
        String name = f.getName();
        int idx = name.lastIndexOf('.');
        if (0 < idx)
            return name.substring(idx+1).toLowerCase();
        else
            return "";
    }
}
