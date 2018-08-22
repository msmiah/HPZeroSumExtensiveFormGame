import extensive_form_efg_game_format.CreateTree;
import extensive_form_game.Game;
import extensive_form_game_solver.AttackerSequenceFormLPSolver;
import extensive_form_game_solver.BestResponseLPSolver;
import extensive_form_game_solver.DefenderSequenceFormLPSolver;
import extensive_form_game_solver.DefenderSequenceFormLPSolverBak2;
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
		DefenderSequenceFormLPSolverBak2 equilibriumSolver = new DefenderSequenceFormLPSolverBak2(drpGame, 1);
		// AttackerSequenceFormLPSolver equilibriumSolver = new AttackerSequenceFormLPSolver(drpGame, 2);
	  //SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(drpGame, 2);
		equilibriumSolver.solveGame();
		
		try {
			equilibriumSolver.writeStrategyToFile("defenderStrategy.txt");
		} catch (IloException e) {
			e.printStackTrace();
		}
      /**
		double[][] p2Strategy = equilibriumSolver.getStrategyProfile()[2];

		BestResponseLPSolver brSolver = new BestResponseLPSolver(drpGame, 1, p2Strategy);
		brSolver.solveGame();

		try {
			brSolver.writeStrategyToFile("attackerStrategy.txt");

		} catch (IloException e) {
			e.printStackTrace();
		}
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