package nl.weeaboo.vn.impl.nvlist;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.weeaboo.common.StringUtil;
import nl.weeaboo.vn.RenderCommand;

public class RenderStats {

	private final CommandStats[] cmdStats;
	private int framesRendered;
	
	public RenderStats() {
		cmdStats = new CommandStats[256]; //Command ID is a byte, so max 256 possibilities
	}
	
	//Functions
	public void startRender() {
	}
	public void stopRender() {
	}
	public void onFrameRenderDone() {
		framesRendered++;
		if ((framesRendered & 0xFF) == 0) {
			System.out.println(this);
		}
		
		Arrays.fill(cmdStats, null);		
	}
	
	public void log(RenderCommand cmd, long durationNanos) {
		CommandStats stats = cmdStats[cmd.id & 0xFF];
		if (stats == null) {
			cmdStats[cmd.id & 0xFF] = stats = new CommandStats();
		}		
		stats.addRun(cmd, durationNanos);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int n = 0; n < cmdStats.length; n++) {
			if (cmdStats[n] != null) {
				if (sb.length() > 0) sb.append('\n');
				sb.append(String.format("ID%03d %s", n, cmdStats[n].toString()));
			}
		}
		return sb.toString();
	}
	
	//Getters
	
	//Setters
	
	//Inner Classes
	private static class CommandStats {
	
		private int count;
		private long durationNanos;
		
		public CommandStats() {			
		}
		
		public void addRun(RenderCommand cmd, long durationNanos) {
			this.count++;
			this.durationNanos += durationNanos;
		}
				
		@Override
		public String toString() {
			return String.format("[%03dx] %s", count, StringUtil.formatTime(durationNanos, TimeUnit.NANOSECONDS));
		}
	}
	
}
