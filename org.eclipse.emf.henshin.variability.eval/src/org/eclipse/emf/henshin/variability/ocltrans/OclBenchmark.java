/**
 * <copyright>
 * Copyright (c) 2010-2012 Henshin developers. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.variability.ocltrans;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.RuleApplication;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.BasicApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.RuleApplicationImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.trace.Trace;
import org.eclipse.emf.henshin.variability.MatchingLog;
import org.eclipse.emf.henshin.variability.MatchingLogEntry;
import org.eclipse.emf.henshin.variability.VarUnitApplicationImpl;
import org.eclipse.emf.henshin.variability.util.RuleUtil;
import org.eclipse.ocl.examples.pivot.Class;
import org.eclipse.ocl.examples.pivot.Constraint;
import org.eclipse.ocl.examples.pivot.PivotPackage;
import org.eclipse.ocl.examples.pivot.Root;

import GraphConstraint.GraphConstraintPackage;
import GraphConstraint.NestedGraphConstraint;

public class OclBenchmark {
	private static final String TRACEROOT = "traceroot";
	private static final String INVARIANT = "invariant";
	private static final String NGC = "ngc";
	private static boolean printTrace = false;

	/**
	 * Relative path to the model files.
	 */
	public static final String PATH = "files/ocl";

	public static void main(String[] args) {
			String[] examples = {
//					"01", "02", "03", 
//					"04", "05a", 
					"05b", 
//					"06" , 
//					"07",
//					
//					"08",		
//					"09"
					};
//			String[] examples = { "08", "09" };
//			boolean performVar = true;
//			boolean performclassic = false;
			boolean performVar = true;
			boolean performclassic = true;
			int runs = 10;
			
			for (String example : examples) {
				System.out.println("Example " +example);
				if (performVar) {
					System.out.println("Variability-aware:");
					for (int i = 0; i < runs; i++) {
						run(PATH, true, example);
						System.gc();
					}
				}
				if (performclassic) {
					System.out.println("Classic:");
					for (int i = 0; i < runs; i++) {
						run(PATH, false, example);
						System.gc();
					}
				}
			}
			// for (int i = 0; i < 10; i++) {
			// run(PATH, false);
			// }
			// for (int i = 0; i < 10; i++) {
			// run(PATH, true);
			// }
		}

	/**
	 * Run the benchmark.
	 * 
	 * @param path
	 *            Relative path to the model files.
	 * @param iterations
	 *            Number of iterations.
	 */
	public static void run(String path, boolean variabilityAware,
			String exampleID) {
		PivotPackage.eINSTANCE.eClass();
		GraphConstraintPackage.eINSTANCE.eClass();
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("ecore", new XMIResourceFactoryImpl());
		m.put("oclas", new XMIResourceFactoryImpl());
		m.put("oclstdlib", new XMIResourceFactoryImpl());

		// Create a resource set with a base directory:
		HenshinResourceSet resourceSet = new HenshinResourceSet(path);

		// Load the module and find the rule:

		Module module = resourceSet.getModule(
				variabilityAware ? "henshinvar/OCL2NGC.henshin"
						: "henshinclassic/OCL2NGC.henshin", false);

		Unit initUnit = module.getUnit("init");
		Unit mainUnit = module.getUnit("main");

		// Load the model into a graph:
		Resource metamodel = resourceSet.getResource("instances/" + exampleID
				+ "/PetriNetWithOCLPaper.ecore");
		Resource resource = resourceSet.getResource("instances/" + exampleID
				+ "/PetriNetWithOCLPaper.ecoreecore.oclas");
		Root root = (Root) resource.getContents().get(0);
		EGraph graph = new EGraphImpl(resource);
		graph.addTree(metamodel.getContents().get(0));
		
		int graphInitially = graph.size();

		// Create an engine and a rule application:
		Engine engine = new EngineImpl();

		UnitApplication initUnitApplication = new UnitApplicationImpl(engine,
				graph, initUnit, null);
		ApplicationMonitor monitor = new BasicApplicationMonitor();
		System.gc();
		BasicEList<Constraint> invariants = new BasicEList<Constraint>();
		TreeIterator<EObject> iter = resource.getContents().get(0)
				.eAllContents();
		while (iter.hasNext()) {
			EObject eObject = iter.next();
			if (eObject instanceof Class) {
				invariants.addAll(((Class) eObject).getOwnedInvariant());
			}
		}

		long start = System.currentTimeMillis();
		Trace trace = null;
		NestedGraphConstraint nestedGraphConstraint = null;
		if (initUnitApplication != null) {
			Date date = new GregorianCalendar().getTime();
			for (Constraint inv : invariants) {
				initUnitApplication.setParameterValue(INVARIANT, inv);
				InterpreterUtil.executeOrDie(initUnitApplication);
				nestedGraphConstraint = (NestedGraphConstraint) initUnitApplication
						.getResultParameterValue(NGC);
				trace = (Trace) initUnitApplication
						.getResultParameterValue(TRACEROOT);
			}
		}
		initUnitApplication.execute(monitor);
		if (!initUnitApplication.execute(monitor)) {
			throw new RuntimeException("Error during initialization");
		}
		long stop = System.currentTimeMillis();
		// System.out.println("Initialization time: " + ((stop - start)) +
		// " millisec");

		UnitApplication mainUnitApplication = null;
		if (variabilityAware)
			mainUnitApplication = new VarUnitApplicationImpl(engine, graph,
					mainUnit, null);
		else
			mainUnitApplication = new VarUnitApplicationImpl(engine, graph,
					mainUnit, null);

		System.gc();

		long startTime = System.currentTimeMillis();

		monitor = new BasicApplicationMonitor();
		System.gc();

		startTime = System.currentTimeMillis();
		if (!mainUnitApplication.execute(monitor)) {
			throw new RuntimeException("Error during transformation");
		}
		long runtime = (System.currentTimeMillis() - startTime);

		printInfo(graph, graphInitially, runtime);

	}

	private static void printInfo(EGraph graph, int graphInitially, long runtime) {
		int varSuccessfully = 0;
		int classicSuccessfully = 0;
		int varFailed = 0;
		int classicFailed = 0;

		for (MatchingLogEntry e : MatchingLog.getEntries()) {
			if (RuleUtil.isVarRule(e.getUnit())) {
				if (e.isSuccessful())
					varSuccessfully++;
				else
					varFailed++;
			} else {
				if (e.isSuccessful())
					classicSuccessfully++;
				else
					classicFailed++;
			}
		}

		// System.out.println(graphInitially + "\t" + graph.size() +
		String t = "\t";
		// graph.size() + t 
		System.out.println(+ MatchingLog.getEntries().size() + t
				+ (varSuccessfully + classicSuccessfully) + t
				+ (varFailed + classicFailed) + t + varSuccessfully + t +
				varFailed + t + classicSuccessfully + t + classicFailed + t
				+ runtime);
		if (printTrace)
			System.out.println(MatchingLog.createString());
		MatchingLog.getEntries().clear();
	}

	private static void saveNestedGraphConstraint(Date date,
			NestedGraphConstraint ngc, Root root, Trace trace) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
		String path = new Path(PATH + "/graphconstraints/" + sdf.format(date))
				.toOSString();
		HenshinResourceSet resourceSet = new HenshinResourceSet(path);
		resourceSet.saveEObject(ngc,
				path + "/" + ngc.getName().concat("GraphConstraint"));
		resourceSet.saveEObject(root,
				path + "/" + root.getName().concat(".ecore.oclass"));
		resourceSet.saveEObject(trace, path + "/"
				+ ngc.getName().concat("1" + ".trace"));
		resourceSet.saveEObject(trace, path + "/"
				+ ngc.getName().concat("2" + ".trace"));
		// try {
		// oclasFile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
		// } catch (CoreException e) {
		// e.printStackTrace();
		// }
	}
	
	

}
