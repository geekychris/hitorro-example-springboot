# 📁 Docker Build Directory - Index

This directory contains everything you need to build and run Hitorro with Docker.

## 🎯 Start Here

### Absolute Quickest (1 command)
```bash
./build-and-start.sh
```
Builds and starts everything. Done!

### Recommended (Master Script)
```bash
./hitorro.sh build-ui    # Build once
./hitorro.sh start-ui    # Start anytime
```

## 📜 Scripts Overview

### 🌟 Main Scripts

| Script | Purpose | When to Use |
|--------|---------|-------------|
| **`build-and-start.sh`** | Build + Start in one go | First time setup, complete rebuild |
| **`hitorro.sh`** | Master control script | Day-to-day operations |

### 🔨 Build Scripts

| Script | Purpose | Output |
|--------|---------|--------|
| `build-backend.sh` | Build backend only | `hitorro-example-springboot:latest` |
| `build-ui.sh` | Build with React UI | `hitorro-example-springboot:ui-latest` |

### ⚙️ Operation Scripts

| Script | Purpose |
|--------|---------|
| `start.sh` | Start container |
| `stop.sh` | Stop container |
| `clean.sh` | Remove containers/images |

## 📚 Documentation

| File | Description |
|------|-------------|
| **`QUICK_START.md`** | Fastest way to get started |
| **`README.md`** | Complete documentation |
| **`INDEX.md`** | This file |

## 🚀 Common Tasks

### First Time Setup
```bash
./build-and-start.sh
```

### Daily Start
```bash
./hitorro.sh start-ui
```

### Check Status
```bash
./hitorro.sh status
```

### View Logs
```bash
./hitorro.sh logs
```

### Stop Application
```bash
./hitorro.sh stop
```

### Update & Rebuild
```bash
git pull
./hitorro.sh clean
./build-and-start.sh
```

### Complete Reset
```bash
./hitorro.sh clean-all
./build-and-start.sh
```

## 🎓 Script Comparison

### `build-and-start.sh` vs `hitorro.sh`

**Use `build-and-start.sh` when:**
- ✅ First time setup
- ✅ Want everything in one command
- ✅ Don't care about intermediate steps
- ✅ Making major updates

**Use `hitorro.sh` when:**
- ✅ Day-to-day operations
- ✅ Need fine-grained control
- ✅ Already have images built
- ✅ Troubleshooting

## 📖 Learning Path

1. **Start here**: `QUICK_START.md`
2. **Daily use**: `hitorro.sh help`
3. **Deep dive**: `README.md`
4. **Parent docs**: `../REACT_UI_GUIDE.md`

## 🔗 Quick Reference

### Build Commands
```bash
./build-and-start.sh          # Build + start (UI)
./build-and-start.sh backend  # Build + start (backend only)
./hitorro.sh build-ui         # Build with UI
./hitorro.sh build            # Build backend only
```

### Start Commands
```bash
./hitorro.sh start-ui         # Start with UI
./hitorro.sh start            # Start backend only
./hitorro.sh compose-up-ui    # Start with compose
```

### Management Commands
```bash
./hitorro.sh stop             # Stop
./hitorro.sh restart          # Restart
./hitorro.sh logs             # View logs
./hitorro.sh status           # Check status
```

### Cleanup Commands
```bash
./hitorro.sh clean            # Remove containers/images
./hitorro.sh clean-all        # Remove everything + volumes
./clean.sh                    # Remove containers/images
./clean.sh --all              # Remove everything
```

## 🌐 Access URLs

After starting:

| Service | URL |
|---------|-----|
| **React UI** | http://localhost:8080 |
| **Swagger** | http://localhost:8080/swagger-ui.html |
| **H2 Console** | http://localhost:8080/h2-console |
| **Actuator** | http://localhost:8080/actuator |
| **REST API** | http://localhost:8080/api/rest |
| **Telnet** | `telnet localhost 9000` |
| **SSH** | `ssh -p 9022 localhost` |

## 💡 Tips

1. **Always use `./hitorro.sh`** for daily operations
2. **Use `build-and-start.sh`** for quick setup
3. **Check logs** if something doesn't work: `./hitorro.sh logs`
4. **Status first** before troubleshooting: `./hitorro.sh status`
5. **Clean regularly** to save disk space: `./hitorro.sh clean`

## 🆘 Troubleshooting

**Nothing works?**
```bash
./hitorro.sh status
./hitorro.sh logs
```

**Need fresh start?**
```bash
./hitorro.sh clean-all
./build-and-start.sh
```

**Docker issues?**
```bash
docker system prune -a  # Clean Docker
./build-and-start.sh    # Rebuild
```

## 📁 Directory Structure

```
docker_build/
├── hitorro.sh              ⭐ Master control script
├── build-and-start.sh      ⭐ One-command build & start
├── build-backend.sh        🔨 Build backend
├── build-ui.sh            🔨 Build with UI
├── start.sh               ▶️  Start container
├── stop.sh                ⏹️  Stop container
├── clean.sh               🧹 Cleanup
├── QUICK_START.md         📖 Quick start guide
├── README.md              📖 Full documentation
└── INDEX.md               📖 This file
```

## 🎉 You're Ready!

Pick your path:
- **Fastest**: `./build-and-start.sh`
- **Controlled**: `./hitorro.sh build-ui && ./hitorro.sh start-ui`
- **Learning**: Read `QUICK_START.md`

---

**Questions?** Check `README.md` or run `./hitorro.sh help`
