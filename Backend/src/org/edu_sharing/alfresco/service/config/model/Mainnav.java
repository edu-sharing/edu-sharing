package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Mainnav implements Serializable {
	@XmlElement	public Icon icon;
	@XmlElement	public String mainMenuStyle;
}
