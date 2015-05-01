package org.eclipse.emf.henshin.variability.matcher;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.model.And;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.BinaryFormula;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Formula;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Not;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.variability.util.FeatureExprLibUtil;

import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprParser;

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
	private Map<Rule, BitSet> subrule2bitset;
	private RuleInfo ruleInfo;

	private static Map<Rule, RuleInfo> ruleInfoRegistry = new HashMap<Rule, RuleInfo>();

	public VariabilityAwareEngine(Rule rule, EGraph graph) {
		super();
		this.rule = rule;
		this.graph = graph;
		this.engine = new EngineImpl();
		this.subrule2bitset = new HashMap<Rule, BitSet>();
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
		Rule baseRule = getBaseRule(rule);

		Set<Match> baseMatches = new HashSet<Match>();
		baseMatches.addAll(InterpreterUtil.findAllMatches(engine, baseRule,
				graph, null));
		Set<VariabilityAwareMatch> matches = new HashSet<VariabilityAwareMatch>();
		
		// Iterator<Match> it = engine.findMatches(baseRule, graph,null
		// ).iterator();
		// for (Match m = it.next(); it.hasNext(); m = it.next())
		// baseMatches.add(m);

		if (!baseMatches.isEmpty()) {
			List<FeatureExpr> conditions = new LinkedList<FeatureExpr>();
			conditions.addAll(expressions.values());
			// Collections.sort(conditions, new ImplicationComparator());
			MatchingInfo mo = new MatchingInfo(conditions, ruleInfo);

			findMatch(rule, mo, baseMatches, matches);
		}

		return matches;
	}

	private Set<VariabilityAwareMatch> findMatch(Rule rule,
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
			VariabilityAwareMatch m = findMatchForReducedRule(rule,
					matchingInfo, baseMatches);
			if (m != null)
				matches.add(m);
		}
		// Otherwise, analyse all of the remaining presence conditions,
		else {
			findMatch(rule, matchingInfo, baseMatches, matches);
		}

		// clean up
		matchingInfo.setAll(newImplicated, true, null);
		matchingInfo.setAll(newContradictory, false, null);
		return matches;
	}

	private VariabilityAwareMatch findMatchForReducedRule(Rule rule,
			MatchingInfo matchingInfo, Set<Match> baseMatches) {
		Rule reducedRule = getReducedRuleCopy(rule,
				matchingInfo.getAssumedFalse());
		// The following check ensures that we will not match the same sub-rule
		// twice.
		if (!matchingInfo.getMatchedSubrules().contains(
				subrule2bitset.get(reducedRule))) {
			VariabilityAwareMatch m = createMatch(reducedRule,
					matchingInfo.getAssumedTrue(), baseMatches);
			matchingInfo.getMatchedSubrules().add(
					subrule2bitset.get(reducedRule));

			if (m != null)
				return m;
		}
		return null;
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

	private Set<FeatureExpr> getNewContradictory(MatchingInfo mo) {
		Set<FeatureExpr> result = new HashSet<FeatureExpr>();
		FeatureExpr knowledge = getKnowledgeBase(mo);
		for (FeatureExpr e : mo.getNeutrals())
			if (ExprInfo.contradicts(knowledge, e))
				result.add(e);
		return result;
	}

	private Set<FeatureExpr> getNewImplicated(MatchingInfo mo) {
		Set<FeatureExpr> result = new HashSet<FeatureExpr>();
		FeatureExpr knowledge = getKnowledgeBase(mo);
		for (FeatureExpr e : mo.getNeutrals())
			if (ExprInfo.implies(knowledge, e))
				result.add(e);
		return result;
	}

	private FeatureExpr getKnowledgeBase(MatchingInfo mo) {
		FeatureExpr fe = ExprInfo.true_;
		for (FeatureExpr t : mo.getAssumedTrue())
			fe = ExprInfo.and(fe, t);
		for (FeatureExpr f : mo.getAssumedFalse())
			fe = ExprInfo.andNot(fe, f);
		return fe;
	}

	private VariabilityAwareMatch createMatch(Rule reducedRule,
			Set<FeatureExpr> trueConditions, Set<Match> baseMatches) {
		for (Match bm : baseMatches) {
			Iterable<Match> classicMatches = engine.findMatches(reducedRule,
					graph, createMatch(reducedRule, bm));
			if (classicMatches.iterator().hasNext()) {
				Match classicMatch = classicMatches.iterator().next();
				return new VariabilityAwareMatch(classicMatch, trueConditions, reducedRule);
			}

		}
		return null;
	}

	private Match createMatch(Rule reducedRule, Match bm) {
		Match result = new MatchImpl(reducedRule);
		Map<EObject, EObject> mappingsBaseToMain = ruleInfo
				.getMappingsSubrule2Mainrule().get(bm.getRule());
		Copier mappingsMainToReduced = ruleInfo.getMappingsMainrule2Subrule()
				.get(reducedRule);
		for (Node nBase : bm.getRule().getLhs().getNodes()) {
			Node nReduced = (Node) mappingsMainToReduced.get(mappingsBaseToMain
					.get(nBase));
			result.setNodeTarget(nReduced, bm.getNodeTarget(nBase));
		}
		return result;
	}

	/**
	 * Creates a copy of the given rule, graph elements with presence conditions
	 * are removed.
	 * 
	 * @param rule
	 * @param rejected
	 * @return
	 */
	private Rule getBaseRule(Rule rule) {
		return getReducedRuleCopy(rule, ruleInfo.getPc2Elem().keySet());
	}

	/**
	 * Creates a copy of the given rule. Graph elements annotated with any of
	 * the provided presence conditions are removed.
	 * 
	 * @param rule
	 * @param rejected
	 * @return
	 */
	private Rule getReducedRuleCopy(Rule rule, Set<FeatureExpr> rejected) {
		Copier copier = new Copier();
		EObject copy = copier.copy(rule);
		copier.copyReferences();
		@SuppressWarnings("unchecked")
		Rule result = (Rule) copy;

		Set<Node> deleteNodes = new HashSet<Node>();
		Set<Edge> deleteEdges = new HashSet<Edge>();
		Set<Attribute> deleteAttributes = new HashSet<Attribute>();
		Set<NestedCondition> deleteNestedConditions = new HashSet<NestedCondition>();
		for (FeatureExpr expr : rejected) {
			for (GraphElement ge : ruleInfo.getPc2Elem().get(expr)) {
				if (ge instanceof Node) {
					deleteNodes.add((Node) copier.get(ge));
				} else if (ge instanceof Edge) {
					deleteEdges.add((Edge) copier.get(ge));
				} else if (ge instanceof Attribute) {
					deleteAttributes.add((Attribute) copier.get(ge));
				}

			}
		}

		BitSet bitset = getAsBitSet(result, deleteAttributes, deleteNodes,
				deleteEdges, deleteNestedConditions);
		subrule2bitset.put(result, bitset);

		Map<EObject, EObject> backwardsMap = new HashMap<EObject, EObject>();
		ruleInfo.getMappingsSubrule2Mainrule().put(result, backwardsMap);
		ruleInfo.getMappingsMainrule2Subrule().put(result, copier);
		for (EObject o : copier.keySet()) {
			if (o instanceof GraphElement)
				backwardsMap
						.put((GraphElement) copier.get(o), (GraphElement) o);
		}

		for (NestedCondition nc : deleteNestedConditions) {
			removeNestedCondition(nc);
		}
		for (Attribute a : deleteAttributes)
			a.getNode().getAttributes().remove(a);
		for (Edge e : deleteEdges) {
			e.getGraph().getEdges().remove(e);
			e.getSource().getOutgoing().remove(e);
			e.getTarget().getIncoming().remove(e);
		}
		for (Node n : deleteNodes)
			n.getGraph().getNodes().remove(n);
		result.setCheckDangling(false);
		return result;
	}

	private void removeNestedCondition(NestedCondition nc) {
		EStructuralFeature containingFeature = nc.eContainingFeature();
		EObject container = nc.eContainer();
		if (container instanceof Not) {
			containingFeature = container.eContainingFeature();
			container = container.eContainer();
		}
		
		if (container instanceof And) {
			// replace containing 'and' by the other side of the conjunction
			And and = (And) container;
			Formula child = (and.getLeft() == nc || and.getLeft() == nc.eContainer()) ? and.getRight() : and
					.getLeft();
			and.eContainer().eSet(and.eContainingFeature(), child);
		} else if (container instanceof Graph) {
			((Graph) container).eUnset(containingFeature);
		} else {
			// TODO: Only AND-based nesting of applications conditions supported yet.
		}		
	}
	

	private BitSet getAsBitSet(Rule rule, Set<Attribute> deleteAttributes,
			Set<Node> deleteNodes, Set<Edge> deleteEdges,
			Set<NestedCondition> deleteNestedConditions) {
		BitSet result = new BitSet(rule.getLhs().getNodes().size()
				+ rule.getLhs().getEdges().size()
				+ rule.getLhs().getNestedConditions().size());
		int i = 0;
		for (Node n : rule.getLhs().getNodes()) {
			if (deleteNodes.contains(n))
				result.set(i, false);
			else
				result.set(i, true);
			i++;

			for (Attribute a : n.getAttributes()) {
				if (deleteAttributes.contains(a))
					result.set(i, false);
				else
					result.set(i, true);
				i++;
			}
		}

		for (Edge e : rule.getLhs().getEdges()) {
			if (deleteEdges.contains(e))
				result.set(i, false);
			else
				result.set(i, true);
			i++;
		}

		for (Node n : rule.getRhs().getNodes()) {
			if (deleteNodes.contains(n))
				result.set(i, false);
			else
				result.set(i, true);
			i++;

			for (Attribute a : n.getAttributes()) {
				if (deleteAttributes.contains(a))
					result.set(i, false);
				else
					result.set(i, true);
				i++;
			}
		}

		for (Edge e : rule.getRhs().getEdges()) {
			if (deleteEdges.contains(e))
				result.set(i, false);
			else
				result.set(i, true);
			i++;
		}

		for (NestedCondition nc : rule.getLhs().getNestedConditions()) {
			if (deleteNestedConditions.contains(nc))
				result.set(i, false);
			else
				result.set(i, true);
			i++;
		}
		return result;
	}

	private boolean presenceConditionEmpty(GraphElement elem) {
		String presenceCondition = elem.getPresenceCondition();
		return (presenceCondition == null) || presenceCondition.isEmpty();
	}

	class ImplicationComparator implements Comparator<FeatureExpr> {

		@Override
		public int compare(FeatureExpr o1, FeatureExpr o2) {
			if (ExprInfo.implies(o1, o2))
				return -1;
			if (ExprInfo.implies(o2, o1))
				return 1;
			else
				return 0;
		}

	}

	class RuleInfo {
		Rule rule;

		Map<String, FeatureExpr> usedExpressions;

		FeatureExpr featureModel;

		public FeatureExpr getFeatureModel() {
			return featureModel;
		}

		Map<Rule, Map<String, FeatureExpr>> featureMaps;

		Map<FeatureExpr, Set<GraphElement>> pc2elem;

		Map<Rule, Copier> mappingsMainrule2Subrule;

		Map<Rule, Map<EObject, EObject>> mappingsSubrule2Mainrule;

		public RuleInfo(Rule rule) {
			this.rule = rule;
			this.mappingsMainrule2Subrule = new HashMap<Rule, Copier>();
			this.mappingsSubrule2Mainrule = new HashMap<Rule, Map<EObject, EObject>>();
			populateMaps();
		}

		public Map<FeatureExpr, Set<GraphElement>> getPc2Elem() {
			return pc2elem;
		}

		public Map<String, FeatureExpr> getExpressions() {
			return usedExpressions;
		}

		public Map<Rule, Copier> getMappingsMainrule2Subrule() {
			return mappingsMainrule2Subrule;
		}

		public Map<Rule, Map<EObject, EObject>> getMappingsSubrule2Mainrule() {
			return mappingsSubrule2Mainrule;
		}

		public void populateMaps() {
			usedExpressions = new HashMap<String, FeatureExpr>();

			featureModel = ExprInfo.getExpr(rule.getFeatureModel());
			pc2elem = new HashMap<FeatureExpr, Set<GraphElement>>();
			TreeIterator<EObject> it = rule.eAllContents();

			for (EObject o = it.next(); it.hasNext(); o = it.next()) {
				if (o instanceof Node || o instanceof Edge
						|| o instanceof Attribute) {
					GraphElement g = (GraphElement) o;
					if (!presenceConditionEmpty(g)) {
						String pc = g.getPresenceCondition();
						FeatureExpr expr = ExprInfo.getExpr(pc);
						usedExpressions.put(pc, expr);
						if (!pc2elem.containsKey(expr))
							pc2elem.put(expr, new HashSet<GraphElement>());
						pc2elem.get(expr).add(g);
					}
				}
			}

			if (featureModel != null && !featureModel.equals("")) {
				if (!pc2elem.containsKey(featureModel))
					pc2elem.put(featureModel, new HashSet<GraphElement>());
			}
		}
	}

}
