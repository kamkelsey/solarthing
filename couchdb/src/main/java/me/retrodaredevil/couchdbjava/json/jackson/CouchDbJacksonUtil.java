package me.retrodaredevil.couchdbjava.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import me.retrodaredevil.couchdbjava.json.JsonData;

public final class CouchDbJacksonUtil {
	private CouchDbJacksonUtil() { throw new UnsupportedOperationException(); }

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static <T> T readValue(ObjectMapper objectMapper, JsonData jsonData, Class<T> clazz) throws JsonProcessingException {
		if (jsonData instanceof JacksonJsonData) {
			return objectMapper.treeToValue(((JacksonJsonData) jsonData).getNode(), clazz);
		}
		return objectMapper.readValue(jsonData.getJson(), clazz);
	}
	public static JsonNode getNodeFrom(JsonData jsonData) throws JsonProcessingException {
		if (jsonData instanceof JacksonJsonData) {
			return ((JacksonJsonData) jsonData).getNode();
		}
		return MAPPER.readTree(jsonData.getJson());
	}
}
