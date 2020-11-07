package mcts;

import java.util.LinkedList;

import enumerate.Action;

/**
 * MCTSで利用するNode
 *
 * @author Taichi Miyazaki modified my Makoto Ishihara
 */
public class Node implements Cloneable {

	/** 親ノード */
	private Node parent;

	/** 子ノード */
	public Node[] children;

	private Action action;

	/** ノードの深さ */
	public int depth;

	/** ノードが探索された回数 */
	public int games;

	/** UCB1値 */
	public double ucb;

	/** 評価値 */
	public double score;

	public boolean isCreateNode;

	/**
	 * 子ノード専用のコンストラクタ. 情報をノードに格納
	 */
	public Node(Node parent, Action act) {
		this(parent);
		this.action = act;
	}

	/**
	 * ルートノード専用のコンストラクタ. 情報をノードに格納
	 */
	public Node(Node parent) {
		this.parent = parent;

		if (this.parent != null) {
			this.depth = this.parent.depth + 1;
		} else {
			this.depth = 0;
		}
	}

	/**
	 * ノードに格納されている親ノードの情報を返す
	 *
	 * @return 親ノード
	 */
	public Node getParentNode() {
		return this.parent.clone();
	}

	public Action getAction() {
		return this.action;
	}

	/**
	 * @return rootNodeからのActionの系列
	 */
	public LinkedList<Action> selectedActionFromRoot() {
		LinkedList<Action> actionList = new LinkedList<Action>();
		Node temp = this;

		while (temp.parent != null) {
			actionList.addFirst(temp.action);
			temp = temp.parent;
		}

		return actionList;
	}

	@Override
	public Node clone() {
		Node node = null;

		try {
			node = (Node) super.clone();
			if (node.parent != null) {
				node.parent = this.parent.clone();
			}

			if (node.children != null) {
				node.children = this.children.clone();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return node;
	}

}