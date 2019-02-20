package com.davidgalindo.rnarcgismapview;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import java.util.HashMap;
import java.util.Map;

public class RNAGSGraphicsOverlay {
    private GraphicsOverlay graphicsOverlay;
    private HashMap<String, String> pointImageDictionary;
    private String referenceId;

    public RNAGSGraphicsOverlay(ReadableMap rawData, GraphicsOverlay graphicsOverlay) {
        this.referenceId = rawData.getString("referenceId");
        ReadableArray pointImageDictionaryRaw = rawData.getArray("pointGraphics");
        pointImageDictionary = new HashMap<>();
        this.graphicsOverlay = graphicsOverlay;

        for (int i = 0; i < pointImageDictionaryRaw.size(); i++) {
            ReadableMap item = pointImageDictionaryRaw.getMap(i);
            if (item.hasKey("graphicId")) {
                String graphicId = item.getString("graphicId");
                String uri = item.getMap("graphic").getString("uri");
                pointImageDictionary.put(graphicId, uri);
            }
        }
        // Create graphics within overlay
        ReadableArray rawPoints = rawData.getArray("points");
        for (int i = 0; i < rawPoints.size(); i++) {
            addGraphicsLoop(rawPoints.getMap(i));

        }
    }

    // Getters
    public GraphicsOverlay getAGSGraphicsOverlay() {
        return graphicsOverlay;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void updateGraphics(ReadableArray args) {
        for (int i = 0; i < args.size(); i++) {
            updateGraphicLoop(args.getMap(i));
        }
    }

    private void updateGraphicLoop(ReadableMap args) {
        // Establish variables
        com.esri.arcgisruntime.geometry.Point agsPoint = null;
        // Get references
        String referenceId = args.getString("referenceId");
        Map<String, Object> attributes = null;

        // Once we have all the required values, we change them
        Graphic graphic = ArrayHelper.graphicViaReferenceId(graphicsOverlay, referenceId);
        if (graphic == null) {
            return;
        }
        if (args.hasKey("latitude") && args.hasKey("longitude")) {
            Double latitude = args.getDouble("latitude");
            Double longitude = args.getDouble("longitude");
            agsPoint = new com.esri.arcgisruntime.geometry.Point(longitude, latitude, SpatialReferences.getWgs84());
            graphic.setGeometry(agsPoint);
        }
        if (args.hasKey("attributes")) {
            attributes = RNAGSGraphicsOverlay.readableMapToMap(args.getMap("attributes"));
            graphic.getAttributes().putAll(attributes);

        }
        if (args.hasKey("graphicsId")) {
            String graphicsId = args.getString("graphicsId");
            String graphicUri = pointImageDictionary.get(graphicsId);
            if (graphicUri != null) {
                PictureMarkerSymbol symbol = new PictureMarkerSymbol(graphicUri);
                graphic.setSymbol(symbol);
            }
        }
        if (args.hasKey("rotation")) {
            Double rotation = args.getDouble("rotation");
            ((PictureMarkerSymbol) graphic.getSymbol()).setAngle(rotation.floatValue());
        }
        // End of updates

    }

    public void addGraphics(ReadableArray args) {
        for (int i = 0; i < args.size(); i++) {
            addGraphicsLoop(args.getMap(i));
        }
    }

    private void addGraphicsLoop(ReadableMap map) {
        Point point = Point.fromRawData(map);
        Graphic graphic = RNAGSGraphicsOverlay.rnPointToAGSGraphic(point, pointImageDictionary);
        graphicsOverlay.getGraphics().add(graphic);
    }

    public void removeGraphics(ReadableArray args) {
        for (int i = 0; i < args.size(); i++) {
            removeGraphicsLoop(args.getString(i));
        }
    }

    private void removeGraphicsLoop(String referenceId) {
        // Identify the graphic and remove it
        Graphic graphic = ArrayHelper.graphicViaReferenceId(graphicsOverlay, referenceId);
        if (graphic != null) {
            graphicsOverlay.getGraphics().remove(graphic);
        }
    }

    // MARK: Static methods
    public static Graphic rnPointToAGSGraphic(Point point, Map<String, String> pointImageDictionary) {
        com.esri.arcgisruntime.geometry.Point agsPoint = new com.esri.arcgisruntime.geometry.Point(point.getLongitude(), point.getLatitude(), SpatialReferences.getWgs84());
        Graphic result;
        if (point.getGraphicId() != null && pointImageDictionary.get(point.getGraphicId()) != null) {
            String imageUri = pointImageDictionary.get(point.getGraphicId());
            assert imageUri != null;
            PictureMarkerSymbol symbol = new PictureMarkerSymbol(imageUri);
            if (point.getAttributes() != null) {
                result = new Graphic(agsPoint, point.getAttributes(), symbol);
            } else {
                result = new Graphic(agsPoint, symbol);
            }
        } else {
            SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 10);
            if (point.getAttributes() != null) {
                result = new Graphic(agsPoint, point.getAttributes(), symbol);
            } else {
                result = new Graphic(agsPoint, symbol);
            }
        }
        result.getAttributes().put("referenceId", point.getReferenceId());
        return result;

    }

    private static Map<String, Object> readableMapToMap(ReadableMap rawMap) {
        Map<String, Object> map = new HashMap<>();
        ReadableMapKeySetIterator iterator = rawMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            map.put(key, rawMap.getString(key));
        }
        return map;
    }

    // MARK: Inner class
    public static class Point {
        private Double latitude;
        private Double longitude;
        private Double rotation;
        private String referenceId;
        private Map<String, Object> attributes;
        private String graphicId;

        public static Point fromRawData(ReadableMap rawData) {
            // Convert map to attribute map
            Map<String, Object> map = null;
            if (rawData.hasKey("attributes")) {
                ReadableMap rawMap = rawData.getMap("attributes");
                map = RNAGSGraphicsOverlay.readableMapToMap(rawMap);
            }
            Double rotation = 0.0;
            if (rawData.hasKey("rotation")) {
                rotation = rawData.getDouble("rotation");
            }
            String graphicId = "";
            if (rawData.hasKey("graphicId")) {
                graphicId = rawData.getString("graphicId");
            }
            return new Point(
                    rawData.getDouble("latitude"),
                    rawData.getDouble("longitude"),
                    rotation,
                    rawData.getString("referenceId"),
                    map,
                    graphicId
            );
        }

        private Point(@NonNull Double latitude, @NonNull Double longitude, @NonNull Double rotation, @NonNull String referenceId,
                      @Nullable Map<String, Object> attributes, @Nullable String graphicId) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.rotation = rotation;
            this.referenceId = referenceId;
            this.attributes = attributes;
            this.graphicId = graphicId;
        }

        // MARK: Get/Set
        public void setRotation(Double rotation) {
            this.rotation = rotation;
        }

        public void setReferenceId(String referenceId) {
            this.referenceId = referenceId;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public void setGraphicId(String graphicId) {
            this.graphicId = graphicId;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public String getGraphicId() {
            return graphicId;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public Double getRotation() {
            return rotation;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getLatitude() {
            return latitude;
        }
    }
}
