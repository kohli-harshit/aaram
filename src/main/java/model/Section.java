package model;

public class Section {

	public String sectionName;
	public APIList apiList;
	public String sectionDetail;

	public String getSectionName() {
		return sectionName;
	}

	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	public String getSectionDetail() {
		return sectionDetail;
	}

	public void setSectionDetail(String sectionDetail) {
		this.sectionDetail = sectionDetail;
	}

	public APIList getApiList() {
		return apiList;
	}

	public void setApiList(APIList apiList) {
		this.apiList = apiList;
	}

}
