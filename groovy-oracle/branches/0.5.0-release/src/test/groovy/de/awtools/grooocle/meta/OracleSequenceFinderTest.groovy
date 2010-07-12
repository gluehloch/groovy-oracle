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

package de.awtools.grooocle.meta

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test
/**
 * Test für die Klasse OracleSequenceFinder.
 * 
 * @author  $Author$
 * @version $Revision$ $Date$
 */
class OracleSequenceFinderTest extends TestDatabaseUtility {

    @Test
    void testOracleSequenceFinder() {
    	def oracleSequenceFinder = new OracleSequenceFinder()
    	def sequences = oracleSequenceFinder.getSequences(sql)
    	assert sequences.contains('XXX_SQ')
    }

	@BeforeClass
	static void beforeClass() {
		TestDatabaseUtility.beforeClass()
	}

	@AfterClass
	static void afterClass() {
		TestDatabaseUtility.afterClass()
	}

}
