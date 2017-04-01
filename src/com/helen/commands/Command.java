package com.helen.commands;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.jibble.pircbot.PircBot;

import com.helen.database.Config;
import com.helen.database.Configs;
import com.helen.database.Tell;
import com.helen.database.Tells;
import com.helen.search.WebSearch;
import com.helen.search.YouTubeSearch;

public class Command {
	private static final Logger logger = Logger.getLogger(Command.class);

	private PircBot helen;

	private boolean magnusMode = true;

	private static HashMap<String, Method> hashableCommandList = new HashMap<String, Method>();
	private static HashMap<String, Method> slowCommands = new HashMap<String, Method>();

	public Command() {

	}

	public Command(PircBot ircBot) {
		helen = ircBot;
	}

	static {
		for (Method m : Command.class.getDeclaredMethods()) {
			logger.info(m);
			logger.info(m.isAnnotationPresent(IRCCommand.class));
			if (m.isAnnotationPresent(IRCCommand.class)) {
				if (m.getAnnotation(IRCCommand.class).startOfLine()) {
					hashableCommandList.put(((IRCCommand) m.getAnnotation(IRCCommand.class)).command(), m);
				} else {
					slowCommands.put(((IRCCommand) m.getAnnotation(IRCCommand.class)).command(), m);
				}

				logger.info(((IRCCommand) m.getAnnotation(IRCCommand.class)).command());
			}
		}
		logger.info("Finished Initializing commandList.");
	}
	
	private void checkTells(CommandData data){
		ArrayList<Tell> tells = Tells.getTells(data.getSender());
		for(Tell tell: tells){
			helen.sendMessage(tell.getTarget(), tell.toString());
		}
	}

	public void dispatchTable(CommandData data) {
		
		checkTells(data);
		
		logger.info("Entering dispatch table with command: \"" + data.getCommand() + "\"");
		if (hashableCommandList.containsKey(data.getCommand())) {
			try {
				hashableCommandList.get(data.getCommand()).invoke(this, data);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.error("Exception invoking start-of-line command: " + data.getCommand(), e);
			}
		} else {
			for(String command : slowCommands.keySet()){
				if(data.getMessage().contains(command)){
					try {
						slowCommands.get(command).invoke(this, data);
					} catch (Exception e) {
						logger.error("Exception invoking command: " + command,e);
					}
				}
			}
			
		}
	}

	// Relateively unregulated commands (anyone can try these)
	@IRCCommand(command = ".HelenBot", startOfLine = false)
	public void versionResponse(CommandData data) {
		if (data.getChannel().isEmpty()) {
			helen.sendMessage(data.getSender(),
					data.getSender() + ": Greetings, I am HelenBot v" + Configs.getSingleProperty("version").getValue());
		}
		helen.sendMessage(data.getChannel(),
				data.getSender() + ": Greetings, I am HelenBot v" + Configs.getSingleProperty("version").getValue());
	}

	@IRCCommand(command = ".modeToggle", startOfLine = true)
	public void toggleMode(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, true)) {
			magnusMode = !magnusMode;
		}
	}

	@IRCCommand(command = ".mode", startOfLine = true)
	public void displayMode(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, false)) {
			helen.sendMessage(data.getChannel(),
					data.getSender() + ": I am currently in " + (magnusMode ? "Magnus Only" : " Any User") + " mode.");
		}
	}

	@IRCCommand(command = ".msg", startOfLine = true)
	public void sendMessage(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, false)) {
			String target = data.getTarget();
			String payload = data.getPayload();

			helen.sendMessage(target, data.getSender() + " said:" + payload);

		}
	}

	@IRCCommand(command = ".roll", startOfLine = true)
	public void roll(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, true)) {
			RollData roll = new RollData(data.getMessage());
			if (roll.save()) {
				RollDB.saveRoll(data.getSender(), roll);
			}
			helen.sendMessage(data.getChannel(), data.getSender() + ": " + roll.getRoll());
		}
	}

	@IRCCommand(command = ".myRolls", startOfLine = true)
	public void getRolls(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, true)) {
			String rolls = RollDB.getUserRolls(data.getSender());
			if (rolls != null) {
				helen.sendMessage(data.getChannel(), data.getSender() + ": " + rolls);
			} else {
				helen.sendMessage(data.getChannel(),
						data.getSender() + ": Apologies, I do not have any saved " + "rolls for you at this time.");
			}

		}
	}

	@IRCCommand(command = ".g", startOfLine = true)
	public void webSearch(CommandData data) {
		try {
			helen.sendMessage(data.getChannel(),
					data.getSender() + ": " + WebSearch.search(data.getMessage()).toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Exception during web search", e);
		}
	}

	@IRCCommand(command = ".y", startOfLine = true)
	public void youtubeSearch(CommandData data) {
		helen.sendMessage(data.getChannel(),
				data.getSender() + ": " + YouTubeSearch.youtubeSearch(data.getMessage()).toString());
	}

	// Authentication Required Commands
	@IRCCommand(command = ".join", startOfLine = true)
	public void enterChannel(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, true))
			helen.joinChannel(data.getTarget());

	}

	@IRCCommand(command = ".leave", startOfLine = true)
	public void leaveChannel(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, true))
			helen.partChannel(data.getTarget());

	}
	
	@IRCCommand(command = ".tell", startOfLine = true)
	public void tell(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, true))
			Tells.sendTell(data.getTarget(), data.getSender(), data.getTellMessage());

	}

	@IRCCommand(command = ".exit", startOfLine = true)
	public void exitBot(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, true)) {
			for (String channel : helen.getChannels()) {
				helen.sendMessage(channel, "I have been instructed by my developer to exit.  Have a good day.");
				helen.partChannel(channel);
			}

			helen.disconnect();
			System.exit(0);
		}
	}
	
	@IRCCommand(command = ".allProperties", startOfLine = true)
	public void getAllProperties(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, false)) {
			ArrayList<Config> properties = Configs.getConfiguredProperties();
			helen.sendMessage(data.getChannel(), data.getSender() + ": Configured properties: " + buildConfigResponse(properties));
		}
	}
	
	@IRCCommand(command = ".property", startOfLine = true)
	public void getProperty(CommandData data) {
		if (data.isAuthenticatedUser(magnusMode, false)) {
			ArrayList<Config> properties = Configs.getProperty(data.getTarget());
			helen.sendMessage(data.getChannel(), data.getSender() + ": Configured properties: " + buildConfigResponse(properties));
		}
	}

	private String buildConfigResponse(ArrayList<Config> parts){
		ArrayList<String> stringList = new ArrayList<String>();
		for(Config part: parts){
			if (part.isPublic()){
				stringList.add(part.toString());
			}
		}
		return buildResponse(stringList);
	}
	
	private String buildResponse(ArrayList<String> parts){
		StringBuilder response = new StringBuilder();
		response.append("{");
		for(String str: parts){
			response.append(str);
			response.append("|");
		}
		response.delete(response.length() - 1, response.length());
		response.append("}");
		
		return response.toString();
	}
}
