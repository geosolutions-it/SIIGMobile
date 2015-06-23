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
package it.geosolutions.android.map.overlay;

import it.geosolutions.android.map.style.AdvancedStyle;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.vividsolutions.jts.android.PointTransformation;
import com.vividsolutions.jts.android.ShapeWriter;
import com.vividsolutions.jts.android.geom.DrawableShape;
import com.vividsolutions.jts.geom.Geometry;

import eu.geopaparazzi.spatialite.database.spatial.core.GeometryIterator;
import it.geosolutions.android.map.style.StyleManager;
import it.geosolutions.android.map.style.Symbolizer;

/**
 * A class used to draw various shapes on a map
 * @author Jacopo Pianigiani (jacopo.pianigiani85@gmail.com)
 */
public class Shapes {
	
	private PointTransformation pointTransformer;
	private Canvas canvas;
	private AdvancedStyle style4Table;
	private GeometryIterator geometryIterator;
	
	/**
	 * Constructor for class shapes
	 * @param pointTransformer
	 * @param canvas
	 * @param style4Table
	 * @param geometryIterator
	 */
	public Shapes(PointTransformation pointTransformer, Canvas canvas, AdvancedStyle style4Table, GeometryIterator geometryIterator){
		this.pointTransformer = pointTransformer;
		this.canvas = canvas;
		this.style4Table = style4Table;
		this.geometryIterator = geometryIterator;

	}
	
	/**
	 * Method used to draw a line on map.
     * @param advancedStyle
     */
	public void drawLines(AdvancedStyle advancedStyle){
        Paint stroke = StyleManager.getStrokePaint4Style(advancedStyle);
        Paint textStroke = StyleManager.getTextStrokePaint4Style(advancedStyle);
        ShapeWriter wr = new ShapeWriter(pointTransformer);
        wr.setRemoveDuplicatePoints(true);
        wr.setDecimation(style4Table.decimationFactor);
        if(stroke == null && textStroke == null){
            // Can't draw
            return;
        }

        while( geometryIterator.hasNext() ) {
            Geometry geom = geometryIterator.next();
            DrawableShape shape = wr.toShape(geom);
            if (stroke != null){
                shape.draw(canvas, stroke);
            }

            if(textStroke != null){
                //canvas.drawTextOnPath();
            }
        }
	}
	
	/**
	 * Method used to draw a point on map.
	 * @param fill
	 * @param stroke
	 */
	public void drawPoints(Paint fill, Paint stroke){
		ShapeWriter wr = new ShapeWriter(pointTransformer,style4Table.shape,style4Table.size);
        wr.setRemoveDuplicatePoints(true);
        wr.setDecimation(style4Table.decimationFactor);
        while( geometryIterator.hasNext() ) {
            Geometry geom = geometryIterator.next();
            geom.getCoordinate();
            DrawableShape shape = wr.toShape(geom);
            
            if (fill != null){
                shape.fill(canvas, fill);
            }
            if (stroke != null){
                shape.draw(canvas, stroke);
            }
        }
	}
	
	/**
	 * method used to draw a polygon on map
	 * @param advancedStyle
	 */
	public void drawPolygons(AdvancedStyle advancedStyle){
        Paint stroke = StyleManager.getStrokePaint4Style(advancedStyle);
        Paint fill = StyleManager.getFillPaint4Style(advancedStyle);


        if(stroke == null && fill==null){
            // Can't draw
            return;
        }

        ShapeWriter wr = new ShapeWriter(pointTransformer);
        wr.setRemoveDuplicatePoints(true);
        wr.setDecimation(style4Table.decimationFactor);
		while( geometryIterator.hasNext() ) {
			
            Geometry geom = geometryIterator.next();
            if (geom != null) {
                DrawableShape shape = wr.toShape(geom);
                if (fill != null){
                    shape.fill(canvas, fill);
                }
                if (stroke != null){
                    shape.draw(canvas, stroke);
                }

            }
		}
	}

    public void drawLines(Symbolizer symbolizer) {
        AdvancedStyle astyle = new AdvancedStyle();
        astyle.dashed = symbolizer.dashed;
        astyle.decimationFactor = symbolizer.decimationFactor;
        astyle.fillalpha = symbolizer.fillalpha;
        astyle.fillcolor = symbolizer.fillcolor;
        astyle.shape = symbolizer.shape;
        astyle.strokealpha = symbolizer.strokealpha;
        astyle.strokecolor = symbolizer.strokecolor;
        astyle.size = symbolizer.size;
        astyle.textfield = symbolizer.textfield;
        astyle.textsize = symbolizer.textsize;
        astyle.textfillcolor = symbolizer.textfillcolor;
        astyle.textstrokecolor = symbolizer.textstrokecolor;
        astyle.width = symbolizer.width;
        this.drawLines(astyle);
    }


    public void drawPolygons(Symbolizer symbolizer) {
        AdvancedStyle astyle = new AdvancedStyle();
        astyle.dashed = symbolizer.dashed;
        astyle.decimationFactor = symbolizer.decimationFactor;
        astyle.fillalpha = symbolizer.fillalpha;
        astyle.fillcolor = symbolizer.fillcolor;
        astyle.shape = symbolizer.shape;
        astyle.strokealpha = symbolizer.strokealpha;
        astyle.strokecolor = symbolizer.strokecolor;
        astyle.size = symbolizer.size;
        astyle.textfield = symbolizer.textfield;
        astyle.textsize = symbolizer.textsize;
        astyle.width = symbolizer.width;
        this.drawPolygons(astyle);
    }
}