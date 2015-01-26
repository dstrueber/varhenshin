package org.eclipse.emf.henshin.variability;

import java.util.ArrayList;
import java.util.List;

/**
 * Log being recorded during variability-aware matching.
 * 
 * @author Daniel Strüber
 *
 */
public class MatchingLog {
	static List<MatchingLogEntry> entries = new ArrayList<MatchingLogEntry>();

	public static List<MatchingLogEntry> getEntries() {
		return entries;
	}

	public static void setEntries(List<MatchingLogEntry> entries) {
		MatchingLog.entries = entries;
	}
	
	public static String createString() {
		StringBuffer sb = new StringBuffer();
		sb.append(entries.size());
		sb.append(" entries:\n");
		int i=1;
		for (MatchingLogEntry entry :entries) {
			sb.append(i);
			sb.append("\t");
			sb.append(entry.getUnit().getName());

			sb.append("\t");
			sb.append(entry.isSuccessful());
			sb.append("\t");
			sb.append(entry.getGraphNodes());
			sb.append("\t");
			sb.append(entry.getGraphEdges());
			sb.append("\t");
			sb.append(entry.getRuntime());
			sb.append("\n");
			i++;
		}
		return sb.toString();
	}
}
