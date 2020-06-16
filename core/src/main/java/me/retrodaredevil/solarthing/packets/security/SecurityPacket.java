package me.retrodaredevil.solarthing.packets.security;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import me.retrodaredevil.solarthing.packets.DocumentedPacket;

@JsonSubTypes({
		@JsonSubTypes.Type(IntegrityPacket.class),
		@JsonSubTypes.Type(LargeIntegrityPacket.class),
		@JsonSubTypes.Type(AuthNewSenderPacket.class),
})
public interface SecurityPacket extends DocumentedPacket<SecurityPacketType> {
}
