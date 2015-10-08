package org.envirocar.core;

/*
 * enviroCar 2013
 * Copyright (C) 2013
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 *
 */


import java.io.File;

/**
 * this interface can be used to provide a cache directory.
 *
 * @author matthes rieke
 */
public interface CacheDirectoryProvider {
    /**
     * @return the base directory for storing cache files
     */
    File getBaseFolder();
}
