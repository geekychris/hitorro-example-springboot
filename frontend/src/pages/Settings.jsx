import React from 'react'
import {
  Box,
  Typography,
  Paper,
  Tabs,
  Tab,
  TextField,
  Button,
  Divider,
  Switch,
  FormControlLabel,
  Alert,
} from '@mui/material'

function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`settings-tabpanel-${index}`}
      aria-labelledby={`settings-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

function Settings() {
  const [tabValue, setTabValue] = React.useState(0)
  const [settings, setSettings] = React.useState({
    notificationsEnabled: true,
    autoSave: true,
    theme: 'light',
  })

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue)
  }

  const handleSettingChange = (key) => (event) => {
    setSettings({
      ...settings,
      [key]: event.target.checked,
    })
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>

      <Paper>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          aria-label="settings tabs"
        >
          <Tab label="General" />
          <Tab label="Notifications" />
          <Tab label="Storage" />
          <Tab label="About" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          <Typography variant="h6" gutterBottom>
            General Settings
          </Typography>
          <Divider sx={{ mb: 3 }} />

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={settings.autoSave}
                  onChange={handleSettingChange('autoSave')}
                />
              }
              label="Enable Auto-save"
            />

            <TextField
              label="Default Upload Folder"
              defaultValue="/"
              variant="outlined"
              fullWidth
            />

            <TextField
              label="Items Per Page"
              type="number"
              defaultValue={25}
              variant="outlined"
            />

            <Box sx={{ mt: 2 }}>
              <Button variant="contained" color="primary">
                Save Changes
              </Button>
            </Box>
          </Box>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" gutterBottom>
            Notification Settings
          </Typography>
          <Divider sx={{ mb: 3 }} />

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={settings.notificationsEnabled}
                  onChange={handleSettingChange('notificationsEnabled')}
                />
              }
              label="Enable Notifications"
            />

            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Upload Notifications"
            />

            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Transformation Notifications"
            />

            <Box sx={{ mt: 2 }}>
              <Button variant="contained" color="primary">
                Save Changes
              </Button>
            </Box>
          </Box>
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" gutterBottom>
            Storage Settings
          </Typography>
          <Divider sx={{ mb: 3 }} />

          <Alert severity="info" sx={{ mb: 2 }}>
            Storage configuration is managed through the backend configuration
          </Alert>

          <Typography variant="body2" color="textSecondary">
            To configure storage backends (S3, local filesystem, etc.), please
            update the application.yml configuration file.
          </Typography>
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          <Typography variant="h6" gutterBottom>
            About Hitorro DMS
          </Typography>
          <Divider sx={{ mb: 3 }} />

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box>
              <Typography variant="subtitle2" color="textSecondary">
                Version
              </Typography>
              <Typography variant="body1">1.0.0</Typography>
            </Box>

            <Box>
              <Typography variant="subtitle2" color="textSecondary">
                API Endpoint
              </Typography>
              <Typography variant="body1">{window.location.origin}/api</Typography>
            </Box>

            <Box>
              <Typography variant="subtitle2" color="textSecondary">
                Documentation
              </Typography>
              <Typography variant="body1">
                <a href="/swagger-ui.html" target="_blank" rel="noopener noreferrer">
                  API Documentation
                </a>
              </Typography>
            </Box>

            <Box>
              <Typography variant="subtitle2" color="textSecondary">
                License
              </Typography>
              <Typography variant="body1">
                Copyright © 2006-2025 Chris Collins
              </Typography>
            </Box>
          </Box>
        </TabPanel>
      </Paper>
    </Box>
  )
}

export default Settings
