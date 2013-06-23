package car.io.application;

public class NavMenuItem {

	private int id;
	private boolean enabled;
	private String title;
	private String subtitle;
	
	public NavMenuItem(int id, String title){
		this.id = id;
		this.title = title;
		this.subtitle = "";
		this.enabled = true;
	}
	
	public NavMenuItem(int id, String title, String subtitle) {
		this.id = id;
		this.title = title;
		this.subtitle = subtitle;
	}
	
	public NavMenuItem(int id,String title, String subtitle, boolean enabled){
		this(id,title,subtitle);
		setEnabled(enabled);
	}
	
	public int getId(){
		return id;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}
	
	
	
}
