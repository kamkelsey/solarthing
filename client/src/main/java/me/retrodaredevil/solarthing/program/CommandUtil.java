package me.retrodaredevil.solarthing.program;

import me.retrodaredevil.couchdb.CouchDbUtil;
import me.retrodaredevil.couchdbjava.CouchDbInstance;
import me.retrodaredevil.solarthing.PacketGroupReceiver;
import me.retrodaredevil.solarthing.commands.packets.open.CommandOpenPacket;
import me.retrodaredevil.solarthing.config.databases.IndividualSettings;
import me.retrodaredevil.solarthing.config.databases.implementations.CouchDbDatabaseSettings;
import me.retrodaredevil.solarthing.config.options.PacketHandlingOption;
import me.retrodaredevil.solarthing.database.DatabaseDocumentKeyMap;
import me.retrodaredevil.solarthing.database.MillisQueryBuilder;
import me.retrodaredevil.solarthing.database.SolarThingDatabase;
import me.retrodaredevil.solarthing.database.couchdb.CouchDbSolarThingDatabase;
import me.retrodaredevil.solarthing.database.exception.SolarThingDatabaseException;
import me.retrodaredevil.solarthing.packets.collection.PacketCollection;
import me.retrodaredevil.solarthing.packets.collection.StoredPacketGroup;
import me.retrodaredevil.solarthing.packets.handling.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandUtil {

	/**
	 * Gets packet handlers that will download requested commands
	 * @param databaseConfigs The list of database configs
	 * @param packetGroupReceiver Receives data that has been downloaded. Note that this may be called in a separate thread, so make sure it is thread safe
	 * @param options The options object
	 * @return A list of packet handlers that, when called, will possibly download commands and then forward those commands to {@code packetGroupReceiver}
	 */
	public static List<PacketHandler> getCommandRequesterHandlerList(List<DatabaseConfig> databaseConfigs, PacketGroupReceiver packetGroupReceiver, PacketHandlingOption options) {
		final List<PacketHandler> commandRequesterHandlerList = new ArrayList<>(); // Handlers to request and get new commands to send (This may block the current thread). (This doesn't actually handle packets)
		for(DatabaseConfig config : databaseConfigs){
			if(CouchDbDatabaseSettings.TYPE.equals(config.getType())){
				CouchDbDatabaseSettings settings = (CouchDbDatabaseSettings) config.getSettings();
				CouchDbInstance instance = CouchDbUtil.createInstance(settings.getCouchProperties(), settings.getOkHttpProperties());
				SolarThingDatabase database = CouchDbSolarThingDatabase.create(instance);

				IndividualSettings individualSettings = config.getIndividualSettingsOrDefault(Constants.DATABASE_COMMAND_DOWNLOAD_ID, null);
				FrequencySettings frequencySettings = individualSettings != null ? individualSettings.getFrequencySettings() : FrequencySettings.NORMAL_SETTINGS;
				PacketHandler packetHandler = new PacketHandler() {
					private final SecurityPacketReceiver securityPacketReceiver = new SecurityPacketReceiver(
							DatabaseDocumentKeyMap.createFromDatabase(database),
							packetGroupReceiver,
							new SecurityPacketReceiver.InstanceTargetPredicate(options.getSourceId(), options.getFragmentId()),
							Collections.singleton(CommandOpenPacket.class),
							System.currentTimeMillis(),
							options.getFragmentId(),
							options.getSourceId(),
							database.getEventDatabase()
					);
					@Override
					public void handle(PacketCollection packetCollection) throws PacketHandleException {
						final List<StoredPacketGroup> packetGroups;
						try {
							packetGroups = database.getOpenDatabase().query(new MillisQueryBuilder().startKey(System.currentTimeMillis() - 5 * 60 * 1000).build());
						} catch (SolarThingDatabaseException e) {
							throw new PacketHandleException(e);
						}
						securityPacketReceiver.receivePacketGroups(packetGroups);
					}
				};
				commandRequesterHandlerList.add(new ThrottleFactorPacketHandler(new AsyncPacketHandlerWrapper(new PrintPacketHandleExceptionWrapper(
						packetHandler
				)), frequencySettings));
			}
		}
		return commandRequesterHandlerList;
	}
}
