package it.geosolutions.android.map.style.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import it.geosolutions.android.map.style.Filter;
import it.geosolutions.android.map.style.Rule;
import it.geosolutions.android.map.style.Symbolizer;

/**
 * Created by Lorenzo on 18/06/2015.
 */
public class RuleDeserializer implements JsonDeserializer {

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();

        final Rule rule = new Rule();
        rule.setFilter(context.<Filter>deserialize(jsonObject.get("filter"), Filter.class));
        rule.setSymbolizer(context.<Symbolizer>deserialize(jsonObject.get("symbolizer"), Symbolizer.class));
        return rule;
    }
}
