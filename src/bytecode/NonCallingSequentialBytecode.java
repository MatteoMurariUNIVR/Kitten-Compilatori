package bytecode;

/**
 * A sequential bytecode of the intermediate Kitten language that does not call
 * any piece of code.
 *
 * @author <A HREF="mailto:fausto.spoto@univr.it">Fausto Spoto</A>
 */


public abstract class NonCallingSequentialBytecode extends SequentialBytecode implements NonCallingBytecode {

	/**
	 * Constructs a bytecode that does not call any piece of code.
	 */

	protected NonCallingSequentialBytecode() {
	}
}