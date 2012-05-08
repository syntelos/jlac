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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sump.util.ClassPath;
import org.sump.util.Properties;

/**
 * Project maintains a global properties list for all registered objects implementing {@link Configurable}.
 * It also provides methods for loading and storing these properties from and to project configuration files.
 * This allows to keep multiple sets of user settings across multiple instance lifecycles.
 * 
 * @version 0.8
 * @author Michael "Mr. Sump" Poppitz
 * @author John Pritchard
 */
public class Project extends Object {

    private final Configurable[] configurables;


    public Project(ClassPath classpath) {
        super();
        this.configurables = classpath.configurables();
    }
	
    /**
     * Loads properties from the given file and notifies all registered configurable objects.
     * @param file file to read properties from
     * @throws IOException when IO operation failes
     */
    public void load(File file) throws IOException {
        final Properties properties = new Properties();
        {
            final InputStream in = new FileInputStream(file);
            try {
                properties.load(in);
            }
            finally {
                in.close();
            }
        }
        for (Configurable c : this.configurables){

            c.readProperties(properties);
        }
    }

    /**
     * Stores properties fetched from all registered configurable objects in the given file.
     * @param file file to store properties in
     * @throws IOException when IO operation failes
     */
    public void store(File file) throws IOException {
        final Properties properties = new Properties();
        {
            for (Configurable c : this.configurables){

                c.writeProperties(properties);
            }
        }
        {
            OutputStream out = new FileOutputStream(file);
            try {
                properties.store(out, "Sumps Logic Analyzer Project File");
            }
            finally {
                out.close();
            }
        }
    }

}
