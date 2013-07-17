package com.gentics.cr.util;

import java.util.Comparator;

import com.gentics.api.lib.datasource.Datasource;
<<<<<<< HEAD
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * A Comparator for sorting collections of Resolvables by a given Sorting.
 * @author Christopher
 *
 * @param <T>
 */
=======
import com.gentics.api.lib.resolving.Resolvable;

>>>>>>> cca15a7... moved sorting comparator to core package
public class PNSortingComparator<T extends Resolvable> implements Comparator<Resolvable> {

	private String columnName;
	private int sortOrder;
	
<<<<<<< HEAD
	/**
	 * Constructor with column name and sort order (Datasource.SORTORDER_DESC/Datasource.SORTORDER_ASC).
	 * @param columnName
	 * @param sortOrder
	 */
=======
>>>>>>> cca15a7... moved sorting comparator to core package
	public PNSortingComparator(String columnName, int sortOrder) {
		this.columnName = columnName;
		this.sortOrder = sortOrder;
	}
	
<<<<<<< HEAD
	/**
	 * Constructor with Sorting.
	 * @param sorting
	 */
	public PNSortingComparator(Sorting sorting) {
		this.columnName = sorting.getColumnName();
		this.sortOrder = sorting.getSortOrder();
	}
	
=======
>>>>>>> cca15a7... moved sorting comparator to core package
	
	@Override
	public int compare(Resolvable o1, Resolvable o2) {
		Comparable key1 = (Comparable) o1.get(this.columnName);
		Comparable key2 = (Comparable) o2.get(this.columnName);
		if (this.sortOrder == Datasource.SORTORDER_DESC) {
			return key2.compareTo(key1);
		} else {
			return key1.compareTo(key2);
		}
	}

}
