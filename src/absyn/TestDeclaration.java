package absyn;

import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import bytecode.CONST;
import bytecode.NEWSTRING;
import bytecode.RETURN;
import bytecode.VIRTUALCALL;

import semantical.TypeChecker;
import translation.Block;
import types.*;

public class TestDeclaration extends CodeDeclaration
{
	private String name;
	
	/**
	 * Constructs the abstract syntax of a test declaration.
	 *
	 * @param pos the starting position in the source file of
	 *            the concrete syntax represented by this abstract syntax
	 * @param body the abstract syntax of the body of the constructor
	 * @param next the abstract syntax of the declaration of the
	 *             subsequent class member, if any.
	 */

	public TestDeclaration(int pos, String name, Command body, ClassMemberDeclaration next)
	{
		super(pos, null, body, next);
		this.name=name;
	}
	
	/**
	 * Yields the signature of this test declaration.
	 *
	 * @return the signature of this test declaration.
	 *         Yields {@code null} if type-checking has not been performed yet
	 */

	@Override
	public TestSignature getSignature()
	{
		return (TestSignature) super.getSignature();
	}
	
	/**
	 * Adds arcs between the dot node for this piece of abstract syntax
	 * and those representing the formal parameters and body of the test.
	 *
	 * @param where the file where the dot representation must be written
	 */

	protected void toDotAux(FileWriter where) throws java.io.IOException
	{
		linkToNode("body", getBody().toDot(where), where);
	}
	
	/**
	 * Adds the signature of this test declaration to the given class.
	 *
	 * @param clazz the class where the signature of this constructor
	 *              declaration must be added
	 */

	@Override
	protected void addTo(ClassType clazz)
	{
		TestSignature tSig=new TestSignature(clazz, name, this);
		clazz.addTest(tSig);

		// we record the signature of this test inside this abstract syntax
		setSignature(tSig);
	}
	
	/**
	 * Type-checks this test declaration. Namely, it builds a type-checker
	 * whose only variable in scope is {@code this} of the defining class of the
	 * test, and where only return instructions of type {@code void} are allowed.
	 * It then type-checks the body of the constructor in that type-checker
	 * and checks that it does not contain any dead-code.
	 *
	 * @param clazz the semantical type of the class where this constructor occurs.
	 */

	@Override
	protected void typeCheckAux(ClassType clazz)
	{
		TypeChecker checker = new TypeChecker(VoidType.INSTANCE, clazz.getErrorMsg(), true);
		checker = checker.putVar("this", clazz);

		// we type-check the body of the test in the resulting type-checker.
		getBody().typeCheck(checker);

		// we check that there is no dead-code in the body of the test.
		if(getBody().checkForDeadcode())
			error(checker, "There are some unreachable parts of code.");
	}

	public void translate(Set<ClassMemberSignature> done)
	{
		if(done.add(getSignature()))
		{
			Block post = new Block(new RETURN(IntType.INSTANCE));
			post = new CONST(0).followedBy(post);
			post = new VIRTUALCALL(ClassType.mkFromFileName("String.kit"), ClassType.mkFromFileName("String.kit").methodLookup("output", TypeList.EMPTY)).followedBy(post);
			post = new NEWSTRING("passed").followedBy(post);	
			getSignature().setCode(getBody().translate(getSignature(), post));
			translateReferenced(getSignature().getCode(), done, new HashSet<Block>());
		}
	}
}
