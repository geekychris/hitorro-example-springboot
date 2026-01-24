# 🚀 START HERE - Hitorro Example Spring Boot

Welcome! This is the fastest way to get Hitorro running.

## ⚡ Super Quick Start

```bash
cd docker_build
./build-and-start.sh
```

**That's it!** Access at: http://localhost:8080

---

## 📁 What You Have

```
hitorro-example-springboot/
├── 🎯 START_HERE.md              ← You are here
├── 📂 docker_build/              ← All Docker scripts
│   ├── ⭐ build-and-start.sh    ← ONE command to rule them all
│   ├── ⭐ hitorro.sh             ← Master control script
│   ├── 📖 QUICK_START.md        ← Quick start guide
│   ├── 📖 INDEX.md              ← Script directory index
│   └── 📖 README.md             ← Full documentation
├── 📂 frontend/                  ← React UI application
├── 📂 src/                       ← Spring Boot backend
└── 📂 docker/                    ← Docker configuration
```

## 🎯 Choose Your Path

### Path 1: Just Want It Running (Fastest)
```bash
cd docker_build
./build-and-start.sh
```
Done! Go to http://localhost:8080

### Path 2: Want Control (Recommended)
```bash
cd docker_build
./hitorro.sh build-ui    # Build once
./hitorro.sh start-ui    # Start anytime
./hitorro.sh logs        # View logs
./hitorro.sh stop        # Stop
```

### Path 3: Development Mode (Hot Reload)
```bash
cd docker_build
./hitorro.sh dev
```
- Backend: http://localhost:8080
- Frontend: http://localhost:3000 (with hot reload)

## 📚 Documentation Guide

### For Getting Started
1. **This file** - You're reading it! ✓
2. `docker_build/QUICK_START.md` - Fastest way to run
3. `docker_build/INDEX.md` - Script directory overview

### For Daily Use
- `docker_build/hitorro.sh help` - Command reference
- `docker_build/README.md` - Complete script docs

### For Deep Dive
- `DOCKER_DEPLOYMENT.md` - Full Docker guide
- `REACT_UI_GUIDE.md` - React UI documentation
- `DOCKER_WITH_UI_SUMMARY.md` - Complete overview
- `frontend/README.md` - Frontend development

## 🌟 What You Get

✅ **Document Management System**  
✅ **Modern React Web Interface**  
✅ **Drag & Drop File Upload**  
✅ **Content Transformation** (PDF, Word, Excel, etc.)  
✅ **REST API** with Swagger docs  
✅ **CLI Access** (Telnet & SSH)  
✅ **Health Monitoring**  
✅ **H2 Database** (or PostgreSQL)  

## 🌐 Access Points

Once running, you can access:

| What | Where |
|------|-------|
| **Main UI** | http://localhost:8080 |
| **API Docs** | http://localhost:8080/swagger-ui.html |
| **Database** | http://localhost:8080/h2-console |
| **Metrics** | http://localhost:8080/actuator |
| **REST API** | http://localhost:8080/api/rest |
| **Telnet CLI** | `telnet localhost 9000` |
| **SSH CLI** | `ssh -p 9022 localhost` |

## 🔧 Common Commands

All commands run from `docker_build/` directory:

```bash
# Build and start everything (one command)
./build-and-start.sh

# Or use the master script for control
./hitorro.sh build-ui         # Build with UI
./hitorro.sh start-ui         # Start
./hitorro.sh stop             # Stop
./hitorro.sh restart          # Restart
./hitorro.sh logs             # View logs
./hitorro.sh status           # Check status
./hitorro.sh clean-all        # Complete cleanup

# Development mode
./hitorro.sh dev              # Hot reload

# Docker Compose
./hitorro.sh compose-up-ui    # Start with compose
./hitorro.sh compose-down     # Stop compose
```

## 🆘 Troubleshooting

### Container won't start?
```bash
cd docker_build
./hitorro.sh logs
```

### Need fresh start?
```bash
cd docker_build
./hitorro.sh clean-all
./build-and-start.sh
```

### Port 8080 in use?
```bash
# Check what's using it
lsof -i :8080

# Kill it
kill -9 <PID>

# Or change port (edit docker-compose.yml)
```

### Build fails?
```bash
# Clear Docker cache
docker system prune -a

# Rebuild
cd docker_build
./build-and-start.sh
```

## 💡 Pro Tips

1. **Use `hitorro.sh`** - It's your Swiss Army knife
2. **Check `hitorro.sh status`** first when troubleshooting
3. **Use `dev` mode** when developing - it's faster
4. **Read the logs** - `hitorro.sh logs` shows everything
5. **Clean regularly** - Saves disk space

## 📖 Learning Path

1. ✅ **START_HERE.md** (you are here)
2. 📖 `docker_build/QUICK_START.md` - Quick reference
3. 🎓 `docker_build/README.md` - Detailed docs
4. 🚀 `REACT_UI_GUIDE.md` - UI customization
5. 🐳 `DOCKER_DEPLOYMENT.md` - Production deployment

## 🎨 Customization

### Change Theme
Edit `frontend/src/main.jsx`:
```javascript
const theme = createTheme({
  palette: {
    primary: { main: '#YOUR_COLOR' },
  },
})
```

### Add New Page
1. Create `frontend/src/pages/MyPage.jsx`
2. Add route in `frontend/src/App.jsx`
3. Add menu item in `frontend/src/components/Sidebar.jsx`

### Configure Backend
Edit `docker/application-docker.yml`

## 🚢 Production Deployment

### Quick Deploy
```bash
cd docker_build
./build-and-start.sh

# Or with PostgreSQL
./hitorro.sh compose-up-ui -f docker-compose-postgres.yml
```

### Cloud Deploy
See `DOCKER_DEPLOYMENT.md` for:
- AWS ECS
- Kubernetes
- Azure Container Instances
- Google Cloud Run

## 📦 What's Included

### Backend Technologies
- Spring Boot 3.x
- Hitorro DMS framework
- Hibernate ORM
- H2/PostgreSQL database
- LibreOffice (transformations)
- Swagger/OpenAPI

### Frontend Technologies
- React 18
- Material-UI (MUI)
- Vite (build tool)
- React Query (data fetching)
- Axios (HTTP client)
- React Dropzone (file upload)

### DevOps
- Multi-stage Docker builds
- Docker Compose orchestration
- Automated scripts
- Health checks
- Volume persistence

## 🎉 You're All Set!

### Next Steps:

1. **Run it**:
   ```bash
   cd docker_build && ./build-and-start.sh
   ```

2. **Explore the UI** at http://localhost:8080

3. **Upload some documents**

4. **Try the API** at http://localhost:8080/swagger-ui.html

5. **Customize it** - See `REACT_UI_GUIDE.md`

## 💬 Need Help?

1. Check `docker_build/README.md`
2. Run `./hitorro.sh help`
3. View logs: `./hitorro.sh logs`
4. Check status: `./hitorro.sh status`

---

## 🏁 The Absolute Shortest Path

```bash
cd docker_build && ./build-and-start.sh && open http://localhost:8080
```

**That's all you need!** 🚀

---

**Happy Document Managing! 🎉**

*Copyright © 2006-2025 Chris Collins*
