package me.retrodaredevil.solarthing.commands.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.retrodaredevil.solarthing.annotations.NotNull;
import me.retrodaredevil.solarthing.annotations.WorkInProgress;
import me.retrodaredevil.solarthing.marker.EventPacket;
import me.retrodaredevil.solarthing.packets.TypedDocumentedPacket;
import me.retrodaredevil.solarthing.packets.collection.StoredIdentifier;

/**
 * A security event packets describe if a given requested encrypted data was successfully decrypted.
 * All security event packets contain a {@link StoredIdentifier}. The stored identifier can be used to uniquely identify
 * one stored packet group in the "solarthing_open" database.
 */
@WorkInProgress
public interface SecurityEventPacket extends TypedDocumentedPacket<SecurityEventPacketType>, EventPacket {
	/*
	The reason that we use StoredIdentifier rather than UpdateTokens alongside a document ID is because
	we are referring to a document that is in solarthing-open, which is a MillisDatabase. MillisDatabase is setup
	to deal with StoredPacketGroups, rather than VersionedPacket<StoredPacketGroup>s. Plus, UpdateTokens are
	designed to do just that, update, which we don't need to do.

	Although it may not seem like it, StoredIdentifiers are actually higher level than UpdateTokens, which is why
	we StoredIdentifiers.
	 */

	@JsonProperty("storedIdentifier")
	@NotNull StoredIdentifier getStoredIdentifier();
}