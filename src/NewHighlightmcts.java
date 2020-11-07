import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import enumerate.Action;
import enumerate.State;
import mcts.MCTS;
import mcts.Node;
import mcts.Prediction;
import parameter.FixParameter;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.MotionData;

/**Highlight AI<br>
 *
 *
 * 動作環境: FightingICE version 4以降
 *
 * @author Ishii Ryota
 * */
public class NewHighlightmcts implements AIInterface {

	private Simulator simulator;
	private Key key;
	private CommandCenter commandCenter;
	private boolean playerNumber;

	/** 大本のFrameData */
	private FrameData frameData;

	/** 大本よりFRAME_AHEAD分遅れたFrameData */
	private FrameData simulatorAheadFrameData;

	/** 自分が行える行動全て */
	private LinkedList<Action> myActions;

	/** 相手が行える行動全て */
	private LinkedList<Action> oppActions;

	/** 自分の情報 */
	private CharacterData myCharacter;

	/** 相手の情報 */
	private CharacterData oppCharacter;

	private Action[] actionAir;

	private Action[] actionGround;

	/** STAND_D_DF_FCの回避行動用フラグ */
	private boolean isFcFirst = true;

	/** 敵がSTAND_D_DF_FCを使ってくるかどうか */
	private boolean canFC = true;

	/** STAND_D_DF_FCの回避行動時間を計測する */
	private long firstFcTime;

	private ArrayList<MotionData>  myMotion;

	private ArrayList<MotionData>  oppMotion;

	private Action spSkill;

	private Node rootNode;

	private MCTS mcts;

	Logger logger;

	@Override
	public void close() {
		// TODO 自動生成されたメソッド・スタブ
		logger.outputLog();
	}

	@Override
	public void getInformation(FrameData frameData, boolean isControl) {
		this.frameData = frameData;
		this.commandCenter.setFrameData(this.frameData, playerNumber);
		this.myCharacter = this.frameData.getCharacter(playerNumber);
		this.oppCharacter = this.frameData.getCharacter(!playerNumber);
	}

	private static FileWriter csvWriter = null;

	@Override
	public int initialize(GameData gameData, boolean playerNumber) {
		this.playerNumber = playerNumber;

		this.key = new Key();
		this.frameData = new FrameData();
		this.commandCenter = new CommandCenter();

		this.myActions = new LinkedList<Action>();
		this.oppActions = new LinkedList<Action>();

		this.simulator = gameData.getSimulator();
		this.myMotion = gameData.getMotionData(playerNumber);
		this.oppMotion = gameData.getMotionData(!playerNumber);

		logger = new Logger(playerNumber);

	/*	this.beta = 0;
		this.resultHpDiff = new int[3];
		Arrays.fill(resultHpDiff, 0);*/

		// 各種項目の初期化
		setPerformAction();

		return 0;
	}

	@Override
	public void processing() {

		if (canProcessing()) {

			// フラグによって予測をするか選択
			if (FixParameter.PREDICT_FLAG) {
				if (oppMotion.get(oppCharacter.getAction().ordinal()).getFrameNumber() == oppCharacter
						.getRemainingFrame()) {
					Prediction.getInstance().countOppAction(this.frameData,oppCharacter, commandCenter);
				}
			}

			if (commandCenter.getSkillFlag()) {
				key = commandCenter.getSkillKey();
			} else {
				key.empty();
				commandCenter.skillCancel();

				aheadFrame(); // 遅れフレーム分進める

				// フラグによって回避行動をするかどうか選択
				if (FixParameter.AVOID_FLAG) {
					String enemyAction = this.frameData.getCharacter(!playerNumber).getAction().name();
					int enemyEnergy = this.frameData.getCharacter(!playerNumber).getEnergy();

					if (enemyAction.equals("STAND_D_DF_FC")) {
						canFC = true;
						isFcFirst = true;
					}

					if (enemyEnergy >= 150 && canFC) {
						if (isFcFirst) {
							firstFcTime = frameData.getRemainingTime();
							isFcFirst = false;
						}
						if (firstFcTime - frameData.getRemainingTime() >= FixParameter.AVOIDANCE_TIME) {
							canFC = false;
							isFcFirst = true;
						} else {
							commandCenter.commandCall("STAND_D_DB_BA");
							rootNode = null;
							return;
						}
					}
				}

				if (FixParameter.PREDICT_FLAG) {
					Prediction.getInstance().getInfomation(); // 回数順でソート
				}

				// MCTSによる行動決定
				Action bestAction = Action.STAND_F_D_DFB;
				if(rootNode == null){
					mctsPrepare(); // MCTSの下準備を行う
				}
				bestAction = mcts.runMcts(); // MCTSの実行
				if(ableAction(bestAction)){
					commandCenter.commandCall(bestAction.name()); // MCTSで選択された行動を実行する
					logger.updateLog(rootNode.games);
					if (FixParameter.DEBUG_MODE) {
						mcts.printNode(rootNode);
					}
					rootNode = null;
				}
			}
		} else {
			canFC = true;
			isFcFirst = true;
		}
	}

	public boolean ableAction(Action action) {
		if (action == null)
			return false;
		if (myCharacter.isControl()) {
			return true;
		} else {
			return myCharacter.isHitConfirm() && checkFrame() && checkAction(action);
		}
	}

	public boolean checkFrame(){
		return (myMotion.get(myCharacter.getAction().ordinal()).getCancelAbleFrame() <= myMotion.get(myCharacter.getAction().ordinal()).getFrameNumber() - myCharacter.getRemainingFrame());
	}

	public boolean checkAction(Action act){
		return (myMotion.get(myCharacter.getAction().ordinal()).getCancelAbleMotionLevel() >= myMotion.get(act.ordinal()).getMotionLevel());
	}

	@Override
	public void roundEnd(int x, int y, int frame) {

	}

	/**
	 * MCTSの下準備 <br>
	 * 遅れフレーム分進ませたFrameDataの取得などを行う
	 */
	public void mctsPrepare() {
		setMyAction();
		setOppAction();

		rootNode = new Node(null);
		mcts = new MCTS(rootNode, simulatorAheadFrameData, simulator, myCharacter.getHp(), oppCharacter.getHp(),
				myActions, oppActions, playerNumber, myMotion);

		mcts.createNode(rootNode);

	}

	/** 自身の可能な行動をセットする */
	public void setMyAction() {
		myActions.clear();

		int energy = myCharacter.getEnergy();

		if (myCharacter.getState() == State.AIR) {
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(myMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					myActions.add(actionAir[i]);
				}
			}
		} else {
			if (Math.abs(
					myMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
				myActions.add(spSkill);
			}

			for (int i = 0; i < actionGround.length; i++) {
				if (Math.abs(myMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					myActions.add(actionGround[i]);
				}
			}
		}

	}

	/** 相手の可能な行動をセットする */
	public void setOppAction() {
		oppActions.clear();

		int energy = oppCharacter.getEnergy();

		if (oppCharacter.getState() == State.AIR) {
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(oppMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(actionAir[i]);
				}
			}
		} else {
			if (Math.abs(oppMotion.get(Action.valueOf(spSkill.name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				oppActions.add(spSkill);
			}

			for (int i = 0; i < actionGround.length; i++) {
				if (Math.abs(oppMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(actionGround[i]);
				}
			}
		}
	}

	/** 遅れフレーム分進める */
	private void aheadFrame() {

		simulatorAheadFrameData = simulator.simulate(this.frameData, playerNumber, null, null,14);
		myCharacter = simulatorAheadFrameData.getCharacter(playerNumber);
		oppCharacter = simulatorAheadFrameData.getCharacter(!playerNumber);
	}

	/** アクションの配列の初期化 */
	private void setPerformAction() {
		actionAir = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA, Action.AIR_DB,
				Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FA, Action.AIR_D_DF_FB,
				Action.AIR_F_D_DFA, Action.AIR_F_D_DFB, Action.AIR_D_DB_BA, Action.AIR_D_DB_BB };
		/*actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK, Action.DASH,
				Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD, Action.CROUCH_GUARD, Action.THROW_A,
				Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
				Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB,
				Action.STAND_F_D_DFA, Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };*/
		actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK, Action.DASH,
				Action.JUMP, Action.STAND_GUARD, Action.CROUCH_GUARD, Action.THROW_A,
				Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
				Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB,
				Action.STAND_F_D_DFA, Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };
		spSkill = Action.STAND_D_DF_FC;
	}

	/**
	 * AIが行動できるかどうかを判別する
	 *
	 * @return AIが行動できるかどうか
	 */
	public boolean canProcessing() {
		return !frameData.getEmptyFlag() && frameData.getRemainingTime() > 0;
	}

	@Override
	public Key input() {
		// TODO 自動生成されたメソッド・スタブ
		return key;
	}
}
