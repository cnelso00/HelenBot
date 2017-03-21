package com.helen.commands;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.jibble.pircbot.PircBot;

import com.helen.bots.PropertiesManager;

public class Command {

	private PircBot helen;

	private boolean magnusMode = true;

	private static HashMap<IRCCommand, Method> commandList = new HashMap<IRCCommand, Method>();

	public Command() {

	}

	public Command(PircBot ircBot) {
		helen = ircBot;
	}

	static {
		for (Method m : Command.class.getDeclaredMethods()) {
			if (m.isAnnotationPresent(IRCCommand.class)) {
				commandList.put(m.getAnnotation(IRCCommand.class), m);
			}
		}
	}

	public void dispatchTable(CommandData data) {

		for (IRCCommand a : commandList.keySet()) {
			if (a.startOfLine()) {
				if (a.command().equals(data.getCommand())) {
					try {
						commandList.get(a).invoke(this, data);
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				if (data.getMessage().contains(a.command())) {
					try {
						commandList.get(a).invoke(this, data);
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
					data.getSender() + ": Greetings, I am HelenBot v" + PropertiesManager.getProperty("version"));
		}
		helen.sendMessage(data.getChannel(),
				data.getSender() + ": Greetings, I am HelenBot v" + PropertiesManager.getProperty("version"));
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
	
	/*
	 * UNIMPLEMENTED CODE
	 * 
	 * 
	 * 
	 * if (message.substring(0, 3).equalsIgnoreCase(".g")) {
			try {
				webSearch(message.split(".g")[1], channel, sender);
			} catch (IOException e) {
				sendMessage(channel, sender
						+ ": There was some kind of error.  Please contact DrMagnus, and give him the following error code: IOEx_web_search_01");
			}
		}
	 */

}
