package car.io.application;

public class NavMenuItem {

	private int id;
	private boolean enabled;
	private String title;
	private String subtitle;
	private int iconRes;
	
	public NavMenuItem(int id, String title, int iconRes){
		this(id,title,"",iconRes,true);
	}
	
	public NavMenuItem(int id, String title, String subtitle, int icon) {
		this.id = id;
		this.title = title;
		this.subtitle = subtitle;
		this.iconRes = icon;
	}
	
	public NavMenuItem(int id,String title, String subtitle, int iconRes, boolean enabled){
		this(id,title,subtitle,iconRes);
		setEnabled(enabled);
	}
	
	public int getIconRes(){
		return iconRes;
	}
	
	public void setIconRes(int iconRes){
		this.iconRes = iconRes;
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
