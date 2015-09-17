package javaBytecodeGenerator;

import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import types.ClassMemberSignature;
import types.ClassType;
import types.FixtureSignature;
import types.TestSignature;

/**
 * A Java bytecode generator. It transforms the Kitten intermediate language
 * into Java bytecode that can be dumped to Java class files and run.
 * It uses the BCEL library to represent Java classes and dump them on the file-system.
 *
 * @author <A HREF="mailto:fausto.spoto@univr.it">Fausto Spoto</A>
 */

@SuppressWarnings("serial")
public class TestClassGenerator extends JavaClassGenerator {

	private ClassType targetClass;
	
	/**
	 * Builds a class generator for the given class type.
	 *
	 * @param clazz the class type
	 * @param sigs a set of class member signatures. These are those that must be
	 *             translated. If this is {@code null}, all class members are translated
	 */

	public TestClassGenerator(ClassType clazz, Set<ClassMemberSignature> sigs)
	{
		super(clazz, sigs, "Test");
		targetClass=clazz;		

		// we add the fixtures
		for(FixtureSignature fixture : clazz.getFixtures())
			if(sigs==null || sigs.contains(fixture))
				fixture.createFixture(this);
		
		//we add the tests
		for(TestSignature test : clazz.getTests())
			if(sigs==null || sigs.contains(test))
				test.createTest(this);
		
		InstructionList instructionsForMain=mainGenerator();
		
		MethodGen methodGen=new MethodGen
				(Constants.ACC_PUBLIC | Constants.ACC_STATIC, // public and static
				org.apache.bcel.generic.Type.VOID, // return type
				new org.apache.bcel.generic.Type[]{ new org.apache.bcel.generic.ArrayType("java.lang.String", 1) },
				null, // parameters names: we do not care
				"main", // method's name
				getClassName(), // defining class
				instructionsForMain, //bytecode of the method
				getConstantPool()); // constant pool
		
		methodGen.setMaxStack();
		methodGen.setMaxLocals();

		addMethod(methodGen.getMethod());
	}
	
	public InstructionList mainGenerator()
	{
		InstructionList main=new InstructionList();
		
		main.append(getFactory().createPrintln("Test execution for class " + targetClass.getName() + ":")); 
		main.append(InstructionFactory.ICONST_0);
		main.append(InstructionFactory.ISTORE_1);
		
		for(TestSignature test : targetClass.getTests())
		{
			InstructionList testInstructions;
			testInstructions=testInstructions(test, targetClass.getFixtures());
			
			testInstructions.append(InstructionFactory.ILOAD_1);
			testInstructions.append(InstructionFactory.IADD);
			testInstructions.append(InstructionFactory.ISTORE_1);
			
			timer(testInstructions);
		
			main.append(testInstructions);
		}
		
		main.append(getFactory().createGetStatic("java/lang/System", "out", Type.getType(java.io.PrintStream.class)));
		main.append(new LDC(getConstantPool().addInteger(targetClass.getTests().size())));
		main.append(InstructionFactory.ILOAD_1);
		main.append(InstructionFactory.ISUB);
		main.append(getFactory().createInvoke("java/io/PrintStream", "print", Type.VOID, new org.apache.bcel.generic.Type[]{org.apache.bcel.generic.Type.INT}, org.apache.bcel.Constants.INVOKEVIRTUAL));
		main.append(print(" passed, "));
		
		main.append(getFactory().createGetStatic("java/lang/System", "out",  Type.getType(java.io.PrintStream.class)));
		main.append(InstructionFactory.ILOAD_1);
		main.append(getFactory().createInvoke("java/io/PrintStream", "print", Type.VOID, new org.apache.bcel.generic.Type[]{org.apache.bcel.generic.Type.INT}, org.apache.bcel.Constants.INVOKEVIRTUAL));
		main.append(print(" failed "));
		
		timer(main);

		main.append(InstructionFactory.createReturn(Type.VOID));	
		
		return main;
	}

	private InstructionList testInstructions(TestSignature testSig, Set<FixtureSignature> fixtures)
	{
		InstructionList test=new InstructionList();
		
		test.append(print("\t- " + testSig.getName() + ": "));
		
		test.append(getFactory().createNew(targetClass.getName()));
		test.append(InstructionFactory.DUP);
		test.append(getFactory().createInvoke(targetClass.getName(), "<init>", org.apache.bcel.generic.Type.VOID, new org.apache.bcel.generic.Type[]{}, org.apache.bcel.Constants.INVOKESPECIAL));
		
		for(FixtureSignature fixture: targetClass.getFixtures())
		{		
			test.append(InstructionFactory.DUP);
			test.append(getFactory().createInvoke(targetClass.getName() + "Test", fixture.getName(), org.apache.bcel.generic.Type.VOID, new org.apache.bcel.generic.Type[]{targetClass.toBCEL()}, org.apache.bcel.Constants.INVOKESTATIC));
		}
		
		//Esegue il test sull'oggetto obj rimasto sullo stack.
		test.append(getFactory().createInvoke(targetClass.getName() + "Test", testSig.getName(), org.apache.bcel.generic.Type.INT, new org.apache.bcel.generic.Type[]{targetClass.toBCEL()}, org.apache.bcel.Constants.INVOKESTATIC));	
		return test;
	}

	private InstructionList print(String msg)
	{
		InstructionList print=new InstructionList();
		print.append(getFactory().createGetStatic("java/lang/System", "out", Type.getType(java.io.PrintStream.class)));
		print.append(new LDC(getConstantPool().addString(msg)));
		print.append(getFactory().createInvoke("java/io/PrintStream", "print", Type.VOID, new org.apache.bcel.generic.Type[]{org.apache.bcel.generic.Type.STRING}, org.apache.bcel.Constants.INVOKEVIRTUAL));
		
		return print;
	}
	
	private void timer(InstructionList test)
	{
		InstructionList start = new InstructionList();
		start.append(getFactory().createGetStatic("java/lang/System", "out", Type.getType(java.io.PrintStream.class)));
		start.append(getFactory().createInvoke("java/lang/System", "nanoTime", org.apache.bcel.generic.Type.LONG, new org.apache.bcel.generic.Type[]{}, org.apache.bcel.Constants.INVOKESTATIC));
		
		InstructionList end = new InstructionList();
		end.append(getFactory().createInvoke("java/lang/System", "nanoTime", org.apache.bcel.generic.Type.LONG, new org.apache.bcel.generic.Type[]{}, org.apache.bcel.Constants.INVOKESTATIC));
		
		end.append(InstructionConstants.LSUB);
		end.append(InstructionConstants.LNEG);
		end.append(InstructionConstants.L2F);
		end.append(new LDC(getConstantPool().addFloat(1000000)));
		end.append(InstructionConstants.FDIV);
		
		end.append(print(" [ "));
		end.append(getFactory().createInvoke("java/io/PrintStream", "print", Type.VOID, new org.apache.bcel.generic.Type[]{org.apache.bcel.generic.Type.FLOAT}, org.apache.bcel.Constants.INVOKEVIRTUAL));
		end.append(print(" ms ]\n"));
		
		test.insert(start);
		test.append(end);
	}
}
