import extensive_form_efg_game_format.CreateTree;
import extensive_form_game.Game;
import extensive_form_game_solver.BestResponseLPSolver;
import extensive_form_game_solver.CounterFactualRegretSolver;
import extensive_form_game_solver.DefenderSequenceFormLPApproximationSolver;
import extensive_form_game_solver.DoubleOracleLPSolver;
import gnu.trove.map.TObjectDoubleMap;
import ilog.concert.IloException;

/**
 * @author IASRLUser
 *
 */
public class TestMain {

	public static void main(String[] args) {

		CreateTree tree = new CreateTree();
		tree.init();
		tree.closeFile();

		Game drpGame = new Game();
		drpGame.createGameFromFileZerosumPackageFormat("hsg_4_features.efg");
		
		 DefenderSequenceFormLPApproximationSolver equilibriumSolver = new DefenderSequenceFormLPApproximationSolver(drpGame, 1);
		//DoubleOracleLPSolver equilibriumSolver = new DoubleOracleLPSolver(drpGame, 1);
		//CounterFactualRegretSolver CFRSolver= new CounterFactualRegretSolver(drpGame);
		//DefenderSequenceFormLPAttackerBr equilibriumSolver = new DefenderSequenceFormLPAttackerBr(drpGame, 2);
		// AttackerSequenceFormLPSolver equilibriumSolver = new AttackerSequenceFormLPSolver(drpGame, 2);
	    //SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(drpGame, 2);
		//CFRSolver.solveGame(10);
		//double[][] p2Strategy = CFRSolver.getStrategyProfile()[1];
				
		
		
		equilibriumSolver.solveGame();
		
		try {
			equilibriumSolver.writeStrategyToFile("defenderStrategy.txt");
		} catch (IloException e) {
			e.printStackTrace();
		}
      
/*		double[][] p2Strategy = equilibriumSolver.getStrategyProfile()[1];
		
		
		BestResponseLPSolver brSolver = new BestResponseLPSolver(drpGame, 2, p2Strategy);
		brSolver.solveGame();
		System.out.println("Game value" + brSolver.getValueOfGame());
		TObjectDoubleMap<String> p2StrategyMap = brSolver.getStrategyVarMap();

		try {
			brSolver.writeStrategyToFile("attackerStrategy.txt");

		} catch (IloException e) {
			e.printStackTrace();
		}
		
	
		*/
		/*
		double[][] tmpStrategy = brSolver.getStrategyProfile()[2];
		
		BestResponseLPSolver brSolverdf = new BestResponseLPSolver(drpGame, 1, tmpStrategy);
		brSolverdf.solveGame();
		try {
			brSolverdf.writeStrategyToFile("defenderStrategy2.txt");

		} catch (IloException e) {
			e.printStackTrace();
		}*/
		
	}

}