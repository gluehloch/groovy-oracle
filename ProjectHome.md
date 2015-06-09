

# Oracle schema/data administration with Groovy #

This is an utility project to administrate an Oracle schema with Groovy. Following functions are supported:

  * Read Oracle meta informations
  * Calling of SQL\*Plus with Groovy
  * Import/Export of data
  * ...

The [Here](ReleaseNotes.md) you find the Release Notes.

Take at look OracleSchemaInfos.

## Oracle/JDBC problems ##
  * Java/JDBC Date to Oracle Timestamp conversion: OracleSqlTimestamp
  * Oracle´s PL/SQL index table and Java/JDBC: OracleIndexTable
  * Some helpful SQL scripts: HelpfulSqlScripts
  * OracleTippsAndTricks

# Examples #

## Import and Export ##

### Import of a database table ###
Import of a CSV file to a database table:
```
import de.awtools.grooocle.inout.SqlFileImporter
import de.awtools.grooocle.OraUtils

def sql = OraUtils.createSql('user', 'password', 'host:port:sid')
def loader = new SqlFileImporter(
    sql: sql,
    tableName: 'table_name',
    fileName: 'file_name',
    dateFormat: 'YYYYMMDD HH24:MI:SS',
    columnSeperator: ';'
)
loader.load()
```
The parameter `dateFormat` is optional. The format string must confirm to the Oracle `TO_DATE` function.

### Export of a database table ###
Export of a CSV file from a database table:
```
import de.awtools.grooocle.inout.SqlFileExporter
import de.awtools.grooocle.OraUtils

def sql = OraUtils.createSql('user', 'password', 'host:port:sid')
def sqlFileExporter = new SqlFileExporter(
    sql: sql,
    query: 'table_name',
    fileName: 'file_name',
    dateFormat: 'yyyyMMdd HH:mm:ss'
)
sqlFileExporter.export()
```
Here the dateFormat option must confirm to the rules of the Java class `SimpleDateFormat`.

## Meta data support ##
You find an UML overview diagram [here](http://code.google.com/p/groovy-oracle/wiki/UmlOverviewDiagram). But now the examples.

Shows all user tables with prefix 'PREFIX\_TABLENAME':
```
import de.awtools.grooocle.*
import de.awtools.grooocle.meta.*

def sql = OraUtils.createSql('test', 'test', '192.168.0.5', 1521, 'orcl')
def factory = new OracleMetaDataFactory()
def schema = factory.createOracleSchema(sql)

def tableNames = schema.tables.keySet().findAll { it.startsWith('PREFIX_TABLENAME')}
tableNames.each { println it }
```

Shows the column names of a table:
```
def table = schema.tables['TABLE_NAME']
table.columnMetaData.each { println it.columnName }
```

Shows all referenced tables of table 'TABLENAME':
```
def table = schema.tables.get('TABLENAME')
table.constraint.foreignKeys.each { println it.referencedTableName }
```

Shows all tables, which have a reference to table 'TABLENAME':
```
def fks = []
schema.tables.each { tableName, table -> 
    fks.addAll(table.constraint.foreignKeys.findAll {
        it.referencedTableName == 'TABLENAME'
    })
}
fks.each { fk -> print "${fk} -> ${fk.referencedTableName}; " }
```

The new 0.4.0 release supports a little shorter syntax. The first line demonstrates the usage of the new grape annotation of Groovy 1.7.0:
```
@Grab(group='de.awtools', module='grooocle', version='0.4.0')

import de.awtools.grooocle.*
import de.awtools.grooocle.inout.*
import de.awtools.grooocle.meta.*

def sql = OraUtils.createSql('user', 'password', 'host-name', port, 'sid') 
def schema = OraUtils.createOracleSchema(sql);
/* The line above is a shortcut for the following lines:
 * def factory = new OracleMetaDataFactory() 
 * def schema = factory.createOracleSchema(sql) 
 */

def table = schema.tables.get('TABLE_NAME') 
// OLD: table.constraint.foreignKeys.each { println it.referencedTableName }
table.foreignKeys().each { println it.referencedTableName }
```

Shows the columns of a oracle table:
```
@Grab(group='de.awtools', module='grooocle', version='0.4.0')

import static de.awtools.grooocle.OraUtils.*

def sql = createSql('test', 'test', 'voracle', 1521, 'orcl') 
def schema = createOracleSchema(sql);

def table = schema.tables.get('TABLENAME') 
table.columnMetaData.each { println it.columnName }
```