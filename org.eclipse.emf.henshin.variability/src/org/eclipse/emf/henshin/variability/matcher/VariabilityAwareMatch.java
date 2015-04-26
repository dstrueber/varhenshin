package org.eclipse.emf.henshin.variability.matcher;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.henshin.interpreter.Match;

import de.fosd.typechef.featureexpr.FeatureExpr;

/**
 * One match as yielded by variability-aware matching, comprising a regular
 * match and a set of selected features producing the regular rule yielding the match.
 * 
 * @author Daniel Strüber
 *
 */
public class VariabilityAwareMatch {
	public VariabilityAwareMatch(Match match, Set<FeatureExpr> selected) {
		super();
		this.match = match;
		this.selected = new HashSet<FeatureExpr>();
		this.selected.addAll(selected);
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
}
