package logic.scripter;

import java.util.List;

public class Metric {

	private String name;
	private List<String> values;

	public Metric(String name, List<String> values) {
		this.name = name;
		this.values = values;
	}

	public String getName() {
		return this.name;
	}

	public List<String> getValues() {
		return this.values;
	}

	public int getSize() {
		return this.values.size();
	}

	@Override
	public String toString() {
		// Returns c(value, value, value...) type string
		StringBuilder res = new StringBuilder(this.name).append(" <- c(");
		for (int i = 0; i < this.getSize() - 1; i++) {
			res.append(this.values.get(i)).append(",");
		}
		res.append(this.values.get(this.getSize() - 1)).append(")");
		return res.toString();
	}
}
