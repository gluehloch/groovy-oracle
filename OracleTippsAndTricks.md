# User anlegen #
<pre>
CREATE USER <name> IDENTIFIED BY <password>;<br>
GRANT CONNECT TO <name>;<br>
GRANT RESOURCE TO <name>;<br>
</pre>
Das Password für einen User ändern:
<pre>
alter user user_name identified by new_password;<br>
</pre>
Die Gültigkeit eines Password läßt sich ebenfalls einstellen:
<pre>
ALTER PROFILE DEFAULT LIMIT<br>
FAILED_LOGIN_ATTEMPTS UNLIMITED<br>
PASSWORD_LIFE_TIME UNLIMITED;<br>
</pre>
In diesem Fall wird die Laufzeit auf unendlich gestellt.

# Utility Skripte #

## Blockierende Sessions ##
<pre>
select s1.username || '@' || s1.machine<br>
|| ' ( SID=' || s1.sid || ', OSUSER=' || s1.OSUSER || ' )  is blocking '<br>
|| s2.username || '@' || s2.machine || ' ( SID=' || s2.sid || ', SERIAL='<br>
|| s2.serial# || ', OSUSER=' || s2.OSUSER || ' ) ' AS blocking_status<br>
from v$lock l1, v$session s1, v$lock l2, v$session s2<br>
where s1.sid=l1.sid and s2.sid=l2.sid<br>
and l1.BLOCK=1 and l2.request > 0<br>
and l1.id1 = l2.id1<br>
and l2.id2 = l2.id2;<br>
</pre>

In diesem Zusammenhang ein Skript für das Killen bestimmter Sessions. Die ```
IN``` Klausel ist natürlich entsprechend anzupassen.
<pre>
set serveroutput on;<br>
<br>
DECLARE<br>
v_sql VARCHAR2(1000);<br>
<br>
CURSOR v_cursor IS<br>
SELECT SID as sid, SERIAL# as serial, STATUS, SERVER, OSUSER<br>
FROM V$SESSION<br>
WHERE OSUSER NOT IN ('USER_1', 'USER_2');<br>
BEGIN<br>
FOR row IN v_cursor<br>
LOOP<br>
DBMS_OUTPUT.PUT_LINE('SID: ' || row.sid || ', serial: ' || row.serial || ', os_user: ' || row.osuser);<br>
v_sql := 'ALTER SYSTEM KILL SESSION '''<br>
|| row.sid || ',' || row.serial || '''';<br>
DBMS_OUTPUT.PUT_LINE(v_sql);<br>
execute immediate v_sql;<br>
END LOOP;<br>
END;<br>
</pre>

## Arbeiten mit DATE und TIMESTAMP ##
Der folgende Befehl liefert die aktuelle Systemzeit. In dem Format-String sieht man die von Oracle verwendeten Abkürzungen. MM steht für Monate und MI für Minuten. Das kann schon mal durcheinander gehen.
<pre>
SELECT TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') FROM DUAL;<br>
</pre>

Die Funktion ```
last_day``` liefert den letzten Tag eines Monats.
<pre>
SELECT TO_CHAR(LAST_DAY(TO_DATE('200901', 'YYYYMM')), 'YYYY-MM-DD HH24:MI:SS') as last_day FROM DUAL;<br>
</pre>

Die Funktion ```
trunc``` schneidet bei Aufruf ohne Formatangabe die Uhrzeit aus einem DATE ab.
<pre>
SELECT TRUNC(TO_DATE('24.03.1971', 'dd.mm.yyyy')) FROM DUAL;<br>
</pre>

Die Funktion kann aber noch mehr:
<pre>
TRUNC(TO_DATE('22-AUG-03'), 'YEAR')  	liefert '01-JAN-03'<br>
TRUNC(TO_DATE('22-AUG-03'), 'Q') 	liefert '01-JUL-03'<br>
TRUNC(TO_DATE('22-AUG-03'), 'MONTH') 	liefert '01-AUG-03'<br>
TRUNC(TO_DATE('22-AUG-03'), 'DDD') 	liefert '22-AUG-03'<br>
TRUNC(TO_DATE('22-AUG-03'), 'DAY') 	liefert '17-AUG-03'<br>
</pre>
Interessant finde ich den Parameter ```
'Q'```.

Für das Hinzufügen von Monaten gibt es eine spezielle Funktion:
<pre>
SELECT ADD_MONTHS(TO_DATE('31-MAR-97'),1) from DUAL;<br>
</pre>

## Anwenderobjekte ##
<pre>
select object_name from user_objects where object_type = 'SEQUENCE' order by object_name;<br>
</pre>

## Entfernen von Duplikaten ##
Folgendes Skript entfernt alle Duplikate aus einer Tabelle. Die ```
where``` Bedingung in der SubQuery definiert die Schlüsseleigenschaften der Tabelle.
<pre>
delete from my_table t1<br>
where exists (select 'x' from my_table t2<br>
where t2.key_value1 = t1.key_value1<br>
and t2.key_value2 = t1.key_value2<br>
and t2.rowid      > t1.rowid);<br>
</pre>
Oder eine Abfrage zur Anzeige aller Duplikate:
<pre>
select t1.key_value1, t1.key_value2<br>
from my_table t1<br>
group by t1.key_value1, t1.key_value2<br>
having count(*) > 1;<br>
</pre>

## Entfernen aller User-Tabellen ##
Das folgende Skript entfernt alle vom Anwender angelegten Datenbanktabellen.
<pre>
DECLARE<br>
BEGIN<br>
FOR i IN (SELECT table_name FROM user_tables)<br>
LOOP<br>
EXECUTE IMMEDIATE 'DROP TABLE ' || i.table_name || ' CASCADE CONSTRAINTS';<br>
END LOOP;<br>
END;<br>
</pre>

## SQL Loader ##
Das folgende Skript erstellt die SQL\*Loader Konfiguration für das Beladen einer Datenbanktabelle.
<pre>
set echo off ver off feed off pages 0<br>
accept tname prompt 'Enter Name of Table: '<br>
accept dformat prompt 'Enter Format to Use for Date Columns: '<br>
<br>
spool &tname..ctl<br>
<br>
select 'LOAD DATA'|| chr (10) ||<br>
'INFILE ''' || lower (table_name) || '.dat''' || chr (10) ||<br>
'INTO TABLE '|| table_name || chr (10)||<br>
'FIELDS TERMINATED BY '','''|| chr (10)||<br>
'TRAILING NULLCOLS' || chr (10) || '('<br>
from   user_tables<br>
where  table_name = upper ('&tname');<br>
<br>
select decode (rownum, 1, '   ', ' , ') ||<br>
rpad (column_name, 33, ' ')      ||<br>
decode (data_type,<br>
'VARCHAR2', 'CHAR NULLIF ('||column_name||'=BLANKS)',<br>
'FLOAT',    'DECIMAL EXTERNAL NULLIF('||column_name||'=BLANKS)',<br>
'NUMBER',   decode (data_precision, 0,<br>
'INTEGER EXTERNAL NULLIF ('||column_name||<br>
'=BLANKS)', decode (data_scale, 0,<br>
'INTEGER EXTERNAL NULLIF ('||<br>
column_name||'=BLANKS)',<br>
'DECIMAL EXTERNAL NULLIF ('||<br>
column_name||'=BLANKS)')),<br>
'DATE',     'DATE "&dformat" NULLIF ('||column_name||'=BLANKS)', null)<br>
from   user_tab_columns<br>
where  table_name = upper ('&tname')<br>
order  by column_id;<br>
<br>
select ')'<br>
from dual;<br>
spool off<br>
</pre>

## Code Unterstützung / Analyse ##
### Code Compilieren ###
Ein komplettes Schema compilieren:
<pre>
exec dbms_utility.compile_schema( 'SCOTT' )<br>
</pre>

Oder ein einzelnes Package mit oder ohne Body:
<pre>
ALTER PACKAGE <packageName> COMPILE;<br>
ALTER PACKAGE <packageName> COMPILE BODY;<br>
</pre>

### Code Modifikationen ###
Das Statement ermittelt das Datum von Code-Modifikationen:
<pre>
SELECT<br>
object_name<br>
TO_CHAR(created,       'DD-Mon-RR HH24:MI') create_time,<br>
TO_CHAR(LAST_DDL_TIME, 'DD-Mon-RR HH24:MI') mod_time,<br>
STATUS<br>
FROM<br>
user_objects<br>
WHERE<br>
LAST_DDL_TIME > '&CHECK_FROM_DATE'<br>
ORDER BY<br>
LAST_DDL_TIME;<br>
</pre>

Beziehungsweise den Code direkt anzeigen lassen:
<pre>
SELECT * FROM ALL_SOURCE;<br>
</pre>
Die Spalte ```
type``` enthält dabei folgende Werte:
```
FUNCTION, PACKAGE, PACKAGE BODY, PROCEDURE, TRIGGER, TYPE```

Hier kann man sich auf die Suche nach Zeichenketten begeben:
<pre>
SELECT<br>
type, name, line<br>
FROM<br>
user_source<br>
WHERE<br>
UPPER(text) LIKE UPPER('%&KEYWORD%');<br>
</pre>

Oder auf der Suche nach Objektabhängigkeiten:
<pre>
SELECT<br>
owner || '.' || name packages_refs_table, referenced_owner || '.' || referenced_name table_referenced<br>
FROM<br>
all_dependencies<br>
WHERE<br>
owner LIKE UPPER ('&1')<br>
AND referenced_name='CPMAPPING'<br>
AND TYPE IN ('PACKAGE', 'PACKAGE BODY', 'PROCEDURE', 'FUNCTION')<br>
AND referenced_type IN ('TABLE', 'VIEW')<br>
ORDER BY<br>
owner, name, referenced_owner, referenced_name;<br>
</pre>

# Oracle´s Instant Client #
Oracle bietet mittlerweiele einen sogenannten 'Instant Client' an. In diesem ist dann z.B. nur ```
SQL*PLus``` enthalten. Nach dem die Software heruntergeladen und installiert ist, fehlt nur noch die Definition der ```
TNSNAMES.ORA``` Datei.
<pre>
ORCL =<br>
(DESCRIPTION =<br>
(ADDRESS_LIST =<br>
(ADDRESS =<br>
(COMMUNITY = tcp.world)<br>
(PROTOCOL = TCP)<br>
(Host = 192.168.0.5)<br>
(Port = 1521)<br>
)<br>
)<br>
(CONNECT_DATA = (SID = ORCL)<br>
)<br>
)<br>
</pre>
Im Anschluss definiert man sich die Variable ```
TNS_ADMIN``` und läßt diese auf das Verzeichnis mit der ```
TNSNAMES.ORA``` Datei verweisen.

# SQL Query mit VARRAYs #
Eklärung anhand eines Beispiels:
<pre>create or replace TYPE X_QUERY_ARRAY AS TABLE OF VARCHAR2(6)</pre>

In dem folgendem Beispiel dient das ```
VARRAY``` als Eingabemenge für das Ausfüllen der ```
WHERE``` Bedingung:
<pre>
set serveroutput on;<br>
declare<br>
type cursor_t is ref cursor;<br>
v_array  x_query_array;<br>
v_cursor cursor_t;<br>
<br>
TYPE t_cpmapping_rec IS RECORD<br>
(<br>
uploadtype cpmapping.uploadtype%TYPE<br>
,productid  cpmapping.productid%TYPE<br>
);<br>
<br>
<br>
v_cpmapping_row t_cpmapping_rec;<br>
begin<br>
v_array := X_QUERY_ARRAY('AAAARS', 'AAACKW', 'AAALSA', 'AABAFI');<br>
<br>
open v_cursor for<br>
'select uploadtype, productid from cpmapping where bankcode in (select column_value from table(:1))'<br>
using v_array;<br>
loop<br>
fetch v_cursor into v_cpmapping_row;<br>
exit when v_cursor%notfound;<br>
dbms_output.put_line(v_cpmapping_row.uploadtype || ', ' || v_cpmapping_row.productid);<br>
end loop;<br>
close v_cursor;<br>
end;<br>
/<br>
</pre>
Ein ähnliches Statement Muster kann in SQL\*Plus verwendet werden:
<pre>
select uploadtype, productid from cpmapping where bankcode in<br>
(select column_value from table(X_QUERY_ARRAY('AAAARS', 'AAACKW', 'AAALSA', 'AABAFI')));<br>
</pre>

# SQL\*Plus #
## Kopieren von Tabellen zwischen Oracle Datenbanken ##
<pre>
COPY FROM scott/tiger@db1 TO scott/tiger@db2 INSERT mytable USING select * from mytable;<br>
</pre>
Ausführlicher erklärt im SQL\*Plus Manual:
<pre>
COPY<br>
----<br>
Copies data from a query to a table in the same or another<br>
database. COPY supports CHAR, DATE, LONG, NUMBER and VARCHAR2.<br>
<br>
COPY {FROM database | TO database | FROM database TO database}<br>
{APPEND|CREATE|INSERT|REPLACE} destination_table<br>
[(column, column, column, ...)] USING query<br>
<br>
where database has the following syntax:<br>
username[/password]@connect_identifier<br>
</pre>

## Einstellungen für lesbaren Output ##
Stellt die Breite ein, definiert die Spaltenbreite für das Spalte 'column\_name', setzt die Zeilen pro Seite und definiert ein Spalte, die es nicht in den Output schafft.
<pre>
set linesize 200<br>
column 'column_name' format a20<br>
set pagesize 50000<br>
column 'column_no_print' noprint<br>
</pre>

Die verschiedenen Einstellungen für das Kommando ```
column```.
<pre>
column colum_name alias alias_name<br>
column colum_name clear<br>
<br>
column colum_name entmap on<br>
column colum_name entmap off<br>
<br>
column colum_name fold_after<br>
column colum_name fold_before<br>
column colum_name format a25<br>
column colum_name heading header_text<br>
<br>
column colum_name justify left<br>
column colum_name justify right<br>
column colum_name justify center<br>
<br>
column colum_name like expr|alias<br>
column colum_name newline<br>
column colum_name new_value variable<br>
column colum_name print<br>
column colum_name noprint<br>
column colum_name old_value<br>
<br>
column colum_name on<br>
column colum_name off<br>
<br>
column colum_name wrapped<br>
column colum_name word_wrapped<br>
column colum_name truncated<br>
</pre>

Formatierungseinstellungen:
<pre>
column column_name format a20<br>
column column_name format a50 word_wrapped<br>
column column_name format 999.999  -- Decimal sign<br>
column column_name format 999,999  -- Seperate thousands<br>
column column_name format $999     -- Include leading $ sign<br>
</pre>

Zeilen bis Seitenumbruch:
<pre>
set pagesize n<br>
</pre>

## Meine Einstellungen ##
Am besten in einer Datei ```
login.sql``` erreichbar über ```
PATH``` aufgehoben:
<pre>
set lines 1000<br>
set pages 50<br>
set serverout on size 500000<br>
set head off<br>
set pages 0<br>
set termout off<br>
alter session set nls_date_format = 'DD-MON-YYYY HH24:MI:SS'<br>
/<br>
spool z1.sql<br>
select 'set sqlprompt '''||lower(global_name)||':'||user||'>''' from sys.global_name<br>
/<br>
spool off<br>
@z1<br>
set head on<br>
set pages 60<br>
set termout on<br>
</pre>

# Oracle´s Metadaten #
Oracle metadata is information contained within the Oracle Database about the objects contained within.

The ORACLE application server and Oracle relational database keep metadata in two areas: data dictionary tables (accessed via built-in views) and a metadata registry. The total number of these views depends on the Oracle version, but is in a 1000 range.

The few main built-in views accessing Oracle RDBMS data dictionary tables are:

**ALL\_TABLES - list of all tables in the current database that are accessible to the current user** ALL\_TAB\_COLUMNS - list of all columns in the database that are accessible to the current user
**ALL\_ARGUMENTS - lists the arguments of functions and procedures that are accessible to the current user** ALL\_ERRORS - lists descriptions of errors on all stored objects (views, procedures, functions, packages, and package bodies) that are accessible to the current user
**ALL\_OBJECT\_SIZE - included for backward compatibility with Oracle version 5** ALL\_PROCEDURES - (from Oracle 9 onwards) lists all functions and procedures (along with associated properties) that are accessible to the current user
**ALL\_SOURCE - describes the text (i.e. PL/SQL) source of the stored objects accessible to the current user**


In addition there are equivalent views prefixed "USER_" which show only the objects owned by the current user (i.e. a more restricted view of metadata) and prefixed "DBA_" which show all objects in the database (i.e. an unrestricted global view of metadata for the database instance). Naturally the access to "DBA_" metadata views requires specific privileges._

## Example 1: finding tables ##
Find all Tables that have PATTERN in the table name
<pre>
SELECT<br>
TABLE_NAME<br>
FROM<br>
ALL_TABLES<br>
WHERE<br>
TABLE_NAME LIKE '%PATTERN%'<br>
ORDER<br>
BY TABLE_NAME;<br>
</pre>

## Example 2: finding columns ##
Find all tables that have at least one column that matches a specific PATTERN in the column name
<pre>
SELECT<br>
TABLE_NAME,<br>
COLUMN_NAME<br>
FROM<br>
ALL_TAB_COLUMNS<br>
WHERE<br>
COLUMN_NAME LIKE '%PATTERN%';<br>
</pre>

## Example 3: counting rows of columns ##
Estimate a total number of rows in all tables containing a column name that matches PATTERN (this is SQL\*Plus specific script)
<pre>
COLUMN DUMMY NOPRINT<br>
COMPUTE SUM OF NUM_ROWS ON DUMMY<br>
BREAK ON DUMMY<br>
SELECT<br>
NULL DUMMY,<br>
T.TABLE_NAME,<br>
C.COLUMN_NAME,<br>
T.NUM_ROWS<br>
FROM<br>
ALL_TABLES T,<br>
ALL_TAB_COLUMNS C<br>
WHERE<br>
T.TABLE_NAME = C.TABLE_NAME<br>
AND C.COLUMN_NAME LIKE '%PATTERN%'<br>
ORDER BY T.TABLE_NAME;<br>
</pre>

Note that NUM\_ROWS records the number of rows which were in a table when (and if) it was last analyzed. This will most likely deviate from the actual number of rows currently in the table.

## Use of underscore in table and column names ##
The underscore is a special SQL pattern match to a single character and should be escaped if you are in fact looking for an underscore character in the LIKE clause of a query.

Just add the following after a LIKE statement:

> ESCAPE '_'_

And then each literal underscore should be a double underscore: 

Example

> LIKE '%G' ESCAPE '_'_

## Oracle Metadata Registry ##
The Oracle product Oracle Enterprise Metadata Manager (EMM) is an ISO/IEC 11179 compatible metadata registry.  It stores administered metadata in a consistent format that can be used for metadata publishing.  As of|2006|1, EMM is available only through Oracle consulting services.

# Oracle PL/SQL Support für ETL Prozesse #

http://www.oracle-developer.com/oracle_etl.html Beschreibung der ETL Möglichkeiten mit PL/SQL (Hier vor allem der Zugriff auf Daten mittels externer Tabellen.

# Linksammlung #
**[Thema INDEX COMPRESSION oder INDEX ORGANIZED TABLES](http://www.ordix.de/ORDIXNews/1_2007/Datenbanken/datenkompression_IOT.html)** [Eine Bibliothek für den Zugriff auf Betriebssystem Resourcen](http://plsqlexecoscomm.sourceforge.net/)
**[article on Oracle Metadata](http://www.oreillynet.com/pub/a/network/2002/10/28/data_dictionary.html)** [SQL Tuning](http://www.dba-oracle.com/art_sql_tune.htm)
