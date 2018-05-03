import extensive_form_game.Game;


/**
 * @author IASRLUser
 *
 */
public class TestMain {

	public static void main(String[] args) {
	Game drpGame = new Game();
	drpGame.createGameFromFileZerosumPackageFormat("hsg_4_features.txt");
	//drpGame.createGameFromFileZerosumPackageFormat("prsl.txt");
	}
}