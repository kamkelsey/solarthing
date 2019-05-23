package me.retrodaredevil.iot.packets;

import java.util.Collection;

public interface PacketCreator {

	/**
	 * 
	 * @param bytes the characters to read from
	 * @return An empty collection, or if it can be read, a collection of SolarPackets
	 */
	Collection<Packet> add(char[] bytes);
	
	
	
	
}