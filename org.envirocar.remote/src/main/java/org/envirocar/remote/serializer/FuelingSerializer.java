package org.envirocar.remote.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.entity.FuelingImpl;
import org.envirocar.core.util.Util;

import java.lang.reflect.Type;
import java.text.ParseException;

/**
 * @author dewall
 */
public class FuelingSerializer implements JsonSerializer<Fueling>, JsonDeserializer<Fueling> {

    @Override
    public JsonElement serialize(Fueling src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.addProperty(Fueling.KEY_CAR, src.getCar().getId());
        result.addProperty(Fueling.KEY_FUEL_TYPE, src.getCar().getFuelType().toString());
        result.addProperty(Fueling.KEY_TIME, Util.longToIsoDate(src.getTime()));

        result.add(Fueling.KEY_COST, createFuelingProperty(src.getCost(),
                src.getCostUnit().toString()));
        result.add(Fueling.KEY_MILEAGE, createFuelingProperty(src.getMilage(),
                src.getMilageUnit().toString()));
        result.add(Fueling.KEY_VOLUME, createFuelingProperty(src.getVolume(),
                src.getVolumeUnit().toString()));

        result.addProperty(Fueling.KEY_MISSED_FUEL_STOP, src.isMissedFuelStop());
        return result;
    }

    private JsonObject createFuelingProperty(double value, String unit){
        JsonObject result = new JsonObject();
        result.addProperty(Fueling.KEY_VALUE, value);
        result.addProperty(Fueling.KEY_UNIT, unit);
        return result;
    }

    @Override
    public Fueling deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = (JsonObject) json;

        Fueling result = new FuelingImpl();
        try {
            result.setTime(Util.isoDateToLong(jsonObject.get(Fueling.KEY_TIME).getAsString()));
        } catch (ParseException e) {
            throw new JsonParseException(e);
        }
        result.setCar(context.deserialize(jsonObject.get(Fueling.KEY_CAR), Car.class));
        result.setMissedFuelStop(jsonObject.get(Fueling.KEY_MISSED_FUEL_STOP).getAsBoolean());

        JsonObject milageObject = jsonObject.get(Fueling.KEY_MILEAGE).getAsJsonObject();
        result.setMilage(milageObject.get(Fueling.KEY_VALUE).getAsDouble(),
                Fueling.MilageUnit.fromString(milageObject.get(Fueling.KEY_UNIT).getAsString()));

        JsonObject volumeObject = jsonObject.get(Fueling.KEY_VOLUME).getAsJsonObject();
        result.setVolume(volumeObject.get(Fueling.KEY_VALUE).getAsDouble(),
                Fueling.VolumeUnit.fromString(volumeObject.get(Fueling.KEY_UNIT).getAsString()));

        JsonObject costObject = jsonObject.get(Fueling.KEY_COST).getAsJsonObject();
        result.setCost(costObject.get(Fueling.KEY_VALUE).getAsDouble(),
                Fueling.CostUnit.fromString(costObject.get(Fueling.KEY_UNIT).getAsString()));

        if(jsonObject.has(Fueling.KEY_COMMENT)){
            result.setComment(jsonObject.get(Fueling.KEY_COMMENT).getAsString());
        }

        return result;
    }

}
