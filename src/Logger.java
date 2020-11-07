import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import enumerate.Action;
import struct.FrameData;
import parameter.FixParameter;

public class Logger {

	public ArrayList<Deque<Action>> action_Log;
	public ArrayList<Integer> feature_Log;
	public ArrayList<Integer> searchNum_Log;

	private boolean player;
	
	public Logger(boolean p) {
		player = p;
		if (FixParameter.ACTION_LOG) {
			action_Log = new ArrayList<Deque<Action>>();
			action_Log.add(new LinkedList<Action>());
			action_Log.add(new LinkedList<Action>());
			new File("./log/Action").mkdir();
		}
		if (FixParameter.PLAY_FEATURE_LOG) {
			feature_Log = new ArrayList<Integer>();
			new File("./log/Playarc").mkdir();
		}
		if(FixParameter.SEARCHNUM_LOG){
			searchNum_Log = new ArrayList<Integer>();
			new File("./log/searchNum").mkdir();
		}
		
	}

	/**
	 * Logの記録
	 * 
	 * @param fd
	 *            frameData
	 */
	public void updateLog(FrameData fd) {
		if (FixParameter.ACTION_LOG) {
			action_Log.get(0).add(fd.getCharacter(true).getAction());
			action_Log.get(1).add(fd.getCharacter(false).getAction());
		}
		if (FixParameter.PLAY_FEATURE_LOG) {
			feature_Log.add((fd.getCharacter(true).getHp() - fd.getCharacter(false).getHp()));
		}
	}

	public void updateLog(int searchNum) {
		if (FixParameter.SEARCHNUM_LOG) {
			searchNum_Log.add(searchNum);
		}
	}
	/**
	 * Logの書き出し
	 */
	public void outputLog() {

		if (FixParameter.ACTION_LOG) {
			int i = 0;
			File actionFile = new File("./log/Action/Action" +"_"+ NewHighlightmcts.class.getName() + (player?0:1) + "_" + i + ".txt");
			while (actionFile.exists()) {
				actionFile = new File("./log/Action/Action" +"_"+ NewHighlightmcts.class.getName() + (player?0:1) + "_" + i + ".txt");
				i++;
			}
			try {
				actionFile.createNewFile();
				BufferedWriter bw = new BufferedWriter(new FileWriter(actionFile));
				while (!action_Log.get(0).isEmpty() || !action_Log.get(1).isEmpty()) {
					if (!action_Log.get(0).isEmpty()) {
						bw.append(action_Log.get(0).removeFirst().name());
					}
					bw.append(",");
					if (!action_Log.get(1).isEmpty()) {
						bw.append(action_Log.get(1).removeFirst().name());
					}
					bw.append(System.getProperty("line.separator"));
				}
				bw.close();
			} catch (IOException e) {
			}
		}
		if (FixParameter.PLAY_FEATURE_LOG) {
			int i = 0;
			File playarcFile = new File("./log/Playarc/Playarc" +"_"+ NewHighlightmcts.class.getName() + (player?0:1)  + "_" + i + ".txt");
			while (playarcFile.exists()) {
				playarcFile = new File("./log/Playarc/Playarc" +"_"+ NewHighlightmcts.class.getName() + (player?0:1)  + "_" + i + ".txt");
				i++;
			}
			try {
				playarcFile.createNewFile();
				BufferedWriter bw = new BufferedWriter(new FileWriter(playarcFile));
				for (int j = 0; j < feature_Log.size() ; j++) {
					bw.append(Integer.toString(feature_Log.get(j)));
					bw.append(System.getProperty("line.separator"));
				}
				bw.close();
			} catch (IOException e) {
			}
		}
		if (FixParameter.SEARCHNUM_LOG) {
			int i = 0;
			File playarcFile = new File("./log/searchNum/searchNum" +"_"+ NewHighlightmcts.class.getName() + (player?0:1)  + "_" + i + ".txt");
			while (playarcFile.exists()) {
				playarcFile = new File("./log/searchNum/searchNum" +"_"+ NewHighlightmcts.class.getName() + (player?0:1)  + "_" + i + ".txt");
				i++;
			}
			try {
				playarcFile.createNewFile();
				BufferedWriter bw = new BufferedWriter(new FileWriter(playarcFile));
				for (int j = 0; j < searchNum_Log.size() ; j++) {
					bw.append(Integer.toString(searchNum_Log.get(j)));
					bw.append(System.getProperty("line.separator"));
				}
				bw.close();
			} catch (IOException e) {
			}
		}
	}
}
