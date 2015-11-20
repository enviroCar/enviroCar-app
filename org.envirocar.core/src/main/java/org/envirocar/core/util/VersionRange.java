/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.util;

/**
 * This class represents a range of version numbers following
 * semantic versioning ("major.minor.bugfix") pattern.
 * 
 * @author matthes rieke
 *
 */
public class VersionRange {

	private Version minimum;
	private Version maximum;
	private boolean minimumIncluded;
	private boolean maximumIncluded;

	private VersionRange(Version min, Version max,
						 boolean minInclude, boolean maxInclude) {
		this.minimum = min;
		this.maximum = max;
		this.minimumIncluded = minInclude;
		this.maximumIncluded = maxInclude;
	}

	/**
	 * Parses a Version range string (e.g. "[0.2, 12.3.2)")
	 * into its object representation.
	 * 
	 * @param ver the range string
	 * @return the instance object
	 */
	public static VersionRange fromString(String ver) {
		boolean minInclude = includeToBoolean(ver.charAt(0));
		boolean maxInclude = includeToBoolean(ver.charAt(ver.length()-1));
		
		String[] split = ver.trim().substring(1, ver.length()-1).split(",");
		
		if (split.length != 2) throw new IllegalArgumentException("Invalid version count.");
		
		return new VersionRange(Version.fromString(split[0]), Version.fromString(split[1]),
				minInclude, maxInclude);
	}
	
	private static boolean includeToBoolean(char c) {
		if (c == '(' || c == ')') {
			return false;
		}
		if (c == '[' || c == ']') {
			return true;
		}
		
		throw new IllegalArgumentException("Invalid surronding character of range: "+c);
	}

	/**
	 * @return the maximum version of this range
	 */
	public Version getMaximum() {
		return this.maximum;
	}

	/**
	 * @return the minimum version of this range
	 */
	public Version getMinimum() {
		return this.minimum;
	}

	/**
	 * @return true if the maximum is included within this range ('<=')
	 */
	public boolean isMaximumIncluded() {
		return this.maximumIncluded;
	}

	/**
	 * @return true if the minimum is included within this range ('>=')
	 */
	public boolean isMinimumIncluded() {
		return this.minimumIncluded;
	}
	
	/**
	 * @param v the version to check
	 * @return true if the provided version is within this range
	 */
	public boolean isInRange(Version v) {
		if (v.compareTo(this.minimum) == 0 && minimumIncluded) {
			return true;
		}
		
		if (v.compareTo(this.maximum) == 0 && maximumIncluded) {
			return true;
		}
		
		if (v.compareTo(this.minimum) > 0 && v.compareTo(this.maximum) < 0) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * A class representing a Version following the semantic
	 * versioning pattern ("major.minor.bugfix").
	 * 
	 * @author matthes rieke
	 *
	 */
	public static class Version implements Comparable<Version> {
		
		private int major;
		private int minor;
		private int fix;
		private boolean snapshot;
		
		private Version(int ma, int mi, int fi) {
			this(ma, mi, fi, false);
		}
		
		private Version(int ma, int mi, int fi, boolean snap) {
			this.major = ma;
			this.minor = mi;
			this.fix = fi;
			this.snapshot = snap;
		}
		
		
		/**
		 * Takes a String (e.g. "0.3.1") and creates a corresponding
		 * object.
		 * 
		 * @param ver the version as a string
		 * @return the version instance
		 */
		public static Version fromString(String ver) {
			String[] split = ver.split("\\.");
			
			int ma = 0;
			if (split.length > 0) {
				ma = Integer.parseInt(split[0].trim());
			}
			
			int mi = 0;
			if (split.length > 1) {
				mi = Integer.parseInt(split[1].trim());
			}
			
			int fi = 0;
			boolean snap = false;
			if (split.length > 2) {
				if (split[2].contains("-")) {
					fi = Integer.parseInt(split[2].substring(0, split[2].indexOf("-")).trim());
					snap = true;
				}
				else {
					fi = Integer.parseInt(split[2].trim());
				}
			}
			
			return new Version(ma, mi, fi, snap);
		}

		public int getMajor() {
			return major;
		}

		public int getMinor() {
			return minor;
		}

		public int getFix() {
			return fix;
		}

		public boolean isSnapshot() {
			return snapshot;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.major);
			sb.append(".");
			sb.append(this.minor);
			sb.append(".");
			sb.append(this.fix);
			if (this.snapshot) {
				sb.append("-SNAPSHOT");
			}
			return sb.toString();
		}

		@Override
		public int compareTo(Version that) {
			if (this.major == that.major && this.minor == that.minor
					&& this.fix == that.fix) {
				return 0;
			}
			
			if (this.major > that.major) {
				return 1;
			}
			
			if (this.major == that.major && this.minor > that.minor) {
				return 1;
			}
			
			if (this.major == that.major && this.minor == that.minor && this.fix > that.fix) {
				return 1;
			}
			
			return -1;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null || o.getClass() != this.getClass()) {
				return false;
			}
			
			Version that = (Version) o;
			
			return this.major == that.major && this.minor == that.minor &&
					this.fix == that.fix && this.snapshot == that.snapshot;
		}
		
		@Override
		public int hashCode() {
			return this.major*1000 + this.minor*100 + this.fix*10;
		}
		
	}

}
