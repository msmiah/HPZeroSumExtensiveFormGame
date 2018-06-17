import extensive_form_efg_game_format.CreateTree;
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

		CreateTree tree = new CreateTree();
		tree.init();
		tree.closeFile();
		
		Game drpGame = new Game();
		drpGame.createGameFromFileZerosumPackageFormat("hsg_4_features.efg");
		SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(drpGame, 1);
		equilibriumSolver.solveGame();
		try {
			equilibriumSolver.writeStrategyToFile("defenderStrategy.txt");
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// equilibriumSolver.printStrategyVarsAndGameValue();

		double[][] p2Strategy = equilibriumSolver.getStrategyProfile()[1];

		BestResponseLPSolver brSolver = new BestResponseLPSolver(drpGame, 2, p2Strategy);
		brSolver.solveGame();

		// double[][] p1Strategy = brSolver.getStrategyProfile()[1];

		// BestResponseLPSolver brSolver2 = new BestResponseLPSolver(drpGame, 2,
		// p1Strategy);
		// brSolver2.solveGame();
		// double[][] p1Strategy = brSolver.getStrategyProfile();
		// brSolver.wr
		// brSolver.printStrategyVarsAndGameValue();
		try {
			brSolver.writeStrategyToFile("attackerStrategy.txt");

		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}