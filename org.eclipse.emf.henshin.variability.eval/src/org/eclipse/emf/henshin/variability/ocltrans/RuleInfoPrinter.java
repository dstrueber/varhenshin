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

import java.util.Map;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.ocl.examples.pivot.PivotPackage;

import GraphConstraint.GraphConstraintPackage;

public class RuleInfoPrinter {
	private static final String TRACEROOT = "traceroot";
	private static final String INVARIANT = "invariant";
	private static final String NGC = "ngc";

	/**
	 * Relative path to the model files.
	 */
	public static final String PATH = "files/ocl";


	/**
	 * Run the benchmark.
	 * 
	 * @param path
	 *            Relative path to the model files.
	 * @param iterations
	 *            Number of iterations.
	 */

	public static void main(String[] args) {
		PivotPackage.eINSTANCE.eClass();
		GraphConstraintPackage.eINSTANCE.eClass();
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("ecore", new XMIResourceFactoryImpl());
		m.put("oclas", new XMIResourceFactoryImpl());
		m.put("oclstdlib", new XMIResourceFactoryImpl());

		// Create a resource set with a base directory:
		HenshinResourceSet resourceSet = new HenshinResourceSet(PATH);

		// Load the module and find the rule:

		Module moduleClassic = resourceSet.getModule(
				"henshinclassic/OCL2NGC.henshin", false);
		Module moduleVar = resourceSet.getModule("henshinvar/OCL2NGC.henshin",
				false);

		String brs[] = { "04", "09", "10", "13a", "13b", "14a", "14b", "15a", "15b", "16", "17a","17b" };
		String suffixes[] = { "source", "argument", "body" };
		String t = "\t";
		for (String b : brs) {
			for (String s : suffixes) {
				String ruleName = "tr_E_" + b + "_" + s;
				Rule r = moduleClassic.getRule(ruleName);
				System.out.println(r.getName() + t + r.getLhs().getNodes().size() + t + r.getLhs().getEdges().size() 
						+ t + r.getRhs().getNodes().size() + t + r.getRhs().getEdges().size() );

			}
		}

		for (Rule r : moduleVar.getRules()) {
			if (r.getName().endsWith("_var")) {
				int varNodesLhs=0;
				int varEdgesLhs=0;
				for (Node n : r.getLhs().getNodes()) {
					if (n.getPresenceCondition() != null && !n.getPresenceCondition() .equals("")) {
						varNodesLhs++;
					}
				}
				for (Edge e: r.getLhs().getEdges()) {
					if (e.getPresenceCondition() != null && !e.getPresenceCondition() .equals("")) {
						varEdgesLhs++;
					}
				}	
				int varNodesRhs=0;
				int varEdgesRhs=0;
				for (Node n : r.getRhs().getNodes()) {
					if (n.getPresenceCondition() != null && !n.getPresenceCondition() .equals("")) {
						varNodesRhs++;
					}
				}
				for (Edge e: r.getRhs().getEdges()) {
					if (e.getPresenceCondition() != null && !e.getPresenceCondition() .equals("")) {
						varEdgesRhs++;
					}
				}	
				
				System.out.println(r.getName() + t + (r.getLhs().getNodes().size() -varNodesLhs) + t + varNodesLhs + t + (r.getLhs().getEdges().size() - varEdgesLhs) + t + varEdgesLhs
						+ t + (r.getRhs().getNodes().size() -varNodesRhs) + t + varNodesRhs + t + (r.getRhs().getEdges().size() - varEdgesRhs) + t + varEdgesRhs

						);
				
			}
		}
	}

}
