/**
 * <copyright>
 * Copyright (c) 2010-2012 Henshin developers. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.variability.refac;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
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
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.variability.VarUnitApplicationImpl;


public class VarBenchmark {
	
	/** 
	 * Relative path to the  model files.
	 */
	public static final String PATH = "src/org/eclipse/emf/henshin/variability/refac";
	
	/**
	 * Run the benchmark.
	 * @param path Relative path to the model files.
	 * @param iterations Number of iterations.
	 */
	public static void run(String path, boolean variabilityAware) {
		
		// Create a resource set with a base directory:
		HenshinResourceSet resourceSet = new HenshinResourceSet(path);
		
		// Load the module and find the rule:
		
		Module module = resourceSet.getModule(variabilityAware ? "RefactorExpressionsVar.henshin" : "RefactorExpressions.henshin");
		Unit unit = (Unit) module.getUnit("main");

		// Load the model into a graph:
		Resource resource = resourceSet.getResource("Expression.xmi");
		EGraph graph = new EGraphImpl(resource);
		int graphInitially = graph.size();
		
		// Create an engine and a rule application:
		Engine engine = new EngineImpl();
		
		UnitApplication application = null;
		if (variabilityAware)
			application = new VarUnitApplicationImpl(engine);
		else
			application = new UnitApplicationImpl(engine);
			
		
		application.setUnit(unit);
		application.setEGraph(graph);
		
		// Check how much memory is available:
//		System.out.println("Starting  benchmark...");
//		System.out.println(Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB available memory\n");

//		System.out.println("Level\tMatches\tNodes\tMatTime\tAppTime\tTotTime");



//		List<Match> matches = new ArrayList<Match>();
//
//			// Clear the matches:
//			matches.clear();
			System.gc();

			// Find all matches:
			long startTime = System.currentTimeMillis();
//			for (Match match : engine.(rule, graph, null)) {


			ApplicationMonitor monitor = new BasicApplicationMonitor();
			System.gc();

			startTime = System.currentTimeMillis();
				if (!application.execute(monitor)) {
					throw new RuntimeException("Error transforming Sierpinski model");
				}
			long runtime = (System.currentTimeMillis() - startTime);

			// Print info:
			System.out.println(graphInitially + "\t" + graph.size() + "\t" + runtime);

		}
		
	
	public static void main(String[] args) {

		System.out.println("Nodes\tTotTime");
		for (int i = 0; i<10; i++) {
//			run(PATH, false); 
		}
		for (int i = 0; i<10; i++) {
			run(PATH, true); 
		}
		for (int i = 0; i<10; i++) {
			run(PATH, false); 
		}
		for (int i = 0; i<10; i++) {
			run(PATH, true); 
		}
	}
	
}
