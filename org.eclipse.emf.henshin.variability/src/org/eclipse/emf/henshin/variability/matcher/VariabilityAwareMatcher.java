//
//package org.eclipse.emf.henshin.variability.matcher;
//
//import java.util.BitSet;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.eclipse.emf.common.util.TreeIterator;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
//import org.eclipse.emf.henshin.interpreter.EGraph;
//import org.eclipse.emf.henshin.interpreter.Match;
//import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
//import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
//import org.eclipse.emf.henshin.model.Attribute;
//import org.eclipse.emf.henshin.model.Edge;
//import org.eclipse.emf.henshin.model.GraphElement;
//import org.eclipse.emf.henshin.model.Node;
//import org.eclipse.emf.henshin.model.Rule;
//import org.eclipse.emf.henshin.variability.util.FeatureExprLibUtil;
//import org.eclipse.featuremodel.FeatureModel;
//
//import de.fosd.typechef.featureexpr.FeatureExpr;
//import de.fosd.typechef.featureexpr.FeatureExprParser;
//
//public class VariabilityAwareMatcher {
//
//	private Rule rule;
//
//	private EGraph graph;
//
//	private EngineImpl engine;
//
//	private Map<String, FeatureExpr> expressions;
//
//	private Map<Rule, BitSet> subrule2bitset;
//
//	private FeatureExprParser parser = FeatureExprLibUtil
//			.createPresenceConditionParser();
//
//	private ExprInfo exprInfo = new ExprInfo();
//
//	private RuleInfo ruleInfo;
//	
//	private static Map<Rule, RuleInfo> ruleInfoRegistry = new HashMap<Rule, RuleInfo>();
//
//	public VariabilityAwareMatcher(Rule rule, EGraph graph) {
//		super();
//		this.rule = rule;
//		this.graph = graph;
//		this.engine = new EngineImpl();
//		this.subrule2bitset = new HashMap<Rule, BitSet>();
//		if (!ruleInfoRegistry.containsKey(rule))
//			ruleInfoRegistry.put(rule, new RuleInfo(rule));
//		this.ruleInfo = ruleInfoRegistry.get(rule);
//		populateExpressionMap();
//	}
//
////	public VariabilityAwareMatcher(Rule rule, EGraph graph,
////			FeatureModel featureModel) {
////		this.rule = rule;
////		this.graph = graph;
////		this.featureModel = featureModel;
////		this.engine = new EngineImpl();
////		this.subrule2bitset = new HashMap<Rule, BitSet>();
////		if (!ruleInfoRegistry.containsKey(rule))
////			ruleInfoRegistry.put(rule, new RuleInfo(rule));
////		this.ruleInfo = ruleInfoRegistry.get(rule);
////		populateExpressionMap();
////	}
//
//	private void populateExpressionMap() {
//		if (ruleInfoRegistry.containsKey(rule)) {
//			expressions = ruleInfo.getExpressions();
//		}
//	}
//
//	public Set<VariabilityAwareMatch> findMatches() {
//		Set<VariabilityAwareMatch> result = new HashSet<VariabilityAwareMatch>();
//		Rule baseRule = getBaseRule(rule);
//		
//		Set<Match> baseMatches = new HashSet<Match>();
//		Iterator<Match> it = engine.findMatches(baseRule, graph,
//				new MatchImpl(baseRule)).iterator();
//		for (Match m = it.next(); it.hasNext(); m = it.next())
//			baseMatches.add(m);
//		
//		System.out.println(baseMatches.size());
//		
//		List<FeatureExpr> conditions = new LinkedList<FeatureExpr>();
//		conditions.addAll(expressions.values());
//		Collections.sort(conditions, new ImplicationComparator());
//		MatchingInfo mo = new MatchingInfo(conditions);
//
//		addMatches(result, rule, mo, baseMatches);
//		return result;
//	}
//
//	private void addMatches(Set<VariabilityAwareMatch> accumulatedMatches,
//			Rule rule, MatchingInfo matchingInfo, Set<Match> baseMatches) {
//		FeatureExpr current = getFirstNeutral(matchingInfo);
//		matchingInfo.set(current, null, true);
//		addMatchesInner(accumulatedMatches, rule, matchingInfo, baseMatches);
//		matchingInfo.set(current, true, false);
//		addMatchesInner(accumulatedMatches, rule, matchingInfo, baseMatches);
//		matchingInfo.set(current, false, null);
//	}
//
//	private FeatureExpr getFirstNeutral(MatchingInfo matchingInfo) {
//		for (FeatureExpr e : matchingInfo.getInfo().keySet()) {
//			if (matchingInfo.getInfo().get(e) == null)
//				return e;
//		}
//		return null;
//	}
//
//	private void addMatchesInner(Set<VariabilityAwareMatch> accumulatedMatches,
//			Rule rule, MatchingInfo matchingInfo, Set<Match> baseMatches) {
//		Set<FeatureExpr> newContradictory = getNewContradictory(matchingInfo);
//		matchingInfo.setAll(newContradictory, null, false);
//
//		Set<FeatureExpr> newImplicated = getNewImplicated(matchingInfo);
//		matchingInfo.setAll(newImplicated, null, true);
//
//		// If there are no presence conditions contradicting or implied by the
//		// current assignment (= neutral is empty), calculate the matches
//		// classically.
//		if (matchingInfo.getNeutrals().isEmpty()) {
//			addMatchesForReducedRule(accumulatedMatches, rule, matchingInfo, baseMatches);
//		}
//		// Otherwise, analyse all of the remaining presence conditions,
//		else {
//			addMatches(accumulatedMatches, rule, matchingInfo, baseMatches);
//		}
//
//		// clean up
//		matchingInfo.setAll(newImplicated, true, null);
//		matchingInfo.setAll(newContradictory, false, null);
//	}
//
//	private void addMatchesForReducedRule(
//			Set<VariabilityAwareMatch> accumulatedMatches, Rule rule,
//			MatchingInfo matchingInfo, Set<Match> baseMatches) {
//		Rule reducedRule = getReducedRuleCopy(rule,
//				matchingInfo.getAssumedFalse());
//		// The following check ensures that we will not match the same sub-rule twice.
//		if (!matchingInfo.getMatchedSubrules().contains(
//				subrule2bitset.get(reducedRule))) {
//			accumulatedMatches.addAll(createMatches(reducedRule,
//					matchingInfo.getAssumedTrue(), baseMatches));
//			matchingInfo.getMatchedSubrules().add(
//					subrule2bitset.get(reducedRule));
//		}
//	}
//
//	class MatchingInfo {
//		private Map<FeatureExpr, Boolean> info = new LinkedHashMap<FeatureExpr, Boolean>();
//		private Set<FeatureExpr> assumedTrue = new HashSet<FeatureExpr>();
//		private Set<FeatureExpr> assumedFalse = new HashSet<FeatureExpr>();
//		private Set<FeatureExpr> neutrals = new HashSet<FeatureExpr>();
//		private Set<BitSet> matchedSubRules = new HashSet<BitSet>();
//
//		public MatchingInfo(List<FeatureExpr> conditions) {
//			for (FeatureExpr expr : conditions) {
//				info.put(expr, null);
//			}
//			neutrals.addAll(conditions);
//		}
//
//		public Set<BitSet> getMatchedSubrules() {
//			return matchedSubRules;
//		}
//
//		public void setAll(Collection<FeatureExpr> exprs, Boolean old,
//				Boolean new_) {
//			for (FeatureExpr expr : exprs) {
//				set(expr, old, new_);
//			}
//		}
//
//		public void set(FeatureExpr expr, Boolean old, Boolean new_) {
//			if (old == null) {
//				neutrals.remove(expr);
//			} else {
//				if (old)
//					assumedTrue.remove(expr);
//				else
//					assumedFalse.remove(expr);
//			}
//
//			if (new_ == null) {
//				neutrals.add(expr);
//			} else {
//				if (new_)
//					assumedTrue.add(expr);
//				else
//					assumedFalse.add(expr);
//			}
//
//			info.put(expr, new_);
//		}
//
//		public Set<FeatureExpr> getAssumedTrue() {
//			return assumedTrue;
//		}
//
//		public Set<FeatureExpr> getAssumedFalse() {
//			return assumedFalse;
//		}
//
//		public Map<FeatureExpr, Boolean> getInfo() {
//			return info;
//		}
//
//		public Set<FeatureExpr> getNeutrals() {
//			return neutrals;
//		}
//	}
//
//	private Set<FeatureExpr> getNewContradictory(MatchingInfo mo) {
//		Set<FeatureExpr> result = new HashSet<FeatureExpr>();
//		FeatureExpr knowledge = getKnowledgeBase(mo);
//		for (FeatureExpr e : mo.getNeutrals())
//			if (exprInfo.contradicts(knowledge, e))
//				result.add(e);
//		return result;
//	}
//
//	private Set<FeatureExpr> getNewImplicated(MatchingInfo mo) {
//		Set<FeatureExpr> result = new HashSet<FeatureExpr>();
//		FeatureExpr knowledge = getKnowledgeBase(mo);
//		for (FeatureExpr e : mo.getNeutrals())
//			if (exprInfo.implies(knowledge, e))
//				result.add(e);
//		return result;
//	}
//
//	private FeatureExpr getKnowledgeBase(MatchingInfo mo) {
//		FeatureExpr fe = exprInfo.true_;
//		for (FeatureExpr t : mo.getAssumedTrue())
//			fe = exprInfo.and(fe, t);
//		for (FeatureExpr f : mo.getAssumedFalse())
//			fe = exprInfo.andNot(fe, f);
//		return fe;
//	}
//
//	private Collection<VariabilityAwareMatch> createMatches(Rule reducedRule,
//			Set<FeatureExpr> trueConditions, Set<Match> baseMatches) {
//		Set<VariabilityAwareMatch> matches = new HashSet<VariabilityAwareMatch>();
//		for (Match bm : baseMatches) {
//			Iterable<Match> classicMatches = engine.findMatches(reducedRule, graph,
//					createMatch(reducedRule, bm));
//			for (Match m : classicMatches) {
//				matches.add(new VariabilityAwareMatch(m, trueConditions));
//			}
//		}
//		return matches;
//	}
//
//	private Match createMatch(Rule reducedRule, Match bm) {
//		Match result = new MatchImpl(reducedRule);
//		Map<GraphElement, GraphElement> mappingsBaseToMain = ruleInfo.getMappingsSubrule2Mainrule().get(bm.getRule());
//		Copier mappingsMainToReduced = ruleInfo.getMappingsMainrule2Subrule().get(reducedRule);
//		for (Node nBase : bm.getRule().getLhs().getNodes()) {
//			Node nMain = (Node) mappingsBaseToMain.get(nBase);
//			Node nReduced = (Node) mappingsMainToReduced.get(mappingsBaseToMain.get(nBase));
//			result.setNodeTarget(nReduced, bm.getNodeTarget(nBase));
//		}
//		return result;
//	}
//
//	/**
//	 * Creates a copy of the given rule, graph elements with presence
//	 * conditions are removed.
//	 * 
//	 * @param rule
//	 * @param rejected
//	 * @return
//	 */
//	private Rule getBaseRule(Rule rule) {
//		return getReducedRuleCopy(rule, ruleInfo.getPc2Elem()
//				.keySet());
//	}
//
//	/**
//	 * Creates a copy of the given rule. Graph elements annotated with any
//	 * of the provided presence conditions are removed.
//	 * 
//	 * @param rule
//	 * @param rejected
//	 * @return
//	 */
//	private Rule getReducedRuleCopy(Rule rule, Set<FeatureExpr> rejected) {
//		Copier copier = new Copier();
//		EObject copy = copier.copy(rule);
//		copier.copyReferences();
//		@SuppressWarnings("unchecked")
//		Rule result = (Rule) copy;
//		
//		Set<Node> deleteNodes = new HashSet<Node>();
//		Set<Edge> deleteEdges = new HashSet<Edge>();
//		Set<Attribute> deleteAttributes = new HashSet<Attribute>();
//		for (FeatureExpr expr : rejected) {
//			for (GraphElement ge : ruleInfo.getPc2Elem().get(expr)) {
//				if (ge instanceof Node) {
//					deleteNodes.add((Node) copier.get(ge));
//				} else if (ge instanceof Edge) {
//					deleteEdges.add((Edge) copier.get(ge));
//				} else if (ge instanceof Attribute) {
//					deleteAttributes.add((Attribute) copier.get(ge));
//				}
//			}
//		}
//
//		BitSet bitset = getAsBitSet(result, deleteAttributes, deleteNodes,
//				deleteEdges);
//		subrule2bitset.put(result, bitset);
//		
//		Map<GraphElement, GraphElement> backwardsMap = new HashMap<GraphElement, GraphElement>();
//		ruleInfo.getMappingsSubrule2Mainrule().put(result, backwardsMap);
//		ruleInfo.getMappingsMainrule2Subrule().put(result, copier);
//		for (EObject o : copier.keySet()) {
//			if (o instanceof GraphElement)
//				backwardsMap.put( (GraphElement)copier.get(o) , (GraphElement) o);
//		}
//		
//		
////		for (Attribute a : deleteAttributes)
////			result.removeAttribute(a, false);
////		for (Edge e : deleteEdges)
////			result.removeEdge(e, false);
////		for (Node n : deleteNodes)
////			result.removeNode(n, false);
////		
//		for (Attribute a : deleteAttributes)
//			a.getNode().getAttributes().remove(a);
//		for (Edge e : deleteEdges) {
//			e.getGraph().getEdges().remove(e);
//			e.getSource().getOutgoing().remove(e);
//			e.getTarget().getIncoming().remove(e);
//		}
//		for (Node n : deleteNodes)
//			n.getGraph().getNodes().remove(n);
////		for (Attribute a : deleteAttributes)
////			a.getNode().getAttributes().remove(a);
////		for (Edge e : deleteEdges)
////			result.getLhs().getEdges().remove(e);
////		for (Node n : deleteNodes)
////			result.getLhs().getNodes().remove(n);
//		
//		
//		result.setCheckDangling(false);
//		return result;
//	}
//
//	private BitSet getAsBitSet(Rule rule, Set<Attribute> deleteAttributes,
//			Set<Node> deleteNodes, Set<Edge> deleteEdges) {
//		BitSet result = new BitSet(rule.getLhs().getNodes().size()
//				+ rule.getLhs().getEdges().size());
//		int i = 0;
//		for (Node n : rule.getLhs().getNodes()) {
//			if (deleteNodes.contains(n))
//				result.set(i, false);
//			else
//				result.set(i, true);
//			i++;
//
//			for (Attribute a : n.getAttributes()) {
//				if (deleteAttributes.contains(a))
//					result.set(i, false);
//				else
//					result.set(i, true);
//				i++;
//			}
//		}
//
//		for (Edge e : rule.getLhs().getEdges()) {
//			if (deleteNodes.contains(e))
//				result.set(i, false);
//			else
//				result.set(i, true);
//			i++;
//		}
//
//		return result;
//	}
//
//	private boolean presenceConditionEmpty(GraphElement elem) {
//		String presenceCondition = elem.getPresenceCondition();
//		return (presenceCondition == null) || presenceCondition.isEmpty();
//	}
//
//	class ImplicationComparator implements Comparator<FeatureExpr> {
//
//		@Override
//		public int compare(FeatureExpr o1, FeatureExpr o2) {
//			if (exprInfo.implies(o1, o2))
//				return -1;
//			if (exprInfo.implies(o2, o1))
//				return 1;
//			else
//				return 0;
//		}
//
//	}
//
//	class RuleInfo {
//		Rule rule;
//
//		Map<String, FeatureExpr> usedExpressions;
//
//		Map<Rule, Map<String, FeatureExpr>> featureMaps;
//
//		Map<FeatureExpr, Set<GraphElement>> pc2elem;
//
//		Map<Rule, Copier> mappingsMainrule2Subrule;
//		
//		Map<Rule, Map<GraphElement, GraphElement>> mappingsSubrule2Mainrule;
//
//		
//		public RuleInfo(Rule rule) {
//			this.rule = rule;
//			this.mappingsMainrule2Subrule = new HashMap<Rule, Copier>();
//			this.mappingsSubrule2Mainrule = new HashMap<Rule,  Map<GraphElement, GraphElement>>();
//			populateMaps();
//		}
//
//		public Map<FeatureExpr, Set<GraphElement>> getPc2Elem() {
//			return pc2elem;
//		}
//
//		public Map<String, FeatureExpr> getExpressions() {
//			return usedExpressions;
//		}
//
//		public Map<Rule, Copier> getMappingsMainrule2Subrule() {
//			return mappingsMainrule2Subrule;
//		}
//
//		public Map<Rule, Map<GraphElement, GraphElement>> getMappingsSubrule2Mainrule() {
//			return mappingsSubrule2Mainrule;
//		}
//
//		public void populateMaps() {
//			usedExpressions = new HashMap<String, FeatureExpr>();
//			pc2elem = new HashMap<FeatureExpr, Set<GraphElement>>();
//			TreeIterator<EObject> it = rule.eAllContents();
//			for (EObject o = it.next(); it.hasNext(); o = it.next()) {
//				if (o instanceof Node || o instanceof Edge
//						|| o instanceof Attribute) {
//					GraphElement g = (GraphElement) o;
//					if (!presenceConditionEmpty(g)) {
//						String pc = g.getPresenceCondition();
//						FeatureExpr expr = exprInfo.getExpr(pc);
//						usedExpressions.put(pc, expr);
//						if (!pc2elem.containsKey(expr))
//							pc2elem.put(expr, new HashSet<GraphElement>());
//						pc2elem.get(expr).add(g);
//					}
//				}
//			}
//		}
//	}
//
//	class ExprInfo {
//		public final FeatureExpr true_ = parser.parse("true");
//
//		Map<String, FeatureExpr> string2expr = new HashMap<String, FeatureExpr>();
//		Map<FeatureExpr, Map<FeatureExpr, Boolean>> implies = new HashMap<FeatureExpr, Map<FeatureExpr, Boolean>>();
//		Map<FeatureExpr, Map<FeatureExpr, Boolean>> contradicts = new HashMap<FeatureExpr, Map<FeatureExpr, Boolean>>();
//		Map<FeatureExpr, Map<FeatureExpr, FeatureExpr>> and = new HashMap<FeatureExpr, Map<FeatureExpr, FeatureExpr>>();
//		Map<FeatureExpr, Map<FeatureExpr, FeatureExpr>> andNot = new HashMap<FeatureExpr, Map<FeatureExpr, FeatureExpr>>();
//
//		/**
//		 * Does expression 1 imply expression 2?
//		 * 
//		 * @param expr1
//		 * @param expr2
//		 * @return
//		 */
//		public boolean implies(FeatureExpr expr1, FeatureExpr expr2) {
//			if (implies.containsKey(expr1)) {
//				if (implies.get(expr1).containsKey(expr2)) {
//					return implies.get(expr1).get(expr2);
//				} else {
//					boolean val = expr1.andNot(expr2).isContradiction();
//					implies.get(expr1).put(expr2, val);
//					return val;
//				}
//			} else {
//				implies.put(expr1, new HashMap<FeatureExpr, Boolean>());
//				return implies(expr1, expr2);
//			}
//		}
//
//		public FeatureExpr and(FeatureExpr expr1, FeatureExpr expr2) {
//			if (and.containsKey(expr1)) {
//				if (and.get(expr1).containsKey(expr2)) {
//					return and.get(expr1).get(expr2);
//				} else {
//					FeatureExpr val = expr1.and(expr2);
//					and.get(expr1).put(expr2, val);
//					return val;
//				}
//			} else {
//				and.put(expr1, new HashMap<FeatureExpr, FeatureExpr>());
//				return and(expr1, expr2);
//			}
//		}
//
//		public FeatureExpr andNot(FeatureExpr expr1, FeatureExpr expr2) {
//			if (andNot.containsKey(expr1)) {
//				if (andNot.get(expr1).containsKey(expr2)) {
//					return andNot.get(expr1).get(expr2);
//				} else {
//					FeatureExpr val = expr1.andNot(expr2);
//					andNot.get(expr1).put(expr2, val);
//					return val;
//				}
//			} else {
//				andNot.put(expr1, new HashMap<FeatureExpr, FeatureExpr>());
//				return andNot(expr1, expr2);
//			}
//		}
//
//		public FeatureExpr getExpr(String presenceCondition) {
//			FeatureExpr result = string2expr.get(presenceCondition);
//			if (result == null) {
//				result = parser.parse(presenceCondition);
//				string2expr.put(presenceCondition, result);
//			}
//			return result;
//		}
//
//		/**
//		 * Does expression 1 contradict expression 2?
//		 * 
//		 * @param expr1
//		 * @param expr2
//		 * @return
//		 */
//		public boolean contradicts(FeatureExpr expr1, FeatureExpr expr2) {
//			if (contradicts.containsKey(expr1)) {
//				if (contradicts.get(expr1).containsKey(expr2)) {
//					return contradicts.get(expr1).get(expr2);
//				} else {
//					boolean val = expr1.and(expr2).isContradiction();
//					contradicts.get(expr1).put(expr2, val);
//					return val;
//				}
//			} else {
//				contradicts.put(expr1, new HashMap<FeatureExpr, Boolean>());
//				return contradicts(expr1, expr2);
//			}
//		}
//
//	}
//
//}
