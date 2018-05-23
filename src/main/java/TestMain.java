import extensive_form_game.Game;
import extensive_form_game_solver.BestResponseLPSolver;
import extensive_form_game_solver.SequenceFormLPSolver;


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
	//equilibriumSolver.printStrategyVarsAndGameValue();

	double[][] p1Strategy = equilibriumSolver.getStrategyProfile()[2];
	
	BestResponseLPSolver brSolver = new BestResponseLPSolver(drpGame, 1, p1Strategy);
	brSolver.solveGame();
	brSolver.printStrategyVarsAndGameValue();
	//drpGame.createGameFromFileZerosumPackageFormat("prsl.txt");
	}
}