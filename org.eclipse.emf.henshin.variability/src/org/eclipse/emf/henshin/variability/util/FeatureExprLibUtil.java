package org.eclipse.emf.henshin.variability.util;

import scala.util.matching.Regex;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprParser;

public class FeatureExprLibUtil {

	public static FeatureExprParser createPresenceConditionParser() {
		return new FeatureExprParser(FeatureExprParser.init$default$1()) {
			@Override
			public Regex ID() {
				return new Regex("[A-Za-z0-9\\.]*", null);
			}
		};
	}


	public static java.util.Set<String> getDistinctFeatures(FeatureExpr expr) {
		return scala.collection.JavaConversions.asJavaSet(expr.collectDistinctFeatures());
	}
}
