/*
 * Copyright 2010 Daniel Weisser
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.danielweisser.android.ldapsync.client;

import android.text.TextUtils;

/**
 * Data container for an address.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class Address {

	private String street = "";
	private String city = "";
	private String state = "";
	private String zip = "";
	private String country = "";

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Address) {
			Address t = (Address) o;
			if (street == null || city == null || state == null || zip == null || country == null) {
				return false;
			}
			return (street.equals(t.getStreet()) && city.equals(t.getCity()) && state.equals(t.getState()) && zip.equals(t.getZip()) && country.equals(t
					.getCountry()));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 4;
		hash = 17 * hash + (null == street ? 0 : street.hashCode());
		hash = 17 * hash + (null == city ? 0 : city.hashCode());
		hash = 17 * hash + (null == state ? 0 : state.hashCode());
		hash = 17 * hash + (null == zip ? 0 : zip.hashCode());
		hash = 17 * hash + (null == country ? 0 : country.hashCode());

		return hash;
	}

	/**
	 * Quick check for an empty address.
	 * 
	 * @return <code>true</code> if all fields are empty.
	 */
	public boolean isEmpty() {
		if (TextUtils.isEmpty(street) && TextUtils.isEmpty(city) && TextUtils.isEmpty(state) && TextUtils.isEmpty(zip) && TextUtils.isEmpty(country)) {
			return true;
		}
		return false;
	}

}
