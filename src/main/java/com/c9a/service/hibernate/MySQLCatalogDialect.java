package com.c9a.service.hibernate;

import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.util.StringHelper;

public class MySQLCatalogDialect extends MySQL5InnoDBDialect {
	
	/*
	 * (non-Javadoc)
	 * @see org.hibernate.dialect.MySQLDialect#getAddForeignKeyConstraintString(java.lang.String, java.lang.String[], java.lang.String, java.lang.String[], boolean)
	 */
	/**
	 * This method overrides default implementation to allow specific creation of the index and foreign keys for MySQL.
	 * 
	 * @param constraintName		The constraint name to add the index and foreign keys
	 * @param foreignKey			The collection of foreign keys
	 * @param referencedTable		The referenced table
	 * @param primaryKey			Collection of primary keys to reference
	 * @param referencesPrimaryKey	boolean representing if the foreign keys reference primary keys in other tables.
	 * 
	 * @see org.hibernate.dialect.MySQLDialect#getAddForeignKeyConstraintString(java.lang.String, java.lang.String[], java.lang.String, java.lang.String[], boolean)
	 */
	@Override
	public String getAddForeignKeyConstraintString(String constraintName,String[] foreignKey, String referencedTable, String[] primaryKey, boolean referencesPrimaryKey) {
		String cols = StringHelper.join(", ", foreignKey);
		String pks = StringHelper.join(", ", primaryKey);
		String statementToIssue = new StringBuffer()
		.append(" add index ")
		.append("IDX_")
		.append(constraintName)
		.append(" (")
		.append(cols)
		.append("), add constraint ")
		.append(constraintName)
		.append(" foreign key (")
		.append(cols)
		.append(") references ")
		.append(referencedTable)
		.append(" (")
		.append( pks  )
		.append(") ON DELETE CASCADE ")
		.toString(); 
		return statementToIssue;
	}
}