package it.geosolutions.android.map.wms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Robert Oehler on 02.09.15.
 *
 * class containing the configuration to displays keys
 * GetFeatureInfo request in different languages
 *
 * It is structure in a list of locales
 * which contains a list of layers for which
 * a map of properties is defined
 *
 * locales : [
 *           "en",
 *           layers :
 *           [
 *              layer :
 *              properties :
 *              [
 *                key : value,
 *                ...
 *              ]
 *           ],
 *           ....
 *         ],
 *         ....
 *
 *
 */
public class GetFeatureInfoConfiguration implements Serializable {

    private final static String DEFAULT_LANGUAGE = "en";

    private List<Locale> locales = new ArrayList<>();

    public List<Locale> getLocales() {
        return locales;
    }

    public void setLocales(List<Locale> locales) {
        this.locales = locales;
    }

    /**
     * returns the locale for the @param language code if available
     * otherwise the default is returned
     * @param languageCode the language code of the locale
     * @return the configuration for this locale
     */
    public GetFeatureInfoConfiguration.Locale getLocaleForLanguageCode(final String languageCode){

        for(Locale locale : locales){
            if(languageCode.equals(locale.locale)){
                return locale;
            }
        }
        return getDefaultLocale();
    }

    /**
     * returnes the default locale if available, null otherwise
     * @return the default locale configuration
     */
    public GetFeatureInfoConfiguration.Locale getDefaultLocale(){

        for(Locale locale : locales){
            if(DEFAULT_LANGUAGE.equals(locale.locale)){
                return  locale;
            }
        }
        return null;
    }


    public static class Locale implements Serializable {

        private String locale;
        private List<Layer> layers = new ArrayList<>();

        /**
         * returnes the properties for a layer defined by @param _layer
         * @param _layer the layer to get the props for
         * @return the map of props or null if this layer is not available or has no props
         */
        public Map<String,String> getPropertiesForLayer(final String _layer){
            for(Layer layer : layers){
                if(layer.getName().equals(_layer)){
                    return layer.getProperties();
                }
            }
            return null;
        }

        /**
         *
         * @return
         * The locale
         */
        public String getLocale() {
            return locale;
        }

        /**
         *
         * @param locale
         * The locale
         */
        public void setLocale(String locale) {
            this.locale = locale;
        }

        /**
         *
         * @return
         * The layers
         */
        public List<Layer> getLayers() {
            return layers;
        }

        /**
         *
         * @param layers
         * The layers
         */
        public void setLayers(List<Layer> layers) {
            this.layers = layers;
        }



    }

    public static class Layer implements Serializable {

        private String name;
        private Map<String,String> properties = new HashMap<>();

        /**
         *
         * @return
         * The name
         */
        public String getName() {
            return name;
        }

        /**
         *
         * @param name
         * The name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         *
         * @return
         * The properties
         */
        public  Map<String,String> getProperties() {
            return properties;
        }

        /**
         *
         * @param properties
         * The properties
         */
        public void setProperties( Map<String,String> properties) {
            this.properties = properties;
        }


    }

}
