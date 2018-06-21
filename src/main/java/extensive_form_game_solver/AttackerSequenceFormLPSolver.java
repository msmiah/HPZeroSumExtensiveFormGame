package extensive_form_game_solver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import extensive_form_game.Game;
import extensive_form_game.Game.Action;
import extensive_form_game.Game.Node;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ilog.cplex.*;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import utils.Utils;



public class AttackerSequenceFormLPSolver<E> extends ZeroSumGameSolver {
    Game game;

    int playerToSolveFor;
    int playerNotToSolveFor;

    IloCplex cplex;
    IloLinearNumExpr objective;
    //IloNumVar[] modelStrategyVars;
    IloNumVar[] dualVars; // indexed as [informationSetId]. Note that we expect information sets to be 1-indexed, but the code corrects for when this is not the case
    HashMap<String, IloNumVar>[] strategyVarsByInformationSet; // indexed as [inforationSetId][action.name]

    TIntList[] sequenceFormDualMatrix; // indexed as [dual sequence id][information set]
    TIntDoubleMap[] dualPayoffMatrix; // indexed as [dual sequence][primal sequence]
    TIntDoubleMap[] primalPayoffMatrix; 
    ArrayList sameInfosetAction;
    TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP1; // indexed as [informationSetId][action.name]
    TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP2; // indexed as [informationSetId][action.name]
    IloNumVar[] strategyVarsBySequenceId;
    int [] realValueBySequenceId;
    int numSequencesP1;
    int numSequencesP2;
    int numPrimalSequences; 
    int numDualSequences;
    int numPrimalInformationSets;
    int numDualInformationSets;

    String[] dualSequenceNames;
    String[] primalSequenceNames;

    TIntObjectMap<IloConstraint> primalConstraints; // indexed as [informationSetId], without correcting for 1-indexing
    TIntObjectMap<IloRange> dualConstraints; // indexed as [sequenceId]
    double[] nodeNatureProbabilities; // indexed as [nodeId]. Returns the probability of that node being reached when considering only nature nodes
    int[] sequenceIdForNodeP1; // indexed as [nodeId]. Returns the sequenceId of the last sequence belonging to Player 1 on the path to the node.
    int[] sequenceIdForNodeP2; // indexed as [nodeId]. Returns the sequenceId of the last sequence belonging to Player 2 on the path to the node.
    int cnt = 0;
    public AttackerSequenceFormLPSolver(Game game, int playerToSolveFor) {
        this(game, playerToSolveFor, 1e-6);
    }

    public AttackerSequenceFormLPSolver(Game game, int playerToSolveFor, double tol) {
        super(game);
        this.game = game;
        try {
            cplex = new IloCplex();
        } catch (IloException e) {
            System.out.println("Error SequenceFormLPSolver(): CPLEX setup failed");
        }

        this.playerToSolveFor = playerToSolveFor;
        this.playerNotToSolveFor = (playerToSolveFor % 2) + 1;

        initializeDataStructures();
        //modelStrategyVars = new ArrayList<IloNumVar>();
        //dualVars = new ArrayList<IloNumVar>();
        //strategyVarsByRealGameSequences = new ArrayList<IloNumVar>();

        try {
            setUpModel(tol);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the arrays and other data structure objects that we use.
     */
    @SuppressWarnings("unchecked")
    private void initializeDataStructures() {
        int numInformationSets = 0;
        //int numDualInformationSets = 0;


        if (playerToSolveFor == 1) {
            numInformationSets = game.getNumInformationSetsPlayer1();
            //numDualInformationSets = game.getNumInformationSetsPlayer2();
        } else {
            numInformationSets = game.getNumInformationSetsPlayer2();
            //numDualInformationSets = game.getNumInformationSetsPlayer1();
        }
        this.strategyVarsByInformationSet = (HashMap<String, IloNumVar>[]) new HashMap[numInformationSets+1];
        for (int i = 0; i <= numInformationSets; i++) {
            this.strategyVarsByInformationSet[i] = new HashMap<String, IloNumVar>();
        }


        numPrimalSequences = playerToSolveFor == 1 ? game.getNumSequencesP1() : game.getNumSequencesP2();
        numDualSequences = playerNotToSolveFor == 1 ? game.getNumSequencesP1() : game.getNumSequencesP2();
        //System.out.println("Num dual seq : " + numDualSequences);
        sequenceFormDualMatrix = new TIntList[numDualSequences];
        for (int i = 0; i < numDualSequences; i++) {
            sequenceFormDualMatrix[i] =  new TIntArrayList();
        }
        numPrimalInformationSets = playerToSolveFor == 1 ? game.getNumInformationSetsPlayer1() : game.getNumInformationSetsPlayer2();
        numDualInformationSets = playerNotToSolveFor == 1 ? game.getNumInformationSetsPlayer1() : game.getNumInformationSetsPlayer2();

        dualSequenceNames = new String[numDualSequences];
        primalSequenceNames = new String[numPrimalSequences];
        sameInfosetAction = new ArrayList<E>();
        

        dualPayoffMatrix = new TIntDoubleHashMap[numDualSequences];
        primalPayoffMatrix = new TIntDoubleHashMap[numPrimalSequences];
        
        for (int i = 0; i < numDualSequences; i++) {
            dualPayoffMatrix[i] = new TIntDoubleHashMap();
        }
        
        for (int i = 0; i < numPrimalSequences; i++) {
        	primalPayoffMatrix[i] = new TIntDoubleHashMap();
        }

        // ensure that we have a large enough array for both the case where information sets start at 1 and 0
        sequenceIdByInformationSetAndActionP1 = new TObjectIntMap[game.getNumInformationSetsPlayer1()+1];
        sequenceIdByInformationSetAndActionP2 = new TObjectIntMap[game.getNumInformationSetsPlayer2()+1];
        for (int i = 0; i <= game.getNumInformationSetsPlayer1(); i++) {
            sequenceIdByInformationSetAndActionP1[i] = new TObjectIntHashMap<String>();
        }
        for (int i = 0; i <= game.getNumInformationSetsPlayer2(); i++) {
            sequenceIdByInformationSetAndActionP2[i] = new TObjectIntHashMap<String>();
        }

        if (playerToSolveFor == 1) {
            strategyVarsBySequenceId = new IloNumVar[game.getNumSequencesP1()];
            realValueBySequenceId = new int[game.getNumSequencesP1()];
        } else {
            strategyVarsBySequenceId = new IloNumVar[game.getNumSequencesP2()];
            realValueBySequenceId = new int[game.getNumSequencesP2()];
        }

        primalConstraints = new TIntObjectHashMap<IloConstraint>();
        dualConstraints = new TIntObjectHashMap<IloRange>();
        nodeNatureProbabilities = new double[game.getNumNodes()+1]; // Use +1 to be robust for non-zero indexed nodes
        sequenceIdForNodeP1 = new int[game.getNumNodes()+1];
        sequenceIdForNodeP2 = new int[game.getNumNodes()+1];

    }

    /**
     * Tries to solve the current model. Currently relies on CPLEX to throw an exception if no model has been built.
     */
    @Override
    public void solveGame() {
        try {
        	System.out.println(" Length " + strategyVarsBySequenceId.length );
        	
            if (cplex.solve()) {
                for (int i = 0; i < strategyVarsBySequenceId.length; i++) {
                // System.out.println("Sequence = " + strategyVarsBySequenceId[i]);
                  IloNumVar v = strategyVarsBySequenceId[i];
                 // if(v != null)
                  //System.out.println("Cplex val : " + cplex.getValue(v));
                  }
                //for(int i = 0; i < 16; i++)
               // System.out.println("Test y1 : " + cplex.getValue(dualVars[i]));
                valueOfGame = cplex.getObjValue();
                System.out.println("Game val : " + valueOfGame);
            }
        } catch (IloException e) {
            e.printStackTrace();
            System.out.println("Error SequenceFormLPSolver::solveGame: solve exception");
        }
    }

    /**
     * Creates and returns a mapping from variable names to the values they take on in the solution computed by CPLEX.
     */
    public TObjectDoubleMap<String> getStrategyVarMap() {
        TObjectDoubleMap<String> map = new TObjectDoubleHashMap<String>();
        for (IloNumVar v : strategyVarsBySequenceId) {
            try {
                map.put(v.getName(), cplex.getValue(v));
            } catch (UnknownObjectException e) {
                e.printStackTrace();
            } catch (IloException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    /**
     * Creates and returns a mapping from information set id and action name pairs to the probability of taking that action in the computed solution
     */
    @SuppressWarnings("unchecked")
    public TObjectDoubleMap<String>[] getInformationSetActionProbabilities() {
        TObjectDoubleMap<String>[] map = new TObjectDoubleHashMap[numPrimalInformationSets];
        for (int informationSetId = 0; informationSetId < numPrimalInformationSets; informationSetId++) {
            map[informationSetId] = new TObjectDoubleHashMap();
            double sum = 0;
            for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
                try {
                    sum += cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName));
                } catch (IloException e) {
                    e.printStackTrace();
                }
            }
            for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
                try {
                    if (sum > 0) {
                        map[informationSetId].put(actionName, cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName)) / sum);
                    } else {
                        map[informationSetId].put(actionName, 0);
                    }
                } catch (IloException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    /**
     * Creates and returns a mapping from information set id and action name pairs to the probability of taking that action in the computed solution
     */
    public TIntDoubleMap[] getInformationSetActionProbabilitiesByActionId() {
        TIntDoubleMap[] map = new TIntDoubleHashMap[numPrimalInformationSets];
        for (int informationSetId = 0; informationSetId < numPrimalInformationSets; informationSetId++) {
            map[informationSetId] = new TIntDoubleHashMap();
            double sum = 0;
            for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
                try {
                    sum += cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName));
                } catch (IloException e) {
                    e.printStackTrace();
                }
            }
            for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId); actionId++) {
                String actionName = game.getActionsAtInformationSet(playerToSolveFor, informationSetId)[actionId].getName();
                try {
                    if (sum > 0) {
                        map[informationSetId].put(actionId, cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName)) / sum);
                    } else {
                        map[informationSetId].put(actionId, 0);
                    }
                } catch (IloException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }


    /**
     * Prints the value of the game along with the names and computed values for each variable.
     */
    @Override
    public void printStrategyVarsAndGameValue() {
        printGameValue();
        for (IloNumVar v : strategyVarsBySequenceId) {
            try {
            	if(null != v)
            		System.out.println(v.getName() + ": \t" + cplex.getValue(v));
            } catch (UnknownObjectException e) {
                e.printStackTrace();
            } catch (IloException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Prints the value of the game, as computed by CPLEX. If solve() has not been called, an exception will be thrown.
     */
    @Override
    public void printGameValue() {
        try {
            System.out.println("Solve status: " + cplex.getStatus());
            if	(cplex.getStatus() == IloCplex.Status.Optimal) {
                System.out.println("Objective value: " + this.valueOfGame);
            }
        } catch (IloException e) {
            e.printStackTrace();
        }

    }

    /**
     * Writes the computed strategy to a file. An exception is thrown if solve() has not been called.
     * @param filename the absolute path to the file being written to
     */
    public void writeStrategyToFile(String filename) throws IloException{
        try {
            FileWriter fw = new FileWriter(filename);
            for (IloNumVar v : strategyVarsBySequenceId) {
            	if(v != null)
            		fw.write(v.getName() + ": \t" + cplex.getValue(v) + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the current model to a file. CPLEX throws an exception if the model is faulty or the path does not exist.
     * @param filename the absolute path to the file being written to
     */
    public void writeModelToFile(String filename) {
        try {
            cplex.exportModel(filename);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the parameters of CPLEX such that minimal output is produced.
     */
    private void setCplexParameters(double tol) {
        try {
            cplex.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Barrier);
            cplex.setParam(IloCplex.DoubleParam.EpOpt, tol);
            cplex.setParam(IloCplex.DoubleParam.BarEpComp, tol);
            cplex.setParam(IloCplex.IntParam.BarCrossAlg, -1);
//            cplex.setParam(IloCplex.IntParam.SimDisplay, 0);
//            cplex.setParam(IloCplex.IntParam.MIPDisplay, 0);
//            cplex.setParam(IloCplex.IntParam.MIPInterval, -1);
//            cplex.setParam(IloCplex.IntParam.TuningDisplay, 0);
//            cplex.setParam(IloCplex.IntParam.BarDisplay, 0);
//            cplex.setParam(IloCplex.IntParam.SiftDisplay, 0);
//            cplex.setParam(IloCplex.IntParam.ConflictDisplay, 0);
//            cplex.setParam(IloCplex.IntParam.NetDisplay, 0);
            cplex.setParam(IloCplex.DoubleParam.TiLim, 1e+75);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds the LP model based on the game instance.
     * @throws IloException
     */
    private void setUpModel(double tol) throws IloException {
        setCplexParameters(tol);

        objective = cplex.linearNumExpr();
        // The empty sequence is the 0'th sequence for each player
        numSequencesP1 = numSequencesP2 = 1;
        primalSequenceNames[0] = "root";
        dualSequenceNames[0] = "root";
        CreateSequenceFormIds(game.getRoot(), new TIntHashSet(), new TIntHashSet());
        assert(numSequencesP1 == game.getNumSequencesP1()); // Ensure that our recursive function agrees with the game reader on how many sequences there are
        assert(numSequencesP2 == game.getNumSequencesP2());

        // create root sequence var
        IloNumVar rootSequence = cplex.numVar(1, 1, "I_root");
        strategyVarsBySequenceId[0] = rootSequence;
        
        
        CreateSequenceFormVariablesAndConstraints(game.getRoot(), rootSequence, new TIntHashSet(),1);

        CreateDualVariablesAndConstraints();


        SetObjective();
    }

    /**
     * Recursive function that traverses the game tree, assigning Id values, starting at 1 due to the empty sequence, to sequences in pre-order. Sequence IDs are only assigned if an information set has not previously been visited
     * @param currentNodeId id into the game.nodes array
     * @param visitedP1 an integer set indicating which information sets have already been visited for Player 1
     * @param visitedP2 an integer set indicating which information sets have already been visited for Player 2
     */
    private void CreateSequenceFormIds(int currentNodeId, TIntSet visitedP1, TIntSet visitedP2) {
    	//System.out.println(currentNodeId);
        Node node = game.getNodeById(currentNodeId);
        if (node.isLeaf()) return;

        for (Action action : node.getActions()) {
        	//System.out.println(action +" p :" + node.getPlayer());
            if (node.getPlayer() == 1 && !visitedP1.contains(node.getInformationSet())) {
                sequenceIdByInformationSetAndActionP1[node.getInformationSet()].put(action.getName(), numSequencesP1++);
               // System.out.println("Sequence player 1: " + Integer.toString(node.getInformationSet()) + ";" + action.getName());
                if (playerToSolveFor ==1) primalSequenceNames[numSequencesP1-1] = Integer.toString(node.getInformationSet()) + ";" + action.getName();
                else dualSequenceNames[numSequencesP1-1] = Integer.toString(node.getInformationSet()) + ";" + action.getName();
            } else if (node.getPlayer() == 2 && !visitedP2.contains(node.getInformationSet())) {
            	//System.out.println("Sequence player 2: " + Integer.toString(node.getInformationSet()) + ";" + action.getName());
                sequenceIdByInformationSetAndActionP2[node.getInformationSet()].put(action.getName(), numSequencesP2++);
               if (playerToSolveFor == 2) primalSequenceNames[numSequencesP2-1] = Integer.toString(node.getInformationSet()) + ";" + action.getName();
               else dualSequenceNames[numSequencesP2-1] = Integer.toString(node.getInformationSet()) + ";" + action.getName();
            }
            	CreateSequenceFormIds(action.getChildId(), visitedP1, visitedP2);
        }
        if (node.getPlayer() == 1) {
        	//System.out.println("Info set : " + node.getInformationSet());
            visitedP1.add(node.getInformationSet());
        } else if (node.getPlayer() == 2) {
            visitedP2.add(node.getInformationSet());
        }
    }

    /**
     * Creates sequence form variables in pre-order traversal. A constraint is also added to ensure that the probability sum over the new sequences sum to the value of the last seen sequence on the path to this information set
     * @param currentNodeId
     * @param parentSequence last seen sequence belonging to the primal player
     * @param visited keeps track of which information sets have been visited
     * @throws IloException
     */
    private void CreateSequenceFormVariablesAndConstraints(int currentNodeId, IloNumVar parentSequence, TIntSet visited, double natureProbability) throws IloException{
        Node node = game.getNodeById(currentNodeId);
        if (null == node) return;
       
        if (node.isLeaf()) { // Added for test sujan
			double value = playerToSolveFor == player1 ? node.getPlayerOneValue() : node.getPlayerTwoValue();
			//System.out.println("Val :" + natureProbability * value);
			//objective.addTerm(natureProbability * value, parentSequence);
			//System.out.println("obj :" + objective);
			//System.out.println(++cnt + " val :" + value);
			return;
		}

        if (node.getPlayer() == playerToSolveFor && !visited.contains(node.getInformationSet())) {
            visited.add(node.getInformationSet()); 
            IloLinearNumExpr sum = cplex.linearNumExpr();
            //sum.addTerm(-1, parentSequence);
            for (Action action : node.getActions()) {
                // real-valued variable in (0,1)
                IloNumVar v = cplex.numVar(0, 1, "I:" + node.getInformationSet() + "  action:" + action.getName());
                
                strategyVarsByInformationSet[node.getInformationSet()].put(action.getName(), v);
                int sequenceId = getSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName());
                int val = node.getInformationSet() >> 2;
                cplex.output().println("SequenceID = " + node.getInformationSet() + "val " + val +  "Prob :"  + natureProbability);
                strategyVarsBySequenceId[sequenceId] = v;
                realValueBySequenceId[sequenceId] = node.getInformationSet();
                // add 1*v to the sum over all the sequences at the information set
                sum.addTerm(1, v);
               // System.out.println("Child val" + game.getNodeById(action.getChildId()).getValue());
                CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited, natureProbability);
            }
            // sum_{sequences} = parent_sequence. cplex.addEq returns a reference to the range object describing the constraint. This is useful for dynamically modifying the model in derived classes.
             // System.out.println("parentSequence :" + parentSequence + " sum : " + sum);
            
            // ******************* Set constraints for defender **************************************
           // parentSequence.setLB(natureProbability);
            //parentSequence.setUB(natureProbability);
            primalConstraints.put(node.getInformationSet(), cplex.addEq(sum, parentSequence,"Primal"+node.getInformationSet()));
          // primalConstraints.put(node.getInformationSet(), cplex.addEq(sum, 1));
        } else {
			for (Action action : node.getActions()) {

				if (null != action) {
					double newNatureProbability = node.getPlayer() == 0 ? natureProbability * action.getProbability()
							: natureProbability;
					if (node.getPlayer() == playerToSolveFor) {
						// update parentSequence to be the current sequence
						IloNumVar v = strategyVarsByInformationSet[node.getInformationSet()].get(action.getName());
						if (null != action) {
							
							CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited,
									newNatureProbability);
						}
					} else {
						if (null != action) {
							if(node.getPlayer() == playerNotToSolveFor) {
								//double realVal = node.getPlayerTwoValue();
								//parentSequence = cplex.numVar(0, 1, "I:" + node.getInformationSet() + "  action:" + action.getName());
                                //System.out.println("Real val :" + parentSequence);
        						//objective.addTerm(natureProbability * realVal, parentSequence);
        						//System.out.println(objective);
							}
							CreateSequenceFormVariablesAndConstraints(action.getChildId(), parentSequence, visited,
									newNatureProbability);}
					}
				}
            }
        }
    }


    private void CreateDualVariablesAndConstraints() throws IloException {
        int numVars = 0;
        if (playerToSolveFor == 1) {
            numVars = game.getNumInformationSetsPlayer2() + 1;
        } else {
            numVars = game.getNumInformationSetsPlayer1() + 1;
        }
        String[] names = new String[numVars];
        for (int i = 0; i < numVars; i++) { names[i] = "Y" + i;}
          //this.dualVars = cplex.numVarArray(numVars, -Double.MAX_VALUE, Double.MAX_VALUE, names);
          this.dualVars = cplex.numVarArray(numVars, -Double.MAX_VALUE , Utils.PLAYER_ONE_MAX_VAL, names);

        InitializeDualSequenceMatrix();
        InitializeDualPayoffMatrix();
        /*
        for(int k =1; k  < numVars; k++) {
        	IloLinearNumExpr VI = cplex.linearNumExpr();
        	VI.addTerm(1, dualVars[k]);
        	objective.add(VI);
        	System.out.println("Obj " + objective );
        }*/
        for (int sequenceId = 0; sequenceId < numDualSequences; sequenceId++) {
        	
           CreateDualConstraintForSequence(sequenceId);
        }
    }

    private void InitializeDualSequenceMatrix() throws IloException {
        sequenceFormDualMatrix[0].add(0);
        InitializeDualSequenceMatrixRecursive(game.getRoot(), new TIntHashSet(), 0);
    }

    private void InitializeDualSequenceMatrixRecursive(int currentNodeId, TIntSet visited, int parentSequenceId) throws IloException {
        Node node = this.game.getNodeById(currentNodeId);
        if (null == node || node.isLeaf()) return;

        if (playerNotToSolveFor == node.getPlayer() && !visited.contains(node.getInformationSet())) {
            visited.add(node.getInformationSet());
            int informationSetMatrixId = node.getInformationSet() + (1-game.getSmallestInformationSetId(playerNotToSolveFor)); // map information set ID to 1 indexing. Assumes that information sets are named by consecutive integers
            //System.out.println("informationSetMatrixId" + informationSetMatrixId);
            sequenceFormDualMatrix[parentSequenceId].add(informationSetMatrixId);
            for (Action action : node.getActions()) {
				if (null != action) {
					int newSequenceId = getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName());
					sequenceFormDualMatrix[newSequenceId].add(informationSetMatrixId);
					InitializeDualSequenceMatrixRecursive(action.getChildId(), visited, newSequenceId);
				}
            }
        } else {
            for (Action action : node.getActions()) {
            	
				if (null != action) {
					int newSequenceId = playerNotToSolveFor == node.getPlayer()
							? getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName())
							: parentSequenceId;

					InitializeDualSequenceMatrixRecursive(action.getChildId(), visited, newSequenceId);
				}
            }
        }

    }

    private void InitializeDualPayoffMatrix() throws IloException {
       InitializeDualPayoffMatrixRecursive(game.getRoot(), 0, 0, 1);     // Start with the root sequences
      //  InitializePrimalPayoffMatrixRecursive(game.getRoot(), 0, 0, 1);     // Start with the root sequences
    }

    private void InitializeDualPayoffMatrixRecursive(int currentNodeId,int primalSequence, int dualSequence, double natureProbability) throws IloException{
        Node node = this.game.getNodeById(currentNodeId);
        if(null == node)
        	return;
        
       // System.out.println(node.getName()+"Infoset :" +node.getInformationSet() );

        if (node.isLeaf()) {
            double valueMultiplier = playerToSolveFor == 1? node.getPlayerOneValue() : node.getPlayerTwoValue();
            //System.out.println("Current node : " + currentNodeId + "Primal seq : " + primalSequence + " Dual seq " + dualSequence);
            double leafValue = valueMultiplier;
           // System.out.println("leaf node val :" + node.getValue());
            if (dualPayoffMatrix[dualSequence].containsKey(primalSequence)) {
            	//System.out.println("A:" +primalSequence  );
                dualPayoffMatrix[dualSequence].put(primalSequence, leafValue + dualPayoffMatrix[dualSequence].get(primalSequence));
            } else {
            	//System.out.println(primalSequence );
                dualPayoffMatrix[dualSequence].put(primalSequence, leafValue);
            }
        } else {
            for (Action action : node.getActions()) {
				if (action != null) {
					int newPrimalSequence = node.getPlayer() == playerToSolveFor
							? getSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName())
							: primalSequence;
					int newDualSequence = node.getPlayer() == playerNotToSolveFor
							? getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName())
							: dualSequence;
					double newNatureProbability = node.getPlayer() == 0 ? natureProbability * action.getProbability()
							: natureProbability;
					InitializeDualPayoffMatrixRecursive(action.getChildId(),newPrimalSequence, newDualSequence,
							newNatureProbability);
				}
            }
        }
    }
    

    private void CreateDualConstraintForSequence(int sequenceId) throws IloException{
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        //System.out.println("Seq id " + sequenceId);
        for (int i = 0; i < sequenceFormDualMatrix[sequenceId].size(); i++) {
            int informationSetId = sequenceFormDualMatrix[sequenceId].get(i);// + (1-game.getSmallestInformationSetId(playerNotToSolveFor)); // map information set ID to 1 indexing. Assumes that information sets are named by consecutive integers
            int valueMultiplier = i == 0? 1 : -1;
            //System.out.println("seq id : "+ sequenceId+ " Val mul : " + valueMultiplier + " Dual var " + dualVars[informationSetId]);
            //System.out.println("Test");
            lhs.addTerm(valueMultiplier, dualVars[informationSetId]);
           // System.out.println(lhs);
            
            objective.add(lhs);
        }

        //IloLinearNumExpr RCS = cplex.linearNumExpr();
        //System.out.println("Size :" + dualPayoffMatrix[sequenceId].size() );
        TIntDoubleIterator it = dualPayoffMatrix[sequenceId].iterator();
        for ( int i = dualPayoffMatrix[sequenceId].size(); i-- > 0; ) {
            it.advance();
            //System.out.println("Val mul : " + -it.value() + " Dual var " + strategyVarsBySequenceId[it.key()]);
            lhs.addTerm(-it.value(), strategyVarsBySequenceId[it.key()]);
        }
         
        //System.out.println("exp for dual : " + lhs);
        dualConstraints.put(sequenceId, cplex.addLe(lhs, 0, "Dual"+sequenceId));
    }

    /**
     * Fills in the convenience arrays nodeNatureProbabilities and sequenceIdForNodeP1/2
     */
    void computeAuxiliaryInformationForNodes() { 
        computeAuxiliaryInformationForNodesRecursive(game.getRoot(), 0, 0, 1);
    }
    private void computeAuxiliaryInformationForNodesRecursive(int currentNodeId, int sequenceIdP1, int sequenceIdP2, double natureProbability) {
        Node node = this.game.getNodeById(currentNodeId);

        nodeNatureProbabilities[node.getNodeId()] = natureProbability;
        sequenceIdForNodeP1[currentNodeId] = sequenceIdP1;
        sequenceIdForNodeP2[currentNodeId] = sequenceIdP2;
        if (node.isLeaf()) return;

        for (Action action : node.getActions()) {
            int newSequenceIdP1= node.getPlayer() == 1? sequenceIdByInformationSetAndActionP1[node.getInformationSet()].get(action.getName()) : sequenceIdP1;
            int newSequenceIdP2= node.getPlayer() == 2? sequenceIdByInformationSetAndActionP2[node.getInformationSet()].get(action.getName()) : sequenceIdP2;
            double newNatureProbability = node.getPlayer() == 0? natureProbability * action.getProbability() : natureProbability;
            computeAuxiliaryInformationForNodesRecursive(action.getChildId(), newSequenceIdP1, newSequenceIdP2, newNatureProbability);
        }
    }

    int getSequenceIdForPlayerToSolveFor(int informationSet, String actionName) {
        if (playerToSolveFor == 1) {
            return sequenceIdByInformationSetAndActionP1[informationSet].get(actionName);
        } else {
            return sequenceIdByInformationSetAndActionP2[informationSet].get(actionName);
        }
    }

    int getSequenceIdForPlayerNotToSolveFor(int informationSet, String actionName) {
        if (playerNotToSolveFor == 1) {
            return sequenceIdByInformationSetAndActionP1[informationSet].get(actionName);
        } else {
            return sequenceIdByInformationSetAndActionP2[informationSet].get(actionName);
        }
    }


    private void SetObjective() throws IloException {
        cplex.addMaximize(cplex.prod(1, dualVars[0]));
        //System.out.println("Object :" + objective);
    	//cplex.addMaximize(objective);
    }


    public int getPlayerToSolveFor() {
        return playerToSolveFor;
    }

    public int getPlayerNotToSolveFor() {
        return playerNotToSolveFor;
    }

    public IloCplex getCplex() {
        return cplex;
    }

    public IloNumVar[] getDualVars() {
        return dualVars;
    }

    public HashMap<String, IloNumVar>[] getStrategyVarsByInformationSet() {
        return strategyVarsByInformationSet;
    }

    public TIntList[] getSequenceFormDualMatrix() {
        return sequenceFormDualMatrix;
    }

    public TIntDoubleMap[] getDualPayoffMatrix() {
        return dualPayoffMatrix;
    }

    public TObjectIntMap<String>[] getSequenceIdByInformationSetAndActionP1() {
        return sequenceIdByInformationSetAndActionP1;
    }

    public TObjectIntMap<String>[] getSequenceIdByInformationSetAndActionP2() {
        return sequenceIdByInformationSetAndActionP2;
    }

    public IloNumVar[] getStrategyVarsBySequenceId() {
        return strategyVarsBySequenceId;
    }

    public int getNumSequencesP1() {
        return numSequencesP1;
    }

    public int getNumSequencesP2() {
        return numSequencesP2;
    }

    public int getNumPrimalSequences() {
        return numPrimalSequences;
    }

    public int getNumDualSequences() {
        return numDualSequences;
    }

    public TIntObjectMap<IloConstraint> getPrimalConstraints() {
        return primalConstraints;
    }

    public TIntObjectMap<IloRange> getDualConstraints() {
        return dualConstraints;
    }

    @Override
    public double[][][] getStrategyProfile() {
        double[][][] profile = new double[3][][];
        profile[playerToSolveFor] = new double [numPrimalInformationSets][];
        //player two has 15 information set thats why minus 2 added
        for (int informationSetId = 0; informationSetId < numPrimalInformationSets-1; informationSetId++) {
            profile[playerToSolveFor][informationSetId] = new double[game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId)];
            double sum = 0;
            for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
                try {
                //System.out.println(strategyVarsByInformationSet[informationSetId].get(actionName) + ": " + cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName)) );

                    sum += cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName));
   
                } catch (IloException e) {
                    e.printStackTrace();
                }
            }
            
            
            
           // System.out.println("Sum : " + sum);

            for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId); actionId++) {
                String actionName = game.getActionsAtInformationSet(playerToSolveFor, informationSetId)[actionId].getName();
                try {
                	//System.out.println(informationSetId + "action" + actionId);
                    if (sum > 0) {
						if (!sameInfosetAction
								.contains(strategyVarsByInformationSet[informationSetId].get(actionName))) {
							sameInfosetAction.add(strategyVarsByInformationSet[informationSetId].get(actionName));
							// System.out.println(strategyVarsByInformationSet[informationSetId].get(actionName));
							profile[playerToSolveFor][informationSetId][actionId] = cplex
									.getValue(strategyVarsByInformationSet[informationSetId].get(actionName)) / sum;
						}
                    } else {
                        profile[playerToSolveFor][informationSetId][actionId] = 1.0 / game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId);
                    }
                System.out.println(strategyVarsByInformationSet[informationSetId].get(actionName) + " :  " + profile[playerToSolveFor][informationSetId][actionId]);
                } catch (IloException e) {
                    e.printStackTrace();
                }
            }
        }
        return profile;
    }
}
