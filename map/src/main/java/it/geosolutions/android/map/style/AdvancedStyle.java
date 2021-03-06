/*
 * GeoSolutions Android Map Library - Digital field mapping on Android based devices
 * Copyright (C) 2013  GeoSolutions (www.geo-solutions.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.android.map.style;

import java.io.Serializable;

import eu.geopaparazzi.spatialite.database.spatial.core.Style;

/**
 * Extends style with Zoom levels 
 * @author Lorenzo Natali
 */
public class AdvancedStyle extends Style implements Serializable{
	public byte minZoom = 0;
    public byte maxZoom = 30;//TODO do it better
    public boolean dashed = false;
    public Rule[] rules = null;
    public String textstrokecolor = "";
    public String textfillcolor = "";
    public int label_minZoom = 14;
}
