package translation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javaBytecodeGenerator.JavaClassGenerator;
import javaBytecodeGenerator.TestClassGenerator;

import types.ClassMemberSignature;
import types.CodeSignature;
import types.ClassType;
import bytecode.Bytecode;
import bytecode.CALL;
import bytecode.FieldAccessBytecode;

/**
 * A program, that is, a set of class member signatures.
 *
 * @author <A HREF="mailto:fausto.spoto@univr.it">Fausto Spoto</A>
 */

public class Program {

	/**
	 * The set of class signatures making up this program.
	 */

	private final Set<ClassMemberSignature> sigs;

	/**
	 * The starting code of this program. This is usually the {@code main} method of this program.
	 */

	private final CodeSignature start;

	/**
	 * Builds a program, that is, a set of class member signatures.
	 *
	 * @param sigs the set of signatures
	 * @param start the code where the program starts
	 */

	public Program(Set<ClassMemberSignature> sigs, CodeSignature start) {
		this.sigs = sigs;
		this.start = start;

		// we clean-up the code, in order to remove useless nop's and merge blocks whenever possible
		if (start != null)
			cleanUp();
	}

	/**
	 * Yields the class signatures that make up this program.
	 *
	 * @return the signatures
	 */

	public Set<ClassMemberSignature> getSigs() {
		return sigs;
	}

	/**
	 * Yields the method from which the program starts.
	 *
	 * @return the method where the program starts
	 */

	public CodeSignature getStart() {
		return start;
	}

	/**
	 * Yields the first block of code from which the program starts.
	 *
	 * @return the first block of code from which the program starts
	 */

	public Block firstBlock() {
		return getStart().getCode();
	}

	/**
	 * Cleans-up the code of this program. This amounts to removing useless
	 * nop's or methods or constructors that are not called.
	 */

	public void cleanUp() {
		/* sigs.clear(); */
		start.getCode().cleanUp(this);
	}

	/**
	 * Dumps the Kitten code of the signatures in this set into dot files. It is
	 * assumed that all these signatures have already been translated into Kitten code.
	 */

	public void dumpCodeDot() {
		for (ClassMemberSignature sig: sigs)
			if (sig instanceof CodeSignature)
				try {
					dumpCodeDot((CodeSignature) sig, "./");
				}
				catch (IOException e) {
						System.out.println("Could not dump Kitten code for " + sig);
				}
	}

	/**
	 * Writes a dot file containing a representation of the graph of blocks
	 * for the code of the given code signature (method or constructor).
	 *
	 * @param sig the signature
	 * @param dir the directory where the file must be written
	 * @throws IOException if an input/output error occurs
	 */

	private void dumpCodeDot(CodeSignature sig, String dir) throws IOException {
		try (FileWriter dot = new FileWriter(dir + sig + ".dot")) {
			// the name of the graph
			dot.write("digraph \"" + sig + "\" {\n");

			// the size of a standard A4 sheet (in inches)
			dot.write("size = \"11,7.5\";\n");

			toDot(sig.getCode(), dot, new HashSet<Block>());

			dot.write("}");
			dot.flush();
		}
	}

	/**
	 * Auxiliary method which writes in the dot file a box standing for the
	 * given block, linked to the following blocks, if any.
	 *
	 * @param block the block
	 * @param where the file where the dot representation must be written
	 * @param done the set of blocks which have been processed up to now
	 * @return the identifier of {@code block} in the dot file
	 * @throws IOException if an input/output error occurs
	 */

	private String toDot(Block block, FileWriter where, Set<Block> done) throws IOException {
		String name = block.dotNodeName();

		// did we already dumped the given block in the file?
		if (!done.contains(block)) {
			// we never dumped the block before: we add it to the set of already dumped blocks
			done.add(block);

			// we add a box to the dot file
			where.write(name + " [ shape = box, label = \"block " + block.getId() + "\\n");

			// in the middle there is a dump of the bytecode inside the block
			where.write(block.getBytecode().toString().replaceAll("\n","\\\\n"));

			// end of the label of the node
			where.write("\"];\n");

			// we add a dot representation for the follows of the block
			for (Block follow: block.getFollows())
				where.write(name + "->" + toDot(follow, where, done) + " [color = blue label = \"\" fontsize = 8]\n");
		}

		// we return the unique identifier of the block in the dot file
		return name;
	}

	/**
	 * Generates the Java bytecode for all the class types and
	 * dumps the relative {@code .class} files on the file system.
	 */

	public void generateJavaBytecode() {
		// we consider one class at the time and we generate its Java bytecode
		for (ClassType clazz: ClassType.getAll())
			try {
				//TODO loro hanno preferito generare una classe a parte detta NormalClassGenerator nel package JavaBytecodeGenerator, la quale è figlia dell'originale JavaClassGenerator (tutto ciò che hanno messo in Normal è parte del costruttore di JavaClassGenerator). In sostanza, possiamo vedere la LORO NormalClassGenerator come la nostra JavaClassGenerator. 
				new JavaClassGenerator(clazz, sigs).getJavaClass().dump(clazz + ".class");
				//TODO hanno aggiunto una semplice stampa in stile Mattia di clazz.
			}
			catch (IOException e) {
				System.out.println("Could not dump the Java bytecode for class " + clazz);
			}
	}
	
	/**
	 * Generates the Java bytecode for all the class types and
	 * dumps the relative {@code .class} files on the file system.
	 */

	public synchronized void generateJavaBytecodeForTests()
	{
		// we consider one class at the time and we generate its Java bytecode
		for (ClassType clazz: ClassType.getAll())
		{
			/* TODO: questo metodo corrisponde, ma i nostri amici hanno aggiunto del codice da babbei con qualche stampa. */

			if(!clazz.getTests().isEmpty())
				try
				{
					new TestClassGenerator(clazz, sigs).getJavaClass().dump(clazz + "Test.class");
				}
				catch(IOException e)
				{
					System.out.println("Could not dump the Java bytecode for testClass " + clazz);
				}
		}
	}

	/**
	 * Takes note that this program contains the given bytecode. This amounts
	 * to adding some signature to the set of signatures for the program.
	 *
	 * @param bytecode the bytecode
	 */

	protected void storeBytecode(Bytecode bytecode) {
		if (bytecode instanceof FieldAccessBytecode)
			sigs.add(((FieldAccessBytecode) bytecode).getField());
		else if (bytecode instanceof CALL)
			// a call instruction might call many methods or constructors at runtime
			sigs.addAll(((CALL) bytecode).getDynamicTargets());
	}
}
