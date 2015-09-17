package absyn;

import java.io.FileWriter;
import java.util.StringTokenizer;

import bytecode.CONST;
import bytecode.NEWSTRING;
import bytecode.RETURN;
import bytecode.VIRTUALCALL;

import semantical.TypeChecker;
import translation.Block;
import types.ClassType;
import types.CodeSignature;
import types.IntType;
import types.MethodSignature;
import types.TypeList;

public class Assert extends Command
{
	/**
	 * The guard or condition of the conditional.
	 */

	private final Expression condition;
	private String errorMsg;
	
	public Assert(int pos, Expression condition)
	{
		super(pos);
		this.condition=condition;
	}
	
	/**
	 * Yields the abstract syntax of the condition.
	 *
	 * @return the abstract syntax of the condition
	 */

	public Expression getCondition()
	{
		return condition;
	}
	
	/**
	 * Adds abstract syntax class-specific information in the dot file
	 * representing the abstract syntax of the {@code assert} command.
	 * This amounts to adding an arc from the node for the {@code assert}
	 * command to the abstract syntax for {@link #returned}, if any.
	 *
	 * @param where the file where the dot representation must be written
	 */

	@Override
	protected void toDotAux(FileWriter where) throws java.io.IOException
	{
		//if(condition!=null)
			linkToNode("assertion", condition.toDot(where), where);
	}
	
	/**
	 * Performs the type-checking of the assert command
	 * by using a given type-checker. It type-checks the condition.
	 * It checks that the condition is a Boolean expression.
	 *
	 * @param checker the type-checker to be used for type-checking
	 * @return the type-checker {@code checker}
	 */

	@Override
	protected TypeChecker typeCheckAux(TypeChecker checker)
	{
		if(!checker.getAssertPermission())
			error(checker, "Assert permitted only in class member 'TEST'.");
		
		condition.mustBeBoolean(checker);

		errorMsg="Test fallito @" + error(checker);
		StringTokenizer st=new StringTokenizer(errorMsg, "->");
		st.nextToken();
		errorMsg="failed at " + st.nextToken();
		
		return checker;
		
	}

	/**
	 * Checks that this conditional does not contain <i>dead-code</i>, that is,
	 * commands which can never be executed.
	 *
	 * @return true if and only if every execution path in both branches of the
	 *         conditional ends with a {@code assert} command
	 */

	@Override
	public boolean checkForDeadcode()
	{
		return false;
	}

	@Override
	public Block translate(CodeSignature where, Block continuation)
	{	
		NEWSTRING errorString=new NEWSTRING(errorMsg);
		Block fail=new Block(where);
		ClassType stringClass=ClassType.mkFromFileName("String.kit");
		MethodSignature outputMethod=stringClass.methodLookup("output", TypeList.EMPTY);
		
		fail=fail.prefixedBy(new RETURN(IntType.INSTANCE));
		fail=fail.prefixedBy(new CONST(1));
		fail=fail.prefixedBy(new VIRTUALCALL(stringClass, outputMethod));
		fail=fail.prefixedBy(errorString);
		
		return condition.translateAsTest(where, continuation, fail);
	}
}
