package env;

/**
 * Types of operations available to the agents.
 */
enum OperationType {
	SELL, BUY
};

class Operation implements Comparable<Operation> {

	/** Operation id generator */
	static private int idGenerator = 1;

	private int id;

	private OperationType type;
	private String agentName;
	private int agentIdOperation;

	private int shares;
	private double price;

	Operation(OperationType type, String agentName, int agentIdOperation, int shares, double price) {
		// Assign operation id
		synchronized (Operation.class) {
			id = idGenerator;
			if (idGenerator == Integer.MAX_VALUE)
				idGenerator = 1;
			else
				idGenerator++;
		}

		this.type = type;
		this.agentName = agentName;
		this.agentIdOperation = agentIdOperation;
		this.shares = shares;
		this.price = price;
	}

	public int compareTo(Operation o) {
		double comparationValue = this.price - o.price;

		if (this.type == OperationType.SELL) {
			if (comparationValue < 0)
				return -1;
			else if (comparationValue > 0)
				return +1;
			else
				return this.agentIdOperation - o.agentIdOperation;
		} else {
			if (comparationValue < 0)
				return +1;
			else if (comparationValue > 0)
				return -1;
			else
				return this.agentIdOperation - o.agentIdOperation;
		}
	}

	public int getId() {
		return id;
	}

	public String getAgentName() {
		return agentName;
	}

	public int getAgentIdOperation() {
		return agentIdOperation;
	}

	public double getPrice() {
		return price;
	}

	public int getShares() {
		return shares;
	}

	public void setShares(int shares) {
		this.shares = shares;
	}

	public String toString() {
		return "[" + id + ", " + type + ", " + agentName + ", " + agentIdOperation + ", " + shares + ", " + price + "]";
	}
}
