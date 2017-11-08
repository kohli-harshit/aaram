package model;

import java.util.List;

public class Swagger {

	private String title;
	private List<Section> sectionList;

	/**
	 * return the swagger document title.
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * return the details about all availablle section in swagger document.
	 * @return
	 */
	public List<Section> getSectionList() {
		return sectionList;
	}

	public void setSectionList(List<Section> sectionList) {
		this.sectionList = sectionList;
	}

}
