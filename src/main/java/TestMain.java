import extensive_form_efg_game_format.CreateTree;
import extensive_form_game.Game;
import extensive_form_game_solver.AttackerSequenceFormLPSolver;
import extensive_form_game_solver.BestResponseLPSolver;
import extensive_form_game_solver.DefenderSequenceFormLPSolver;
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
		DefenderSequenceFormLPSolver equilibriumSolver = new DefenderSequenceFormLPSolver(drpGame, 1);
		// AttackerSequenceFormLPSolver equilibriumSolver = new AttackerSequenceFormLPSolver(drpGame, 2);
	  //SequenceFormLPSolverTwo equilibriumSolver = new SequenceFormLPSolverTwo(drpGame, 1);
		equilibriumSolver.solveGame();
		
		try {
			equilibriumSolver.writeStrategyToFile("defenderStrategy.txt");
		} catch (IloException e) {
			e.printStackTrace();
		}
      
		double[][] p2Strategy = equilibriumSolver.getStrategyProfile()[1];

		BestResponseLPSolver brSolver = new BestResponseLPSolver(drpGame, 2, p2Strategy);
		brSolver.solveGame();

		try {
			brSolver.writeStrategyToFile("attackerStrategy.txt");

		} catch (IloException e) {
			e.printStackTrace();
		}
	}

}