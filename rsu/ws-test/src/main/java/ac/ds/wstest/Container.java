package ac.ds.wstest;

public class Container {

	private Long time;
	private String cpu_share;


	public Container(Long time, String cpu_share) {
		this.time = time;
		this.cpu_share = cpu_share;
	}	


	public void set_Time(Long time) {
		this.time = time;
	}

	public void set_cpuShare(String cpu_share) {
		this.cpu_share = cpu_share;
	}


	public Long get_Time() {
		return this.time;
	}

	public String get_cpuShare() {
		return this.cpu_share;
	}

}
