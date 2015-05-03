 package org.eclipse.emf.henshin.variability.matcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;

import de.fosd.typechef.featureexpr.FeatureExpr;

/**
 * One match as yielded by variability-aware matching, comprising a regular
 * match and a set of selected features producing the regular rule yielding the match.
 * 
 * @author Daniel Strüber
 *
 */
public class VariabilityAwareMatch {
	private Rule rule;
	private RulePreparator rulePreperator;
	
	public VariabilityAwareMatch(Match match, Set<FeatureExpr> selected, Rule rule, RulePreparator rulePreparator) {
		super();
		this.match = match;
		this.selected = new HashSet<FeatureExpr>();
		this.selected.addAll(selected);
		this.rule = rule;
		this.rulePreperator = rulePreparator;
	}

	public Match getMatch() {
		return match;
	}

	public void setMatch(Match match) {
		this.match = match;
	}

	public Set<FeatureExpr> getSelected() {
		return selected;
	}

	public void setSelected(Set<FeatureExpr> selected) {
		this.selected = selected;
	}

	private Match match;

	private Set<FeatureExpr> selected;

	public Rule getRule() {
		return rule;
	}
	
	public void prepareRule() {
		rulePreperator.doPreparation();
	}
	
	public void undoPreparation() {
		rulePreperator.undo();
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}
}
