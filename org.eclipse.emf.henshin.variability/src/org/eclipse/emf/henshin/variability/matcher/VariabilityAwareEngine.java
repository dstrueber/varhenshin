package org.eclipse.emf.henshin.variability.matcher;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.model.And;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.MappingList;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Rule;

import de.fosd.typechef.featureexpr.FeatureExpr;

/**
 * Applies the algorithm described in [1] to determine all variability-aware
 * matches.
 * 
 * [1] <a href=
 * "https://www.uni-marburg.de/fb12/swt/forschung/publikationen/2015/SRCT15.pdf"
 * >Strüber, Julia , Chechik, Taentzer (2015): A Variability-Based Approach to
 * Reusable and Efficient Model Transformations</a>.
 * 
 * @author Daniel Strüber
 *
 */
public class VariabilityAwareEngine {

	private Rule rule;
	private EGraph graph;
	private EngineImpl engine;

	private Map<String, FeatureExpr> expressions;

	private RuleInfo ruleInfo;
	private RulePreparator rulePreparator;

	/**
	 * Variability-based matching needs to create a new matching engine for each
	 * base match. Hence, if the number of base matches is too great,
	 * performance will suffer due to the initialization effort.
	 */
	private int THRESHOLD_MAXIMUM_BASE_MATCHES = 10;

	private static Map<Rule, RuleInfo> ruleInfoRegistry = new HashMap<Rule, RuleInfo>();

	public VariabilityAwareEngine(Rule rule, EGraph graph) {
		super();
		this.rule = rule;
		this.graph = graph;
		this.engine = new EngineImpl();
		this.rulePreparator = new RulePreparator(rule);

		if (!ruleInfoRegistry.containsKey(rule))
			ruleInfoRegistry.put(rule, new RuleInfo(rule));
		this.ruleInfo = ruleInfoRegistry.get(rule);
		populateExpressionMap();
	}

	private void populateExpressionMap() {
		if (ruleInfoRegistry.containsKey(rule)) {
			expressions = ruleInfo.getExpressions();
		}
	}

	public Set<VariabilityAwareMatch> findMatches() {
		// Remove everything except for the base rule
		BitSet bs = rulePreparator.prepare(ruleInfo, ruleInfo.getPc2Elem()
				.keySet(), rule.isInjectiveMatching());
//		long time = System.currentTimeMillis();
		Set<Match> baseMatches = new HashSet<Match>();
		Iterator<Match> it = engine.findMatches(rule, graph, null).iterator();
		while (it.hasNext()) {
			if (baseMatches.size() < THRESHOLD_MAXIMUM_BASE_MATCHES) {
				baseMatches.add(it.next());
			} else {
				baseMatches.clear();
				baseMatches.add(null);
				break;
			}
		}
		
		rulePreparator.undo();
//		System.err.println(System.currentTimeMillis()-time);

		Set<VariabilityAwareMatch> matches = new HashSet<VariabilityAwareMatch>();
		if (!baseMatches.isEmpty()) {			
//			System.out.println(baseMatches);	
			List<FeatureExpr> conditions = new LinkedList<FeatureExpr>();
			conditions.addAll(expressions.values());
			MatchingInfo mo = new MatchingInfo(conditions, ruleInfo);
			mo.getMatchedSubrules().add(bs);
			findMatches(rule, mo, baseMatches, matches);
		}

		return matches;
	}

	private Set<VariabilityAwareMatch> findMatches(Rule rule,
			MatchingInfo matchingInfo, Set<Match> baseMatches,
			Set<VariabilityAwareMatch> matches) {
		FeatureExpr current = getFirstNeutral(matchingInfo);
		matchingInfo.set(current, null, true);
		findMatchInner(rule, matchingInfo, baseMatches, matches);

		matchingInfo.set(current, true, false);
		findMatchInner(rule, matchingInfo, baseMatches, matches);
		matchingInfo.set(current, false, null);
		return matches;
	}

	private FeatureExpr getFirstNeutral(MatchingInfo matchingInfo) {
		for (FeatureExpr e : matchingInfo.getInfo().keySet()) {
			if (matchingInfo.getInfo().get(e) == null)
				return e;
		}
		return null;
	}

	private Set<VariabilityAwareMatch> findMatchInner(Rule rule,
			MatchingInfo matchingInfo, Set<Match> baseMatches,
			Set<VariabilityAwareMatch> matches) {
		Set<FeatureExpr> newContradictory = getNewContradictory(matchingInfo);
		matchingInfo.setAll(newContradictory, null, false);

		Set<FeatureExpr> newImplicated = getNewImplicated(matchingInfo);
		matchingInfo.setAll(newImplicated, null, true);

		// If there are no presence conditions contradicting or implied by the
		// current assignment (= neutral is empty), calculate the matches
		// classically.
		if (matchingInfo.getNeutrals().isEmpty()) {
			BitSet reducedRule = rulePreparator.prepare(ruleInfo,
					matchingInfo.getAssumedFalse(),
					determineInjectiveMatching(matchingInfo));
			// The following check ensures that we will not match the same
			// sub-rule twice.
			if (!matchingInfo.getMatchedSubrules().contains(reducedRule)) {
				for (Match bm : baseMatches) {
					Iterator<Match> classicMatches = engine.findMatches(rule,
							graph, bm).iterator();
					
					RulePreparator prep = rulePreparator.getSnapShot();
					while (classicMatches.hasNext()) {
						Match classicMatch = classicMatches.next();
						matches.add(new VariabilityAwareMatch(classicMatch,
								matchingInfo.getAssumedTrue(), rule, prep));
					}
				}
				matchingInfo.getMatchedSubrules().add(reducedRule);

			}
			rulePreparator.undo();

		}
		// Otherwise, analyse all of the remaining presence conditions,
		else {
			findMatches(rule, matchingInfo, baseMatches, matches);
		}

		// clean up
		matchingInfo.setAll(newImplicated, true, null);
		matchingInfo.setAll(newContradictory, false, null);
		return matches;
	}

	private boolean determineInjectiveMatching(MatchingInfo matchingInfo) {
		return (FeatureExpression.contradicts(ruleInfo.getInjectiveMatching(),
				getKnowledgeBase(matchingInfo)));
	}

	private Set<FeatureExpr> getNewContradictory(MatchingInfo mo) {
		Set<FeatureExpr> result = new HashSet<FeatureExpr>();
		FeatureExpr knowledge = getKnowledgeBase(mo);
		for (FeatureExpr e : mo.getNeutrals())
			if (FeatureExpression.contradicts(knowledge, e))
				result.add(e);
		return result;
	}

	private Set<FeatureExpr> getNewImplicated(MatchingInfo mo) {
		Set<FeatureExpr> result = new HashSet<FeatureExpr>();
		FeatureExpr knowledge = getKnowledgeBase(mo);
		for (FeatureExpr e : mo.getNeutrals())
			if (FeatureExpression.implies(knowledge, e))
				result.add(e);
		return result;
	}

	private FeatureExpr getKnowledgeBase(MatchingInfo mo) {
		FeatureExpr fe = FeatureExpression.true_;
		for (FeatureExpr t : mo.getAssumedTrue())
			fe = FeatureExpression.and(fe, t);
		for (FeatureExpr f : mo.getAssumedFalse())
			fe = FeatureExpression.andNot(fe, f);
		return fe;
	}

	private boolean presenceConditionEmpty(GraphElement elem) {
		String presenceCondition = elem.getPresenceCondition();
		return (presenceCondition == null) || presenceCondition.isEmpty();
	}

	class RuleInfo {
		Rule rule;
		Map<String, FeatureExpr> usedExpressions;
		Map<FeatureExpr, Set<GraphElement>> pc2elem;
		Map<Node, Set<Mapping>> node2Mapping;
		FeatureExpr featureModel;
		FeatureExpr injectiveMatching;

		public RuleInfo(Rule rule) {
			this.rule = rule;
			this.featureModel = FeatureExpression.getExpr(rule
					.getFeatureModel());
			String injective = rule
					.getInjectiveMatchingPresenceCondition();
			if (injective == null)
				injective = rule.isInjectiveMatching()+"";
			this.injectiveMatching = FeatureExpression.getExpr(injective); 
			populateMaps();
		}

		public Map<FeatureExpr, Set<GraphElement>> getPc2Elem() {
			return pc2elem;
		}

		public Map<String, FeatureExpr> getExpressions() {
			return usedExpressions;
		}

		public FeatureExpr getFeatureModel() {
			return featureModel;
		}

		public void populateMaps() {
			usedExpressions = new HashMap<String, FeatureExpr>();
			node2Mapping = new HashMap<Node, Set<Mapping>>();
			pc2elem = new HashMap<FeatureExpr, Set<GraphElement>>();
			TreeIterator<EObject> it = rule.eAllContents();
			while (it.hasNext()) {
				EObject o = it.next();
				if (o instanceof Node || o instanceof Edge
						|| o instanceof Attribute) {
					GraphElement g = (GraphElement) o;
					if (!presenceConditionEmpty(g)) {
						String pc = g.getPresenceCondition();
						FeatureExpr expr = FeatureExpression.getExpr(pc);
						usedExpressions.put(pc, expr);
						if (!pc2elem.containsKey(expr))
							pc2elem.put(expr, new HashSet<GraphElement>());
						pc2elem.get(expr).add(g);
					}
				}
				if (o instanceof Mapping) {
					Mapping m = (Mapping) o;

					Node image = m.getImage();
					Set<Mapping> set = node2Mapping.get(image);
					if (set == null) {
						set = new HashSet<Mapping>();
						node2Mapping.put(image, set);
					}
					set.add(m);
					Node origin = m.getOrigin();
					set = node2Mapping.get(origin);
					if (set == null) {
						set = new HashSet<Mapping>();
						node2Mapping.put(origin, set);
					}
					set.add(m);
				}
			}

			if (featureModel != null && !featureModel.equals("")) {
				if (!pc2elem.containsKey(featureModel))
					pc2elem.put(featureModel, new HashSet<GraphElement>());
			}
		}

		public Map<Node, Set<Mapping>> getNode2Mapping() {
			return node2Mapping;
		}

		public FeatureExpr getInjectiveMatching() {
			return injectiveMatching;
		}

	}

	class MatchingInfo {
		private Map<FeatureExpr, Boolean> info = new LinkedHashMap<FeatureExpr, Boolean>();
		private Set<FeatureExpr> assumedTrue = new HashSet<FeatureExpr>();
		private Set<FeatureExpr> assumedFalse = new HashSet<FeatureExpr>();
		private Set<FeatureExpr> neutrals = new HashSet<FeatureExpr>();
		private Set<BitSet> matchedSubRules = new HashSet<BitSet>();

		public MatchingInfo(List<FeatureExpr> conditions, RuleInfo ruleInfo) {
			for (FeatureExpr expr : conditions) {
				info.put(expr, null);
			}
			assumedTrue.add(ruleInfo.getFeatureModel());
			neutrals.addAll(conditions);
		}

		public Set<BitSet> getMatchedSubrules() {
			return matchedSubRules;
		}

		public void setAll(Collection<FeatureExpr> exprs, Boolean old,
				Boolean new_) {
			for (FeatureExpr expr : exprs) {
				set(expr, old, new_);
			}
		}

		public void set(FeatureExpr expr, Boolean old, Boolean new_) {
			if (old == null) {
				neutrals.remove(expr);
			} else {
				if (old)
					assumedTrue.remove(expr);
				else
					assumedFalse.remove(expr);
			}

			if (new_ == null) {
				neutrals.add(expr);
			} else {
				if (new_)
					assumedTrue.add(expr);
				else
					assumedFalse.add(expr);
			}

			info.put(expr, new_);
		}

		public Set<FeatureExpr> getAssumedTrue() {
			return assumedTrue;
		}

		public Set<FeatureExpr> getAssumedFalse() {
			return assumedFalse;
		}

		public Map<FeatureExpr, Boolean> getInfo() {
			return info;
		}

		public Set<FeatureExpr> getNeutrals() {
			return neutrals;
		}
	}

}
