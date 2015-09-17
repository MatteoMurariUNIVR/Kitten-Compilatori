package types;

import javaBytecodeGenerator.JavaClassGenerator;
import javaBytecodeGenerator.TestClassGenerator;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.MethodGen;

import absyn.TestDeclaration;
import translation.Block;

public class TestSignature extends CodeSignature
{	
	public TestSignature(ClassType clazz, String name, TestDeclaration abstractSyntax)
	{
		super(clazz, VoidType.INSTANCE, TypeList.EMPTY, name, abstractSyntax);
	}

	@Override
	protected Block addPrefixToCode(Block code)
	{
		return code;
	}

	/**
     * Creates a Java bytecode test and adds it the the given class with the
     * given constant pool.
     *
     * @param testGen the generator of the test class where the test lives
     */

    public void createTest(TestClassGenerator testClassGen)
    {
    	MethodGen testGen;
		testGen=new MethodGen(Constants.ACC_PRIVATE | Constants.ACC_STATIC, // private and static
							  org.apache.bcel.generic.Type.INT, // return type
							  new org.apache.bcel.generic.Type[]{getDefiningClass().toBCEL()}, // parameters types, if any
							  null, // parameters names: we do not care
							  getName(), // test's name
							  testClassGen.getClassName(), // defining class
							  testClassGen.generateJavaBytecode(getCode()), // bytecode of the test
							  testClassGen.getConstantPool()); // constant pool

		// we must always call these tests before the getTest()
		// test below. They set the number of local variables and stack
		// elements used by the code of the method
		testGen.setMaxStack();
		testGen.setMaxLocals();

		// we add a test to the class that we are generating
		testClassGen.addMethod(testGen.getMethod());
    }

	public INVOKESTATIC createINVOKESTATIC(JavaClassGenerator classGen)
	{
		return (INVOKESTATIC) createInvokeInstruction(classGen, Constants.INVOKESTATIC);
	}
	
}
