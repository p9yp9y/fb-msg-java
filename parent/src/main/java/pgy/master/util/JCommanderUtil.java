package pgy.master.util;

import com.beust.jcommander.JCommander;

public class JCommanderUtil {

	public static void parseArgs(Object main, String[] args) {
		JCommander build = JCommander.newBuilder().addObject(main).build();
		try {
			build.parse(args);
		} catch (Exception e) {
			build.usage();
			return;
		}
	}
}
