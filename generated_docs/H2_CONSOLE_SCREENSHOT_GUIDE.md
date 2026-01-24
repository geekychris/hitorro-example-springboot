# H2 Console Visual Guide

## Step-by-Step with Screenshots Description

### Step 1: Start Application

**Terminal Command**:
```bash
cd hitorro-example-springboot
mvn spring-boot:run
```

**Look for this in logs**:
```
...
2026-01-14 13:45:00.123  INFO ... : H2 console available at '/h2-console'. Available on path '/h2-console'
2026-01-14 13:45:00.456  INFO ... : Started HitorroExampleApplication in 3.456 seconds (process running for 3.789)
```

### Step 2: Open Browser

**URL to visit**:
```
http://localhost:8080/h2-console
```

**What you'll see**:
- H2 Console login page
- Form with connection settings
- Language selector (English)
- Login button

### Step 3: Login Form

**Fill in these exact values**:

```
┌─────────────────────────────────────────────────────────┐
│ H2 Console                                              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Saved Settings:     [Generic H2 (Embedded)        ▼]   │
│ Setting Name:        Generic H2 (Embedded)              │
│ Driver Class:        org.h2.Driver                      │
│ JDBC URL:           jdbc:h2:file:./data/hitorrodb       │
│ User Name:          sa                                  │
│ Password:           hitorro                             │
│                                                         │
│ [Test Connection]              [Connect]                │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Important**:
- ⚠️ Change JDBC URL from `mem:` to `file:./data/hitorrodb`
- ✓ Username is `sa` (lowercase)
- ✓ Password is `hitorro` (all lowercase)

### Step 4: After Login

**Main H2 Console Interface**:

```
┌─────────────────────────────────────���──────────────────────────┐
│ [Disconnect] [Refresh] [Preferences] [Tools ▼] [Help]         │
├──────────────┬─────────────────────────────────────────────────┤
│              │                                                 │
│ PUBLIC       │  SQL Command:                                  │
│  ├─ CONTENT  │  ┌─────────────────────────────────────────┐  │
│  ├─ STORE    │  │ SELECT * FROM sysobject;                 │  │
│  └─ SYSOBJECT│  │                                          │  │
│              │  └─────────────────────────────────────────┘  │
│              │  [Run] [Clear] [History] [Explain] [Format]    │
│              │                                                 │
│              │  Results:                                      │
│              │  ┌─────────────────────────────────────────┐  │
│              │  │ R_OBJECT_ID  | OBJECT_NAME | R_VERSION  │  │
│              │  │ 123456789    | doc1        | 1.0        │  │
│              │  └─────────────────────────────────────────┘  │
│              │  1 row(s) selected                            │
└──────────────┴─────────────────────────────────────────────────┘
```

**Left Panel**: Table browser
- Click on table names to explore
- Right-click for context menu

**Top Panel**: SQL command editor
- Type SQL queries
- Syntax highlighting
- Multi-statement support

**Bottom Panel**: Results
- Table format
- Pagination for large results
- Export options

### Step 5: Run Your First Query

**Click on `INFORMATION_SCHEMA.TABLES` in left panel**

This auto-generates:
```sql
SELECT * FROM INFORMATION_SCHEMA.TABLES;
```

**Click "Run" button**

**You'll see**:
- List of all tables in database
- Columns: TABLE_NAME, TABLE_SCHEMA, TABLE_TYPE, etc.
- Row count at bottom

### Step 6: Explore Tables

**Click on `SYSOBJECT` table in left panel**

Options appear:
```
SYSOBJECT
  ├─ Select (generates SELECT *)
  ├─ Script (shows CREATE TABLE)
  └─ Columns
       ├─ R_OBJECT_ID (VARCHAR)
       ├─ OBJECT_NAME (VARCHAR)
       ├─ R_VERSION_LABEL (VARCHAR)
       ├─ R_CREATION_DATE (TIMESTAMP)
       └─ ...
```

### Step 7: Common Queries

Copy-paste these into the SQL editor:

**View all documents**:
```sql
SELECT * FROM sysobject 
ORDER BY r_modify_date DESC 
LIMIT 10;
```

**Count by type**:
```sql
SELECT r_object_type, COUNT(*) as count
FROM sysobject 
GROUP BY r_object_type;
```

**Export to CSV**:
```sql
CALL CSVWRITE('./data/export.csv', 'SELECT * FROM sysobject');
```

### Step 8: Use Query History

**Click "History" button**

Shows:
```
┌────────────────────────────────────────────┐
│ Query History                              │
├────────────────────────────────────────────┤
│ SELECT * FROM sysobject;                   │
│ SELECT COUNT(*) FROM content;              │
│ SELECT * FROM INFORMATION_SCHEMA.TABLES;   │
└────────────────────────────────────────────┘
```

Click any query to re-run it.

## Console Features Guide

### Top Menu Bar

**[Disconnect]**
- Closes database connection
- Returns to login screen

**[Refresh]**
- Reloads table list
- Updates schema information

**[Preferences]**
- Change display settings
- Adjust font size
- Configure result limits

**[Tools ▼]**
- Backup Database
- Restore Database  
- Script Generator
- CSV Import/Export

**[Help]**
- H2 documentation
- SQL reference
- Function reference

### Left Panel - Table Browser

**Structure**:
```
PUBLIC (schema)
├─ CONTENT
│  ├─ Columns (expandable)
│  ├─ Indexes (expandable)
│  └─ Foreign Keys (expandable)
├─ STORE
└─ SYSOBJECT
```

**Right-click options**:
- Select rows
- Insert row
- Update row
- Delete row
- Show DDL
- Export data

### SQL Editor

**Features**:
- ✓ Syntax highlighting (SQL keywords in blue)
- ✓ Auto-completion (Ctrl+Space)
- ✓ Multi-statement execution (separated by `;`)
- ✓ Comments support (`--` or `/* */`)
- ✓ Parameter support (`?` placeholders)

**Keyboard shortcuts**:
- `Ctrl+Enter` - Run query
- `Ctrl+Space` - Auto-complete
- `Ctrl+/` - Toggle comment
- `Ctrl+F` - Find/replace

### Results Panel

**Display options**:
- Table view (default)
- Edit mode (for updates)
- Export options (CSV, SQL, XML, JSON)

**Pagination**:
```
[<< First] [< Prev] Page 1 of 5 [Next >] [Last >>]
Showing rows 1-100 of 457
```

**Export buttons**:
- CSV
- SQL INSERT statements
- XML
- JSON

## Common Use Cases

### Use Case 1: Find a Document

1. Type in SQL editor:
   ```sql
   SELECT * FROM sysobject 
   WHERE object_name LIKE '%test%';
   ```
2. Click **Run**
3. Results show matching documents
4. Click on any row to see details

### Use Case 2: Export Data

1. Type query:
   ```sql
   CALL CSVWRITE('./data/backup.csv', 
     'SELECT * FROM sysobject');
   ```
2. Click **Run**
3. File created in `./data/backup.csv`
4. Download or access from file system

### Use Case 3: Backup Database

1. Click **Tools** → **Backup**
2. Or run SQL:
   ```sql
   BACKUP TO './backups/hitorro-backup.zip';
   ```
3. Zip file created with all data

### Use Case 4: View Version History

1. Find chronicle ID:
   ```sql
   SELECT i_chronicle_id, object_name 
   FROM sysobject 
   WHERE object_name = 'mydoc';
   ```
2. View all versions:
   ```sql
   SELECT * FROM sysobject 
   WHERE i_chronicle_id = 'chronicle_123'
   ORDER BY r_version_label;
   ```

### Use Case 5: Analyze Database

1. Table sizes:
   ```sql
   SELECT TABLE_NAME, ROW_COUNT_ESTIMATE
   FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = 'PUBLIC'
   ORDER BY ROW_COUNT_ESTIMATE DESC;
   ```

2. Database info:
   ```sql
   SELECT H2VERSION(), 
          DATABASE_PATH() as path,
          CURRENT_SCHEMA() as schema;
   ```

## Visual Layout

```
Browser Window: http://localhost:8080/h2-console
┌──────────────────────────────────────────────────────────────────┐
│ ← → ⟳  http://localhost:8080/h2-console                      🔒  │
├──────────────────────────────────────────────────────────────────┤
│ H2 Console                                        [En ▼] [Help]  │
├──────────────────────────────────────────────────────────────────┤
│ [Disconnect] [⟳] [⚙️] [🛠️ Tools ▼] [❓ Help]                    │
├────────────────┬─────────────────────────────────────────────────┤
│                │                                                 │
│ 📁 PUBLIC      │  📝 SQL Command:                               │
│   📊 CONTENT   │  ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓  │
│   📊 STORE     │  ┃ SELECT * FROM sysobject                 ┃  │
│   📊 SYSOBJECT │  ┃ WHERE object_name = 'test';             ┃  │
│                │  ┃                                         ┃  │
│                │  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛  │
│                │  [▶️ Run] [🗑️ Clear] [📜 History] [💡 Explain]  │
│                │                                                 │
│                │  📊 Results: (1 row)                           │
│                │  ┏━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━┳━━━━━━━━━┓  │
│                │  ┃ R_OBJECT_ID   ┃ OBJECT_NAME  ┃ R_VERSION┃  │
│                │  ┣━━━━━━━━━━━━━━╋━━━━━━━━━━━━━━╋━━━━━━━━━┫  │
│                │  ┃ 123456789     ┃ test         ┃ 1.0      ┃  │
│                │  ┗━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━┻━━━━━━━━━┛  │
│                │  [<< First] [< Prev] Page 1/1 [Next >] [Last]   │
└────────────────┴─────────────────────────────────────────────────┘
```

## Tips & Tricks

### 💡 Quick Tips

1. **Double-click table name** - Auto-generates SELECT statement
2. **Ctrl+Enter** - Quick run without clicking button
3. **Use semicolons** - Run multiple queries at once
4. **Save queries** - Copy to text file for reuse
5. **Use LIMIT** - Always limit large result sets

### 🎯 Best Practices

1. **Test queries first**:
   ```sql
   SELECT COUNT(*) FROM sysobject;  -- Check size first
   SELECT * FROM sysobject LIMIT 10;  -- Then view sample
   ```

2. **Use transactions for updates**:
   ```sql
   BEGIN;
   UPDATE sysobject SET object_name = 'new_name' WHERE r_object_id = '123';
   ROLLBACK;  -- or COMMIT;
   ```

3. **Export before major changes**:
   ```sql
   BACKUP TO './backups/before-change.zip';
   ```

### ⚠️ Common Mistakes

1. **Wrong JDBC URL**:
   - ❌ `jdbc:h2:mem:hitorrodb` (in-memory)
   - ✅ `jdbc:h2:file:./data/hitorrodb` (persistent)

2. **Case sensitivity**:
   - H2 in MySQL mode is case-insensitive
   - But column names in quotes are case-sensitive

3. **Missing semicolon**:
   - Required when running multiple statements

4. **Large result sets**:
   - Always use LIMIT for large tables
   - Can crash browser with millions of rows

## Troubleshooting Guide

### Problem: Can't connect

**Check**:
```bash
# 1. Is app running?
curl http://localhost:8080/actuator/health

# 2. Check database files exist
ls -la ./data/

# 3. Check logs
tail -f logs/spring.log | grep -i h2
```

### Problem: Table not found

**Solution**:
```sql
-- Check table exists
SELECT * FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME = 'SYSOBJECT';

-- Refresh table list
-- Click [Refresh] button in console
```

### Problem: Connection timeout

**Check database lock**:
```bash
# Look for lock file
ls -la ./data/*.lock.db

# If exists, stop app and delete
rm ./data/hitorrodb.lock.db
```

## Security Reminder

### ⚠️ Development Only

The current setup is for **development only**:
- Simple password
- No encryption
- No access control
- localhost only

### 🔒 For Production

Never use this setup in production:
- Disable H2 Console
- Use PostgreSQL/MySQL
- Strong authentication
- Encrypted connections
- Firewall rules

## Summary

✅ **Access**: `http://localhost:8080/h2-console`  
✅ **Login**: JDBC URL: `jdbc:h2:file:./data/hitorrodb`, User: `sa`, Password: `hitorro`  
✅ **Features**: SQL queries, table browser, export/import, backup/restore  
✅ **Documentation**: See `H2_DATABASE_GUIDE.md` for complete reference

---

**Happy querying!** 🎉 For more details, see [H2_DATABASE_GUIDE.md](H2_DATABASE_GUIDE.md)
