package function;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 
 * @author Christophe Debruyne
 *
 */
public class JSEnv {

	private static ScriptEngineManager manager = new ScriptEngineManager();
	private static ScriptEngine engine = manager.getEngineByName("javascript");

	/**
	 * Invoking a function with an array of parameters.
	 * 
	 * @param functionName
	 * @param parameters
	 * @return
	 * @throws NoSuchMethodException
	 * @throws ScriptException
	 */
	public static String invoke(String functionName, Object... parameters) 
			throws NoSuchMethodException, ScriptException {
		Invocable invokeEngine = (Invocable) engine;
		Object o = invokeEngine.invokeFunction(functionName, parameters);
		return o.toString();
	}

	/**
	 * Loading JavaScript code.
	 * 
	 * @param code
	 * @throws ScriptException
	 */
	public static void loadCode(String code) throws ScriptException {
		engine.eval(code);
	}

	/**
	 * A utility function for setting up a new engine and environment.
	 * 
	 */
	public static void reset() {
		manager = new ScriptEngineManager();
		manager.getEngineByName("javascript");
	}

}
