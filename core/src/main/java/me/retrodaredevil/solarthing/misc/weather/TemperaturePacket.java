package me.retrodaredevil.solarthing.misc.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.retrodaredevil.solarthing.annotations.DefaultFinal;
import me.retrodaredevil.solarthing.annotations.JsonExplicit;
import me.retrodaredevil.solarthing.annotations.NotNull;
import me.retrodaredevil.solarthing.misc.common.SourcedData;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@JsonDeserialize(as = CelsiusTemperaturePacket.class)
@JsonExplicit
@JsonTypeName("TEMPERATURE")
public interface TemperaturePacket extends WeatherPacket, SourcedData {
	/**
	 * Represents temperature celsius values that might indicate a bad reading
	 */
	Set<Float> POSSIBLE_BAD_VALUES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(0.0f, 25.0f)));

	@DefaultFinal
	@Override
	default @NotNull WeatherPacketType getPacketType() {
		return WeatherPacketType.TEMPERATURE;
	}
	@JsonProperty("temperatureCelsius")
	float getTemperatureCelsius();
	@JsonProperty("temperatureFahrenheit")
	float getTemperatureFahrenheit();
}
