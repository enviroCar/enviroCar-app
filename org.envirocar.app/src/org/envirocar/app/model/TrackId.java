package org.envirocar.app.model;

public class TrackId {

	private long id;
	
	public TrackId(long i) {
		this.id = i;
	}
	
	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return Long.toString(id);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof TrackId) {
			return (this.getId() == ((TrackId) o).getId());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (int) this.id;
	}
}
