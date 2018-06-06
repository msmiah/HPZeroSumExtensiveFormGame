import extensive_form_game.Game;
import extensive_form_game_solver.BestResponseLPSolver;
import extensive_form_game_solver.SequenceFormLPSolver;
import extensive_form_game_solver.SequenceFormLPSolverTwo;
import ilog.concert.IloException;


/**
 * @author IASRLUser
 *
 */
public class TestMain {

	public static void main(String[] args) {
	Game drpGame = new Game();
	drpGame.createGameFromFileZerosumPackageFormat("hsg_4_features.efg");
     SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(drpGame, 2);
	 equilibriumSolver.solveGame();
	/*try {
		equilibriumSolver.writeStrategyToFile("attackerStrategy.txt");
	} catch (IloException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}*/
	//equilibriumSolver.printStrategyVarsAndGameValue();
    
	double[][] p2Strategy = equilibriumSolver.getStrategyProfile()[2];
	
	BestResponseLPSolver brSolver = new BestResponseLPSolver(drpGame, 1, p2Strategy);
	brSolver.solveGame();
	
	// double[][] p1Strategy = brSolver.getStrategyProfile()[1];
	 
	//BestResponseLPSolver brSolver2 = new BestResponseLPSolver(drpGame, 2, p1Strategy);
	//brSolver2.solveGame();
	//double[][] p1Strategy = brSolver.getStrategyProfile();
	//brSolver.wr
	//brSolver.printStrategyVarsAndGameValue();
	try {
		brSolver.writeStrategyToFile("defenderStrategy.txt");
	} catch (IloException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	
}