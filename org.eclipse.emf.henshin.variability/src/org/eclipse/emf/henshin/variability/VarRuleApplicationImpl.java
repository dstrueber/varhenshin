package org.eclipse.emf.henshin.variability;

import java.util.Set;

import org.eclipse.emf.henshin.interpreter.ApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.interpreter.impl.RuleApplicationImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.matcher.VariabilityAwareMatch;
import org.eclipse.emf.henshin.variability.matcher.VariabilityAwareEngine;
import org.eclipse.emf.henshin.variability.util.RuleUtil;

/**
 * Variability-aware {@link org.eclipse.emf.henshin.interpreter.RuleApplication
 * RuleApplication} implementation.
 * 
 * @author Daniel Str�ber
 */
public class VarRuleApplicationImpl extends RuleApplicationImpl {

	public VarRuleApplicationImpl(Engine engine, EGraph graph, Rule rule,
			Assignment partialMatch) {
		super(engine, graph, rule, partialMatch);
	}

	public VarRuleApplicationImpl(Engine engine) {
		super(engine);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.henshin.interpreter.UnitApplication#execute(org.eclipse
	 * .emf.henshin.interpreter.ApplicationMonitor)
	 */
	@Override
	public boolean execute(ApplicationMonitor monitor) {
		if (unit == null) {
			throw new NullPointerException("No transformation unit set");
		}
		// Already executed?
		if (isExecuted) {
			if (isCompleteMatchDerived) {
				completeMatch = null; // reset the complete match if it was
										// derived
				isCompleteMatchDerived = false;
			}
			isExecuted = false;
			isUndone = false;
			change = null;
			resultMatch = null;
		}
		// Do we need to derive a complete match?
		long startTime = System.currentTimeMillis();
		VariabilityAwareMatch completeMatchVar = null;
		if (completeMatch == null) {
			if (!RuleUtil.isVarRule(unit)) {
				completeMatch = engine
						.findMatches((Rule) unit, graph, partialMatch)
						.iterator().next();
			} else {
				Set<VariabilityAwareMatch> matches = new VariabilityAwareEngine(
						(Rule) unit, graph).findMatches();
				if (!matches.isEmpty()) {
					completeMatchVar = (VariabilityAwareMatch) matches
							.iterator().next();
					completeMatch = completeMatchVar.getMatch();
					unit = completeMatchVar.getMatch().getRule();

				}

			}
			isCompleteMatchDerived = true;
		}
		long runtime = (System.currentTimeMillis() - startTime);
		MatchingLog.getEntries().add(
				new MatchingLogEntry(unit, completeMatch != null, runtime,
						graph.size(), 0)); // InterpreterUtil.countEdges(graph)));
		if (completeMatch == null) {
			if (monitor != null) {
				monitor.notifyExecute(this, false);
			}
			return false;
		}
		Rule rule = null;
		if (completeMatchVar != null) {
			completeMatchVar.prepareRule();
		} 
		resultMatch = new MatchImpl((Rule) unit, true);
		change = engine.createChange((Rule) unit, graph, completeMatch,
				resultMatch);
		if (change == null) {
			if (monitor != null) {
				monitor.notifyExecute(this, false);
			}
			return false;
		}
		change.applyAndReverse();
		isExecuted = true;
		if (monitor != null) {
			monitor.notifyExecute(this, true);
		}	if (completeMatchVar != null) {
			completeMatchVar.undoPreparation();
		} 
		return true;
	}

}
