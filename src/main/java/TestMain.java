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
	drpGame.createGameFromFileZerosumPackageFormat("hsg_4_features.txt");
	SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(drpGame, 1);
	equilibriumSolver.solveGame();

	double[][] p1Strategy = equilibriumSolver.getStrategyProfile()[1];
	
	//BestResponseLPSolver brSolver = new BestResponseLPSolver(drpGame, 2, p1Strategy);
	//brSolver.solveGame();
	
	//drpGame.createGameFromFileZerosumPackageFormat("prsl.txt");
	}
}