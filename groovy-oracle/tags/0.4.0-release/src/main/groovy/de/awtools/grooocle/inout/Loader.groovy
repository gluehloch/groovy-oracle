/*
 * $Id$
 * ============================================================================
 * Project grooocle
 * Copyright (c) 2008-2010 by Andre Winkler. All rights reserved.
 * ============================================================================
 *          GNU LESSER GENERAL PUBLIC LICENSE
 *  TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package de.awtools.grooocle.inout

import java.sql.*

import groovy.sql.Sql

import de.awtools.grooocle.*
import de.awtools.grooocle.meta.*

/**
 * Uploads data to the database.
 *
 * @author  $Author$
 * @version $Revision$ $Date$
 */
class Loader {

    def sql
    def logEnabled = false
    def log

	/**
	 * Set this property to true, if you want to delete the table before an
	 * import.
	 */ 
	def deleteTableBefore = false

    /**
     * Oracle stores the meta data in upper case. So it is a good idea to
     * define all table/column names in upper case.
     *
     * table = [
     *     new Data(tableName: 'TABLENAME', rows: [
     *         [COL_1: 'value_1', COL_2: 'value_2'],
     *         [COL_1: 'value_3', COL_2: 'value_4'],
     *         [COL_1: 'value_5', COL_2: 'value_6']
     *     ])
     * ]
     */
    def load(data) {
    	if (logEnabled) log = ""

        def omdf = new OracleMetaDataFactory()
        def tableMetaData = omdf.createOracleTable(sql, data.tableName.toUpperCase())
        sql.getConnection().setAutoCommit(false)

        if (deleteTableBefore) {
			if (logEnabled) {
			    log += "DELETE FROM ${data.tableName}"
			}
			sql.call('DELETE FROM ' + data.tableName)
		}

        data.rows.each { row ->
            def columnNames = row.keySet().join(', ') 
            def insert = "INSERT INTO ${data.tableName}(${columnNames}) VALUES("
            def index = 0
            row.each { key, value ->
            	def column = tableMetaData.columnMetaData.find { it.columnName == key.toUpperCase()}
            	def columns = row.size()
            	if (!column) {
            		throw new IllegalArgumentException("Column ${key} does not exist for table ${data.tableName}")
            	}

                if (column.isNumber()) {
                    if (value == null) {
                        insert += 'NULL'
                    } else {
                        insert += "${value}"
                    }
                } else if (column.isDate()) {
                    if (value == null) {
                        insert += 'NULL'
                    } else {
                        insert += "to_date('${value}', '${InOutUtils.ORACLE_DATE_FORMAT}')"
                    }
                } else {
                    if (value == null) {
                        insert += 'NULL'
                    } else if (value instanceof Number) {
                    	insert += "'${value}'"
                    } else {
                        def insertValue = value?.replaceAll("'", "''")
                        insert += "'${value}'"
                    }
                }
                if (index + 1 < columns) {
                    insert += ", "
                }
                index++
            }

            insert += ")"
            if (logEnabled) {
            	log += insert
            	log += data.lineSeperator
            }
            
            try {
                sql.executeInsert(insert.toString())
            } catch (SQLException ex) {
            	println "Caused by ${insert}"
            	throw ex
            }
        }
    }
   
}
