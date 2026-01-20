# Quick Start - IntelliJ IDEA Debugging

## 🚀 3-Step Quick Start

### Step 1: Open Project
- Open `/Users/chris/hitorro` in IntelliJ IDEA
- Wait for Maven import to complete

### Step 2: Select Run Configuration
Look at the top toolbar:
```
┌─────────────────────────────────────────┐
│  [HitorroExampleSpringBoot ▼] ▶️ 🐛 ⏹️   │
│   ^                              ^       │
│   └─ Click dropdown              └─ Click to Debug
└─────────────────────────────────────────┘
```

1. Click the dropdown (shows current configuration)
2. Select **"HitorroExampleSpringBoot"** from the list
3. If you don't see it, reload: `File` → `Sync Project with Gradle/Maven Files`

### Step 3: Debug!
- Click the 🐛 **Debug** button, or
- Press `Shift+F9`

**That's it!** The application will start in debug mode with all required configuration.

---

## 📍 What You'll See

### Console Output (if configured correctly):
```
HT_BIN configured: /Users/chris/hitorro
HT_HOME configured: /Users/chris/hthome
=== Initializing JsonTypeSystem ===
✓ HT_BIN already configured: /Users/chris/hitorro
✓ HT_HOME configured: /Users/chris/hthome
✓ JsonTypeSystem initialized successfully
```

### Application Ready:
```
Started HitorroExampleApplication in 3.456 seconds
```

### Endpoints Available:
- http://localhost:8080
- http://localhost:8080/actuator/health
- http://localhost:8080/api/commands

---

## 🔧 If Configuration Not Found

### Option A: Manual Configuration (2 minutes)

1. **Open Run Configurations**
   - Top toolbar: Click dropdown → `Edit Configurations...`
   - Or: `Run` menu → `Edit Configurations...`

2. **Add Spring Boot Configuration**
   - Click `+` button (top left)
   - Select `Spring Boot`

3. **Fill in the form**:
   ```
   Name: HitorroExampleSpringBoot
   
   Main class: com.hitorro.example.HitorroExampleApplication
   (Click [...] to browse)
   
   Module: hitorro-example-springboot
   (Select from dropdown)
   
   JRE: 23 (or your Java version)
   ```

4. **Add VM Options** (CRITICAL - click "Modify options" → "Add VM options" if field hidden):
   ```
   -server -DHT_BIN=$PROJECT_DIR$/ -DHT_HOME="$PROJECT_DIR$/../hthome" -Xmx2010M --add-opens java.base/java.lang=ALL-UNNAMED
   ```

5. **Working directory**:
   ```
   $PROJECT_DIR$/hitorro-example-springboot
   ```

6. **Click Apply → OK**

### Option B: Copy Pre-Made Configuration

The configuration file is already created at:
```
/Users/chris/hitorro/.idea/runConfigurations/HitorroExampleSpringBoot.xml
```

If IntelliJ doesn't see it:
1. Close IntelliJ
2. Restart IntelliJ
3. Configuration should appear in dropdown

---

## 🐛 Debugging Tips

### Set Your First Breakpoint

Good places to start:

1. **Check configuration** - Set breakpoint at:
   ```java
   // HitorroExampleApplication.java, line ~56
   private static void configureHitorroSystemProperties() {
   ```

2. **Check JVS initialization** - Set breakpoint at:
   ```java
   // JsonTypeSystemManager.java, line ~68
   public void afterPropertiesSet() throws Exception {
   ```

3. **Check your controller** - Set breakpoint in:
   ```java
   // ExampleController.java
   @GetMapping("/status")
   public ResponseEntity<?> status() {
   ```

### How to Set a Breakpoint

1. Open the Java file
2. Click in the left margin (gutter) next to line number
3. Red dot appears = breakpoint set ✓

### Debug Controls

Once stopped at a breakpoint:

| Button | Shortcut | What it does |
|--------|----------|--------------|
| ▶️ Resume | F9 | Continue running |
| ⏭️ Step Over | F8 | Run current line, don't enter methods |
| ⏬ Step Into | F7 | Enter the method call |
| ⏫ Step Out | Shift+F8 | Finish current method |
| 📊 Evaluate | Alt+F8 | Calculate expression value |

---

## 🔍 Verify Everything Works

### Test 1: Check Console Output

After starting in debug mode, look for:
```
✓ HT_BIN already configured: /Users/chris/hitorro
✓ JsonTypeSystem initialized successfully
Started HitorroExampleApplication in X.XXX seconds
```

### Test 2: Hit an Endpoint

Open browser or use curl:
```bash
curl http://localhost:8080/api/example/status
```

Should return JSON with application status.

### Test 3: Check Actuator

```bash
curl http://localhost:8080/actuator/health
```

Should return:
```json
{
  "status": "UP"
}
```

---

## ⚠️ Troubleshooting

### Problem: "Cannot resolve symbol 'HitorroExampleApplication'"

**Fix**: Reimport Maven project
- Right-click on root `pom.xml` → Maven → Reload Project
- Or: `View` → `Tool Windows` → `Maven` → Click refresh icon

### Problem: "Module not specified" error

**Fix**: 
1. Edit configuration
2. Module dropdown → Select `hitorro-example-springboot`
3. If not listed, reimport Maven (see above)

### Problem: Configuration runs but "HT_BIN not configured" in logs

**Fix**: Check VM options are exactly:
```
-server -DHT_BIN=$PROJECT_DIR$/ -DHT_HOME="$PROJECT_DIR$/../hthome" -Xmx2010M --add-opens java.base/java.lang=ALL-UNNAMED
```

Note: No line breaks, `$PROJECT_DIR$` should auto-expand

### Problem: Can't find Debug button

**Look here**:
```
Top toolbar, right side:
[Configuration Dropdown ▼] [▶️ Run] [🐛 Debug] [⏹️ Stop]
```

If toolbar is hidden:
- `View` → `Appearance` → `Toolbar`

---

## 💡 Pro Tips

### Tip 1: Hot Swap (Change code while debugging)
1. Make code changes while debugging
2. `Build` → `Recompile 'FileName.java'` (Ctrl+Shift+F9)
3. IntelliJ hot-swaps the changes (for simple changes)

### Tip 2: Conditional Breakpoints
1. Right-click breakpoint (red dot)
2. Add condition: `someVariable == "test"`
3. Breakpoint only triggers when condition is true

### Tip 3: Watch Variables
1. While debugging, select a variable
2. Right-click → `Add to Watches`
3. Watch window shows value in real-time

### Tip 4: Evaluate Expression
1. While stopped at breakpoint
2. Press `Alt+F8`
3. Type any expression: `System.getProperty("HT_BIN")`
4. See result immediately

### Tip 5: Debug Multiple Instances
1. Run configuration → `Allow parallel run`
2. Start multiple debug sessions
3. Debug distributed scenarios

---

## 📚 More Help

- **Detailed guide**: See `INTELLIJ_SETUP.md`
- **Configuration issues**: See `TROUBLESHOOTING.md`
- **All config methods**: See `CONFIGURATION.md`

---

## ✅ Success Checklist

After following this guide, you should have:
- ✅ Run configuration visible in dropdown
- ✅ Debug button working (Shift+F9)
- ✅ Application starts with proper HT_BIN/HT_HOME
- ✅ Console shows successful initialization
- ✅ Endpoints responding (http://localhost:8080)
- ✅ Can set breakpoints and inspect variables

**Happy Debugging! 🎉**
