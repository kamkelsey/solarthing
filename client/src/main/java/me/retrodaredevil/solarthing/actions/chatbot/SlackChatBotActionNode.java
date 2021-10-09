package me.retrodaredevil.solarthing.actions.chatbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.util.http.SlackHttpClient;
import me.retrodaredevil.action.Action;
import me.retrodaredevil.action.Actions;
import me.retrodaredevil.solarthing.AlterPacketsProvider;
import me.retrodaredevil.solarthing.FragmentedPacketGroupProvider;
import me.retrodaredevil.solarthing.actions.ActionNode;
import me.retrodaredevil.solarthing.actions.command.CommandManager;
import me.retrodaredevil.solarthing.actions.environment.ActionEnvironment;
import me.retrodaredevil.solarthing.actions.environment.AlterPacketsEnvironment;
import me.retrodaredevil.solarthing.actions.environment.InjectEnvironment;
import me.retrodaredevil.solarthing.actions.environment.LatestFragmentedPacketGroupEnvironment;
import me.retrodaredevil.solarthing.chatbot.*;
import me.retrodaredevil.solarthing.message.implementations.SlackMessageSender;
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup;
import okhttp3.OkHttpClient;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@JsonTypeName("chatbot_slack")
public class SlackChatBotActionNode implements ActionNode {

	private final Action action;

	private volatile FragmentedPacketGroup latestPacketGroup = null;
	private volatile InjectEnvironment injectEnvironment;

	/*
	The app level token should have connections:write permission
	Then add the app to the channel you want it to listen to
	You also need to enable "Socket Mode" for your app
	Then subscribe to events: message.channels
	Then make sure to reinstall your app
	 */

	// useful doc: https://slack.dev/java-slack-sdk/guides/socket-mode

	public SlackChatBotActionNode(
			@JsonProperty(value = "app_token", required = true) String appToken,
			@JsonProperty(value = "token", required = true) String authToken,
			@JsonProperty(value = "channel_id", required = true) String channelId,
			@JsonProperty(value = "permissions", required = true) Map<String, List<String>> permissionMap,
			@JsonProperty(value = "sender", required = true) String sender,
			@JsonProperty(value = "key_directory", required = true) File keyDirectory
	) {
		Slack slack = Slack.getInstance(new SlackConfig(), new SlackHttpClient(new OkHttpClient.Builder()
				.callTimeout(Duration.ofSeconds(10))
				.connectTimeout(Duration.ofSeconds(4))
				.build()));
		FragmentedPacketGroupProvider packetGroupProvider = () -> latestPacketGroup;
		ChatBotCommandHelper commandHelper = new ChatBotCommandHelper(permissionMap, packetGroupProvider, new CommandManager(keyDirectory, sender));

		Supplier<InjectEnvironment> injectEnvironmentSupplier = () -> injectEnvironment;
		AlterPacketsProvider alterPacketsProvider = () -> injectEnvironment.get(AlterPacketsEnvironment.class).getAlterPacketsProvider().getPackets();
		action = new SlackChatBotAction(
				appToken,
				new SlackMessageSender(authToken, channelId, slack),
				slack,
				new HelpChatBotHandler(
						new ChatBotHandlerMultiplexer(Arrays.asList(
								new StaleMessageHandler(), // note: this isn't applied to "help" commands
								new ScheduleCommandChatBotHandler(commandHelper, injectEnvironmentSupplier),
								new CommandChatBotHandler(commandHelper, injectEnvironmentSupplier),
								new StatusChatBotHandler(packetGroupProvider, alterPacketsProvider),
								(message, messageSender) -> {
									messageSender.sendMessage("Unknown command!");
									return true;
								}
						))
				)
		);
	}

	// This is designed to be handled by the automation program.
	// The automation program creates a new action each iteration,
	//   so we update a single action each iteration instead of creating a new
	//   one a bunch of times.

	@Override
	public Action createAction(ActionEnvironment actionEnvironment) {
		LatestFragmentedPacketGroupEnvironment latestPacketGroupEnvironment = actionEnvironment.getInjectEnvironment().get(LatestFragmentedPacketGroupEnvironment.class);
		actionEnvironment.getInjectEnvironment().require(AlterPacketsEnvironment.class); // used elsewhere, so let's assert that we have it
		this.injectEnvironment = actionEnvironment.getInjectEnvironment();
		return Actions.createRunOnce(() -> {
			latestPacketGroup = latestPacketGroupEnvironment.getFragmentedPacketGroupProvider().getPacketGroup();
			action.update();
		});
	}
}
