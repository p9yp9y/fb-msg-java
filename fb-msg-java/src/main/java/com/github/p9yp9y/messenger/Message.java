package com.github.p9yp9y.messenger;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Message {
	public enum Type {
		IN, OUT;
	}

	private String body;
	private String dateString;
	private Type type;

	public String getBody() {
		return body;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getDateString() {
		return dateString;
	}

	public void setDateString(final String dateString) {
		this.dateString = dateString;
	}

	public Type getType() {
		return type;
	}

	public void setType(final Type type) {
		this.type = type;
	}
}
