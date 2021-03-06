package com.helen.database;

import java.sql.ResultSet;
import java.util.ArrayList;

import com.helen.commands.CommandData;
import org.apache.log4j.Logger;

public class Tells {

	private final static Logger logger = Logger.getLogger(Tells.class);

	public static String sendMultitell(CommandData data){
		String sender = data.getSender().toLowerCase();
		String target = data.getTarget().toLowerCase();
		String message = data.getTellMessage();
		boolean privateMessage = data.getChannel().isEmpty();

		Integer id = Nicks.getNickGroup(target);
		if(id == null || id == -1){
			return sendTell(target, sender, message, privateMessage);
		}else{
			return sendTell(id.toString(),sender,message,privateMessage);
		}
	}

	public static String sendTell(String target, String sender, String message, boolean privateMessage) {
		try {
			CloseableStatement stmt = Connector.getStatement(Queries.getQuery("insertTell"),
					target.toLowerCase(),
					sender,
					message,
					privateMessage);
			if (stmt.executeUpdate()) {
				return "*Jots that down on her clipboard* Got it, I'll let them know...";
			}
		} catch (Exception e) {
			logger.error("Exception sending tell", e);
		}
		return "There was some sort of error.  Please contact DrMagnus";
	}
	
	public static ArrayList<Tell> getTells(String username){
		ArrayList<Tell> list = new ArrayList<Tell>();
		try{

			Integer id = Nicks.getNickGroup(username.toLowerCase());

			CloseableStatement stmt = Connector.getStatement(Queries.getQuery("searchTells"),
					username.toLowerCase(), username.toLowerCase());
			ResultSet rs = stmt.executeQuery();
			while(rs != null && rs.next()){
				list.add(new Tell(rs.getString("sender"),username
						,rs.getTimestamp("tell_time"),rs.getString("message"), rs.getBoolean("privateMessage")));
			}
			stmt.close();
			return list;
		}catch (Exception e){
			logger.error("Exception retreiving tells",e);
		}
		return list;
	}
	
	public static void clearTells(String username){
		try{
			Connector.getStatement(Queries.getQuery("clearTells"),username.toLowerCase(), username.toLowerCase()).executeUpdate();
		}catch (Exception e){
			logger.error("Exception clearing tells",e);
		}
	}

}
