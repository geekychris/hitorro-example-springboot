# CLI Quick Start Guide

## 🚀 Start the Application

```bash
cd hitorro-example-springboot
mvn spring-boot:run
```

Wait for:
```
✓ Telnet CLI started on port 5050
✓ SSH CLI started on port 5022
```

## 🔌 Connect via Telnet

Open a **new terminal** and run:
```bash
telnet localhost 5050
```

You'll see:
```
Trying ::1...
Connected to localhost.
Escape character is '^]'.
HitorroExample> 
```

## 🔐 Connect via SSH

```bash
ssh -p 5022 user@localhost
```

When prompted:
- Password: `user`

## 📝 Try Some Commands

### Help
```
HitorroExample> help
```

Shows all available commands.

### Uptime
```
HitorroExample> uptime
```

Shows how long the application has been running.

### Memory
```
HitorroExample> memory
```

Shows current memory usage.

### Threads
```
HitorroExample> threads
```

Shows active threads.

### Properties
```
HitorroExample> props
```

Shows JVS properties.

### Exit
```
HitorroExample> exit
```

Closes the CLI session (application keeps running).

## 🎯 Custom Commands

Register your own commands:

```java
@Component
public class MyCommand implements CommandInterface {
    
    @Override
    public String getCommandName() {
        return "mycommand";
    }
    
    @Override
    public String getUsage() {
        return "mycommand - Does something cool";
    }
    
    @Override
    public void execute(CommandSession session, String[] args) {
        session.println("Hello from my custom command!");
    }
}
```

Spring Boot will automatically register it!

## ⚙️ Configuration

Edit `application.yml`:

```yaml
hitorro:
  # Change the CLI prompt
  application-name: MyApp
  
  cli:
    # Enable/disable
    native-enabled: true
    
    # Change ports
    telnet-port: 5050
    ssh-port: 5022
```

## 🐛 Troubleshooting

### Can't connect?

**Check if ports are in use:**
```bash
lsof -i :5050
lsof -i :5022
```

**Check application logs:**
```
✓ Telnet CLI started on port 5050
```

If you see errors, another application may be using the ports.

### Connection refused?

Make sure the application is running:
```bash
curl http://localhost:8080/actuator/health
```

Should return `{"status":"UP"}`

### SSH asks for password but rejects it?

The default credentials are:
- Username: `user`
- Password: `user`

### Prompt shows "null>" instead of application name?

Set in `application.yml`:
```yaml
hitorro:
  application-name: HitorroExample
```

## 🔒 Security Notes

**⚠️ Development Only**

These CLI interfaces are for **development and debugging**:
- Telnet has **no authentication** - anyone can connect
- SSH has simple **hardcoded credentials**
- Commands can modify application state
- Should be **disabled in production**

**Production Configuration:**
```yaml
hitorro:
  cli:
    native-enabled: false  # Disable CLI in production
```

Or restrict to localhost and use firewall rules.

## 📚 Advanced Usage

### Using CLI in Tests

```java
@SpringBootTest
public class CliTest {
    
    @Autowired
    private CommandRegistry registry;
    
    @Test
    public void testCommand() {
        CommandInterface cmd = registry.getCommand("mycommand");
        assertNotNull(cmd);
        
        // Execute programmatically
        StringCommandSession session = new StringCommandSession();
        cmd.execute(session, new String[0]);
        
        String output = session.getOutput();
        assertTrue(output.contains("Hello"));
    }
}
```

### Accessing Spring Beans from Commands

Commands are Spring-managed:

```java
@Component
public class DmsCommand implements CommandInterface {
    
    @Autowired
    private DMSSession dmsSession;  // Inject Spring beans!
    
    @Override
    public void execute(CommandSession session, String[] args) {
        Document doc = dmsSession.newDocument();
        session.println("Created document: " + doc.getId());
    }
}
```

### Multiple CLI Sessions

You can have multiple simultaneous connections:
- Multiple telnet sessions
- Multiple SSH sessions
- Mixed telnet + SSH

Each gets its own `CommandSession`.

## 🎓 Next Steps

1. **Try the H2 Console**: `http://localhost:8080/h2-console`
2. **Check Actuator**: `http://localhost:8080/actuator`
3. **REST API**: `http://localhost:8080/api/rest/dms/documents`
4. **Create custom commands** for your application logic

## 📖 More Information

- **CLI Fix Details**: See `/Users/chris/hitorro/CLI_FIX_SUMMARY.md`
- **Command Framework**: Check Hitorro documentation
- **Spring Boot Integration**: See Spring Boot autoconfiguration docs

Happy commanding! 🎉
