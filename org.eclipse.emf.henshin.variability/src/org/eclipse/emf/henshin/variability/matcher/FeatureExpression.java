package org.eclipse.emf.henshin.variability.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.henshin.variability.util.FeatureExprLibUtil;
import org.eclipse.emf.henshin.variability.util.XorEncoderUtil;

import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprParser;

/**
 * This class serves as a cache for SAT evaluation results, helping to avoid
 * performing the same computations repeatedly.
 * 
 * @author Daniel Strüber
 *
 */
class FeatureExpression {
	private static FeatureExprParser parser = FeatureExprLibUtil
			.createPresenceConditionParser();


	public static final FeatureExpr true_ = parser.parse("true");
	
	static Map<String, FeatureExpr> string2expr = new HashMap<String, FeatureExpr>();
	static Map<FeatureExpr, Map<FeatureExpr, Boolean>> implies = new HashMap<FeatureExpr, Map<FeatureExpr, Boolean>>();
	static Map<FeatureExpr, Map<FeatureExpr, Boolean>> contradicts = new HashMap<FeatureExpr, Map<FeatureExpr, Boolean>>();
	static Map<FeatureExpr, Map<FeatureExpr, FeatureExpr>> and = new HashMap<FeatureExpr, Map<FeatureExpr, FeatureExpr>>();
	static Map<FeatureExpr, Map<FeatureExpr, FeatureExpr>> andNot = new HashMap<FeatureExpr, Map<FeatureExpr, FeatureExpr>>();

	/**
	 * Does expression 1 imply expression 2?
	 * 
	 * @param expr1
	 * @param expr2
	 * @return
	 */
	public static boolean implies(FeatureExpr expr1, FeatureExpr expr2) {
		if (implies.containsKey(expr1)) {
			if (implies.get(expr1).containsKey(expr2)) {
				return implies.get(expr1).get(expr2);
			} else {
				boolean val = expr1.andNot(expr2).isContradiction();
				implies.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			implies.put(expr1, new HashMap<FeatureExpr, Boolean>());
			return implies(expr1, expr2);
		}
	}

	public static FeatureExpr and(FeatureExpr expr1, FeatureExpr expr2) {
		if (and.containsKey(expr1)) {
			if (and.get(expr1).containsKey(expr2)) {
				return and.get(expr1).get(expr2);
			} else {
				FeatureExpr val = expr1.and(expr2);
				and.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			and.put(expr1, new HashMap<FeatureExpr, FeatureExpr>());
			return and(expr1, expr2);
		}
	}

	public static FeatureExpr andNot(FeatureExpr expr1, FeatureExpr expr2) {
		if (andNot.containsKey(expr1)) {
			if (andNot.get(expr1).containsKey(expr2)) {
				return andNot.get(expr1).get(expr2);
			} else {
				FeatureExpr val = expr1.andNot(expr2);
				andNot.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			andNot.put(expr1, new HashMap<FeatureExpr, FeatureExpr>());
			return andNot(expr1, expr2);
		}
	}

	public static FeatureExpr getExpr(String condition) {
		condition = XorEncoderUtil.encodeXor(condition);
		FeatureExpr result = string2expr.get(condition);
		if (result == null) {
			result = parser.parse(condition);
			string2expr.put(condition, result);
		}
		return result;
	}

	/**
	 * Does expression 1 contradict expression 2?
	 * 
	 * @param expr1
	 * @param expr2
	 * @return
	 */
	public static boolean contradicts(FeatureExpr expr1, FeatureExpr expr2) {
		if (contradicts.containsKey(expr1)) {
			if (contradicts.get(expr1).containsKey(expr2)) {
				return contradicts.get(expr1).get(expr2);
			} else {
				boolean val = expr1.and(expr2).isContradiction();
				contradicts.get(expr1).put(expr2, val);
				return val;
			}
		} else {
			contradicts.put(expr1, new HashMap<FeatureExpr, Boolean>());
			return contradicts(expr1, expr2);
		}
	}

}
