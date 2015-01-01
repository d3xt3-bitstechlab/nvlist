package nl.weeaboo.vn.script.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.vn.script.IScriptEventDispatcher;
import nl.weeaboo.vn.script.IScriptFunction;

@LuaSerializable
public class ScriptEventDispatcher implements IScriptEventDispatcher, Serializable {

	private static final long serialVersionUID = ScriptImpl.serialVersionUID;

	private static final Comparator<Task> TASK_SORTER = new TaskSorter();

	private final List<IScriptFunction> events = new ArrayList<IScriptFunction>();
	private final List<Task> tasks = new ArrayList<Task>();

	private transient boolean tasksSorted = false;

	public ScriptEventDispatcher() {
	}

	@Override
	public void addEvent(IScriptFunction func) {
		events.add(func);
	}

	@Override
	public void addTask(IScriptFunction func, double priority) {
		tasks.add(new Task(func, priority));
		tasksSorted = false;
	}

	@Override
	public boolean removeTask(IScriptFunction func) {
		for (Iterator<Task> itr = tasks.iterator(); itr.hasNext(); ) {
			Task task = itr.next();
			if (task.matches(func)) {
				itr.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public void clear() {
		events.clear();
	}

	@Override
	public boolean isEmpty() {
		return events.isEmpty();
	}

	private void sortTasks() {
		if (tasksSorted) {
			return;
		}
		Collections.sort(tasks, TASK_SORTER);
		tasksSorted = true;
	}

	@Override
	public List<IScriptFunction> retrieveWork() {
		List<IScriptFunction> result = new ArrayList<IScriptFunction>(tasks.size() + events.size());

		// Tasks (sorted by descending priority)
		sortTasks();
		for (Task task : tasks) {
			result.add(task.function);
		}

		// Events
		for (IScriptFunction func : events) {
			result.add(func);
		}
		events.clear();

		return result;
	}

	@LuaSerializable
	private static class Task implements Serializable {

		private static final long serialVersionUID = 1L;

		private final IScriptFunction function;
		private final double priority;

		public Task(IScriptFunction function, double priority) {
			this.function = function;
			this.priority = priority;
		}

		public boolean matches(IScriptFunction func) {
			return function.equals(func);
		}

	}

	private static class TaskSorter implements Comparator<Task> {

		@Override
		public int compare(Task a, Task b) {
			return Double.compare(b.priority, a.priority); // Descending (reverse) order
		}

	}

}
