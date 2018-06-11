package extensive_form_efg_game_format;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import javax.rmi.CORBA.Util;

import extensive_form_filemanager.CreateGambitEFGFile;
import utils.Utils;


public class CreateTree {
	private ArrayList<String> mChnaceNodeActionList;
	private ArrayList<Double> mChanceNodeProbablityList;
	private CreateGambitEFGFile createGambitFile;
	private Hashtable<String, Integer> mBinarytoIntNumbers;
	private Node mChanceNode;
	private int mChaceInfoSetNo = 1;
	private int mTotalFeatures = Utils.TOTAL_FEATUES_NUMBER_IN_GAME;
	private int mOutcomeCnt;
	private double[] modificationCost = {2.0,3.0,1.0,4.0};
	

	public void init() {
		mOutcomeCnt = 0;
		mChnaceNodeActionList = new ArrayList<String>();
		mChanceNodeProbablityList = new ArrayList();
		createGambitFile = new CreateGambitEFGFile("hsg_4_features");
		mBinarytoIntNumbers = new Hashtable<>();
		int numChanceNode = (int)Math.pow(2.0,(double) mTotalFeatures);
		double[] prob = generateRandomProbability(numChanceNode);
		for (int i = 0; i < prob.length; i++) {
			double tmp = (Math.round(prob[i] * 100.0)) / 100.0;
			// System.out.println(tmp);
			mChanceNodeProbablityList.add(tmp);
		}
		generateBinaryRepresentation(0, mTotalFeatures);
		mChanceNode = new Node(Utils.CHANCE_NODE_NAME, mChaceInfoSetNo, mChnaceNodeActionList,
				mChanceNodeProbablityList, 0);
		createGambitFile.createChanceNode(mChanceNode.getNodeName(), mChanceNode.getInfoSetNumber(),
				mChanceNode.getActionsList(), mChanceNode.getProbabilitiees(), 0);

		/*
		 * mChnaceNodeActionList.add("A"); mChnaceNodeActionList.add("B");
		 * mChanceNodeProbablityList.add(0.5); mChanceNodeProbablityList.add(0.5);
		 * createGambitFile.createChanceNode("c", 1, mChnaceNodeActionList,
		 * mChanceNodeProbablityList, 0);
		 */
	}

	private void movePlayerOne() {
		for (int i = 0; i < mChnaceNodeActionList.size(); i++) {
			// System.out.println("Main " + mChnaceNodeActionList.get(i));
			ArrayList<String> actions = new ArrayList<>();
			ArrayList<Integer> flipPositions = new ArrayList<>();
			//actions.add(mChnaceNodeActionList.get(i));
			//flipPositions.add(-1);
			int infoNo = Integer.parseInt(mChnaceNodeActionList.get(i),2);
			for (int j = 0; j < Utils.HONEYPOT_FEATURES_NUM + Utils.REAL_HOST_FEATURES_NUM; j++) {
				int flipFeature = flipBits(infoNo, j);
				String strFormat = "\""+"%"+ Utils.TOTAL_FEATUES_NUMBER_IN_GAME +"s\"";//TODO find a solution
				//System.out.println(strFormat);
				String flippedStr = String.format("%4s", Integer.toBinaryString(flipFeature)).replace(' ', '0');
				actions.add(flippedStr);
				flipPositions.add(j);
				//System.out.println(flippedStr);
			}

			createGambitFile.createPlayerNode(Utils.PLAYER_NODE_NAME, Utils.PLAYER_ONE, infoNo,
					mChnaceNodeActionList.get(i), actions, 0);
			for (int k = 0; k < actions.size(); k++) {
				movePlayerTwo(actions.get(k),flipPositions.get(k), infoNo, mChnaceNodeActionList.get(i));
			}
			if (null != actions) {
				actions.clear();
				actions = null;
			}

		}
	}

	private void movePlayerTwo(String playerOneAction,int flipPos, int palyerOneInfoSet, String natureAction) {
		//System.out.println("P1:"+palyerOneInfoSet);
		ArrayList<String> actions = new ArrayList<>();
		int len = playerOneAction.length();
		actions.add(playerOneAction.substring(0, Utils.REAL_HOST_FEATURES_NUM));
		actions.add(playerOneAction.substring(Utils.REAL_HOST_FEATURES_NUM, Utils.TOTAL_FEATUES_NUMBER_IN_GAME));
		int infosetNo = mBinarytoIntNumbers.get(playerOneAction);
		// System.out.println(infosetNo);
		createGambitFile.createPlayerNode(Utils.PLAYER_NODE_NAME, Utils.PLAYER_TWO, infosetNo, playerOneAction, actions,
				0);
		setTerminalNode(actions,flipPos, playerOneAction, palyerOneInfoSet,natureAction);
	}

	private double calculatePayoff(String playerOneAction) {
		int val = Integer.parseInt(playerOneAction, 2);
		int realHostval = val >> Utils.HONEYPOT_FEATURES_NUM;
		// Feature starts 0000 where 00 is real host's value. Add 1 to every realhost
		// value
		double payoff = realHostval + 1;
		return payoff;
	}
	
	
	private double calculateFeatureChaningCost(String playerOneAction, int infoSet, int flipPos) {
		//System.out.println(playerOneAction + ":" + infoSet);
		int val = Integer.parseInt(playerOneAction, 2);
		//int cost = val ^ infoSet; // XOR operation to chage the feature
		int cost = 1;//flipPos+1;
		return cost;
	}

	private double getUtility(int index) {
		
		return modificationCost[index];
		
	}
	private boolean isActionsEqual(List actions) {
		for (int i = 0; i < actions.size() - 1; i++) {
			if (!actions.get(i).equals(actions.get(i + 1)))
				return false;
		}
		return true;
	}

	private void setTerminalNode(List actions,int flipPos, String playerOneAction, int playerOneInfoSet, String natureAction) {

		//double realVal = calculatePayoff(natureAction);

		double payoff = getUtility(flipPos);
		//System.out.println("pos" + flipPos + " payoff " + payoff);
		boolean isEqual = isActionsEqual(actions);
		ArrayList<Double> payoffs = new ArrayList<>();
		for (int k = 0; k < actions.size(); k++) {
			double actionVal = Integer.parseInt((String) actions.get(k), 2);
			if (payoffs.size() != 0)
				payoffs.clear();
			/*if (realVal == actionVal + 1) {
				
				double payoff;
				if(flipPos >=0)
				payoff=  realVal+ calculateFeatureChaningCost(playerOneAction, playerOneInfoSet, flipPos);// feature changing cost added with real value
				else
					payoff = realVal;*/
			if (k == 0 || isEqual) {
				if (isEqual) {

					payoffs.add(-payoff / 2);
					payoffs.add(payoff / 2);
				} else {
					payoffs.add(-payoff);
					payoffs.add(payoff);
				}
			} else {
				payoffs.add(0.0);
				payoffs.add(0.0);
			}
			createGambitFile.createTerminalNode(Utils.TERMINAL_NODE_NAME, ++mOutcomeCnt, "Outcome " + mOutcomeCnt,
					payoffs);
		}

	}

	public int flipBits(int n, int k) {
		int mask = 1 << k;

		return n ^ mask;
	}

	private void closeFile() {
		createGambitFile.closeFile();
	}

	private double[] generateRandomProbability(int n) {
		double a[] = new double[n];
		double s = 0.0d;
		Random random = new Random();
		for (int i = 0; i < n; i++) {
			a[i] = 1.0d - random.nextDouble();
			a[i] = -1 * Math.log(a[i]);
			s += a[i];
		}
		for (int i = 0; i < n; i++) {
			a[i] /= s;
		}
		return a;
	}

	public void generateBinaryRepresentation(int i, int n) {
		if (i == (1 << n))
			return;
		else {
			String temp = Integer.toBinaryString(i);
			while (temp.length() < n) {
				temp = '0' + temp;
			}
			//System.out.println(temp);
			mBinarytoIntNumbers.put(temp, Integer.parseInt(temp, 2));
			mChnaceNodeActionList.add(temp);
			generateBinaryRepresentation(i + 1, n);
		}
	}

	public static void main(String[] args) {
		CreateTree tree = new CreateTree();
		tree.init();
		// System.out.println(Integer.toBinaryString(tree.flipBits(0,1)));
		tree.movePlayerOne();
		tree.closeFile();
		// tree.generateBinaryRepresentation(0, 2);
		// double[] arr = tree.getRandomProbability(5);

	}
}
