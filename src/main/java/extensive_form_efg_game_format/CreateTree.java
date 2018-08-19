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
	private ArrayList<String> realHostConfigList;
	private ArrayList<String> honeypotConfigLIst;
	private ArrayList<Double> mChanceNodeProbablityList;
	private CreateGambitEFGFile createGambitFile;
	private Hashtable<String, Integer> mBinarytoIntNumbers;
	public Hashtable<String, Integer> realSystemValues;
	public Hashtable<String, Integer> honeypotValues;
	public Hashtable<String, Double> realSystemProbabilities;
	public Hashtable<String, Double> honeypotProbabilites;
	public Hashtable<String,String> p2InformationSet;
	private Node mChanceNode;
	private int mChaceInfoSetNo = 1;
	private int mTotalFeatures = Utils.TOTAL_FEATUES_NUMBER_IN_GAME;
	private int mOutcomeCnt;
	private double[] modificationCost = {1.0,1.0,0.0,2.0,2.0};
	
	

	public void init() {
		mOutcomeCnt = 0;
		mChnaceNodeActionList = new ArrayList<String>();
		realHostConfigList = new ArrayList<String>();
		honeypotConfigLIst = new ArrayList<String>();
		mChanceNodeProbablityList = new ArrayList();
		createGambitFile = new CreateGambitEFGFile("hsg_4_features");
		mBinarytoIntNumbers = new Hashtable<>(); 
		realSystemValues = new Hashtable<>();
		realSystemProbabilities = new Hashtable<>();
		honeypotValues = new Hashtable<>();
		honeypotProbabilites = new Hashtable<>();
		p2InformationSet = new Hashtable<>();
		setSystemValues();
		int numChanceNode = (int)Math.pow(2.0,(double) mTotalFeatures)*2;
		double[] prob = generateRandomProbability(numChanceNode);
		for (int i = 0; i < prob.length; i++) {
			//double tmp = (Math.round(prob[i] * 100.0)) / 100.0; //Random nature probability generation
			double tmp = ((1.0/prob.length) * 100.0) / 100.0;
			//System.out.println(tmp);
			//mChanceNodeProbablityList.add(tmp);
		}
		setProbability();
		generateBinaryRepresentation(0, Utils.REAL_HOST_FEATURES_NUM);
		generateNatureActions();
		
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
		movePlayerOne();
	}
	
	public void setProbability() {
		realSystemProbabilities.put("00",0.3);
		realSystemProbabilities.put("01", 0.3);
		realSystemProbabilities.put("10", 0.3); 
		realSystemProbabilities.put("11", 0.1);
		honeypotProbabilites.put("00", 0.25);
		honeypotProbabilites.put("01", 0.25);
		honeypotProbabilites.put("10", 0.25);
		honeypotProbabilites.put("11", 0.25);
		
	}
	
	public void setSystemValues() {
		realSystemValues.put("00", 3);
		realSystemValues.put("01", 3);
		realSystemValues.put("10", 3);
		realSystemValues.put("11", 3);
		honeypotValues.put("00", 3);
		honeypotValues.put("01", 3); 
		honeypotValues.put("10", 3);
		honeypotValues.put("11",3);
		
	}
	
	public static String reverseStr(String str) {
	    if ( str == null ) {
	          return null;
	    }
	    int len = str.length();
	    if (len <= 0) {
	        return "";
	    }
	    char[] strArr = new char[len];
	    int count = 0;
	    for (int i = len - 1; i >= 0; i--) {
	        strArr[count] = str.charAt(i);
	        count++;
	    }
	    return new String(strArr);
	}
	
	public void generateNatureActions() {
		double sum = 0;
		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < realHostConfigList.size(); j++) {
				for (int k = 0; k < realHostConfigList.size(); k++) {
					int realFlag = 0;
					double probability=0;
					if(i == 0) {
						realFlag = 1;
						probability = realSystemProbabilities.get(realHostConfigList.get(j)) * honeypotProbabilites.get(realHostConfigList.get(k));
						//System.out.println("Prob : " + probability);
						sum += probability;
					}
					String actionStr= realFlag +realHostConfigList.get(j)+ i + realHostConfigList.get(k);
					//System.out.println(actionStr);
					mChanceNodeProbablityList.add(probability);
					mBinarytoIntNumbers.put(actionStr, Integer.parseInt(actionStr, 2));
					mChnaceNodeActionList.add(actionStr);
					
				}
			}
		}
		//System.out.println("Sum : " + sum);
	}

	private void movePlayerOne() {
		for (int i = 0; i < mChnaceNodeActionList.size(); i++) {
			//System.out.println("Main " + mChnaceNodeActionList.get(i));
			ArrayList<String> actions = new ArrayList<>();
			ArrayList<Integer> flipPositions = new ArrayList<>();
			actions.add(mChnaceNodeActionList.get(i));
			flipPositions.add(-1);
			int infoNo = Integer.parseInt(mChnaceNodeActionList.get(i),2);
			for (int j = 0; j < Utils.HONEYPOT_FEATURES_NUM; j++) {
				if (j == 2)
					continue;
				int flipFeature = flipBits(infoNo, j+3);
				String strFormat = "\""+"%"+ Utils.TOTAL_FEATUES_NUMBER_IN_GAME +"s\"";//TODO find a solution
				//System.out.println(strFormat);
				String flippedStr = String.format("%6s", Integer.toBinaryString(flipFeature)).replace(' ', '0');
				//System.out.println(flippedStr);
				actions.add(flippedStr);
				flipPositions.add(j+3);
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
		ArrayList<String> payoffActions = new ArrayList<>();
		int len = playerOneAction.length();
		// nature action is give to fix the payoff calculation
		
		String natureReal = natureAction.substring(1, Utils.REAL_HOST_FEATURES_NUM+1);
		String natureHp= natureAction.substring(Utils.REAL_HOST_FEATURES_NUM+2, Utils.TOTAL_FEATUES_NUMBER_IN_GAME+2);
		payoffActions.add(natureReal);
		payoffActions.add(natureHp);
		
		String realSysStr = playerOneAction.substring(1, Utils.REAL_HOST_FEATURES_NUM+1);
		String hpStr = playerOneAction.substring(Utils.REAL_HOST_FEATURES_NUM+2, Utils.TOTAL_FEATUES_NUMBER_IN_GAME+2);
		actions.add(realSysStr);
		actions.add(hpStr);
		
		String p2InfoStr = playerOneAction.substring(0,1)+ p2InformationSet.get(realSysStr)+
				playerOneAction.substring(Utils.REAL_HOST_FEATURES_NUM+1,Utils.REAL_HOST_FEATURES_NUM+2)+p2InformationSet.get(hpStr);
		int infosetNo = mBinarytoIntNumbers.get(playerOneAction); // playerOne action was used previously. For creating uncertainity for player2 new information set is used.
		//int infosetNo = mBinarytoIntNumbers.get(p2InfoStr);
		//System.out.println(infoStr);
		createGambitFile.createPlayerNode(Utils.PLAYER_NODE_NAME, Utils.PLAYER_TWO, infosetNo, playerOneAction, actions,
				0);
		setTerminalNode(payoffActions,flipPos, playerOneAction, palyerOneInfoSet,natureAction);
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
		if(index == -1)
			return 0;
		//System.out.println("index" + index);
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

		double cost = getUtility(flipPos);
		//System.out.println("pos" + flipPos + " payoff " + payoff);
		int isReal = Integer.parseInt(playerOneAction, 2);
		isReal = isReal >> 5;
		boolean isEqual = isActionsEqual(actions);
		ArrayList<Double> payoffs = new ArrayList<>();
		for (int k = 0; k < actions.size(); k++) {
			
			//System.out.println(playerOneAction+"Action:" + actions.get(k));

			if (payoffs.size() != 0)
				payoffs.clear();
			if(isReal == 1) {
				isReal = 0;
				double payoff = realSystemValues.get(actions.get(k));
				payoffs.add(-(cost + payoff));
				payoffs.add(payoff);
			}else {
				isReal = 1;
				double payoff = honeypotValues.get(actions.get(k));
				payoffs.add((payoff-cost));
				payoffs.add(-payoff);
			}
			
			createGambitFile.createTerminalNode(Utils.TERMINAL_NODE_NAME, ++mOutcomeCnt, "Outcome " + mOutcomeCnt,
					payoffs);
		}

	}

	public int flipBits(int n, int k) {
		int mask = 1 << k;
				
		return n ^ mask;
	}

	public void closeFile() {
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
			// System.out.println(temp);
			if (!p2InformationSet.containsKey(temp)) {
				if (p2InformationSet.contains(reverseStr(temp))) {
					p2InformationSet.put(temp, p2InformationSet.get(reverseStr(temp)));
				} else {
					p2InformationSet.put(temp, temp);
				}

			}
			realHostConfigList.add(temp);
			// mBinarytoIntNumbers.put(temp, Integer.parseInt(temp, 2));
			// mChnaceNodeActionList.add(temp);
			generateBinaryRepresentation(i + 1, n);
		}
	}
/*
	public static void main(String[] args) {
		CreateTree tree = new CreateTree();
		tree.init();
		// System.out.println(Integer.toBinaryString(tree.flipBits(0,1)));
		tree.movePlayerOne();
		tree.closeFile();
		// tree.generateBinaryRepresentation(0, 2);
		// double[] arr = tree.getRandomProbability(5);

	}*/
}
