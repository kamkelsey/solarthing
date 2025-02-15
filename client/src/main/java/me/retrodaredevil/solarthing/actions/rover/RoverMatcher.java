package me.retrodaredevil.solarthing.actions.rover;

import me.retrodaredevil.action.node.environment.InjectEnvironment;
import me.retrodaredevil.solarthing.FragmentedPacketGroupProvider;
import me.retrodaredevil.solarthing.PacketGroupProvider;
import me.retrodaredevil.solarthing.actions.environment.LatestFragmentedPacketGroupEnvironment;
import me.retrodaredevil.solarthing.actions.environment.LatestPacketGroupEnvironment;
import me.retrodaredevil.solarthing.annotations.Nullable;
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup;
import me.retrodaredevil.solarthing.packets.collection.PacketGroup;
import me.retrodaredevil.solarthing.packets.identification.NumberedIdentifier;
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoverMatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(RoverMatcher.class);

	private final Integer fragmentId;
	private final int number;

	public RoverMatcher(Integer fragmentId, int number) {
		this.fragmentId = fragmentId;
		this.number = number;
	}
	public static RoverMatcher createFromRaw(Integer fragmentId, Integer number) {
		return new RoverMatcher(
				fragmentId,
				number == null ? NumberedIdentifier.DEFAULT_NUMBER : number
		);
	}

	public Provider createProvider(InjectEnvironment injectEnvironment) {
		LatestFragmentedPacketGroupEnvironment latestFragmentedPacketGroupEnvironment = injectEnvironment.getOrNull(LatestFragmentedPacketGroupEnvironment.class);
		if (latestFragmentedPacketGroupEnvironment == null) {
			LatestPacketGroupEnvironment latestPacketGroupEnvironment = injectEnvironment.get(LatestPacketGroupEnvironment.class);
			return new LocalProvider(latestPacketGroupEnvironment.getPacketGroupProvider());
		}
		return new FragmentedProvider(latestFragmentedPacketGroupEnvironment.getFragmentedPacketGroupProvider());
	}

	interface Provider {
		/**
		 * @throws IllegalStateException Thrown if there was any invalid configuration
		 */
		void validateData();

		/**
		 * Note: You should call {@link #validateData()} at least once before calling this method. {@link #validateData()} throws only a certain set of exceptions,
		 * where as this could throw any type of {@link RuntimeException} if you do not validate beforehand.
		 * @return The latest {@link RoverStatusPacket} that matches data from a {@link RoverMatcher}
		 */
		@Nullable RoverStatusPacket get();
	}

	private class LocalProvider implements Provider {
		private final PacketGroupProvider packetGroupProvider;

		private LocalProvider(PacketGroupProvider packetGroupProvider) {
			this.packetGroupProvider = packetGroupProvider;
		}

		@Override
		public void validateData() {
			if (fragmentId != null) {
				throw new IllegalStateException("For a local environment, you cannot specify the fragment ID!");
			}
		}

		@Override
		public @Nullable RoverStatusPacket get() {
			PacketGroup packetGroup = packetGroupProvider.getPacketGroup();
			if (packetGroup == null) {
				LOGGER.warn("packetGroup is null!");
				return null;
			}
			return packetGroup.getPackets().stream()
					.filter(packet -> packet instanceof RoverStatusPacket)
					.map(packet -> (RoverStatusPacket) packet)
					.filter(packet -> packet.getNumber() == number)
					.findAny().orElse(null);
		}
	}
	private class FragmentedProvider implements Provider {
		private final FragmentedPacketGroupProvider fragmentedPacketGroupProvider;

		public FragmentedProvider(FragmentedPacketGroupProvider fragmentedPacketGroupProvider) {
			this.fragmentedPacketGroupProvider = fragmentedPacketGroupProvider;
		}

		@Override
		public void validateData() {
			if (fragmentId == null) {
				// A RoverMatcherData can be used "locally" in the request (rover) program and can also be used in the automation program.
				//   When used in the automation program, there's the possibility of multiple fragments each having their own rovers
				//   so, in that case, we have to be explicit
				throw new IllegalStateException("In the automation program, or in an environment where the fragment ID is provided, you must supply a fragment ID to compare against!");
			}
		}

		@Override
		public @Nullable RoverStatusPacket get() {
			FragmentedPacketGroup packetGroup = fragmentedPacketGroupProvider.getPacketGroup();
			if (packetGroup == null) {
				LOGGER.warn("packetGroup is null!");
				return null;
			}
			int fragmentId = RoverMatcher.this.fragmentId; // implicitly assert this is non-null // should have been caught by validateData earlier anyway if it was null
			return packetGroup.getPackets().stream()
					.filter(packet -> packetGroup.getFragmentId(packet) == fragmentId && packet instanceof RoverStatusPacket)
					.map(packet -> (RoverStatusPacket) packet)
					.filter(packet -> packet.getNumber() == number)
					.findAny().orElse(null);
		}
	}
}
