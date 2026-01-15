# H2 Database Quick Start

## 🚀 3-Minute Setup

### 1. Start the Application

```bash
cd hitorro-example-springboot
mvn spring-boot:run
```

Wait for:
```
H2 console available at '/h2-console'
Started HitorroExampleApplication in X.XXX seconds
```

### 2. Open H2 Console

Open in your browser:
```
http://localhost:8080/h2-console
```

### 3. Login

Enter these values exactly:

| Field | Value |
|-------|-------|
| **JDBC URL** | `jdbc:h2:file:./data/hitorrodb` |
| **User Name** | `sa` |
| **Password** | `hitorro` |

Click **Connect**

### 4. Run Your First Query

Try this:

```sql
-- See all tables
SELECT * FROM INFORMATION_SCHEMA.TABLES;

-- Create a test document (if DMS is enabled)
SELECT * FROM sysobject;
```

## What Changed?

✅ **Before**: In-memory database (data lost on restart)  
✅ **After**: File-based database (data persists)

**Database Location**: `./data/hitorrodb.mv.db`

## Common Tasks

### View All Data

```sql
SELECT * FROM sysobject ORDER BY r_modify_date DESC LIMIT 10;
```

### Export Data

```sql
CALL CSVWRITE('./export.csv', 'SELECT * FROM sysobject');
```

### Reset Database

Stop app, then:
```bash
rm -rf ./data
```

Start app again - fresh database!

## Troubleshooting

### App won't start - "Failed to configure DataSource"

**Error**:
```
Failed to configure a DataSource: 'url' attribute is not specified
```

**Fix**: H2 dependency scope issue
```bash
# Check pom.xml - should be runtime, not test
# See H2_DEPENDENCY_FIX.md for details
mvn clean install
mvn spring-boot:run
```

**Can't connect to H2 Console?**
- Check app is running: `http://localhost:8080`
- Verify JDBC URL: `jdbc:h2:file:./data/hitorrodb` (not `mem:`)
- Check username: `sa` (lowercase)
- Check password: `hitorro` (all lowercase)

**Database locked?**
- Only one connection allowed
- Close other H2 Console tabs
- Restart application

## Next Steps

📖 **Full Guide**: [H2_DATABASE_GUIDE.md](H2_DATABASE_GUIDE.md)

- Advanced queries
- Backup/restore
- IntelliJ integration
- Production considerations
- Performance tuning

## URLs

| Service | URL |
|---------|-----|
| Application | `http://localhost:8080` |
| H2 Console | `http://localhost:8080/h2-console` |
| Actuator | `http://localhost:8080/actuator` |
| Health | `http://localhost:8080/actuator/health` |

---

**Happy querying!** 🎉
