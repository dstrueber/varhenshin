package org.eclipse.emf.henshin.variability.ocltrans;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.trace.Trace;
import org.eclipse.ocl.examples.pivot.BooleanLiteralExp;
import org.eclipse.ocl.examples.pivot.CallExp;
import org.eclipse.ocl.examples.pivot.Class;
import org.eclipse.ocl.examples.pivot.Constraint;
import org.eclipse.ocl.examples.pivot.ExpressionInOCL;
import org.eclipse.ocl.examples.pivot.IfExp;
import org.eclipse.ocl.examples.pivot.IntegerLiteralExp;
import org.eclipse.ocl.examples.pivot.Iteration;
import org.eclipse.ocl.examples.pivot.IteratorExp;
import org.eclipse.ocl.examples.pivot.LiteralExp;
import org.eclipse.ocl.examples.pivot.OCLExpression;
import org.eclipse.ocl.examples.pivot.Operation;
import org.eclipse.ocl.examples.pivot.OperationCallExp;
import org.eclipse.ocl.examples.pivot.PivotFactory;
import org.eclipse.ocl.examples.pivot.PivotPackage;
import org.eclipse.ocl.examples.pivot.PivotStandaloneSetup;
import org.eclipse.ocl.examples.pivot.RealLiteralExp;
import org.eclipse.ocl.examples.pivot.Root;
import org.eclipse.ocl.examples.pivot.StringLiteralExp;
import org.eclipse.ocl.examples.pivot.UnlimitedNaturalLiteralExp;
import org.eclipse.ocl.examples.pivot.model.OCLstdlib;
import org.eclipse.ocl.examples.pivot.resource.OCLASResourceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import GraphConstraint.GraphConstraintPackage;
import GraphConstraint.NestedGraphConstraint;

public class OcltransExample {

	private static final String ECOREOCLAS = "ecore.oclas";
	private static final String TRACEFILE = ".trace";
	private static final String TRACEROOT = "traceroot";
	private static final String AND = "and";
	private static final String NOT = "not";
	private static final String OR = "or";
	private static final String IMPLIES = "implies";
	private static final String NGC = "ngc";
	private static final String GRAPHCONSTRAINT = ".graphconstraint";
	private static final String GRAPHCONSTRAINTS = "/graphconstraints_";
	private static final String INVARIANT = "invariant";
	private static final String INIT_UNIT = "init";
	private static final String MAIN_UNIT = "main";
	private static final String HENSHIN_FILE_DIR = "files\\ocl\\henshinclassic";
	private static final String HENSHIN_FILE = "OCL2NGC.henshin";
	private static final String PATH = "files";
	private static final String OCL_STD_LIB_URI = "http://www.eclipse.org/ocl/3.1.0/OCL.oclstdlib.oclas";
	
	private String oclasFile = null;
	private String ecoreFile = null;
	private EList<Constraint> invariants = null;	
	private UnitApplication initUnitApp = null;
	private UnitApplication mainUnitApp = null;
	private EPackage metamodel = null;
	private Root root = null;
	private long start;

	public static void main(String[] args) {
		BitSet a = new BitSet();
		BitSet b = new BitSet();
		a.set(0,15,true);
		b.set(0,15,true);
		Map<BitSet,String> m = new HashMap<BitSet,String>();
		m.put(a, "Hey");
		System.out.println(m.containsKey(b));
		
//		for (int i = 1; i<11; i++) {
//			String exampleId = "0"+i;
//			if (exampleId.equals("05"))
//				exampleId = "05a";
//			if (exampleId.equals("010"))
//				exampleId = "05b";
			String exampleId = "06";
			String path = "files\\ocl\\instances\\"+exampleId+"\\";
			String ecoreFilePath = path+"PetriNetWithOCLPaper.ecore";
			String oclFilePath = path+"PetriNetWithOCLPaper.ecore.oclas";
			OcltransExample example = new OcltransExample(oclFilePath, ecoreFilePath);
			example.translate();
			System.out.println("Trasnlated "+exampleId);
//		}
	}
	
	public OcltransExample(String oclasFile, String ecoreFile) {
		this.oclasFile = oclasFile;
		this.ecoreFile = ecoreFile;
		invariants = new BasicEList<Constraint>();
		init();
	}

	private void init() {
//		OCLstdlib.lazyInstall();
		start = System.currentTimeMillis();		
		
	    ResourceSet resSet = new ResourceSetImpl();
		 org.eclipse.ocl.examples.pivot.OCL.initialize(resSet);
		 org.eclipse.ocl.examples.pivot.model.OCLstdlib.install();
		 org.eclipse.ocl.examples.pivot.delegate.OCLDelegateDomain.initialize(resSet);
		
		if (oclasFile != null) {
			URI uriOclAS = URI.createFileURI(new File(oclasFile).getAbsolutePath());
			URI uriEcore = URI.createFileURI(new File(ecoreFile).getAbsolutePath());
			if (uriOclAS != null && uriEcore != null) {
				// Load the input models and the corresponding invariants
				PivotPackage.eINSTANCE.eClass();
				GraphConstraintPackage.eINSTANCE.eClass();
				
//				http://www.eclipse.org/ocl/3.1.0/OCL.oclstdlib.oclas
					
			    Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
			    Map<String, Object> m = reg.getExtensionToFactoryMap();
			    m.put("*.oclas", new XMIResourceFactoryImpl());
			    m.put("oclas", new XMIResourceFactoryImpl());
			    m.put("oclstdlib", new XMIResourceFactoryImpl());
			    m.put("ecore", new XMIResourceFactoryImpl());
		
				// Load Ecore for meta/type model references
				Resource resourceEcore = resSet.getResource(uriEcore, true);
				metamodel = (EPackage) resourceEcore.getContents().get(0);			
			    
			    Resource resourceOclAS = resSet.getResource(uriOclAS, true);
			    root = (Root) resourceOclAS.getContents().get(0);
			    // Preparations
			    prepareOCLModel();
			    savePreparedOCLModel();		
				
				// Load the transformation module
				HenshinResourceSet resourceSet = new HenshinResourceSet(HENSHIN_FILE_DIR);
				Module module = resourceSet.getModule(HENSHIN_FILE, false);
							
				// Initialize the Henshin interpreter
				EGraph graph = new EGraphImpl(root);
				graph.addGraph(metamodel);
				Engine engine = new EngineImpl();			
				Unit initUnit = module.getUnit(INIT_UNIT);
				initUnitApp = new UnitApplicationImpl(engine, graph, initUnit, null);
				Unit mainUnit = module.getUnit(MAIN_UNIT);
				mainUnitApp = new UnitApplicationImpl(engine, graph, mainUnit, null);				
			}
		}		
		System.out.println("Finished initialization");
	}

	public void savePreparedOCLModel() {
		Date date = new GregorianCalendar().getTime();
		String path = new Path(oclasFile).removeLastSegments(1).append(GRAPHCONSTRAINTS + date.getTime()).toOSString();
		HenshinResourceSet resourceSet = new HenshinResourceSet(path);
		resourceSet.saveEObject(root, root.getName().concat(ECOREOCLAS));
//		try {
//			oclasFile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
	}

	private void prepareOCLModel() {
		TreeIterator<EObject> iter = root.eAllContents();
		while(iter.hasNext()) {
			EObject eObject = iter.next();
			if (eObject instanceof Class) {
				invariants.addAll(((Class) eObject).getOwnedInvariant());
			}
			if (eObject instanceof OperationCallExp) {
				OperationCallExp operationCallExp = (OperationCallExp) eObject;		
				Operation op = operationCallExp.getReferredOperation();
				// put operation name to operation call 
			
				operationCallExp.setName(op.getName());
				// remove referred operation since not needed anymore 
				operationCallExp.setReferredOperation(null);
			}
			if (eObject instanceof IteratorExp) {
				IteratorExp iteratorExp = (IteratorExp) eObject;
				Iteration iteration = iteratorExp.getReferredIteration();
				// put iteration name to iteration call 
				iteratorExp.setName(iteration.getName());
				// remove referred iteration since not needed anymore 
				iteratorExp.setReferredIteration(null);
			}
		}
		iter = root.eAllContents();
		while(iter.hasNext()) {
			EObject eObject = iter.next();
			if (eObject instanceof OperationCallExp) {
				OperationCallExp operationCallExp = (OperationCallExp) eObject;
				// refactor boolean operation 'implies'
				if (operationCallExp.getName().equals(IMPLIES)) {
					refactorImpliesOperation(operationCallExp);
				}
			}
			// refactor IF expression
			if (eObject instanceof IfExp) {
				IfExp ifExp = (IfExp) eObject;
				refactorIfExpression(ifExp);
			}	
			// refactor literals
			if (eObject instanceof LiteralExp) {
				LiteralExp literalExp = (LiteralExp) eObject;
				refactorLiteralExpression(literalExp);
			}
		}
	}

	private void refactorLiteralExpression(LiteralExp literalExp) {
		if (literalExp instanceof StringLiteralExp) {
			StringLiteralExp exp = (StringLiteralExp) literalExp;
			exp.setName(exp.getStringSymbol());
		}
		if (literalExp instanceof BooleanLiteralExp) {
			BooleanLiteralExp exp = (BooleanLiteralExp) literalExp; 
			exp.setName(Boolean.toString(exp.isBooleanSymbol()));
		}
		if (literalExp instanceof RealLiteralExp) {
			RealLiteralExp exp = (RealLiteralExp) literalExp;
			exp.setName(Double.toString(exp.getRealSymbol().doubleValue()));
		}
		if (literalExp instanceof UnlimitedNaturalLiteralExp) {
			UnlimitedNaturalLiteralExp exp = (UnlimitedNaturalLiteralExp) literalExp;
			exp.setName(Integer.toString(exp.getUnlimitedNaturalSymbol().intValue()));
		}
		if (literalExp instanceof IntegerLiteralExp) {
			IntegerLiteralExp exp = (IntegerLiteralExp) literalExp;
			exp.setName(Integer.toString(exp.getIntegerSymbol().intValue()));
		}		
	}

	private void refactorImpliesOperation(OperationCallExp operationCallExp) {
		operationCallExp.setName(OR);
		OCLExpression oldSourceExpression = operationCallExp.getSource();
		OperationCallExp newSourceExpression = PivotFactory.eINSTANCE.createOperationCallExp();
		newSourceExpression.setName(NOT);
		operationCallExp.setSource(newSourceExpression);
		newSourceExpression.setSource(oldSourceExpression);
	}

	private void refactorIfExpression(IfExp ifExp) {
		// remember and copy OCL expressions
		OCLExpression conditionExpression = ifExp.getCondition();
		OCLExpression conditionExpressioCopy = EcoreUtil.copy(conditionExpression);
		OCLExpression thenExpression = ifExp.getThenExpression();
		OCLExpression elseExpression = ifExp.getElseExpression();
		ifExp.setCondition(null);
		ifExp.setThenExpression(null);
		ifExp.setElseExpression(null);
		// create the new structure
		PivotFactory factory = PivotFactory.eINSTANCE;
		OperationCallExp opCallOR = factory.createOperationCallExp();
		opCallOR.setName(OR);
		OperationCallExp opCallANDSource = factory.createOperationCallExp();
		opCallANDSource.setName(AND);
		opCallANDSource.setSource(conditionExpression);
		opCallANDSource.getArgument().add(thenExpression);
		opCallOR.setSource(opCallANDSource);
		OperationCallExp opCallANDArgument = factory.createOperationCallExp();
		opCallANDArgument.setName(AND);
		OperationCallExp opCallNOT = factory.createOperationCallExp();
		opCallNOT.setName(NOT);
		opCallNOT.setSource(conditionExpressioCopy);
		opCallANDArgument.setSource(opCallNOT);
		opCallANDArgument.getArgument().add(elseExpression);
		opCallOR.getArgument().add(opCallANDArgument);
		// replace if expression by new structure
		EObject container = ifExp.eContainer();
		// case 1: is body expression of specification
		if (container instanceof ExpressionInOCL) {
			ExpressionInOCL expressionInOCL = (ExpressionInOCL) container;
			if (expressionInOCL.getBodyExpression() == ifExp) {
				expressionInOCL.setBodyExpression(opCallOR);
			}
		}
		// case 2: is body of iterator
		if (container instanceof IteratorExp) {
			IteratorExp iteratorExp = (IteratorExp) container;
			if (iteratorExp.getBody() == ifExp) {
				iteratorExp.setBody(opCallOR);
			}
		}
		// case 3: is source of call expression
		if (container instanceof CallExp) {
			CallExp callExp = (CallExp) container;
			if (callExp.getSource() == ifExp) {
				callExp.setSource(opCallOR);
			}
		}
		// case 4: is argument of operation call expression
		if (container instanceof OperationCallExp) {
			OperationCallExp operationCallExp = (OperationCallExp) container;
			if (operationCallExp.getArgument().contains(ifExp)) {
				int index = operationCallExp.getArgument().indexOf(ifExp);
				operationCallExp.getArgument().add(index, opCallOR);
			}
		}
		if (container instanceof IfExp) {
			IfExp ifExpContainer = (IfExp) container;
			// case 5: is condition of if expression
			if (ifExpContainer.getCondition() == ifExp) {
				ifExpContainer.setCondition(opCallOR);
			}
			// case 6: is then of if expression
			if (ifExpContainer.getThenExpression() == ifExp) {
				ifExpContainer.setThenExpression(opCallOR);
			}
			// case 7: is else of if expression
			if (ifExpContainer.getElseExpression() == ifExp) {
				ifExpContainer.setElseExpression(opCallOR);
			}
		}
		
	}

	public long translate() {		
		if (initUnitApp != null) {
			Date date = new GregorianCalendar().getTime();
			for (Constraint inv: invariants) {
				initUnitApp.setParameterValue(INVARIANT, inv);
				InterpreterUtil.executeOrDie(initUnitApp);
				NestedGraphConstraint nestedGraphConstraint = (NestedGraphConstraint) initUnitApp.getResultParameterValue(NGC);	
				Trace trace = (Trace) initUnitApp.getResultParameterValue(TRACEROOT);
//				InterpreterUtil.executeOrDie(mainUnitApp);	
//				saveNestedGraphConstraint(date, nestedGraphConstraint, trace);				
			}
		}
		long stop = System.currentTimeMillis();
		System.out.println("Initialization time: " + ((stop - start)) + " millisec");
		return (stop - start);
	}

	private void saveNestedGraphConstraint(Date date, NestedGraphConstraint ngc, Trace trace) {	
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
		String path = new Path(oclasFile).removeLastSegments(1).append(GRAPHCONSTRAINTS + sdf.format(date)).toOSString();
		HenshinResourceSet resourceSet = new HenshinResourceSet(path);
		resourceSet.saveEObject(ngc, ngc.getName().concat(GRAPHCONSTRAINT));
		resourceSet.saveEObject(root, root.getName().concat(ECOREOCLAS));
		resourceSet.saveEObject(trace, ngc.getName().concat("1" + TRACEFILE));
		printTrace(trace);
		resourceSet.saveEObject(trace, ngc.getName().concat("2" + TRACEFILE));
//		try {
//			oclasFile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
	}

	private void printTrace(Trace trace) {
		trace.getSource().clear();
		trace.getTarget().clear();
		if (trace.getName() != null && ! trace.getName().isEmpty()) {
			System.out.println(trace.getName());
		}
		for (Trace subTrace : trace.getSubTraces()) {
			printTrace(subTrace);
		}
	}

//	private String getFullPath() {
//		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
//		URL url = FileLocator.find(bundle, new Path(PATH), Collections.EMPTY_MAP);
//		URL fileUrl = null;
//		try {
//			fileUrl = FileLocator.toFileURL(url);
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
//		return 	fileUrl.getPath();
//	}	

}
