import React from 'react'
import { useQuery } from 'react-query'
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  CircularProgress,
} from '@mui/material'
import FolderIcon from '@mui/icons-material/Folder'
import DescriptionIcon from '@mui/icons-material/Description'
import CloudUploadIcon from '@mui/icons-material/CloudUpload'
import StorageIcon from '@mui/icons-material/Storage'
import { dmsApi, systemApi } from '../services/api'

function StatCard({ title, value, icon, color }) {
  return (
    <Card>
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Box>
            <Typography color="textSecondary" gutterBottom variant="body2">
              {title}
            </Typography>
            <Typography variant="h4">{value}</Typography>
          </Box>
          <Box
            sx={{
              backgroundColor: color,
              borderRadius: '50%',
              width: 56,
              height: 56,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'white',
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  )
}

function Dashboard() {
  const { data: health, isLoading: healthLoading } = useQuery(
    'health',
    () => systemApi.getHealth(),
    { refetchInterval: 30000 }
  )

  const { data: stores, isLoading: storesLoading } = useQuery(
    'stores',
    () => dmsApi.getStores()
  )

  if (healthLoading || storesLoading) {
    return (
      <Box className="loading-spinner">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      <Typography variant="body1" color="textSecondary" paragraph>
        Welcome to Hitorro Document Management System
      </Typography>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Documents"
            value="0"
            icon={<DescriptionIcon />}
            color="#1976d2"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Folders"
            value="0"
            icon={<FolderIcon />}
            color="#2e7d32"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Uploads Today"
            value="0"
            icon={<CloudUploadIcon />}
            color="#ed6c02"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Stores"
            value={stores?.data?.length || 0}
            icon={<StorageIcon />}
            color="#9c27b0"
          />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              System Health
            </Typography>
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" gutterBottom>
                Status: <strong>{health?.data?.status || 'Unknown'}</strong>
              </Typography>
              {health?.data?.components && (
                <Box sx={{ mt: 1 }}>
                  {Object.entries(health.data.components).map(([key, value]) => (
                    <Typography key={key} variant="body2" color="textSecondary">
                      {key}: {value.status}
                    </Typography>
                  ))}
                </Box>
              )}
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Available Stores
            </Typography>
            <Box sx={{ mt: 2 }}>
              {stores?.data?.map((store) => (
                <Box key={store.id} sx={{ mb: 1 }}>
                  <Typography variant="body1">
                    <strong>{store.name}</strong>
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Type: {store.storeType} | Default: {store.isDefault ? 'Yes' : 'No'}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Quick Links
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, mt: 2, flexWrap: 'wrap' }}>
              <Box
                component="a"
                href="/swagger-ui.html"
                target="_blank"
                sx={{ textDecoration: 'none' }}
              >
                <Typography color="primary">API Documentation</Typography>
              </Box>
              <Box
                component="a"
                href="/h2-console"
                target="_blank"
                sx={{ textDecoration: 'none' }}
              >
                <Typography color="primary">H2 Console</Typography>
              </Box>
              <Box
                component="a"
                href="/actuator"
                target="_blank"
                sx={{ textDecoration: 'none' }}
              >
                <Typography color="primary">Actuator</Typography>
              </Box>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}

export default Dashboard
