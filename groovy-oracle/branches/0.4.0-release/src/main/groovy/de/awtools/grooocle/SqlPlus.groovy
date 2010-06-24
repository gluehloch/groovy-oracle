/*
 * $Id$
 * ============================================================================
 * Project grooocle
 * Copyright (c) 2008-2009 by Andre Winkler. All rights reserved.
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

package de.awtools.grooocle

/**
 * Wrapper for calling Oracle´s SQL*Plus command line tool.
 * 
 * @author  $Author$
 * @version $Revision$ $Date$
 */
class SqlPlus {

	static final def LINE_SEPARATOR = System.getProperty('line.separator');

	def sqlplusExecutable = 'sqlplus'
	def user
	def password
	def script
	def tnsName
	def dir

	/** Output StringBuffer. */
	def sout

	/** Error-Output as StringBuffer. */
	def serr

	/**
	 * Executes a sql script with SQL*Plus.
	 * 
	 * @return Returns 0, if everything was fine. 
	 */
    def start() {
		def ant = new AntBuilder()
		ant.exec(outputproperty: "cmdOut",
			errorproperty: "cmdErr",
		    resultproperty:"cmdExit",
		    failonerror: "true",
		    dir: "${dir}",
		    executable: "${sqlplusExecutable}") {
		        arg(line: "${user}/${password}@${tnsName} @${script}")
		    }

		serr = ant.project.properties.cmdErr
		sout = ant.project.properties.cmdOut

		return ant.project.properties.cmdExit
    }

}
