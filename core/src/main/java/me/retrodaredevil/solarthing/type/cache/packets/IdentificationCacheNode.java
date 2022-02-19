package me.retrodaredevil.solarthing.type.cache.packets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.retrodaredevil.solarthing.annotations.JsonExplicit;
import me.retrodaredevil.solarthing.annotations.NotNull;
import me.retrodaredevil.solarthing.type.cache.packets.data.IdentificationCacheData;

import static java.util.Objects.requireNonNull;

@JsonExplicit
public class IdentificationCacheNode<T extends IdentificationCacheData> {
	private final int fragmentId;
	private final T data;

	@JsonCreator
	public IdentificationCacheNode(
			@JsonProperty(value = "fragmentId", required = true) int fragmentId,
			@JsonProperty(value = "data", required = true) @NotNull T data) {
		this.fragmentId = fragmentId;
		requireNonNull(this.data = data);
	}

	@JsonProperty("fragmentId")
	public int getFragmentId() { return fragmentId; }

	@JsonProperty("data")
	public @NotNull T getData() { return data; }
}
